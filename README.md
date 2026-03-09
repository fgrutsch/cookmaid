# Cookmaid

An app for managing shopping lists, recipes and meal planning.

## Tech Stack

- Kotlin 2.3.0
- Compose Multiplatform 1.10.0 (Android, JS, WasmJS)
- Ktor 3.3.3 (Server)
- Koin (Dependency Injection)

## Project Structure

- **`shared/`** — Multiplatform library with business logic (Android, JVM, JS, WasmJS)
- **`composeApp/`** — Compose Multiplatform UI (Android, JS, WasmJS)
- **`server/`** — Ktor backend (JVM)

## Build & Run

```shell
# Run web app (Wasm)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Run web app (JS)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Build Android app
./gradlew :composeApp:assembleDebug

# Run server (port 8080)
./gradlew :server:run

# Run all tests
./gradlew test
```
