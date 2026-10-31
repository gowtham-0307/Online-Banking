# APEX BANKING MICROSERVICES PLATFORM
## Comprehensive Component Encyclopedia, Execution Manual & Operational Runbook

---

## 1. PROJECT OVERVIEW & ARCHITECTURAL TOPOLOGY

The Apex Banking Platform is an enterprise-grade, distributed core-banking system engineered with Java 17, Spring Boot 3, MySQL 8, Apache Kafka (KRaft), OpenTelemetry (OTel), Prometheus, and Grafana.

It demonstrates production banking patterns:
1. Database-per-Microservice isolation
2. Optimistic Concurrency Control (OCC) to eliminate balance race conditions
3. Distributed Idempotency Key validation to prevent duplicate debits during network retries
4. Asynchronous Event-Driven Choreography via Kafka
5. Distributed Tracing across synchronous HTTP and asynchronous messaging
6. Unified Observability (Metrics, Traces, Structured MDC Logs)

### 1.1 Service Port & Responsibility Matrix

| Component | Port | Technology | Primary Domain Responsibility |
|:---|:---:|:---|:---|
| **`api-gateway`** | 8080 | Spring Cloud Gateway, Reactive Netty, OTel | Single ingress proxy, W3C trace injection, correlation IDs, routing, circuit breaker fallbacks. |
| **`account-service`** | 8081 | Spring Boot 3, Spring Data JPA, MySQL, Micrometer | Account onboarding, KYC data, balances, ledger entries, optimistic locking (`@Version`). |
| **`transaction-service`** | 8082 | Spring Boot 3, Spring Data JPA, Spring Kafka, MySQL | Transfer orchestrator, idempotency enforcement, debit/credit coordination with rollback compensation. |
| **`fraud-detection-service`** | 8083 | Spring Boot 3, Spring Data JPA, Spring Kafka, MySQL | Real-time risk analysis engine, rules evaluation, Kafka event consumer/producer, audit log. |
| **`notification-service`** | 8084 | Spring Boot 3, Spring Data JPA, Spring Kafka, MySQL | Asynchronous alert dispatcher (Email/SMS simulation), multi-topic Kafka consumer, audit history. |
| **`common-dto`** | N/A | Java Library (Maven module) | Shared domain contracts, DTOs, Kafka events, and enums. |
| **`MySQL 8.0`** | 3306 | MySQL Community Server | Dedicated relational databases: `apex_account_db`, `apex_transaction_db`, `apex_fraud_db`, `apex_notification_db`. |
| **`Apache Kafka`** | 9092 | Kafka 3.7+ (KRaft mode) | Distributed streaming broker handling `bank.transactions` and `bank.fraud-alerts`. |
| **`Jaeger / OTel`** | 16686 / 4318 | Jaeger All-In-One | Distributed tracing UI (16686) and OTLP HTTP receiver (4318). |
| **`Prometheus`** | 9090 | Prometheus Server | Time-series scraper querying `/actuator/prometheus` on all 5 services every 5 seconds. |
| **`Grafana`** | 3000 | Grafana Open Source | Visualization engine with auto-provisioned Prometheus datasource and banking dashboard. |

---

## 2. DETAILED EXPLANATION OF EVERY COMPONENT

### Component 1: `common-dto` (Shared Domain Contracts)
- **Location:** `common-dto/`
- **Purpose:** In enterprise banking systems, domain contracts (DTOs, Enums, and Kafka Event structures) must be strictly typed and versioned to ensure producer-consumer compatibility.
- **Key Classes & Their Explanations:**
  - `AccountType.java`: Defines account classes (`CHECKING`, `SAVINGS`, `BUSINESS`, `INVESTMENT`).
  - `AccountStatus.java`: Life-cycle states (`ACTIVE`, `SUSPENDED`, `FROZEN`, `CLOSED`). Transactions are blocked if status is not `ACTIVE`.
  - `TransactionStatus.java`: States (`PENDING`, `COMPLETED`, `FAILED`, `REJECTED_INSUFFICIENT_FUNDS`, `REJECTED_FRAUD_DETECTED`).
  - `TransactionCreatedEvent.java`: Immutable domain event emitted after a transaction is committed. Carries transaction ID, source/target accounts, amount, currency, timestamp, and correlation ID.
  - `FraudAlertEvent.java`: Emitted by Fraud Service when a suspicious transfer is flagged. Carries risk score, risk level, rules triggered, and notes.
  - `TransferRequest.java`: DTO containing `sourceAccountNumber`, `targetAccountNumber`, `amount`, `currency`, and `idempotencyKey`.

