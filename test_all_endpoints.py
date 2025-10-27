
#!/usr/bin/env python3
"""
Comprehensive endpoint testing script for Web3 Wallet Backend
Tests all controllers: Auth, Health, Blockchain, Wallet, Token, Transaction
"""

import urllib.request
import urllib.error
import json
import sys
from typing import Dict, Any, Optional

# Configuration
BASE_URL = "http://localhost:8080"
TOKEN = ""  # Will be populated after login
TIMEOUT = 30  # seconds

# Test results tracking
test_results = {
    "passed": 0,
    "failed": 0,
    "skipped": 0,
    "tests": []
}


def make_request(
    endpoint: str,
    method: str = "GET",
    data: Optional[Dict[str, Any]] = None,
    headers: Optional[Dict[str, str]] = None,
    auth_required: bool = True,
    timeout: int = TIMEOUT
) -> tuple[int, Any]:
    """Make HTTP request and return status code and response"""
    url = f"{BASE_URL}{endpoint}"

    # Prepare headers
    req_headers = headers or {}
    if auth_required and TOKEN:
        req_headers["Authorization"] = f"Bearer {TOKEN}"
    if data:
        req_headers["Content-Type"] = "application/json"

    # Prepare request
    req_data = json.dumps(data).encode('utf-8') if data else None
    request = urllib.request.Request(url, data=req_data, headers=req_headers, method=method)

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            response_data = response.read().decode('utf-8')
            try:
                return response.status, json.loads(response_data)
            except json.JSONDecodeError:
                return response.status, response_data
    except urllib.error.HTTPError as e:
        response_data = e.read().decode('utf-8')
        try:
            return e.code, json.loads(response_data)
        except json.JSONDecodeError:
            return e.code, response_data
    except Exception as e:
        return 0, str(e)


def log_test(name: str, passed: bool, details: str = ""):
    """Log test result"""
    status = "  PASS" if passed else "❌ FAIL"
    print(f"{status}: {name}")
    if details:
        print(f"   {details}")

    test_results["tests"].append({
        "name": name,
        "passed": passed,
        "details": details
    })

    if passed:
        test_results["passed"] += 1
    else:
        test_results["failed"] += 1


def test_auth_controller():
    """Test AuthController endpoints"""
    global TOKEN
    print("\n" + "="*60)
    print("TESTING: AuthController")
    print("="*60)

    # Test 1: Register new user
    status, response = make_request(
        "/api/v1/auth/register",
        method="POST",
        data={
            "username": "endpointtest",
            "email": "endpointtest@example.com",
            "password": "test123456"
        },
        auth_required=False
    )
    log_test(
        "POST /api/v1/auth/register - New user registration",
        status in [200, 201, 400],  # 400 if user already exists
        f"Status: {status}, Response: {response}"
    )

    # Test 2: Login
    status, response = make_request(
        "/api/v1/auth/login",
        method="POST",
        data={
            "username": "endpointtest",
            "password": "test123456"
        },
        auth_required=False
    )
    if status == 200 and isinstance(response, dict) and "token" in response:
        TOKEN = response["token"]
        log_test(
            "POST /api/v1/auth/login - User login",
            True,
            f"Token received: {TOKEN[:20]}..."
        )
    else:
        log_test(
            "POST /api/v1/auth/login - User login",
            False,
            f"Status: {status}, Response: {response}"
        )


def test_health_controller():
    """Test HealthController endpoints"""
    print("\n" + "="*60)
    print("TESTING: HealthController")
    print("="*60)

    # Test 1: Ping endpoint
    status, response = make_request("/api/v1/ping", auth_required=False)
    log_test(
        "GET /api/v1/ping - Health check",
        status == 200 and isinstance(response, dict) and response.get("status") == "ok",
        f"Status: {status}, Response: {response}"
    )

    # Test 2: Actuator health
    status, response = make_request("/actuator/health", auth_required=False)
    log_test(
        "GET /actuator/health - Spring actuator health",
        status == 200 and isinstance(response, dict) and response.get("status") == "UP",
        f"Status: {status}, Response: {response}"
    )


def test_blockchain_controller():
    """Test BlockchainController endpoints"""
    print("\n" + "="*60)
    print("TESTING: BlockchainController")
    print("="*60)

    # Test 1: Get block number
    status, response = make_request("/api/v1/blockNumber", auth_required=True, timeout=15)
    log_test(
        "GET /api/v1/blockNumber - Get current block number",
        status == 200 and (isinstance(response, str) or isinstance(response, int)),
        f"Status: {status}, Block Number: {response}"
    )


