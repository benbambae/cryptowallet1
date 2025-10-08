package com.wallet.web3_wallet_backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for token information.
 */
@Schema(description = "ERC-20 token information")
public record TokenInfoResponse(
    @Schema(description = "Token contract address", example = "0xdAC17F958D2ee523a2206206994597C13D831ec7")
    String contractAddress,

    @Schema(description = "Token name", example = "Tether USD")
    String name,

    @Schema(description = "Token symbol", example = "USDT")
    String symbol,

    @Schema(description = "Token decimals", example = "6")
    int decimals,

    @Schema(description = "Total supply (raw)", example = "1000000000000000")
    String totalSupply
) {}