---

### Component 2: `api-gateway` (Edge Service & Router)
- **Location:** `api-gateway/` (Port: 8080)
- **Purpose:** Acts as the reverse proxy for all external client traffic. Hides internal topology and standardizes cross-cutting concerns (correlation tracking, security, latency recording).
- **Core Files & Logic:**
  - `LoggingAndTracingFilter.java`:
    - Intercepts every inbound HTTP request.
    - Inspects the `X-Correlation-ID` header. If missing, generates a unique UUID.
    - Mutates the downstream request to inject the correlation ID.
    - Adds `X-Correlation-ID` to the outgoing response headers for client tracking.
    - Captures high-precision execution time (ms) and logs ingress/egress records with HTTP status.
  - `application.yml`:
    - Declares routes mapping `/api/v1/accounts/**` -> `account-service:8081`
    - `/api/v1/transactions/**` -> `transaction-service:8082`
    - `/api/v1/fraud/**` -> `fraud-detection-service:8083`
    - `/api/v1/notifications/**` -> `notification-service:8084`
  - `FallbackController.java`:
    - Returns graceful `HTTP 503 SERVICE UNAVAILABLE` envelopes if downstream services fail.

---

### Component 3: `account-service` (Core Ledger & Balances)
- **Location:** `account-service/` (Port: 8081)
- **Database:** `apex_account_db`
- **Purpose:** Manages the customer balance ledger. Financial systems cannot tolerate lost updates or race conditions.
- **Core Files & Logic:**
  - `BankAccount.java`:
    - Maps the `bank_accounts` table.
    - Features `@Version private Long version;`: **Optimistic Concurrency Control**. When two requests attempt to debit an account concurrently, the first committer increments the version. The second transaction encounters a version mismatch and triggers an `ObjectOptimisticLockingFailureException`, preventing double-spending without expensive table-level database locks.
  - `AccountAuditLog.java`:
    - Immutable ledger recording balance before, balance after, amount, action type (`ACCOUNT_CREATED`, `DEBIT`, `CREDIT`), and reference ID for regulatory audit trails.
  - `AccountServiceImpl.java`:
    - `debitAccount()`: Validates that status is `ACTIVE` and current balance >= requested debit amount. If insufficient, throws `InsufficientFundsException`. Updates balance and writes audit record.
    - `creditAccount()`: Validates account status, credits amount, updates balance, and writes audit record.
  - `GlobalExceptionHandler.java`:
    - Intercepts exceptions and formats them into clean `ApiResponse<T>` envelopes with HTTP status codes (400 for insufficient funds, 404 for missing accounts, 409 for concurrency conflicts).

---

### Component 4: `transaction-service` (Transfer Orchestrator)
- **Location:** `transaction-service/` (Port: 8082)
- **Database:** `apex_transaction_db`
- **Purpose:** Coordinates money movements, enforces distributed idempotency, communicates synchronously with Account Service, and emits events to Kafka.
- **Core Files & Logic:**
  - `TransactionRecord.java`:
    - Maps `transactions` table with unique constraint on `idempotency_key`.
  - `TransactionServiceImpl.java`:
    - **Step 1 (Idempotency Check):** Queries database for `idempotencyKey`. If found, immediately returns the cached transaction, preventing duplicate processing if a client retries due to a network timeout.
    - **Step 2 (Pre-flight Validation):** Verifies source != target and queries Account Service to ensure both accounts exist and are `ACTIVE`.
    - **Step 3 (Debit Source):** Executes synchronous REST call to Account Service debit endpoint.
    - **Step 4 (Credit Target with Compensation):** Executes credit call. If credit fails (e.g. network partition or target account suddenly frozen), it invokes a **compensating transaction** (re-crediting the source account) to maintain balance integrity (Saga pattern).
    - **Step 5 (Kafka Event Publication):** Calls `TransactionProducer.java` to send `TransactionCreatedEvent` to `bank.transactions`.
  - `TransactionProducer.java`:
    - Attaches `X-Correlation-ID` into Kafka record headers.
    - Asynchronously logs Kafka topic, partition, offset, and transaction ID upon acknowledgment.

