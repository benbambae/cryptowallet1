package com.wallet.web3_wallet_backend.api.controller;

import com.wallet.web3_wallet_backend.api.dto.*;
import com.wallet.web3_wallet_backend.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/transaction")
public class TransactionController {
    
    private final TransactionService transactionService;
    
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
    
    @PostMapping("/send")
    public ResponseEntity<?> sendTransaction(@RequestBody TransactionRequest request) {
        try {
            TransactionResponse response = transactionService.sendTransaction(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Transaction failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/estimate-gas")
    public ResponseEntity<?> estimateGas(@RequestBody GasEstimateRequest request) {
        try {
            GasEstimateResponse response = transactionService.estimateGas(
                request.from(),
                request.to(),
                request.value(),
                request.data()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Gas estimation failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{hash}/status")
    public ResponseEntity<?> getTransactionStatus(@PathVariable String hash) {
        try {
            TransactionStatusResponse response = transactionService.getTransactionStatus(hash);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Failed to get transaction status: " + e.getMessage()));
        }
    }

    @GetMapping("/history/{address}")
    public ResponseEntity<?> getTransactionHistory(@PathVariable String address) {
        try {
            TransactionHistoryResponse response = transactionService.getTransactionHistory(address);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Failed to get transaction history: " + e.getMessage()));
        }
    }

    public record GasEstimateRequest(
        String from,
        String to,
        BigDecimal value,
        String data
    ) {}
}