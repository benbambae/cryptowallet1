package com.wallet.web3_wallet_backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for POST /api/wallets endpoint.
 * Contains the generated mnemonic and the first derived Ethereum address.
 */
@Schema(description = "Response containing generated mnemonic and first Ethereum address")
public record CreateWalletResponse(
    @Schema(description = "BIP39 mnemonic phrase (12 or 24 words)", example = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about")
    String mnemonic,
    
    @Schema(description = "First derived Ethereum address (m/44'/60'/0'/0/0)", example = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94")
    String firstAddress
) {}