---

### Component 5: `fraud-detection-service` (Real-Time Risk Scoring)
- **Location:** `fraud-detection-service/` (Port: 8083)
- **Database:** `apex_fraud_db`
- **Purpose:** Evaluates financial transactions in real-time to detect suspicious patterns without slowing down the primary payment flow.
- **Core Files & Logic:**
  - `TransactionConsumer.java`:
    - `@KafkaListener` consuming `bank.transactions`.
    - Extracts `X-Correlation-ID` from Kafka headers and injects into SLF4J MDC.
  - `FraudRuleEngine.java`:
    - **Rule 1 (High-Value Threshold):**
      - Transfers >= $10,000 add +65 to risk score.
      - Transfers >= $50,000 add +90 to risk score.
    - **Rule 2 (Round Amount Anomaly):** High round numbers (e.g., $10,000.00 with zero cents) add +10.
    - **Risk Classification:**
      - 0–29: `LOW`
      - 30–59: `MEDIUM`
      - 60–84: `HIGH`
      - 85–100: `CRITICAL`
    - If `HIGH` or `CRITICAL`, triggers `FraudAlertProducer.java` to publish `FraudAlertEvent` to `bank.fraud-alerts`.
    - Increments Prometheus counter `apex.banking.fraud.evaluations.total`.
  - `FraudAssessmentRecord.java`:
    - Persists every evaluation, score, triggered rules, and notes in MySQL for regulatory compliance reviews.

---

### Component 6: `notification-service` (Alert Dispatcher)
- **Location:** `notification-service/` (Port: 8084)
- **Database:** `apex_notification_db`
- **Purpose:** Decoupled customer communication layer. Dispatches notifications based on transaction events and security alerts.
- **Core Files & Logic:**
  - `BankingEventNotificationConsumer.java`:
    - Multi-topic listener:
      - Listens to `bank.transactions` -> triggers transaction receipt (Email).
      - Listens to `bank.fraud-alerts` -> triggers urgent security notification (SMS).
  - `NotificationDispatcher.java`:
    - Formats templates, sets status to `SENT`, records notification timestamp, correlation ID, and reference ID.
    - Increments Prometheus counter `apex.banking.notifications.dispatched.total` with tags `channel` (EMAIL/SMS) and `type` (TRANSACTION/FRAUD_ALERT).
  - `NotificationRecord.java`:
    - Stores complete notification history in `apex_notification_db`.

---

### Component 7: Infrastructure & Observability Stack

1. **MySQL 8.0 & `docker/mysql/init.sql`:**
   - Initializes 4 isolated database schemas:
     - `apex_account_db`
     - `apex_transaction_db`
     - `apex_fraud_db`
     - `apex_notification_db`
   - Adheres strictly to the Database-per-Service microservice pattern.
2. **Apache Kafka 3.7+ (KRaft Mode):**
   - Runs with internal KRaft metadata quorum without Zookeeper.
   - Provides durable message logs for `bank.transactions` and `bank.fraud-alerts`.
3. **OpenTelemetry & Jaeger (`http://localhost:16686`):**
   - Each service uses `micrometer-tracing-bridge-otel` and exports spans over OTLP HTTP to `http://jaeger:4318/v1/traces`.
   - Propagates W3C `traceparent` headers through HTTP calls and Kafka headers.
   - Allows visualization of complete request journeys (Gateway -> Transaction Service -> Account Service -> Kafka -> Fraud & Notification).
