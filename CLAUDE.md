# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with
code in this repository.

## Project Overview

Cookmaid is a Kotlin Multiplatform (KMP) project targeting Android,
Web (WasmJS), and a Ktor backend server. All modules share a base
package of `io.github.fgrutsch.cookmaid`.

## Build Commands

```shell
# Build Android app
./gradlew :androidApp:assembleDebug

# Run server (Ktor on port 8081)
./gradlew :server:run

# Run web app (Wasm, dev)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Build web app (Wasm, production)
./gradlew :composeApp:wasmJsBrowserDistribution
# Output: composeApp/build/dist/wasmJs/productionExecutable/

# Build Docker image (server + WasmJS bundled)
./gradlew buildDockerImage

# Run all tests
./gradlew test

# Run tests per module
./gradlew :server:test
./gradlew :shared:allTests
./gradlew :composeApp:allTests

# Run a single test class (server example)
./gradlew :server:test --tests "io.github.fgrutsch.cookmaid.ApplicationTest"

# Lint
./gradlew detektAll
```

## Architecture

Four Gradle modules:

- **`shared/`** — Multiplatform library (targets: Android, JVM, WasmJS).
  Data models, DTOs, request/response types shared across all platforms.
- **`composeApp/`** — Compose Multiplatform UI library (targets: Android, WasmJS).
  Depends on `shared`. Web entry point: `main.kt` (via `ComposeViewport`).
- **`androidApp/`** — Android application entry point. Depends on `composeApp`.
  Contains `MainActivity.kt`, manifest, resources, product flavors, buildConfig.
- **`server/`** — Ktor backend (JVM only). Depends on `shared`.
  Entry point: `Application.kt` (`embeddedServer` with Netty on port 8081).

## Tech Stack

- Kotlin, Compose Multiplatform, Ktor
  (see `gradle/libs.versions.toml` for exact versions)
- Exposed (ORM), Flyway (migrations), PostgreSQL
- Koin (DI), kotlinx.serialization, kotlinx.datetime
- Detekt (linting), Kover (coverage), Testcontainers (integration tests)
- Gradle with version catalog (`gradle/libs.versions.toml`)
- Android: minSdk 24, targetSdk 36

## Key Patterns

### Shared module

- Data models (`Recipe`, `MealPlanItem`, `ShoppingItem`, etc.) live here
- Sealed interfaces use `@SerialName` for explicit JSON discriminators
  (e.g., `@SerialName("catalog")` on `Item.Catalog`)
- Request/response DTOs: `CreateRecipeRequest`, `UpdateRecipeRequest`, etc.

### Server module

- **Layered**: Routes (Module) → Service → Repository → Exposed tables
- **`UserId` value class**: All service/repository methods use
  `UserId` (wraps `Uuid`) instead of raw `Uuid` for user identity.
  Extracted from JWT via `call.userId()` extension.
- **Method naming**: With `UserId` as parameter type, prefer short names
  (`find`, `findTags`, `isOwner`) over verbose ones (`findByUserId`).
- **Ownership checks**: Services check `repository.isOwner(userId, id)`
  and return 404 (not 403) to avoid leaking resource existence.
- **Flyway migrations**: `server/src/main/resources/db/migration/V*__*.sql`
- **Timestamps**: All tables (except catalog) have `created_at`/`updated_at`
  with a shared `set_updated_at()` trigger.
- **Ktor testing**: Integration tests use `testApplication { }`;
  unit tests use Koin + Testcontainers + `runTest`.
- **Static file serving**: `staticFiles("/", File(webDir))` serves the
  WasmJS web app. Must be registered **before** the `/api` route.
  Ktor config key is `web.dir` (populated from `WEB_DIR` env var, defaults to
  `"web"`). Missing dir is a no-op — API routes still work, used for tests.
  Integration tests using `MapApplicationConfig` must include `"web.dir" to "web"`
  or `ApplicationConfigurationException` is thrown at startup.
- **Distribution**: `:server:installDist` produces `server/build/install/server/`
  (`bin/` + `lib/`) — no fat-JAR plugin needed, used by the Docker image.

### ComposeApp module

- **MVI pattern**: Each screen has `State`, `Event`, `Effect` types +
  `MviViewModel` base class. ViewModels use `launch {}` for coroutines
  and `updateState {}` for state mutations.
- **Koin DI**: ViewModels injected via `koinInject<T>()` in composables
  or `getKoin()` in navigation entries.
- **Repository pattern**: `ApiXxxRepository` wraps `XxxClient` (Ktor HTTP).
- **Compose Material3**: UI uses `MaterialTheme` from Material3.
- **Reusable components**: `SwipeItem` (optional `onEdit`),
  `DayPickerDialog` (has its own `DayPickerViewModel`).
