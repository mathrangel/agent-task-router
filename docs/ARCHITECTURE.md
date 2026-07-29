# Architecture Decisions — Agent Task Router

> Por que cada escolha foi feita. Este documento existe para explicar em entrevistas.
>
> Dividido em duas partes: decisões **em vigor** (implementadas e verificadas contra o
> código) e um **backlog de decisões** — direção já definida, ainda não construída.
> Um ADR só sai do backlog quando o código que ele descreve existe de verdade.

---

## Decisões em vigor

### ADR-002 — UUID como chave primária

**Decisão:** `@GeneratedValue(strategy = GenerationType.UUID)` em vez de auto-increment.

**Por quê:**
- IDs não são sequenciais — não vaza volume de negócio para o cliente
- Pode ser gerado antes de persistir (útil em sistemas distribuídos)
- Compatível com Kafka, eventos assíncronos sem depender do ID gerado pelo banco

**Trade-off:** UUID ocupa mais espaço no índice que um BIGINT. Irrelevante até dezenas de milhões de registos.

---

### ADR-003 — Flyway para migrations

**Decisão:** todo schema change vai via Flyway em vez de `spring.jpa.hibernate.ddl-auto=update`.

**Por quê:**
- `ddl-auto=update` é perigoso em produção — pode dropar colunas silenciosamente
- Flyway tem histórico auditável de todas as mudanças de schema
- Migrations são code-reviewed igual ao código da aplicação

**Como funciona:** ao subir a aplicação, Flyway verifica quais `V{N}__*.sql` ainda não foram executados e os roda em ordem.

---

### ADR-011 — Redis provisionado antecipadamente, sem implementação atrelada

**Decisão:** Redis foi adicionado ao `docker-compose.yml` como infraestrutura genérica de cache, ainda sem nenhum caso de uso implementado no código.

**Por quê:**
- Provisionar agora tem custo de fricção quase nulo — um serviço a mais no compose, sem tocar em código existente
- Destrava o ADR-005 (backlog): quando a implementação do cache de agentes for feita, a infra já está pronta, sem esperar por deploy de infraestrutura no meio do caminho
- É parte de uma decisão maior de stack (as tecnologias que o projeto vai usar), não um caso isolado de otimização prematura

**Status:** infraestrutura provisionada (docker-compose) · implementação do cache ainda pendente (ver ADR-005 no backlog)

---

## Backlog — direção decidida, ainda não implementada

> Estes ADRs descrevem escolhas já feitas para quando essa parte do projeto for
> construída (ver `docs/DEVELOPMENT_PLAN.md` para as fases). Não descrevem o
> código como ele é hoje.

### ADR-001 — Hexagonal Architecture (Ports & Adapters)

**Decisão planejada:** organizar o código em camadas `domain`, `application`, `infra`, `api` por bounded context.

**Estado real hoje:** package-by-feature simples (`agent/`, `task/`, `controller/`, `security/`, `entity/`, `repository/`, `dto/`), sem essas camadas. Este ADR nunca foi executado — ficou registado como direção, não como decisão em vigor.

**Por quê (quando for feito):**
- O domínio (Agent, Task, Execution) não dependeria do Spring, JPA ou HTTP
- Fácil de testar: testa o domínio sem banco, sem servidor
- Fácil de trocar: substituir PostgreSQL por outro banco sem tocar no domain ou application

**Trade-off:** mais estrutura inicial. Para o tamanho atual do projeto, é overkill — motivo real de ainda não ter sido feito.

---

### ADR-004 — JSONB para payload das tasks

**Decisão planejada:** `payload` como `JSONB` no PostgreSQL em vez de coluna de texto.

**Estado real hoje:** a coluna `payload` na tabela `tasks` (migration `V3`) é `TEXT`. Nunca foi migrada para `JSONB` — decisão registada, implementação divergiu e ninguém atualizou este ADR até agora.

**Por quê (quando for feito):**
- Cada tipo de task tem payload diferente (text task tem `prompt`, code task tem `code` + `language`, etc.)
- Não precisaria de uma tabela por tipo — JSONB é flexível
- PostgreSQL indexa JSONB com GIN index — queries performáticas

