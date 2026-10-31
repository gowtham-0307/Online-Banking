# INTELLIJ IDEA BEGINNER'S WALKTHROUGH & EXECUTION GUIDE
## Apex Banking Microservices Platform

Welcome! You have just opened the **Apex Banking Microservices** project in IntelliJ IDEA. 
This document assumes **zero prior knowledge** of this codebase and explains every single click, configuration, command, and behind-the-scenes event step-by-step.

---

## 📋 TABLE OF CONTENTS
1. [What You See Right Now in IntelliJ](#1-what-you-see-right-now-in-intellij)
2. [What Needs to be Changed in the Project?](#2-what-needs-to-be-changed-in-the-project)
3. [Step 1: One-Time IntelliJ Setup (2 Minutes)](#3-step-1-one-time-intellij-setup-2-minutes)
4. [Step 2: Starting the Supporting Infrastructure (Docker)](#4-step-2-starting-the-supporting-infrastructure-docker)
5. [Step 3: Running the 5 Microservices Inside IntelliJ](#5-step-3-running-the-5-microservices-inside-intellij)
6. [Step 4: Testing the Banking System (How It Works Behind the Scenes)](#6-step-4-testing-the-banking-system-how-it-works-behind-the-scenes)
7. [Step 5: How to View Dashboards, Traces, and Logs](#7-step-5-how-to-view-dashboards-traces-and-logs)
8. [Troubleshooting Common Beginner Mistakes](#8-troubleshooting-common-beginner-mistakes)

---

## 1. WHAT YOU SEE RIGHT NOW IN INTELLIJ

When you open this project in IntelliJ IDEA:
- **Left Pane (Project View):** You will see the project folders:
  - `common-dto` (Shared data structures)
  - `api-gateway` (Port 8080 - Front door router)
  - `account-service` (Port 8081 - Balance ledger)
  - `transaction-service` (Port 8082 - Fund transfer engine)
  - `fraud-detection-service` (Port 8083 - Risk scoring)
  - `notification-service` (Port 8084 - SMS/Email alerts)
  - `docker/` (MySQL, Prometheus, Grafana configs)
  - `pom.xml` (The master Maven blueprint)
- **Center Pane:** Editor window.
- **Right Pane:** The **Maven** tool window tab.
- **Bottom Bar:** The **Terminal** tab (`Alt + F12`), **Run** tab, and **Git** tab.

---

## 2. WHAT NEEDS TO BE CHANGED IN THE PROJECT?

### The Direct Answer:
👉 **Nothing in the Java code needs to be modified.** The code is fully written, production-ready, and configured.

### What about Ports and Configuration?
- **Ports Verified:** We already checked your Windows machine: ports `3306` (MySQL), `9092` (Kafka), `8080` (Gateway), `8081` (Account), `8082` (Transaction), `8083` (Fraud), and `8084` (Notification) are **completely free**. There are zero port collisions.
- **Configuration Defaults:** Every microservice's `application.yml` file is configured with intelligent defaults (`localhost`), meaning that when you start services directly inside IntelliJ, they automatically talk to `localhost:3306` for MySQL, `localhost:9092` for Kafka, and `localhost:4318` for Jaeger!

---

## 3. STEP 1: ONE-TIME INTELLIJ SETUP (2 MINUTES)

Before running the code, complete these 3 quick IntelliJ configurations:

### 1.1 Set Project JDK to Java 17+
1. In the top menu, click **File** ➔ **Project Structure...** (or press `Ctrl + Alt + Shift + S`).
2. On the left, select **Project**.
3. Under **SDK**, ensure Java 17, 21, or 26 is selected (e.g. `temurin-17`, `openjdk-21`, or your installed JDK).
4. Under **Language level**, choose **17 - Sealed types, always-strict floating-point**.
5. Click **Apply**, then **OK**.

*Why this step?* Tells IntelliJ which compiler to use so that modern Java records, enums, and switch expressions compile properly.

---

### 1.2 Enable Annotation Processing (Crucial for Lombok)
1. In the top menu, click **File** ➔ **Settings** (or press `Ctrl + Alt + S`).
2. In the search box at the top-left, type: `Annotation Processors` (or navigate to: **Build, Execution, Deployment** ➔ **Compiler** ➔ **Annotation Processors**).
3. Check the box: **☑ Enable annotation processing**.
4. Click **Apply**, then **OK**.

*Why this step?* The project uses **Project Lombok** (`@Getter`, `@Setter`, `@Builder`) to eliminate boilerplate code. This setting instructs IntelliJ to generate getters/setters during compilation, preventing red squiggly lines in the editor.

---

### 1.3 Sync / Reload Maven Projects
1. On the far right edge of your IntelliJ window, click the **Maven** tool window tab.
2. In the Maven panel, click the **Reload All Maven Projects** icon (the two circular arrows in the top left of the Maven panel).
3. Wait 15–30 seconds for IntelliJ to index the modules and download dependencies.

*Why this step?* Ensures IntelliJ recognizes all 6 modules (`common-dto`, `api-gateway`, etc.) as interconnected Maven modules.

---

## 4. STEP 2: STARTING THE SUPPORTING INFRASTRUCTURE (DOCKER)

Our microservices need 5 supporting infrastructure systems to store data, stream messages, and collect traces:
1. **MySQL Server** (Stores bank accounts and transaction history)
2. **Apache Kafka** (Streams events between services)
3. **Jaeger** (Collects OpenTelemetry traces)
4. **Prometheus** (Gathers health metrics)
5. **Grafana** (Displays visual monitoring charts)

You do **NOT** need to manually install MySQL, Kafka, or Grafana on Windows. We run them with one command using Docker!

### How to Start Infrastructure:
1. Open the built-in **Terminal** in IntelliJ at the bottom bar (or press `Alt + F12`).
2. Type or paste the following command and press **Enter**:
   ```powershell
   docker compose up -d mysql kafka jaeger prometheus grafana
   ```
3. Wait about 10 seconds. You will see:
   ```text
   [+] Running 6/6
   ✔ Network apex-banking-microservices_apex-network  Created
   ✔ Container apex-mysql                             Started
   ✔ Container apex-kafka                             Started
   ✔ Container apex-jaeger                            Started
   ✔ Container apex-prometheus                        Started
   ✔ Container apex-grafana                           Started
   ```

*What this step does behind the scenes:*
- **MySQL** starts and executes `docker/mysql/init.sql`, automatically creating 4 isolated banking databases: `apex_account_db`, `apex_transaction_db`, `apex_fraud_db`, and `apex_notification_db`.
- **Kafka** boots in native KRaft mode on port `9092`.
- **Jaeger** starts listening for traces on port `4318`.
- **Prometheus** begins waiting for microservice metrics on port `9090`.
- **Grafana** boots with our pre-built Banking Dashboard on port `3000`.

---

## 5. STEP 3: RUNNING THE 5 MICROSERVICES INSIDE INTELLIJ

Now you run the Java microservices. Run them in the recommended order below:

### Service 1: `account-service` (Port 8081)
1. In the Project pane on the left, navigate to:
   `account-service` ➔ `src` ➔ `main` ➔ `java` ➔ `com.apex.account` ➔ **`AccountServiceApplication.java`**.
2. Double-click to open it.
3. Look at line 14: `public class AccountServiceApplication`.
4. Click the **Green Play Arrow ▶** on the left margin (next to `public class` or `public static void main`).
5. Click **Run 'AccountServiceApplication'**.
6. At the bottom of IntelliJ, the **Run** window opens. You will see Spring Boot logs ending with:
   `>>> Apex Banking Account Service initialized on port 8081 <<<`

---

### Service 2: `transaction-service` (Port 8082)
1. Navigate to:
   `transaction-service` ➔ `src` ➔ `main` ➔ `java` ➔ `com.apex.transaction` ➔ **`TransactionServiceApplication.java`**.
2. Open it and click the **Green Play Arrow ▶** ➔ **Run 'TransactionServiceApplication'**.
3. Log output will conclude with:
   `>>> Apex Banking Transaction Service initialized on port 8082 <<<`

---

### Service 3: `fraud-detection-service` (Port 8083)
1. Navigate to:
   `fraud-detection-service` ➔ `src` ➔ `main` ➔ `java` ➔ `com.apex.fraud` ➔ **`FraudDetectionApplication.java`**.
2. Open it and click the **Green Play Arrow ▶** ➔ **Run 'FraudDetectionApplication'**.
3. Log output will conclude with:
   `>>> Apex Banking Fraud Detection Service initialized on port 8083 <<<`

---

### Service 4: `notification-service` (Port 8084)
1. Navigate to:
   `notification-service` ➔ `src` ➔ `main` ➔ `java` ➔ `com.apex.notification` ➔ **`NotificationServiceApplication.java`**.
2. Open it and click the **Green Play Arrow ▶** ➔ **Run 'NotificationServiceApplication'**.
3. Log output will conclude with:
   `>>> Apex Banking Notification Service initialized on port 8084 <<<`

---

### Service 5: `api-gateway` (Port 8080)
1. Navigate to:
   `api-gateway` ➔ `src` ➔ `main` ➔ `java` ➔ `com.apex.gateway` ➔ **`ApiGatewayApplication.java`**.
2. Open it and click the **Green Play Arrow ▶** ➔ **Run 'ApiGatewayApplication'**.
3. Log output will conclude with:
   `>>> Apex Banking API Gateway started successfully on port 8080 <<<`

> [!TIP]
> **Services Dashboard in IntelliJ:** IntelliJ will automatically show a popup in the bottom right corner: *"Services tool window available"*. Click **Show Services**. All 5 running microservices will now be grouped together in the **Services** tab at the bottom, where you can view logs, restart, or stop them with one click!

---

## 6. STEP 4: TESTING THE BANKING SYSTEM (HOW IT WORKS BEHIND THE SCENES)

Now that everything is running, let's execute real banking transactions!

### Quick Automated Test:
In IntelliJ's Terminal (`Alt + F12`), run:
```powershell
.\test-api.ps1
```
This runs 10 end-to-end integration tests and prints `[PASS]` for all steps.

---

### Manual Walkthrough: What Happens Behind the Scenes

Open IntelliJ's Terminal (or Windows PowerShell) and run these requests step-by-step to understand the system:

#### Request 1: Open an Account for Alice Smith ($5,000 Deposit)
```bash
curl -X POST http://localhost:8080/api/v1/accounts -H "Content-Type: application/json" -d "{\"customerId\":\"CUST-1001\",\"customerName\":\"Alice Smith\",\"email\":\"alice@bank.com\",\"phoneNumber\":\"+1-555-0101\",\"accountType\":\"CHECKING\",\"initialDeposit\":5000.00,\"currency\":\"USD\"}"
```
*What happens:*
1. Request hits `api-gateway` on port `8080`.
2. Gateway's `LoggingAndTracingFilter` generates a correlation ID and forwards request to `account-service:8081`.
3. `account-service` generates a unique account number (e.g. `APX1092837461`).
4. It inserts a row into the `bank_accounts` table in `apex_account_db` with `balance = 5000.00` and `version = 0`.
5. It inserts an initial audit record into `account_audit_logs`.
6. Look at the **AccountServiceApplication** console in IntelliJ: you will see structured SLF4J logs detailing the account creation!

---

#### Request 2: Open an Account for Bob Jones ($1,200 Deposit)
```bash
curl -X POST http://localhost:8080/api/v1/accounts -H "Content-Type: application/json" -d "{\"customerId\":\"CUST-1002\",\"customerName\":\"Bob Jones\",\"email\":\"bob@bank.com\",\"phoneNumber\":\"+1-555-0102\",\"accountType\":\"SAVINGS\",\"initialDeposit\":1200.00,\"currency\":\"USD\"}"
```
*Result:* Creates Bob's account (e.g. `APX8172635490`).

---

#### Request 3: Alice Transfers $400 to Bob (The Full Microservices Dance!)
Replace `APX_ALICE` and `APX_BOB` with the actual account numbers returned from Requests 1 & 2:
```bash
curl -X POST http://localhost:8080/api/v1/transactions/transfer -H "Content-Type: application/json" -d "{\"sourceAccountNumber\":\"APX_ALICE\",\"targetAccountNumber\":\"APX_BOB\",\"amount\":400.00,\"currency\":\"USD\",\"description\":\"Invoice #101\",\"idempotencyKey\":\"KEY-TRANS-01\"}"
```
*What happens across the entire distributed system:*
1. **Gateway (8080):** Injects W3C `traceparent` and routes to `transaction-service:8082`.
2. **Transaction Service (8082):**
   - Checks `idempotencyKey` in `transactions` table. It's new, so it proceeds.
   - Makes an HTTP call to `account-service` to debit Alice $400.
   - Alice's balance in `apex_account_db` becomes `$4,600.00`, and `version` increments from 0 to 1 (**Optimistic Concurrency Control**).
   - Makes an HTTP call to `account-service` to credit Bob $400. Bob's balance becomes `$1,600.00`.
   - Saves transaction state as `COMPLETED`.
   - Asynchronously publishes a `TransactionCreatedEvent` to Kafka topic `bank.transactions`.
3. **Kafka (9092):** Receives the message and distributes it to consumer groups.
4. **Fraud Detection Service (8083):**
   - `@KafkaListener` picks up the event.
   - `FraudRuleEngine` calculates risk score: Transfer is $400 (under $10,000 threshold), so `Score = 0 (LOW)`.
   - Records assessment in `apex_fraud_db`.
5. **Notification Service (8084):**
   - `@KafkaListener` picks up the event.
   - Simulates formatting an Email receipt for Alice and Bob.
   - Saves record into `apex_notification_db`.

---

#### Request 4: Test Idempotency Protection (Re-sending the Exact Same Transfer)
Re-run the exact same curl command from Request 3 with `idempotencyKey: "KEY-TRANS-01"`.
*What happens:*
- The Transaction Service queries MySQL and sees `KEY-TRANS-01` already exists.
- It returns the original transaction receipt immediately!
- **Crucial Banking Guarantee:** Alice is **NOT** debited a second time. Her balance remains $4,600 (not $4,200).

---

#### Request 5: Trigger Real-Time Fraud Alert ($15,000 Transfer > $10,000 Threshold)
Deposit $20,000 into Alice's account, then transfer $15,000 to Bob:
```bash
# 1. Deposit
curl -X POST http://localhost:8080/api/v1/accounts/credit -H "Content-Type: application/json" -d "{\"accountNumber\":\"APX_ALICE\",\"amount\":20000.00,\"referenceId\":\"BONUS\"}"

# 2. Transfer $15,000
curl -X POST http://localhost:8080/api/v1/transactions/transfer -H "Content-Type: application/json" -d "{\"sourceAccountNumber\":\"APX_ALICE\",\"targetAccountNumber\":\"APX_BOB\",\"amount\":15000.00,\"currency\":\"USD\",\"description\":\"Large wire transfer\",\"idempotencyKey\":\"KEY-FRAUD-01\"}"
```
*What happens in Fraud Detection Service:*
- Amount $15,000 exceeds the $10,000 threshold!
- `FraudRuleEngine` assigns `Risk Score = 75` and flags the transfer as **`HIGH RISK`**.
- It publishes a `FraudAlertEvent` to Kafka topic `bank.fraud-alerts`.
- **Notification Service** consumes the alert and dispatches an **URGENT SMS SECURITY WARNING**!
- Verify by querying fraud alerts:
  ```bash
  curl http://localhost:8080/api/v1/fraud/alerts/high-risk
  ```

---

## 7. STEP 5: HOW TO VIEW DASHBOARDS, TRACES, AND LOGS

### 7.1 View Distributed Tracing in Jaeger (`http://localhost:16686`)
1. Open your web browser and navigate to: **`http://localhost:16686`**.
2. In the left panel, under **Service**, select **`transaction-service`**.
3. Click the blue **Find Traces** button.
4. Click on any trace for `/api/v1/transactions/transfer`.
5. **What you see:** A full visual timeline showing:
   - Request entering `api-gateway`
   - Reaching `transaction-service`
   - Child span calling `account-service` debit endpoint
   - Child span calling `account-service` credit endpoint
   - Kafka producer publishing the message
   - Kafka consumers in `fraud-detection-service` and `notification-service`
   - **All correlated under a single Trace ID!**

---

### 7.2 View Live Dashboards in Grafana (`http://localhost:3000`)
1. Open your web browser and navigate to: **`http://localhost:3000`**.
2. Login with credentials:
   - **Username:** `admin`
   - **Password:** `admin`
3. Click **Dashboards** (icon on the left) ➔ Open **Apex Banking - Distributed Microservices Dashboard**.
4. **What you see:**
   - Real-time stat cards: **Completed Transactions**, **Failed Transactions**, **Fraud Alerts Triggered**, **Notifications Sent**.
   - Graphs for **HTTP Request Rate** and **P95 Latency**.
   - **JVM Heap Memory Usage** for each running microservice container.

---

### 7.3 View Raw Metrics in Prometheus (`http://localhost:9090`)
1. Open your browser to: **`http://localhost:9090`**.
2. Type in the query box:
   `sum(apex_banking_transactions_completed_total_total)`
3. Click **Execute** ➔ Click the **Graph** tab to see transaction count growth over time!

---

### 7.4 Reading Structured Logs in IntelliJ
Click on any running service console in IntelliJ (e.g. `TransactionServiceApplication`). Every log line displays structured metadata:
```text
2026-10-30 21:15:30.124 [http-nio-8082-exec-1] INFO  c.a.t.s.TransactionServiceImpl - [traceId=4bf92f3577b34da6a3ce929d0e0e4736 spanId=00f067aa0ba902b7 corrId=run-01] - [TRANSFER-SUCCESS] txId=TXN-A1B2 from=APX1001 to=APX1002 amount=400.00
```
- Notice `traceId`, `spanId`, and `corrId` in brackets!
- Copy the `traceId` and paste it into Jaeger's search bar to view the trace graph.

---

## 8. TROUBLESHOOTING COMMON BEGINNER MISTAKES

### Mistake 1: Red squiggly lines under getters/setters in code
- **Cause:** Lombok annotation processor is disabled in IntelliJ.
- **Fix:** Go to **File** ➔ **Settings** ➔ **Build, Execution, Deployment** ➔ **Compiler** ➔ **Annotation Processors** ➔ Check **Enable annotation processing** ➔ Click **OK**.

### Mistake 2: "Connection refused: localhost:3306" when starting Account Service
- **Cause:** Docker infrastructure was not started yet.
- **Fix:** In IntelliJ Terminal (`Alt + F12`), run:
  `docker compose up -d mysql kafka jaeger prometheus grafana`
  Wait 10 seconds, then restart the service in IntelliJ.

### Mistake 3: Starting API Gateway before the other services
- **Cause:** Gateway looks for downstream services.
- **Fix:** While the Gateway has built-in retry, it is best practice to start domain services (`account-service`, `transaction-service`, `fraud-detection-service`, `notification-service`) before starting `api-gateway`.

---

## 🎯 SUMMARY OF WHAT TO DO RIGHT NOW IN INTELLIJ:

1. Press `Ctrl + Alt + S` ➔ Enable **Annotation Processors**.
2. Open Terminal (`Alt + F12`) ➔ Run:
   ```powershell
   docker compose up -d mysql kafka jaeger prometheus grafana
   ```
3. Run the 5 applications via their **Green Play Arrows ▶** (`Account`, `Transaction`, `Fraud`, `Notification`, `Gateway`).
4. In Terminal, run:
   ```powershell
   .\test-api.ps1
   ```
5. Open your browser to `http://localhost:3000` (Grafana) and `http://localhost:16686` (Jaeger) to see your banking system live!
