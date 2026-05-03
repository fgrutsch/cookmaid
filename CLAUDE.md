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
- BuildKonfig (compile-time build config for KMP)
- Detekt (linting), Kover (coverage), Testcontainers (integration tests)
- Gradle with version catalog (`gradle/libs.versions.toml`)
- Android: minSdk 24, targetSdk 36. `versionCode` derived from semver
  (`major*10000 + minor*100 + patch`); `versionName` is `project.version`.
- Android backup: `android:allowBackup="false"` in `AndroidManifest.xml` — the
  DataStore-backed OIDC token store must never land in `adb backup` or
  Google Drive restores. If backup is ever re-enabled, pair it with
  `android:dataExtractionRules` (API 31+) AND `android:fullBackupContent`
  (API <31) that explicitly exclude the credential store.

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
  When a service accepts a foreign resource ID (e.g., `recipeId` on a
  meal plan item), inject the foreign repository and call its `isOwner()`
  before persisting. Service returns `T?` (null = failure) or `Boolean`;
  route maps null/false to 404.
- **Route ordering**: Register literal routes (e.g., `get("/random")`)
  **before** parameterized routes (`route("/{id}")`) — Ktor matches the
  first route that fits, and a path parameter will swallow literal segments.
- **JWT authentication**: `AuthModule.kt` validates issuer, JWKS signature,
  and audience. Config keys: `oidc.issuer`, `oidc.jwks-url`, `oidc.client-id`
  (all from env vars). `oidc.client-id` has no default — server fails fast
  if absent. Use `property()` (not `propertyOrNull()`) for security-critical
  config to prevent silent misconfiguration. For local runs, `:server:run`
  is hooked in `server/build.gradle.kts` to read `oidc.clientId` from
  `local.properties` and inject it as `OIDC_CLIENT_ID`, so devs don't need
  to export env vars. Same early-exit guard as `wasmJsProcessResources`:
  `takeIf { it.exists() } ?: return@named` — CI/production has no
  `local.properties` and reads the env var directly.
- **Flyway migrations**: `server/src/main/resources/db/migration/V*__*.sql`
- **Timestamps**: All tables (except catalog) have `created_at`/`updated_at`
  with a shared `set_updated_at()` trigger.
- **List query parameters**: Encode lists as comma-separated values in a
  single query parameter (e.g., `?excludeIds=a,b,c`). Parse with
  `split(",").filter { it.isNotBlank() }.map { ... }.orEmpty()`.
- **Batch loading**: When loading child entities for a page of parents
  (e.g., ingredients per recipe), batch with Exposed's `inList` in a
  single query instead of per-entity calls. Use `notInList` for
  multi-value exclusions. Guard with early return if the ID list is
  empty. Single-entity lookups (`findById`) can keep the per-row variant.
- **Ktor testing**: Integration tests use `testApplication { }`;
  unit tests use Koin + Testcontainers + `runTest`. Multi-user tests:
  generate tokens with `TestJwt.generateToken(subject)` using distinct
  subjects, and register each via `POST /api/users/me` before testing.
- **StatusPages error handling**: `Application.configureStatusPages()` maps
  exceptions to HTTP responses. Do **not** throw raw exceptions with
  user/internal data in the message — the catch-all responds 500 with empty
  body, but a typed handler higher up the chain will leak the message if you
  define one. Use a dedicated exception class (see
  `common/ktor/Errors.kt` for `UserNotRegisteredException`). Path-param
  parsing in `RouteExtensions` (`uuid()`, `localDate()`) throws
  `IllegalArgumentException` — mapped to 400. `Parameters.getOrFail` throws
  `MissingRequestParameterException` — also 400.
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
- **Structured concurrency in suspend catches**: Any new
  `catch (e: Exception)` in suspend code must rethrow `CancellationException`
  first. In Kotlin, `CancellationException` extends `Exception`, so a bare
  catch silently swallows cancellation and breaks scope teardown. Pattern
  already modeled in `MviViewModel.launch` / `launchOptimistic` /
  `AuthViewModel.logout()`:

  ```kotlin
  try { block() }
  catch (e: CancellationException) { throw e }
  catch (@Suppress("TooGenericExceptionCaught") e: Exception) { ... }
  ```
