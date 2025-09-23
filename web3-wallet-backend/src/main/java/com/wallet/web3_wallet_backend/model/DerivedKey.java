package com.wallet.web3_wallet_backend.model;

import java.math.BigInteger;

/**
 * Represents a derived key from BIP44 hierarchical deterministic wallet derivation.
 * Contains the private key, public key, and the corresponding Ethereum address.
 */
public record DerivedKey(
        BigInteger privateKey,      // Private key as BigInteger
        BigInteger publicKey,       // Uncompressed public key as BigInteger
        String ethereumAddress,     // Ethereum address with EIP-55 checksum
        String derivationPath       // BIP44 derivation path (e.g., m/44'/60'/0'/0/0)
) {
    
    /**
     * Creates a DerivedKey with all components.
     */
    public static DerivedKey of(BigInteger privateKey, BigInteger publicKey, 
                               String ethereumAddress, String derivationPath) {
        return new DerivedKey(privateKey, publicKey, ethereumAddress, derivationPath);
    }
    
    /**
     * Gets the private key in hex format without 0x prefix.
     * Always returns 64 characters (32 bytes) with leading zeros if necessary.
     */
    public String getPrivateKeyHex() {
        String hex = privateKey.toString(16);
        return String.format("%64s", hex).replace(' ', '0');
    }
    
    /**
     * Gets the private key in hex format with 0x prefix.
     */
    public String getPrivateKeyHexWith0x() {
        return "0x" + getPrivateKeyHex();
    }
    
    /**
     * Gets the uncompressed public key in hex format without 0x prefix.
     * The uncompressed public key is 65 bytes (130 hex chars) starting with 04.
     */
    public String getPublicKeyHex() {
        String hex = publicKey.toString(16);
        // Ensure it starts with 04 for uncompressed format and is properly padded
        if (!hex.startsWith("04")) {
            hex = "04" + String.format("%128s", hex).replace(' ', '0');
        }
        return hex;
    }
    
    /**
     * Gets the uncompressed public key in hex format with 0x prefix.
     */
    public String getPublicKeyHexWith0x() {
        return "0x" + getPublicKeyHex();
    }
    
    /**
     * Returns the Ethereum address (already includes 0x prefix and EIP-55 checksum).
     */
    public String getAddress() {
        return ethereumAddress;
    }
    
    /**
     * Returns the BIP44 derivation path used to generate this key.
     */
    public String getPath() {
        return derivationPath;
    }
}