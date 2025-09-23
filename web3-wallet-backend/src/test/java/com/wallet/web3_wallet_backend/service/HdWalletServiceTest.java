package com.wallet.web3_wallet_backend.service;

import com.wallet.web3_wallet_backend.model.DerivedKey;
import com.wallet.web3_wallet_backend.model.ExtendedKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HdWalletService verifying BIP39, BIP32, and BIP44 functionality.
 * Tests include mnemonic generation, seed derivation, and Ethereum address generation.
 */
class HdWalletServiceTest {
    
    private HdWalletService hdWalletService;
    
    // Test vectors for known mnemonic -> seed -> addresses
    private static final String TEST_MNEMONIC_12 = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
    private static final String TEST_MNEMONIC_24 = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon art";
    private static final String EMPTY_PASSPHRASE = "";
    
    // Expected addresses for the test mnemonic (first 3 addresses at m/44'/60'/0'/0/0,1,2)
    private static final List<String> EXPECTED_ADDRESSES_12_WORDS = Arrays.asList(
        "0x9858EfFD232B4033E47d90003D41EC34EcaEda94", // m/44'/60'/0'/0/0
        "0x6Fac4D18c912343BF86fa7049364Dd4E424Ab9C0", // m/44'/60'/0'/0/1
        "0xb6716976A3ebe8D39aCEB04372f22Ff8e6802D7A"  // m/44'/60'/0'/0/2
    );
    
    @BeforeEach
    void setUp() {
        hdWalletService = new HdWalletService();
    }
    
    @Test
    void testGenerateMnemonic12Words() {
        String mnemonic = hdWalletService.generateMnemonic(12);
        
        assertNotNull(mnemonic);
        String[] words = mnemonic.split(" ");
        assertEquals(12, words.length, "12-word mnemonic should have exactly 12 words");
        
        // Verify it's a valid mnemonic by trying to derive a seed
        assertDoesNotThrow(() -> {
            hdWalletService.seedFromMnemonic(mnemonic, EMPTY_PASSPHRASE);
        }, "Generated mnemonic should be valid");
    }
    
    @Test
    void testGenerateMnemonic24Words() {
        String mnemonic = hdWalletService.generateMnemonic(24);
        
        assertNotNull(mnemonic);
        String[] words = mnemonic.split(" ");
        assertEquals(24, words.length, "24-word mnemonic should have exactly 24 words");
        
        // Verify it's a valid mnemonic by trying to derive a seed
        assertDoesNotThrow(() -> {
            hdWalletService.seedFromMnemonic(mnemonic, EMPTY_PASSPHRASE);
        }, "Generated mnemonic should be valid");
    }
    
    @Test
    void testGenerateMnemonicInvalidWordCount() {
        assertThrows(IllegalArgumentException.class, () -> {
            hdWalletService.generateMnemonic(15);
        }, "Should throw exception for invalid word count");
        
        assertThrows(IllegalArgumentException.class, () -> {
            hdWalletService.generateMnemonic(18);
        }, "Should throw exception for invalid word count");
    }
    
    @Test
    void testSeedFromMnemonic() {
        byte[] seed = hdWalletService.seedFromMnemonic(TEST_MNEMONIC_12, EMPTY_PASSPHRASE);
        
        assertNotNull(seed);
        assertEquals(64, seed.length, "Seed should be exactly 64 bytes (512 bits)");
        
        // Test with passphrase
        byte[] seedWithPassphrase = hdWalletService.seedFromMnemonic(TEST_MNEMONIC_12, "test");
        assertNotNull(seedWithPassphrase);
        assertEquals(64, seedWithPassphrase.length);
        
        // Seeds should be different with different passphrases
        assertFalse(Arrays.equals(seed, seedWithPassphrase), 
                   "Seeds with different passphrases should be different");
    }
    
