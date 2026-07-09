CREATE TABLE agents(
    id UUID PRIMARY KEY,
    name VARCHAR,
    endpoint_url VARCHAR,
    capabilities TEXT[],
    status VARCHAR,
        max_concurrency INT,
        created_at TIMESTAMP DEFAULT now()
)