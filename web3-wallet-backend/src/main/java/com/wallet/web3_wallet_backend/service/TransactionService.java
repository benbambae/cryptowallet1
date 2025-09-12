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
    
    private final Web3j web3j;
    private final GasManager gasManager;
    private final NonceManager nonceManager;
    private final TransactionBuilder transactionBuilder;
    private final WalletService walletService;
    
    public TransactionService(Web3j web3j, GasManager gasManager, NonceManager nonceManager,
                            TransactionBuilder transactionBuilder, WalletService walletService) {
        this.web3j = web3j;
        this.gasManager = gasManager;
        this.nonceManager = nonceManager;
        this.transactionBuilder = transactionBuilder;
        this.walletService = walletService;
    }
    
    public TransactionResponse sendTransaction(TransactionRequest request) throws Exception {
        validateTransactionRequest(request);
        
        String from = request.from();
        String to = request.to();
        BigInteger value = Convert.toWei(request.value(), Convert.Unit.ETHER).toBigInteger();
        String data = request.data() != null ? request.data() : "";
        
        BigInteger nonce = request.nonce() != null 
            ? BigInteger.valueOf(request.nonce())
            : nonceManager.getNextNonce(from);
        
        BigInteger gasLimit = request.gasLimit() != null
            ? BigInteger.valueOf(request.gasLimit())
            : gasManager.estimateGasLimit(from, to, value, data);
        
        RawTransaction rawTransaction;
        
        if (request.isEIP1559()) {
            BigInteger maxPriorityFee = Convert.toWei(request.maxPriorityFeePerGas(), Convert.Unit.GWEI).toBigInteger();
            BigInteger maxFee = Convert.toWei(request.maxFeePerGas(), Convert.Unit.GWEI).toBigInteger();
            
            rawTransaction = transactionBuilder.buildEIP1559Transaction(
                nonce, to, value, gasLimit, maxPriorityFee, maxFee, data
            );
        } else if (request.isLegacy()) {
            BigInteger gasPrice = Convert.toWei(request.gasPrice(), Convert.Unit.GWEI).toBigInteger();
            
            rawTransaction = transactionBuilder.buildLegacyTransaction(
                nonce, to, value, gasLimit, gasPrice, data
            );
        } else {
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
        
        Credentials credentials = Credentials.create(normalizePrivateKey(request.privateKey()));
        String signedTx = transactionBuilder.signTransaction(rawTransaction, credentials);
        
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signedTx).send();
        
        if (ethSendTransaction.hasError()) {
            nonceManager.releaseNonce(from, nonce);
            throw new RuntimeException("Transaction failed: " + ethSendTransaction.getError().getMessage());
        }
        
        String transactionHash = ethSendTransaction.getTransactionHash();
        
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
                nonceManager.confirmTransaction(from, nonce);
            } catch (Exception e) {
            }
        });
        
        return TransactionResponse.pending(
            transactionHash,
            from,
            to,
            request.value().toString()
        );
    }
    
    public GasEstimateResponse estimateGas(String from, String to, BigDecimal value, String data) throws IOException {
        BigInteger weiValue = value != null 
            ? Convert.toWei(value, Convert.Unit.ETHER).toBigInteger()
            : BigInteger.ZERO;
        
        BigInteger gasLimit = gasManager.estimateGasLimit(from, to, weiValue, data);
        
        GasEstimateResponse.GasPrice legacyPrices = null;
        GasEstimateResponse.EIP1559GasPrice eip1559Prices = null;
        GasEstimateResponse.EstimatedCost estimatedCost;
        
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
            GasManager.GasPrices legacy = gasManager.getLegacyGasPrices();
            
            legacyPrices = new GasEstimateResponse.GasPrice(
                GasManager.weiToGwei(legacy.slow),
                GasManager.weiToGwei(legacy.medium),
                GasManager.weiToGwei(legacy.fast)
            );
            
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
        
        return new GasEstimateResponse(
            gasLimit.longValue(),
            legacyPrices,
            eip1559Prices,
            estimatedCost
        );
    }
    
    public TransactionStatusResponse getTransactionStatus(String transactionHash) throws IOException {
        EthTransaction ethTransaction = web3j.ethGetTransactionByHash(transactionHash).send();
        Optional<org.web3j.protocol.core.methods.response.Transaction> txOpt = ethTransaction.getTransaction();
        
        if (txOpt.isEmpty()) {
            return TransactionStatusResponse.notFound(transactionHash);
        }
        
        org.web3j.protocol.core.methods.response.Transaction tx = txOpt.get();
        
        BigInteger currentBlock = web3j.ethBlockNumber().send().getBlockNumber();
        BigInteger txBlock = tx.getBlockNumber();
        
        TransactionStatusResponse.Status status;
        Integer confirmations = 0;
        
        if (txBlock == null) {
            status = TransactionStatusResponse.Status.PENDING;
        } else {
            confirmations = currentBlock.subtract(txBlock).intValue() + 1;
            
            Optional<TransactionReceipt> receiptOpt = web3j.ethGetTransactionReceipt(transactionHash)
                .send()
                .getTransactionReceipt();
            
            if (receiptOpt.isPresent()) {
                TransactionReceipt receipt = receiptOpt.get();
                boolean success = "0x1".equals(receipt.getStatus());
                
                if (success) {
                    status = confirmations >= 12 
                        ? TransactionStatusResponse.Status.CONFIRMED 
                        : TransactionStatusResponse.Status.CONFIRMING;
                } else {
                    status = TransactionStatusResponse.Status.FAILED;
                }
            } else {
                status = TransactionStatusResponse.Status.CONFIRMING;
            }
        }
        
        BigDecimal value = Convert.fromWei(new BigDecimal(tx.getValue()), Convert.Unit.ETHER);
        
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
    
    private String normalizePrivateKey(String privateKey) {
        String normalized = privateKey.trim();
        if (normalized.startsWith("0x")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }
}