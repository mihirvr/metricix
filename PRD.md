# Metricix Telemetry Engine — Product Requirements Document

**Document Version:** 1.1
**Product Name:** Metricix
**Architecture:** Reactive JVM (Java 21+, Spring WebFlux)

---

## Executive Summary

Metricix is a self-hosted, extremely high-throughput telemetry and event analytics engine built natively for the Java ecosystem. Transitioning from the original Python/FastAPI architecture, Metricix utilizes **Spring WebFlux (Project Reactor)** to achieve purely non-blocking, asynchronous I/O operations.

By buffering incoming event payloads in **Redis** before bulk-inserting them into **PostgreSQL** via a background worker, Metricix ensures that API latency remains consistently **under 15ms**, even under extreme traffic spikes (e.g., thousands of concurrent users interacting with client applications).

---

## Tech Stack at a Glance

| Layer | Technology | Notes |
|---|---|---|
| Runtime | Java 21 (LTS) | Virtual threads available |
| Framework | Spring Boot 3.2+ | WebFlux, Actuator, Scheduling |
| Reactive Web | Spring WebFlux (Project Reactor) | Netty server, non-blocking throughout |
| Cache / Queue | Redis 7+ | Event buffer, rate limit state, DLQ |
| Redis Client | Lettuce | Via `spring-boot-starter-data-redis-reactive` |
| Database | PostgreSQL 16+ | Persistent event store |
| DB Driver | R2DBC (`r2dbc-postgresql`) | Fully non-blocking SQL |
| Migrations | Flyway | `flyway-core` + `flyway-database-postgresql` |
| Logging | SLF4J / Logback | Structured JSON output |
| Metrics | Spring Actuator + Micrometer | Prometheus endpoint at `/actuator/prometheus` |
| Containerization | Docker (multi-stage) | JRE Alpine or Distroless runtime image |
| Security | Spring Security | Symmetric API Key Auth for minimal latency overhead. |

---

## Data Architecture

### PostgreSQL Schema — `metricix_events`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY | Auto-generated unique identifier |
| `tenant_id` | VARCHAR(64) | NOT NULL, INDEX | Derived from the API Key |
| `event_type` | VARCHAR(128) | NOT NULL, INDEX | e.g., `page_view`, `checkout_click` |
| `url` | VARCHAR(2048) | NULL | Origin of the event |
| `payload` | JSONB | NOT NULL | Unstructured/flexible data payload |
| `client_ip` | VARCHAR(45) | NULL | Captured client IP address |
| `created_at` | TIMESTAMPTZ | DEFAULT NOW(), INDEX | Time the event was ingested |
| `is_deleted` | BOOLEAN | DEFAULT FALSE | Soft-delete flag for data archival |

### Flyway Migration Naming Convention

```
V{version}__{description}.sql
e.g., V1__create_metricix_events.sql
```

Migrations execute automatically on startup. Manual DDL against any environment is not permitted.

---

## API Specification

### `POST /api/v1/track`

**Request:**
```http
POST /api/v1/track HTTP/1.1
Host: telemetry.yourdomain.com
X-API-Key: mtx_pub_8f92a4b1c
# Uses symmetric key verification to maintain < 15ms ingestion latency.
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

**Responses:**

| Status | Condition |
|---|---|
| `202 Accepted` | Event successfully buffered to Redis |
| `400 Bad Request` | Missing required fields |
| `401 Unauthorized` | Missing or invalid `X-API-Key` |
| `429 Too Many Requests` | Rate limit exceeded for key |

**Success Response (202):**
```json
{
  "status": "buffered",
  "timestamp": "2025-05-03T10:00:05.123Z"
}
```

---

### `GET /api/v1/tenants`

**Description:** Returns a list of unique tenant IDs that have sent events, for populating UI elements.

**Request:**
```http
GET /api/v1/tenants HTTP/1.1
Host: telemetry.yourdomain.com
X-API-Key: mtx_pub_...
```

**Success Response (200):**
```json
[
  "tenant_a",
  "tenant_b",
  "tenant_c"
]
```

---

### `GET /api/v1/events`

**Description:** Fetches a paginated stream of historical events for a given tenant.

**Request:**
```http
GET /api/v1/events?limit=50&offset=100 HTTP/1.1
Host: telemetry.yourdomain.com
X-API-Key: mtx_pub_...
```

**Query Parameters:**
- `limit` (integer, optional, default: 100): Number of events to return.
- `offset` (integer, optional, default: 0): Number of events to skip for pagination.
- `event_type` (string, optional): Filter events by a specific type.

**Success Response (200):**
```json
{
  "data": [
    {
      "id": "...",
      "event_type": "page_view",
      "created_at": "...",
      "payload": {}
    }
  ],
  "has_more": true
}
```

---

### `DELETE /api/v1/purge`

**Description:** Performs a "soft delete" on all events associated with the API key's tenant. This is a data archival action and does not permanently remove records.

**Request:**
```http
DELETE /api/v1/purge HTTP/1.1
Host: telemetry.yourdomain.com
X-API-Key: mtx_pub_...
```

**Success Response (200):**
```json
{
  "status": "archived",
  "tenant_id": "the_tenant_id",
  "events_archived": 12345
}
```

---

## Redis Key Reference

| Key | Type | Description |
|---|---|---|
| `metricix_events_queue` | List | Active ingestion buffer (`RPUSH` target) |
| `metricix_events_queue_processing` | List | Transient processing queue (atomic `RENAME` target) |
| `metricix_dlq` | List | Dead Letter Queue for failed DB batches |

---

*See [`docs/FR.md`](FR.md), [`docs/NFR.md`](NFR.md), and [`docs/MVP.md`](MVP.md) for detailed specifications.*
