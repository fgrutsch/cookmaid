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

# Run server (Ktor on port 8080)
./gradlew :server:run

# Run web app (Wasm)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

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
  Entry point: `Application.kt` (`embeddedServer` with Netty on port 8080).

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
  no Gradle or webpack config needed. `index.html` supports Gradle
  `expand()` for variable substitution.

## Version Catalog (`libs.versions.toml`)

- Grouped by domain: Android, Compose, Kotlin, Ktor, DI, Database,
  Auth, Testing, Tooling, Logging, Misc. Alphabetical within groups.
  Same grouping across `[versions]`, `[libraries]`, `[bundles]`, `[plugins]`.
- Only pin direct dependencies. Don't catalog transitive-only libraries.
- When removing a dep from `build.gradle.kts`, also remove the matching
  library entry (and version key if unused) from the catalog.

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
