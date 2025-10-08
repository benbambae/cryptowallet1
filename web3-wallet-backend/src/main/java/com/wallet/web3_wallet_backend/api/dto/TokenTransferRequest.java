package com.wallet.web3_wallet_backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO for ERC-20 token transfers.
 */
@Schema(description = "Request to transfer ERC-20 tokens")
public record TokenTransferRequest(
    @NotBlank(message = "From address is required")
    @Schema(description = "Sender address", example = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7", required = true)
    String from,

    @NotBlank(message = "Private key is required")
    @Schema(description = "Sender's private key (hex format)", example = "0x...", required = true)
    String privateKey,

    @NotBlank(message = "To address is required")
    @Schema(description = "Recipient address", example = "0x5aAeb6053f3E94C9b9A09f33669435E7Ef1BeAed", required = true)
    String to,

    @NotBlank(message = "Token contract address is required")
    @Schema(description = "ERC-20 token contract address", example = "0xdAC17F958D2ee523a2206206994597C13D831ec7", required = true)
    String tokenContract,

    @NotNull(message = "Amount is required")
    @Schema(description = "Amount to transfer (in token units, not wei)", example = "100.5", required = true)
    BigDecimal amount,

    @Schema(description = "Gas limit override (optional)", example = "65000")
    Long gasLimit,

    @Schema(description = "Gas price in Gwei (optional, for legacy transactions)", example = "20")
    BigDecimal gasPrice,

    @Schema(description = "Max priority fee per gas in Gwei (optional, for EIP-1559)", example = "1.5")
    BigDecimal maxPriorityFeePerGas,

    @Schema(description = "Max fee per gas in Gwei (optional, for EIP-1559)", example = "30")
    BigDecimal maxFeePerGas
) {}
