CREATE TABLE IF NOT EXISTS scrape_job (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    status TEXT NOT NULL,
    schedule TEXT NOT NULL,
    priority INTEGER NOT NULL,
    max_concurrency INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    next_run_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS scrape_target (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES scrape_job(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    method TEXT NOT NULL,
    target_type TEXT NOT NULL,
    headers JSONB,
    body JSONB,
    selectors JSONB,
    pagination JSONB,
    last_scraped_at TIMESTAMPTZ,
    content_hash TEXT
);

CREATE TABLE IF NOT EXISTS scrape_result (
    id BIGSERIAL,
    job_id BIGINT,
    target_id BIGINT,
    http_status INTEGER NOT NULL,
    duration_ms BIGINT NOT NULL,
    fetched_at TIMESTAMPTZ NOT NULL,
    change_detected BOOLEAN NOT NULL,
    payload JSONB,
    content_hash TEXT,
    PRIMARY KEY (id, fetched_at)
) PARTITION BY RANGE (fetched_at);

CREATE TABLE IF NOT EXISTS scrape_result_default PARTITION OF scrape_result
    DEFAULT;

CREATE TABLE IF NOT EXISTS scrape_attempt (
    id BIGSERIAL PRIMARY KEY,
    target_id BIGINT,
    status TEXT NOT NULL,
    error_message TEXT,
    retry_count INTEGER,
    proxy_id BIGINT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS proxy_endpoint (
    id BIGSERIAL PRIMARY KEY,
    host TEXT NOT NULL,
    port INTEGER NOT NULL,
    username TEXT,
    password TEXT,
    protocol TEXT,
    geo TEXT,
    status TEXT,
    success_rate DOUBLE PRECISION,
    last_checked_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS change_event (
    id BIGSERIAL PRIMARY KEY,
    target_id BIGINT,
    previous_hash TEXT NOT NULL,
    new_hash TEXT NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    diff JSONB
);

CREATE TABLE IF NOT EXISTS notification (
    id BIGSERIAL PRIMARY KEY,
    channel TEXT NOT NULL,
    recipient TEXT NOT NULL,
    payload JSONB,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS scheduler_state (
    id BIGSERIAL PRIMARY KEY,
    scheduler_name TEXT NOT NULL,
    last_run_at TIMESTAMPTZ,
    cursor TEXT,
    status TEXT
);

CREATE TABLE IF NOT EXISTS metric_sample (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    value DOUBLE PRECISION NOT NULL,
    labels JSONB,
    timestamp TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_scrape_target_job ON scrape_target(job_id);
CREATE INDEX IF NOT EXISTS idx_scrape_result_target ON scrape_result(target_id, fetched_at DESC);
CREATE INDEX IF NOT EXISTS idx_scrape_result_job ON scrape_result(job_id, fetched_at DESC);
CREATE INDEX IF NOT EXISTS idx_proxy_status ON proxy_endpoint(status);
CREATE INDEX IF NOT EXISTS idx_change_target ON change_event(target_id, detected_at DESC);