- **Debounced MutableStateFlow inputs**: When a `MutableStateFlow` drives a
  debounced side-effect (e.g. `searchQueryFlow` in `RecipeListViewModel`),
  do **not** also mutate the flow from a synchronous handler that calls the
  same side-effect directly — it double-fires (one sync, one debounced).
  Let the flow serve only the typing/input path; synchronous actions (close
  search, clear filter, select tag) call the side-effect directly and read
  from `state.value`. `selectTag()` is the canonical shape; `setSearchActive()`
  was the pitfall. Regression-test with a call-count counter on the repository
  fake, not state-only assertions.
- **Koin DI**: ViewModels injected via `koinInject<T>()` in composables
  or `getKoin()` in navigation entries.
- **Session vs. app scope**: `KoinModules.kt` splits `appModules` (auth,
  settings, user — survives logout) from `sessionModules` (repositories +
  singleton ViewModels holding user data). `App.kt` uses
  `remember(authState.user?.id)` to call `koin.unloadModules(sessionModules)` +
  `koin.loadModules(sessionModules)` synchronously before children compose
  on every identity change, so session-scoped singletons are GC'd on logout
  and rebuilt on login. **Any new user-scoped repository or singleton VM must
  go in a module registered under `sessionModules`** — otherwise its state
  leaks across users. `KoinSessionScopeTest` asserts exact module membership
  at CI; update it when adding modules. Auth infra (`ApiClient`, `HttpClient`,
  `OidcAuthHandler`) stays in `appModules` so logout itself can run;
  `OidcAuthHandler.logout()` still calls `tokenStore.removeTokens()` +
  `httpClient.clearTokens()` to invalidate the bearer on the surviving
  transport. `AuthViewModel.logout()` flips `status = Unauthenticated` +
  `user = null` **synchronously** before `authHandler.logout()` so the
  session rebuild triggers immediately — no observer sees
  `Authenticated && user == null`.
- **Repository pattern**: `ApiXxxRepository` wraps `XxxClient` (Ktor HTTP).
- **Compose Material3**: UI uses `MaterialTheme` from Material3.
- **Reusable components**: `SwipeItem` (optional `onEdit`),
  `DayPickerDialog` (has its own `DayPickerViewModel`).
- **URL handling**: Use `LinkAnnotation.Clickable` (not deprecated
  `ClickableText`) for clickable links. Open via `LocalUriHandler`.
- **BuildKonfig**: Exposes compile-time constants to common code via
  `BuildKonfig` object in package `io.github.fgrutsch.cookmaid`. Configured
  in `composeApp/build.gradle.kts` under `buildkonfig { }`. Use `const = true`
  on `buildConfigField` to generate `const val` (avoids detekt `MayBeConstant`).
- **WasmJS static assets**: Place in `composeApp/src/wasmJsMain/resources/`.
  `wasmJsProcessResources` copies them to build output and performs build-time
  substitutions: `expand()` for OIDC config in `index.html`, `filter {}` for
  `__APP_VERSION__` in `service-worker.js`. Two-environment contract:
  local dev (`local.properties` present) → `expand()` runs, substituting OIDC values
  at build time; CI/production (`local.properties` absent) → task exits early via
  `return@named`, leaving `${VAR}` placeholders intact for `envsubst` at container
  startup. Guard pattern: `val f = rootProject.file("local.properties").takeIf { it.exists() } ?: return@named`.
  Never pass `Properties.getProperty()` results to `expand()` without guaranteeing
  non-null — Gradle silently writes the literal string `"null"` for null values,
  which breaks `envsubst` silently. Use `__PLACEHOLDER__` convention with `filter {}`
  for non-OIDC substitutions to avoid conflicts with `${}` (Groovy templates / envsubst).
- **Content-hashed JS bundles**: Production WasmJS builds produce
  `app.[contenthash].js` via `composeApp/webpack.config.d/output.js` (production
  mode only — dev keeps `composeApp.js`). The `wasmJsBrowserDistribution` task
  has a `doLast` that rewrites the `<script>` src in `index.html` to match the
  hashed filename. Webpack config snippets in `webpack.config.d/` are auto-merged
  by the Kotlin Gradle plugin; the `config` object is pre-defined.