4. **Prometheus (`http://localhost:9090`):**
   - Automatically polls the `/actuator/prometheus` endpoint of all 5 services every 5 seconds.
   - Aggregates JVM metrics, HTTP latency histograms, and custom banking business counters.
5. **Grafana (`http://localhost:3000`):**
   - Auto-provisions Prometheus datasource (`datasource.yml`).
   - Auto-loads `apex-banking-dashboard.json`:
     - Real-time Completed vs Failed Transactions stat cards.
     - Fraud Alerts by Risk Level.
     - Dispatched Customer Notifications.
     - P95 Latency by Service.
     - JVM Heap Utilization.

---

## 3. HOW TO RUN THE PROJECT (STEP-BY-STEP)

You can run this project in two ways:
- **Method A:** Using Docker Compose (Recommended - spins up all 10 containers at once).
- **Method B:** Running infrastructure in Docker and microservices locally in your IDE (for development and debugging).

---

### METHOD A: Complete Docker Compose Run (Recommended)

#### Step 1: Open PowerShell or Terminal
Navigate to the project root directory:
```powershell
cd C:\Users\ELCOT\.gemini\antigravity\scratch\apex-banking-microservices
```

#### Step 2: Build the Java Binaries
Package all 5 microservices and the common library using Maven:
```powershell
mvn clean package -DskipTests
```
*Expected Output: `BUILD SUCCESS` across all 6 modules (`apex-banking-microservices`, `common-dto`, `api-gateway`, `account-service`, `transaction-service`, `fraud-detection-service`, `notification-service`).*

#### Step 3: Launch Containers via Docker Compose
Run the pre-configured deployment script:
```powershell
.\build-and-run.ps1
```
*(Or manually run: `docker compose up --build -d`)*

#### Step 4: Verify All Containers are Healthy
```powershell
docker compose ps
```
You should see 10 active containers:
- `apex-mysql` (Up - healthy)
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

### METHOD B: Local IDE Run (IntelliJ IDEA / Eclipse / VS Code)

If you wish to edit and debug code inside an IDE:

#### Step 1: Start Only Supporting Infrastructure Containers
Run only MySQL, Kafka, Jaeger, Prometheus, and Grafana in Docker:
```powershell
cd C:\Users\ELCOT\.gemini\antigravity\scratch\apex-banking-microservices
docker compose up -d mysql kafka jaeger prometheus grafana
```

#### Step 2: Open the Project in Your IDE
1. Open your IDE and select **Open Project**.
2. Select the directory: `C:\Users\ELCOT\.gemini\antigravity\scratch\apex-banking-microservices`.
3. Allow the IDE to import the root `pom.xml` as a Maven multi-module project.

#### Step 3: Start the Microservices in the Following Order
Run the main Java class for each service:
1. **`AccountServiceApplication`** (`account-service/src/main/java/com/apex/account/AccountServiceApplication.java`) -> Runs on port 8081
2. **`TransactionServiceApplication`** (`transaction-service/src/main/java/com/apex/transaction/TransactionServiceApplication.java`) -> Runs on port 8082
3. **`FraudDetectionApplication`** (`fraud-detection-service/src/main/java/com/apex/fraud/FraudDetectionApplication.java`) -> Runs on port 8083
4. **`NotificationServiceApplication`** (`notification-service/src/main/java/com/apex/notification/NotificationServiceApplication.java`) -> Runs on port 8084
5. **`ApiGatewayApplication`** (`api-gateway/src/main/java/com/apex/gateway/ApiGatewayApplication.java`) -> Runs on port 8080

---

## 4. VERIFICATION & END-TO-END TEST WALKTHROUGH

Once running, execute the automated test script or run the curl steps below.

### Quick Verification via Automated Script
```powershell
.\test-api.ps1
```
*(On Linux/macOS: `./test-api.sh`)*

---

### Manual Verification (Step-by-Step with curl)

