---
title: "Fix wasmJs expand() writing literal 'null' when local.properties is absent"
date: 2026-04-10
problem_type: build_configuration
component: composeApp/wasmJs
symptom: >
  When local.properties does not exist (CI/production), Properties.getProperty() returns null,
  causing expand() to write the literal string "null" into index.html placeholders
  (OIDC_DISCOVERY_URI, OIDC_CLIENT_ID, OIDC_SCOPE), leaving Docker envsubst with nothing to substitute.
tags:
  - gradle
  - wasm
  - kotlin-multiplatform
  - ci
  - oidc
  - envsubst
  - local-properties
related_issues:
  - https://github.com/fgrutsch/cookmaid/issues/61
---

## Root Cause

`Properties.getProperty()` returns `null` when a key is absent. The `expand()` call in
`wasmJsProcessResources` always ran — even when `local.properties` didn't exist — so Gradle's
template engine replaced `${OIDC_DISCOVERY_URI}` etc. with the literal string `"null"`. The
`envsubst` step in `docker-entrypoint.sh` expects raw `${VAR}` placeholders; finding none, it
left the `"null"` strings in place.

## Symptoms

- OIDC config fields contained the string `"null"` in the served `index.html`
- Login/auth flow broke in CI-built Docker images
- Local dev was unaffected (values present in `local.properties`)

## Fix

Early-exit the task when `local.properties` is absent. `return@named` skips the entire
`filesMatching` block, leaving `${VAR}` placeholders untouched for `envsubst` to fill at
container startup.

**Before (broken):**

```kotlin
tasks.named<Copy>("wasmJsProcessResources") {
    val localProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
    }
    filesMatching("index.html") {
        expand(
            "OIDC_DISCOVERY_URI" to localProps.getProperty("oidc.discoveryUri"),
            "OIDC_CLIENT_ID" to localProps.getProperty("oidc.clientId"),
            "OIDC_SCOPE" to localProps.getProperty("oidc.scope"),
        )
    }
}
```

**After (fixed):**

```kotlin
tasks.named<Copy>("wasmJsProcessResources") {
    val localPropsFile = rootProject.file("local.properties").takeIf { it.exists() } ?: return@named
    val localProps = Properties().apply { localPropsFile.reader().use { load(it) } }

    filesMatching("index.html") {
        expand(
            "OIDC_DISCOVERY_URI" to localProps.getProperty("oidc.discoveryUri"),
            "OIDC_CLIENT_ID" to localProps.getProperty("oidc.clientId"),
            "OIDC_SCOPE" to localProps.getProperty("oidc.scope"),
        )
    }
}
```

## Environment Behaviour

| Environment | `local.properties` | Result |
|---|---|---|
| Local dev | Present | `expand()` runs — placeholders substituted at build time |
| CI / production | Absent | `return@named` — placeholders stay as `${VAR}` for `envsubst` |

## Prevention

### Code Review Detection

- Flag any `expand()` call where the map is populated from an optional/conditional source
- Verify every key referenced in the template has a guaranteed non-null value
- If placeholders must survive to runtime, confirm they are **not** passed to `expand()` at all

### General Rule for `expand()`

> Only pass keys to `expand()` that you can guarantee are non-null. Gradle silently writes
> `"null"` with no warning when a value is absent.

- Build the map explicitly with only the keys you want substituted
- Never pass a `Properties` object loaded from an optional file without null-checking every value
- If runtime substitution is needed (Docker `envsubst`), exclude those keys from `expand()` entirely

### CI Verification

Add a step after the WasmJS distribution build to assert no placeholders were corrupted:

```yaml
- name: Assert OIDC placeholders intact in index.html
  run: |
    FILE=composeApp/build/dist/wasmJs/productionExecutable/index.html
    grep -qE '\$\{OIDC_[A-Z_]+\}' "$FILE" \
      || { echo "ERROR: OIDC placeholders missing from index.html"; exit 1; }
    grep -qF '"null"' "$FILE" \
      && { echo "ERROR: 'null' found in OIDC config — expand() received null value"; exit 1; } \
      || true
```

## Related

- [Docker image setup (KMP/Ktor/WasmJS)](../infrastructure/docker-image-kmp-ktor-wasmjs.md) —
  covers `docker-entrypoint.sh` `envsubst` pattern and build-time vs runtime config split
- GitHub issue: fgrutsch/cookmaid#61
