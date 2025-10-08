package com.wallet.web3_wallet_backend.api.controller;

import com.wallet.web3_wallet_backend.api.dto.*;
import com.wallet.web3_wallet_backend.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for ERC-20 token operations.
 */
@RestController
@RequestMapping("/api/v1/tokens")
@Validated
@Tag(name = "Tokens", description = "ERC-20 token operations")
public class TokenController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * Get token information (name, symbol, decimals, total supply).
     */
    @GetMapping("/info/{contractAddress}")
    @Operation(summary = "Get token info", description = "Get ERC-20 token metadata (name, symbol, decimals, total supply)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token info retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid contract address"),
        @ApiResponse(responseCode = "500", description = "Failed to query token contract")
    })
    public ResponseEntity<?> getTokenInfo(
            @Parameter(description = "ERC-20 token contract address", example = "0xdAC17F958D2ee523a2206206994597C13D831ec7")
            @PathVariable String contractAddress) {
        try {
            TokenInfoResponse response = tokenService.getTokenInfo(contractAddress);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to get token info: " + e.getMessage()));
        }
    }

    /**
     * Get token balance for a wallet address.
     */
    @GetMapping("/balance/{address}")
    @Operation(summary = "Get token balance", description = "Get ERC-20 token balance for a wallet address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid address or contract"),
        @ApiResponse(responseCode = "500", description = "Failed to query balance")
    })
    public ResponseEntity<?> getTokenBalance(
            @Parameter(description = "Wallet address", example = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7")
            @PathVariable String address,
            @Parameter(description = "ERC-20 token contract address", example = "0xdAC17F958D2ee523a2206206994597C13D831ec7", required = true)
            @RequestParam String contract) {
        try {
            TokenBalanceResponse response = tokenService.getTokenBalance(address, contract);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to get token balance: " + e.getMessage()));
        }
    }

    /**
     * Transfer ERC-20 tokens.
     */
    @PostMapping("/transfer")
    @Operation(summary = "Transfer tokens", description = "Transfer ERC-20 tokens from one address to another")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token transfer submitted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or insufficient balance"),
        @ApiResponse(responseCode = "500", description = "Transaction failed")
    })
    public ResponseEntity<?> transferToken(@Valid @RequestBody TokenTransferRequest request) {
        try {
            TransactionResponse response = tokenService.transferToken(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Token transfer failed: " + e.getMessage()));
        }
    }
}
