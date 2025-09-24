package com.wallet.web3_wallet_backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for POST /api/wallets/derive endpoint.
 * Contains mnemonic and derivation parameters for BIP44 key derivation.
 * BIP44 = Folder structure for key tree, so wallets know where to find addresses.
 *  m / purpose' / coin_type' / account' / change / address_index
 */
@Schema(description = "Request to derive a key from mnemonic using BIP44 path")
public record DeriveKeyRequest(
    @NotBlank(message = "Mnemonic is required")
    @Schema(description = "BIP39 mnemonic phrase (12 or 24 words)", example = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about", required = true)
    String mnemonic,
    
    @Min(value = 0, message = "Index must be >= 0")
    @Schema(description = "Address index for derivation", example = "0", required = true)
    int index,
    
    @Min(value = 0, message = "Account must be >= 0")
    @Schema(description = "Account index for BIP44 derivation", example = "0", defaultValue = "0")
    Integer account,
    
    @Min(value = 0, message = "Change must be >= 0")
    @Schema(description = "Change index for BIP44 derivation (0=external, 1=internal)", example = "0", defaultValue = "0")
    Integer change
) {
    /**
     * Constructor with defaults for optional parameters.
     */
    public DeriveKeyRequest {
        if (account == null) account = 0;
        if (change == null) change = 0;
    }
}