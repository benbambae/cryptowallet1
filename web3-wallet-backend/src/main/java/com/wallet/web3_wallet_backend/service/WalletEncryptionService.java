package com.wallet.web3_wallet_backend.service;

/**
 * Interface for wallet encryption and storage operations.
 * Provides a clean abstraction that can be swapped with HSM implementations later.
 */
public interface WalletEncryptionService {
    
    /**
     * Encrypts and stores wallet seed/xprv securely.
     * 
     * @param walletId Unique identifier for the wallet
     * @param seed Raw seed bytes to encrypt
     * @param xprv Extended private key to encrypt
     * @param password Password for encryption (can be user-provided or system-generated)
     * @return Storage identifier/key for later retrieval
     * @throws RuntimeException if encryption or storage fails
     */
    String encryptAndStore(String walletId, byte[] seed, String xprv, String password);
    
    /**
     * Retrieves and decrypts wallet seed.
     * 
     * @param storageKey Storage identifier returned from encryptAndStore
     * @param password Password used for encryption
     * @return Decrypted seed bytes
     * @throws RuntimeException if retrieval or decryption fails
     */
    byte[] retrieveAndDecryptSeed(String storageKey, String password);
    
    /**
     * Retrieves and decrypts extended private key.
     * 
     * @param storageKey Storage identifier returned from encryptAndStore
     * @param password Password used for encryption
     * @return Decrypted extended private key
     * @throws RuntimeException if retrieval or decryption fails
     */
    String retrieveAndDecryptXprv(String storageKey, String password);
    
    /**
     * Checks if a wallet is stored.
     * 
     * @param storageKey Storage identifier
     * @return true if wallet exists in storage, false otherwise
     */
    boolean exists(String storageKey);
    
    /**
     * Removes a wallet from storage.
     * 
     * @param storageKey Storage identifier
     * @throws RuntimeException if removal fails
     */
    void remove(String storageKey);
}