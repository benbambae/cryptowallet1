package com.wallet.web3_wallet_backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for token balance queries.
 */
@Schema(description = "Token balance information")
public record TokenBalanceResponse(
    @Schema(description = "Wallet address", example = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7")
    String address,

    @Schema(description = "Token contract address", example = "0xdAC17F958D2ee523a2206206994597C13D831ec7")
    String contractAddress,

    @Schema(description = "Token name", example = "Tether USD")
    String name,

    @Schema(description = "Token symbol", example = "USDT")
    String symbol,

    @Schema(description = "Token decimals", example = "6")
    int decimals,

    @Schema(description = "Raw balance (in smallest unit)", example = "1000000000")
    String balanceRaw,

    @Schema(description = "Formatted balance", example = "1000.00")
    String balanceFormatted
) {}
