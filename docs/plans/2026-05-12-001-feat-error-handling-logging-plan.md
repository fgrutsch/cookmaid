---
title: "feat: Add structured logging and improve error handling"
type: feat
status: active
date: 2026-05-12
---

# feat: Add structured logging and improve error handling

## Summary

Add structured logging across the server (info-level for mutations, debug-level for
validation/auth) and client (error-level in MviViewModel, debug-level in auth and
repositories). Fix the silent exception swallowing in the StatusPages catch-all and
in AuthViewModel, and replace `e.printStackTrace()` with proper logger calls.

---

## Problem Frame

The server has almost no logging — only one info log in `UserService.getOrCreate`.
All other create/update/delete operations, ownership check failures, and the
StatusPages catch-all handler are completely silent. On the client side, exceptions
caught by `MviViewModel.launch` are discarded via a no-op `onError`, and
`AuthViewModel.initialize()` swallows `CancellationException`. These gaps make
production debugging and incident response nearly impossible.

---

## Assumptions

*This plan was authored without synchronous user confirmation. The items below are
agent inferences that fill gaps in the input — un-validated bets that should be
reviewed before implementation proceeds.*

- Logging in services (not routes) is the right layer — consistent with the
  existing `UserService` pattern and keeps route handlers thin.
- The `kotlin-logging` library (already in the version catalog) is the right
  choice for client-side multiplatform logging — no new dependency introduction.
- Centralizing error logging in `MviViewModel.onError` (base class) rather than
  requiring each subclass to log is the preferred approach.

---

## Requirements

- R1. Server: info-level log on every create, update, and delete operation
- R2. Server: debug-level log on validation failures, auth problems,
  and ownership check failures
- R3. Server: log unhandled exceptions in the StatusPages catch-all handler
- R4. Client: error-level log in MviViewModel when exceptions are caught
- R5. Client: debug-level logs for auth state transitions (login, logout,
  auto-login success/failure)
- R6. Fix CancellationException swallowing in AuthViewModel.initialize()
- R7. Replace `e.printStackTrace()` with structured logging

---

## Scope Boundaries

- No changes to log output format (logback.xml pattern stays as-is)
- No log aggregation, external log shipping, or monitoring integration
- No new exception types beyond what already exists
- No changes to HTTP response codes or error response bodies
- No routine read-path logging (find/list operations remain unlogged);
  debug-level logs for ownership check failures during reads are in scope

---

## Context & Research

### Relevant Code and Patterns

- `server/.../user/UserService.kt:19` — logger declaration pattern:
  `private val logger = KotlinLogging.logger {}`
- `server/.../user/UserService.kt:59` — info log pattern:
  `logger.info { "New user created: id=${user.id}, oidcSubject=$oidcSubject" }`
- `server/.../Application.kt:80-95` — StatusPages configuration, catch-all
  discards exception (`cause` named `_`)
- `composeApp/.../ui/common/MviViewModel.kt:59-71` — `launch {}` catch block
  routes to `onError`
- `composeApp/.../ui/auth/AuthViewModel.kt:30` — bare `catch (_: Exception)`
  swallows CancellationException
- `composeApp/.../ui/auth/AuthViewModel.kt:87` — `e.printStackTrace()` in logout
- `gradle/libs.versions.toml:122` — `kotlin-logging` already cataloged as
  multiplatform library
- `config/detekt/detekt.yml` — `PrintStackTrace: active: true`,
  `SwallowedException: active: true`

### Institutional Learnings

No `docs/solutions/` directory exists. CLAUDE.md documents the
CancellationException rethrow convention modeled in `MviViewModel.launch`.

---

## Key Technical Decisions

- **Log in services, not routes** (with StatusPages exception): Follows the
  existing `UserService` pattern. Services are the business logic layer and have
  access to userId and entity IDs. Routes stay thin request/response mappers.
  StatusPages handlers are the exception — they are the only place to log
  unhandled exceptions and framework-level errors (IllegalArgumentException,
  MissingRequestParameterException) that never reach a service.
- **Centralize error logging in MviViewModel base class**: Adding
  `logger.error(e) { "..." }` in the base `onError` prevents silent failures
  without requiring every subclass to remember to log. Subclasses still override
  `onError` for UI-specific behavior (resetting flags, sending effects).
- **Use kotlin-logging on client**: Already in the version catalog, supports
  all KMP targets (Android via SLF4J/Logcat, WasmJS via console). No new
  dependency — just needs adding to `commonMain.dependencies`.
