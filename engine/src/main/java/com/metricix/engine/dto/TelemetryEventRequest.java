package com.metricix.engine.dto;

import java.util.Map;

// This maps directly to the JSON payload coming from your frontend
public record TelemetryEventRequest(
    String event_type,
    String url,
    Map<String, Object> payload
) {}