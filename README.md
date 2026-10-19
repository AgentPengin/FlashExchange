# ⚡ FlashExchange & Launchpad Core

[![Java](https://img.shields.io/badge/Java-21_LTS-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.4-red.svg)](https://redis.io/)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-orange.svg)](https://www.rabbitmq.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose_Ready-blue.svg)](https://www.docker.com/)
[![k6](https://img.shields.io/badge/Load_Testing-k6-purple.svg)](https://k6.io/)

> A high-performance, fault-tolerant distributed cryptocurrency matching engine and flash sale launchpad, engineered with an event-driven architecture, in-memory execution, atomic stock deduction, and real-time observability.

---

## 🚀 Key Architectural Highlights

- **Station 1 — Rate Limiter Gateway**: Custom Token Bucket algorithm running as a Redis Lua script. Multi-tiered rate limiting (VIP: 20 req/s, Guest: 5 req/s) responding with HTTP 429 in under 10ms.
- **Station 2 — Flash Sale Engine**: In-memory atomic stock decrement with Redis Lua scripts (guaranteeing 0% oversell), asynchronous load-leveling via RabbitMQ, and automated 10s timeout rollbacks via Dead Letter Exchanges (DLX).
- **Station 3 — In-Memory Matching Engine**: Price-Time Priority (FIFO) matching engine utilizing `TreeMap` and `ArrayDeque`, delivering sub-millisecond trade executions with real-time WebSocket / STOMP trade broadcast.
- **Station 4 — Persisted Ledger & Accounting**: PostgreSQL-backed double-entry bookkeeping ledger with strict idempotency barriers preventing duplicate debiting.
- **Full Observability & Load Testing**: Integrated Spring Boot Actuator, Micrometer, Prometheus, and Grafana dashboards; stress-tested with Grafana k6 over 27,000 requests.

---

## 🛠️ Technology Stack

- **Backend Runtime**: Java 21 LTS, Spring Boot 3.3.4
- **In-Memory & Cache**: Redis 7, Lua Scripting
- **Message Broker**: RabbitMQ 3 (Direct Exchange, Message TTL, Dead Letter Exchange)
- **Database**: PostgreSQL 16 (Double-entry ledger schema)
- **Real-time Engine**: Spring WebSocket / STOMP
- **Telemetry & Monitoring**: Prometheus, Grafana, Spring Boot Actuator, Micrometer
- **Load Testing**: Grafana k6
- **Containerization**: Docker, Docker Compose (Multi-stage build)

---

## ⚡ Quick Start (1-Click Deployment)

Ensure **Docker** and **Docker Compose** are installed on your machine.

```bash
# 1. Clone the repository
git clone https://github.com/your-username/FlashExchange.git
cd FlashExchange

# 2. Spin up all 6 services with a single command
docker compose up -d
```

All interconnected services will be initialized automatically:
- **API Server**: [http://localhost:8080](http://localhost:8080)
- **Prometheus Scrape Targets**: [http://localhost:9090/targets](http://localhost:9090/targets)
- **Grafana Live Dashboard**: [http://localhost:3000](http://localhost:3000) (`admin` / `admin`)
- **RabbitMQ Management**: [http://localhost:15672](http://localhost:15672) (`guest` / `guest`)

---

## 📡 Core API Reference

### 1. Flash Sale Purchase
```bash
POST /api/v1/flashsale/buy?itemKey=FLASH_PASS
Header: X-User-Id: user-vip-1
```
- `201 Created`: Stock deducted atomically; purchase enqueued for async settlement.
- `429 Too Many Requests`: Rate limiter triggered (Retry-After header included).
- `410 Gone`: Product sold out (0% oversell guarantee).

### 2. Matching Engine — Place Order
```bash
POST /api/v1/orders
Content-Type: application/json

{
  "userId": "trader-1",
  "symbol": "FLASH-USDT",
  "side": "BUY",
  "type": "LIMIT",
  "price": 15,
  "quantity": 10
}
```

### 3. OrderBook Market Depth
```bash
GET /api/v1/orders/depth
```

---

## 🧪 Testing & Verification

### Run Automated Unit & End-to-End Tests
```bash
mvn test
```
*Executes the complete lifecycle flow: Rate Limiter $\rightarrow$ Flash Sale $\rightarrow$ RabbitMQ Worker $\rightarrow$ PostgreSQL Ledger $\rightarrow$ OrderBook Matching.*

### Run Distributed Load Test (k6)
```bash
docker run --rm -i -v "${PWD}/monitoring:/scripts" grafana/k6 run /scripts/load_test_flashsale.js
```

---

## 📖 In-Depth Architecture & Design Trade-offs
For an exhaustive analysis of data structures, race condition preventions, distributed consensus trade-offs, and disaster recovery strategies, see [ARCHITECTURE.md](./ARCHITECTURE.md).