package com.wallet.web3_wallet_backend.repository;

  import com.wallet.web3_wallet_backend.model.TransactionEntity;
  import org.springframework.data.jpa.repository.JpaRepository;
  import org.springframework.stereotype.Repository;

  import java.util.List;
  import java.util.Optional;
  import java.util.UUID;

  @Repository
  public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

      /**
       * Find a transaction by its hash.
       * @param txHash The transaction hash (0x...)
       * @return Optional containing the transaction if found
       */
      Optional<TransactionEntity> findByTxHash(String txHash);

      /**
       * Find all transactions from a specific address.
       * @param fromAddress The sender address
       * @return List of transactions
       */
      List<TransactionEntity> findByFromAddress(String fromAddress);

      /**
       * Find all transactions to a specific address.
       * @param toAddress The recipient address
       * @return List of transactions
       */
      List<TransactionEntity> findByToAddress(String toAddress);

      /**
       * Find all transactions with a specific status.
       * @param status The transaction status (PENDING, CONFIRMED, etc.)
       * @return List of transactions
       */
      List<TransactionEntity> findByStatus(TransactionEntity.TransactionStatus status);

      /**
       * Find all transactions from or to a specific address, ordered by creation date (newest first).
       * @param fromAddress The sender address
       * @param toAddress The recipient address
       * @return List of transactions sorted by created_at descending
       */
      List<TransactionEntity> findByFromAddressOrToAddressOrderByCreatedAtDesc(String fromAddress, String toAddress);
  }