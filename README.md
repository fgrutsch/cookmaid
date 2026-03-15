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

## Build & Run

```shell
# Run web app (Wasm)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Build Android app
./gradlew :composeApp:assembleDebug

# Run server (port 8080)
./gradlew :server:run

# Run all tests
./gradlew test
```
