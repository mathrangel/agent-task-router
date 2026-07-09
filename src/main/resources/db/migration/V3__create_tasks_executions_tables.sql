CREATE TABLE tasks (
        id UUID PRIMARY KEY,
        agent_id UUID  -- FK para agents,
        type VARCHAR,
        payload TEXT,
        status VARCHAR,
        result TEXT,
        created_at TIMESTAMP DEFAULT now()
);

CREATE TABLE executions (
        id UUID PRIMARY KEY,
        task_id UUID   -- FK para tasks,
        agent_id UUID  -- FK para agents,
        status VARCHAR,
        started_at TIMESTAMP DEFAULT now(),
        finished_at TIMESTAMP
)