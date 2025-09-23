package com.wallet.web3_wallet_backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionStatusResponse(
    @JsonProperty("transactionHash") String transactionHash,
    @JsonProperty("status") Status status,
    @JsonProperty("blockNumber") Long blockNumber,
    @JsonProperty("confirmations") Integer confirmations,
    @JsonProperty("from") String from,
    @JsonProperty("to") String to,
    @JsonProperty("value") BigDecimal value,
    @JsonProperty("gasUsed") Long gasUsed,
    @JsonProperty("effectiveGasPrice") BigDecimal effectiveGasPrice,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("blockTimestamp") Instant blockTimestamp,
    @JsonProperty("error") String error
) {
    public enum Status {
        PENDING,
        CONFIRMING,
        CONFIRMED,
        FAILED,
        DROPPED,
        REPLACED
    }
}