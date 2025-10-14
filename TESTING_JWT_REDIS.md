# JWT Authentication + Redis Caching - Testing Guide

## Overview
This guide covers testing the newly implemented JWT authentication and Redis caching features.

---

## Prerequisites

1. **Install Redis** (if not already installed):
```bash
# macOS
brew install redis

# Start Redis
redis-server

# Or run Redis in Docker
docker run -d -p 6379:6379 redis:7-alpine
```

2. **Build the project**:
```bash
cd web3-wallet-backend
./mvnw clean install
```

---

## Testing Steps

### 1. Start the Application

```bash
./mvnw spring-boot:run
```

The application should start successfully with:
- JWT authentication enabled
- Redis caching configured
- Database migrations applied (users table created)

---

### 2. Test User Registration

**Endpoint:** `POST /api/v1/auth/register`

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Expected Response:**
```json
{
  "message": "User registered successfully: testuser"
}
```

---

### 3. Test User Login

**Endpoint:** `POST /api/v1/auth/login`

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "testuser",
  "email": "test@example.com"
}
```

**Copy the token** - you'll need it for authenticated requests!

---

### 4. Test Protected Endpoints (JWT Authentication)

Replace `YOUR_JWT_TOKEN` with the token from step 3.

#### Test Wallet Creation (Protected)
```bash
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "random"
  }'
```

#### Test Without Token (Should Fail)
```bash
curl -X POST http://localhost:8080/api/v1/wallets \
  -H "Content-Type: application/json" \
  -d '{
    "type": "random"
  }'
```

**Expected:** HTTP 401 Unauthorized

---

### 5. Test Redis Caching

#### Test Token Balance Caching

**First Request** (will hit blockchain RPC):
```bash
curl -X GET "http://localhost:8080/api/v1/tokens/balance?address=0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7&contract=0xdAC17F958D2ee523a2206206994597C13D831ec7" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Second Request** (should be faster, served from Redis cache):
```bash
# Run the same request again immediately
curl -X GET "http://localhost:8080/api/v1/tokens/balance?address=0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7&contract=0xdAC17F958D2ee523a2206206994597C13D831ec7" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Notice:** Second request should complete much faster (cached result).

---

#### Test Gas Price Caching

```bash
# First request (hits RPC)
curl -X GET "http://localhost:8080/api/v1/transactions/gas-estimate?from=0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7&to=0x5aAeb6053f3E94C9b9A09f33669435E7Ef1BeAed&value=0.1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Second request (cached for 30 seconds)
curl -X GET "http://localhost:8080/api/v1/transactions/gas-estimate?from=0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7&to=0x5aAeb6053f3E94C9b9A09f33669435E7Ef1BeAed&value=0.1" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

### 6. Verify Redis Cache Content

```bash
# Connect to Redis CLI
redis-cli

# List all keys
KEYS *

# Check specific cache entries
GET "tokenBalance::0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7:0xdAC17F958D2ee523a2206206994597C13D831ec7"
GET "gasPrices::legacy"
GET "gasPrices::eip1559"
GET "tokenInfo::0xdAC17F958D2ee523a2206206994597C13D831ec7"

# Check cache TTL (time to live)
TTL "tokenBalance::0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7:0xdAC17F958D2ee523a2206206994597C13D831ec7"
```

Expected TTL values:
- `tokenBalance`: ~300 seconds (5 minutes)
- `tokenInfo`: ~300 seconds (5 minutes)
- `gasPrices`: ~30 seconds

---

### 7. Test Invalid JWT Scenarios

#### Expired Token
```bash
# Wait for token to expire (default: 1 hour) or use an expired token
curl -X GET http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer EXPIRED_TOKEN"
```

**Expected:** HTTP 401 Unauthorized

#### Malformed Token
```bash
curl -X GET http://localhost:8080/api/v1/wallets \
  -H "Authorization: Bearer invalid.token.here"
```

**Expected:** HTTP 401 Unauthorized

#### Missing Authorization Header
```bash
curl -X GET http://localhost:8080/api/v1/wallets
```

**Expected:** HTTP 401 Unauthorized

---

## Verify Implementation

### Check Application Logs

Look for these indicators in the logs:

1. **Database Migration:**
```
Flyway: Migrating schema to version "2 - create users table"
```

2. **Redis Connection:**
```
Lettuce: Trying to get CHANNEL
```

3. **JWT Token Generation:**
```
JwtTokenProvider: Generating token for user: testuser
```

4. **Cache Hits:**
```
Cache hit for key: tokenBalance::...
```

---

## Performance Comparison

### Without Redis (Before)
- Token balance query: ~500-1000ms (depends on RPC)
- Gas price estimation: ~300-800ms

### With Redis (After)
- First request: ~500-1000ms (cache miss, hits RPC)
- Subsequent requests: ~5-20ms (cache hit, no RPC call)

**Performance improvement:** 20-200x faster for cached responses!

---

## Troubleshooting

### Redis Connection Errors
```
Error: Unable to connect to Redis at localhost:6379
```

**Solution:**
```bash
# Start Redis
redis-server

# Or check if Redis is running
redis-cli ping
# Should return: PONG
```

### JWT Secret Warning
```
Warning: Using default JWT secret
```

**Solution:** Set environment variable:
```bash
export JWT_SECRET=$(echo -n "your-secure-secret-key-min-256-bits" | base64)
./mvnw spring-boot:run
```

### Database Migration Failed
```
Error: Migration V2__create_users_table.sql failed
```

**Solution:**
```bash
# Drop and recreate database
rm -rf data/wallet_dev*
./mvnw spring-boot:run
```

---

## Success Criteria

- User registration works without errors
- User login returns valid JWT token
- Protected endpoints require Bearer token
- Invalid/missing tokens return 401 Unauthorized
- Redis caching reduces response time for repeated requests
- Cache TTL expires correctly (5 minutes for balance, 30 seconds for gas)
- Token expiration works after 1 hour

---

## Next Steps

Once testing is complete, you can:

1. **Add more users** for testing multi-user scenarios
2. **Test token expiration** by changing JWT_EXPIRATION config
3. **Monitor cache hit ratio** in production
4. **Configure Redis password** for production security
5. **Set up Docker deployment** (next phase)

---

## Configuration Reference

### JWT Settings (application-dev.yml)
```yaml
app:
  jwt:
    secret: ${JWT_SECRET:default-base64-encoded-secret}
    expiration-ms: 3600000  # 1 hour
```

### Redis Settings (application-dev.yml)
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 60000
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5 minutes
```

---

**Happy Testing!**
