package com.wallet.web3_wallet_backend.blockchain.transaction;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

public class TransactionBuilder {

    private final long chainId;

    public TransactionBuilder() {
        this.chainId = 1L;
        System.out.println("[DEBUG] TransactionBuilder created with default chainId: " + this.chainId);
    }

    public TransactionBuilder(long chainId) {
        this.chainId = chainId;
        System.out.println("[DEBUG] TransactionBuilder created with chainId: " + this.chainId);
    }
    
    public RawTransaction buildLegacyTransaction(
            BigInteger nonce,
            String to,
            BigInteger value,
            BigInteger gasLimit,
            BigInteger gasPrice,
            String data) {
        
        return RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            to,
            value,
            data
        );
    }
    
    public RawTransaction buildEIP1559Transaction(
            BigInteger nonce,
            String to,
            BigInteger value,
            BigInteger gasLimit,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas,
            String data) {
        
        return RawTransaction.createTransaction(
            chainId,
            nonce,
            gasLimit,
            to,
            value,
            data,
            maxPriorityFeePerGas,
            maxFeePerGas
        );
    }
    
    public String signTransaction(RawTransaction transaction, Credentials credentials) {
        System.out.println("[DEBUG] Signing transaction with chainId: " + this.chainId);
        byte[] signedMessage;

        if (isEIP1559Transaction(transaction)) {
            signedMessage = TransactionEncoder.signMessage(transaction, chainId, credentials);
        } else {
            // Legacy transactions should also use chain ID (EIP-155)
            signedMessage = TransactionEncoder.signMessage(transaction, chainId, credentials);
        }

        return Numeric.toHexString(signedMessage);
    }
    
    public String signTransactionWithChainId(RawTransaction transaction, Credentials credentials, long specificChainId) {
        byte[] signedMessage;
        
        if (isEIP1559Transaction(transaction)) {
            signedMessage = TransactionEncoder.signMessage(transaction, specificChainId, credentials);
        } else {
            signedMessage = TransactionEncoder.signMessage(transaction, specificChainId, credentials);
        }
        
        return Numeric.toHexString(signedMessage);
    }
    
    public TransactionComponents buildTransaction(TransactionRequest request) {
        BigInteger nonce = request.nonce();
        BigInteger gasLimit = request.gasLimit();
        String data = request.data() != null ? request.data() : "";
        BigInteger value = request.value();
        
        RawTransaction rawTransaction;
        TransactionType type;
        
        if (request.isEIP1559()) {
            rawTransaction = buildEIP1559Transaction(
                nonce,
                request.to(),
                value,
                gasLimit,
                request.maxPriorityFeePerGas(),
                request.maxFeePerGas(),
                data
            );
            type = TransactionType.EIP1559;
        } else {
            rawTransaction = buildLegacyTransaction(
                nonce,
                request.to(),
                value,
                gasLimit,
                request.gasPrice(),
                data
            );
            type = TransactionType.LEGACY;
        }
        
        return new TransactionComponents(rawTransaction, type);
    }
    
    private boolean isEIP1559Transaction(RawTransaction transaction) {
        try {
            transaction.getClass().getMethod("getMaxPriorityFeePerGas");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    public static class TransactionRequest {
        private final BigInteger nonce;
        private final String to;
        private final BigInteger value;
        private final BigInteger gasLimit;
        private final BigInteger gasPrice;
        private final BigInteger maxPriorityFeePerGas;
        private final BigInteger maxFeePerGas;
        private final String data;
        
        private TransactionRequest(Builder builder) {
            this.nonce = builder.nonce;
            this.to = builder.to;
            this.value = builder.value;
            this.gasLimit = builder.gasLimit;
            this.gasPrice = builder.gasPrice;
            this.maxPriorityFeePerGas = builder.maxPriorityFeePerGas;
            this.maxFeePerGas = builder.maxFeePerGas;
            this.data = builder.data;
        }
        
        public boolean isEIP1559() {
            return maxPriorityFeePerGas != null && maxFeePerGas != null;
        }
        
        public BigInteger nonce() { return nonce; }
        public String to() { return to; }
        public BigInteger value() { return value; }
        public BigInteger gasLimit() { return gasLimit; }
        public BigInteger gasPrice() { return gasPrice; }
        public BigInteger maxPriorityFeePerGas() { return maxPriorityFeePerGas; }
        public BigInteger maxFeePerGas() { return maxFeePerGas; }
        public String data() { return data; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private BigInteger nonce;
            private String to;
            private BigInteger value = BigInteger.ZERO;
            private BigInteger gasLimit;
            private BigInteger gasPrice;
            private BigInteger maxPriorityFeePerGas;
            private BigInteger maxFeePerGas;
            private String data = "";
            
            public Builder nonce(BigInteger nonce) {
                this.nonce = nonce;
                return this;
            }
            
            public Builder to(String to) {
                this.to = to;
                return this;
            }
            
            public Builder value(BigInteger value) {
                this.value = value;
                return this;
            }
            
            public Builder gasLimit(BigInteger gasLimit) {
                this.gasLimit = gasLimit;
                return this;
            }
            
            public Builder gasPrice(BigInteger gasPrice) {
                this.gasPrice = gasPrice;
                return this;
            }
            
            public Builder maxPriorityFeePerGas(BigInteger maxPriorityFeePerGas) {
                this.maxPriorityFeePerGas = maxPriorityFeePerGas;
                return this;
            }
            
            public Builder maxFeePerGas(BigInteger maxFeePerGas) {
                this.maxFeePerGas = maxFeePerGas;
                return this;
            }
            
            public Builder data(String data) {
                this.data = data;
                return this;
            }
            
            public TransactionRequest build() {
                return new TransactionRequest(this);
            }
        }
    }
    
    public static class TransactionComponents {
        public final RawTransaction rawTransaction;
        public final TransactionType type;
        
        public TransactionComponents(RawTransaction rawTransaction, TransactionType type) {
            this.rawTransaction = rawTransaction;
            this.type = type;
        }
    }
    
    public enum TransactionType {
        LEGACY,
        EIP1559
    }
}