package com.wallet.web3_wallet_backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record TransactionResponse(
    @JsonProperty("transactionHash") String transactionHash,
    @JsonProperty("from") String from,
    @JsonProperty("to") String to,
    @JsonProperty("value") String value,
    @JsonProperty("gasUsed") String gasUsed,
    @JsonProperty("status") TransactionStatus status,
    @JsonProperty("blockNumber") Long blockNumber,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("confirmations") Integer confirmations
) {
    public enum TransactionStatus {
        PENDING,
        CONFIRMED,
        FAILED,
        DROPPED
    }
    
    public static TransactionResponse pending(String hash, String from, String to, String value) {
        return new TransactionResponse(
            hash,
            from,
            to,
            value,
            null,
            TransactionStatus.PENDING,
            null,
            Instant.now(),
            0
        );
    }
    
    public static TransactionResponse confirmed(
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
            TransactionStatus.CONFIRMED,
            blockNumber,
            Instant.now(),
            confirmations
        );
    }
    
    public static TransactionResponse failed(
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
            TransactionStatus.FAILED,
            blockNumber,
            Instant.now(),
            0
        );
    }
    
    public static TransactionResponse dropped(String hash, String from, String to, String value) {
        return new TransactionResponse(
            hash,
            from,
            to,
            value,
            null,
            TransactionStatus.DROPPED,
            null,
            Instant.now(),
            0
        );
    }
}