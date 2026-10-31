# Apex Banking Microservices: Complete Test & Deployment Guide

This document provides a step-by-step walkthrough for configuring, compiling, launching, testing, and observing the **Apex Banking Microservices Platform**. It also details all post-creation configuration changes required to adapt the project for different environments.

---

## 📑 Table of Contents
1. [Post-Creation Configuration & Customizations](#1-post-creation-configuration--customizations)
2. [Prerequisites & Environment Setup](#2-prerequisites--environment-setup)
3. [Build & Launch Instructions](#3-build--launch-instructions)
4. [Step-by-Step API Test Scenarios (curl & PowerShell)](#4-step-by-step-api-test-scenarios)
5. [Observability & Monitoring Verification](#5-observability--monitoring-verification)
6. [Architectural Deep Dive & Interview Defense](#6-architectural-deep-dive--interview-defense)

---

## 1. Post-Creation Configuration & Customizations

After generating or downloading the project folder, review and customize the following configurations according to your local machine or cloud environment:

### A. Database Settings & Passwords
The default database configurations use MySQL on port `3306` with user `apex_user` and password `apex_secret`.
- If you have an existing MySQL instance running locally on port 3306, you can either:
  1. Stop your local MySQL service: `net stop MySQL` (Windows) or `sudo systemctl stop mysql` (Linux), OR
  2. Change the host port mapping in [docker-compose.yml](file:///C:/Users/ELCOT/.gemini/antigravity/scratch/apex-banking-microservices/docker-compose.yml) from `"3306:3306"` to `"3307:3306"`, and update the `MYSQL_PORT` in your service configurations.
- To change database credentials, update:
  - `docker/mysql/init.sql` (User grant statements)
  - `docker-compose.yml` (Under `mysql`, `MYSQL_ROOT_PASSWORD`, and service environment variables)
  - Each microservice's `application.yml` under `spring.datasource`.

### B. Kafka Broker Configuration
- By default, Kafka runs in **KRaft** mode (Apache Kafka 3.7+), eliminating the need for Apache Zookeeper.
- If deploying to a managed Kafka service (e.g., Confluent Cloud, AWS MSK):
  - In `transaction-service`, `fraud-detection-service`, and `notification-service`, set the environment variable:
    ```bash
    KAFKA_BOOTSTRAP_SERVERS=your-kafka-broker-host:9092
    ```
  - For SASL/SSL authentication, add security credentials to `application.yml` under `spring.kafka.properties.security.protocol`.

### C. OpenTelemetry & Jaeger Collector URL
- Traces are exported via OTLP (OpenTelemetry Protocol) over HTTP to `http://jaeger:4318/v1/traces` inside Docker, or `http://localhost:4318/v1/traces` locally.
- If using an enterprise APM (e.g., Datadog, Dynatrace, New Relic, or AWS X-Ray), change the `OTEL_EXPORTER_OTLP_ENDPOINT` variable to point to your APM's OTLP ingest endpoint.

### D. Fraud Detection Rule Customization
- The fraud detection threshold is defined in [`FraudRuleEngine.java`](file:///C:/Users/ELCOT/.gemini/antigravity/scratch/apex-banking-microservices/fraud-detection-service/src/main/java/com/apex/fraud/service/FraudRuleEngine.java).
- Default rules:
  - Any transfer `>= $10,000.00` is flagged with a Risk Score of 65+ (HIGH RISK).
  - Any transfer `>= $50,000.00` is flagged with a Risk Score of 90+ (CRITICAL RISK).
  - You can adjust these threshold constants in `FraudRuleEngine.java` to test different compliance policies.

---

## 2. Prerequisites & Environment Setup

Verify the following tools are installed on your workstation:
1. **Java Development Kit (JDK)**: JDK 17 or higher (`java -version`).
2. **Apache Maven**: Version 3.8+ (`mvn -version`).
3. **Docker & Docker Compose**: Docker Desktop with Linux containers enabled (`docker --version`).
4. **curl** or **Postman** for REST API execution.

---

## 3. Build & Launch Instructions

### Option 1: Automated Script (One-Click)

**On Windows (PowerShell):**
```powershell
cd C:\Users\ELCOT\.gemini\antigravity\scratch\apex-banking-microservices
.\build-and-run.ps1
```

**On Linux / macOS:**
```bash
chmod +x build-and-run.sh test-api.sh
./build-and-run.sh
```

### Option 2: Manual Step-by-Step

**Step 1: Maven Build**
```bash
# Package common-dto and all 5 microservices
mvn clean package -DskipTests
```

**Step 2: Start Containers with Docker Compose**
```bash
docker compose up --build -d
```

**Step 3: Verify Running Services**
```bash
docker compose ps
```
All containers should report `Up` or `healthy`:
- `apex-mysql` (Healthy)
- `apex-kafka` (Up)
- `apex-jaeger` (Up)
- `apex-prometheus` (Up)
- `apex-grafana` (Up)
- `apex-account-service` (Up)
- `apex-transaction-service` (Up)
- `apex-fraud-service` (Up)
- `apex-notification-service` (Up)
- `apex-api-gateway` (Up)

---

## 4. Step-by-Step API Test Scenarios

All client requests are routed through the **API Gateway** on port `8080`.

### Test Case 1: Ingress Gateway Health Check
Verify the gateway and downstream routes are accessible.

```bash
curl -i http://localhost:8080/actuator/health
```
**Expected Response:**
```json
HTTP/1.1 200 OK
{"status":"UP"}
```

---

### Test Case 2: Create Checking Account for Alice Smith
Creates a bank account with initial balance of $5,000.00.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-run-001" \
  -d '{
    "customerId": "CUST-1001",
    "customerName": "Alice Smith",
    "email": "alice.smith@apexbank.com",
    "phoneNumber": "+1-555-0101",
    "accountType": "CHECKING",
    "initialDeposit": 5000.00,
    "currency": "USD"
  }'
```

**Expected Response (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Account successfully created",
  "data": {
    "accountNumber": "APX3829104812",
    "customerId": "CUST-1001",
    "customerName": "Alice Smith",
    "email": "alice.smith@apexbank.com",
    "phoneNumber": "+1-555-0101",
    "accountType": "CHECKING",
    "status": "ACTIVE",
    "balance": 5000.00,
    "currency": "USD",
    "version": 0,
    "createdAt": "2026-10-30T21:00:00Z"
  },
  "correlationId": "test-run-001"
}
```
*(Save Alice's `accountNumber` for subsequent steps)*

---

### Test Case 3: Create Savings Account for Bob Jones
Creates counterparty account with initial balance of $1,200.00.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-run-002" \
  -d '{
    "customerId": "CUST-1002",
    "customerName": "Bob Jones",
    "email": "bob.jones@apexbank.com",
    "phoneNumber": "+1-555-0102",
    "accountType": "SAVINGS",
    "initialDeposit": 1200.00,
    "currency": "USD"
  }'
```
*(Save Bob's `accountNumber`)*

---

### Test Case 4: Execute Normal Fund Transfer ($400: Alice -> Bob)
Transfers $400 from Alice's account to Bob's account with an **Idempotency Key**.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: test-run-003" \
  -d '{
    "sourceAccountNumber": "<ALICE_ACCOUNT_NUMBER>",
    "targetAccountNumber": "<BOB_ACCOUNT_NUMBER>",
    "amount": 400.00,
    "currency": "USD",
    "description": "Consulting invoice settlement",
    "idempotencyKey": "IDEMP-KEY-9901"
  }'
```

**Expected Response (HTTP 201 Created):**
```json
{
  "success": true,
  "message": "Transfer successfully executed",
  "data": {
    "transactionId": "TXN-A1B2C3D4E5F67890",
    "sourceAccountNumber": "<ALICE_ACCOUNT_NUMBER>",
    "targetAccountNumber": "<BOB_ACCOUNT_NUMBER>",
    "amount": 400.00,
    "currency": "USD",
    "type": "INTERNAL_TRANSFER",
    "status": "COMPLETED",
    "description": "Consulting invoice settlement",
    "idempotencyKey": "IDEMP-KEY-9901"
  },
  "correlationId": "test-run-003"
}
```

---

### Test Case 5: Verify Idempotency Protection (Re-sending identical transfer)
Simulate network timeout or retry: resend the identical payload with `IDEMP-KEY-9901`.

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "<ALICE_ACCOUNT_NUMBER>",
    "targetAccountNumber": "<BOB_ACCOUNT_NUMBER>",
    "amount": 400.00,
    "currency": "USD",
    "description": "Consulting invoice settlement",
    "idempotencyKey": "IDEMP-KEY-9901"
  }'
```

**Expected Behavior:**
- The Transaction Service recognizes the existing `idempotencyKey`.
- It returns the original transaction record immediately **without debiting Alice again**.
- Alice's balance remains $4,600.00 (not $4,200.00).

---

### Test Case 6: Test Insufficient Funds Edge Case
Attempt to transfer $50,000 when Alice only has $4,600.

**Request:**
```bash
curl -i -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "<ALICE_ACCOUNT_NUMBER>",
    "targetAccountNumber": "<BOB_ACCOUNT_NUMBER>",
    "amount": 50000.00,
    "currency": "USD",
    "description": "Excessive amount test",
    "idempotencyKey": "IDEMP-KEY-FAIL-01"
  }'
```

**Expected Response (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "message": "Debit failed: Insufficient balance in account APX... Current: 4600.0000, Requested: 50000.00"
}
```

---

### Test Case 7: High-Value Transfer & Fraud Detection Trigger ($15,000)
1. First, credit Alice's account with $20,000:
```bash
curl -X POST http://localhost:8080/api/v1/accounts/credit \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "<ALICE_ACCOUNT_NUMBER>",
    "amount": 20000.00,
    "referenceId": "WIRE-DEP-001",
    "description": "Corporate wire deposit"
  }'
```

2. Execute transfer of $15,000 (exceeds $10,000 threshold):
```bash
curl -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "<ALICE_ACCOUNT_NUMBER>",
    "targetAccountNumber": "<BOB_ACCOUNT_NUMBER>",
    "amount": 15000.00,
    "currency": "USD",
    "description": "Real estate down payment",
    "idempotencyKey": "IDEMP-FRAUD-001"
  }'
```

---

### Test Case 8: Query Fraud Detection Alerts
Verify that Fraud Detection Service consumed the Kafka `bank.transactions` event and flagged the high-value transfer.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/fraud/alerts/high-risk
```

**Expected Response:**
```json
{
  "success": true,
  "message": "High risk fraud alerts retrieved",
  "data": [
    {
      "alertId": "FRD-9F8E7D6C5B4A3210",
      "transactionId": "TXN-...",
      "sourceAccountNumber": "<ALICE_ACCOUNT_NUMBER>",
      "targetAccountNumber": "<BOB_ACCOUNT_NUMBER>",
      "amount": 15000.0000,
      "riskScore": 75,
      "riskLevel": "HIGH",
      "ruleTriggered": "HIGH_VALUE_EXCEEDED_10K; ROUND_HIGH_AMOUNT_ANOMALY; ",
      "notes": "Evaluated with score 75. Status: HIGH"
    }
  ]
}
```

---

### Test Case 9: Inspect Customer Notification Ledger
Verify that Notification Service consumed both the transaction event and the fraud alert event.

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/notifications/recipient/<ALICE_ACCOUNT_NUMBER>
```

**Expected Response:**
Contains both:
1. `ApexBank Alert: Transfer of 15000.00 USD Processed` via `EMAIL`
2. `URGENT SECURITY ALERT: Suspicious Activity on Account APX...` via `SMS`

---

## 5. Observability & Monitoring Verification

### A. Distributed Tracing in Jaeger (`http://localhost:16686`)
1. Open your browser and navigate to `http://localhost:16686`.
2. Under **Service**, select `api-gateway` or `transaction-service`.
3. Click **Find Traces**.
4. Click on any trace corresponding to `/api/v1/transactions/transfer`.
5. **Observe the Span Hierarchy:**
   - `api-gateway` receives HTTP request -> GlobalFilter starts span.
   - Gateway forwards request to `transaction-service`.
   - `transaction-service` calls `account-service` (REST HTTP child span).
   - `transaction-service` publishes to Kafka topic `bank.transactions` (Producer span).
   - `fraud-detection-service` and `notification-service` consume message (Consumer spans).
   - The entire distributed lifecycle is linked under the same single **TraceID**!

### B. Prometheus Metrics Explorer (`http://localhost:9090`)
1. Navigate to `http://localhost:9090`.
2. In the query box, enter:
   ```promql
   sum(apex_banking_transactions_completed_total_total)
   ```
   Click **Execute** to view total successful transfers across the platform.
3. Query P95 latency across all services:
   ```promql
   histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (le, application))
   ```

### C. Grafana Visual Dashboard (`http://localhost:3000`)
1. Navigate to `http://localhost:3000` (Default credentials: `admin` / `admin`).
2. Go to **Dashboards** -> Select **"Apex Banking - Distributed Microservices Dashboard"**.
3. The dashboard auto-refreshes every 5 seconds, displaying:
   - Completed vs Failed Transactions count.
   - Fraud Alerts Triggered (by risk level).
   - Dispatched Notifications.
   - HTTP Latency percentiles (P95).
   - JVM Heap memory consumption per container.

### D. Structured Logging with SLF4J and MDC
Inspect any service log file (or `docker logs apex-transaction-service`). Notice the structured formatting:
```text
2026-10-30 21:15:30.124 [http-nio-8082-exec-3] INFO  c.a.t.s.TransactionServiceImpl - [traceId=4bf92f3577b34da6a3ce929d0e0e4736 spanId=00f067aa0ba902b7 corrId=test-run-003] - [TRANSFER-SUCCESS] txId=TXN-1234 from=APX1001 to=APX1002 amount=400.00
```
- Every log statement contains `traceId`, `spanId`, and `corrId`.
- Search for any `corrId` in your centralized log collector to instantly find every event across all 5 services!

---

## 6. Architectural Deep Dive & Interview Defense

When discussing this project in technical interviews, focus on these real-world design decisions:

### Q1: "How did you prevent race conditions and double debits when two transfers hit the same account simultaneously?"
> **Answer:** "I implemented **JPA Optimistic Concurrency Control** in the `Account` entity using an `@Version` column. If two concurrent transactions attempt to debit the same account simultaneously, the database verifies the row version before committing. The first transaction increments the version, while the second detects a version mismatch and throws an `OptimisticLockingFailureException`. The system catches this conflict and triggers a controlled retry or returns a conflict status, ensuring zero lost updates."

### Q2: "Why did you use Apache Kafka instead of synchronous REST between Transaction and Fraud/Notification services?"
> **Answer:** "Synchronous REST creates tight runtime coupling and increases latency: the user would have to wait for fraud scoring and SMS delivery before receiving a transfer receipt. If the notification provider had a 3-second delay, the transfer API would freeze. By publishing a `TransactionCreatedEvent` to Apache Kafka in KRaft mode, the transfer completes in sub-100ms. Fraud detection and notifications consume events asynchronously and scale independently without risking payment processing throughput."

### Q3: "How is Idempotency handled in distributed payment systems?"
> **Answer:** "In network partitions or client timeouts, callers often retry payment requests. To prevent duplicate debits, the client provides an `idempotencyKey` in the request header or payload. The Transaction Service enforces a unique database constraint on `idempotency_key`. Before processing, it checks if a record with that key exists: if present, it immediately returns the existing completed transaction without executing the debit/credit logic again."

### Q4: "How does OpenTelemetry propagate traces across Kafka and HTTP boundaries?"
> **Answer:** "We use Micrometer Tracing with OpenTelemetry bridge. When the API Gateway receives a request, it initiates a trace context with a W3C `traceparent` header. When the Transaction Service calls the Account Service via `RestClient`, the HTTP client interceptor injects the current trace context into the outgoing HTTP headers. When publishing to Kafka, `TransactionProducer` injects the trace context and correlation ID into Kafka record headers. The Kafka listeners extract these headers and load them into SLF4J MDC, enabling seamless distributed tracing across both synchronous REST and asynchronous message queues."
