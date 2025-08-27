package com.wallet.web3_wallet_backend.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/v1/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "service", "web3-wallet-backend");
    }
}
