#!/usr/bin/env bash
# ==============================================================================
# Apex Banking Platform - One-Click Build & Deployment Script (Bash)
# ==============================================================================

set -e

echo "=========================================================="
echo "   BUILDING & DEPLOYING APEX BANKING MICROSERVICES        "
echo "=========================================================="

echo -e "\n>>> [1/2] Compiling and packaging Java microservices..."
mvn clean package -DskipTests

echo -e "\n>>> [2/2] Launching containers via Docker Compose..."
docker compose up --build -d

echo -e "\n=========================================================="
echo "   APEX BANKING PLATFORM IS UP AND RUNNING!               "
echo "=========================================================="
echo " - API Gateway:        http://localhost:8080"
echo " - Grafana Dashboards: http://localhost:3000 (User: admin / Pass: admin)"
echo " - Prometheus Metrics: http://localhost:9090"
echo " - Jaeger Tracing UI:  http://localhost:16686"
echo " - MySQL Server:       localhost:3306 (User: apex_user / Pass: apex_secret)"
echo " - Kafka Broker:       localhost:9092"
echo -e "\nTo run the automated test suite: ./test-api.sh"
echo "=========================================================="
