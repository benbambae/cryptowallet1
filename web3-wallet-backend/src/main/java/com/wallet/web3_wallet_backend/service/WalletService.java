package com.wallet.web3_wallet_backend.service;

import com.wallet.web3_wallet_backend.model.Wallet;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.web3j.utils.Convert.fromWei;
import static org.web3j.utils.Convert.Unit.ETHER;

/**
 * Service class for wallet operations such as creation, import, balance retrieval, address validation, and message signing.
 */
@Service
public class WalletService {

    /** Web3j instance for blockchain interaction. */
    private final Web3j web3j;

    /**
     * Constructs a WalletService with the provided Web3j instance.
     * @param web3j the Web3j instance to use for blockchain operations
     */
    public WalletService(Web3j web3j) {
        this.web3j = web3j;
    }

    /**
     * Create a fresh wallet (in-memory for demo). Persist securely in real use.
     * @return a new Wallet object with generated keys and address
     * @throws RuntimeException if wallet creation fails
     */
    public Wallet createWallet() {
        try {
            ECKeyPair keyPair = Keys.createEcKeyPair();
            String privateKeyHex = Numeric.toHexStringNoPrefix(keyPair.getPrivateKey());
            String publicKeyHex = Numeric.toHexStringNoPrefix(keyPair.getPublicKey());
            String address = "0x" + Keys.getAddress(keyPair.getPublicKey());

            Wallet wallet = new Wallet(address, publicKeyHex, privateKeyHex, BigDecimal.ZERO);
            // Note: Balance fetching removed to avoid Ethereum node dependency during wallet creation
            // Balance can be fetched separately via getBalance() when needed
            return wallet;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create wallet", e);
        }
    }

    /**
     * Import a wallet from a raw private key hex (0x-prefixed or not).
     * @param privateKeyHex the private key in hex format
     * @return a Wallet object corresponding to the imported private key
     * @throws IllegalArgumentException if the private key is invalid
     */
    public Wallet importWallet(String privateKeyHex) {
        try {
            String normalized = normalizeHex(privateKeyHex);
            BigInteger pk = Numeric.toBigIntNoPrefix(normalized);
            ECKeyPair keyPair = ECKeyPair.create(pk);
            String publicKeyHex = Numeric.toHexStringNoPrefix(keyPair.getPublicKey());
            String address = "0x" + Keys.getAddress(keyPair.getPublicKey());

            Wallet wallet = new Wallet(address, publicKeyHex, add0x(normalized), BigDecimal.ZERO);
            // Note: Balance fetching removed to avoid Ethereum node dependency during wallet import
            // Balance can be fetched separately via getBalance() when needed
            return wallet;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid private key", e);
        }
    }

    /**
     * Get ETH/EVM coin balance (native token) in ETHER as BigDecimal.
     * @param address the wallet address to query
     * @return the balance in Ether as BigDecimal
     * @throws RuntimeException if balance retrieval fails
     */
    public BigDecimal getBalance(String address) {
        try {
            var resp = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            if (resp.hasError()) {
                throw new RuntimeException(resp.getError().getMessage());
            }
            BigInteger wei = resp.getBalance();
            return fromWei(new BigDecimal(wei), ETHER);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch balance for " + address, e);
        }
    }

    /**
     * Validate if an address is a proper 20-byte hex with 0x prefix.
     * @param address the address string to validate
     * @return true if the address is valid, false otherwise
     */
    public boolean isValidAddress(String address) {
        if (address == null) return false;
        String a = address.trim();
        if (!a.startsWith("0x") || a.length() != 42) return false;
        // basic hex check
        String body = a.substring(2);
        return body.matches("(?i)[0-9a-f]{40}");
    }

    /**
     * Sign a message with a wallet's private key (EIP-191 basic personal sign).
     * @param address the wallet address to sign with
     * @param message the message to sign
     * @param privateKeyHex the private key in hex format
     * @return the signature as a hex string
     * @throws RuntimeException if signing fails or the private key does not match the address
     */
    public String signMessage(String address, String message, String privateKeyHex) {
        try {
            if (!isValidAddress(address)) {
                throw new IllegalArgumentException("Invalid address");
            }
            String normalized = normalizeHex(privateKeyHex);
            Credentials creds = Credentials.create(normalized);
            String derived = "0x" + Keys.getAddress(creds.getEcKeyPair().getPublicKey());
            if (!address.equalsIgnoreCase(derived)) {
                throw new IllegalArgumentException("Private key does not match address");
            }
            // Personal sign prefix
            byte[] msg = prefixedMessage(message);
            var sig = org.web3j.crypto.Sign.signMessage(msg, creds.getEcKeyPair(), false);
            return sigToHex(sig);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign message", e);
        }
    }

    // --- helpers ---

    /**
     * Normalize a hex string by removing the 0x prefix and converting to lowercase.
     * @param hex the hex string to normalize
     * @return the normalized hex string (no 0x prefix, lowercase)
     * @throws IllegalArgumentException if the input is null
     */
    private static String normalizeHex(String hex) {
        if (hex == null) throw new IllegalArgumentException("Null hex");
        String h = hex.trim().toLowerCase();
        return h.startsWith("0x") ? h.substring(2) : h;
    }

    /**
     * Add a 0x prefix to a hex string if not already present.
     * @param hexNoPrefix the hex string without 0x prefix
     * @return the hex string with 0x prefix
     */
    private static String add0x(String hexNoPrefix) {
        return hexNoPrefix.startsWith("0x") ? hexNoPrefix : "0x" + hexNoPrefix;
    }

    /**
     * Prefix a message according to EIP-191 for personal signing.
     * @param message the message to prefix
     * @return the prefixed message as a byte array
     */
    private static byte[] prefixedMessage(String message) {
        byte[] msg = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String prefix = "\u0019Ethereum Signed Message:\n" + msg.length;
        byte[] p = prefix.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] out = new byte[p.length + msg.length];
        System.arraycopy(p, 0, out, 0, p.length);
        System.arraycopy(msg, 0, out, p.length, msg.length);
        return out;
    }

    /**
     * Convert a SignatureData object to a hex string (concatenated r, s, v).
     * @param sig the SignatureData object
     * @return the signature as a hex string
     */
    private static String sigToHex(org.web3j.crypto.Sign.SignatureData sig) {
        byte[] r = sig.getR();
        byte[] s = sig.getS();
        byte[] v = sig.getV(); // 27/28
        return Numeric.toHexStringNoPrefix(r) +
                Numeric.toHexStringNoPrefix(s) +
                Numeric.toHexStringNoPrefix(v);
    }
}
