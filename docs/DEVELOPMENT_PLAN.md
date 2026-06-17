# Development Plan — Agent Task Router

> Plano de 10 semanas. Cada fase termina com algo funcional e deployado.
> Objetivo: preencher gaps em Java, AI integration e Cloud de forma forçada pela implementação.

---

## Princípio

Cada feature que adicionas ao projeto te força a estudar um conceito novo.
Não estudas por estudar — estudas para desbloquear a próxima tarefa.

**Métrica de progresso:** commits no GitHub. Nada mais conta.

---

## Fase 1 — Java Fundamentos via Spring Boot (Semanas 1–2)

**Objetivo:** projeto com banco funcionando, primeiros endpoints reais.

### O que implementas

- [ ] Entidade `Agent` com campos: `id` (UUID), `name`, `endpointUrl`, `capabilities` (lista de strings), `status` (enum), `maxConcurrency`, `createdAt`
- [ ] `AgentRepository` extendendo `JpaRepository`
- [ ] `AgentService` com métodos `register(AgentRequest)` e `findById(UUID)`
- [ ] `AgentController` com `POST /agents` e `GET /agents/{id}`
- [ ] Validação de input com `@Valid`, `@NotBlank`, `@NotNull`
- [ ] Handler global de erros com `@ControllerAdvice`
- [ ] Flyway: primeira migration `V1__create_agents_table.sql`

### O que estudas para conseguir fazer isso

| Conceito | Recurso |
|---------|---------|
| `@Entity`, `@Id`, `@GeneratedValue`, `@Column` | Spring Data JPA docs |
| `@Enumerated(EnumType.STRING)` | JPA enum mapping |
| `JpaRepository` — métodos automáticos | Spring Data docs |
| `@Service`, `@Transactional` | Spring docs |
| `@RestController`, `@RequestBody`, `@PathVariable` | Spring MVC docs |
| `@Valid` + Bean Validation (`@NotBlank`, `@Size`) | Jakarta Validation |
| `@ControllerAdvice`, `@ExceptionHandler` | Spring MVC exception handling |
| Flyway — como escrever migrations SQL | Flyway docs |
| UUID como chave primária em JPA | Stack Overflow / Baeldung |

### Java 21 que praticas aqui

- **Records** — usa para os DTOs de request/response em vez de classes com getters
  ```java
  public record AgentRequest(String name, String endpointUrl, List<String> capabilities) {}
  ```
- **`var`** — usa em variáveis locais onde o tipo é óbvio

### Entregável da fase

`POST /agents` funcionando com validação. `GET /agents/{id}` retorna 404 correto se não existe. Migrations rodando via Flyway. Tudo testável via Postman/curl.

---

## Fase 2 — Tasks e Lógica de Routing (Semanas 3–4)

**Objetivo:** submeter uma task e ela ser atribuída a um agente com capability correta.

### O que implementas

- [ ] Entidade `Task` com: `id`, `type`, `payload` (JSONB), `priority` (enum), `status` (enum), `assignedAgentId`, `createdAt`, `completedAt`
- [ ] `TaskRepository` com query: `findByStatus(TaskStatus status)`
- [ ] `RoutingEngine` — lógica: dado um tipo de task, encontra agente ACTIVE que tenha essa capability e menor carga atual
- [ ] `TaskService.submit(TaskRequest)` — persiste task, chama routing engine, atualiza `assignedAgentId`
- [ ] `POST /tasks` e `GET /tasks/{id}`
- [ ] `GET /tasks?status=PENDING` com `@RequestParam`
- [ ] Flyway: `V2__create_tasks_table.sql`

### O que estudas

| Conceito | Recurso |
|---------|---------|
| `@ManyToOne` e foreign keys em JPA | Spring Data JPA docs |
| `@Column(columnDefinition = "jsonb")` | PostgreSQL JSONB com Hibernate |
| JPQL queries customizadas | `@Query` annotation |
| `@RequestParam` com valores opcionais | Spring MVC |
| Transações entre services (`@Transactional` propagation) | Spring docs |

