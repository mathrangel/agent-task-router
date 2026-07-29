# Deployment

**Current target: Railway** (container image, built from the `Dockerfile` at
the repo root). An earlier iteration targeted AWS App Runner — abandoned
before going live because it didn't justify the cost/complexity for a
portfolio project at this stage. Kept the Dockerfile; changed the target.

## Dockerfile structure

Multi-stage build, kept regardless of deploy target because it produces a
small, reproducible image:

**Stage 1 (`build`):** `maven:3.9-eclipse-temurin-21` — compiles the
application. `pom.xml` is copied and dependencies resolved before the source
code is copied in, so Docker's layer cache can reuse the dependency layer
across builds when only application code changes. Tests are skipped in this
stage (`-DskipTests`) — there's no CI running them separately yet, this is
just a build-speed choice, not a policy.

**Stage 2 (runtime):** `eclipse-temurin:21-jre-alpine` — a minimal image
containing only the JRE. Only the built `.jar` is copied over from the build
stage; no Maven, compiler, or source code ends up in the final image.

The application listens on port 8080 (Spring Boot default, no explicit
`server.port` override).

Built on Apple Silicon, this image must be built with
`--platform linux/amd64` — Railway (like App Runner before it) runs x86_64,
and a native arm64 build fails to start with no useful error in the platform
logs (a real bug hit while this was still targeting App Runner).

## Required environment variables

| Variable | Purpose |
|----------|---------|
| `DB_URL` | PostgreSQL JDBC URL (Supabase Session Mode Pooler — see below) |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret — generate with `openssl rand -hex 32` |

## Building and testing locally

```bash
docker build --platform linux/amd64 -t agent-task-router .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/procurement \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e JWT_SECRET=<generate-with-openssl-rand-hex-32> \
  agent-task-router
```

Postgres must be reachable from inside the container (the local
`docker-compose.yml` Postgres service works for this).

## Database — Supabase

Production database is PostgreSQL on Supabase. Supabase's **direct**
connection string is IPv6-only, which failed to connect from Railway — use
the **Session Mode Pooler** connection string instead (Supabase dashboard →
Project Settings → Database → Connection Pooling → Session mode), which is
IPv4-compatible. Set `DB_URL` to that pooler connection string.

## Deploying to Railway

1. Railway detects the `Dockerfile` at the repo root and builds from it —
   no separate build config needed.
2. Set `DB_URL` (Supabase pooler string, see above), `DB_USERNAME`,
   `DB_PASSWORD`, `JWT_SECRET` as environment variables in the Railway
   service.
3. Railway provisions a public HTTPS URL once the service is running.

Current live URL: https://agent-task-router-production.up.railway.app
