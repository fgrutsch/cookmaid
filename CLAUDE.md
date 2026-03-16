# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Cookmaid is a Kotlin Multiplatform (KMP) project targeting Android, Web (JS + WasmJS), and a Ktor backend server. All modules share a base package of `io.github.fgrutsch.cookmaid`.

## Build Commands

```shell
# Build Android app
./gradlew :composeApp:assembleDebug

# Run server (Ktor on port 8080)
./gradlew :server:run

# Run web app (Wasm - faster, modern browsers)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Run web app (JS - broader browser support)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Run all tests
./gradlew test

# Run tests per module
./gradlew :server:test
./gradlew :shared:allTests
./gradlew :composeApp:allTests

# Run a single test class (server example)
./gradlew :server:test --tests "io.github.fgrutsch.cookmaid.ApplicationTest"
```

## Architecture

Three Gradle modules:

- **`shared/`** — Multiplatform library (targets: Android, JVM, JS, WasmJS). Contains business logic shared across all platforms. Uses the `expect`/`actual` pattern for platform-specific implementations (e.g., `Platform.kt`).
- **`composeApp/`** — Compose Multiplatform UI (targets: Android, JS, WasmJS). Depends on `shared`. Platform entry points: `MainActivity.kt` (Android), `main.kt` (Web via `ComposeViewport`).
- **`server/`** — Ktor backend (JVM only). Depends on `shared`. Entry point: `Application.kt` (`embeddedServer` with Netty on port 8080).

## Key Patterns

- **Expect/Actual**: `Platform` interface in `shared/commonMain` with platform-specific implementations in `androidMain`, `jvmMain`, `jsMain`, `wasmJsMain`
- **Shared logic flows up**: Both `composeApp` and `server` depend on `shared` — put cross-platform business logic there
- **Compose Material3**: UI uses `MaterialTheme` from Material3
- **Ktor testing**: Server tests use `testApplication { }` with `client.get()` assertions

## Tech Stack

- Kotlin 2.3.0, Compose Multiplatform 1.10.0, Ktor 3.3.3
- Gradle with version catalog (`gradle/libs.versions.toml`)
- Android: minSdk 24, targetSdk 36, JVM target 11
