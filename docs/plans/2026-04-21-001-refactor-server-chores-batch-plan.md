---
title: "refactor: Server chores batch — pool, headers, ownership, tags, request types, provisioning"
type: refactor
status: active
date: 2026-04-21
---

# Server Chores Batch

## Overview

Six server-side chores consolidated into a single PR. All are self-contained
improvements to the Ktor backend — connection pooling, security headers,
query idioms, type consolidation, and domain decoupling.

## Problem Frame

Each issue addresses a concrete gap:
- #41 — Single JDBC connection, no pool. Scalability blocker.
- #46 — `UserService` directly depends on `ShoppingListRepository` (cross-domain).
- #50 — `findTags` fetches all columns to collect one field.
- #51 — `CreateRecipeRequest` and `UpdateRecipeRequest` are identical types.
- #55 — No HTTP security headers on responses.
- #59 — `isOwner` uses `COUNT(*)` instead of `EXISTS`.

## Requirements Trace

- R1. Database uses HikariCP connection pool (#41)
- R2. `UserService` has no direct shopping-domain dependency (#46)
- R3. `findTags` selects only the `tags` column (#50)
- R4. Single `RecipeRequest` type replaces create/update pair (#51)
- R5. Security headers present on all responses (#55)
- R6. `isOwner`/`isListOwner`/`isItemOwner` use `!query.empty()` for EXISTS semantics (#59)

## Scope Boundaries

- No new features or API changes (except error body format is not in scope)
- No changes to composeApp beyond adapting to the renamed request type
- No CSP tuning beyond WasmJS compatibility — tighten later based on real-world testing
- HikariCP pool config uses sensible defaults; no env-var-driven sizing yet

## Context & Research

### Relevant Code and Patterns

- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/db/DatabaseModule.kt` — bare `Database.connect()`
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/Application.kt` — `configureHttp()` installs only `ContentNegotiation`
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRepository.kt` — `isOwner` (line 267), `findTags` (line 214)
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingListRepository.kt` — `isListOwner` (line 321), `isItemOwner` (line 327)
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/mealplan/MealPlanRepository.kt` — `isOwner` (line 125)
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/user/UserService.kt` — cross-domain `ShoppingListRepository` dependency
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingListService.kt` — `createList()` always passes `default = false`
- `shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRequests.kt` — identical create/update types
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeData.kt` — bridge DTO with duplicate `toData()` extensions
- Exposed version: 1.2.0. Ktor version from `libs.versions.toml`.

### Institutional Learnings

No `docs/solutions/` directory exists. CLAUDE.md covers relevant patterns.

## Key Technical Decisions

- **HikariCP shared DataSource for Flyway and Exposed**: Single pool serves both migration
  and runtime. Flyway runs at startup before the pool serves requests, so no contention.
- **`DefaultHeaders` plugin over manual interceptor**: Ktor's built-in plugin is the idiomatic
  approach. Requires adding `ktor-server-default-headers` artifact.
- **`RecipeRequest` replaces both types + `RecipeData`**: The route's HTTP method already
  distinguishes create from update. `RecipeData` exists only as a bridge and can be deleted.
  The service/repository accept `RecipeRequest` directly.
- **`ShoppingListService.createDefaultList()` over event/callback**: Simplest fix — add a
  method to the service, have `UserService` depend on `ShoppingListService` instead of the
  repository. Avoids over-engineering with events for a single call site.
- **`.select(RecipesTable.tags)` over raw SQL `unnest()`**: Column-selective query is a
  smaller change and sufficient optimization. Deduplication stays in Kotlin.
- **Note on `MealPlanService` cross-domain coupling**: `MealPlanService` also injects
  `RecipeRepository` for ownership checks. This is intentional per CLAUDE.md ("inject the
  foreign repository and call its `isOwner()`"). Not in scope to change.

## Open Questions

### Resolved During Planning

- **Should Flyway and Exposed share the same DataSource?** Yes — avoids two pools. Flyway
  runs synchronously at startup before any request handling.
- **Should `UserService` depend on `ShoppingListService` or use events?** Service dependency.
  Events are over-engineering for one call site.
- **What CSP directives are safe for WasmJS?** `script-src 'self' 'wasm-unsafe-eval'` is
  required for WebAssembly. `style-src 'self' 'unsafe-inline'` needed for Compose's
  inline styles.

### Deferred to Implementation

- **Exact HikariCP version**: Check latest stable at implementation time.
- **CSP fine-tuning**: May need `img-src`, `connect-src` adjustments based on actual
  app behavior (API calls, image loading via coil).

## Implementation Units

- [ ] **Unit 1: Add HikariCP connection pool (#41)**

  **Goal:** Replace bare `Database.connect()` with pooled `HikariDataSource`.

  **Requirements:** R1

  **Dependencies:** None

  **Files:**
  - Modify: `gradle/libs.versions.toml`
  - Modify: `server/build.gradle.kts`
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/db/DatabaseModule.kt`

  **Approach:**
  - Add HikariCP to version catalog and server dependencies
  - Create `HikariDataSource` in `createDatabase()`, pass to both `Flyway.configure().dataSource(ds)` and `Database.connect(ds)`
  - Use sensible defaults: `maximumPoolSize = 10`, `minimumIdle = 2`

  **Patterns to follow:**
  - Existing `DatabaseModule.kt` config property reads

  **Test scenarios:**
  - Happy path: Server starts, runs migrations, handles concurrent requests (covered by existing integration tests — verify they still pass)
  - Integration: Flyway migrations complete before Exposed queries run (startup ordering)

  **Verification:**
  - All existing server tests pass
  - `Database.connect` receives a `DataSource`, not raw URL

- [ ] **Unit 2: Add HTTP security headers (#55)**

  **Goal:** Install `DefaultHeaders` plugin with security headers.

  **Requirements:** R5

  **Dependencies:** None

  **Files:**
  - Modify: `gradle/libs.versions.toml`
  - Modify: `server/build.gradle.kts`
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/Application.kt`
  - Modify: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/ApplicationTest.kt`

  **Approach:**
  - Add `ktor-server-default-headers` to version catalog and ktor server bundle
  - Install `DefaultHeaders` in `configureHttp()` alongside `ContentNegotiation`
  - Headers: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
    `Referrer-Policy: no-referrer`, `Strict-Transport-Security: max-age=31536000; includeSubDomains`,
    `Content-Security-Policy` with `'wasm-unsafe-eval'` for WasmJS compatibility

  **Patterns to follow:**
  - Existing `configureHttp()` function structure in `Application.kt`

  **Test scenarios:**
  - Happy path: Response to any endpoint includes all 5 security headers
  - Happy path: `Content-Security-Policy` includes `wasm-unsafe-eval` in `script-src`
  - Edge case: Static file responses also include headers (DefaultHeaders applies globally)

  **Verification:**
  - Test asserts each header is present on a sample response
  - Web app still loads and functions with CSP enabled (manual verification)

- [ ] **Unit 3: Replace `.count() > 0` with `!query.empty()` in ownership checks (#59)**

  **Goal:** Use Exposed's `!query.empty()` (EXISTS semantics) instead of `.count() > 0`.
  Note: Exposed 1.2.0's `Query` has no `.any()` method. `SizedIterable.empty()` is the
  idiomatic existence check.

  **Requirements:** R6

  **Dependencies:** None

  **Files:**
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRepository.kt`
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingListRepository.kt`
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/mealplan/MealPlanRepository.kt`

  **Approach:**
  - Replace `.count() > 0` with `!...empty()` in all 4 methods (1 in Recipe, 2 in Shopping, 1 in MealPlan)

  **Patterns to follow:**
  - Existing `isOwner` method structure — minimal change, same `where` clause

  **Test scenarios:**
  - Test expectation: none — pure refactor, existing ownership tests cover true/false cases

  **Verification:**
  - All existing repository tests pass unchanged

- [ ] **Unit 4: Optimize `findTags` to select only tags column (#50)**

  **Goal:** Reduce I/O by selecting only the `tags` column instead of all columns.

  **Requirements:** R3

  **Dependencies:** None

  **Files:**
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRepository.kt`

  **Approach:**
  - Replace `RecipesTable.selectAll()` with `RecipesTable.select(RecipesTable.tags)` in `findTags()`
  - Keep `.flatMap`, `.distinct()`, `.sorted()` in Kotlin — sufficient optimization

  **Patterns to follow:**
  - New pattern for this codebase (no existing `.select(columns)` usage)

  **Test scenarios:**
  - Test expectation: none — pure optimization, existing `findTags` tests cover behavior

  **Verification:**
  - `GET /api/recipes/tags` returns same results as before
  - Existing recipe tag tests pass

- [ ] **Unit 5: Consolidate recipe request types (#51)**

  **Goal:** Replace `CreateRecipeRequest` + `UpdateRecipeRequest` + `RecipeData` with single `RecipeRequest`.

  **Requirements:** R4

  **Dependencies:** None

  **Files:**
  - Modify: `shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRequests.kt`
  - Delete: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeData.kt`
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeModule.kt`
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeService.kt`
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRepository.kt`
  - Modify: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRoutesTest.kt`
  - Modify: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeServiceTest.kt`
  - Modify: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/recipe/PostgresRecipeRepositoryTest.kt`
  - Modify: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/mealplan/MealPlanRoutesTest.kt`
  - Modify: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/mealplan/MealPlanServiceTest.kt`
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/RecipeClient.kt`
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/edit/AddRecipeViewModel.kt`

  **Approach:**
  - Rename `CreateRecipeRequest` to `RecipeRequest`, delete `UpdateRecipeRequest`
  - Delete `RecipeData.kt` and both `toData()` extensions
  - Service and repository accept `RecipeRequest` directly where they accepted `RecipeData`
  - Update composeApp client to use `RecipeRequest` for both create and update calls

  **Patterns to follow:**
  - Other request types in `shared/` (e.g., shopping list requests) — single type per entity

  **Test scenarios:**
  - Happy path: Create recipe via POST still works
  - Happy path: Update recipe via PUT still works
  - Integration: Shared module compiles for all targets (Android, JVM, WasmJS)

  **Verification:**
  - `RecipeData.kt` is gone
  - All recipe CRUD tests pass
  - `./gradlew :shared:allTests` and `./gradlew :composeApp:allTests` pass

- [ ] **Unit 6: Extract user provisioning to decouple domains (#46)**

  **Goal:** Remove `ShoppingListRepository` dependency from `UserService`.

  **Requirements:** R2

  **Dependencies:** None

  **Files:**
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/user/UserService.kt`
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/user/UserModule.kt`
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingListService.kt`
  - Modify: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/user/UserServiceTest.kt` (if exists)

  **Approach:**
  - Add `createDefaultList(userId: UserId)` to `ShoppingListService` as a plain `suspend fun`
    that delegates to `repository.createList(userId, "Shopping List", default = true)` — no
    own transaction wrapper; the caller's `suspendTransaction` in `getOrCreate()` handles atomicity
  - Change `UserService` to depend on `ShoppingListService` instead of `ShoppingListRepository`
  - Call `shoppingListService.createDefaultList(userId)` in `getOrCreate()`

  **Patterns to follow:**
  - Existing service-to-service dependency: `MealPlanService` depends on `RecipeRepository`
    (but that's repository-level for ownership checks — this moves to service-level)

  **Test scenarios:**
  - Happy path: New user registration creates user + default shopping list
  - Happy path: Existing user lookup returns user without creating duplicate list
  - Integration: `userModule` resolves correctly with `shoppingModule` providing `ShoppingListService`

  **Verification:**
  - `UserService` constructor has no `ShoppingListRepository` parameter
  - All user registration tests pass
  - Default shopping list still created on first login

## System-Wide Impact

- **Interaction graph:** Unit 6 changes Koin resolution order — `UserService` now depends
  on `ShoppingListService` instead of `ShoppingListRepository`. Both are in separate modules
  but already loaded together.
- **Error propagation:** No change — all error paths remain the same.
- **State lifecycle risks:** HikariCP pool lifecycle is tied to the JVM process. No explicit
  shutdown hook needed for embedded Netty.
- **API surface parity:** Unit 5 changes the shared DTO name. ComposeApp client must update
  references. No external API consumers.
- **Unchanged invariants:** All REST endpoints, response shapes, and HTTP methods stay the same.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| CSP too restrictive for WasmJS | Test web app manually after adding headers. `'wasm-unsafe-eval'` + `'unsafe-inline'` for styles should cover Compose output |
| `RecipeRequest` rename breaks composeApp compilation | Unit 5 updates both shared and composeApp in the same commit |
| HikariCP version incompatibility with Exposed 1.2.0 | HikariCP is a standard JDBC pool — no Exposed-specific coupling |

## Sources & References

- Related issues: #41, #46, #50, #51, #55, #59
- Exposed `SizedIterable.empty()`: Exposed Query API
- Ktor DefaultHeaders plugin: Ktor documentation
