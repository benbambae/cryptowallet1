package com.wallet.web3_wallet_backend.service;

import com.wallet.web3_wallet_backend.model.DerivedKey;
import com.wallet.web3_wallet_backend.model.ExtendedKey;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;

/**
 * HD Wallet Service implementing BIP39, BIP32, and BIP44 standards.
 * Provides hierarchical deterministic wallet functionality with mnemonic generation,
 * seed derivation, master key creation, and Ethereum address generation.
 * 
 * SECURITY NOTE: Private keys are never logged to prevent exposure.
 */
@Service
public class HdWalletService {
    
    private static final String HMAC_SHA512 = "HmacSHA512";
    private static final String BITCOIN_SEED = "Bitcoin seed";
    private static final int BIP44_PURPOSE = 44;
    private static final int ETH_COIN_TYPE = 60; // Ethereum coin type
    
    private final SecureRandom secureRandom;
    private final MnemonicCode mnemonicCode;
    
    public HdWalletService() {
        this.secureRandom = new SecureRandom(); // SecureRandom is a secure random number generator
        try {
            this.mnemonicCode = MnemonicCode.INSTANCE; // Singleton instance of MnemonicCode because we dont have to create a new instance every time
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MnemonicCode", e);
        }
    }
    
    /**
     * Generates a BIP39 mnemonic phrase with the specified number of words.
     * 
     * @param words Number of words (12 or 24)
     * @return BIP39 mnemonic phrase as a space-separated string
     * @throws IllegalArgumentException if words is not 12 or 24
     */
    /**
     * Generates a BIP39 mnemonic phrase with the specified number of words.
     * 
     * @param words Number of words for the mnemonic phrase (must be 12 or 24).
     * @return A space-separated BIP39 mnemonic phrase.
     * @throws IllegalArgumentException if the word count is not 12 or 24.
     * @throws RuntimeException if mnemonic generation fails.
     */
    public String generateMnemonic(int words) {
        // Validate that the word count is either 12 or 24, as per BIP39 standard.
        if (words != 12 && words != 24) {
            throw new IllegalArgumentException("Word count must be 12 or 24, got: " + words);
        }
        
        try {
            // Calculate the required entropy bits for the given word count.
            // BIP39: entropyBits = words * 11 - words / 3
            // For 12 words: 128 bits; for 24 words: 256 bits.
            int entropyBits = words * 11 - words / 3;
            byte[] entropy = new byte[entropyBits / 8];
            
            // Fill the entropy array with cryptographically secure random bytes.
            secureRandom.nextBytes(entropy);
            
            // Convert entropy to mnemonic words using BIP39 wordlist.
            List<String> mnemonicWords = mnemonicCode.toMnemonic(entropy);
            
            // Join the words into a single space-separated string.
            return String.join(" ", mnemonicWords);
        } catch (MnemonicException e) {
            // Wrap and rethrow as unchecked exception for upstream handling.
            throw new RuntimeException("Failed to generate mnemonic", e);
        }
    }
    
    /**
     * Derives a 512-bit seed from a BIP39 mnemonic using PBKDF2-HMAC-SHA512.
     * 
     * @param mnemonic BIP39 mnemonic phrase
     * @param passphrase Optional passphrase (can be empty string)
     * @return 512-bit (64-byte) seed
     * @throws RuntimeException if seed derivation fails
     */
    public byte[] seedFromMnemonic(String mnemonic, String passphrase) {
        if (mnemonic == null || mnemonic.trim().isEmpty()) {
            throw new IllegalArgumentException("Mnemonic cannot be null or empty");
        }
        if (passphrase == null) {
            passphrase = "";
        }
        
        try {
            // Validate mnemonic
            List<String> words = Arrays.asList(mnemonic.trim().split("\\s+"));
            mnemonicCode.check(words);
            
            return MnemonicCode.toSeed(words, passphrase);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive seed from mnemonic", e);
        }
    }
    
    /**
     * Creates a BIP32 master key (root) from a seed.
     * 
     * @param seed 512-bit seed from BIP39
     * @return ExtendedKey representing the master private key
     * @throws RuntimeException if master key generation fails
     */
    public ExtendedKey rootFromSeed(byte[] seed) {
        if (seed == null || seed.length != 64) {
            throw new IllegalArgumentException("Seed must be exactly 64 bytes (512 bits)");
        }
        
        try {
            // Create master key using HMAC-SHA512
            DeterministicKey masterKey = HDKeyDerivation.createMasterPrivateKey(seed);
            
            BigInteger privateKey = masterKey.getPrivKey();
            BigInteger publicKey = new BigInteger(1, masterKey.getPubKey());
            byte[] chainCode = masterKey.getChainCode();
            
            // Serialize to xprv/xpub format
            String xprv = masterKey.serializePrivB58(org.bitcoinj.core.NetworkParameters.fromID(org.bitcoinj.core.NetworkParameters.ID_MAINNET));
            String xpub = masterKey.serializePubB58(org.bitcoinj.core.NetworkParameters.fromID(org.bitcoinj.core.NetworkParameters.ID_MAINNET));
            
            return ExtendedKey.withPrivateKey(privateKey, publicKey, chainCode, xprv, xpub);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create master key from seed", e);
        }
    }
    
