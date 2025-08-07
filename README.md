# 🪙 Web3 Wallet Backend (Java + Spring Boot)

A secure and scalable Web3 wallet backend built using Java and Spring Boot. This backend allows users to generate Ethereum wallets, check balances, and send transactions on the Ethereum testnet, all without relying on a centralized exchange.

---

## 🚀 Features

- ✅ **Wallet Generation** (anonymous, locally generated key pairs)
- 💰 **Balance Checker** for ETH and ERC-20 tokens
- 🔐 **Secure Transaction Sender** using web3j
- 🔑 **JWT-based Authentication** (optional for user binding)
- 🧠 **Modular and Extendable** – add tokens, DEX, or multi-chain support easily
- 🧾 **(Optional)** Redis Caching and MySQL for metadata/user sessions

---

## 📦 Tech Stack

| Layer         | Technology                 |
|---------------|----------------------------|
| Language      | Java 17 (Corretto)         |
| Framework     | Spring Boot                |
| Blockchain    | Ethereum (via web3j)       |
| Database      | MySQL (optional)           |
| Caching       | Redis (optional)           |
| Security      | JWT Authentication         |
| Build Tool    | Maven or Gradle            |

---

## 📂 Project Structure

```

web3-wallet-backend/
├── src/
│   ├── main/
│   │   ├── java/com/wallet/api/
│   │   │   ├── controller/        # REST API endpoints
│   │   │   ├── service/           # Wallet logic
│   │   │   ├── util/              # Crypto helpers
│   │   │   └── model/             # DTOs and entities
│   │   └── resources/
│   │       └── application.yml    # Config file
├── pom.xml                        # Maven dependencies
└── README.md                      # Project documentation

````

---

## 📬 API Endpoints (Simplified)

### 1. `POST /api/wallet/create`
Create a new Ethereum wallet.
```json
Response:
{
  "address": "0x123...",
  "privateKey": "abc123..." // (Store this safely!)
}
````

---

### 2. `GET /api/wallet/balance?address=0x123...`

Fetch ETH (and optionally token) balance of a wallet.

```json
Response:
{
  "ETH": "0.153",
  "USDT": "50.00"
}
```

---

### 3. `POST /api/wallet/send`

Send ETH or tokens from one wallet to another.

```json
Request:
{
  "fromAddress": "0x123...",
  "privateKey": "...",
  "toAddress": "0x456...",
  "amount": "0.05",
  "token": "ETH"
}
```

---

## 🧪 How to Run

### Prerequisites:

* Java 17+
* Maven or Gradle
* Infura or Alchemy API key (for Ethereum access)

### Setup:

```bash
git clone https://github.com/yourusername/web3-wallet-backend.git
cd web3-wallet-backend
```

### Configure:

Edit `src/main/resources/application.yml`:

```yaml
web3:
  infuraUrl: "https://goerli.infura.io/v3/YOUR_PROJECT_ID"
```

### Run:

```bash
./mvnw spring-boot:run
```

---

## 🌐 Deployment Options

* Run on **localhost**
* Deploy to **Heroku**, **AWS EC2**, or **Render**
* Use **Docker** (optional)

---

## 🧠 Learnings & Relevance

This project helped me:

* Understand Ethereum’s structure (wallets, private keys, transactions)
* Work with web3j and Spring Boot
* Explore security practices (private key handling, JWT auth)
* Apply blockchain principles to real-world backend systems

✅ This project aligns with backend engineering roles at Web3 companies like **Binance**.

---

## 📌 Future Enhancements

* [ ] ERC-20 and ERC-721 (NFT) support
* [ ] Transaction history with Etherscan API
* [ ] Multi-chain support (Polygon, BSC)
* [ ] Telegram alerts for wallet activity
* [ ] DEX integration (Uniswap API)

---

## 📄 License

MIT License – free for personal or commercial use

---

## 🙋‍♂️ Author

**Benjamin Lee**