# Deployment

## Why a Dockerfile

AWS App Runner can build and run Java applications directly from source code,
but its automatic build only supports the Amazon Corretto 8 and 11 runtimes.
This project targets Java 21, which App Runner cannot build automatically.
A container image is required instead, built locally (or in CI) from the
`Dockerfile` at the repository root and deployed to App Runner as a
container-image service.

## Dockerfile structure

The build uses a multi-stage Dockerfile to keep the final image small and
free of build tooling:

**Stage 1 (`build`):** `maven:3.9-eclipse-temurin-21` — compiles the
application. `pom.xml` is copied and dependencies resolved before the source
code is copied in, so Docker's layer cache can reuse the dependency layer
across builds when only application code changes. Tests are skipped in this
stage (`-DskipTests`); they run separately in CI.

**Stage 2 (runtime):** `eclipse-temurin:21-jre-alpine` — a minimal image
containing only the JRE. Only the built `.jar` is copied over from the build
stage; no Maven, compiler, or source code ends up in the final image.

The application listens on port 8080 (Spring Boot default, no explicit
`server.port` override in `application.properties`).

## Required environment variables

| Variable | Purpose | Default (insecure, override in production) |
|----------|---------|---------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/procurement` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | JWT signing secret | placeholder value, must be overridden |

## Building and testing locally

```bash
docker build -t agent-task-router .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/procurement \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e JWT_SECRET=<generate-with-openssl-rand-hex-32> \
  agent-task-router
```

Postgres must be reachable from inside the container (the local
`docker-compose.yml` Postgres service works for this).

## Deploying to App Runner

1. Push the built image to Amazon ECR
2. Create an App Runner service from the ECR image (not source code)
3. Configure the environment variables above in the App Runner service config
4. App Runner provisions a public HTTPS URL once the service is running