    /**
     * Derives an Ethereum key using BIP44 path: m/44'/60'/account'/change/index
     * 
     * @param root Master extended key
     * @param account Account index (usually 0)
     * @param change Change index (0 for external, 1 for internal/change addresses)
     * @param index Address index
     * @return DerivedKey with private key, public key, and Ethereum address
     * @throws RuntimeException if key derivation fails
     */
    public DerivedKey deriveEthKey(ExtendedKey root, int account, int change, int index) {
        if (root == null || !root.hasPrivateKey()) {
            throw new IllegalArgumentException("Root key must have private key components");
        }
        
        try {
            // Recreate DeterministicKey from ExtendedKey
            DeterministicKey masterKey = DeterministicKey.deserializeB58(root.xprv(), 
                org.bitcoinj.core.NetworkParameters.fromID(org.bitcoinj.core.NetworkParameters.ID_MAINNET));
            
            // BIP44 derivation path: m/44'/60'/account'/change/index
            // The apostrophe indicates hardened derivation (add 0x80000000)
            DeterministicKey purposeKey = HDKeyDerivation.deriveChildKey(masterKey, BIP44_PURPOSE | 0x80000000);
            DeterministicKey coinTypeKey = HDKeyDerivation.deriveChildKey(purposeKey, ETH_COIN_TYPE | 0x80000000);
            DeterministicKey accountKey = HDKeyDerivation.deriveChildKey(coinTypeKey, account | 0x80000000);
            DeterministicKey changeKey = HDKeyDerivation.deriveChildKey(accountKey, change);
            DeterministicKey addressKey = HDKeyDerivation.deriveChildKey(changeKey, index);
            
            BigInteger privateKey = addressKey.getPrivKey();
            BigInteger publicKey = new BigInteger(1, addressKey.getPubKey());
            
            // Generate Ethereum address from public key
            String ethereumAddress = ethAddressFromPublicKey(publicKey);
            
            // Build derivation path string
            String derivationPath = String.format("m/44'/60'/%d'/%d/%d", account, change, index);
            
            return DerivedKey.of(privateKey, publicKey, ethereumAddress, derivationPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive Ethereum key", e);
        }
    }
    
    /**
     * Derives account-level extended public key using BIP44 path: m/44'/60'/account'
     * 
     * @param root Master extended key
     * @param account Account index (usually 0)
     * @return ExtendedKey containing the account-level xpub
     * @throws RuntimeException if key derivation fails
     */
    public ExtendedKey deriveAccountXpub(ExtendedKey root, int account) {
        if (root == null || !root.hasPrivateKey()) {
            throw new IllegalArgumentException("Root key must have private key components");
        }
        
        try {
            // Recreate DeterministicKey from ExtendedKey
            DeterministicKey masterKey = DeterministicKey.deserializeB58(root.xprv(), 
                org.bitcoinj.core.NetworkParameters.fromID(org.bitcoinj.core.NetworkParameters.ID_MAINNET));
            
            // BIP44 derivation path: m/44'/60'/account'
            DeterministicKey purposeKey = HDKeyDerivation.deriveChildKey(masterKey, BIP44_PURPOSE | 0x80000000);
            DeterministicKey coinTypeKey = HDKeyDerivation.deriveChildKey(purposeKey, ETH_COIN_TYPE | 0x80000000);
            DeterministicKey accountKey = HDKeyDerivation.deriveChildKey(coinTypeKey, account | 0x80000000);
            
            BigInteger privateKey = accountKey.getPrivKey();
            BigInteger publicKey = new BigInteger(1, accountKey.getPubKey());
            byte[] chainCode = accountKey.getChainCode();
            
            // Serialize to xprv/xpub format
            String xprv = accountKey.serializePrivB58(org.bitcoinj.core.NetworkParameters.fromID(org.bitcoinj.core.NetworkParameters.ID_MAINNET));
            String xpub = accountKey.serializePubB58(org.bitcoinj.core.NetworkParameters.fromID(org.bitcoinj.core.NetworkParameters.ID_MAINNET));
            
            return ExtendedKey.withPrivateKey(privateKey, publicKey, chainCode, xprv, xpub);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive account xpub", e);
        }
    }
    
    /**
     * Generates an Ethereum address from an uncompressed public key with EIP-55 checksum.
     * 
     * @param publicKey Uncompressed public key as BigInteger
     * @return Ethereum address with 0x prefix and EIP-55 checksum
     * @throws RuntimeException if address generation fails
     */
    public String ethAddressFromPublicKey(BigInteger publicKey) {
        if (publicKey == null) {
            throw new IllegalArgumentException("Public key cannot be null");
        }
        
        try {
            // Convert to uncompressed format (remove 0x04 prefix if present)
            String pubKeyHex = publicKey.toString(16);
            if (pubKeyHex.startsWith("04")) {
                pubKeyHex = pubKeyHex.substring(2);
            }
            
            // Ensure proper length (128 hex chars = 64 bytes for uncompressed key without 04 prefix)
            if (pubKeyHex.length() > 128) {
                // If too long, take the last 128 characters (this handles the case where leading zeros were added)
                pubKeyHex = pubKeyHex.substring(pubKeyHex.length() - 128);
            } else {
                // Pad with leading zeros if necessary
                pubKeyHex = String.format("%128s", pubKeyHex).replace(' ', '0');
            }
            
            // Convert to byte array for hashing
            byte[] pubKeyBytes = Numeric.hexStringToByteArray(pubKeyHex);
            
            // Use web3j's Keys.getAddress which expects the 64-byte public key without 0x04 prefix
            // Keys.getAddress returns a byte array, so we need to convert it to hex
            byte[] addressBytes = Keys.getAddress(pubKeyBytes);
            String address = "0x" + Numeric.toHexStringNoPrefix(addressBytes);
            
            // Apply EIP-55 checksum
            return Keys.toChecksumAddress(address);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Ethereum address from public key", e);
        }
    }
}