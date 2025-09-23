package com.wallet.web3_wallet_backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for POST /api/wallets/xpub endpoint.
 * Contains mnemonic and account parameter for extended public key derivation.
 */
@Schema(description = "Request to get extended public key (xpub) from mnemonic")
public record XpubRequest(
    @NotBlank(message = "Mnemonic is required")
    @Schema(description = "BIP39 mnemonic phrase (12 or 24 words)", example = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about", required = true)
    String mnemonic,
    
    @Min(value = 0, message = "Account must be >= 0")
    @Schema(description = "Account index for BIP44 derivation", example = "0", defaultValue = "0")
    Integer account
) {
    /**
     * Constructor with default for optional parameter.
     */
    public XpubRequest {
        if (account == null) account = 0;
    }
}