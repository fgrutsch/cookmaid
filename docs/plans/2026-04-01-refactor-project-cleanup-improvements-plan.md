---
title: "refactor: project cleanup/improvements"
type: refactor
status: active
date: 2026-04-01
origin: docs/brainstorms/2026-04-01-project-cleanup-brainstorm.md
---

# Project Cleanup/Improvements

## Overview

Maintenance pass: remove unused dependencies, upgrade all dependency/plugin
versions to latest stable, and reorganize `libs.versions.toml` with
consistent grouping and alphabetical sorting.

## Acceptance Criteria

- [x] Unused library and version entries removed from `libs.versions.toml`
- [x] Unused dependencies removed from `build.gradle.kts` files
- [x] All dependencies upgraded to latest stable versions
- [x] `libs.versions.toml` reorganized: grouped by domain, alphabetical within groups
- [x] `./gradlew detektAll` passes
- [x] `./gradlew test` passes (all modules)
- [x] `./gradlew :composeApp:assembleDebug` succeeds
- [ ] `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` succeeds (manual smoke)

## Phase 1: Remove Unused Dependencies

### `gradle/libs.versions.toml`

Remove these unused version entries:
- `androidx-appcompat` (1.7.1)
- `androidx-core` (1.18.0)
- `androidx-espresso` (3.7.0)
- `androidx-testExt` (1.3.0)

Remove these unused library entries:
- `androidx-appcompat`
- `androidx-core-ktx`
- `androidx-espresso-core`
- `androidx-testExt-junit`

### `shared/build.gradle.kts`

- Remove `kotlinx-coroutines-core` from `commonMain` — not used in shared
  source code (no suspend functions, no Flow usage)
- Remove `koin-core` api from `commonMain` — Koin is not used in shared code;
  consumers (`composeApp`, `server`) already declare their own Koin deps
  which pull in `koin-core` transitively

**Verification**: build all targets after removals to catch transitive breakage.

## Phase 2: Version Upgrades

### Safe patches (no breaking changes)

| Dependency | Current | Target |
|---|---|---|
| Kotlin | 2.3.10 | 2.3.20 |
| Compose Multiplatform | 1.10.2 | 1.10.3 |
| Ktor | 3.4.1 | 3.4.2 |
| Flyway | 12.1.0 | 12.3.0 |
| Kover | 0.9.1 | 0.9.8 |

**Note**: Verify Compose 1.10.3 + Kotlin 2.3.20 compatibility before bumping
both. Check the [Compose-Kotlin compatibility matrix](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compatibility-and-versioning.html).

### Minor upgrades (small migration possible)

| Dependency | Current | Target | Notes |
|---|---|---|---|
| Koin | 4.1.1 | 4.2.0 | Check `lazyModule` rename, ViewModel scope changes |
| AndroidX Lifecycle | 2.9.6 | 2.10.0 | minSdk 21→23 (we use 24, OK) |

### Also check for updates (not covered by research)

- `coil` (3.4.0)
- `kotlin-logging` (8.0.01)
- `materialkolor` (4.1.1)
- `multiplatform-settings` (1.3.0)
- `oidc` (0.16.5)
- `nimbus-jose-jwt` (10.3)
- `junit5` (5.12.2)
- `navigation3` (1.0.0-alpha06)
- `lifecycle-viewmodel-nav3` (2.10.0-beta01)
- `material3` (1.10.0-alpha05)

### Out of scope (major breaking, separate issue)

| Dependency | Current | Latest | Why defer |
|---|---|---|---|
| AGP | 8.11.2 | 9.1.0 | Requires Gradle 9, KMP migration guide, significant work |
| Gradle | 8.14.3 | 9.4.1 | Kotlin DSL changes, removed APIs, plugin compat |
| Testcontainers | 1.21.1 | 2.0.4 | Module renaming, package relocation, API changes |

**Upgrade order**: Kotlin + Compose together → Ktor → Koin → Flyway →
Kover → AndroidX Lifecycle → remaining libs. Build + test after each group.

## Phase 3: Reorganize `libs.versions.toml`

Group versions and libraries by domain, alphabetical within groups:

```
[versions]
# Android
agp, android-compileSdk, android-minSdk, android-targetSdk,
  androidx-activity, androidx-lifecycle

# Compose
composeMultiplatform, lifecycle-viewmodel-nav3, material3,
  materialkolor, navigation3

# Kotlin
kotlin, kotlinx-coroutines, kotlinx-datetime, kotlinx-serialization

# Ktor
ktor

# DI
koin

# Database
exposed, flyway, postgresql

# Auth
nimbus-jose-jwt, oidc

# Testing
junit5, testcontainers

# Tooling
detekt, kover

# Misc
coil, kotlin-logging, logback, multiplatform-settings
```

Apply same grouping to `[libraries]`, `[bundles]`, and `[plugins]` sections.

## Sources

- **Origin brainstorm:** docs/brainstorms/2026-04-01-project-cleanup-brainstorm.md
- Related issue: #30