#### Step 1: Create Account 1 (Alice Smith - Checking Account)
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: flow-test-001" \
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
- **What happens:**
  - `api-gateway` logs ingress and routes to `account-service:8081`.
  - `account-service` generates account number (e.g. `APX10001`), creates `BankAccount` entity with initial balance `$5,000.00`, creates `AccountAuditLog`, and commits to `apex_account_db`.
  - Returns HTTP 201 Created with JSON envelope.

#### Step 2: Create Account 2 (Bob Jones - Savings Account)
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: flow-test-002" \
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

#### Step 3: Execute Fund Transfer ($400 from Alice to Bob)
Replace `<ALICE_ACC>` and `<BOB_ACC>` with the account numbers generated in Steps 1 & 2:
```bash
curl -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: flow-test-003" \
  -d '{
    "sourceAccountNumber": "<ALICE_ACC>",
    "targetAccountNumber": "<BOB_ACC>",
    "amount": 400.00,
    "currency": "USD",
    "description": "Invoice payment",
    "idempotencyKey": "IDEMP-KEY-1001"
  }'
```
- **What happens behind the scenes:**
  1. `transaction-service` checks `idempotency_key` (not found).
  2. Calls `account-service` via REST to debit Alice $400.
  3. Calls `account-service` via REST to credit Bob $400.
  4. Saves `TransactionRecord` with status `COMPLETED` in `apex_transaction_db`.
  5. Publishes `TransactionCreatedEvent` to Kafka topic `bank.transactions`.
  6. `fraud-detection-service` consumes event: evaluates risk score (Score: 0, `LOW`).
  7. `notification-service` consumes event: records an `EMAIL` confirmation record in `apex_notification_db`.

#### Step 4: Verify Idempotency Protection
Re-run the exact same curl command from Step 3 with `IDEMP-KEY-1001`.
- **Result:** It returns the original transaction record immediately.
- **Verification:** Alice's balance remains $4,600 (not $4,200). Duplicate debit was prevented!

#### Step 5: High-Value Transfer & Fraud Detection Trigger ($15,000)
1. Deposit $20,000 into Alice's account:
```bash
curl -X POST http://localhost:8080/api/v1/accounts/credit \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "<ALICE_ACC>",
    "amount": 20000.00,
    "referenceId": "PAYROLL-BONUS",
    "description": "Executive bonus"
  }'
```
2. Transfer $15,000 to Bob (exceeds the $10,000 compliance rule):
```bash
curl -X POST http://localhost:8080/api/v1/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "<ALICE_ACC>",
    "targetAccountNumber": "<BOB_ACC>",
    "amount": 15000.00,
    "currency": "USD",
    "description": "Investment transfer",
    "idempotencyKey": "IDEMP-KEY-1002"
  }'
```
3. Check the Fraud Detection Service:
```bash
curl -X GET http://localhost:8080/api/v1/fraud/alerts/high-risk
```
- **Result:** You will see an alert flagged with `riskLevel: "HIGH"`, `riskScore: 75`, and `ruleTriggered: "HIGH_VALUE_EXCEEDED_10K; ROUND_HIGH_AMOUNT_ANOMALY; "`.
4. Check the Notification Service:
```bash
curl -X GET http://localhost:8080/api/v1/notifications/recipient/<ALICE_ACC>
```
- **Result:** You will see an urgent security alert dispatched via `SMS`.

---

## 5. OBSERVABILITY & MONITORING GUIDE

### 1. View Distributed Traces in Jaeger (`http://localhost:16686`)
1. Open browser to `http://localhost:16686`.
2. Under **Service**, pick `api-gateway` or `transaction-service`.
3. Click **Find Traces**.
4. Click on any transfer trace.
5. **What you will see:** A full waterfall visualization showing the request arriving at `api-gateway`, propagating to `transaction-service`, branching out to `account-service` HTTP calls, sending to Kafka, and Kafka consumers picking it up—all under one unified Trace ID.

