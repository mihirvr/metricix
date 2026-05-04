CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE metricix_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    url VARCHAR(2048),
    payload JSONB NOT NULL,
    client_ip VARCHAR(45),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes to make querying insanely fast later
CREATE INDEX idx_tenant_event ON metricix_events(tenant_id, event_type);
CREATE INDEX idx_created_at ON metricix_events(created_at);