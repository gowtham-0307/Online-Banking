# ==============================================================================
# Apex Banking Microservices - Automated End-to-End Test Suite
# Tests: Gateway Routing, Account Management, Fund Transfers, Idempotency,
#        Kafka Event Streaming, Fraud Detection Engine, and Alert Notifications.
# ==============================================================================

$baseUrl = "http://localhost:8080"
Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "   APEX BANKING PLATFORM - AUTOMATED TEST SUITE        " -ForegroundColor Cyan
Write-Host "=======================================================`n" -ForegroundColor Cyan

function Assert-Step($title, $condition) {
    if ($condition) {
        Write-Host " [PASS] $title" -ForegroundColor Green
    } else {
        Write-Host " [FAIL] $title" -ForegroundColor Red
    }
}

# 1. Health Checks
Write-Host "`n>>> [STEP 1] Checking API Gateway Health..." -ForegroundColor Yellow
try {
    $gwHealth = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
    Assert-Step "API Gateway Actuator UP" ($gwHealth.status -eq "UP")
} catch {
    Write-Host " [ERROR] Gateway is unreachable at $baseUrl. Ensure services are running." -ForegroundColor Red
    exit 1
}

# 2. Create Account 1 (Alice)
Write-Host "`n>>> [STEP 2] Creating Account 1 (Alice Smith - Checking)..." -ForegroundColor Yellow
$aliceReq = @{
    customerId = "CUST-1001"
    customerName = "Alice Smith"
    email = "alice.smith@apexbank.com"
    phoneNumber = "+1-555-0101"
    accountType = "CHECKING"
    initialDeposit = 5000.00
    currency = "USD"
} | ConvertTo-Json

$aliceResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/accounts" -Method Post -Body $aliceReq -ContentType "application/json"
$aliceAcc = $aliceResp.data.accountNumber
Assert-Step "Alice Account Created: $aliceAcc (Initial Balance: $5000)" ($aliceAcc -ne $null)

# 3. Create Account 2 (Bob)
Write-Host "`n>>> [STEP 3] Creating Account 2 (Bob Jones - Savings)..." -ForegroundColor Yellow
$bobReq = @{
    customerId = "CUST-1002"
    customerName = "Bob Jones"
    email = "bob.jones@apexbank.com"
    phoneNumber = "+1-555-0102"
    accountType = "SAVINGS"
    initialDeposit = 1200.00
    currency = "USD"
} | ConvertTo-Json

$bobResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/accounts" -Method Post -Body $bobReq -ContentType "application/json"
$bobAcc = $bobResp.data.accountNumber
Assert-Step "Bob Account Created: $bobAcc (Initial Balance: $1200)" ($bobAcc -ne $null)

# 4. Standard Fund Transfer (Alice -> Bob: $400)
Write-Host "`n>>> [STEP 4] Executing Normal Fund Transfer ($400: Alice -> Bob)..." -ForegroundColor Yellow
$idempotencyKey1 = [System.Guid]::NewGuid().ToString()
$transfer1Req = @{
    sourceAccountNumber = $aliceAcc
    targetAccountNumber = $bobAcc
    amount = 400.00
    currency = "USD"
    description = "Payment for consulting services"
    idempotencyKey = $idempotencyKey1
} | ConvertTo-Json

$tx1Resp = Invoke-RestMethod -Uri "$baseUrl/api/v1/transactions/transfer" -Method Post -Body $transfer1Req -ContentType "application/json"
$tx1Id = $tx1Resp.data.transactionId
Assert-Step "Transfer Executed Successfully (TxID: $tx1Id, Status: $($tx1Resp.data.status))" ($tx1Resp.data.status -eq "COMPLETED")

# 5. Idempotency Test (Re-send identical transfer request)
Write-Host "`n>>> [STEP 5] Testing Idempotency Protection (Re-submitting Transfer with Same Key)..." -ForegroundColor Yellow
$tx1Replay = Invoke-RestMethod -Uri "$baseUrl/api/v1/transactions/transfer" -Method Post -Body $transfer1Req -ContentType "application/json"
Assert-Step "Idempotency Handled (Same TxID: $($tx1Replay.data.transactionId))" ($tx1Replay.data.transactionId -eq $tx1Id)

