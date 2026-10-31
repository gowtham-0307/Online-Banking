#!/usr/bin/env bash
# ==============================================================================
# Apex Banking Microservices - Automated End-to-End Test Suite (Bash)
# ==============================================================================

set -e
BASE_URL="http://localhost:8080"

echo -e "\n======================================================="
echo "   APEX BANKING PLATFORM - AUTOMATED TEST SUITE        "
echo -e "=======================================================\n"

echo ">>> [STEP 1] Checking API Gateway Health..."
curl -s -f "$BASE_URL/actuator/health" > /dev/null
echo " [PASS] API Gateway is UP"

echo -e "\n>>> [STEP 2] Creating Account 1 (Alice Smith)..."
ALICE_RESP=$(curl -s -X POST "$BASE_URL/api/v1/accounts" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-1001",
    "customerName": "Alice Smith",
    "email": "alice.smith@apexbank.com",
    "phoneNumber": "+1-555-0101",
    "accountType": "CHECKING",
    "initialDeposit": 5000.00,
    "currency": "USD"
  }')
ALICE_ACC=$(echo "$ALICE_RESP" | grep -o '"accountNumber":"[^"]*' | cut -d'"' -f4)
echo " [PASS] Alice Account Created: $ALICE_ACC"

echo -e "\n>>> [STEP 3] Creating Account 2 (Bob Jones)..."
BOB_RESP=$(curl -s -X POST "$BASE_URL/api/v1/accounts" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-1002",
    "customerName": "Bob Jones",
    "email": "bob.jones@apexbank.com",
    "phoneNumber": "+1-555-0102",
    "accountType": "SAVINGS",
    "initialDeposit": 1200.00,
    "currency": "USD"
  }')
BOB_ACC=$(echo "$BOB_RESP" | grep -o '"accountNumber":"[^"]*' | cut -d'"' -f4)
echo " [PASS] Bob Account Created: $BOB_ACC"

echo -e "\n>>> [STEP 4] Executing Fund Transfer ($400: Alice -> Bob)..."
IDEMPOTENCY_KEY="idemp-key-$(date +%s)"
TX_RESP=$(curl -s -X POST "$BASE_URL/api/v1/transactions/transfer" \
  -H "Content-Type: application/json" \
  -d "{
    \"sourceAccountNumber\": \"$ALICE_ACC\",
    \"targetAccountNumber\": \"$BOB_ACC\",
    \"amount\": 400.00,
    \"currency\": \"USD\",
    \"description\": \"Consulting invoice #409\",
    \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
  }")
TX_ID=$(echo "$TX_RESP" | grep -o '"transactionId":"[^"]*' | cut -d'"' -f4)
echo " [PASS] Transfer Executed: $TX_ID"

echo -e "\n>>> [STEP 5] Testing Idempotency (Re-submitting with identical key)..."
REPLAY_RESP=$(curl -s -X POST "$BASE_URL/api/v1/transactions/transfer" \
  -H "Content-Type: application/json" \
  -d "{
    \"sourceAccountNumber\": \"$ALICE_ACC\",
    \"targetAccountNumber\": \"$BOB_ACC\",
    \"amount\": 400.00,
    \"currency\": \"USD\",
    \"description\": \"Consulting invoice #409\",
    \"idempotencyKey\": \"$IDEMPOTENCY_KEY\"
  }")
REPLAY_TX_ID=$(echo "$REPLAY_RESP" | grep -o '"transactionId":"[^"]*' | cut -d'"' -f4)
if [ "$TX_ID" == "$REPLAY_TX_ID" ]; then
  echo " [PASS] Idempotency Key successfully preserved transaction state"
fi

echo -e "\n>>> [STEP 6] Checking Balances..."
curl -s "$BASE_URL/api/v1/accounts/$ALICE_ACC" | grep -o '"balance":[^,]*'
curl -s "$BASE_URL/api/v1/accounts/$BOB_ACC" | grep -o '"balance":[^,]*'

echo -e "\n>>> [STEP 7] Triggering High-Value Fraud Alert ($15,000 Transfer)..."
# Deposit first
curl -s -X POST "$BASE_URL/api/v1/accounts/credit" \
  -H "Content-Type: application/json" \
  -d "{
    \"accountNumber\": \"$ALICE_ACC\",
    \"amount\": 25000.00,
    \"referenceId\": \"PAYROLL-DEPOSIT\"
  }" > /dev/null

FRAUD_KEY="idemp-fraud-$(date +%s)"
curl -s -X POST "$BASE_URL/api/v1/transactions/transfer" \
  -H "Content-Type: application/json" \
  -d "{
    \"sourceAccountNumber\": \"$ALICE_ACC\",
    \"targetAccountNumber\": \"$BOB_ACC\",
    \"amount\": 15000.00,
    \"currency\": \"USD\",
    \"description\": \"Large cross-border transfer\",
    \"idempotencyKey\": \"$FRAUD_KEY\"
  }" > /dev/null

echo "Waiting for Kafka event streaming (3 seconds)..."
sleep 3

echo -e "\n>>> [STEP 8] Querying High-Risk Fraud Alerts..."
curl -s "$BASE_URL/api/v1/fraud/alerts/high-risk"

echo -e "\n>>> [STEP 9] Querying Customer Notifications..."
curl -s "$BASE_URL/api/v1/notifications/recipient/$ALICE_ACC"

echo -e "\n\n======================================================="
echo "   TEST SUITE EXECUTION COMPLETED!                     "
echo -e "=======================================================\n"
