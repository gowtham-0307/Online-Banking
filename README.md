# Banking Microservices Platform

An enterprise-grade, event-driven distributed banking architecture engineered with **Java 17 / 21**, **Spring Boot 3**, **MySQL**, **Apache Kafka**, **OpenTelemetry (OTel)**, **SLF4J / MDC Structured Logging**, **Prometheus**, and **Grafana**.

---

## 🏛️ System Architecture

```mermaid
flowchart TD
    Client["Client / Postman / Frontend"] -->|HTTP / REST| GW["API Gateway (Port 8080)<br/>Spring Cloud Gateway + OTel Trace Filter"]
    
    subgraph Core Domain Microservices
        GW -->|/api/v1/accounts/**| ACT["Account Service (Port 8081)<br/>Ledger & Optimistic Locking"]
        GW -->|/api/v1/transactions/**| TXN["Transaction Service (Port 8082)<br/>Transfer Orchestrator & Idempotency"]
        GW -->|/api/v1/fraud/**| FRD["Fraud Detection Service (Port 8083)<br/>Rule Engine & Risk Assessment"]
        GW -->|/api/v1/notifications/**| NTF["Notification Service (Port 8084)<br/>Multi-channel Alert Dispatcher"]
    end

    TXN -->|REST Synchronous Debit/Credit| ACT
    TXN -->|Publishes: bank.transactions| KAFKA["Apache Kafka (KRaft Broker)<br/>Topic: bank.transactions"]
    
    KAFKA -->|Consumes: bank.transactions| FRD
    KAFKA -->|Consumes: bank.transactions| NTF

    FRD -->|Publishes: bank.fraud-alerts (High/Critical)| KAFKA_ALERTS["Kafka Topic: bank.fraud-alerts"]
    KAFKA_ALERTS -->|Consumes: bank.fraud-alerts| NTF

    subgraph Persistence Layer
        ACT --> DB1[("MySQL: apex_account_db")]
        TXN --> DB2[("MySQL: apex_transaction_db")]
        FRD --> DB3[("MySQL: apex_fraud_db")]
        NTF --> DB4[("MySQL: apex_notification_db")]
    end

    subgraph Observability & Monitoring
        GW -.->|OTLP Traces| JAEGER["Jaeger / OTel UI (Port 16686)"]
        ACT -.->|OTLP Traces| JAEGER
        TXN -.->|OTLP Traces| JAEGER
        FRD -.->|OTLP Traces| JAEGER
        NTF -.->|OTLP Traces| JAEGER

        PROM["Prometheus (Port 9090)"] -.->|Scrapes /actuator/prometheus| GW
        PROM -.->|Scrapes /actuator/prometheus| ACT
        PROM -.->|Scrapes /actuator/prometheus| TXN
        PROM -.->|Scrapes /actuator/prometheus| FRD
        PROM -.->|Scrapes /actuator/prometheus| NTF

        GRAFANA["Grafana Dashboards (Port 3000)"] -->|Queries Metrics| PROM
    end
```

---

## 🚀 The 5 Microservices

| Service | Port | Database | Primary Responsibilities |
| :--- | :---: | :--- | :--- |
| **`api-gateway`** | `8080` | None | Ingress routing, distributed correlation ID injection (`X-Correlation-ID`), W3C TraceContext propagation, circuit breaker fallbacks, latency logging. |
| **`account-service`** | `8081` | `apex_account_db` | Core bank account lifecycle, balances, deposits, debits/credits with JPA `@Version` **Optimistic Locking** to eliminate concurrent double-spending race conditions, audit ledger. |
| **`transaction-service`** | `8082` | `apex_transaction_db` | Fund transfers orchestrator, **Idempotency Key** protection (re-submitting identical keys returns cached results), atomic debit/credit coordination with automated compensation/rollback, Kafka event producer (`bank.transactions`). |
| **`fraud-detection-service`** | `8083` | `apex_fraud_db` | Asynchronous Kafka event consumer, real-time multi-rule risk evaluation engine (e.g. transfer >= $10,000 threshold, round-amount heuristics), produces `FraudAlertEvent` to `bank.fraud-alerts`. |
| **`notification-service`** | `8084` | `apex_notification_db` | Event-driven customer alert dispatcher, consumes transaction and fraud alert topics, multi-channel dispatch simulation (EMAIL, SMS), notification audit history. |

