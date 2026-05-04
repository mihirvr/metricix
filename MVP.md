# Metricix — MVP Definition

**Document Version:** 1.1
**Scope:** Defines the boundaries of the Minimum Viable Product. Everything here is required for a shippable v1.0.

---

## Objective

The MVP is strictly focused on a **rock-solid, zero-data-loss Ingestion Pipeline**. It deliberately trades feature breadth for depth — prioritizing raw ingestion speed, database integrity, and API reliability above all else.

There is no GUI, no analytics dashboard, and no query API in the MVP. The measure of success is a provably correct event buffer that acknowledges in under 15ms and loses nothing.

---

## In Scope

### 1. `POST /api/v1/track` Endpoint

A single, highly optimized REST endpoint that accepts event payloads, validates them, decorates them with server-side metadata, and acknowledges immediately without waiting for PostgreSQL.

**Success criteria:**
- Returns `202 Accepted` within 15ms (P95) under sustained load.
- Validates required fields (`event_type`, `payload`); returns `400` on missing fields.
- Appends `received_at` timestamp, `client_ip`, and `tenant_id` before queuing.

---

### 2. API Key Authentication

Every request must carry a valid `X-API-Key` header. Keys must conform to the `mtx_pub_` prefix.

**Success criteria:**
- Requests with missing or invalid keys return `401 Unauthorized`.
- Key validation runs in the `WebFilter` chain before any payload work.
- Key validation adds negligible latency (< 1ms overhead).

---

### 3. Redis Event Buffer

Incoming events are pushed to a Redis List using a non-blocking Lettuce reactive client. The API does not block on this operation.

**Success criteria:**
- `RPUSH` to `metricix_events_queue` completes without blocking the Netty event loop thread.
- Event is durably queued in Redis before `202` is returned to the client.

---

### 4. Sweeper — Batch Processor

A `@Scheduled` background worker that atomically drains the Redis queue and bulk-inserts events into PostgreSQL every 5 seconds (configurable).

**Success criteria:**
- Queue drain uses the `RENAME` atomic pattern to prevent race conditions with live API traffic.
- Entire batch is written in a single `INSERT ... VALUES (), (), ()` SQL statement.
- Empty queue cycles are handled gracefully (no error, no-op exit).
- Interval is configurable via `BATCH_INTERVAL_MS` env var.

---

### 5. Dead Letter Queue (DLQ)

Failed database writes are pushed to `metricix_dlq` in Redis. No event is ever silently dropped.

**Success criteria:**
- Any DB failure causes the entire batch to be pushed to `metricix_dlq` via `RPUSH`.
- An `ERROR` log entry is emitted with failure reason and batch size.
- `metricix_dlq_events_total` Prometheus counter is incremented by the batch size.
- DLQ is inspectable via `redis-cli LRANGE metricix_dlq 0 -1`.

---

### 6. Rate Limiting

Per-key token bucket rate limiting enforced at the `WebFilter` level, backed by Redis.

**Success criteria:**
- Keys exceeding `RATE_LIMIT_RPS` (default: 200 RPS) receive `429 Too Many Requests`.
- Rate limit state lives in Redis and works correctly across multiple app instances.
- Rate-limited requests do not reach Redis `RPUSH` or payload validation.

---

### 7. Containerized Deployment

The full stack (app + Redis + PostgreSQL) can be brought up with a single command on a fresh machine.

**Success criteria:**
- `docker compose up -d` starts all services with no manual setup required.
- Redis and PostgreSQL healthchecks pass before the API container starts.
- Multi-stage Dockerfile produces an optimized image no larger than 250 MB.
- PostgreSQL data persists across `docker compose down` / `docker compose up` cycles (named volume).

---

### 8. Flyway Schema Migrations

PostgreSQL schema is provisioned automatically on startup. No manual DDL.

**Success criteria:**
- `metricix_events` table is created on first boot via `V1__create_metricix_events.sql`.
- Subsequent restarts do not duplicate or corrupt the schema.
- Schema version history is tracked in the Flyway `flyway_schema_history` table.

---

### 9. Real-time Dashboard

A Chart.js-powered frontend to visualize event volume by type.

**Success criteria:**
- Connects to the `/api/v1/events` endpoint to fetch data.
- Displays a time-series chart showing event counts grouped by `event_type`.
- UI is served from the `frontend/` directory.

---

### 10. Tenant Discovery API

Automated identification of active clients via a `/api/v1/tenants` endpoint.

**Success criteria:**
- Returns a JSON array of unique `tenant_id` strings present in the `metricix_events` table.
- Endpoint is secured via API Key Authentication.

---

### 11. Retrieval API

A `GET /api/v1/events` route for fetching historical data for the UI.

**Success criteria:**
- Returns paginated event data from PostgreSQL.
- Supports filtering by `tenant_id` and `event_type` via query parameters.
- Endpoint is secured via API Key Authentication.

---

### 12. Data Archival (Soft Delete)

A safety-first approach to data removal using an `is_deleted` flag rather than hard `DELETE` commands.

**Success criteria:**
- `metricix_events` table includes an `is_deleted` boolean column, defaulting to `false`.
- A `DELETE /api/v1/events/{eventId}` endpoint sets `is_deleted` to `true`.
- Retrieval APIs exclude records where `is_deleted` is `true` by default.

## Out of Scope (Explicitly Deferred)

The following features are **not** part of the MVP and must not be built until the ingestion pipeline is proven stable in production:

| Feature | Reason Deferred |
|---|---|
| Multi-node Redis Clustering | Single Redis instance sufficient for MVP scale |
| Automated DLQ Replay | Operational tooling, post-MVP |
| Multi-tenancy UI / Key Management Portal | Admin layer, post-MVP |
| Client SDKs (JS, Python, Go, etc.) | Developer experience layer, post-MVP |

---

## MVP Exit Criteria (Definition of Done)

The MVP is considered shippable when **all** of the following pass:

- [ ] `POST /api/v1/track` returns `202` in < 15ms at P95 under a sustained 1,000 RPS load test.
- [ ] All events from the load test are present in PostgreSQL after sweeper flushes (zero loss verified by count comparison).
- [ ] A simulated DB failure results in the batch appearing in `metricix_dlq` — not dropped, not partially inserted.
- [ ] `metricix_dlq_events_total` increments correctly on each DLQ write.
- [ ] A key exceeding the rate limit receives `429`, not `202`.
- [ ] `docker compose up -d` starts the full stack cleanly from a fresh clone with no manual steps.
- [ ] `/actuator/prometheus` exposes `metricix_events_ingested_total` with counts matching the load test volume.
- [ ] Flyway migration runs successfully on first boot; idempotent on subsequent boots.
- [ ] Verify that a purged tenant instantly disappears from the Dashboard dropdown without a page reload.
