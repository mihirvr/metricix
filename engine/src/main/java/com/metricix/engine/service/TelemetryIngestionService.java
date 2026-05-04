package com.metricix.engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metricix.engine.dto.TelemetryEventRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class TelemetryIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryIngestionService.class);
    private static final String REDIS_QUEUE_KEY = "metricix_ingest_queue";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DatabaseClient databaseClient;

    public TelemetryIngestionService(ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper, DatabaseClient databaseClient) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.databaseClient = databaseClient;
    }

    public Mono<Void> bufferEvent(String apiKey, TelemetryEventRequest request) {
        try {
            Map<String, Object> decoratedEvent = new HashMap<>();
            decoratedEvent.put("tenant_id", apiKey);
            decoratedEvent.put("event_type", request.event_type());
            decoratedEvent.put("url", request.url());
            decoratedEvent.put("payload", request.payload());
            decoratedEvent.put("received_at", Instant.now().toString());

            String jsonString = objectMapper.writeValueAsString(decoratedEvent);
            return redisTemplate.opsForList().rightPush(REDIS_QUEUE_KEY, jsonString).then();
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize telemetry event", e);
            return Mono.error(e);
        }
    }

    // 1. Only discover tenants that haven't been soft-deleted
    public Flux<String> getAllTenants() {
        return databaseClient.sql("SELECT DISTINCT tenant_id FROM metricix_events WHERE is_deleted = FALSE")
                .map(row -> row.get("tenant_id", String.class))
                .all();
    }

    // 2. Only retrieve active events and handle the Postgres JSON codec
    public Flux<Map<String, Object>> getRecentEvents(String tenantId, int limit) {
        return databaseClient.sql("SELECT * FROM metricix_events WHERE tenant_id = :tenant AND is_deleted = FALSE ORDER BY created_at DESC LIMIT :limit")
                .bind("tenant", tenantId)
                .bind("limit", limit)
                .fetch().all()
                .map(row -> {
                    Map<String, Object> formattedRow = new HashMap<>(row);
                    Object payload = formattedRow.get("payload");
                    if (payload instanceof io.r2dbc.postgresql.codec.Json postgresJson) {
                        try {
                            formattedRow.put("payload", objectMapper.readValue(postgresJson.asString(), Map.class));
                        } catch (Exception e) {
                            log.error("Payload parse error", e);
                        }
                    }
                    return formattedRow;
                });
    }

    // 3. Flip the bit instead of deleting the row
    public Mono<Long> purgeTenantData(String tenantId) {
        log.info("Archiving data for tenant: {}", tenantId);
        return databaseClient.sql("UPDATE metricix_events SET is_deleted = TRUE WHERE tenant_id = :tenant")
                .bind("tenant", tenantId)
                .fetch().rowsUpdated();
    }
}