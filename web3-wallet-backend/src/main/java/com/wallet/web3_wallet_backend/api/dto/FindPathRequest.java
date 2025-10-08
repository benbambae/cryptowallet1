package com.wallet.web3_wallet_backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for finding derivation path of a target address.
 */
@Schema(description = "Request to find which derivation path produces a target address")
public record FindPathRequest(
    @NotBlank(message = "Mnemonic is required")
    @Schema(description = "BIP39 mnemonic phrase", example = "word1 word2 word3...")
    String mnemonic,

    @NotBlank(message = "Target address is required")
    @Schema(description = "The Ethereum address to search for", example = "0x...")
    String targetAddress
) {}
