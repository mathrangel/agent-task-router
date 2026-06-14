# Agent Task Router

A production-grade REST API that receives tasks, routes them to registered AI agents based on semantic capability matching, executes asynchronously, and exposes full observability.

Built with Java 21 + Spring Boot 3 as a portfolio project to demonstrate backend, AI integration, and cloud-native engineering skills.

---

## What problem does this solve?

In agentic systems, you often have multiple specialized agents (one for text, one for code, one for data analysis) and a stream of incoming tasks that need to reach the right agent. Routing manually doesn't scale.

This API automates that routing:
1. Agents register themselves with their capabilities
2. Tasks are submitted with a type and payload
3. The router finds the best available agent using semantic matching
4. Execution is tracked, retried on failure, and exposed via metrics

---

## Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| Language | Java 21 | Virtual threads, records, sealed classes |
| Framework | Spring Boot 3.3 | Industry standard for EU enterprise backend |
| Security | Spring Security 6 + JWT | Stateless auth for REST APIs |
| Persistence | Spring Data JPA + PostgreSQL | Relational data with full ACID guarantees |
| Migrations | Flyway | Version-controlled schema changes |
| AI Routing | Spring AI + Claude API | Semantic capability matching via embeddings |
| Vectors | pgvector (PostgreSQL extension) | Embedding storage and similarity search |
| Cache | Redis | Agent availability cache + distributed lock |
| Async | Spring `@Async` + Virtual Threads | Non-blocking execution pipeline |
| State Machine | Spring State Machine | Explicit execution state transitions |
| Observability | Micrometer + Prometheus + Grafana | Metrics, dashboards, alerting |
| Tracing | OpenTelemetry + Jaeger | Distributed trace per task execution |
| Testing | JUnit 5 + Testcontainers + MockMvc | Integration tests against real DB |
| Resilience | Resilience4j | Circuit breaker on external agent calls |
| Containerization | Docker + Docker Compose | One command to run everything locally |
| CI/CD | GitHub Actions | Automated test + build + deploy pipeline |
| Deploy | Railway (prod) | Public URL, PostgreSQL included, free tier |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (HTTP)                           │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Security (JWT)                         │
└──────────────────────┬──────────────────────────────────────────┘
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
   AgentController  TaskController  MetricsController
          │            │
          ▼            ▼
   AgentService    TaskService
          │            │
          │       RoutingEngine ──────► Spring AI (embeddings)
          │            │                    │
          │            ▼                    ▼
          │       ExecutionEngine        pgvector (similarity search)
          │            │
          ▼            ▼
   PostgreSQL ◄──── JPA Repositories
          │
   Redis (cache + distributed lock)
          │
   Prometheus ◄── Micrometer (metrics)
          │
   Grafana (dashboards)
          │
   OpenTelemetry ──► Jaeger (traces)
```

**Architecture pattern:** Hexagonal (Ports & Adapters) — domain logic has zero framework dependencies.

```
src/main/java/com/matheus/agenttaskrouter/
├── agent/
│   ├── domain/          ← Agent entity, AgentStatus enum, capability value objects
│   ├── application/     ← AgentService, use cases (RegisterAgent, DeactivateAgent)
│   ├── infra/           ← AgentJpaRepository, AgentRedisCache
│   └── api/             ← AgentController, AgentRequest/Response DTOs
├── task/
│   ├── domain/          ← Task entity, TaskType enum, TaskStatus sealed hierarchy
│   ├── application/     ← TaskService, SubmitTask use case
│   ├── infra/           ← TaskJpaRepository
│   └── api/             ← TaskController, TaskRequest/Response DTOs
├── routing/
│   ├── RoutingEngine    ← finds best agent for a task
│   └── EmbeddingMatcher ← semantic similarity via Spring AI + pgvector
├── execution/
│   ├── ExecutionEngine  ← async execution, state transitions, retry
│   └── ExecutionStateMachine ← Spring State Machine config
├── observability/
│   ├── MetricsController ← /metrics/agents/{id}
│   └── ExecutionMetrics ← Micrometer counters/timers
└── config/
    ├── SecurityConfig   ← JWT filter chain
    ├── AsyncConfig      ← Virtual threads executor
    ├── RedisConfig
    └── OpenTelemetryConfig
