# HTTP Test Files for Web3 Wallet Backend API

This folder contains HTTP test files for testing all API endpoints of the Web3 Wallet Backend.

## Files

- `health.http` - Health check endpoint
- `wallet.http` - Wallet operations (create, import, balance, sign)
- `transaction.http` - Transaction operations (send, estimate gas, status)
- `blockchain.http` - Blockchain operations (block number)

## Authentication

All endpoints (except ping) require Basic Authentication:
- Username: `admin`
- Password: `admin`

## Usage

You can use these files with:
- IntelliJ IDEA HTTP Client
- VS Code REST Client extension
- Any HTTP client that supports .http files

## Important Notes

1. **Private Keys**: The example private keys in these files are for testing only. Never use real private keys in test files.

2. **Addresses**: Replace the example addresses with valid Ethereum addresses when testing.

3. **Ethereum Node**: Most endpoints require a running Ethereum node. The application is configured to connect to Sepolia testnet via Infura.

4. **Expected Behaviors**:
   - Ping endpoint should always work
   - Wallet create/import should work without Ethereum node
   - Balance, transaction, and blockchain endpoints require Ethereum connectivity
   - Sign message endpoint returns 501 (not implemented)

## Testing Order

1. Start with `health.http` to verify the service is running
2. Test wallet creation and import from `wallet.http`
3. Try balance retrieval (may fail without Ethereum node)
4. Test transaction operations from `transaction.http`
5. Test blockchain operations from `blockchain.http`

## Sample Valid Private Key Format

Private keys should be 64 hex characters (with or without 0x prefix):
```
0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef
```

## Sample Valid Address Format

Ethereum addresses should be 40 hex characters with 0x prefix:
```
0x742d35Cc6639C43B59123456789012345678901a
```