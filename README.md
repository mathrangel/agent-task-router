# Agent Task Router

A REST API, built with Java 21 + Spring Boot 3, for registering AI agents and
submitting tasks to them. Portfolio project — built in public, in phases,
while learning backend Java. This README describes what's actually in the
code today, not the finished vision.

**Live:** https://agent-task-router-production.up.railway.app/health

---

## What it does today

- Register/list/deactivate agents (`/agents`)
- Register/login users with JWT authentication (`/auth`)
- Submit and look up tasks (`/tasks`) — stored, not yet routed or executed
- Health check (`/health`)

## What it doesn't do yet

The original scope for this project includes semantic task routing (matching
a task to the best available agent via embeddings), async execution with
retries, observability (metrics/tracing), and resilience patterns. None of
that is built yet. Full phase-by-phase plan and real status:
[`docs/DEVELOPMENT_PLAN.md`](docs/DEVELOPMENT_PLAN.md).

Concretely, as of now:
- No routing logic — a submitted task is not assigned to an agent
- No async execution engine or state machine — `executions` table exists
  (migration `V3`) but nothing writes to it yet
- No input validation (`@Valid`) or global exception handling
- No role-based authorization — the `role` field on `User` exists but isn't
  enforced anywhere (no `@PreAuthorize`, no custom `UserDetailsService`)
- No AI integration, no vector search, no Redis usage in application code
  (Redis is provisioned in `docker-compose.yml` ahead of implementation —
  see `docs/ARCHITECTURE.md`, ADR-011)
- No metrics/tracing endpoints, no CI pipeline
- Test suite is the default Spring Boot context-load smoke test — no
  integration tests yet

---

## Key technical decisions

Full reasoning and trade-offs in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) —
split into decisions actually in effect and a backlog of decisions made ahead
of implementation (explicitly labeled as such, not presented as done):

- UUID primary keys over auto-increment (ADR-002)
- Flyway over `ddl-auto=update` for schema changes (ADR-003)
- Redis provisioned in infra ahead of any code using it, deliberately (ADR-011)

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3 (Web, Data JPA, Security) |
| Auth | Spring Security 6 + JJWT (JWT, stateless) |
| Persistence | PostgreSQL (Supabase in production) |
| Migrations | Flyway |
| Local infra | Docker Compose (Postgres + Redis) |
| Deploy | Railway (container image, multi-stage `Dockerfile`) |

## Project structure

Package-by-feature, not a hexagonal/layered structure (an earlier version of
this README described one that was never actually built):

```
src/main/java/com/matheus/procurement/
├── agent/        Agent entity, repository, service, controller
├── task/         Task entity, repository, service, controller,
│                 TaskStateMachineConfig, TaskEvent
├── execution/    Execution entity, ExecutionStatus, ExecutionRepository,
│                 ExecutionEngine
├── controller/   AuthController, HealthController
├── security/     JwtAuthFilter, JwtTokenProvider
├── entity/       User
├── repository/   UserRepository
├── dto/          RegisterRequest
└── config/       SecurityConfig
```

The base package (`com.matheus.procurement`) and artifact name
(`procurement-api`) are leftover from before the project was renamed to
Agent Task Router — cosmetic debt, not yet cleaned up.

---

## API reference (real endpoints only)

### Agents
```
POST   /agents         Register a new agent
GET    /agents         List agents
GET    /agents/{id}    Get one agent
DELETE /agents/{id}    Deactivate an agent
```

### Tasks
```
POST   /tasks          Submit a task (stored, not routed or executed yet)
GET    /tasks          List tasks
GET    /tasks/{id}     Get one task
```

### Auth
```
POST   /auth/register  Create a user (password hashed with BCrypt)
POST   /auth/login     Returns a JWT
```

### System
```
GET    /health          Basic health check
GET    /health/details  Version + uptime
```

All routes except `/auth/**`, `/health`, `/health/details` require a valid
JWT in the `Authorization` header.

---

## Data model

```
users
  id             BIGSERIAL PK
  email          VARCHAR UNIQUE
  password_hash  VARCHAR
  role           VARCHAR        ← stored, not enforced yet
  created_at     TIMESTAMP

agents
  id               UUID PK
  name             VARCHAR
  endpoint_url     VARCHAR
  capabilities     TEXT[]
  status           VARCHAR
  max_concurrency  INT
  created_at       TIMESTAMP

tasks
  id          UUID PK
  agent_id    UUID            ← FK, not yet populated by any routing logic
  type        VARCHAR
  payload     TEXT
  status      VARCHAR
  result      TEXT
  created_at  TIMESTAMP

executions
  id           UUID PK
  task_id      UUID FK
  agent_id     UUID FK
  status       VARCHAR
  started_at   TIMESTAMP
  finished_at  TIMESTAMP
```

`executions` is migrated but unused — no service in the codebase writes to
it yet (that's Fase 3 in the dev plan).

---

## Running locally

```bash
docker compose up -d          # Postgres + Redis
./mvnw spring-boot:run        # API on http://localhost:8080
```

## Running tests

```bash
./mvnw test
```

## Environment variables

```env
DB_URL=jdbc:postgresql://localhost:5432/procurement
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=changeme-generate-with-openssl-rand-hex-32
JWT_EXPIRATION_MS=86400000
```

## Deployment

Deployed on [Railway](https://railway.com) as a container image, built from
the multi-stage `Dockerfile` at the repo root. Database is PostgreSQL on
Supabase (Session Mode Pooler, IPv4). See
[`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) for the full setup.

---

## Roadmap

Phase-by-phase plan, with real done/pending status per item:
[`docs/DEVELOPMENT_PLAN.md`](docs/DEVELOPMENT_PLAN.md).

## Author

Matheus Rangel · [github.com/mathrangel](https://github.com/mathrangel)
