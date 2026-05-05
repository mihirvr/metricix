# Metricix — Non-Functional Requirements (NFR)

**Document Version:** 1.1
**Scope:** System quality attributes, performance targets, and operational constraints.

---

## NFR-1: Performance & Latency

### NFR-1.1 — P95 Latency Target
95% of all successful `POST /api/v1/track` requests **MUST** complete in **less than 15 milliseconds** of server-side processing time, measured from request receipt to `202 Accepted` response dispatch.

**Measurement Methodology:** Latency MUST be measured using an **Open Model (Constant Arrival Rate)** load test. This ensures we are measuring the Netty/WebFlux processing speed, not the artificial delay caused by the operating system's TCP socket queue backing up under unbounded concurrency.

Measurement boundary:

| Included | Excluded |
|---|---|
| Payload deserialization and validation | Network RTT between client and server |
| Auth and rate limit checks (WebFilter) | TLS handshake time |
| Redis `RPUSH` (non-blocking, Lettuce) | |
| Response serialization and dispatch | |

Throughput degradation or latency regression beyond this threshold on the specified hardware profile (see NFR-1.2) is treated as a **P1 regression** and must block release.

### NFR-1.2 — Throughput Floor
The application **MUST** comfortably sustain **> 1,000 requests per second (RPS)** on a single baseline container (1 vCPU, 1 GB RAM).

**Validation:** The system must process exactly 1,000 RPS for a sustained duration of at least 60 seconds with a **0.00% failure rate** and without breaching the NFR-1.1 latency target.

| Resource | Limit |
|---|---|
| CPU | 1 vCPU |
| RAM | 1 GB |

This maps to the AWS `t3.micro` / `t3.small` target deployment instance (see deployment guide in `README.md`).

---

## NFR-2: Scalability

### NFR-2.1 — Stateless Application Design
The Spring WebFlux application container **MUST** store **zero local state**. Specifically:

- No in-memory event queues or caches local to the JVM instance.
- No sticky sessions or instance-affinity requirements.
- All shared state (rate limit counters, event queue, DLQ) lives exclusively in Redis.

Multiple application instances **MUST** be deployable behind a load balancer without any configuration changes or inter-instance coordination.

### NFR-2.2 — Redis as Single Source of Truth for Shared State
All state that must be consistent across instances (rate limit tokens, queue contents) **MUST** reside in Redis and be accessed via atomic Redis operations. JVM-local approximations are not acceptable.

---

## NFR-3: Observability & Logging

### NFR-3.1 — Structured JSON Logging
All application logs **MUST** be emitted in **JSON format** via **SLF4J / Logback**. Log entries must be structured for direct ingestion by log aggregation platforms without transformation.

Compatible targets:
- **Datadog** — Datadog Agent with JSON log collection enabled
- **ELK Stack** — Filebeat → Logstash → Elasticsearch
- **Grafana Loki** — Promtail with Docker log driver

Minimum required fields per log entry:

| Field | Description |
|---|---|
| `timestamp` | ISO-8601 UTC |
| `level` | `INFO`, `WARN`, `ERROR` |
| `message` | Human-readable description |
| `service` | `metricix` (static) |
| `traceId` | Correlation ID (if propagated) |

### NFR-3.2 — Prometheus Metrics Endpoint
The system **MUST** expose a Prometheus-compatible scrape endpoint at:

```
GET /actuator/prometheus
```

Required metrics:

| Metric Name | Type | Description |
|---|---|---|
| `metricix_events_ingested_total` | Counter | Total events pushed to the Redis buffer |
| `metricix_batch_size_written` | Histogram | Distribution of records per DB flush cycle |
| `metricix_dlq_events_total` | Counter | Total events routed to the Dead Letter Queue |
| `webflux_requests_active` | Gauge | Active in-flight WebFlux request count |
| `jvm_*` | Various | Standard JVM metrics (heap, GC, threads) |

`metricix_dlq_events_total` **MUST** be incremented by the Sweeper (FR-5.2) whenever a batch is pushed to `metricix_dlq`. This metric is the primary alerting signal for DB connectivity failures.

---

## NFR-4: Deployability

### NFR-4.1 — Multi-Stage Dockerfile
The system **MUST** include a **multi-stage Dockerfile** that:

1. **Build stage:** Compiles the application JAR using a full JDK 21 image.
2. **Runtime stage:** Copies only the compiled JAR into a minimal base image.

Acceptable minimal runtime base images:
- `eclipse-temurin:21-jre-alpine`
- Google Distroless Java 21

The final Docker image size **MUST NOT** exceed **250 MB**.

### NFR-4.2 — Schema Management via Flyway
All database schema changes **MUST** be managed through **Flyway migrations**. Migrations execute automatically on startup (`spring.flyway.enabled=true`).

Migration file naming convention:
```
V{version}__{description}.sql
e.g., V1__create_metricix_events.sql
```

Manual DDL execution against any environment (dev, staging, production) is explicitly **not permitted**.

### NFR-4.3 — Docker Compose Stack
A `docker-compose.yml` **MUST** be provided that:

- Brings up `redis:7-alpine`, `postgres:16-alpine`, and the `metricix-api` container.
- Uses `depends_on` with `service_healthy` conditions so the API only starts after Redis and PostgreSQL pass their healthchecks.
- Mounts a named Docker volume for PostgreSQL data persistence across restarts.
- Accepts all runtime configuration via environment variables (no hardcoded secrets in the Compose file).

Full `docker-compose.yml` template and EC2 deployment walkthrough are in `README.md`.
