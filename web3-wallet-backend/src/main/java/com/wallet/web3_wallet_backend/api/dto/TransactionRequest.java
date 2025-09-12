package com.wallet.web3_wallet_backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record TransactionRequest(
    @JsonProperty("from") String from,
    @JsonProperty("to") String to,
    @JsonProperty("value") BigDecimal value,
    @JsonProperty("data") String data,
    @JsonProperty("gasLimit") Long gasLimit,
    @JsonProperty("gasPrice") BigDecimal gasPrice,
    @JsonProperty("maxFeePerGas") BigDecimal maxFeePerGas,
    @JsonProperty("maxPriorityFeePerGas") BigDecimal maxPriorityFeePerGas,
    @JsonProperty("privateKey") String privateKey,
    @JsonProperty("nonce") Long nonce
) {
    public TransactionRequest {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("From address is required");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("To address is required");
        }
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Value must be non-negative");
        }
    }
    
    public boolean isEIP1559() {
        return maxFeePerGas != null && maxPriorityFeePerGas != null;
    }
    
    public boolean isLegacy() {
        return gasPrice != null && !isEIP1559();
    }
}