<div align="center">
<img src="docs/images/cookmaid_logo.png" alt="Cookmaid logo" width="300"/>

[![Github Actions CI Workflow](https://img.shields.io/github/v/tag/fgrutsch/cookmaid?logo=Github&style=for-the-badge)](https://github.com/fgrutsch/cookmaid/releases)
[![Github Actions CI Workflow](https://img.shields.io/github/actions/workflow/status/fgrutsch/cookmaid/ci.yml?logo=Github&style=for-the-badge)](https://github.com/fgrutsch/cookmaid/actions/workflows/ci.yml?query=branch%3Amain)
[![Codecov](https://img.shields.io/codecov/c/github/fgrutsch/cookmaid/main?logo=Codecov&style=for-the-badge)](https://codecov.io/gh/fgrutsch/cookmaid)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)
</div>

Cookmaid is a self-hosted meal planning app. Manage your recipes, plan meals for the week,
and generate shopping lists — available on Android and as a Progressive Web App.

## Tech Stack

- Kotlin + Compose Multiplatform (Android, WasmJS)
- Ktor server (JVM)
- Exposed ORM + Flyway migrations on PostgreSQL
- Koin for DI, kotlinx.serialization + kotlinx.datetime
- OIDC auth via PocketID

Pinned versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Project Structure

- **`shared/`** — Multiplatform library (Android, JVM, WasmJS). Data models
  and DTOs shared across client and server.
- **`composeApp/`** — Compose Multiplatform UI library (Android, WasmJS).
  Web build ships as an installable Progressive Web App (manifest, service
  worker, maskable icons for Android adaptive masks, `apple-touch-icon` for iOS).
- **`androidApp/`** — Android application entry point. Depends on `composeApp`.
- **`server/`** — Ktor backend (JVM). Serves API + WasmJS static files.
- **`dev/`** — Docker Compose setup for local infrastructure
- **`docker/`** — Production Dockerfile + entrypoint

## Local Development Setup

### Prerequisites

- JDK 17+ (runtime container uses JDK 21)
- Docker & Docker Compose

### 1. Start Infrastructure

```shell
cd dev
docker compose up -d
```

This starts:
- **PostgreSQL** on port 5432 (databases: `cookmaid`, `pocketid`)
- **PocketID** (OIDC provider) fronted by **nginx** on http://localhost:8082

### 2. Configure PocketID

Open http://localhost:8082 and create an OIDC client for the app.
Note the client ID for the next step.

### 3. Configure local.properties

Add OIDC settings to `local.properties` (gitignored):

```properties
oidc.discoveryUri=http://localhost:8082/.well-known/openid-configuration
oidc.clientId=<your-client-id>
oidc.scope=openid profile email offline_access
```

These are injected into the WasmJS web app at build time via Gradle's
`expand()` in `wasmJsProcessResources`. `:server:run` also picks up
`oidc.clientId` from the same file and sets it as `OIDC_CLIENT_ID`, so
no manual env export is needed for local runs.

The server reads its OIDC config from `application.yaml`. `oidc.issuer`
and `oidc.jwks-url` default to the local PocketID instance;
`oidc.client-id` has no default — `:server:run` reads it from
`local.properties`, production reads it from the `OIDC_CLIENT_ID` env var.

### 4. Run

```shell
# Run server (port 8081)
./gradlew :server:run

# Run web app (Wasm, port 8080)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Build Android app
./gradlew :androidApp:assembleDebug
```

### 5. Run Tests

```shell
# All tests
./gradlew test

# Per module
./gradlew :server:test
./gradlew :shared:allTests
./gradlew :composeApp:allTests
```
