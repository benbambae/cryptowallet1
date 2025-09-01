package com.wallet.web3_wallet_backend.api.controller;

import com.wallet.web3_wallet_backend.api.dto.*;
import com.wallet.web3_wallet_backend.model.Wallet;
import com.wallet.web3_wallet_backend.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Simple REST controller for wallet operations.
 */
@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    /**
     * Inject WalletService.
     */
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Create a new wallet.
     */
    @PostMapping("/create")
    public ResponseEntity<WalletResponse> createWallet() {
        Wallet w = walletService.createWallet();
        return ResponseEntity.ok(new WalletResponse(w.getAddress(), w.getPublicKey()));
    }

    /**
     * Import a wallet using a private key.
     */
    @PostMapping("/import")
    public ResponseEntity<?> importWallet(@RequestBody ImportWalletRequest req) {
        try {
            Wallet w = walletService.importWallet(req.privateKeyHex());
            return ResponseEntity.ok(new WalletResponse(w.getAddress(), w.getPublicKey()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get the balance for a wallet address.
     */
    @GetMapping("/{address}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String address) {
        if (!walletService.isValidAddress(address)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid address"));
        }
        var bal = walletService.getBalance(address);
        return ResponseEntity.ok(new BalanceResponse(address, bal));
    }

    /**
     * Sign a message with a wallet.
     */
    @PostMapping("/sign")
    public ResponseEntity<?> signMessage(@RequestBody SignMessageRequest req) {
        // stubbed out for now
        return ResponseEntity.ok(new SignedMessageResponse(req.address(), "0xSIGNATURE"));
    }
}
