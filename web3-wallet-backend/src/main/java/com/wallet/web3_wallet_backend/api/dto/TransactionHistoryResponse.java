package com.wallet.web3_wallet_backend.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransactionHistoryResponse(
    @JsonProperty("address") String address,
    @JsonProperty("transactions") List<TransactionSummary> transactions,
    @JsonProperty("totalCount") Integer totalCount,
    @JsonProperty("page") Integer page,
    @JsonProperty("pageSize") Integer pageSize
) {
    
    public record TransactionSummary(
        @JsonProperty("hash") String hash,
        @JsonProperty("from") String from,
        @JsonProperty("to") String to,
        @JsonProperty("value") BigDecimal value,
        @JsonProperty("direction") Direction direction,
        @JsonProperty("status") String status,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("blockNumber") Long blockNumber,
        @JsonProperty("gasUsed") Long gasUsed,
        @JsonProperty("gasPrice") BigDecimal gasPrice
    ) {
        public enum Direction {
            INCOMING,
            OUTGOING,
            SELF
        }
        
        public static Direction determineDirection(String address, String from, String to) {
            if (from.equalsIgnoreCase(address) && to.equalsIgnoreCase(address)) {
                return Direction.SELF;
            } else if (from.equalsIgnoreCase(address)) {
                return Direction.OUTGOING;
            } else {
                return Direction.INCOMING;
            }
        }
    }
}