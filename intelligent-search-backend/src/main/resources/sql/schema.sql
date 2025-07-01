CREATE TABLE IF NOT EXISTS search_analytics (
    id BIGSERIAL PRIMARY KEY,
    analytics_id VARCHAR(36) NOT NULL,
    query TEXT,
    result_count INTEGER,
    session_id VARCHAR(36),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    action_type VARCHAR(50),
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_search_analytics_timestamp ON search_analytics (timestamp);

CREATE TABLE IF NOT EXISTS sources (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(2048) NOT NULL UNIQUE,
    name VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_crawled_at TIMESTAMP WITH TIME ZONE,
    source_type VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS crawl_sessions (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE,
    documents_crawled BIGINT,
    status VARCHAR(50),
    FOREIGN KEY (source_id) REFERENCES sources(id)
);

CREATE INDEX IF NOT EXISTS idx_crawl_sessions_source_id ON crawl_sessions (source_id);
CREATE INDEX IF NOT EXISTS idx_crawl_sessions_session_id ON crawl_sessions (session_id);

CREATE TABLE IF NOT EXISTS search_contexts (
    session_id VARCHAR(36) PRIMARY KEY,
    geo_location JSONB,
    user_profile JSONB
);

CREATE TABLE IF NOT EXISTS search_queries (
    id BIGSERIAL PRIMARY KEY,
    query_id VARCHAR(36) NOT NULL,
    query TEXT,
    session_id VARCHAR(36),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    search_type VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_search_queries_timestamp ON search_queries (timestamp);


CREATE TABLE IF NOT EXISTS users (
    uuid UUID PRIMARY KEY,
    organization VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    contact_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    secret_phrase VARCHAR(255),
    hint VARCHAR(255),
    api_key VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS login_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    success BOOLEAN NOT NULL
);