package com.wallet.web3_wallet_backend.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for POST /api/wallets/xpub endpoint.
 * Contains the extended public key for the specified account.
 */
@Schema(description = "Response containing extended public key (xpub)")
public record XpubResponse(
    @Schema(description = "Base58-encoded extended public key", example = "xpub6BosfCnifzxcFwrSzQiqu2DBVTshkCXacvNsWGYJVVhhawA7d4R5WSWGFNbi8Aw6ZRc1brxMyWMzG3DSSSSoekkudhUd9yLb6qx39T9nMdj")
    String xpub
) {}