---
title: "fix: Install StatusPages plugin and stop leaking internals via 500"
type: fix
status: active
date: 2026-04-15
---

# fix: Install StatusPages plugin and stop leaking internals via 500

## Overview

No `StatusPages` plugin is installed, so thrown exceptions bubble to 500 with
exception messages in the response body. Two concrete leaks today:
`CallExtensions.userId()` throws `IllegalStateException("User not found for
subject: $subject")`, and `Parameters.uuid()` / `localDate()` throw on bad
input — both produce 500 instead of 400 with the message exposed.

## Requirements Trace

- R1. Invalid UUID path parameter → 400, empty body.
- R2. Invalid date path parameter → 400, empty body.
- R3. User not registered (JWT subject with no matching user row) → 401 with `{"error":"user_not_registered"}`.
- R4. Any other unhandled throwable → 500 with empty body (no stack trace, no subject, no SQL).
- R5. Existing server tests pass unchanged.

## Scope Boundaries

- Not redesigning the user bootstrap flow. Client still POSTs `/api/users/me` on first login.
- Not restructuring existing route handlers. Typed validation errors inside handlers already return 400 explicitly (e.g. `checked` query param); no changes there.
- Not adding i18n for the error body.

## Context & Research

### Relevant Code and Patterns

- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/Application.kt` — where plugins are installed.
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/common/ktor/CallExtensions.kt:27` — the offending `error(...)` call.
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/common/ktor/RouteExtensions.kt` — `uuid()` and `localDate()` parse/throw.
- `gradle/libs.versions.toml:84-89` — Ktor server libs are listed individually and referenced via the `ktor-server` bundle at line 136.

## Key Technical Decisions

- **Typed exception for user-not-registered.** Replace `error(...)` with a `UserNotRegisteredException` the StatusPages block can route to 401 + structured body. The exception *class name* is what the handler keys on; the message doesn't reach the wire.
- **Catch `IllegalArgumentException` for invalid path params.** `Uuid.parse` and `LocalDate.parse` both throw `IllegalArgumentException` subclasses, so a single handler covers R1 and R2 without touching `RouteExtensions`.
- **Separate 500 catch-all with empty body.** Keeps the handler ordering explicit and prevents any future `throw RuntimeException("...")` from leaking a message.

## Implementation Units

- [ ] **Unit 1: Add StatusPages dependency**

**Goal:** The plugin class is on the classpath.

**Requirements:** R1-R4

**Files:**
- Modify: `gradle/libs.versions.toml` — add `ktor-serverStatusPages` library entry.
- Modify: `gradle/libs.versions.toml` — add `ktor-serverStatusPages` to the `ktor-server` bundle.

**Approach:**
- New library: `ktor-serverStatusPages = { module = "io.ktor:ktor-server-status-pages-jvm", version.ref = "ktor" }`.
- Append `"ktor-serverStatusPages"` to the `ktor-server` bundle array.

**Test scenarios:** none — dependency wiring only.

**Verification:** `./gradlew :server:compileKotlin` resolves.

- [ ] **Unit 2: Typed user-not-registered exception**

**Goal:** `userId()` fails in a way StatusPages can discriminate from other `IllegalStateException`s.

**Requirements:** R3

**Files:**
- Create: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/common/ktor/Errors.kt` (tiny — one exception class).
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/common/ktor/CallExtensions.kt`.

**Approach:**
- `class UserNotRegisteredException : RuntimeException()` — no message, nothing to leak.
- `CallExtensions.userId()`: replace `error("User not found for subject: $subject")` with `throw UserNotRegisteredException()`.
- Update the KDoc `@throws` to mention the new type.

**Patterns to follow:**
- Minimal exception class; no serialization. Keep it in a dedicated `Errors.kt` so future error types live next to it.

**Test scenarios:** none at unit level — behavior covered in Unit 4 integration tests.

- [ ] **Unit 3: Install StatusPages in Application.kt**

**Goal:** Wire exception handlers.

**Requirements:** R1-R4

**Files:**
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/Application.kt`.

**Approach:**
- Add `configureStatusPages()` function and call from `module()` between `configureHttp()` and `configureStaticFiles()`.
- Handler block (ordering matters — most specific first):
  - `exception<UserNotRegisteredException>` → `call.respond(HttpStatusCode.Unauthorized, ErrorResponse("user_not_registered"))`.
  - `exception<MissingRequestParameterException>` → `call.respond(HttpStatusCode.BadRequest)`.
  - `exception<IllegalArgumentException>` → `call.respond(HttpStatusCode.BadRequest)`.
  - `exception<Throwable>` → `call.respond(HttpStatusCode.InternalServerError)`.
- `ErrorResponse` is a tiny `@Serializable data class ErrorResponse(val error: String)` co-located with `UserNotRegisteredException` in `Errors.kt`.

**Patterns to follow:**
- Keep `configure*` functions `private` (matches `configureHttp`, `configureDI`).

**Test scenarios:** covered in Unit 4.

- [ ] **Unit 4: Integration tests**

**Goal:** Verify the four acceptance criteria from the issue.

**Requirements:** R1, R2, R3, R4, R5

**Files:**
- Create: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/StatusPagesTest.kt`.

**Approach:**
- Extend `BaseIntegrationTest`, authenticate with `TestJwt.generateToken(...)`.
- Cases:
  - Valid JWT but user never registered → `GET /api/shopping-lists` → 401 with body `{"error":"user_not_registered"}`.
  - Register user, then `GET /api/shopping-lists/not-a-uuid/items` → 400.
  - Register user, then `GET /api/meal-plan?from=not-a-date&to=...` → 400 (adjust to whatever meal-plan route expects — if it doesn't use `localDate()` via a path param, pick any route that does).

**Test scenarios:**
- Happy path / error path: user-not-registered → 401 + structured body.
- Edge case: malformed UUID → 400.
- Edge case: malformed date → 400.

**Verification:**
- `./gradlew :server:test` passes all old + new tests.
- `./gradlew detektAll` passes.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Existing handlers swallow an `IllegalArgumentException` they actually want to surface as 500 | None currently known; all business-logic paths use nullable returns or explicit 400s. New tests verify the happy paths still work. |
| A future route throws `IllegalArgumentException` for business reasons and gets 400 when they wanted 500 | Document the convention; handlers should use domain exceptions, not raw `IllegalArgumentException`. |

## Sources & References

- Issue: #44
- PR: #76
- Ktor `StatusPages` docs (3.4.x): default `MissingRequestParameterException` raised by `Parameters.getOrFail`.
