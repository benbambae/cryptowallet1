package com.wallet.web3_wallet_backend.model;

import java.math.BigInteger;

/**
 * Represents an extended key (xprv/xpub) from BIP32 hierarchical deterministic wallets.
 * Contains the private key, public key, chain code, and serialized forms.
 */
public record ExtendedKey(
        BigInteger privateKey,      // Private key as BigInteger (null for public-only keys)
        BigInteger publicKey,       // Public key as BigInteger
        byte[] chainCode,           // 32-byte chain code for key derivation
        String xprv,                // Base58 encoded extended private key (null for public-only keys)
        String xpub                 // Base58 encoded extended public key
) {
    
    /**
     * Creates an ExtendedKey with both private and public key components.
     */
    public static ExtendedKey withPrivateKey(BigInteger privateKey, BigInteger publicKey, 
                                           byte[] chainCode, String xprv, String xpub) {
        return new ExtendedKey(privateKey, publicKey, chainCode, xprv, xpub);
    }
    
    /**
     * Creates an ExtendedKey with only public key components (for watch-only wallets).
     */
    public static ExtendedKey publicOnly(BigInteger publicKey, byte[] chainCode, String xpub) {
        return new ExtendedKey(null, publicKey, chainCode, null, xpub);
    }
    
    /**
     * Checks if this extended key has private key components.
     */
    public boolean hasPrivateKey() {
        return privateKey != null && xprv != null;
    }
    
    /**
     * Gets the private key in hex format without 0x prefix.
     * Returns null if this is a public-only key.
     */
    public String getPrivateKeyHex() {
        if (privateKey == null) return null;
        String hex = privateKey.toString(16);
        // Pad with leading zeros if necessary to ensure 64 characters (32 bytes)
        return String.format("%64s", hex).replace(' ', '0');
    }
    
    /**
     * Gets the public key in hex format without 0x prefix (uncompressed format).
     * The uncompressed public key is 65 bytes (130 hex chars) starting with 0x04.
     */
    public String getPublicKeyHex() {
        String hex = publicKey.toString(16);
        // Ensure it starts with 04 for uncompressed format and is properly padded
        if (!hex.startsWith("04")) {
            hex = "04" + String.format("%128s", hex).replace(' ', '0');
        }
        return hex;
    }
}