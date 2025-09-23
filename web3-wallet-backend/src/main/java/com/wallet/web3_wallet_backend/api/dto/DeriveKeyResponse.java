package com.wallet.web3_wallet_backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for POST /api/wallets/derive endpoint.
 * Contains the derived key information without exposing private keys.
 */
@Schema(description = "Response containing derived key information")
public record DeriveKeyResponse(
    @Schema(description = "Address index used for derivation", example = "0")
    int index,
    
    @Schema(description = "Derived Ethereum address with EIP-55 checksum", example = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94")
    String address,
    
    @Schema(description = "BIP44 derivation path used", example = "m/44'/60'/0'/0/0")
    String path
) {}