### 2. View Grafana Dashboards (`http://localhost:3000`)
1. Open browser to `http://localhost:3000`.
2. Login with:
   - Username: `admin`
   - Password: `admin`
3. Click **Dashboards** -> Open **Apex Banking - Distributed Microservices Dashboard**.
4. The dashboard displays:
   - Total Completed Transactions
   - Failed Transactions
   - High/Critical Fraud Alerts Triggered
   - Notifications Dispatched
   - P95 Latency Graphs per Service
   - JVM Memory Utilization per Service

### 3. View Prometheus Metrics (`http://localhost:9090`)
1. Open `http://localhost:9090`.
2. Query custom banking metrics:
   - `apex_banking_transactions_completed_total_total`
   - `apex_banking_fraud_evaluations_total_total`
   - `apex_banking_notifications_dispatched_total_total`

### 4. Structured Logging with SLF4J and MDC
Check logs from any container:
```powershell
docker logs apex-transaction-service
```
Every log line follows this standard structure:
```text
2026-10-30 21:15:30.124 [http-nio-8082-exec-1] INFO  c.a.t.s.TransactionServiceImpl - [traceId=a1b2c3d4e5f6 spanId=12345678 corrId=flow-test-003] - [TRANSFER-SUCCESS] txId=TXN-9876 from=APX10001 to=APX10002 amount=400.00
```
Searching by `corrId` in your terminal or log viewer displays the complete lifecycle across every microservice.

---

## 6. POST-CREATION ADJUSTMENTS & TROUBLESHOOTING

### Issue 1: Port 3306 is already in use
- **Cause:** You have a local MySQL or MariaDB instance running on your host machine.
- **Solution:**
  1. Open [docker-compose.yml](file:///C:/Users/ELCOT/.gemini/antigravity/scratch/apex-banking-microservices/docker-compose.yml).
  2. In the `mysql` block, change the port mapping from `"3306:3306"` to `"3307:3306"`.
  3. In the service blocks (`account-service`, `transaction-service`, etc.), change `MYSQL_PORT: 3306` to `MYSQL_PORT: 3307` (if running locally) or keep internal port 3306 inside Docker network.

### Issue 2: Docker memory exhaustion
- **Cause:** Running 10 containers simultaneously may require 4GB–6GB of RAM.
- **Solution:** Ensure Docker Desktop has at least 6GB of RAM allocated in Docker Settings -> Resources -> Memory.

### Issue 3: Kafka connection timeout on initial startup
- **Cause:** Microservices may boot faster than the Kafka broker's internal metadata initialization.
- **Solution:** The microservices have built-in retry mechanisms (`spring.kafka.producer.retries: 3`), but if a container exits prematurely, simply run:
  ```powershell
  docker compose restart transaction-service fraud-detection-service notification-service
  ```

---

## 7. RESUME TALKING POINTS & INTERVIEW CHEAT SHEET

When asked about this project in technical interviews:

1. **"How do you ensure data consistency without distributed 2PC transactions?"**
   > *"We employ the Saga Choreography pattern. The Transaction Service coordinates transfers by invoking atomic account debits. If the downstream credit fails, a compensatory transaction executes immediately to refund the source account. Eventual consistency is maintained for downstream analytics (fraud detection and notifications) via Apache Kafka event streaming."*

2. **"How does the system prevent lost updates under heavy concurrent traffic?"**
   > *"We implemented Optimistic Concurrency Control using JPA `@Version` on the BankAccount entity. Rather than pessimistic row-locking which degrades throughput, database row versions are verified at commit time. Version collisions trigger a conflict exception that is handled cleanly without data corruption."*

3. **"How is distributed tracing implemented across synchronous REST and asynchronous Kafka?"**
   > *"We used Micrometer Tracing with OpenTelemetry bridge. Outgoing HTTP requests carry W3C `traceparent` headers injected by RestClient interceptors. For Kafka messages, `TransactionProducer` injects the trace context and correlation ID directly into Kafka record headers, allowing consumers to continue the exact same trace span in Jaeger."*
