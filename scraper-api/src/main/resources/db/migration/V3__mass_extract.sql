CREATE TABLE IF NOT EXISTS mass_extract_job (
    id BIGSERIAL PRIMARY KEY,
    query TEXT NOT NULL,
    location TEXT NOT NULL,
    radius_km INTEGER NOT NULL,
    max_results INTEGER NOT NULL,
    status TEXT NOT NULL,
    total_found INTEGER DEFAULT 0,
    processed_count INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS mass_extract_result (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES mass_extract_job(id) ON DELETE CASCADE,
    place_id TEXT NOT NULL,
    name TEXT NOT NULL,
    category TEXT,
    subcategory TEXT,
    description TEXT,
    rating DOUBLE PRECISION,
    reviews_count INTEGER DEFAULT 0,
    phone TEXT,
    secondary_phone TEXT,
    email TEXT,
    website_url TEXT,
    address TEXT,
    city TEXT,
    state TEXT,
    country TEXT,
    postal_code TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    maps_url TEXT,
    price_level INTEGER,
    business_status TEXT,
    opening_hours JSONB,
    open_now BOOLEAN,
    permanently_closed BOOLEAN,
    logo_url TEXT,
    photos JSONB,
    reviews_summary TEXT,
    reviews_keywords JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS mass_extract_social (
    id BIGSERIAL PRIMARY KEY,
    result_id BIGINT NOT NULL REFERENCES mass_extract_result(id) ON DELETE CASCADE,
    platform TEXT NOT NULL,
    url TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS mass_extract_contact (
    id BIGSERIAL PRIMARY KEY,
    result_id BIGINT NOT NULL REFERENCES mass_extract_result(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    value TEXT NOT NULL
);

-- Indexes for performance Optimization
CREATE INDEX IF NOT EXISTS idx_mass_job_status ON mass_extract_job(status);
CREATE INDEX IF NOT EXISTS idx_mass_result_job ON mass_extract_result(job_id);
CREATE INDEX IF NOT EXISTS idx_mass_result_place ON mass_extract_result(place_id);
CREATE INDEX IF NOT EXISTS idx_mass_social_result ON mass_extract_social(result_id);
CREATE INDEX IF NOT EXISTS idx_mass_contact_result ON mass_extract_contact(result_id);
