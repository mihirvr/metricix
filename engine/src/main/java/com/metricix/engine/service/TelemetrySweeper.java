package com.metricix.engine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class TelemetrySweeper {

    private static final Logger log = LoggerFactory.getLogger(TelemetrySweeper.class);
    private static final String REDIS_QUEUE_KEY = "metricix_ingest_queue";
    private static final String PROCESSING_QUEUE_KEY = "metricix_processing_queue";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;

    public TelemetrySweeper(ReactiveStringRedisTemplate redisTemplate, DatabaseClient databaseClient, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.databaseClient = databaseClient;
        this.objectMapper = objectMapper;
    }

    // Runs every 5 seconds
    @Scheduled(fixedDelayString = "${BATCH_INTERVAL_MS:5000}")
    public void processEvents() {
        // 1. Let's prove the clock is actually ticking!
        log.info("Sweeper clock ticked! Checking Redis...");

        // 2. Check if the queue actually exists before trying to rename it
        redisTemplate.hasKey(REDIS_QUEUE_KEY)
            .flatMap(hasKey -> {
                if (!Boolean.TRUE.equals(hasKey)) {
                    return Mono.empty(); // Queue is empty, go back to sleep
                }
                // Queue has data! Rename it so the API can keep taking new events safely
                return redisTemplate.rename(REDIS_QUEUE_KEY, PROCESSING_QUEUE_KEY)
                        .thenMany(redisTemplate.opsForList().range(PROCESSING_QUEUE_KEY, 0, -1))
                        .collectList();
            })
            .flatMap(events -> {
                if (events == null || events.isEmpty()) return Mono.empty();
                
                log.info("🧹 Sweeper found {} events. Moving to Postgres...", events.size());

                // 3. Loop through the events and insert them into PostgreSQL
                return Flux.fromIterable(events)
                    .flatMap(eventString -> {
                        try {
                            Map<String, Object> map = objectMapper.readValue((String) eventString, new TypeReference<>() {});
                            String payloadJson = objectMapper.writeValueAsString(map.get("payload"));

                            return databaseClient.sql("INSERT INTO metricix_events (tenant_id, event_type, url, payload) VALUES (:tenant, :type, :url, :payload)")
                                .bind("tenant", map.get("tenant_id"))
                                .bind("type", map.get("event_type"))
                                .bind("url", map.get("url") != null ? map.get("url") : "")
                                .bind("payload", Json.of(payloadJson))
                                .then();
                                
                        } catch (Exception e) {
                            log.error("Failed to parse or save event", e);
                            return Mono.empty();
                        }
                    })
                    // 4. Delete the processing queue once saved
                    .then(redisTemplate.delete(PROCESSING_QUEUE_KEY)); 
            })
            .subscribe(
                success -> {}, // Fire and forget on success
                error -> log.error("Sweeper encountered a critical error!", error) // Catch any silent crashes
            );
    }
    // --- 3. THE RETRIEVAL API (Pulling from Postgres) ---
    public Flux<Map<String, Object>> getRecentEvents(String tenantId, int limit) {
        log.info("Fetching up to {} events for tenant: {}", limit, tenantId);
        
        return databaseClient.sql("SELECT * FROM metricix_events WHERE tenant_id = :tenant ORDER BY created_at DESC LIMIT :limit")
                .bind("tenant", tenantId)
                .bind("limit", limit)
                .fetch()
                .all(); 
    }
}