**Trade-off:** sem validação de schema no banco. Validação ficaria na camada de aplicação.

---

### ADR-005 — Redis para cache de agentes disponíveis

**Decisão planejada:** manter cache `agents:active` no Redis, atualizado a cada mudança de estado.

**Estado real hoje:** não implementado. `AgentService` não toca no Redis — Redis existe só no `docker-compose.yml` (ver ADR-011).

**Por quê (quando for feito):**
- Routing acontece em cada task submetida — sem cache, cada task faz query no banco
- Redis tem latência < 1ms vs PostgreSQL ~5-10ms para esta query
- Cache com TTL de 30s garantiria que agentes que caem são removidos do routing em até 30s

---

### ADR-006 — Spring State Machine para execuções

**Decisão planejada:** usar Spring State Machine em vez de if/else para transições de estado.

**Estado real hoje:** não implementado — não há execução assíncrona nem máquina de estados no código (Fase 3 do `DEVELOPMENT_PLAN.md`).

**Por quê (quando for feito):**
- Torna os estados e transições explícitos e auditáveis
- Impede transições inválidas (não iria de SUCCESS para EXECUTING)
- Fácil de adicionar listeners em cada transição (para logging, métricas)

**Estados planejados:**
```
PENDING → ROUTING → EXECUTING → SUCCESS
                          └→ FAILED → (retry) → EXECUTING
                                    → CANCELLED (max retries)
```

---

### ADR-007 — Semantic routing com embeddings

**Decisão planejada:** usar embeddings para match task↔agent em vez de comparação de strings.

**Estado real hoje:** não implementado — não há routing de nenhum tipo (Fase 5 do `DEVELOPMENT_PLAN.md`, depende da Fase 2 primeiro).

**Por quê (quando for feito):**
- Match por string falharia: task `"condense-document"` não encontraria agente com capability `"summarization"` mesmo sendo semanticamente igual
- Embeddings capturam significado semântico — `"code-review"` e `"analyze-code"` teriam alta similaridade
- pgvector no mesmo banco PostgreSQL — sem infra adicional para vector store

**Como funcionaria:**
1. Ao registar agente, gera embedding das capabilities concatenadas
2. Ao receber task, gera embedding do tipo da task
3. Query pgvector: `ORDER BY embedding <=> $task_embedding LIMIT 1`

---

### ADR-008 — Virtual Threads para execução assíncrona

**Decisão planejada:** usar Virtual Threads (Java 21) no executor assíncrono em vez de thread pool tradicional.

**Estado real hoje:** não implementado — não há executor assíncrono ainda.

**Por quê (quando for feito):**
- Virtual threads são leves — permitem milhares sem consumir memória de heap como threads normais
- Execução de tasks pode bloquear I/O (chamada HTTP ao agente) — virtual threads são ideais para I/O-bound work
- Não precisaria dimensionar thread pool — a JVM gere automaticamente

---

### ADR-009 — OpenTelemetry em vez de Zipkin/Sleuth

**Decisão planejada:** OpenTelemetry como standard de tracing.

**Estado real hoje:** não implementado — não há tracing distribuído (Fase 6 do `DEVELOPMENT_PLAN.md`).

**Por quê (quando for feito):**
- Spring Cloud Sleuth foi descontinuado no Spring Boot 3
- OpenTelemetry é o standard open-source da CNCF — vendor neutral
- Funciona com Jaeger, Zipkin, Datadog, Honeycomb — sem lock-in

---

### ADR-010 — Resilience4j Circuit Breaker por agente

**Decisão planejada:** cada agente teria o seu próprio Circuit Breaker independente.

**Estado real hoje:** não implementado (Fase 7 do `DEVELOPMENT_PLAN.md`).

**Por quê (quando for feito):**
- Se o agente A está falhando, não deveria impactar o routing para o agente B
- Circuit por agente permitiria granularidade fina: abrir/fechar por agente individualmente
- Evitaria cascading failures: um agente sobrecarregado ficaria `OVERLOADED` e sairia do routing
