# ==============================================================================
# Apex Banking Platform - One-Click Build & Deployment Script (PowerShell)
# ==============================================================================

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   BUILDING & DEPLOYING APEX BANKING MICROSERVICES        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Maven Build
Write-Host "`n>>> [1/2] Compiling and packaging Java microservices..." -ForegroundColor Yellow
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[ERROR] Maven build failed. Please resolve compilation errors." -ForegroundColor Red
    exit 1
}

# 2. Docker Compose Up
Write-Host "`n>>> [2/2] Launching containers via Docker Compose..." -ForegroundColor Yellow
docker compose up --build -d

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "   APEX BANKING PLATFORM IS UP AND RUNNING!               " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " - API Gateway:        http://localhost:8080"
Write-Host " - Grafana Dashboards: http://localhost:3000 (User: admin / Pass: admin)"
Write-Host " - Prometheus Metrics: http://localhost:9090"
Write-Host " - Jaeger Tracing UI:  http://localhost:16686"
Write-Host " - MySQL Server:       localhost:3306 (User: apex_user / Pass: apex_secret)"
Write-Host " - Kafka Broker:       localhost:9092"
Write-Host "`nTo run the automated test suite: .\test-api.ps1" -ForegroundColor Cyan
Write-Host "==========================================================`n" -ForegroundColor Green
