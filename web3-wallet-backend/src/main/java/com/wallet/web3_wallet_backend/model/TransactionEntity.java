
package com.wallet.web3_wallet_backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tx_hash", nullable = false, unique = true, length = 66)
    private String txHash;

    @Column(name = "from_address", nullable = false, length = 42)
    private String fromAddress;

    @Column(name = "to_address", nullable = false, length = 42)
    private String toAddress;

    @Column(name = "\"value\"", nullable = false, precision = 36, scale = 18)
    private BigDecimal value;

    @Column(name = "gas_limit")
    private Long gasLimit;

    @Column(name = "gas_price", precision = 36, scale = 18)
    private BigDecimal gasPrice;

    @Column(name = "gas_used")
    private Long gasUsed;

    private Long nonce;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer confirmations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum TransactionStatus {
        PENDING, CONFIRMING, CONFIRMED, FAILED, DROPPED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTxHash() { return txHash; }
    public void setTxHash(String txHash) { this.txHash = txHash; }

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getToAddress() { return toAddress; }
    public void setToAddress(String toAddress) { this.toAddress = toAddress; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public Long getGasLimit() { return gasLimit; }
    public void setGasLimit(Long gasLimit) { this.gasLimit = gasLimit; }

    public BigDecimal getGasPrice() { return gasPrice; }
    public void setGasPrice(BigDecimal gasPrice) { this.gasPrice = gasPrice; }

    public Long getGasUsed() { return gasUsed; }
    public void setGasUsed(Long gasUsed) { this.gasUsed = gasUsed; }

    public Long getNonce() { return nonce; }
    public void setNonce(Long nonce) { this.nonce = nonce; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public Long getBlockNumber() { return blockNumber; }
    public void setBlockNumber(Long blockNumber) { this.blockNumber = blockNumber; }

    public Integer getConfirmations() { return confirmations; }
    public void setConfirmations(Integer confirmations) { this.confirmations = confirmations; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}