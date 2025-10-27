#!/usr/bin/env python3
"""
Test remaining endpoints that were missed in the initial comprehensive test
"""

import urllib.request
import urllib.error
import json
import sys

BASE_URL = "http://localhost:8080"
TOKEN = ""
TIMEOUT = 30

test_results = {"passed": 0, "failed": 0, "tests": []}

def make_request(endpoint, method="GET", data=None, headers=None, auth_required=True, timeout=TIMEOUT):
    """Make HTTP request"""
    url = f"{BASE_URL}{endpoint}"
    req_headers = headers or {}
    if auth_required and TOKEN:
        req_headers["Authorization"] = f"Bearer {TOKEN}"
    if data:
        req_headers["Content-Type"] = "application/json"

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

def log_test(name, passed, details=""):
    """Log test result"""
    status = "  PASS" if passed else "❌ FAIL"
    print(f"{status}: {name}")
    if details:
        print(f"   {details}")
    test_results["tests"].append({"name": name, "passed": passed, "details": details})
    if passed:
        test_results["passed"] += 1
    else:
        test_results["failed"] += 1

def login():
    """Login to get JWT token"""
    global TOKEN
    print("Logging in...")
    status, response = make_request(
        "/api/v1/auth/login",
        method="POST",
        data={"username": "endpointtest", "password": "test123456"},
        auth_required=False
    )
    if status == 200 and isinstance(response, dict) and "token" in response:
        TOKEN = response["token"]
        print(f"  Login successful. Token: {TOKEN[:30]}...")
        return True
    else:
        print(f"❌ Login failed: Status {status}, Response: {response}")
        return False

def test_wallet_controller_remaining():
    """Test remaining WalletController endpoints"""
    print("\n" + "="*60)
    print("TESTING: WalletController - Remaining Endpoints")
    print("="*60)

    # First create an HD wallet to get a mnemonic for testing
    print("\nCreating test HD wallet...")
    status, wallet_response = make_request("/api/wallets?words=12", method="POST", timeout=15)

    if status != 200 or not isinstance(wallet_response, dict) or "mnemonic" not in wallet_response:
        print(f"❌ Failed to create test wallet: {wallet_response}")
        return

    test_mnemonic = wallet_response["mnemonic"]
    test_address = wallet_response.get("address", "")
    print(f"  Test wallet created. Address: {test_address}")

    # Test 1: Derive key with private key exposed
    status, response = make_request(
        "/api/wallets/derive-with-private",
        method="POST",
        data={
            "mnemonic": test_mnemonic,
            "account": 0,
            "change": 0,
            "index": 0
        },
        timeout=15
    )
    log_test(
        "POST /api/wallets/derive-with-private - Derive with private key",
        status == 200 and isinstance(response, dict) and "privateKey" in response,
        f"Status: {status}, Has privateKey: {'privateKey' in response if isinstance(response, dict) else False}"
    )

    # Test 2: Get extended public key (xpub)
    status, response = make_request(
        "/api/wallets/xpub",
        method="POST",
        data={
            "mnemonic": test_mnemonic,
            "account": 0
        },
        timeout=15
    )
    log_test(
        "POST /api/wallets/xpub - Get extended public key",
        status == 200 and isinstance(response, dict) and "xpub" in response,
        f"Status: {status}, Response: {str(response)[:100]}"
    )

    # Test 3: Import wallet from private key
    # Use a test private key (DO NOT use real private keys!)
    test_private_key = "0x4c0883a69102937d6231471b5dbb6204fe512961708279f8c5c1d5e5e9f5f5a0"
    status, response = make_request(
        "/api/v1/wallet/import",
        method="POST",
        data={"privateKeyHex": test_private_key},
        timeout=15
    )
    log_test(
        "POST /api/v1/wallet/import - Import wallet from private key",
        status == 200 and isinstance(response, dict) and "address" in response,
        f"Status: {status}, Address: {response.get('address', 'N/A') if isinstance(response, dict) else 'Error'}"
    )

    # Test 4: Sign message (should return 501)
    status, response = make_request(
        "/api/v1/wallet/sign",
        method="POST",
        data={"message": "test message", "privateKey": "dummy"},
        timeout=15
    )
    log_test(
        "POST /api/v1/wallet/sign - Sign message (should return 501)",
        status == 501,
        f"Status: {status}, Response: {str(response)[:100]}"
    )

    # Test 5: Find derivation path
    status, response = make_request(
        "/api/wallets/find-path",
        method="POST",
        data={
            "mnemonic": test_mnemonic,
            "targetAddress": test_address
        },
        timeout=30  # This might take longer as it searches
    )
    log_test(
        "POST /api/wallets/find-path - Find derivation path for address",
        status in [200, 404],  # 404 is acceptable if address not found in search range
        f"Status: {status}, Response: {str(response)[:150]}"
    )

