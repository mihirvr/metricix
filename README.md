<div align="center">

# ⚡ Metricix 
### (pronounced: meh-TRIK-iks)

🔴 **Live Portal:** [https://metricix.mihirr.in](https://metricix.mihirr.in)
<br>
📖 **Read the [User Guide](USER_GUIDE.md)** for UI instructions and API documentation.

### High-Performance, Self-Hosted Telemetry & Event Analytics Engine

[![Java](https://img.shields.io/badge/Java-24-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7+-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![AWS EC2](https://img.shields.io/badge/AWS-EC2-FF9900?style=flat-square&logo=amazon-aws&logoColor=white)](https://aws.amazon.com/ec2/)
[![Nginx](https://img.shields.io/badge/Nginx-Proxy-269539?style=flat-square&logo=nginx&logoColor=white)](https://www.nginx.com/)
[![Vercel](https://img.shields.io/badge/Vercel-Edge-black?style=flat-square&logo=vercel&logoColor=white)](https://vercel.com/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-CSS-38B2AC?style=flat-square&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![Chart.js](https://img.shields.io/badge/Chart.js--FF6384?style=flat-square&logo=chart.js&logoColor=white)](https://www.chartjs.org/)

**P95 Latency &lt; 15ms &nbsp;|&nbsp; &gt; 1,000 req/sec on 1 vCPU &nbsp;|&nbsp; Zero Silent Data Loss**


</div>
<img width="1123" height="842" alt="copy Untitled-2026-05-06-2027" src="https://github.com/user-attachments/assets/2a07811b-a68e-4a98-b874-735462e245ca" />

---

## Overview

Metricix is a **self-hosted, non-blocking telemetry ingestion and analytics engine** built for engineering teams that need full data ownership, predictable latency, and high throughput — without the cost or lock-in of third-party analytics platforms.

Built natively on **Spring WebFlux (Project Reactor)**, every operation in the ingestion path is fully asynchronous. Incoming events are immediately buffered in **Redis** and flushed to **PostgreSQL** in bulk by a background worker — decoupling API response time from database write performance entirely. The system now includes a real-time dashboard to visualize event data as it arrives.

---

## ☁️ Cloud Architecture

The Metricix engine is deployed in a decoupled, scalable cloud architecture for production-grade reliability and performance.

- **Backend Services (AWS EC2):** The core backend services (Spring Boot API, PostgreSQL, Redis) are containerized using Docker and run on a dedicated AWS EC2 instance.
- **Secure API Gateway (Nginx):** An Nginx reverse proxy is deployed on the same EC2 instance, serving as the public-facing gateway. It terminates SSL/TLS, provides HTTPS for the API endpoint (`api.mihirr.in`), and forwards traffic to the Spring Boot application.
- **Frontend Hosting (Vercel):** The frontend dashboard is a decoupled static application hosted on Vercel's global Edge Network. This ensures fast load times for users worldwide and separates the UI from the backend infrastructure.

This architecture ensures that the backend is securely isolated, while the frontend is globally distributed for optimal user experience.

---

## ✨ Features

- **Non-blocking I/O** — Built on Project Reactor; zero JVM thread blocking on the hot path for both reads and writes.
- **Interactive Dashboard with Chart.js** — A real-time, browser-based UI to visualize event volume and trends.
- **Immediate `202 Accepted`** — API responds before any database interaction occurs on ingestion.
- **Redis-backed buffer** — Events are atomically queued and drained; no data loss on DB failures.
- **Bulk PostgreSQL writes** — A single `INSERT ... VALUES (), (), ()` per batch cycle, not N individual inserts.
- **Dead Letter Queue (DLQ) & Archival** — Failed batches are preserved in `metricix_dlq`. Works alongside the soft-delete archival system to ensure no data is ever lost.
- **High-Throughput Symmetric Auth** — Per-key validation with `mtx_pub_` prefix enforcement.
- **Multi-tenant Discovery** — An API endpoint (`/api/v1/tenants`) to automatically discover active tenants for UI population.
- **Per-key Rate Limiting** — Token bucket algorithm, Redis-backed, works correctly across multiple instances.
- **Stateless & horizontally scalable** — Deploy N instances behind a load balancer with no config changes.
- **Prometheus metrics** — Custom ingestion counters and batch size histograms out of the box.
- **Structured JSON logging** — Compatible with Datadog, ELK, and Grafana Loki.
- **Flyway schema management** — Schema is provisioned automatically on startup; no manual DDL.
---
## 🖥️ User Interface (Frontend Portals)

Metricix includes a premium, Tailwind-styled frontend suite consisting of two dedicated portals, featuring persistent Light/Dark mode and responsive design.

### 1. The Developer Emitter (`index.html`)
A robust testing portal designed to simulate real-world traffic patterns without needing Postman or external scripts.
* **Traffic Simulation:** Queue up to 5 different event types with specific quantities.
* **Execution Modes:** Fire events sequentially or shuffle them into randomized chaotic traffic.
* **Smart Inputs:** Dropdown presets for API keys and event types with seamless custom input fallbacks.
* **Live Console:** A built-in stylized terminal logging request/response cycles in real-time.

### 2. The Analytics Hub (`dashboard.html`)
An administrative dashboard for visualizing telemetry streams dynamically.
* **Automated Discovery:** Automatically fetches available tenants and binds required API keys.
* **Interactive Visualization:** Powered by Chart.js. Features Bar Charts (Event Volume) and Line Charts (Time Series).
* **Deep Data Exploration:** Supports mouse-wheel zooming, click-and-drag panning, and dynamic time-binning (Per Hour vs. Per Day).
* **The Danger Zone:** UI-driven soft-deletion with confirmation modals for safe tenant purging.
---
## ⚡ Performance Benchmarks

Metricix is built for extreme throughput and low latency. To validate our Non-Functional Requirements (NFRs), the engine was stress-tested using **k6**.

### The Benchmark (Sustained Load Test)
To accurately measure the application's true processing latency without being bottlenecked by the local operating system's TCP network queue, we utilize an **Open Model (Constant Arrival Rate)** test. 

Instead of spamming the server as fast as possible (which tests network bridge limits, capping around ~2,500 RPS locally with a queued P95 of ~218ms), the Constant Arrival Rate model guarantees exactly 1,000 requests are dispatched every second. 

**Results (Local Windows WSL2/Docker Environment):**
* **Target Load:** 1,000 Requests Per Second (RPS)
* **Duration:** 60 seconds
* **Events Processed:** 59,985
* **Failure Rate:** 0.00%
* **P95 Latency:** **5.62 ms**

**Conclusion:** Under a sustained, production-grade load of 1,000 RPS, the Spring WebFlux + Redis Lettuce architecture successfully validates, decorates, and buffers telemetry payloads in under 6 milliseconds.
---
## Architecture

### Pipeline Flow

```
┌──────────────────────────────┐   ┌─────────────────────────────────────────────────────────────────┐
│    CLIENT APP (Ingestion)    │   │                   CLIENT APP (Dashboard)                      │
└───────────────┬──────────────┘   └───────────────────────────────┬─────────────────────────────────┘
                │ POST /api/v1/track                               │ GET /api/v1/tenants
                │ X-API-Key: mtx_pub_***                           │ GET /api/v1/events?limit=50
                ▼                                                  │
┌─────────────────────────────────────────────────────────────────┐│
│                    SPRING WEBFLUX (Netty)                       ││
│                                                                 ││
│   WebFilter: Rate Limiting  ──►  WebFilter: Symmetric Key Lookup      ││
│                  │                      │                       ││
│     (Write Path) │         (Read Path)  │                       ││
│                  ▼                      ▼                       ││
│     Validate + Decorate Payload      Query Controller           │◄┘
│ (append received_at, client_ip)                                 │
└─────────────────┬───────────────────────────────────────────────┘
                  │                                      │
      Reactive RPUSH (Lettuce)                           │ R2DBC SELECT
                  │                                      │ (WHERE is_deleted=FALSE)
                  ▼                                      │
┌──────────────────────────────────┐                     │
│      REDIS 7+ (Event Buffer)     │                     │
│                                  │                     │
│    LIST: metricix_events_queue   │                     │
│    LIST: metricix_dlq            │                     │
└─────────────────┬────────────────┘                     │
                  │                                      │
◄── 202 Accepted ─┘                                      │
                  │                                      │
     [Background @Scheduled, every 5s]                   │
                  │                                      │
     RENAME queue → processing queue (atomic)            │
                  │                                      │
          LRANGE + deserialize                           │
                  │                                      │
                  ▼                                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SPRING DATA R2DBC (Non-blocking)              │
│                                                                 │
│   Single bulk INSERT INTO metricix_events VALUES (…),(…),(…)   │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                        POSTGRESQL 16+                           │
│                                                                 │
│   Table: metricix_events    (schema managed by Flyway)          │
└─────────────────────────────────────────────────────────────────┘

  On DB failure:  batch ──► metricix_dlq   (no silent drops, ever)
```

### Tech Stack at a Glance

| Layer | Technology | Notes |
|---|---|---|
| Runtime | Java 24 | Virtual threads available |
| Framework | Spring Boot 3.2+ | WebFlux, Actuator, Scheduling |
| Reactive Web | Spring WebFlux (Project Reactor) | Netty server, non-blocking throughout |
| Cache / Queue | Redis 7+ | Event buffer, rate limit state, DLQ |
| Redis Client | Lettuce | Reactive driver via `spring-boot-starter-data-redis-reactive` |
| Database | PostgreSQL 16+ | Persistent event store |
| DB Driver | R2DBC (`r2dbc-postgresql`) | Fully non-blocking SQL |
| Schema Migrations | Flyway | Auto-applied on startup |
| Logging | SLF4J + Logback | Structured JSON output |
| Metrics | Micrometer + Prometheus | Exposed at `/actuator/prometheus` |
| Containerization | Docker (multi-stage) | JRE Alpine or Distroless runtime image |

---

## Requirements Summary

### Functional Requirements

| # | Requirement | Detail |
|---|---|---|
| FR-1 | Ingestion endpoint | `POST /api/v1/track` accepts `event_type` (required), `payload` (required), `url` (optional) |
| FR-2 | Immediate acknowledgement | Returns `202 Accepted` after Redis `RPUSH`; never waits for PostgreSQL |
| FR-3 | Data decoration | Server appends `received_at` (UTC timestamp) and `client_ip` before queuing |
| FR-4 | API Key auth | `X-API-Key` header required; must match `mtx_pub_` prefix; invalid keys → `401` |
| FR-5 | Atomic batch drain | Sweeper uses `RENAME` pattern to atomically swap queue; prevents race conditions |
| FR-6 | Bulk DB insert | Entire batch written in a single SQL statement via R2DBC |
| FR-7 | Dead Letter Queue | DB failures push batch to `metricix_dlq`; no event is ever silently discarded |
| FR-8 | Rate limiting | Token bucket per API key, Redis-backed; exceeding threshold → `429` |

### Non-Functional Requirements

| # | Requirement | Target |
|---|---|---|
| NFR-1 | P95 Latency | **< 15ms** server-side for `POST /api/v1/track` |
| NFR-2 | Throughput | **> 1,000 req/sec** sustained on 1 vCPU / 1 GB RAM |
| NFR-3 | Stateless design | Zero local JVM state; all shared state lives in Redis |
| NFR-4 | Horizontal scaling | N instances behind load balancer, no coordination required |
| NFR-5 | Structured logging | JSON logs compatible with Datadog, ELK, Grafana Loki |
| NFR-6 | Observability | Prometheus endpoint exposing custom ingestion metrics |
| NFR-7 | Containerization | Multi-stage Docker image, final size < 250 MB |
| NFR-8 | Schema management | Flyway migrations, auto-applied; no manual DDL permitted |

---

## 🚀 Quick Start (Local Development)

**Prerequisites:** Docker and Docker Compose installed locally.

```bash
# 1. Clone the repository
git clone https://github.com/your-org/metricix.git
cd metricix

# 2. Start the full stack (app + Redis + PostgreSQL)
docker compose up -d

# 3. Verify the service is healthy
curl http://localhost:8080/actuator/health
```

Flyway applies the `metricix_events` schema automatically on first boot. No manual database setup is required.

---

## 📡 API Reference

### `POST /api/v1/track`

Ingests a single event. Returns immediately after buffering to Redis.

**Request**

```http
POST /api/v1/track HTTP/1.1
Host: api.mihirr.in
X-API-Key: mtx_pub_8f92a4b1c
Content-Type: application/json

{
  "event_type": "user_signup",
  "url": "https://myapp.com/register",
  "payload": {
    "referral_source": "twitter",
    "browser": "Chrome",
    "subscription_tier": "pro"
  }
}
```

**Request Body Schema**

| Field | Type | Required | Description |
|---|---|---|---|
| `event_type` | `string` | ✅ | Event identifier (e.g. `page_view`, `checkout_click`) |
| `payload` | `object` | ✅ | Arbitrary JSON data |
| `url` | `string` | ❌ | Origin URL of the event |

**Response Reference**

| Status | Condition | Body |
|---|---|---|
| `202 Accepted` | Event buffered successfully | `{ "status": "buffered", "timestamp": "…" }` |
| `400 Bad Request` | Missing required fields | `{ "error": "validation_failed", "message": "…" }` |
| `401 Unauthorized` | Missing or invalid API key | `{ "error": "unauthorized", "message": "…" }` |
| `429 Too Many Requests` | Rate limit exceeded for key | `{ "error": "rate_limit_exceeded", "message": "…" }` |

**Success Response**

```json
{
  "status": "buffered",
  "timestamp": "2025-05-03T10:00:05.123Z"
}
```

**cURL Example**

```bash
curl -s -X POST https://api.mihirr.in/api/v1/track \
  -H "X-API-Key: mtx_pub_your_key_here" \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "button_click",
    "url": "https://myapp.com/pricing",
    "payload": {
      "button_id": "cta_upgrade",
      "user_id": "usr_a4f92"
    }
  }'
```

---

## ☁️ Deployment: Single-Node AWS EC2

This guide deploys the full Metricix stack (API + Redis + PostgreSQL) on a single EC2 instance using Docker Compose. Suitable for development, staging, and low-to-medium production workloads.

### Step 1 — Provision EC2 Instance

Launch an EC2 instance with the following configuration:

| Setting | Value |
|---|---|
| AMI | Ubuntu 24.04 LTS (`ubuntu-noble-24.04-amd64-server`) |
| Instance Type | `t3.micro` (dev/staging) or `t3.small` (production) |
| Storage | 20 GB gp3 EBS minimum |
| Security Group — Inbound | Port `22` (SSH) from your IP; Port `80` (HTTP) and `443` (HTTPS) from `0.0.0.0/0` |
| Security Group — Outbound | All traffic |

> **Note:** For production, restrict port `8080` to your load balancer or VPC CIDR rather than `0.0.0.0/0`. Never expose ports `5432` (PostgreSQL) or `6379` (Redis) to the public internet.

---

### Step 2 — Connect & Install Dependencies

SSH into your instance and install Docker:

```bash
# Connect
ssh -i your-key.pem ubuntu@<EC2_PUBLIC_IP>

# Update package index
sudo apt update && sudo apt upgrade -y

# Install Docker engine and the Compose v2 plugin
sudo apt install -y docker.io docker-compose-v2

# Add current user to docker group (avoids needing sudo for docker commands)
sudo usermod -aG docker $USER

# Apply group change without logging out
newgrp docker

# Verify installation
docker --version
docker compose version
```

---

### Step 3 — Configure Environment

Create a working directory and a `.env` file for secrets:

```bash
mkdir ~/metricix && cd ~/metricix

cat > .env << 'EOF'
# PostgreSQL
POSTGRES_DB=metricix
POSTGRES_USER=metricix_user
POSTGRES_PASSWORD=change_me_in_production

# Spring R2DBC
SPRING_R2DBC_URL=r2dbc:postgresql://postgres:5432/metricix
SPRING_R2DBC_USERNAME=metricix_user
SPRING_R2DBC_PASSWORD=change_me_in_production

# Redis
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379

# Metricix App
BATCH_INTERVAL_MS=5000
RATE_LIMIT_RPS=200
EOF
```

> ⚠️ **Security:** Do not commit `.env` to version control. For production, use AWS Secrets Manager or SSM Parameter Store and inject values at runtime.

---

### Step 4 — Create `docker-compose.yml`

```bash
cat > docker-compose.yml << 'EOF'
version: "3.9"

services:

  redis:
    image: redis:7-alpine
    container_name: metricix-redis
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  postgres:
    image: postgres:16-alpine
    container_name: metricix-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5

  metricix-api:
    image: ghcr.io/your-org/metricix:latest   # Replace with your registry path
    container_name: metricix-api
    restart: unless-stopped
    env_file: .env
    environment:
      SPRING_R2DBC_URL: ${SPRING_R2DBC_URL}
      SPRING_R2DBC_USERNAME: ${SPRING_R2DBC_USERNAME}
      SPRING_R2DBC_PASSWORD: ${SPRING_R2DBC_PASSWORD}
      SPRING_DATA_REDIS_HOST: ${SPRING_DATA_REDIS_HOST}
      SPRING_DATA_REDIS_PORT: ${SPRING_DATA_REDIS_PORT}
      BATCH_INTERVAL_MS: ${BATCH_INTERVAL_MS}
      RATE_LIMIT_RPS: ${RATE_LIMIT_RPS}
      SERVER_PORT: 8080
    ports:
      - "127.0.0.1:8080:8080" # Bind to localhost only
    depends_on:
      redis:
        condition: service_healthy
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 30s    # Allow time for Flyway migrations on first boot

volumes:
  postgres_data:
    driver: local
EOF
```

---

### Step 5 — Start the Stack

```bash
# Pull images and start all services in detached mode
docker compose up -d

# Tail logs to confirm healthy startup and Flyway migration
docker compose logs -f metricix-api
```

Expected output on first boot:

```
metricix-api  | Flyway: Successfully applied 1 migration to schema "public"
metricix-api  | Started MetricixApplication in 3.2 seconds
metricix-api  | Netty started on port 8080
```

---

### Step 6 — Verify Deployment

```bash
# Health check (from the EC2 instance itself)
curl http://localhost:8080/actuator/health

# Send a test event via the public Nginx proxy
curl -s -X POST https://api.mihirr.in/api/v1/track \
  -H "X-API-Key: mtx_pub_your_key_here" \
  -H "Content-Type: application/json" \
  -d '{"event_type": "deployment_test", "payload": {"env": "ec2"}}'
```

Expected health response:
```json
{ "status": "UP" }
```

---

### Operational Reference

```bash
# View running containers and health status
docker compose ps

# Stop the stack (data is preserved in volumes)
docker compose down

# Stop and wipe all persisted data (destructive — use with caution)
docker compose down -v

# Restart only the API service (e.g. after image update)
docker compose pull metricix-api && docker compose up -d metricix-api

# Connect to PostgreSQL database
docker compose exec postgres psql -U metricix_user -d metricix

# Inspect the Dead Letter Queue
docker exec metricix-redis redis-cli LRANGE metricix_dlq 0 -1

# Stream application logs
docker compose logs -f metricix-api
```

---

## ⚙️ Configuration Reference

All application settings are controlled via environment variables. No application config files need to be edited.

| Variable | Default | Description |
|---|---|---|
| `BATCH_INTERVAL_MS` | `5000` | Sweeper flush interval in milliseconds |
| `RATE_LIMIT_RPS` | `200` | Max requests per second per API key |
| `REDIS_QUEUE_KEY` | `metricix_events_queue` | Redis list key for the active event buffer |
| `SPRING_R2DBC_URL` | — | PostgreSQL R2DBC connection string |
| `SPRING_R2DBC_USERNAME` | — | PostgreSQL username |
| `SPRING_R2DBC_PASSWORD` | — | PostgreSQL password |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Redis hostname |
| `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `SERVER_PORT` | `8080` | HTTP server port |

---

## 📊 Observability

### Endpoints

| Endpoint | Method | Description |
|---|---|---|
| `/actuator/health` | `GET` | Liveness and readiness status |
| `/actuator/prometheus` | `GET` | Prometheus metrics scrape endpoint |

### Custom Metrics

| Metric | Type | Description |
|---|---|---|
| `metricix_events_ingested_total` | Counter | Total events pushed to the Redis buffer |
| `metricix_batch_size_written` | Histogram | Distribution of records per DB flush cycle |
| `metricix_dlq_events_total` | Counter | Total events routed to the Dead Letter Queue |
| `webflux_requests_active` | Gauge | Currently in-flight WebFlux requests |

### Structured Logging

All logs are emitted as JSON via SLF4J/Logback. Compatible with:

- **Datadog** — Datadog Agent with JSON log collection enabled.
- **ELK Stack** — Filebeat → Logstash → Elasticsearch.
- **Grafana Loki** — Promtail with the Docker log driver.

---

## 🗃 Dead Letter Queue

Failed database batches are never dropped. They are preserved in the `metricix_dlq` Redis list for inspection and replay.

```bash
# Inspect DLQ contents
redis-cli LRANGE metricix_dlq 0 -1

# Count pending failed events
redis-cli LLEN metricix_dlq
```

> **DLQ Replay:** Automated replay is a post-MVP feature. To recover: diagnose and resolve the DB connectivity issue, then manually re-queue events from `metricix_dlq` back into `metricix_events_queue`.

---

## 📁 Documentation

| File | Description |
|---|---|
| [`docs/PRD.md`](docs/PRD.md) | Full product requirements, data schema, and API specification |
| [`docs/FR.md`](docs/FR.md) | Detailed functional requirements |
| [`docs/NFR.md`](docs/NFR.md) | Non-functional requirements (performance, scalability, operations) |
| [`docs/MVP.md`](docs/MVP.md) | MVP scope definition, exit criteria, and deferred features |

---

## 🗺 Roadmap

The following are explicitly **out of scope for the current MVP**:

- [ ] Multi-node Redis clustering
- [ ] Automated DLQ replay
- [ ] API key management portal
- [ ] Client SDKs (JavaScript, Python, Go)
- [ ] Multi-tenancy UI

---

## 🤝 Contributing

Contributions are welcome. Please open an issue first to discuss significant changes. Ensure all PRs include tests and pass the existing suite before requesting review.

---

## 📄 License

[MIT](LICENSE) — © 2025 Metricix Contributors
