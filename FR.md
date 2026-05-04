# Metricix — Functional Requirements (FR)

**Document Version:** 1.1
**Scope:** All functional behavior the system must exhibit.

---

## FR-1: Event Ingestion (API Layer)

### FR-1.1 — Endpoint Structure
The system **MUST** provide a single REST endpoint:

```
POST /api/v1/track
```

### FR-1.2 — Payload Validation
The endpoint **MUST** accept a JSON request body and validate the presence of the following fields:

| Field | Type | Required | Description |
|---|---|---|---|
| `event_type` | `string` | ✅ Yes | Event identifier (e.g., `page_view`, `checkout_click`) |
| `payload` | `object` | ✅ Yes | Arbitrary unstructured JSON data |
| `url` | `string` | ❌ Optional | Origin URL of the event |

Requests with missing required fields **MUST** return:

```
HTTP 400 Bad Request
```

```json
{
  "error": "validation_failed",
  "message": "Missing required field: event_type"
}
```

### FR-1.3 — Immediate Acknowledgement
Upon receiving a valid payload, the API **MUST**:

1. Decorate the payload with server-side fields (see FR-1.4).
2. Push the decorated payload to the Redis buffer via `RPUSH`.
3. Immediately return `HTTP 202 Accepted`.

The API **MUST NOT** wait for any database write confirmation before responding.

### FR-1.4 — Data Decoration
Before pushing to Redis, the API **MUST** append the following server-side fields:

| Field | Source | Description |
|---|---|---|
| `received_at` | Server clock | ISO-8601 UTC timestamp of ingestion |
| `client_ip` | `X-Forwarded-For` header or remote address | Client IP address (nullable) |
| `tenant_id` | Derived from the validated API Key | Owning tenant identifier |

---

## FR-2: Authentication & Security

### FR-2.1 — API Key Validation
Every incoming HTTP request **MUST** include the following header:

```
X-API-Key: <key>
```

The system **MUST** validate the key against the set of registered active keys.

### FR-2.2 — Key Prefix Convention
The system **MUST** enforce a naming prefix convention on all API keys:

| Prefix | Purpose |
|---|---|
| `mtx_pub_` | Public ingestion write-keys (used by client applications) |

Keys not conforming to a recognized prefix **MUST** be rejected.

### FR-2.3 — Rejection Behavior
Requests with **missing**, **malformed**, or **invalid** API keys **MUST** receive:

```
HTTP 401 Unauthorized
```

```json
{
  "error": "unauthorized",
  "message": "Invalid or missing X-API-Key header."
}
```

---

## FR-3: The Buffer (Redis Operations)

### FR-3.1 — Reactive Redis Client
The system **MUST** use **Lettuce** (reactive Redis client) to push events to Redis without blocking JVM threads. All Redis interactions **MUST** return `Mono` / `Flux` types compatible with the Project Reactor execution model.

### FR-3.2 — Serialization
Java domain objects (event POJOs) **MUST** be serialized to compact JSON strings before being appended to the Redis list using the `RPUSH` command.

| Config | Default | Description |
|---|---|---|
| `REDIS_QUEUE_KEY` (env var) | `metricix_events_queue` | Redis list key for the active event buffer |

---

## FR-4: The Sweeper (Batch Processing Worker)

### FR-4.1 — Scheduling
A Spring `@Scheduled` worker **MUST** execute at a configurable interval.

| Config | Default |
|---|---|
| `BATCH_INTERVAL_MS` (env var) | `5000` (5 seconds) |

### FR-4.2 — Atomic Queue Drain
The worker **MUST** read and clear the Redis queue **atomically** to prevent race conditions with concurrent API traffic. Required command sequence:

1. `RENAME metricix_events_queue metricix_events_queue_processing` — atomically promotes the live queue to a processing queue. Incoming `RPUSH` calls during processing target a fresh `metricix_events_queue` list.
2. `LRANGE metricix_events_queue_processing 0 -1` — reads all items from the processing queue.
3. `DEL metricix_events_queue_processing` — clears the processing queue after successful DB insertion.

If `RENAME` fails because the source key does not exist (empty queue), the worker **MUST** exit the cycle without error.

### FR-4.3 — Deserialization
The worker **MUST** parse each JSON string from the batch back into a valid Java entity (`MetricixEvent`) before attempting database insertion. Malformed records that cannot be deserialized **MUST** be routed to the DLQ individually and logged at `ERROR` level.

---

## FR-5: Database Persistence (PostgreSQL)

### FR-5.1 — Reactive SQL via R2DBC
The system **MUST** use **Spring Data R2DBC** for all database interactions. Batch inserts **MUST** be executed as a single bulk `INSERT` statement per sweep cycle:

```sql
INSERT INTO metricix_events (id, tenant_id, event_type, url, payload, client_ip, created_at)
VALUES ($1,  $2,  $3,  $4,  $5,  $6,  $7),
       ($8,  $9,  $10, $11, $12, $13, $14),
       ...
```

A single database round-trip per batch is required. Individual per-row inserts are not acceptable.

All data retrieval queries (e.g., for the dashboard or API) **MUST** include a `WHERE is_deleted = FALSE` clause to exclude archived records from the results.

### FR-5.2 — Dead Letter Queue (DLQ)
If the database insert **fails** for any reason (connection timeout, SQL error, constraint violation on the batch), the worker **MUST**:

1. Push the entire failed batch to the Redis DLQ list: `metricix_dlq` (via `RPUSH`).
2. Emit an application-level `ERROR` log entry including: failure reason, batch size, and first event timestamp.
3. Increment the `metricix_dlq_events_total` Prometheus counter by the batch size.

**Data MUST NEVER be silently dropped.** Exception swallowing on the batch processing path is explicitly prohibited.

---

## FR-6: Rate Limiting

### FR-6.1 — Token Bucket Algorithm
A Spring `WebFilter` **MUST** implement rate-limiting logic using a token bucket algorithm. The token bucket state **MUST** be stored in Redis (not JVM memory) so that rate limits are enforced correctly across multiple stateless application instances.

### FR-6.2 — Threshold Enforcement
If a given `X-API-Key` exceeds the configured request threshold within a one-second window, the system **MUST** respond with:

```
HTTP 429 Too Many Requests
```

| Config | Default |
|---|---|
| `RATE_LIMIT_RPS` (env var) | `200` requests/second per key |

```json
{
  "error": "rate_limit_exceeded",
  "message": "Request quota exceeded. Retry after 1 second."
}
```

The rate limiter **MUST** execute in the `WebFilter` chain **before** payload validation and Redis writes — a rate-limited request must not touch Redis.

---

## FR-7: Dashboard Visualization

### FR-7.1 — User Interface
The system **MUST** provide a browser-based graphical user interface (GUI) served from the `frontend/` directory.

### FR-7.2 — Data Representation
The GUI **MUST** render a bar chart visualizing the total count of events, grouped by `event_type`, over a configurable time window. The chart **MUST** update in near real-time by polling the `/api/v1/events` endpoint.

---

## FR-8: Soft-Delete Protocol

### FR-8.1 — Archival Endpoint
The system **MUST** provide an endpoint for data archival:

```
DELETE /api/v1/purge
```

### FR-8.2 — Logical Deletion
This operation **MUST NOT** physically delete rows from the `metricix_events` table. Instead, it **MUST** execute a bulk `UPDATE` statement that sets the `is_deleted` flag to `true` for all records associated with the `tenant_id` of the provided `X-API-Key`. This ensures data is hidden from query APIs but remains recoverable.
