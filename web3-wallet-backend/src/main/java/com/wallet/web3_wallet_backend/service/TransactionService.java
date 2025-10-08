package com.wallet.web3_wallet_backend.service;

import com.wallet.web3_wallet_backend.api.dto.*;
import com.wallet.web3_wallet_backend.blockchain.transaction.GasManager;
import com.wallet.web3_wallet_backend.blockchain.transaction.TransactionBuilder;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class TransactionService {

    // Dependencies for blockchain interaction and transaction management
    private final Web3j web3j;
    private final GasManager gasManager;
    private final NonceManager nonceManager;
    private final TransactionBuilder transactionBuilder;
    private final WalletService walletService;
    private final com.wallet.web3_wallet_backend.repository.TransactionRepository transactionRepository;

    // Constructor injection for dependencies
    public TransactionService(Web3j web3j, GasManager gasManager, NonceManager nonceManager,
                              TransactionBuilder transactionBuilder, WalletService walletService,
                              com.wallet.web3_wallet_backend.repository.TransactionRepository transactionRepository) {
        this.web3j = web3j;
        this.gasManager = gasManager;
        this.nonceManager = nonceManager;
        this.transactionBuilder = transactionBuilder;
        this.walletService = walletService;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Sends a transaction to the blockchain.
     * Handles both legacy and EIP-1559 transaction types.
     * @param request TransactionRequest object containing all transaction details
     * @return TransactionResponse with transaction hash and status
     * @throws Exception if transaction fails or is invalid
     */
    public TransactionResponse sendTransaction(TransactionRequest request) throws Exception {
        // Validate the transaction request fields
        validateTransactionRequest(request);

        // Extract transaction details from request
        String from = request.from();
        String to = request.to();
        // Convert value from ETH to Wei (smallest unit)
        BigInteger value = Convert.toWei(request.value(), Convert.Unit.ETHER).toBigInteger();
        String data = request.data() != null ? request.data() : "";

        // Determine nonce (transaction count for the sender)
        BigInteger nonce = request.nonce() != null
                ? BigInteger.valueOf(request.nonce())
                : nonceManager.getNextNonce(from);

        // Determine gas limit (max gas allowed for transaction)
        BigInteger gasLimit = request.gasLimit() != null
                ? BigInteger.valueOf(request.gasLimit())
                : gasManager.estimateGasLimit(from, to, value, data);

        RawTransaction rawTransaction;

        // Build the transaction based on type (EIP-1559 or legacy)
        if (request.isEIP1559()) {
            // For EIP-1559, use maxPriorityFeePerGas and maxFeePerGas
            BigInteger maxPriorityFee = Convert.toWei(request.maxPriorityFeePerGas(), Convert.Unit.GWEI).toBigInteger();
            BigInteger maxFee = Convert.toWei(request.maxFeePerGas(), Convert.Unit.GWEI).toBigInteger();

            rawTransaction = transactionBuilder.buildEIP1559Transaction(
                    nonce, to, value, gasLimit, maxPriorityFee, maxFee, data
            );
        } else if (request.isLegacy()) {
            // For legacy, use gasPrice
            BigInteger gasPrice = Convert.toWei(request.gasPrice(), Convert.Unit.GWEI).toBigInteger();

            rawTransaction = transactionBuilder.buildLegacyTransaction(
                    nonce, to, value, gasLimit, gasPrice, data
            );
        } else {
            // If not specified, auto-detect based on network support
            if (gasManager.supportsEIP1559()) {
                GasManager.EIP1559GasPrices eipPrices = gasManager.getEIP1559GasPrices();
                rawTransaction = transactionBuilder.buildEIP1559Transaction(
                        nonce, to, value, gasLimit,
                        eipPrices.maxPriorityFeePerGas.medium,
                        eipPrices.maxFeePerGas.medium,
                        data
                );
            } else {
                GasManager.GasPrices legacyPrices = gasManager.getLegacyGasPrices();
                rawTransaction = transactionBuilder.buildLegacyTransaction(
                        nonce, to, value, gasLimit, legacyPrices.medium, data
                );
            }
        }

        // Sign the transaction with the sender's private key
        Credentials credentials = Credentials.create(normalizePrivateKey(request.privateKey()));

        // Verify private key matches the from address
        String derivedAddress = credentials.getAddress();
        System.out.println("[DEBUG] Address derived from private key: " + derivedAddress);
        System.out.println("[DEBUG] Requested from address: " + from);

        if (!derivedAddress.equalsIgnoreCase(from)) {
            throw new RuntimeException("Private key does not match the 'from' address. " +
                    "Private key controls: " + derivedAddress + ", but you specified: " + from);
        }

        System.out.println("[DEBUG] Transaction details:");
        System.out.println("  From: " + from);
        System.out.println("  To: " + to);
        System.out.println("  Value (Wei): " + value);
        System.out.println("  Nonce: " + nonce);
        System.out.println("  Gas Limit: " + gasLimit);

        String signedTx = transactionBuilder.signTransaction(rawTransaction, credentials);

        // Send the signed transaction to the blockchain
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signedTx).send();

        // If there is an error, release the nonce and throw an exception
        if (ethSendTransaction.hasError()) {
            nonceManager.releaseNonce(from, nonce);
            System.err.println("[ERROR] Transaction failed: " + ethSendTransaction.getError().getMessage());
            System.err.println("[ERROR] Error code: " + ethSendTransaction.getError().getCode());
            throw new RuntimeException("Transaction failed: " + ethSendTransaction.getError().getMessage());
        }

        // Get the transaction hash from the response
        String transactionHash = ethSendTransaction.getTransactionHash();

        // Confirm the nonce asynchronously after a short delay
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                nonceManager.confirmTransaction(from, nonce);
            } catch (Exception e) {
                // Ignore exceptions in async confirmation
            }
        });

        // Return a pending transaction response
        return createPendingTransactionResponse(
                transactionHash,
                from,
                to,
                request.value().toString()
        );
    }

    /**
     * Estimates gas usage and cost for a transaction.
     * Returns both legacy and EIP-1559 gas price/costs if available.
     */
    public GasEstimateResponse estimateGas(String from, String to, BigDecimal value, String data) throws IOException {
        // Convert value to Wei, or use zero if null
        BigInteger weiValue = value != null
                ? Convert.toWei(value, Convert.Unit.ETHER).toBigInteger()
                : BigInteger.ZERO;

        // Estimate gas limit for the transaction
        BigInteger gasLimit = gasManager.estimateGasLimit(from, to, weiValue, data);

        GasEstimateResponse.GasPrice legacyPrices = null;
        GasEstimateResponse.EIP1559GasPrice eip1559Prices = null;
        GasEstimateResponse.EstimatedCost estimatedCost;

        // If EIP-1559 is supported, provide EIP-1559 gas price/costs
        if (gasManager.supportsEIP1559()) {
            GasManager.EIP1559GasPrices eip = gasManager.getEIP1559GasPrices();

            eip1559Prices = new GasEstimateResponse.EIP1559GasPrice(
                    new GasEstimateResponse.EIP1559GasPrice.MaxFee(
                            GasManager.weiToGwei(eip.maxFeePerGas.slow),
                            GasManager.weiToGwei(eip.maxFeePerGas.medium),
                            GasManager.weiToGwei(eip.maxFeePerGas.fast)
                    ),
                    new GasEstimateResponse.EIP1559GasPrice.PriorityFee(
                            GasManager.weiToGwei(eip.maxPriorityFeePerGas.slow),
                            GasManager.weiToGwei(eip.maxPriorityFeePerGas.medium),
                            GasManager.weiToGwei(eip.maxPriorityFeePerGas.fast)
                    )
            );

            // Calculate estimated cost for slow, medium, and fast speeds (in ETH)
            BigDecimal slowCost = Convert.fromWei(
                    new BigDecimal(gasLimit.multiply(eip.maxFeePerGas.slow)),
                    Convert.Unit.ETHER
            );
            BigDecimal mediumCost = Convert.fromWei(
                    new BigDecimal(gasLimit.multiply(eip.maxFeePerGas.medium)),
                    Convert.Unit.ETHER
            );
            BigDecimal fastCost = Convert.fromWei(
                    new BigDecimal(gasLimit.multiply(eip.maxFeePerGas.fast)),
                    Convert.Unit.ETHER
            );

            estimatedCost = new GasEstimateResponse.EstimatedCost(slowCost, mediumCost, fastCost);
        } else {
            // Otherwise, provide legacy gas price/costs
            GasManager.GasPrices legacy = gasManager.getLegacyGasPrices();

            legacyPrices = new GasEstimateResponse.GasPrice(
                    GasManager.weiToGwei(legacy.slow),
                    GasManager.weiToGwei(legacy.medium),
                    GasManager.weiToGwei(legacy.fast)
            );

            // Calculate estimated cost for slow, medium, and fast speeds (in ETH)
            BigDecimal slowCost = Convert.fromWei(
                    new BigDecimal(gasLimit.multiply(legacy.slow)),
                    Convert.Unit.ETHER
            );
            BigDecimal mediumCost = Convert.fromWei(
                    new BigDecimal(gasLimit.multiply(legacy.medium)),
                    Convert.Unit.ETHER
            );
            BigDecimal fastCost = Convert.fromWei(
                    new BigDecimal(gasLimit.multiply(legacy.fast)),
                    Convert.Unit.ETHER
            );

            estimatedCost = new GasEstimateResponse.EstimatedCost(slowCost, mediumCost, fastCost);
        }

        // Return the gas estimate response with all calculated values
        return new GasEstimateResponse(
                gasLimit.longValue(),
                legacyPrices,
                eip1559Prices,
                estimatedCost
        );
    }

    /**
     * Gets the status of a transaction by its hash.
     * Returns status as PENDING, CONFIRMING, CONFIRMED, FAILED, or DROPPED.
     */
    public TransactionStatusResponse getTransactionStatus(String transactionHash) throws IOException {
        // Fetch the transaction details from the blockchain
        EthTransaction ethTransaction = web3j.ethGetTransactionByHash(transactionHash).send();
        Optional<org.web3j.protocol.core.methods.response.Transaction> txOpt = ethTransaction.getTransaction();

        // If transaction not found, return a dropped status
        if (txOpt.isEmpty()) {
            return createNotFoundTransactionStatusResponse(transactionHash);
        }

        org.web3j.protocol.core.methods.response.Transaction tx = txOpt.get();

        // Get the current block number and the block number of the transaction
        BigInteger currentBlock = web3j.ethBlockNumber().send().getBlockNumber();
        BigInteger txBlock = tx.getBlockNumber();

        TransactionStatusResponse.Status status;
        Integer confirmations = 0;

        if (txBlock == null) {
            // Transaction is pending (not yet mined)
            status = TransactionStatusResponse.Status.PENDING;
        } else {
            // Calculate number of confirmations
            confirmations = currentBlock.subtract(txBlock).intValue() + 1;

            // Try to get the transaction receipt to check if it succeeded
            Optional<TransactionReceipt> receiptOpt = web3j.ethGetTransactionReceipt(transactionHash)
                    .send()
                    .getTransactionReceipt();

            if (receiptOpt.isPresent()) {
                TransactionReceipt receipt = receiptOpt.get();
                boolean success = "0x1".equals(receipt.getStatus());

                if (success) {
                    // If enough confirmations, mark as CONFIRMED, else CONFIRMING
                    status = confirmations >= 12
                            ? TransactionStatusResponse.Status.CONFIRMED
                            : TransactionStatusResponse.Status.CONFIRMING;
                } else {
                    // Transaction failed
                    status = TransactionStatusResponse.Status.FAILED;
                }
            } else {
                // Transaction is mined but receipt not available yet
                status = TransactionStatusResponse.Status.CONFIRMING;
            }
        }

        // Convert transaction value from Wei to ETH
        BigDecimal value = Convert.fromWei(new BigDecimal(tx.getValue()), Convert.Unit.ETHER);

        // Build and return the transaction status response
        return new TransactionStatusResponse(
                transactionHash,
                status,
                txBlock != null ? txBlock.longValue() : null,
                confirmations,
                tx.getFrom(),
                tx.getTo(),
                value,
                tx.getGas().longValue(),
                GasManager.weiToGwei(tx.getGasPrice()),
                Instant.now(),
                null,
                null
        );
    }

    /**
     * Validates the transaction request fields.
     * Throws IllegalArgumentException if any field is invalid.
     */
    private void validateTransactionRequest(TransactionRequest request) {
        if (!walletService.isValidAddress(request.from())) {
            throw new IllegalArgumentException("Invalid from address");
        }
        if (!walletService.isValidAddress(request.to())) {
            throw new IllegalArgumentException("Invalid to address");
        }
        if (request.value().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Value cannot be negative");
        }
        if (request.privateKey() == null || request.privateKey().isBlank()) {
            throw new IllegalArgumentException("Private key is required");
        }
    }

    /**
     * Normalizes a private key string by trimming and removing '0x' prefix if present.
     */
    private String normalizePrivateKey(String privateKey) {
        String normalized = privateKey.trim();
        if (normalized.startsWith("0x")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }

    // ----------------- Factory Methods for TransactionResponse -----------------

    /**
     * Creates a pending TransactionResponse.
     */
    public TransactionResponse createPendingTransactionResponse(String hash, String from, String to, String value) {
        return new TransactionResponse(
                hash,
                from,
                to,
                value,
                null,
                TransactionResponse.TransactionStatus.PENDING,
                null,
                Instant.now(),
                0
        );
    }

    /**
     * Creates a confirmed TransactionResponse.
     */
    public TransactionResponse createConfirmedTransactionResponse(
            String hash,
            String from,
            String to,
            String value,
            String gasUsed,
            Long blockNumber,
            Integer confirmations) {
        return new TransactionResponse(
                hash,
                from,
                to,
                value,
                gasUsed,
                TransactionResponse.TransactionStatus.CONFIRMED,
                blockNumber,
                Instant.now(),
                confirmations
        );
    }

    /**
     * Creates a failed TransactionResponse.
     */
    public TransactionResponse createFailedTransactionResponse(
            String hash,
            String from,
            String to,
            String value,
            String gasUsed,
            Long blockNumber) {
        return new TransactionResponse(
                hash,
                from,
                to,
                value,
                gasUsed,
                TransactionResponse.TransactionStatus.FAILED,
                blockNumber,
                Instant.now(),
                0
        );
    }

    /**
     * Creates a dropped TransactionResponse.
     */
    public TransactionResponse createDroppedTransactionResponse(String hash, String from, String to, String value) {
        return new TransactionResponse(
                hash,
                from,
                to,
                value,
                null,
                TransactionResponse.TransactionStatus.DROPPED,
                null,
                Instant.now(),
                0
        );
    }

    // ----------------- Factory Methods for TransactionStatusResponse -----------------

    /**
     * Creates a TransactionStatusResponse for a not found (dropped) transaction.
     */
    public TransactionStatusResponse createNotFoundTransactionStatusResponse(String hash) {
        return new TransactionStatusResponse(
                hash,
                TransactionStatusResponse.Status.DROPPED,
                null,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Transaction not found"
        );
    }

    // ----------------- Business Logic Methods -----------------

    /**
     * Checks if a transaction is finalized based on required confirmations.
     */
    public boolean isTransactionFinalized(TransactionStatusResponse response, int requiredConfirmations) {
        return response.confirmations() != null && response.confirmations() >= requiredConfirmations;
    }

    /**
     * Determines the direction of a transaction (INCOMING, OUTGOING, SELF) for a given address.
     */
    public TransactionHistoryResponse.TransactionSummary.Direction determineTransactionDirection(String address, String from, String to) {
        if (from.equalsIgnoreCase(address) && to.equalsIgnoreCase(address)) {
            return TransactionHistoryResponse.TransactionSummary.Direction.SELF;
        } else if (from.equalsIgnoreCase(address)) {
            return TransactionHistoryResponse.TransactionSummary.Direction.OUTGOING;
        } else {
            return TransactionHistoryResponse.TransactionSummary.Direction.INCOMING;
        }
    }

    /**
     * Get transaction history for a specific address.
     * Returns all transactions from or to the address, sorted by newest first.
     */
    public TransactionHistoryResponse getTransactionHistory(String address) {
        // Get all transactions from database
        java.util.List<com.wallet.web3_wallet_backend.model.TransactionEntity> entities =
            transactionRepository.findByFromAddressOrToAddressOrderByCreatedAtDesc(address, address);

        // Convert entities to transaction summaries
        java.util.List<TransactionHistoryResponse.TransactionSummary> summaries =
            entities.stream().map(entity -> {
                TransactionHistoryResponse.TransactionSummary.Direction direction =
                    determineTransactionDirection(address, entity.getFromAddress(), entity.getToAddress());

                return new TransactionHistoryResponse.TransactionSummary(
                    entity.getTxHash(),
                    entity.getFromAddress(),
                    entity.getToAddress(),
                    entity.getValue(),
                    direction,
                    entity.getStatus().toString(),
                    entity.getCreatedAt(),
                    entity.getBlockNumber(),
                    entity.getGasUsed(),
                    entity.getGasPrice()
                );
            }).toList();

        return new TransactionHistoryResponse(
            address,
            summaries,
            summaries.size(),
            1,  // page
            summaries.size()  // pageSize
        );
    }
}