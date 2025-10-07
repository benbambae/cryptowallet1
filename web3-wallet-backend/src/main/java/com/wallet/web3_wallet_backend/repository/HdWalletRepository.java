package com.wallet.web3_wallet_backend.repository;

import com.wallet.web3_wallet_backend.model.HdWalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HdWalletRepository extends JpaRepository<HdWalletEntity, UUID> {

    /**
     * Find HD wallet metadata by the associated wallet ID.
     * @param walletId The ID of the parent wallet
     * @return Optional containing the HD wallet metadata if found
     */
    Optional<HdWalletEntity> findByWalletId(UUID walletId);
}