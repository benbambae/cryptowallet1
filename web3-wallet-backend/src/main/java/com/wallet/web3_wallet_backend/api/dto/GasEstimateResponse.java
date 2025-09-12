package com.wallet.web3_wallet_backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record GasEstimateResponse(
    @JsonProperty("gasLimit") Long gasLimit,
    @JsonProperty("gasPrice") GasPrice gasPrice,
    @JsonProperty("eip1559") EIP1559GasPrice eip1559,
    @JsonProperty("estimatedCost") EstimatedCost estimatedCost
) {
    
    public record GasPrice(
        @JsonProperty("slow") BigDecimal slow,
        @JsonProperty("medium") BigDecimal medium,
        @JsonProperty("fast") BigDecimal fast
    ) {}
    
    public record EIP1559GasPrice(
        @JsonProperty("maxFeePerGas") MaxFee maxFeePerGas,
        @JsonProperty("maxPriorityFeePerGas") PriorityFee maxPriorityFeePerGas
    ) {
        public record MaxFee(
            @JsonProperty("slow") BigDecimal slow,
            @JsonProperty("medium") BigDecimal medium,
            @JsonProperty("fast") BigDecimal fast
        ) {}
        
        public record PriorityFee(
            @JsonProperty("slow") BigDecimal slow,
            @JsonProperty("medium") BigDecimal medium,
            @JsonProperty("fast") BigDecimal fast
        ) {}
    }
    
    public record EstimatedCost(
        @JsonProperty("slow") BigDecimal slow,
        @JsonProperty("medium") BigDecimal medium,
        @JsonProperty("fast") BigDecimal fast,
        @JsonProperty("currency") String currency
    ) {
        public EstimatedCost(BigDecimal slow, BigDecimal medium, BigDecimal fast) {
            this(slow, medium, fast, "ETH");
        }
    }
}