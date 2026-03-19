![Cookmaid](docs/images/logo.png)

# Cookmaid

An app for managing shopping lists, recipes and meal planning.

## Tech Stack

- Kotlin 2.3.10
- Compose Multiplatform 1.10.2 (Android, WasmJS)
- Ktor 3.4.1 (Server)
- Exposed 1.1.1 (Database ORM)
- PostgreSQL (Database)
- Flyway 12.1.0 (Migrations)
- Koin 4.1.1 (Dependency Injection)

## Project Structure

- **`shared/`** — Multiplatform library with business logic (Android, JVM, WasmJS)
- **`composeApp/`** — Compose Multiplatform UI (Android, WasmJS)
- **`server/`** — Ktor backend (JVM)
- **`dev/`** — Docker Compose setup for local infrastructure

## Local Development Setup

### Prerequisites

- JDK 11+
- Docker & Docker Compose
- [mkcert](https://github.com/FiloSottile/mkcert) — run `mkcert -install`
  once to trust the local CA (dev TLS certs are committed in `dev/`)

### 1. Start Infrastructure

```shell
cd dev
docker compose up -d
```

This starts:
- **PostgreSQL** on port 5432 (databases: `cookmaid`, `pocketid`)
- **PocketID** (OIDC provider) on port 1411
- **nginx** reverse proxy with HTTPS on port 443

### 2. Configure PocketID

Open https://idp.localhost and create an OIDC client for the app.
Note the client ID for the next step.

### 3. Configure local.properties

Add OIDC settings to `local.properties` (gitignored):

```properties
oidc.discoveryUri=https://idp.localhost/.well-known/openid-configuration
oidc.clientId=<your-client-id>
oidc.scope=openid profile email offline_access
```

These are injected into the WasmJS web app at build time via Gradle's
`expand()` in `wasmJsProcessResources`.

The server reads its OIDC config from `application.yaml` with defaults
pointing to the local PocketID instance.

### 4. Run

```shell
# Run server (port 8081)
./gradlew :server:run

# Run web app (Wasm, port 8080)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Build Android app
./gradlew :composeApp:assembleDebug
```

### 5. Run Tests

```shell
# All tests
./gradlew test

# Per module
./gradlew :server:test
./gradlew :composeApp:allTests
```
