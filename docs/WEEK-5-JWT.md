# Semana 5 — Spring Security + JWT

> Teach-to-learn: escrito como se explicasse para um dev JS que nunca viu Java.
> Data: 2026-06-17 | Status: autenticação básica funcionando ✅

---

## O que aprendi hoje

### 1. Como o Spring registra dependências — o problema do campo faltando

Em JavaScript, você pode importar qualquer coisa no topo do arquivo e usar diretamente. Em Spring (Java), isso não existe — você precisa **declarar o campo** na classe antes de poder injetar algo nele.

O erro foi exatamente esse:

```java
// ERRADO — Spring não sabe onde colocar a dependência
public class AuthController {
    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider; // ← "jwtTokenProvider cannot be found"
    }
}
```

```java
// CORRETO — campo declarado, Spring injeta no construtor
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider; // ← campo aqui

    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider; // agora funciona
    }
}
```

**Por que `private final`?** Boa prática: imutável após construção. Se você puder mudar a dependência depois de criar o objeto, você tem um problema de design.

---

### 2. Docker — container vs imagem

Analogia JS: **imagem = package.json** (receita), **container = node_modules rodando** (instância).

Quando você faz `docker-compose up -d`, o Docker:
1. Lê a imagem (`postgres:16`)
2. Cria um container com nome específico (`procurement-db`)
3. Sobe o processo

O erro de hoje:
```
Conflict. The container name "/procurement-db" is already in use
```

Isso significa: a "instância" já existe (de uma sessão anterior) mas está parada. O Docker não cria duas instâncias com o mesmo nome. Opções:
- `docker start procurement-db` — liga o que estava parado
- `docker rm procurement-db` + `docker-compose up -d` — recria do zero

---

### 3. Base64 vs Base64URL — por que `-` quebrou o JWT

JWT internamente usa **Base64URL**, que troca `+` por `-` e `/` por `_` para ser seguro em URLs.

O problema estava em `JwtTokenProvider.getSigninKey()`:

```java
// ERRADO — Decoders.BASE64 (padrão) não aceita '-' nem '_'
return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
```

O secret default em `application.properties` era:
```
changeme-generate-with-openssl-rand-hex-32
```

Esse valor tem `-` no meio. O `Decoders.BASE64` (Base64 padrão, RFC 4648) não aceita esse caractere — ele é válido em Base64**URL** mas não em Base64 padrão. Resultado: `DecodingException: Illegal base64 character: '-'`

**Fix:** usar os bytes diretos em vez de decodificar Base64:

```java
// CORRETO — raw bytes, sem decodificação
return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
```

Por que funciona? `Keys.hmacShaKeyFor` só precisa de ≥32 bytes para HMAC-SHA256. A string tem 36 chars = 36 bytes. Sem Base64 envolvido.

**Em produção:** o secret seria gerado assim:
```bash
openssl rand -base64 32
# ex: xK8mP3nL9qR5tY2wV7jB1sD4eH6gF0iN
```
Aí sim usaria `Decoders.BASE64` porque o output do openssl É Base64 válido.

---

### 4. Spring Security Filter Chain

Analogia JS: **middleware do Express**. Cada request passa por uma fila de middlewares antes de chegar no controller.

```
Request → [JwtAuthFilter] → [AuthorizationFilter] → [Controller]
```

O `JwtAuthFilter` extends `OncePerRequestFilter` — garante que roda exatamente uma vez por request (sem duplicação em redirects).

O que ele faz:
1. Lê o header `Authorization: Bearer <token>`
2. Extrai e valida o token
3. Se válido: coloca o usuário no `SecurityContextHolder` (Spring sabe quem é)
4. Deixa o request continuar

```java
// Fluxo dentro do filtro
String token = extractToken(request);     // pega o token do header
if (token != null && jwtTokenProvider.validateToken(token)) {
    String email = jwtTokenProvider.getEmailFromToken(token);
    // ... cria Authentication e coloca no SecurityContext
}
filterChain.doFilter(request, response); // continua para o próximo filtro
```

---

### 5. JWT — o que é e o que não é

**JWT = JSON Web Token.** Não é criptografia — é **assinatura**. Qualquer um pode ler o conteúdo (decodificando Base64), mas só quem tem a chave secreta pode **criar um token válido**.

Estrutura: `header.payload.signature` (3 partes separadas por `.`)

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkB0ZXN0LmNvbSJ9.abc123
    ↑ header                ↑ payload (email, expiry)        ↑ assinatura
```

Como o backend valida:
1. Separa as 3 partes
2. Recalcula a assinatura com a chave secreta
3. Compara com a assinatura recebida
4. Se bater → token legítimo (não foi alterado)

---

## O que funciona agora

```bash
# Login — retorna token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"123456"}'
# → {"token": "eyJ..."}

# Rota pública — sem token
curl http://localhost:8080/health
# → {"status": "ok"}
```

---

## O que ainda falta (próxima sessão)

Ver `DEVELOPMENT_PLAN.md` Fase 4 — itens pendentes:
- `POST /auth/register` com BCrypt (senha hasheada no banco)
- `UserDetails` + `UserDetailsService` — tirar as credenciais hardcoded
- Role-based: `ROLE_ADMIN` vs `ROLE_USER`

---

## Bugs encontrados e o que ensinaram

| Bug | Causa | Lição |
|-----|-------|-------|
| `cannot find symbol: variable jwtTokenProvider` | Campo não declarado na classe | Em Java, dependência injetável precisa de campo declarado |
| `DecodingException: Illegal base64 character '-'` | Secret com `-` sendo decodificado como Base64 padrão | Base64 padrão ≠ Base64URL. Para secrets simples em dev, raw bytes é mais robusto |
| `Conflict: container name already in use` | Container Docker parado ainda existe | `docker ps -a` mostra todos (inclusive parados) |