- **URL handling**: Use `LinkAnnotation.Clickable` (not deprecated
  `ClickableText`) for clickable links. Open via `LocalUriHandler`.
- **WasmJS static assets**: Place in `composeApp/src/wasmJsMain/resources/`.
  `wasmJsProcessResources` copies them to build output automatically —
  no Gradle or webpack config needed. `index.html` uses Gradle `expand()`
  for build-time substitution from `local.properties`. Two-environment contract:
  local dev (`local.properties` present) → `expand()` runs, substituting OIDC values
  at build time; CI/production (`local.properties` absent) → task exits early via
  `return@named`, leaving `${VAR}` placeholders intact for `envsubst` at container
  startup. Guard pattern: `val f = rootProject.file("local.properties").takeIf { it.exists() } ?: return@named`.
  Never pass `Properties.getProperty()` results to `expand()` without guaranteeing
  non-null — Gradle silently writes the literal string `"null"` for null values,
  which breaks `envsubst` silently.
- **WasmJS external declarations**: JS interop `external object` / `external class`
  names must match the JS runtime name and cannot follow Kotlin conventions.
  Suppress detekt at the declaration site:
  `@Suppress("ClassNaming")` on the object, `@Suppress("ObjectPropertyNaming")`
  on properties with non-standard names (e.g. `__customLocale`).
- **Test fakes — exception type**: Fakes that simulate failure throw
  `IllegalStateException`, not `RuntimeException` (detekt `TooGenericExceptionThrown`).
- **Test fakes — no-op overrides**: Express empty overrides as `= Unit`, not `{}`.

## Docker

Ktor serves both the API and the WasmJS web app as static files from a single
runtime image. Artifacts are built by Gradle on the host, then COPYed in.

- **Runtime image**: `eclipse-temurin:21-jre-alpine` — non-root user `cookmaid`
- **Entrypoint**: `docker/docker-entrypoint.sh` — runs `envsubst` on
  `index.html` to inject `OIDC_DISCOVERY_URI`, `OIDC_CLIENT_ID`, `OIDC_SCOPE`
  at container startup. CI builds leave `${VAR}` placeholders intact (no
  `local.properties`); `envsubst` and `expand()` are mutually exclusive per
  variable — never pass a runtime-injected placeholder to Gradle `expand()`.
- **Port**: 8081

Build locally:
```shell
./gradlew buildDockerImage   # builds artifacts + docker buildx build --load
```

Required env vars at runtime: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`,
`OIDC_ISSUER`, `OIDC_JWKS_URL`, `OIDC_DISCOVERY_URI`, `OIDC_CLIENT_ID`, `OIDC_SCOPE`.

CI: `ci.yml` builds the Docker image on every push/PR (no `needs:` gate —
Gradle `buildDockerImage` task's own `dependsOn` handles prerequisites).
`release.yml` runs on `v*` tags and pushes to GHCR. Multi-platform push
requires `driver: docker-container` on `setup-buildx-action` — the default
`docker` driver does not support `--push` with multi-platform manifests.

## Version Catalog (`libs.versions.toml`)

- Grouped by domain: Android, Compose, Kotlin, Ktor, DI, Database,
  Auth, Testing, Tooling, Logging, Misc. Alphabetical within groups.
  Same grouping across `[versions]`, `[libraries]`, `[bundles]`, `[plugins]`.
- Only pin direct dependencies. Don't catalog transitive-only libraries.
- When removing a dep from `build.gradle.kts`, also remove the matching
  library entry (and version key if unused) from the catalog.
- Plugin alias keys must be **camelCase** (e.g., `axionRelease`, not
  `axion-release`) — the Kotlin DSL generates `libs.plugins.axionRelease`
  and hyphenated keys produce no accessor, causing unresolved reference errors.

## KDoc

Add multiline KDoc on public classes, interfaces, methods, and public
`@Composable` functions. Document `@param`, `@property`, `@return`,
`@throws` where relevant. Skip KDoc on obvious things (data classes,
simple getters, test methods, internal/private composables).

```kotlin
/**
 * Resolves the authenticated user's ID from the JWT principal.
 *
 * @return the [UserId] extracted from the cached request attribute
 *   or looked up via [UserService].
 * @throws IllegalStateException if the JWT principal is missing
 *   or the user is not found.
 */
suspend fun ApplicationCall.userId(): UserId { ... }

/**
 * Checks whether [itemId] belongs to [userId].
 *
 * @param userId the owner to check against.
 * @param itemId the item to verify ownership of.
 * @return true if the item is owned by the user.
 */
suspend fun isOwner(userId: UserId, itemId: Uuid): Boolean
```
