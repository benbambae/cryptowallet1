
package com.wallet.web3_wallet_backend.repository;

import com.wallet.web3_wallet_backend.model.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {

    /**
     * Find a wallet by its Ethereum address.
     * @param address The wallet address (0x...)
     * @return Optional containing the wallet if found
     */
    Optional<WalletEntity> findByAddress(String address);

    /**
     * Check if a wallet with this address already exists.
     * @param address The wallet address
     * @return true if exists, false otherwise
     */
    boolean existsByAddress(String address);
}