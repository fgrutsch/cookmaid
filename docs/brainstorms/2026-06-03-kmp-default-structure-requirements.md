---
type: requirements
issue: "#138"
date: 2026-06-03
scope: standard
status: approved
---

# Switch to New KMP Default Structure

## Context

AGP 9.0+ mandates separating application entry points from multiplatform
library modules. The current `composeApp` module mixes both: it's a KMP
library (shared Compose UI for Android + WasmJS) and the WasmJS application
entry point. The new KMP default structure (announced May 2026) splits these
responsibilities into distinct modules.

Reference: https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/

## Current Structure

```
cookmaid/
├── shared/          # KMP library: DTOs, models (Android, JVM, WasmJS)
├── composeApp/      # KMP library + WasmJS entry point (mixed)
│   ├── src/commonMain/    # Shared Compose UI
│   ├── src/androidMain/   # Android-specific UI code
│   ├── src/wasmJsMain/    # WasmJS entry point + resources
│   └── src/commonTest/    # Tests
├── androidApp/      # Android application entry point
└── server/          # Ktor backend (JVM)
```

## Target Structure

Following the "with server component" convention:

```
cookmaid/
├── core/            # KMP library: DTOs, models (renamed from shared/)
├── composeApp/      # KMP library: shared Compose UI (Android, WasmJS)
│   ├── src/commonMain/    # Shared Compose UI
│   ├── src/androidMain/   # Android-specific UI code
│   └── src/commonTest/    # Tests
├── webApp/          # WasmJS application entry point (new)
│   ├── src/wasmJsMain/    # main.kt, index.html, service-worker.js, manifest
│   └── build.gradle.kts
├── androidApp/      # Android application entry point (unchanged)
└── server/          # Ktor backend, depends on core/ (unchanged)
```

## Requirements

### R1: Rename `shared` to `core`

Rename the module directory and update all references:
- `settings.gradle.kts`: `:shared` → `:core`
- `composeApp/build.gradle.kts`: dependency on `projects.shared` → `projects.core`
- `server/build.gradle.kts`: dependency on `projects.shared` → `projects.core`
- CLAUDE.md documentation

### R2: Extract `webApp` module from `composeApp`

Move WasmJS entry-point code from `composeApp` to a new `webApp` module:
- `composeApp/src/wasmJsMain/kotlin/.../main.kt` → `webApp/src/wasmJsMain/`
- `composeApp/src/wasmJsMain/resources/` (index.html, service-worker.js,
  manifest.json, icons) → `webApp/src/wasmJsMain/resources/`
- `composeApp/webpack.config.d/` → `webApp/webpack.config.d/`
- WasmJS-specific Gradle tasks (`wasmJsProcessResources`,
  `wasmJsBrowserDistribution`) → `webApp/build.gradle.kts`
- BuildKonfig config (if WasmJS-specific) → `webApp/build.gradle.kts`
- `webApp` depends on `:composeApp` and `:core`

### R3: Clean up `composeApp` as a pure library

After extracting `webApp`:
- Remove `wasmJsMain` entry point code (keep `wasmJsMain` source set
  only if platform-specific Compose code exists beyond the entry point)
- Remove WasmJS packaging tasks (service worker, distribution rewrite)
- Remove webpack config
- Keep Android + WasmJS targets (for shared Compose UI compilation)

### R4: Update `androidApp` dependencies

- `androidApp` depends on `:composeApp` (unchanged module name)
- Verify Android build still works after `shared` → `core` rename

### R5: Update build infrastructure

- `settings.gradle.kts`: add `:webApp`, rename `:shared` → `:core`
- `docker/Dockerfile`: update paths if distribution output directory changes
- `docker/docker-entrypoint.sh`: verify `envsubst` paths
- CI workflows: update any module-specific Gradle tasks
- `dev/local.properties` references in `webApp/build.gradle.kts`

### R6: Update documentation

- CLAUDE.md: update module descriptions, build commands, architecture section
- README.md: update project structure section

## Non-Goals

- No `sharedLogic`/`sharedUI` split (Compose used everywhere)
- No `app/` nesting folder (only 2 client modules)
- No iOS target
- No package name changes (base package stays `io.github.fgrutsch.cookmaid`)

## Assumptions

- `composeApp` keeps its name (rather than renaming to `sharedUI`) since it's
  already well-established in the codebase and the convention is flexible
- The `core` name follows the JetBrains "with server" convention for
  client+server shared code
