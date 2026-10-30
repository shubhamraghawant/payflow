# 💳 PayFlow — Real-Time Payment Processing Microservices System

![Java](https://img.shields.io/badge/Java-21-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-7.5-black?logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)
![OpenAPI](https://img.shields.io/badge/OpenAPI-Swagger-green?logo=swagger)
![License](https://img.shields.io/badge/License-MIT-green)

A high-reliability, asynchronous **payment processing platform** modelled after Tier-1 fintech architectures. Built with **Java 21, Spring Boot 3.2, Apache Kafka, and PostgreSQL**, featuring event-driven fraud detection, sliding-window rate limiting, and guaranteed transaction idempotency.

---

## 🏛️ Architecture Overview

PayFlow decouples payment initiation from fraud evaluation and notifications through Kafka event streams:

```
Client ──[POST /api/v1/payments]──▶ payment-service (:8081)
                                      │
                                      ▼ saves PENDING to PostgreSQL
                                      │
                                      ▼ Kafka topic: 'payment.initiated'
                                      │
                                      ▼
                             fraud-detection-service (:8082)
                             [3 Fraud Rules + Concurrent Sliding Window]
                                      │
                                      ▼ Kafka topic: 'fraud.decision'
                                ┌─────┴────────────────┐
                                ▼                      ▼
                     payment-service (:8081)    notification-service (:8083)
                     [Update status in DB:       [Persists CRITICAL alert
                      APPROVED or FLAGGED]        for audit & compliance]
```

### Microservices Breakdown
* **`payment-service`** (`:8081`): Ingests payment requests, guarantees idempotency, persists state in PostgreSQL, publishes to `payment.initiated`, and handles state transitions upon fraud decisions.
* **`fraud-detection-service`** (`:8082`): Evaluates real-time transaction velocity using an in-memory sliding window deque (`ConcurrentHashMap<String, Deque<Instant>>`), checks high-value risk thresholds ($10k+), and flags currency anomalies. Emits decisions to `fraud.decision`.
* **`notification-service`** (`:8083`): Asynchronously listens to `fraud.decision` and records audit alerts for flagged or high-risk payments.

---

## ✨ Key Technical Highlights

* **Transaction Idempotency:** Prevents double charging during retries using idempotency keys backed by PostgreSQL constraints and Kafka idempotent producer configs (`enable.idempotence=true`, `acks=all`).
* **Stateful Fraud Detection:** Sliding-window rate limiting purges expired timestamps (>60s) atomically, catching velocity spikes (>3 txn/min) without database table scans.
* **Fault Isolation & Backpressure:** Asynchronous Kafka messaging ensures downstream delays never block client payment submissions.
* **Resilient Schema:** Multi-database segregation on PostgreSQL 16 (`payflow_payments` and `payflow_notifications`).
* **Containerized Deployment:** Entire ecosystem (Zookeeper, Kafka, PostgreSQL, 3 microservices) orchestrates via a single Docker Compose command.

---

## 🚀 Quick Start

### Prerequisites
* **Docker** & **Docker Compose**
* **JDK 21** & **Maven 3.9+** (for local development)

### 1. Clone the repository
```bash
git clone https://github.com/shubhamraghawant/payflow.git
cd payflow
```

### 2. Run with Docker Compose
```bash
docker compose up --build -d
```

### 3. Verify services
* `payment-service`: http://localhost:8081/swagger-ui.html
* `fraud-detection-service`: http://localhost:8082/actuator/health
* `notification-service`: http://localhost:8083/swagger-ui.html

---

## 📡 API Usage Examples

### 1. Initiate a Payment
```bash
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "user_101",
    "receiverId": "merchant_505",
    "amount": 250.00,
    "currency": "USD",
    "idempotencyKey": "idem_abc_123"
  }'
```

### 2. Check Payment Status
```bash
curl http://localhost:8081/api/v1/payments/{payment-id}
```

### 3. List Audit Notifications
```bash
curl http://localhost:8083/api/v1/notifications
```

---

## 🧪 Testing

Run all unit and integration tests across all microservices:
```bash
mvn clean test
```

---

## 📄 License
This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
