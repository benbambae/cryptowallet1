package com.wallet.web3_wallet_backend.model;

import java.math.BigDecimal;

public class Wallet {
    private String address;
    private String publicKey;
    private String privateKey; // TODO: store encrypted or as a keystore reference
    private BigDecimal balance;

    public Wallet() {}

    public Wallet(String address, String publicKey, String privateKey, BigDecimal balance) {
        this.address = address;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.balance = balance;
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