- **Debug-level for ownership check failures**: These are expected control-flow
  outcomes (not errors), but useful for debugging. Info would be too noisy.

---

## Open Questions

### Resolved During Planning

- **Where to put the logger in each service?**: Inside the class body as a
  private val — per CLAUDE.md convention for proper SLF4J logger names.
- **Should routes also log?**: No — services already have the context (userId,
  entityId). Route logging would duplicate.

### Deferred to Implementation

- **Exact log message wording**: Will be determined per-service based on the
  operation semantics. Follow the UserService pattern for consistency.

---

## Implementation Units

### U1. Server: Add logging to StatusPages catch-all

**Goal:** Log unhandled exceptions at error level in the StatusPages catch-all
handler so 500s are visible in production logs.

**Requirements:** R3

**Dependencies:** None

**Files:**
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/Application.kt`
- Test: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/StatusPagesTest.kt`

**Approach:**
- Add a logger to `Application.kt` (top-level or companion)
- In the `exception<Throwable>` handler, rename `_` to `cause` and add
  `logger.error(cause) { "Unhandled exception" }`
- Also add debug-level logging for the `IllegalArgumentException` and
  `MissingRequestParameterException` handlers (R2)

**Patterns to follow:**
- `UserService.kt:19` — logger declaration
- `UserService.kt:59` — log message format

**Test scenarios:**
- Happy path: An unhandled exception in a route results in 500 response
  (existing behavior preserved)
- Integration: Verify the catch-all still returns 500 with empty body after
  adding logging (no response body leak)

**Verification:**
- StatusPages catch-all logs the exception at error level
- 400-level handlers log at debug level
- Response codes and bodies unchanged

---

### U2. Server: Add logging to RecipeService

**Goal:** Add info-level logging for create, update, delete operations and
debug-level logging for ownership check failures.

**Requirements:** R1, R2

**Dependencies:** None

**Files:**
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeService.kt`

**Approach:**
- Add `private val logger = KotlinLogging.logger {}` inside the class
- Info log after successful create/update/delete with userId and recipeId
- Debug log when `isOwner` returns false (ownership check failure in
  `findById`, `update`, `delete`)

**Patterns to follow:**
- `UserService.kt:19` — logger inside class
- `UserService.kt:59` — info log with entity identifiers

**Test scenarios:**
- Test expectation: none — logging is a side-effect observable only via log
  output; the service's business logic and return values are already tested
  by existing tests.

**Verification:**
- Create, update, delete operations produce info-level logs with userId and
  recipeId
- Ownership failures produce debug-level logs

---

### U3. Server: Add logging to ShoppingListService

**Goal:** Add info-level logging for list and item mutations, debug-level
logging for ownership check failures.

**Requirements:** R1, R2

**Dependencies:** None

**Files:**
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingListService.kt`

**Approach:**
- Same pattern as U2
- Info log for: `createList`, `updateList`, `deleteList`, `addItem`, `addItems`,
  `updateItem`, `deleteItem`, `deleteCheckedItems`
- Debug log for ownership check failures and `CannotDeleteDefault` outcome
- `createDefaultList` is internal provisioning — info log is fine here too,
  consistent with `UserService.getOrCreate` logging the user creation

**Patterns to follow:**
- `UserService.kt:19,59` — logger and log pattern
- `RecipeService` (U2) — ownership failure debug logging

**Test scenarios:**
- Test expectation: none — same rationale as U2.

**Verification:**
- All mutation operations produce info-level logs
- Ownership failures and CannotDeleteDefault produce debug-level logs

---

### U4. Server: Add logging to MealPlanService

**Goal:** Add info-level logging for create, update, delete operations and
debug-level logging for ownership/validation failures.

**Requirements:** R1, R2

**Dependencies:** None

**Files:**
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/mealplan/MealPlanService.kt`

**Approach:**
- Same pattern as U2/U3
- Info log for: `create`, `update`, `delete`
- Debug log for: ownership check failure, recipeId ownership check failure
  in `create`

**Patterns to follow:**
- `UserService.kt:19,59`
- `RecipeService` (U2)

**Test scenarios:**
- Test expectation: none — same rationale as U2.

**Verification:**
- All mutation operations produce info-level logs
- Ownership and foreign-key validation failures produce debug-level logs

---

### U5. Client: Add kotlin-logging dependency and centralize error logging in MviViewModel

**Goal:** Add the `kotlin-logging` dependency to the client and log all
caught exceptions at error level in the MviViewModel base class.

**Requirements:** R4, R7

**Dependencies:** None

**Files:**
- Modify: `composeApp/build.gradle.kts`
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/common/MviViewModel.kt`

