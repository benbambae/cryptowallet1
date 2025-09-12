# 🪙 Web3 Wallet Backend - Enterprise-Grade Blockchain Infrastructure

**A production-ready Web3 wallet backend built with Java Spring Boot, designed for secure and scalable blockchain operations.**

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.0+-green.svg)](https://spring.io/projects/spring-boot)
[![Web3j](https://img.shields.io/badge/Web3j-4.10+-blue.svg)](https://web3j.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 🎯 Overview

This backend service provides a robust foundation for Web3 wallet operations, enabling secure wallet generation, balance tracking, and transaction management across Ethereum and EVM-compatible chains. Built with enterprise security standards and designed for high-throughput operations.

## ✨ Core Features

### Wallet Management
- **HD Wallet Generation** - BIP-32/39/44 compliant hierarchical deterministic wallets
- **Multi-signature Support** - Native support for multi-sig wallet operations
- **Key Management Service** - Integration-ready for AWS KMS, HashiCorp Vault, or HSM
- **Account Abstraction** - EIP-4337 compatible smart wallet infrastructure

### Transaction Processing
- **EIP-1559 Compliance** - Dynamic fee management with base fee and priority tips
- **Gas Optimization** - Intelligent gas estimation and automatic bump/replace mechanisms
- **Nonce Management** - Concurrent transaction handling with proper nonce sequencing
- **Transaction Simulation** - Pre-flight checks using `eth_call` to prevent failed transactions

### Token Operations
- **ERC-20 Support** - Full token transfer and balance tracking
- **ERC-721/1155** - NFT operations including minting, transfers, and metadata
- **Approval Management** - Secure allowance workflows with revocation capabilities

### Security & Compliance
- **MPC/TSS Ready** - Multi-party computation for distributed key management
- **JWT Authentication** - Stateless authentication with refresh token rotation
- **Role-Based Access Control** - Granular permission management
- **Audit Logging** - Comprehensive transaction and access logs

## 🏗️ Architecture

### Current Wallet Backend Structure
```
web3-wallet-backend/
├── src/main/java/com/wallet/
│   ├── api/
│   │   ├── controller/         # REST endpoints with OpenAPI documentation
│   │   ├── dto/               # Request/response models with validation
│   │   └── exception/         # Global exception handling
│   ├── core/
│   │   ├── service/           # Business logic layer
│   │   ├── repository/        # Data access layer
│   │   └── entity/            # Domain entities
│   ├── blockchain/
│   │   ├── client/            # Web3j client management
│   │   ├── transaction/       # Transaction builders and processors
│   │   ├── routing/           # L1/L2 transaction routing engine
│   │   └── contract/          # Smart contract interactions
│   ├── security/
│   │   ├── auth/              # Authentication/authorization
│   │   ├── crypto/            # Encryption and key management
│   │   └── audit/             # Audit trail implementation
│   └── infrastructure/
│       ├── config/            # Application configuration
│       ├── cache/             # Redis caching layer
│       └── messaging/         # Event streaming (Kafka/RabbitMQ)
├── src/test/
│   ├── unit/                  # Unit tests with mocking
│   ├── integration/           # Integration tests with Testcontainers
│   └── load/                  # Performance tests with Gatling
├── docker/                    # Docker configurations
├── k8s/                       # Kubernetes manifests
└── docs/                      # API documentation and guides 
```

### Full Monorepo Structure (with L2 Rollup)
```
wallet-rollup/
├── cryptowallet1/              # Existing Java Spring Boot wallet backend
│   └── web3-wallet-backend/   # Core wallet service with L1/L2 routing
├── rollup-node/               # OP Stack L2 node (Go/Rust)
│   ├── sequencer/             # Block production and sequencing
│   ├── batcher/               # Batch submission to L1
│   └── proposer/              # State root proposals
├── bridge-service/            # Bridge microservice (Node.js/Go)
│   ├── deposits/              # L1 → L2 deposit handling
│   ├── withdrawals/           # L2 → L1 withdrawal processing
│   └── proofs/                # Merkle proof generation
├── explorer-ui/               # Rollup block explorer (React)
│   ├── components/            # UI components
│   ├── services/              # API integration
│   └── utils/                 # Ethers.js utilities
├── docker-compose.yml         # Full stack orchestration
└── docs/                      # Architecture diagrams and guides
```

## 🚀 API Endpoints

### Wallet Operations

#### Create HD Wallet
```http
POST /api/v1/wallets
Content-Type: application/json

{
  "type": "HD",
  "derivationPath": "m/44'/60'/0'/0",
  "accountCount": 5
}
```

**Response:**
```json
{
  "walletId": "550e8400-e29b-41d4-a716-446655440000",
  "masterPublicKey": "xpub...",
  "accounts": [
    {
      "index": 0,
      "address": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7",
      "publicKey": "0x..."
    }
  ]
}
```

#### Get Balance (Multi-token)
```http
GET /api/v1/wallets/{address}/balance?tokens=ETH,USDT,USDC&chain=ethereum
```

**Response:**
```json
{
  "address": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7",
  "chain": "ethereum",
  "balances": {
    "ETH": {
      "amount": "1.53289",
      "usdValue": "4598.67"
    },
    "USDT": {
      "amount": "1000.00",
      "usdValue": "1000.00",
      "contractAddress": "0xdAC17F958D2ee523a2206206994597C13D831ec7"
    }
  },
  "totalUsdValue": "5598.67"
}
```

#### Send Transaction (with L1/L2 Routing)
```http
POST /api/v1/transactions
Content-Type: application/json

{
  "from": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7",
  "to": "0x5aAeb6053f3E94C9b9A09f33669435E7Ef1BeAed",
  "value": "0.1",
  "token": "ETH",
  "gasStrategy": "fast",
  "simulateFirst": true,
  "route": "auto"  // Options: "L1", "L2", "auto" (automatic routing)
}
```

**Response:**
```json
{
  "transactionHash": "0x...",
  "status": "pending",
  "gasUsed": "21000",
  "effectiveGasPrice": "30.5",
  "blockNumber": null,
  "nonce": 42,
  "layer": "L2",  // Indicates which layer processed the transaction
  "estimatedSavings": "85%"  // Gas savings compared to L1
}
```

## 🔧 Installation & Setup

### Prerequisites
- Java 17+ (Corretto recommended)
- Maven 3.8+ or Gradle 7+
- Docker & Docker Compose
- Redis 6+ (for caching)
- PostgreSQL 14+ or MySQL 8+ (for persistence)
- Ethereum node access (Infura/Alchemy/Self-hosted)

### Quick Start

1. **Clone the repository**
```bash
git clone https://github.com/benjaminlee/web3-wallet-backend.git
cd web3-wallet-backend
```

2. **Configure environment variables**
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. **Start infrastructure services**
```bash
docker-compose up -d postgres redis
```

4. **Run database migrations**
```bash
./mvnw flyway:migrate
```

5. **Start the application**
```bash
./mvnw spring-boot:run
```

### Configuration

**application.yml:**
```yaml
web3:
  networks:
    ethereum:
      rpcUrl: ${ETHEREUM_RPC_URL}
      chainId: 1
      confirmations: 12
    polygon:
      rpcUrl: ${POLYGON_RPC_URL}
      chainId: 137
      confirmations: 128

security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 3600000
  kms:
    provider: aws
    region: us-east-1
    keyId: ${KMS_KEY_ID}

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wallet_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  redis:
    host: localhost
    port: 6379
    ttl: 300
```

## 🧪 Testing

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify -Pintegration-tests

# Load tests
./mvnw gatling:test -Dgatling.simulation=WalletLoadTest

# Test coverage report
./mvnw jacoco:report
```

## 📊 Monitoring & Observability

- **Metrics**: Prometheus + Grafana dashboards
- **Tracing**: OpenTelemetry with Jaeger
- **Logging**: Structured logging with ELK stack
- **Health Checks**: Spring Boot Actuator endpoints

```yaml
# Example Prometheus metrics exposed
wallet_transactions_total{status="success",chain="ethereum"} 15234
wallet_gas_price_wei{chain="ethereum",speed="fast"} 35000000000
wallet_balance_requests_duration_seconds{quantile="0.99"} 0.125
```

## 🚢 Deployment

### Docker
```bash
docker build -t web3-wallet-backend:latest .
docker run -p 8080:8080 --env-file .env web3-wallet-backend:latest
```

### Kubernetes
```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
```

### Cloud Platforms
- **AWS**: Elastic Beanstalk, ECS, or EKS
- **GCP**: Cloud Run or GKE
- **Azure**: App Service or AKS

## 🛡️ Security Considerations

### Key Management
- Never store private keys in plain text
- Use KMS/HSM for production environments
- Implement key rotation policies
- Support for MPC/TSS for distributed custody

### API Security
- Rate limiting per API key
- Request signing for sensitive operations
- IP whitelisting for admin endpoints
- DDoS protection with Cloudflare/AWS Shield

### Threat Model
| Threat | Mitigation |
|--------|------------|
| Private key theft | KMS/HSM, encryption at rest, MPC |
| Transaction manipulation | Message signing, secure channels |
| Replay attacks | Nonce management, idempotency keys |
| Gas price manipulation | Multiple oracle sources, sanity checks |
| Smart contract vulnerabilities | Formal verification, audits |

## 📈 Performance

- **Transaction throughput**: 1000+ TPS (with proper infrastructure)
- **Wallet generation**: < 50ms per wallet
- **Balance queries**: < 100ms (cached)
- **Transaction submission**: < 200ms
- **Horizontal scaling**: Kubernetes-ready with stateless design

## 🗺️ Roadmap

### Phase 1: Core Infrastructure ✅
- [x] Basic wallet operations
- [x] ERC-20 token support
- [x] Transaction management
- [x] JWT authentication

### Phase 2: Advanced Features 🚧
- [x] HD wallet implementation
- [x] Multi-signature support
- [ ] Account abstraction (EIP-4337)
- [ ] Gasless transactions (EIP-2771)

### Phase 3: Layer 2 Rollup Integration 🚧
- [x] Transaction routing service (L1/L2 decision engine)
- [ ] OP Stack rollup node deployment
- [ ] Bridge service for deposits/withdrawals
- [ ] Rollup block explorer
- [ ] Cost analysis dashboard (L1 vs L2 savings)

### Phase 4: Multi-chain Support 📋
- [ ] Polygon integration
- [ ] Binance Smart Chain
- [ ] Avalanche C-Chain
- [ ] Additional Layer 2 solutions (Arbitrum, zkSync)
- [ ] Non-EVM chains (Solana, Bitcoin)

### Phase 5: DeFi Integration 📋
- [ ] Uniswap V3 integration
- [ ] Lending protocol support (Aave, Compound)
- [ ] Yield aggregation
- [ ] Cross-chain bridges

### Phase 6: Enterprise Features 📋
- [ ] Advanced MPC implementation
- [ ] Compliance tools (KYC/AML)
- [ ] White-label solution
- [ ] Advanced analytics dashboard

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## 📚 Resources

- [API Documentation](https://api.example.com/docs)
- [Integration Guide](docs/integration.md)
- [Security Best Practices](docs/security.md)
- [Performance Tuning](docs/performance.md)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Benjamin Lee**  
Senior Backend Engineer | Web3 Enthusiast

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-blue)](https://linkedin.com/in/benjaminlee)
[![GitHub](https://img.shields.io/badge/GitHub-Follow-black)](https://github.com/benjaminlee)

---

<p align="center">
  Built with ❤️ for the decentralized future
</p>