### Java 21 que praticas aqui

- **Sealed classes** — usa para representar os estados possíveis de uma Task:
  ```java
  public sealed interface TaskResult permits TaskResult.Success, TaskResult.Failure {}
  ```
- **Pattern matching** — usa no routing engine para tratar cada tipo de resultado

### Entregável da fase

`POST /tasks` com `{"type": "text-generation", "payload": {"prompt": "hello"}}` retorna task com `assignedAgentId` preenchido. Routing funciona por capability match simples (string comparison por agora).

---

## Fase 3 — Execução Assíncrona e State Machine (Semana 5)

**Objetivo:** execução real em background, transições de estado explícitas.

### O que implementas

- [ ] `ExecutionEngine` com `@Async` — dispara execução sem bloquear o request
- [ ] Entidade `Execution` — registo de cada tentativa (agentId, taskId, attempt, status, startedAt, finishedAt, errorMessage, output JSONB)
- [ ] Spring State Machine config: PENDING → ROUTING → EXECUTING → SUCCESS / FAILED
- [ ] Cada transição de estado persiste no banco
- [ ] `POST /tasks/{id}/retry` — cria nova execução para task FAILED
- [ ] Flyway: `V3__create_executions_table.sql`
- [ ] Configurar Virtual Threads para o executor assíncrono

### O que estudas

| Conceito | Recurso |
|---------|---------|
| `@Async` e `@EnableAsync` | Spring Async docs |
| Virtual Threads (Java 21) — `Executors.newVirtualThreadPerTaskExecutor()` | Java 21 release notes |
| Spring State Machine — `StateMachineConfigurerAdapter` | Spring State Machine docs |
| `@OneToMany` e cascades | JPA docs |
| Concorrência: o que é thread-safe no Spring | Baeldung |

### Java 21 que praticas aqui

- **Virtual Threads** — configura o `AsyncConfig` para usar virtual threads:
  ```java
  executor.setTaskExecutor(runnable -> Thread.ofVirtual().start(runnable));
  ```

### Entregável da fase

Submeter task → execução dispara em background → estado muda de PENDING → ROUTING → EXECUTING → SUCCESS. `GET /tasks/{id}` retorna estado atualizado em tempo real.

---

## Fase 4 — Segurança e Autenticação JWT (Semana 6)

**Objetivo:** API protegida com JWT. Agentes só executam se autenticados.

### O que implementas

- [ ] `POST /auth/register` — cria user com senha hasheada (BCrypt)
- [x] `POST /auth/login` — retorna JWT ✅ 2026-06-17
- [x] `JwtFilter` — valida token em cada request ✅ 2026-06-17
- [ ] Liberar apenas `/auth/**`, `/health/**`, `/actuator/prometheus` sem token
- [ ] `UserDetails` + `UserDetailsService` customizados
- [ ] Role-based: `ROLE_ADMIN` pode criar/deletar agentes, `ROLE_USER` pode submeter tasks

### O que estudas

| Conceito | Recurso |
|---------|---------|
| Spring Security 6 filter chain | Spring Security docs |
| JWT — o que é, como assinar, como validar | jwt.io + JJWT library |
| `BCryptPasswordEncoder` | Spring Security |
| `OncePerRequestFilter` | Spring Security |
| `@PreAuthorize("hasRole('ADMIN')")` | Method security |

### Entregável da fase

Todos os endpoints protegidos. Fluxo: `POST /auth/login` → recebe token → usa token no `Authorization: Bearer` header → funciona.

---

## Fase 5 — AI Integration com Spring AI (Semana 7)

**Objetivo:** routing semântico — em vez de comparar strings, usa embeddings para encontrar o agente mais adequado semanticamente.

### O que implementas

- [ ] Adicionar `spring-ai-anthropic-spring-boot-starter` ao `pom.xml`
- [ ] Ao registar um agente, gerar embedding das suas capabilities via Claude API e guardar em `agents.embedding` (pgvector)
- [ ] `EmbeddingMatcher` — dado o tipo da task, gera embedding e busca agente com maior cosine similarity
- [ ] Substituir routing por string no `RoutingEngine` por routing semântico
- [ ] Adicionar pgvector ao `docker-compose.yml`
- [ ] Migration: `V4__add_embedding_column.sql` — `ALTER TABLE agents ADD COLUMN embedding vector(1536)`

