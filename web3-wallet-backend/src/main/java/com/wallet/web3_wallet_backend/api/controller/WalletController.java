package com.wallet.web3_wallet_backend.api.controller;

import com.wallet.web3_wallet_backend.api.dto.*;
import com.wallet.web3_wallet_backend.model.Wallet;
import com.wallet.web3_wallet_backend.model.DerivedKey;
import com.wallet.web3_wallet_backend.model.ExtendedKey;
import com.wallet.web3_wallet_backend.service.WalletService;
import com.wallet.web3_wallet_backend.service.HdWalletService;
import com.wallet.web3_wallet_backend.service.WalletEncryptionService;
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
 * REST controller for wallet operations including HD wallet functionality.
 */
@RestController
@Validated
@Tag(name = "Wallet", description = "Wallet operations including HD wallet support")
public class WalletController {

    private final WalletService walletService;
    private final HdWalletService hdWalletService;
    private final WalletEncryptionService encryptionService;

    /**
     * Inject WalletService, HdWalletService, and WalletEncryptionService.
     */
    public WalletController(WalletService walletService, HdWalletService hdWalletService, 
                           WalletEncryptionService encryptionService) {
        this.walletService = walletService;
        this.hdWalletService = hdWalletService;
        this.encryptionService = encryptionService;
    }

    // ===== Traditional Wallet Endpoints =====
    
    /**
     * Create a new wallet.
     */
    @PostMapping("/api/v1/wallet/create")
    @Operation(summary = "Create a new wallet", description = "Creates a new wallet with randomly generated keys")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Wallet created successfully")
    })
    public ResponseEntity<WalletResponse> createWallet() {
        Wallet w = walletService.createWallet();
        return ResponseEntity.ok(new WalletResponse(w.getAddress(), w.getPublicKey()));
    }
    
    // ===== HD Wallet Endpoints =====
    
    /**
     * Create HD wallet with mnemonic generation.
     */
    @PostMapping("/api/wallets")
    @Operation(summary = "Create HD wallet", description = "Creates a new HD wallet with BIP39 mnemonic and returns first address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "HD wallet created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<?> createHdWallet(
            @Parameter(description = "Number of words for mnemonic (12 or 24)", example = "12")
            @RequestParam(defaultValue = "12") int words,
            @Parameter(description = "Whether to store the wallet securely", example = "false")
            @RequestParam(defaultValue = "false") boolean store) {
        try {
            // Validate word count
            if (words != 12 && words != 24) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Word count must be 12 or 24, got: " + words));
            }
            
            // Generate mnemonic and derive first address
            String mnemonic = hdWalletService.generateMnemonic(words);
            byte[] seed = hdWalletService.seedFromMnemonic(mnemonic, "");
            ExtendedKey root = hdWalletService.rootFromSeed(seed);
            DerivedKey firstKey = hdWalletService.deriveEthKey(root, 0, 0, 0);
            
            // If store=true, encrypt and store the seed/xprv
            if (store) {
                try {
                    // Generate a secure random password for encryption (in production, this could be user-provided)
                    String encryptionPassword = java.util.UUID.randomUUID().toString();
                    String walletId = "hd_wallet_" + System.currentTimeMillis();
                    
                    String storageKey = encryptionService.encryptAndStore(walletId, seed, root.xprv(), encryptionPassword);
                    System.out.println("[INFO] Wallet encrypted and stored with key: " + storageKey);
                    
                    // In production, you might want to return the storage key and password securely
                    // For now, just log that storage was successful (don't log sensitive data!)
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to store wallet: " + e.getMessage());
                    // Continue without failing the request - storage is optional
                }
            }
            
            return ResponseEntity.ok(new CreateWalletResponse(mnemonic, firstKey.ethereumAddress()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to create HD wallet: " + e.getMessage()));
        }
    }
    
    /**
     * Derive key from mnemonic using BIP44 path.
     */
    @PostMapping("/api/wallets/derive")
    @Operation(summary = "Derive key from mnemonic", description = "Derives a key from mnemonic using BIP44 derivation path")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Key derived successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid mnemonic or parameters")
    })
    public ResponseEntity<?> deriveKey(@Valid @RequestBody DeriveKeyRequest request) {
        try {
            // Validate mnemonic word count
            String[] words = request.mnemonic().trim().split("\\s+");
            if (words.length != 12 && words.length != 24) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Mnemonic must have 12 or 24 words, got: " + words.length));
            }
            
            // Derive key
            byte[] seed = hdWalletService.seedFromMnemonic(request.mnemonic(), "");
            ExtendedKey root = hdWalletService.rootFromSeed(seed);
            DerivedKey derivedKey = hdWalletService.deriveEthKey(root, request.account(), request.change(), request.index());
            
            return ResponseEntity.ok(new DeriveKeyResponse(
                request.index(), 
                derivedKey.ethereumAddress(), 
                derivedKey.derivationPath()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to derive key: " + e.getMessage()));
        }
    }
    
    /**
     * Get extended public key from mnemonic.
     */
    @PostMapping("/api/wallets/xpub")
    @Operation(summary = "Get extended public key", description = "Gets the extended public key (xpub) for the specified account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Extended public key retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid mnemonic or parameters")
    })
    public ResponseEntity<?> getXpub(@Valid @RequestBody XpubRequest request) {
        try {
            // Validate mnemonic word count
            String[] words = request.mnemonic().trim().split("\\s+");
            if (words.length != 12 && words.length != 24) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Mnemonic must have 12 or 24 words, got: " + words.length));
            }
            
            // Derive account-level xpub (m/44'/60'/account')
            byte[] seed = hdWalletService.seedFromMnemonic(request.mnemonic(), "");
            ExtendedKey root = hdWalletService.rootFromSeed(seed);
            ExtendedKey accountKey = hdWalletService.deriveAccountXpub(root, request.account());
            
            return ResponseEntity.ok(new XpubResponse(accountKey.xpub()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Failed to get xpub: " + e.getMessage()));
        }
    }

    /**
     * Import a wallet using a private key.
     */
    @PostMapping("/api/v1/wallet/import")
    @Operation(summary = "Import wallet", description = "Import a wallet from a private key")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Wallet imported successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid private key")
    })
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
    @GetMapping("/api/v1/wallet/{address}/balance")
    @Operation(summary = "Get wallet balance", description = "Get the ETH balance for a wallet address")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid address")
    })
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
    @PostMapping("/api/v1/wallet/sign")
    @Operation(summary = "Sign message", description = "Sign a message with a wallet (not implemented for security)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "501", description = "Not implemented for security reasons")
    })
    public ResponseEntity<?> signMessage(@RequestBody SignMessageRequest req) {
        return ResponseEntity.status(501).body(new ErrorResponse("Message signing endpoint is not implemented; do not send private keys to the backend."));
    }
}