**Approach:**
- Add `implementation(libs.kotlin.logging)` to `commonMain.dependencies`
- Add `private val logger = KotlinLogging.logger {}` in `MviViewModel`
- Add `logger.error(e) { "Unhandled error in ${this::class.simpleName}" }` in
  the catch blocks of `launch` and `launchOptimistic` (before the `onError(e)`
  call), not in `onError` itself — subclasses override `onError` without calling
  `super`, so logging in the base `onError` would be bypassed

**Patterns to follow:**
- `UserService.kt:19` — logger pattern
- `MviViewModel.kt:59-71` — existing catch structure

**Test scenarios:**
- Happy path: Exception thrown in `launch {}` block is caught, logged, and
  routed to `onError`
- Happy path: Exception thrown in `launchOptimistic {}` block is caught,
  logged, state rolled back, and routed to `onError`
- Edge case: `CancellationException` is still rethrown (not logged)

**Verification:**
- All ViewModel exceptions are logged at error level with the exception and
  ViewModel class name
- CancellationException still propagates correctly
- Existing onError behavior in subclasses unchanged

---

### U6. Client: Fix AuthViewModel error handling

**Goal:** Fix CancellationException swallowing in `initialize()` and replace
`e.printStackTrace()` with logger in `logout()`.

**Requirements:** R5, R6, R7

**Dependencies:** U5

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/AuthViewModel.kt`

**Approach:**
- `initialize()`: The bare `catch (_: Exception)` at line 30 swallows
  `CancellationException`. Add `catch (e: CancellationException) { throw e }`
  before the generic catch, following the pattern in `MviViewModel.launch`
  and `AuthViewModel.logout()`
- `login()`: Same bug — inner `catch (@Suppress("TooGenericExceptionCaught") e: Exception)`
  also swallows `CancellationException`. Add `catch (e: CancellationException) { throw e }`
  before the generic catch
- `logout()`: Replace `e.printStackTrace()` with
  `logger.error(e) { "Logout cleanup failed" }` to satisfy detekt
  `PrintStackTrace` rule
- Add `private val logger = KotlinLogging.logger {}` in the class
- Add debug-level logs for auth state transitions (login success, logout,
  auto-login success/failure)

**Patterns to follow:**
- `AuthViewModel.kt:77-78` — CancellationException rethrow pattern already
  used in `logout()`
- `MviViewModel.kt:63-64` — CancellationException rethrow in `launch`

**Test scenarios:**
- Error path: CancellationException in `initialize()` is rethrown, not
  swallowed
- Error path: CancellationException in `login()` is rethrown, not swallowed
- Error path: Logout cleanup failure is logged at error level (not
  printStackTrace)
- Happy path: Successful login produces debug-level log
- Happy path: Successful auto-login produces debug-level log
- Edge case: Failed auto-login (expected path) produces debug-level log

**Verification:**
- `e.printStackTrace()` removed — detekt `PrintStackTrace` satisfied
- CancellationException no longer swallowed in `initialize()`
- Auth state transitions logged at debug level

---

## System-Wide Impact

- **Error propagation:** Server error propagation unchanged — services still
  return `T?`/`Boolean`, routes still map to HTTP status codes. Logging is
  additive side-effect only.
- **State lifecycle risks:** None — logging does not alter state.
- **API surface parity:** No API changes.
- **Unchanged invariants:** HTTP response codes and bodies remain identical.
  The `onError` override contract in MviViewModel subclasses is preserved —
  base class now logs before the subclass override runs.

---

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| kotlin-logging WasmJS target compatibility | Already in version catalog and used on server; library docs confirm wasmJs target support |
| Log noise from ownership debug logs | Using debug level — only visible when app-package log level is DEBUG (already configured in logback.xml) |
| Base class logger in MviViewModel changes test behavior | onError contract preserved; logging is additive |

---

## Sources & References

- Related issue: #15
- `kotlin-logging` multiplatform: `gradle/libs.versions.toml:122`
- Existing logging pattern: `server/.../user/UserService.kt:19,59`
- Detekt rules: `config/detekt/detekt.yml`