### O que estudas

| Conceito | Recurso |
|---------|---------|
| O que são embeddings e como funcionam | OpenAI embeddings guide |
| Cosine similarity | Wikipedia + implementação prática |
| pgvector — `<=>` operator para similarity search | pgvector GitHub |
| Spring AI — `EmbeddingClient` | Spring AI docs |
| `@NativeQuery` com pgvector | Hibernate + pgvector |

### O que adicionas ao CV depois desta fase

- Spring AI (embeddings)
- pgvector (vector database)
- Semantic search
- AI-powered routing

### Entregável da fase

Registar agente com capabilities `["summarization", "text-compression"]`. Submeter task de tipo `"condense-document"` → routing encontra esse agente por similaridade semântica mesmo sem match exato de string.

---

## Fase 6 — Observabilidade: Metrics, Logs e Traces (Semana 8)

**Objetivo:** sistema observável em produção. Grafana dashboard funcional.

### O que implementas

- [ ] Micrometer: contador de execuções por agente, timer de duração média, gauge de tasks pendentes
- [ ] Prometheus scrape endpoint via Spring Actuator (`/actuator/prometheus`)
- [ ] Grafana dashboard com panels: taxa de sucesso/falha, tempo médio por agente, tasks pendentes
- [ ] Structured logging em JSON com Logback — cada log tem `traceId`, `taskId`, `agentId`
- [ ] OpenTelemetry: trace por execução de task (span desde recebimento até conclusão)
- [ ] Jaeger: visualizar traces no browser
- [ ] `docker-compose.yml` com Prometheus + Grafana + Jaeger

### O que estudas

| Conceito | Recurso |
|---------|---------|
| O que são métricas vs logs vs traces | "Observability Engineering" livro (ler capítulo 1-2) |
| Micrometer — `Counter`, `Timer`, `Gauge` | Micrometer docs |
| Prometheus — format de métricas, PromQL básico | Prometheus docs |
| Grafana — criar dashboard, panels, alertas | Grafana docs |
| OpenTelemetry Java agent | OTEL Java docs |
| Logback + estruturado JSON | logstash-logback-encoder |

### O que adicionas ao CV depois desta fase

- Micrometer + Prometheus + Grafana (observability stack)
- OpenTelemetry (distributed tracing)
- Structured logging
- Grafana dashboard design

### Entregável da fase

Executar 10 tasks. Abrir Grafana em `localhost:3000` e ver: taxa de sucesso, tempo médio, tasks por agente — tudo em tempo real.

---

## Fase 7 — Redis, Circuit Breaker e Resiliência (Semana 9)

**Objetivo:** sistema que aguenta falhas e não sobrecarrega agentes.

### O que implementas

- [ ] Redis cache: `agents:active` atualizado a cada registro/desativação — evita query ao DB em cada routing
- [ ] Distributed lock com Redis: garantir que a mesma task não é atribuída a dois agentes simultaneamente em chamadas concorrentes
- [ ] Resilience4j Circuit Breaker: se um agente falha 5x seguidas, circuit abre e ele fica `OVERLOADED` por 60s
- [ ] `@Retry` annotation para tasks que podem ser tentadas novamente automaticamente
- [ ] Redis TTL: cache de disponibilidade expira em 30s

### O que estudas

| Conceito | Recurso |
|---------|---------|
| Redis — estruturas de dados, TTL, comandos básicos | Redis docs |
| Spring Data Redis | Spring docs |
| Distributed lock com Redisson | Redisson docs |
| Circuit Breaker pattern — o que é, por que usar | Martin Fowler |
| Resilience4j — `@CircuitBreaker`, `@Retry` | Resilience4j docs |
| Race conditions em sistemas distribuídos | Concorrência em sistemas distribuídos |

### O que adicionas ao CV depois desta fase

