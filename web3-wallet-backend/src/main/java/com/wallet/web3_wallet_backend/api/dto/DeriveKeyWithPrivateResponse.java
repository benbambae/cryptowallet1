package com.wallet.web3_wallet_backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for deriving keys with private key exposed.
 * ⚠️ WARNING: For testing purposes only! Never use in production.
 */
@Schema(description = "Response containing derived key with private key (TEST ONLY)")
public record DeriveKeyWithPrivateResponse(
    @Schema(description = "Address index used for derivation", example = "0")
    int index,

    @Schema(description = "Derived Ethereum address with EIP-55 checksum", example = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94")
    String address,

    @Schema(description = "BIP44 derivation path used", example = "m/44'/60'/0'/0/0")
    String derivationPath,

    @Schema(description = "Private key in hex format with 0x prefix", example = "0x...")
    String privateKey
) {}
