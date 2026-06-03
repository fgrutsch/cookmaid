---
type: refactor
issue: "#138"
date: 2026-06-03
status: active
origin: docs/brainstorms/2026-06-03-kmp-default-structure-requirements.md
depth: standard
---

# Refactor: Switch to New KMP Default Structure

## Problem Frame

AGP 9.0+ mandates separating application entry points from multiplatform
library modules. The current `composeApp` module mixes both: it's a KMP
library (shared Compose UI for Android + WasmJS) and the WasmJS
application entry point (`main.kt`, `index.html`, webpack config,
distribution tasks). This must be split per the new KMP default structure.

(see origin: `docs/brainstorms/2026-06-03-kmp-default-structure-requirements.md`)

---

## Summary

Rename `shared` → `core` (client+server shared DTOs). Extract the WasmJS
entry point from `composeApp` into a new `webApp` module. `composeApp`
becomes a pure Compose Multiplatform library. Module dependency graph
after migration:

```
androidApp → composeApp → core
webApp     → composeApp → core
server                  → core
```

---

## Scope Boundaries

**In scope:**
- Module rename (`shared` → `core`)
- New `webApp` module for WasmJS entry point
- Build config migration (Gradle tasks, webpack, BuildKonfig)
- Docker, CI, and documentation updates

**Out of scope / Non-goals:**
- Package name changes (`io.github.fgrutsch.cookmaid` stays)
- `sharedLogic`/`sharedUI` split (Compose used on all client targets)
- `app/` nesting folder (only 2 client modules)
- iOS target

### Deferred to Follow-Up Work

- None identified

---

## Key Technical Decisions

1. **`shared` → `core` rename** — follows the JetBrains "with server"
   convention where `core` is the module shared between client and server.
   `shared` is ambiguous in a project that also has shared Compose UI.

2. **`composeApp` keeps its name** — it's already the Compose UI library
   module. Renaming to `sharedUI` adds churn without clarity gain since
   the project doesn't have a separate `sharedLogic`.

3. **`webApp` for WasmJS entry point** — follows the JetBrains convention
   (`webApp` for web, `androidApp` for Android). Only the entry point
   (`main.kt`), resources (`index.html`, service worker, icons), and
   WasmJS-specific build config (webpack, distribution tasks) move.
   Platform `actual` implementations in `wasmJsMain` stay in `composeApp`.

4. **BuildKonfig stays in `composeApp`** — it generates `APP_VERSION` used
   across the shared UI. The WasmJS-specific resource processing
   (`wasmJsProcessResources`, `wasmJsBrowserDistribution`) moves to `webApp`.

---

## Requirements Trace

| Req | Unit |
|-----|------|
| R1: Rename shared → core | U1 |
| R2: Extract webApp module | U2, U3 |
| R3: Clean up composeApp | U3 |
| R4: Update androidApp deps | U1 |
| R5: Update build infrastructure | U1, U2, U4 |
| R6: Update documentation | U5 |

---

## Implementation Units

### U1. Rename `shared` to `core`

**Goal:** Rename the module directory and all references.

**Requirements:** R1, R4, R5

**Dependencies:** None — do this first so subsequent units reference
the correct module name.

**Files:**
- `shared/` → `core/` (directory rename)
- `settings.gradle.kts` — `:shared` → `:core`
- `composeApp/build.gradle.kts` — `projects.shared` → `projects.core`
- `server/build.gradle.kts` — `projects.shared` → `projects.core`
- `build.gradle.kts` (root) — detekt task references if any
- `.github/workflows/ci.yml` — `:shared:allTests` → `:core:allTests`

**Approach:** Git `mv shared core`, then find-and-replace `:shared` /
`projects.shared` across Gradle files and CI workflows. The
`androidApp` doesn't reference `shared` directly (only via `composeApp`),
so no change there.

**Patterns to follow:** Existing module reference style using
`projects.xxx` typesafe accessors.

**Test scenarios:**
- `./gradlew :core:allTests` passes
- `./gradlew :server:test` passes (dependency resolves)
- `./gradlew :composeApp:compileKotlinWasmJs` passes

**Verification:** Full `./gradlew test` passes. No unresolved references
to `:shared` or `projects.shared` in any `.kts` or `.yml` file.

---

### U2. Create `webApp` module with WasmJS entry point

**Goal:** Extract the WasmJS application entry point into a new `webApp`
module.

**Requirements:** R2, R5

**Dependencies:** U1

**Files:**
- `webApp/build.gradle.kts` (new)
- `webApp/src/wasmJsMain/kotlin/io/github/fgrutsch/cookmaid/main.kt`
  (moved from `composeApp/src/wasmJsMain/`)
- `webApp/src/wasmJsMain/resources/` (moved: `index.html`,
  `service-worker.js`, `manifest.json`, `favicon.svg`,
  `apple-touch-icon.png`, `icon-{192,512,1024}.png`)
- `webApp/webpack.config.d/` (moved from `composeApp/webpack.config.d/`)
- `settings.gradle.kts` — add `:webApp`

**Approach:**
- Create `webApp/build.gradle.kts` with:
  - Plugins: `kotlinMultiplatform`, `composeMultiplatform`, `composeCompiler`
  - Target: `wasmJs { browser(); binaries.executable() }`
  - Dependencies: `projects.composeApp` (which transitively brings `core`)
  - OIDC dependencies (`bundles.oidc`) for the token store and auth flow
    factory used in `main.kt`
- Move the `wasmJsProcessResources` task (OIDC var expansion,
  `__APP_VERSION__` substitution) from `composeApp` to `webApp`
- Move the `wasmJsBrowserDistribution` `doLast` (content-hash JS rewrite)
  from `composeApp` to `webApp`