```

---

## API Reference

### Agents

```
POST   /agents                    Register a new agent
GET    /agents                    List all active agents
GET    /agents/{id}               Get agent details
DELETE /agents/{id}               Deactivate agent
```

### Tasks

```
POST   /tasks                     Submit a task for routing and execution
GET    /tasks/{id}                Get task status and execution details
POST   /tasks/{id}/retry          Retry a failed task
GET    /tasks?status=FAILED       Filter tasks by status
```

### Metrics

```
GET    /metrics/agents/{id}       Success rate, avg execution time, total count
GET    /metrics/summary           System-wide execution stats
```

### System

```
GET    /health                    Basic health check
GET    /health/details            Version + uptime
GET    /actuator/prometheus       Prometheus scrape endpoint
```

---

## Data Model

```
agents
  id            UUID PK
  name          VARCHAR
  endpoint_url  VARCHAR          ← where the agent actually runs
  capabilities  TEXT[]           ← ["text-generation", "code-review", "summarization"]
  embedding     vector(1536)     ← pgvector — semantic representation of capabilities
  status        ENUM             ← ACTIVE | INACTIVE | OVERLOADED
  max_concurrency INT
  created_at    TIMESTAMP

tasks
  id            UUID PK
  type          VARCHAR          ← "text-generation", "code-review", etc.
  payload       JSONB            ← task input data
  priority      ENUM             ← LOW | NORMAL | HIGH | CRITICAL
  status        ENUM             ← PENDING | ROUTING | EXECUTING | SUCCESS | FAILED | CANCELLED
  assigned_agent_id UUID FK → agents
  created_at    TIMESTAMP
  completed_at  TIMESTAMP

executions
  id            UUID PK
  task_id       UUID FK → tasks
  agent_id      UUID FK → agents
  attempt       INT              ← retry count
  status        ENUM
  started_at    TIMESTAMP
  finished_at   TIMESTAMP
  error_message TEXT
  output        JSONB
```

---

## Execution State Machine

```
PENDING ──► ROUTING ──► EXECUTING ──► SUCCESS
                │              │
                ▼              ▼
             FAILED ◄──────────┘
                │
                ▼
            CANCELLED (max retries exceeded)
```

Transitions are persisted — every state change is recorded in the `executions` table.

---

## Running Locally

```bash
# Start all infrastructure (PostgreSQL + Redis + Prometheus + Grafana + Jaeger)
docker compose up -d

# Run the application
./mvnw spring-boot:run

# API available at
http://localhost:8080

# Grafana dashboard
http://localhost:3000  (admin/admin)

# Jaeger traces
http://localhost:16686

# Prometheus
http://localhost:9090
```

---

## Running Tests

```bash
# Unit + integration tests (Testcontainers spins up a real PostgreSQL)
./mvnw test

# Test coverage report
./mvnw test jacoco:report
open target/site/jacoco/index.html
```

---

## Environment Variables

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/agent_router
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT
JWT_SECRET=your-256-bit-secret
JWT_EXPIRATION_MS=86400000

# AI (Anthropic Claude)
ANTHROPIC_API_KEY=your-key-here
SPRING_AI_ANTHROPIC_CHAT_MODEL=claude-haiku-4-5-20251001

# OpenTelemetry
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317
OTEL_SERVICE_NAME=agent-task-router
```

---

## CI/CD Pipeline (GitHub Actions)

On every push to `main`:
1. Compile and run tests (Testcontainers)
2. Build Docker image
3. Push to Docker Hub
4. Deploy to Railway

See `.github/workflows/ci.yml`.

---

## CV Talking Points

After completing this project, you can claim experience with:

- **Java 21** — virtual threads, records, sealed classes, pattern matching
- **Spring Boot 3** — full production configuration including security, async, actuator
- **Spring Security 6** — JWT stateless authentication
- **Spring AI** — AI integration, embeddings, semantic search
- **Hexagonal Architecture** — domain isolation, testability, ports & adapters
- **Spring State Machine** — explicit state transitions in business flows
- **PostgreSQL + pgvector** — relational + vector storage in the same database
- **Redis** — distributed caching and locking
- **Testcontainers** — integration tests against real infrastructure
- **Resilience4j** — circuit breaker pattern
- **Micrometer + Prometheus + Grafana** — observability stack
- **OpenTelemetry + Jaeger** — distributed tracing
- **Docker + Docker Compose** — containerized local development
- **GitHub Actions** — CI/CD pipeline with automated deploy
- **Flyway** — database migration management

---

## Author

Matheus Rangel · [github.com/mathrangel](https://github.com/mathrangel)
