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
}