package com.wallet.web3_wallet_backend.api.dto;

public record AuthResponse(
    String token,
    String type,
    String username,
    String email
) {
    public AuthResponse(String token, String username, String email) {
        this(token, "Bearer", username, email);
    }
}
