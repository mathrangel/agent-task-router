# Architecture Decisions — Agent Task Router

> Por que cada escolha foi feita. Este documento existe para explicar em entrevistas.

---

## ADR-001 — Hexagonal Architecture (Ports & Adapters)

**Decisão:** organizar o código em camadas `domain`, `application`, `infra`, `api` por bounded context.

**Por quê:**
- O domínio (Agent, Task, Execution) não depende do Spring, JPA ou HTTP
- Fácil de testar: testa o domínio sem banco, sem servidor
- Fácil de trocar: amanhã posso substituir PostgreSQL por outro banco sem tocar no domain ou application

**Trade-off:** mais estrutura inicial. Para projetos pequenos pode parecer overkill.

**Quando usar em produção:** qualquer sistema com lógica de negócio não trivial. Padrão dominante em empresas EU que trabalham com DDD.

---

## ADR-002 — UUID como chave primária

**Decisão:** `@GeneratedValue(strategy = GenerationType.UUID)` em vez de auto-increment.

**Por quê:**
- IDs não são sequenciais — não vaza volume de negócio para o cliente
- Pode ser gerado antes de persistir (útil em sistemas distribuídos)
- Compatível com Kafka, eventos assíncronos sem depender do ID gerado pelo banco

**Trade-off:** UUID ocupa mais espaço no índice que um BIGINT. Irrelevante até dezenas de milhões de registos.

---

## ADR-003 — Flyway para migrations

**Decisão:** todo schema change vai via Flyway em vez de `spring.jpa.hibernate.ddl-auto=update`.

**Por quê:**
- `ddl-auto=update` é perigoso em produção — pode dropar colunas silenciosamente
- Flyway tem histórico auditável de todas as mudanças de schema
- Migrations são code-reviewed igual ao código da aplicação

**Como funciona:** ao subir a aplicação, Flyway verifica quais `V{N}__*.sql` ainda não foram executados e os roda em ordem.

---

## ADR-004 — JSONB para payload das tasks

**Decisão:** `payload` é `JSONB` no PostgreSQL em vez de colunas tipadas.

**Por quê:**
- Cada tipo de task tem payload diferente (text task tem `prompt`, code task tem `code` + `language`, etc.)
- Não precisamos de uma tabela por tipo — JSONB é flexível
- PostgreSQL indexa JSONB com GIN index — queries performáticas

**Trade-off:** sem validação de schema no banco. Validação acontece na camada de application.

---

## ADR-005 — Redis para cache de agentes disponíveis

**Decisão:** manter cache `agents:active` no Redis atualizado a cada mudança de estado.

**Por quê:**
- Routing acontece em cada task submetida — sem cache, cada task faz query no banco
- Redis tem latência < 1ms vs PostgreSQL ~ 5-10ms para esta query
- Cache com TTL de 30s garante que agentes que caem são removidos do routing em até 30s

**Invalidação:** `AgentService` invalida o cache ao ativar/desativar agente.

---

## ADR-006 — Spring State Machine para execuções

**Decisão:** usar Spring State Machine em vez de if/else para transições de estado.

**Por quê:**
- Torna os estados e transições explícitos e auditáveis
- Impede transições inválidas (não podes ir de SUCCESS para EXECUTING)
- Fácil de adicionar listeners em cada transição (para logging, métricas)

**Estados:**
```
PENDING → ROUTING → EXECUTING → SUCCESS
                          └→ FAILED → (retry) → EXECUTING
                                    → CANCELLED (max retries)
```

---

## ADR-007 — Semantic routing com embeddings

**Decisão:** usar embeddings para match task↔agent em vez de comparação de strings.

**Por quê:**
- Match por string falharia: task `"condense-document"` não ia encontrar agente com capability `"summarization"` mesmo sendo semanticamente igual
- Embeddings capturam significado semântico — `"code-review"` e `"analyze-code"` têm alta similaridade
- pgvector no mesmo banco PostgreSQL — sem infra adicional para vector store

**Como funciona:**
1. Ao registar agente, gera embedding das capabilities concatenadas
2. Ao receber task, gera embedding do tipo da task
3. Query pgvector: `ORDER BY embedding <=> $task_embedding LIMIT 1`

---

## ADR-008 — Virtual Threads para execução assíncrona

**Decisão:** usar Virtual Threads (Java 21) no executor assíncrono em vez de thread pool tradicional.

**Por quê:**
- Virtual threads são leves — podes ter milhares sem consumir memória de heap como threads normais
- Execução de tasks pode bloquear I/O (chamada HTTP ao agente) — virtual threads são ideais para I/O-bound work
- Não precisas dimensionar thread pool — a JVM gere automaticamente

**Configuração:**
```java
@Bean
public Executor taskExecutor() {
    return runnable -> Thread.ofVirtual().name("task-executor").start(runnable);
}
```

---

## ADR-009 — OpenTelemetry em vez de Zipkin/Sleuth

**Decisão:** OpenTelemetry como standard de tracing.

**Por quê:**
- Spring Cloud Sleuth foi descontinuado no Spring Boot 3
- OpenTelemetry é o standard open-source da CNCF — vendor neutral
- Funciona com Jaeger, Zipkin, Datadog, Honeycomb — sem lock-in
- O Java agent de OTEL instrumena Spring Boot automaticamente sem mudanças de código

---

## ADR-010 — Resilience4j Circuit Breaker por agente

**Decisão:** cada agente tem o seu próprio Circuit Breaker independente.

**Por quê:**
- Se o agente A está falhando, não deve impactar o routing para o agente B
- Circuit por agente permite granularidade fina: abrir/fechar por agente individualmente
- Evita cascading failures: um agente sobrecarregado fica `OVERLOADED` e sai do routing

**Configuração por agente:**
```yaml
resilience4j.circuitbreaker.instances:
  agent-{id}:
    failureRateThreshold: 50       # abre se 50% falham
    waitDurationInOpenState: 60s   # tenta de novo após 60s
    slidingWindowSize: 10          # janela de 10 chamadas
```