- Move webpack config snippets (`output.js`, `devServer.js`)
- `main.kt` and `LocalStorageSettingsStore.kt` move to `webApp`.
  Other `wasmJsMain` files (`LocalAppLocale.wasmJs.kt`,
  `SystemBarAppearance.wasmJs.kt`) are `actual` implementations for
  `expect` declarations and stay in `composeApp`

**Patterns to follow:** `androidApp/build.gradle.kts` as the model for
a platform-specific app module depending on `composeApp`.

**Test scenarios:**
- `./gradlew :webApp:wasmJsBrowserDevelopmentRun` starts the dev server
- `./gradlew :webApp:wasmJsBrowserDistribution` produces output in
  `webApp/build/dist/wasmJs/productionExecutable/`
- `index.html` in distribution has OIDC vars substituted (dev) or
  `${VAR}` placeholders intact (CI)
- Content-hashed JS filename is rewritten in `index.html`

**Verification:** Web app loads in browser, OIDC login flow works,
service worker registers.

---

### U3. Clean up `composeApp` as a pure library

**Goal:** Remove WasmJS entry-point code and packaging tasks from
`composeApp` after extraction to `webApp`.

**Requirements:** R2, R3

**Dependencies:** U2

**Files:**
- `composeApp/build.gradle.kts` — remove `wasmJsProcessResources` task,
  `wasmJsBrowserDistribution` doLast, `binaries.executable()` from
  wasmJs target
- `composeApp/src/wasmJsMain/kotlin/.../main.kt` — deleted (moved to U2)
- `composeApp/src/wasmJsMain/kotlin/.../LocalStorageSettingsStore.kt` —
  deleted (moved to U2)
- `composeApp/src/wasmJsMain/resources/` — deleted (moved to U2)
- `composeApp/webpack.config.d/` — deleted (moved to U2)

**Approach:** After U2 is verified working, remove the source files and
Gradle task configurations. Keep the `wasmJs { browser() }` target
declaration (without `binaries.executable()`) so the shared Compose UI
compiles for WasmJS. Keep `wasmJsMain` source set for platform `actual`
implementations.

**Patterns to follow:** Library modules don't have `binaries.executable()`
or resource processing tasks.

**Test scenarios:**
- `./gradlew :composeApp:compileKotlinWasmJs` passes (shared UI compiles)
- `./gradlew :composeApp:allTests` passes
- No `wasmJsProcessResources` or `wasmJsBrowserDistribution` tasks exist
  on `:composeApp`

**Verification:** `composeApp` has no entry-point code, no webpack config,
no distribution tasks.

---

### U4. Update Docker and CI infrastructure

**Goal:** Update build paths in Dockerfile, CI workflows, and root
build tasks.

**Requirements:** R5

**Dependencies:** U1, U2, U3

**Files:**
- `docker/Dockerfile` — update `COPY` source path from
  `composeApp/build/dist/...` to `webApp/build/dist/...`
- `build.gradle.kts` (root) — update `buildDockerImage` and
  `pushDockerImage` `dependsOn` from `:composeApp:wasmJsBrowserDistribution`
  to `:webApp:wasmJsBrowserDistribution`; update detekt task fan-out for
  module rename
- `.github/workflows/ci.yml` — update `distribution` job task, add/rename
  module-specific tasks, update `composeApp` job if test task changes
- `.github/workflows/release.yml` — no changes expected (uses root-level
  `pushDockerImage` which has its own `dependsOn`)

**Approach:** Follow the existing path patterns. The Docker entrypoint
doesn't change — it still runs `envsubst` on `index.html` in
`/app/web/`.

**Test scenarios:**
- `./gradlew buildDockerImage` succeeds
- Docker image serves both API and web app
- CI workflow tasks resolve (no "task not found" errors)

**Verification:** `./gradlew buildDockerImage` produces a working image.
CI dry-run (or push to PR) passes all jobs.

---

### U5. Update documentation

**Goal:** Update CLAUDE.md, README.md, and any other docs referencing
the old module structure.

**Requirements:** R6

**Dependencies:** U1, U2, U3, U4

**Files:**
- `CLAUDE.md` — update Architecture section (module descriptions,
  dependency graph), Build Commands section, Docker section
- `README.md` — update Project Structure section

**Approach:** Replace all references to `shared/` with `core/`, add
`webApp/` to the module list, update build command examples. Remove any
`composeApp` references that imply it's a WasmJS entry point.

**Test scenarios:**

Test expectation: none — documentation-only changes.

**Verification:** Grep for stale references to `shared/` (as a module
path, not the English word), `composeApp:wasmJsBrowserDistribution`,
or `composeApp/build/dist`.

---

## System-Wide Impact

- **Docker image build** — `COPY` source path changes from `composeApp/`
  to `webApp/` for the WasmJS distribution. Runtime behavior unchanged.
- **CI** — Module-specific Gradle tasks change names. The `distribution`
  job switches from `:composeApp:wasmJsBrowserDistribution` to
  `:webApp:wasmJsBrowserDistribution`.
- **Dev workflow** — `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
  becomes `./gradlew :webApp:wasmJsBrowserDevelopmentRun`. README updated
  to reflect.
- **Dependabot** — may need config update if it targets specific module
  paths (check `.github/dependabot.yml`).

---

## Deferred Implementation Notes

- Exact `webApp/build.gradle.kts` plugin and dependency declarations
  will be determined during implementation by referencing the existing
  `composeApp/build.gradle.kts` and `androidApp/build.gradle.kts`
- Whether `LocalStorageSettingsStore.kt` compiles in `webApp` without
  pulling in additional `wasmJsMain` dependencies — verify at
  implementation time