---

## 🛠️ Technology Stack

- **Backend**: Java 17+, Spring Boot 3.2.x / 3.3.x, Spring Cloud Gateway, Spring Data JPA, Spring Kafka, Spring Boot Actuator.
- **Data Stores**: MySQL 8.0 (separate isolated schemas per service adhering to Database-per-Microservice pattern).
- **Messaging**: Apache Kafka 3.7+ (KRaft mode: zero Zookeeper overhead).
- **Observability & Tracing**: OpenTelemetry (OTel), Micrometer Tracing Bridge OTel, Jaeger UI.
- **Logging**: SLF4J, Logback, MDC (Mapped Diagnostic Context) capturing `traceId`, `spanId`, `correlationId`, `accountNumber`, `transactionId`.
- **Metrics & Visualization**: Prometheus, Micrometer Registry Prometheus, Grafana with auto-provisioned dashboards.
- **Containerization**: Docker, Docker Compose.

---

## 📋 Resume Bullet Points (STAR Method)

You can customize and paste these impactful bullets directly into your resume:

- **Architected and delivered an enterprise distributed Banking Microservices Platform** utilizing **Java 17**, **Spring Boot 3**, **Spring Cloud Gateway**, **MySQL**, and **Apache Kafka**, handling end-to-end account lifecycle, idempotent fund transfers, and real-time fraud mitigation.
- **Engineered financial ledger integrity and race condition prevention** by implementing **JPA Optimistic Concurrency Control (`@Version`)** in Account Service and **Distributed Idempotency Key validation** in Transaction Service, preventing duplicate debits during high-concurrency bursts and network retries.
- **Designed an asynchronous event-driven pipeline via Apache Kafka (KRaft)** to decouple payment transfers from non-blocking downstream processes; implemented real-time heuristic rule scoring in Fraud Detection Service and multi-channel customer alerting in Notification Service.
- **Implemented end-to-end Full-Stack Observability** integrating **OpenTelemetry (OTel)**, **Micrometer**, **Prometheus**, and **Grafana**; instrumented custom business metrics (`apex.banking.transactions.completed.total`, fraud evaluation rates) and pre-configured Grafana dashboards for latency and throughput visualization.
- **Standardized structured logging across 5 microservices** leveraging **SLF4J and MDC**, propagating W3C `traceparent` and `X-Correlation-ID` across HTTP and Kafka headers to enable 100% distributed trace correlation in **Jaeger**.

---

## ⚡ Quick Start

### Option A: Complete Docker Compose Run (Recommended)

```powershell
# 1. Compile and package the Java applications
mvn clean package -DskipTests

# 2. Start all 5 microservices and infrastructure containers
docker compose up --build -d

# 3. Run the automated end-to-end test suite
.\test-api.ps1   # (On Linux/macOS: ./test-api.sh)
```

### Option B: Local IDE Run (IntelliJ IDEA / VS Code / Eclipse)

1. Start only infrastructure containers:
   ```bash
   docker compose up mysql kafka jaeger prometheus grafana -d
   ```
2. Open each microservice module and run their Spring Boot main classes:
   - `AccountServiceApplication` (Port 8081)
   - `TransactionServiceApplication` (Port 8082)
   - `FraudDetectionApplication` (Port 8083)
   - `NotificationServiceApplication` (Port 8084)
   - `ApiGatewayApplication` (Port 8080)

---

## 🌐 Web Endpoints & Dashboards

- **API Gateway**: `http://localhost:8080`
- **Grafana Monitoring Dashboard**: `http://localhost:3000` *(Default: admin / admin)*
- **Prometheus Metric Explorer**: `http://localhost:9090`
- **Jaeger Distributed Tracing UI**: `http://localhost:16686`
- **MySQL Database**: `localhost:3306` *(User: apex_user / Pass: apex_secret)*
- **Kafka Broker**: `localhost:9092`