- **Service worker versioning**: `service-worker.js` cache name uses
  `__APP_VERSION__` placeholder, replaced at build time. Each release
  invalidates stale caches via the `activate` handler.
- **PWA icons** (`composeApp/src/wasmJsMain/resources/icon-{192,512,1024}.png`):
  solid white background, no transparency, content within the inner 80%
  (maskable safe zone — Android adaptive masks crop the outer 20%).
  `manifest.json` uses `"purpose": "any maskable"` so one asset serves both
  non-masked and adaptive-icon contexts. Regenerate from
  `docs/images/cookmaid_icon.png` (keep this source as PNG):
  `magick docs/images/cookmaid_icon.png -resize 410x410 -background white -gravity center -extent 512x512 icon-512.png`
  (inner resize = `0.8 * canvas`). `favicon.svg` is an inline `<text>` monogram
  on `#2D3E50`. `apple-touch-icon.png` is 180×180, edge-to-edge, no alpha —
  iOS does not adaptive-mask, and transparent apple-touch icons get filled
  black. iOS reads the manifest (15.4+) for standalone mode, name, and
  `display` — no `apple-mobile-web-app-*` meta tags needed.
- **WasmJS external declarations**: JS interop `external object` / `external class`
  names must match the JS runtime name and cannot follow Kotlin conventions.
  Suppress detekt at the declaration site:
  `@Suppress("ClassNaming")` on the object, `@Suppress("ObjectPropertyNaming")`
  on properties with non-standard names (e.g. `__customLocale`).
- **Full-dataset operations are server-side**: Features requiring the
  complete dataset (random selection, aggregation, global search) must be
  server endpoints. The client's paginated in-memory list is a partial view.
  Code smell: `.random()`, `.filter()`, `.count()` on a list populated by
  a paginated API call.
- **Test fakes — exception type**: Fakes that simulate failure throw
  `IllegalStateException`, not `RuntimeException` (detekt `TooGenericExceptionThrown`).
- **Test fakes — no-op overrides**: Express empty overrides as `= Unit`, not `{}`.
- **Multi-line `OutlinedTextField`**: Use `singleLine = false, maxLines = N`
  (add `minLines` for a minimum height). Remove `keyboardOptions`/`keyboardActions`
  with `ImeAction.Done` — it conflicts with Enter-for-newline. Trim whitespace
  in the ViewModel, not the field. Reference: `AddRecipeComponents.kt` description field.

## Docker

Ktor serves both the API and the WasmJS web app as static files from a single
runtime image. Artifacts are built by Gradle on the host, then COPYed in.

- **Runtime image**: `eclipse-temurin:21-jre-alpine` — non-root user `cookmaid`
- **Entrypoint**: `docker/docker-entrypoint.sh` — runs `envsubst` on
  `index.html` to inject `OIDC_DISCOVERY_URI`, `OIDC_CLIENT_ID`, `OIDC_SCOPE`,
  `OIDC_ACCOUNT_URI`
  at container startup. CI builds leave `${VAR}` placeholders intact (no
  `local.properties`); `envsubst` and `expand()` are mutually exclusive per
  variable — never pass a runtime-injected placeholder to Gradle `expand()`.
- **Port**: 8081

Build locally:
```shell
./gradlew buildDockerImage   # builds artifacts + docker buildx build --load
```

Required env vars at runtime: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`,
`OIDC_ISSUER`, `OIDC_JWKS_URL`, `OIDC_DISCOVERY_URI`, `OIDC_CLIENT_ID`, `OIDC_SCOPE`,
`OIDC_ACCOUNT_URI`.

CI: `ci.yml` builds the Docker image on every push/PR (no `needs:` gate —
Gradle `buildDockerImage` task's own `dependsOn` handles prerequisites).
`release.yml` runs on `v*` tags with two sequential jobs:
1. `docker` — pushes the multi-platform image to GHCR. Requires
   `driver: docker-container` on `setup-buildx-action` — the default
   `docker` driver does not support `--push` with multi-platform manifests.
2. `publish-release` (`needs: docker`) — promotes the draft GitHub release
   to published via `gh release edit --draft=false --latest`. Prerequisite:
   a draft release for the tag must exist before the tag is pushed.
   Requires `permissions: contents: write`; no checkout needed.

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
