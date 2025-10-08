package com.wallet.web3_wallet_backend.service;

import com.wallet.web3_wallet_backend.api.dto.TokenBalanceResponse;
import com.wallet.web3_wallet_backend.api.dto.TokenInfoResponse;
import com.wallet.web3_wallet_backend.api.dto.TokenTransferRequest;
import com.wallet.web3_wallet_backend.api.dto.TransactionResponse;
import com.wallet.web3_wallet_backend.blockchain.contract.ERC20Contract;
import com.wallet.web3_wallet_backend.blockchain.transaction.GasManager;
import com.wallet.web3_wallet_backend.blockchain.transaction.TransactionBuilder;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Service for ERC-20 token operations.
 */
@Service
public class TokenService {

    private final Web3j web3j;
    private final GasManager gasManager;
    private final NonceManager nonceManager;
    private final TransactionBuilder transactionBuilder;

    public TokenService(Web3j web3j, GasManager gasManager, NonceManager nonceManager,
                       TransactionBuilder transactionBuilder) {
        this.web3j = web3j;
        this.gasManager = gasManager;
        this.nonceManager = nonceManager;
        this.transactionBuilder = transactionBuilder;
    }

    /**
     * Get token information (name, symbol, decimals, total supply).
     */
    public TokenInfoResponse getTokenInfo(String contractAddress) throws Exception {
        ERC20Contract contract = new ERC20Contract(web3j, contractAddress);

        String name = contract.name();
        String symbol = contract.symbol();
        int decimals = contract.decimals();
        BigInteger totalSupply = contract.totalSupply();

        return new TokenInfoResponse(
            contractAddress,
            name,
            symbol,
            decimals,
            totalSupply.toString()
        );
    }

    /**
     * Get token balance for an address.
     */
    public TokenBalanceResponse getTokenBalance(String address, String contractAddress) throws Exception {
        ERC20Contract contract = new ERC20Contract(web3j, contractAddress);

        String name = contract.name();
        String symbol = contract.symbol();
        int decimals = contract.decimals();
        BigInteger balance = contract.balanceOf(address);

        // Format balance with proper decimals
        BigDecimal balanceDecimal = new BigDecimal(balance)
            .divide(BigDecimal.TEN.pow(decimals), decimals, RoundingMode.DOWN);

        return new TokenBalanceResponse(
            address,
            contractAddress,
            name,
            symbol,
            decimals,
            balance.toString(),
            balanceDecimal.stripTrailingZeros().toPlainString()
        );
    }

    /**
     * Transfer ERC-20 tokens.
     */
    public TransactionResponse transferToken(TokenTransferRequest request) throws Exception {
        // Validate addresses
        if (!isValidAddress(request.from()) || !isValidAddress(request.to()) ||
            !isValidAddress(request.tokenContract())) {
            throw new IllegalArgumentException("Invalid address format");
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Get token decimals
        ERC20Contract contract = new ERC20Contract(web3j, request.tokenContract());
        int decimals = contract.decimals();

        // Convert amount to smallest unit (wei equivalent for tokens)
        BigInteger amountInSmallestUnit = request.amount()
            .multiply(BigDecimal.TEN.pow(decimals))
            .toBigInteger();

        // Check balance
        BigInteger balance = contract.balanceOf(request.from());
        if (balance.compareTo(amountInSmallestUnit) < 0) {
            throw new RuntimeException("Insufficient token balance. Balance: " + balance +
                ", Required: " + amountInSmallestUnit);
        }

        // Encode transfer function call
        String data = contract.encodeTransfer(request.to(), amountInSmallestUnit);

        // Get nonce
        BigInteger nonce = nonceManager.getNextNonce(request.from());

        // Estimate gas limit (token transfers typically use ~65000 gas)
        BigInteger gasLimit = request.gasLimit() != null
            ? BigInteger.valueOf(request.gasLimit())
            : BigInteger.valueOf(65000);

        // Build transaction
        RawTransaction rawTransaction;

        if (request.maxFeePerGas() != null && request.maxPriorityFeePerGas() != null) {
            // EIP-1559 transaction
            BigInteger maxPriorityFee = Convert.toWei(request.maxPriorityFeePerGas(), Convert.Unit.GWEI).toBigInteger();
            BigInteger maxFee = Convert.toWei(request.maxFeePerGas(), Convert.Unit.GWEI).toBigInteger();

            rawTransaction = transactionBuilder.buildEIP1559Transaction(
                nonce,
                request.tokenContract(),
                BigInteger.ZERO, // Value is 0 for token transfers
                gasLimit,
                maxPriorityFee,
                maxFee,
                data
            );
        } else if (request.gasPrice() != null) {
            // Legacy transaction
            BigInteger gasPrice = Convert.toWei(request.gasPrice(), Convert.Unit.GWEI).toBigInteger();

            rawTransaction = transactionBuilder.buildLegacyTransaction(
                nonce,
                request.tokenContract(),
                BigInteger.ZERO,
                gasLimit,
                gasPrice,
                data
            );
        } else {
            // Auto-detect network support
            if (gasManager.supportsEIP1559()) {
                GasManager.EIP1559GasPrices gasPrices = gasManager.getEIP1559GasPrices();
                rawTransaction = transactionBuilder.buildEIP1559Transaction(
                    nonce,
                    request.tokenContract(),
                    BigInteger.ZERO,
                    gasLimit,
                    gasPrices.maxPriorityFeePerGas.medium,
                    gasPrices.maxFeePerGas.medium,
                    data
                );
            } else {
                GasManager.GasPrices gasPrices = gasManager.getLegacyGasPrices();
                rawTransaction = transactionBuilder.buildLegacyTransaction(
                    nonce,
                    request.tokenContract(),
                    BigInteger.ZERO,
                    gasLimit,
                    gasPrices.medium,
                    data
                );
            }
        }

        // Sign transaction
        Credentials credentials = Credentials.create(normalizePrivateKey(request.privateKey()));

        // Verify private key matches from address
        if (!credentials.getAddress().equalsIgnoreCase(request.from())) {
            throw new RuntimeException("Private key does not match the 'from' address");
        }

        String signedTx = transactionBuilder.signTransaction(rawTransaction, credentials);

        // Send transaction
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signedTx).send();

        if (ethSendTransaction.hasError()) {
            nonceManager.releaseNonce(request.from(), nonce);
            throw new RuntimeException("Token transfer failed: " + ethSendTransaction.getError().getMessage());
        }

        String transactionHash = ethSendTransaction.getTransactionHash();

        // Confirm nonce asynchronously
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                nonceManager.confirmTransaction(request.from(), nonce);
            } catch (Exception e) {
                // Ignore
            }
        });

        return new TransactionResponse(
            transactionHash,
            request.from(),
            request.to(),
            request.amount().toString(),
            null,
            TransactionResponse.TransactionStatus.PENDING,
            null,
            Instant.now(),
            0
        );
    }

    /**
     * Validate Ethereum address format.
     */
    private boolean isValidAddress(String address) {
        return address != null && address.matches("^0x[0-9a-fA-F]{40}$");
    }

    /**
     * Normalize private key (remove 0x prefix if present).
     */
    private String normalizePrivateKey(String privateKey) {
        String normalized = privateKey.trim();
        if (normalized.startsWith("0x")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }
}
