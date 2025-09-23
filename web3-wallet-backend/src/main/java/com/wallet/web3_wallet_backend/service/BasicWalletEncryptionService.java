package com.wallet.web3_wallet_backend.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Basic implementation of WalletEncryptionService using AES-GCM encryption with PBKDF2.
 * Uses in-memory storage - in production, this should be replaced with persistent storage
 * or HSM integration.
 * 
 * SECURITY NOTE: This is a basic implementation for demonstration. In production:
 * - Use proper key management (HSM/KMS)
 * - Use persistent encrypted storage
 * - Implement proper audit logging
 * - Consider using key wrapping instead of password-based encryption
 */
@Service
public class BasicWalletEncryptionService implements WalletEncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 32; // bytes
    
    // In-memory storage - replace with persistent storage in production
    private final Map<String, EncryptedWallet> storage = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Encrypted wallet data container.
     */
    private record EncryptedWallet(
        String walletId,
        byte[] encryptedSeed,
        byte[] encryptedXprv,
        byte[] salt,
        byte[] iv
    ) {}
    
    @Override
    public String encryptAndStore(String walletId, byte[] seed, String xprv, String password) {
        if (walletId == null || seed == null || xprv == null || password == null) {
            throw new IllegalArgumentException("All parameters are required");
        }
        
        try {
            // Generate random salt and IV
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(salt);
            secureRandom.nextBytes(iv);
            
            // Derive key from password using PBKDF2
            SecretKey key = deriveKey(password, salt);
            
            // Encrypt seed and xprv
            byte[] encryptedSeed = encrypt(seed, key, iv);
            byte[] encryptedXprv = encrypt(xprv.getBytes(), key, iv);
            
            // Generate storage key (could be improved with UUID or other schemes)
            String storageKey = walletId + "_" + System.currentTimeMillis();
            
            // Store encrypted data
            EncryptedWallet encryptedWallet = new EncryptedWallet(
                walletId, encryptedSeed, encryptedXprv, salt, iv
            );
            storage.put(storageKey, encryptedWallet);
            
            return storageKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt and store wallet", e);
        }
    }
    
    @Override
    public byte[] retrieveAndDecryptSeed(String storageKey, String password) {
        EncryptedWallet encryptedWallet = storage.get(storageKey);
        if (encryptedWallet == null) {
            throw new RuntimeException("Wallet not found: " + storageKey);
        }
        
        try {
            SecretKey key = deriveKey(password, encryptedWallet.salt());
            return decrypt(encryptedWallet.encryptedSeed(), key, encryptedWallet.iv());
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt seed", e);
        }
    }
    
    @Override
    public String retrieveAndDecryptXprv(String storageKey, String password) {
        EncryptedWallet encryptedWallet = storage.get(storageKey);
        if (encryptedWallet == null) {
            throw new RuntimeException("Wallet not found: " + storageKey);
        }
        
        try {
            SecretKey key = deriveKey(password, encryptedWallet.salt());
            byte[] decryptedBytes = decrypt(encryptedWallet.encryptedXprv(), key, encryptedWallet.iv());
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt xprv", e);
        }
    }
    
    @Override
    public boolean exists(String storageKey) {
        return storage.containsKey(storageKey);
    }
    
    @Override
    public void remove(String storageKey) {
        EncryptedWallet removed = storage.remove(storageKey);
        if (removed == null) {
            throw new RuntimeException("Wallet not found: " + storageKey);
        }
    }
    
    /**
     * Derives a secret key from password using PBKDF2.
     */
    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        spec.clearPassword(); // Clear password from memory
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * Encrypts data using AES-GCM.
     */
    private byte[] encrypt(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
        return cipher.doFinal(data);
    }
    
    /**
     * Decrypts data using AES-GCM.
     */
    private byte[] decrypt(byte[] encryptedData, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
        return cipher.doFinal(encryptedData);
    }
}