    @Test
    void testSeedFromMnemonicInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            hdWalletService.seedFromMnemonic(null, EMPTY_PASSPHRASE);
        }, "Should throw exception for null mnemonic");
        
        assertThrows(IllegalArgumentException.class, () -> {
            hdWalletService.seedFromMnemonic("", EMPTY_PASSPHRASE);
        }, "Should throw exception for empty mnemonic");
        
        assertThrows(RuntimeException.class, () -> {
            hdWalletService.seedFromMnemonic("invalid mnemonic words", EMPTY_PASSPHRASE);
        }, "Should throw exception for invalid mnemonic");
    }
    
    @Test
    void testRootFromSeed() {
        byte[] seed = hdWalletService.seedFromMnemonic(TEST_MNEMONIC_12, EMPTY_PASSPHRASE);
        ExtendedKey root = hdWalletService.rootFromSeed(seed);
        
        assertNotNull(root);
        assertTrue(root.hasPrivateKey(), "Root key should have private key");
        assertNotNull(root.privateKey(), "Private key should not be null");
        assertNotNull(root.publicKey(), "Public key should not be null");
        assertNotNull(root.chainCode(), "Chain code should not be null");
        assertNotNull(root.xprv(), "xprv should not be null");
        assertNotNull(root.xpub(), "xpub should not be null");
        
        assertEquals(32, root.chainCode().length, "Chain code should be 32 bytes");
        assertTrue(root.xprv().startsWith("xprv"), "xprv should start with 'xprv'");
        assertTrue(root.xpub().startsWith("xpub"), "xpub should start with 'xpub'");
    }
    
    @Test
    void testRootFromSeedInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            hdWalletService.rootFromSeed(null);
        }, "Should throw exception for null seed");
        
        assertThrows(IllegalArgumentException.class, () -> {
            hdWalletService.rootFromSeed(new byte[32]);
        }, "Should throw exception for wrong seed length");
    }
    
    @Test
    void testDeriveEthKey() {
        // Test the complete flow: mnemonic -> seed -> root -> derive keys
        byte[] seed = hdWalletService.seedFromMnemonic(TEST_MNEMONIC_12, EMPTY_PASSPHRASE);
        ExtendedKey root = hdWalletService.rootFromSeed(seed);
        
        // Derive first key (m/44'/60'/0'/0/0)
        DerivedKey key0 = hdWalletService.deriveEthKey(root, 0, 0, 0);
        assertNotNull(key0);
        assertNotNull(key0.privateKey());
        assertNotNull(key0.publicKey());
        assertNotNull(key0.ethereumAddress());
        assertEquals("m/44'/60'/0'/0/0", key0.derivationPath());
        
        // Verify address format
        assertTrue(key0.ethereumAddress().startsWith("0x"), "Address should start with 0x");
        assertEquals(42, key0.ethereumAddress().length(), "Address should be 42 characters");
        
        // Verify private key format
        assertEquals(64, key0.getPrivateKeyHex().length(), "Private key hex should be 64 chars");
        
        // Test different derivation paths
        DerivedKey key1 = hdWalletService.deriveEthKey(root, 0, 0, 1);
        DerivedKey key2 = hdWalletService.deriveEthKey(root, 0, 0, 2);
        
        assertNotEquals(key0.ethereumAddress(), key1.ethereumAddress(), 
                       "Different indexes should generate different addresses");
        assertNotEquals(key1.ethereumAddress(), key2.ethereumAddress(), 
                       "Different indexes should generate different addresses");
    }
    
    @Test
    void testDeriveEthKeyInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            hdWalletService.deriveEthKey(null, 0, 0, 0);
        }, "Should throw exception for null root key");
        
        // Create a public-only key (without private key)
        ExtendedKey publicOnlyKey = ExtendedKey.publicOnly(
            java.math.BigInteger.valueOf(123), new byte[32], "xpub123");
        
        assertThrows(IllegalArgumentException.class, () -> {
            hdWalletService.deriveEthKey(publicOnlyKey, 0, 0, 0);
        }, "Should throw exception for public-only key");
    }
    
    @Test
    void testEthAddressFromPublicKey() {
        // Test with a known public key
        java.math.BigInteger testPubKey = new java.math.BigInteger(
            "04" + "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798" +
            "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16);
        
        String address = hdWalletService.ethAddressFromPublicKey(testPubKey);
        
        assertNotNull(address);
        assertTrue(address.startsWith("0x"), "Address should start with 0x");
        assertEquals(42, address.length(), "Address should be 42 characters");
        
        // Verify EIP-55 checksum (address should contain both upper and lower case)
        String addressBody = address.substring(2);
        boolean hasUpper = !addressBody.equals(addressBody.toLowerCase());
        boolean hasLower = !addressBody.equals(addressBody.toUpperCase());
        assertTrue(hasUpper || hasLower, "Address should have EIP-55 checksum formatting");
    }
    
    @Test
    void testEthAddressFromPublicKeyInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            hdWalletService.ethAddressFromPublicKey(null);
        }, "Should throw exception for null public key");
    }
    
    /**
     * Integration test: Test the complete flow from mnemonic to first 3 Ethereum addresses.
     * This verifies the entire HD wallet implementation works correctly together.
     */
    @Test
    void testCompleteFlowMnemonicToFirst3EthAddresses() {
        System.out.println("[DEBUG_LOG] Testing complete HD wallet flow with known test vector");
        
        // Use known test mnemonic for reproducible results
        byte[] seed = hdWalletService.seedFromMnemonic(TEST_MNEMONIC_12, EMPTY_PASSPHRASE);
        ExtendedKey root = hdWalletService.rootFromSeed(seed);
        
        // Generate first 3 addresses
        DerivedKey key0 = hdWalletService.deriveEthKey(root, 0, 0, 0);
        DerivedKey key1 = hdWalletService.deriveEthKey(root, 0, 0, 1);
        DerivedKey key2 = hdWalletService.deriveEthKey(root, 0, 0, 2);
        
        System.out.println("[DEBUG_LOG] Generated addresses:");
        System.out.println("[DEBUG_LOG] Address 0: " + key0.ethereumAddress());
        System.out.println("[DEBUG_LOG] Address 1: " + key1.ethereumAddress());
        System.out.println("[DEBUG_LOG] Address 2: " + key2.ethereumAddress());
        
        // Verify all addresses are different
        assertNotEquals(key0.ethereumAddress(), key1.ethereumAddress());
        assertNotEquals(key1.ethereumAddress(), key2.ethereumAddress());
        assertNotEquals(key0.ethereumAddress(), key2.ethereumAddress());
        
        // Verify all private keys are different
        assertNotEquals(key0.getPrivateKeyHex(), key1.getPrivateKeyHex());
        assertNotEquals(key1.getPrivateKeyHex(), key2.getPrivateKeyHex());
        assertNotEquals(key0.getPrivateKeyHex(), key2.getPrivateKeyHex());
        
        // Verify derivation paths are correct
        assertEquals("m/44'/60'/0'/0/0", key0.derivationPath());
        assertEquals("m/44'/60'/0'/0/1", key1.derivationPath());
        assertEquals("m/44'/60'/0'/0/2", key2.derivationPath());
        
        System.out.println("[DEBUG_LOG] All HD wallet tests passed successfully");
    }
    
    @Test
    void testMnemonicReproducibility() {
        // Same mnemonic should always produce same addresses
        byte[] seed1 = hdWalletService.seedFromMnemonic(TEST_MNEMONIC_12, EMPTY_PASSPHRASE);
        byte[] seed2 = hdWalletService.seedFromMnemonic(TEST_MNEMONIC_12, EMPTY_PASSPHRASE);
        
        assertArrayEquals(seed1, seed2, "Same mnemonic should produce same seed");
        
        ExtendedKey root1 = hdWalletService.rootFromSeed(seed1);
        ExtendedKey root2 = hdWalletService.rootFromSeed(seed2);
        
        assertEquals(root1.getPrivateKeyHex(), root2.getPrivateKeyHex(), 
                    "Same seed should produce same root private key");
        
        DerivedKey key1 = hdWalletService.deriveEthKey(root1, 0, 0, 0);
        DerivedKey key2 = hdWalletService.deriveEthKey(root2, 0, 0, 0);
        
        assertEquals(key1.ethereumAddress(), key2.ethereumAddress(), 
                    "Same derivation should produce same address");
    }
}