# 6. Verify Updated Account Balances
Write-Host "`n>>> [STEP 6] Verifying Ledger Balances after Transfer..." -ForegroundColor Yellow
$aliceCheck = Invoke-RestMethod -Uri "$baseUrl/api/v1/accounts/$aliceAcc" -Method Get
$bobCheck = Invoke-RestMethod -Uri "$baseUrl/api/v1/accounts/$bobAcc" -Method Get
Assert-Step "Alice New Balance is $4600" ($aliceCheck.data.balance -eq 4600.0)
Assert-Step "Bob New Balance is $1600" ($bobCheck.data.balance -eq 1600.0)

# 7. High-Value Fraud Detection Test ($15,000 Transfer > $10,000 Threshold)
Write-Host "`n>>> [STEP 7] Testing Fraud Detection Engine (High-Value Transfer: $15,000)..." -ForegroundColor Yellow
# First credit Alice so balance allows transfer
$creditAlice = @{
    accountNumber = $aliceAcc
    amount = 20000.00
    referenceId = "PAYROLL-DEPOSIT"
    description = "Corporate executive bonus"
} | ConvertTo-Json
Invoke-RestMethod -Uri "$baseUrl/api/v1/accounts/credit" -Method Post -Body $creditAlice -ContentType "application/json" | Out-Null

$idempotencyKey2 = [System.Guid]::NewGuid().ToString()
$transfer2Req = @{
    sourceAccountNumber = $aliceAcc
    targetAccountNumber = $bobAcc
    amount = 15000.00
    currency = "USD"
    description = "High value international procurement"
    idempotencyKey = $idempotencyKey2
} | ConvertTo-Json

$tx2Resp = Invoke-RestMethod -Uri "$baseUrl/api/v1/transactions/transfer" -Method Post -Body $transfer2Req -ContentType "application/json"
$tx2Id = $tx2Resp.data.transactionId
Assert-Step "High Value Transfer Completed: $tx2Id" ($tx2Resp.data.status -eq "COMPLETED")

Write-Host "Waiting 3 seconds for asynchronous Kafka streaming to Fraud and Notification services..." -ForegroundColor Gray
Start-Sleep -Seconds 3

# 8. Check Fraud Detection Service
Write-Host "`n>>> [STEP 8] Querying Fraud Detection Service for Triggered Alerts..." -ForegroundColor Yellow
$fraudAlerts = Invoke-RestMethod -Uri "$baseUrl/api/v1/fraud/alerts/high-risk" -Method Get
$flagged = $fraudAlerts.data | Where-Object { $_.transactionId -eq $tx2Id }
Assert-Step "Fraud Alert Flagged in Real-time (Risk: $($flagged.riskLevel), Score: $($flagged.riskScore))" ($flagged -ne $null)

# 9. Check Notification Service Ledger
Write-Host "`n>>> [STEP 9] Querying Customer Notification Audit Ledger..." -ForegroundColor Yellow
$notifs = Invoke-RestMethod -Uri "$baseUrl/api/v1/notifications/recipient/$aliceAcc" -Method Get
Assert-Step "Customer Notifications Dispatched and Recorded ($($notifs.data.Count) messages)" ($notifs.data.Count -ge 2)

# 10. Prometheus Scrape Verification
Write-Host "`n>>> [STEP 10] Verifying Prometheus Metrics Endpoint..." -ForegroundColor Yellow
$promRaw = (Invoke-WebRequest -Uri "$baseUrl/actuator/prometheus").Content
$hasMetrics = $promRaw -match "http_server_requests_seconds"
Assert-Step "Prometheus Actuator Metrics Emitted Successfully" $hasMetrics

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "   ALL INTEGRATION & RESILIENCE TESTS COMPLETED!       " -ForegroundColor Cyan
Write-Host "=======================================================`n" -ForegroundColor Cyan