- Redis (caching + distributed locking)
- Resilience4j (circuit breaker, retry)
- Distributed systems patterns

---

## Fase 8 — Testes, CI/CD e Deploy (Semana 10)

**Objetivo:** pipeline completo do commit ao deploy. Projeto em produção com URL pública.

### O que implementas

**Testes:**
- [ ] `AgentServiceTest` — unit tests com Mockito
- [ ] `TaskControllerTest` — integration test com `MockMvc`
- [ ] `RoutingEngineIntegrationTest` — Testcontainers com PostgreSQL real
- [ ] `ExecutionFlowTest` — teste end-to-end do fluxo completo
- [ ] JaCoCo: cobertura mínima de 70% nas classes de domínio

**CI/CD:**
- [ ] `.github/workflows/ci.yml`: no push para `main` → rodar testes → build Docker image → push para Docker Hub
- [ ] `.github/workflows/deploy.yml`: no push para `main` com testes passando → deploy no Railway

**Deploy:**
- [ ] Dockerfile otimizado (multi-stage build — imagem final < 200MB)
- [ ] `docker-compose.prod.yml` — sem Grafana/Jaeger, só app + PostgreSQL + Redis
- [ ] Railway deploy com variáveis de ambiente
- [ ] README com URL pública e badge do CI

### O que estudas

| Conceito | Recurso |
|---------|---------|
| Testcontainers — PostgreSQL, Redis em testes | Testcontainers docs |
| MockMvc — testar controllers sem servidor | Spring Testing docs |
| Mockito — `@Mock`, `@InjectMocks`, `when().thenReturn()` | Mockito docs |
| JaCoCo — relatório de cobertura | JaCoCo Maven plugin |
| GitHub Actions — `workflow`, `job`, `step` | GitHub Actions docs |
| Docker multi-stage build | Docker docs |
| Railway deploy | Railway docs |

### O que adicionas ao CV depois desta fase

- Testcontainers
- GitHub Actions CI/CD
- Docker multi-stage build
- Railway (cloud deploy)
- JaCoCo (test coverage)

### Entregável final

`git push origin main` → GitHub Actions roda testes → build Docker image → push para Docker Hub → deploy no Railway → URL pública funcional.

---

## Resumo — Skills adquiridas ao final

### Backend Java
- [ ] Java 21 (virtual threads, records, sealed classes, pattern matching)
- [ ] Spring Boot 3.3 (full production config)
- [ ] Spring Security 6 (JWT, role-based)
- [ ] Spring Data JPA + Hibernate
- [ ] Spring State Machine
- [ ] Spring Async + Virtual Threads
- [ ] Flyway (migrations)
- [ ] Hexagonal Architecture

### AI Integration
- [ ] Spring AI
- [ ] Embeddings + semantic search
- [ ] pgvector
- [ ] Anthropic Claude API

### Data
- [ ] PostgreSQL (avançado — JSONB, pgvector, índices)
- [ ] Redis (cache, TTL, distributed lock)

### Observabilidade
- [ ] Micrometer
- [ ] Prometheus + PromQL
- [ ] Grafana (dashboard design)
- [ ] OpenTelemetry + Jaeger
- [ ] Structured logging (JSON)

### Resiliência
- [ ] Resilience4j (circuit breaker, retry)
- [ ] Distributed locking

### Testes
- [ ] JUnit 5
- [ ] Mockito
- [ ] Testcontainers
- [ ] MockMvc
- [ ] JaCoCo

### Cloud & DevOps
- [ ] Docker + Docker Compose
- [ ] Docker multi-stage build
- [ ] Docker Hub
- [ ] GitHub Actions (CI/CD)
- [ ] Railway (deploy)

---

## Ritmo recomendado

- **Bloco diário:** 45–60 min (não mais — sustentabilidade)
- **Frequência:** 5–6 dias/semana
- **Entregável semanal:** pelo menos 1 endpoint novo funcionando e commitado
- **Não pular fase:** cada fase usa o que foi feito na anterior

---

*Atualizado: 2026-06-09*