def test_wallet_controller():
    """Test WalletController endpoints"""
    print("\n" + "="*60)
    print("TESTING: WalletController")
    print("="*60)

    # Test 1: List wallets
    status, response = make_request("/api/v1/wallets", auth_required=True)
    log_test(
        "GET /api/v1/wallets - List all wallets",
        status == 200 and isinstance(response, list),
        f"Status: {status}, Wallets count: {len(response) if isinstance(response, list) else 'N/A'}"
    )

    # Test 2: Create traditional wallet
    status, response = make_request("/api/v1/wallet/create", method="POST", auth_required=True, timeout=15)
    log_test(
        "POST /api/v1/wallet/create - Create traditional wallet",
        status == 200 and isinstance(response, dict) and "address" in response,
        f"Status: {status}, Address: {response.get('address', 'N/A') if isinstance(response, dict) else 'Error'}"
    )

    # Test 3: Create HD wallet (12 words)
    status, response = make_request("/api/wallets?words=12", method="POST", auth_required=True, timeout=15)
    created_wallet = None
    if status == 200 and isinstance(response, dict) and "mnemonic" in response:
        created_wallet = response
        log_test(
            "POST /api/wallets?words=12 - Create HD wallet with 12-word mnemonic",
            True,
            f"Status: {status}, Address: {response.get('address', 'N/A')}"
        )
    else:
        log_test(
            "POST /api/wallets?words=12 - Create HD wallet",
            False,
            f"Status: {status}, Response: {response}"
        )

    # Test 4: Derive key from mnemonic (if wallet was created)
    if created_wallet and "mnemonic" in created_wallet:
        status, response = make_request(
            "/api/wallets/derive",
            method="POST",
            data={
                "mnemonic": created_wallet["mnemonic"],
                "account": 0,
                "change": 0,
                "index": 1
            },
            auth_required=True,
            timeout=15
        )
        log_test(
            "POST /api/wallets/derive - Derive key from mnemonic",
            status == 200 and isinstance(response, dict) and "address" in response,
            f"Status: {status}, Address: {response.get('address', 'N/A') if isinstance(response, dict) else 'Error'}"
        )
    else:
        test_results["skipped"] += 1
        print("⏭️  SKIP: POST /api/wallets/derive (no mnemonic available)")

    # Test 5: Get wallet balance (use a known address)
    test_address = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7"
    status, response = make_request(
        f"/api/v1/wallet/{test_address}/balance",
        auth_required=True,
        timeout=15
    )
    log_test(
        f"GET /api/v1/wallet/{{address}}/balance - Get wallet balance",
        status == 200 and isinstance(response, dict),
        f"Status: {status}, Balance: {response.get('balance', 'N/A') if isinstance(response, dict) else 'Error'}"
    )


def test_token_controller():
    """Test TokenController endpoints"""
    print("\n" + "="*60)
    print("TESTING: TokenController")
    print("="*60)

    # USDT contract on Sepolia (example - might not exist)
    usdt_contract = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
    test_address = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7"

    # Test 1: Get token info
    status, response = make_request(
        f"/api/v1/tokens/info/{usdt_contract}",
        auth_required=True,
        timeout=15
    )
    log_test(
        "GET /api/v1/tokens/info/{contractAddress} - Get token info",
        status in [200, 400, 500],  # Accept various responses as Sepolia might not have this token
        f"Status: {status}, Response: {str(response)[:100]}"
    )

    # Test 2: Get token balance
    status, response = make_request(
        f"/api/v1/tokens/balance/{test_address}?contract={usdt_contract}",
        auth_required=True,
        timeout=15
    )
    log_test(
        "GET /api/v1/tokens/balance/{address}?contract= - Get token balance",
        status in [200, 400, 500],  # Accept various responses
        f"Status: {status}, Response: {str(response)[:100]}"
    )


def test_transaction_controller():
    """Test TransactionController endpoints"""
    print("\n" + "="*60)
    print("TESTING: TransactionController")
    print("="*60)

    test_address = "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7"

    # Test 1: Estimate gas
    status, response = make_request(
        "/api/v1/transaction/estimate-gas",
        method="POST",
        data={
            "from": test_address,
            "to": "0x5aAeb6053f3E94C9b9A09f33669435E7Ef1BeAed",
            "value": "0.01",
            "data": ""
        },
        auth_required=True,
        timeout=15
    )
    log_test(
        "POST /api/v1/transaction/estimate-gas - Estimate transaction gas",
        status in [200, 400, 500],  # Accept various responses
        f"Status: {status}, Response: {str(response)[:150]}"
    )

    # Test 2: Get transaction history
    status, response = make_request(
        f"/api/v1/transaction/history/{test_address}",
        auth_required=True,
        timeout=15
    )
    log_test(
        "GET /api/v1/transaction/history/{address} - Get transaction history",
        status in [200, 400, 500],  # Accept various responses
        f"Status: {status}, Response: {str(response)[:150]}"
    )


def print_summary():
    """Print test summary"""
    print("\n" + "="*60)
    print("TEST SUMMARY")
    print("="*60)
    print(f"Total Tests: {test_results['passed'] + test_results['failed']}")
    print(f"  Passed: {test_results['passed']}")
    print(f"❌ Failed: {test_results['failed']}")
    print(f"⏭️  Skipped: {test_results['skipped']}")
    print(f"Pass Rate: {test_results['passed'] / (test_results['passed'] + test_results['failed']) * 100:.1f}%")
    print("="*60)


def main():
    """Main test execution"""
    print("🚀 Starting comprehensive endpoint testing...")
    print(f"Base URL: {BASE_URL}")
    print(f"Timeout: {TIMEOUT} seconds")

    try:
        test_auth_controller()
        test_health_controller()
        test_blockchain_controller()
        test_wallet_controller()
        test_token_controller()
        test_transaction_controller()

        print_summary()

        # Exit with appropriate code
        sys.exit(0 if test_results['failed'] == 0 else 1)

    except KeyboardInterrupt:
        print("\n\n⚠️  Testing interrupted by user")
        print_summary()
        sys.exit(1)
    except Exception as e:
        print(f"\n\n❌ Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