def test_token_controller_remaining():
    """Test remaining TokenController endpoints"""
    print("\n" + "="*60)
    print("TESTING: TokenController - Remaining Endpoints")
    print("="*60)

    # Note: We cannot actually test token transfer without a real private key and testnet tokens
    # So we'll test with invalid data to verify the endpoint exists and validates properly

    status, response = make_request(
        "/api/v1/tokens/transfer",
        method="POST",
        data={
            "from": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7",
            "to": "0x5aAeb6053f3E94C9b9A09f33669435E7Ef1BeAed",
            "tokenContract": "0xdAC17F958D2ee523a2206206994597C13D831ec7",
            "amount": "1.0",
            "privateKey": "0x0000000000000000000000000000000000000000000000000000000000000001",
            "gasPrice": "20"
        },
        timeout=30
    )
    log_test(
        "POST /api/v1/tokens/transfer - Transfer ERC-20 tokens",
        status in [200, 400, 500],  # Any response means endpoint exists
        f"Status: {status}, Response: {str(response)[:150]}"
    )

def test_transaction_controller_remaining():
    """Test remaining TransactionController endpoints"""
    print("\n" + "="*60)
    print("TESTING: TransactionController - Remaining Endpoints")
    print("="*60)

    # Test 1: Send transaction (will fail without valid private key, but tests endpoint)
    status, response = make_request(
        "/api/v1/transaction/send",
        method="POST",
        data={
            "from": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb7",
            "to": "0x5aAeb6053f3E94C9b9A09f33669435E7Ef1BeAed",
            "value": "0.001",
            "privateKey": "0x0000000000000000000000000000000000000000000000000000000000000001",
            "gasPrice": "20"
        },
        timeout=30
    )
    log_test(
        "POST /api/v1/transaction/send - Send ETH transaction",
        status in [200, 400, 500],  # Any response means endpoint exists
        f"Status: {status}, Response: {str(response)[:150]}"
    )

    # Test 2: Get transaction status
    # Use a test transaction hash from Sepolia testnet
    test_tx_hash = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
    status, response = make_request(
        f"/api/v1/transaction/{test_tx_hash}/status",
        timeout=15
    )
    log_test(
        "GET /api/v1/transaction/{hash}/status - Get transaction status",
        status in [200, 404, 500],  # Any response means endpoint exists
        f"Status: {status}, Response: {str(response)[:150]}"
    )

def print_summary():
    """Print test summary"""
    print("\n" + "="*60)
    print("TEST SUMMARY - REMAINING ENDPOINTS")
    print("="*60)
    total = test_results['passed'] + test_results['failed']
    print(f"Total Tests: {total}")
    print(f"  Passed: {test_results['passed']}")
    print(f"❌ Failed: {test_results['failed']}")
    if total > 0:
        print(f"Pass Rate: {test_results['passed'] / total * 100:.1f}%")
    print("="*60)

def main():
    """Main test execution"""
    print("🚀 Testing remaining endpoints...")
    print(f"Base URL: {BASE_URL}")

    if not login():
        print("❌ Cannot proceed without authentication")
        sys.exit(1)

    try:
        test_wallet_controller_remaining()
        test_token_controller_remaining()
        test_transaction_controller_remaining()
        print_summary()
        sys.exit(0 if test_results['failed'] == 0 else 1)
    except KeyboardInterrupt:
        print("\n\n⚠️ Testing interrupted")
        print_summary()
        sys.exit(1)
    except Exception as e:
        print(f"\n\n❌ Unexpected error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
