
  -- Wallets table: stores wallet information
  CREATE TABLE wallets (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      address VARCHAR(42) UNIQUE NOT NULL,
      public_key VARCHAR(130),
      wallet_type VARCHAR(20) NOT NULL, -- 'STANDARD' or 'HD'
      encrypted_private_key TEXT,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  );

  CREATE INDEX idx_wallets_address ON wallets(address);
  CREATE INDEX idx_wallets_created_at ON wallets(created_at);

  -- Transactions table: stores transaction history
  CREATE TABLE transactions (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      tx_hash VARCHAR(66) UNIQUE NOT NULL,
      from_address VARCHAR(42) NOT NULL,
      to_address VARCHAR(42) NOT NULL,
      value DECIMAL(36, 18) NOT NULL,
      gas_limit BIGINT,
      gas_price DECIMAL(36, 18),
      gas_used BIGINT,
      nonce BIGINT,
      status VARCHAR(20) NOT NULL, -- 'PENDING', 'CONFIRMING', 'CONFIRMED', 'FAILED', 'DROPPED'
      block_number BIGINT,
      confirmations INTEGER DEFAULT 0,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  );

  CREATE INDEX idx_transactions_hash ON transactions(tx_hash);
  CREATE INDEX idx_transactions_from ON transactions(from_address);
  CREATE INDEX idx_transactions_to ON transactions(to_address);
  CREATE INDEX idx_transactions_status ON transactions(status);
  CREATE INDEX idx_transactions_created_at ON transactions(created_at);

  -- HD Wallets table: stores HD wallet metadata
  CREATE TABLE hd_wallets (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
      mnemonic_encrypted TEXT,
      xpub TEXT,
      derivation_path VARCHAR(100),
      account_index INTEGER DEFAULT 0,
      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
  );

  CREATE INDEX idx_hd_wallets_wallet_id ON hd_wallets(wallet_id);