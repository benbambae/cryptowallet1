
package com.wallet.web3_wallet_backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hd_wallets")
public class HdWalletEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "mnemonic_encrypted", columnDefinition = "TEXT")
    private String mnemonicEncrypted;

    @Column(columnDefinition = "TEXT")
    private String xpub;

    @Column(name = "derivation_path", length = 100)
    private String derivationPath;

    @Column(name = "account_index", columnDefinition = "INTEGER DEFAULT 0")
    private Integer accountIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getWalletId() { return walletId; }
    public void setWalletId(UUID walletId) { this.walletId = walletId; }

    public String getMnemonicEncrypted() { return mnemonicEncrypted; }
    public void setMnemonicEncrypted(String mnemonicEncrypted) { this.mnemonicEncrypted = mnemonicEncrypted; }

    public String getXpub() { return xpub; }
    public void setXpub(String xpub) { this.xpub = xpub; }

    public String getDerivationPath() { return derivationPath; }
    public void setDerivationPath(String derivationPath) { this.derivationPath = derivationPath; }

    public Integer getAccountIndex() { return accountIndex; }
    public void setAccountIndex(Integer accountIndex) { this.accountIndex = accountIndex; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}