---
title: "fix: JWT verifier missing audience validation"
type: fix
status: completed
date: 2026-04-12
origin: docs/brainstorms/2026-04-12-jwt-audience-validation-brainstorm.md
---

# fix: JWT verifier missing audience validation

The JWT verifier in `AuthModule.kt` never calls `withAudience(...)`.
Any valid token from the same Pocket-ID instance — even one issued
for a different client — is accepted. This is an audience confusion
vulnerability (CWE-287). Fix: add `withAudience(oidcClientId)` to
the verifier block.

## Acceptance Criteria

- [x] `oidc.client-id` added to `application.yaml` mapped from
  `OIDC_CLIENT_ID` env var (no default — fail-fast if absent)
- [x] `withAudience(oidcClientId)` added to the verifier block in
  `AuthModule.kt`
- [x] Fail-fast: use `property("oidc.client-id")` (not
  `propertyOrNull`) so the server refuses to start without it
- [x] `TestJwt.generateToken` includes the `aud` claim
- [x] `TestConfig` passes `oidc.client-id` to `MapApplicationConfig`
- [x] Negative test: token with wrong audience returns 401
- [x] All existing integration tests pass
- [x] `CLAUDE.md` — no update needed (`OIDC_CLIENT_ID` already listed
  as required env var in Docker section)

## Context

### Files to change

- `server/src/main/resources/application.yaml:14-16` — add
  `client-id` under `oidc` block
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/auth/AuthModule.kt:16-37`
  — read `oidc.client-id`, add `withAudience()`
- `server/src/test/kotlin/io/github/fgrutsch/cookmaid/support/TestJwt.kt:39-50`
  — add `audience` to `JWTClaimsSet.Builder`
- `server/src/test/kotlin/io/github/fgrutsch/cookmaid/support/TestConfig.kt:1-13`
  — add `oidc.client-id` config entry

### Design decisions (see brainstorm)

- Config key `oidc.client-id` — consistent with `oidc.issuer` /
  `oidc.jwks-url` naming
- Single audience value, not a list
- `OIDC_CLIENT_ID` env var already exists for frontend injection;
  reuse for server — no new env var needed
- Fail-fast on missing config to prevent silent security regression

### SpecFlow gaps addressed

- Negative test added to acceptance criteria (issue requires
  "token for different client ID rejected with 401")
- Fail-fast startup behavior specified
- Confirmed no Docker entrypoint changes needed (server reads
  env var directly via application.yaml, not via envsubst)

## MVP

### application.yaml — add client-id

```yaml
oidc:
  issuer: ${OIDC_ISSUER}
  jwks-url: ${OIDC_JWKS_URL}
  client-id: ${OIDC_CLIENT_ID}   # no default — fail-fast if absent
```

### AuthModule.kt — read config, add withAudience

```kotlin
val oidcClientId = config.property("oidc.client-id").getString()

verifier(jwkProvider, issuer) {
    acceptLeeway(3)
    withAudience(oidcClientId)
}
```

### TestJwt.kt — add audience to generated tokens

```kotlin
const val AUDIENCE = "test-client-id"

fun generateToken(subject: String): String {
    val claimsSet = JWTClaimsSet.Builder()
        .subject(subject)
        .issuer(issuer)
        .audience(AUDIENCE)
        .expirationTime(Date(System.currentTimeMillis() + 60_000))
        .build()
    // ... sign and return
}
```

### TestConfig.kt — add audience config

```kotlin
put("oidc.client-id", TestJwt.AUDIENCE)
```

### Negative test — wrong audience returns 401

Add an overload to `TestJwt` that accepts a custom audience:

```kotlin
fun generateToken(subject: String, audience: String = AUDIENCE): String

@Test
fun `rejects token with wrong audience`() = testApplication {
    val token = TestJwt.generateToken("user-id", audience = "wrong-client")
    val response = client.get("/api/...") {
        bearerAuth(token)
    }
    assertEquals(HttpStatusCode.Unauthorized, response.status)
}
```

## Sources

- **Origin brainstorm:**
  [docs/brainstorms/2026-04-12-jwt-audience-validation-brainstorm.md](docs/brainstorms/2026-04-12-jwt-audience-validation-brainstorm.md)
- Related issue: #40
