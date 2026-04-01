---
title: Dependency cleanup and version catalog maintenance
date: 2026-04-01
category: build-maintenance
tags:
  - gradle
  - version-catalog
  - dependency-management
  - kotlin-multiplatform
severity: low
modules_affected:
  - shared
  - composeApp
  - server
  - gradle/libs.versions.toml
symptoms:
  - Unused dependencies declared in version catalog and module build files
  - Outdated dependency versions across multiple libraries
  - Disorganized libs.versions.toml lacking consistent grouping
  - Orphaned library entries in catalog with no module consumer
root_cause: >
  Incremental feature development added dependencies that were later
  superseded or never fully adopted, leaving stale entries in the Gradle
  version catalog and module build scripts. No automated unused-dependency
  check was in place, so dead entries accumulated silently.
---

# Dependency Cleanup and Version Catalog Maintenance

## Root Cause

The shared module declared `koin-core` (api) and `kotlinx-coroutines-core`
(implementation) but neither was used in shared source code. Four AndroidX
libraries from initial scaffolding were never referenced. The version catalog
had no consistent grouping, making orphaned entries hard to spot.

## Solution Steps

1. Remove unused deps from `shared/build.gradle.kts` (koin-core, coroutines)
2. Remove unused AndroidX entries from `libs.versions.toml` (versions + libraries)
3. Remove orphaned library entries that no build file references
4. Bump all dependencies to latest stable versions
5. Reorganize `libs.versions.toml` with domain grouping, alphabetical within groups
6. Verify: `./gradlew detektAll test`, `assembleDebug`, wasmJs build

## Code Examples

Removing unused deps from `shared/build.gradle.kts`:

```kotlin
// BEFORE
commonMain.dependencies {
    api(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
}

// AFTER
commonMain.dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
}
```

## Gotchas

- **Removing a dep from build.gradle.kts is not enough.** You must also
  remove the matching library alias from `libs.versions.toml`. Orphaned
  catalog entries confuse future maintainers.
- **Koin 4.2.0 `koinConfiguration {}` is not ready.** The deprecation
  message suggests using it, but the parameter name doesn't match what
  the compiler expects. Wait for 4.2.1+.
- **Transitive dependency safety.** Before removing `koin-core` from
  shared, verify every consumer module independently declares a Koin dep
  that pulls in `koin-core` transitively.
- **kotlinx-coroutines-core may be needed later.** Safe to remove only
  because shared currently has no suspend functions or Flow usage.
- **Navigation3 and Material3 are alpha.** Pin and test carefully on
  upgrades — breaking API changes likely.

## Prevention Strategies

- Treat `libs.versions.toml` as the source of truth. Any PR that touches
  build.gradle.kts deps must also review the version catalog.
- After removing a dependency, grep the project for its alias. Zero hits
  means remove the library and version entries too.
- Keep `[versions]`, `[libraries]`, `[bundles]`, and `[plugins]` in the
  same logical grouping order. Slot new entries alphabetically.
- Pin explicit dependencies only. If a library is pulled in transitively
  and no code imports it directly, don't add it to the catalog.

## Dependency Cleanup Checklist

- [ ] For every removed dep in build.gradle.kts, confirm the library
  entry is also removed (or still used by another module)
- [ ] For every removed library entry, confirm the version key is still
  used by at least one other library/plugin
- [ ] Grep the full project for the old alias — zero results expected
- [ ] `./gradlew test` passes across all modules
- [ ] `./gradlew detektAll` passes
- [ ] `./gradlew :composeApp:assembleDebug` succeeds
- [ ] `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` builds cleanly
- [ ] Review build output for new deprecation warnings
- [ ] Verify consistent alphabetical ordering across all toml sections

## Testing Approach

- **Build verification**: `./gradlew test` + `detektAll` is the minimum gate
- **Dependency tree diffing**: Compare `./gradlew :module:dependencies`
  before/after to catch unexpected transitive changes
- **Runtime smoke tests**: Start server and wasmJs app after upgrades —
  Koin graph resolution and classpath issues only surface at runtime
- **Koin-specific**: Integration tests that bootstrap the full Koin module
  set catch missing bindings that compile fine but fail at runtime

## References

- PR: #33
- Issue: #30
