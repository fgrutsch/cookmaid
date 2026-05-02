---
title: Deriving IDP account URL from OIDC discovery URI
module: composeApp
date: 2026-05-02
problem_type: best_practice
component: authentication
severity: low
tags:
  - oidc
  - pocket-id
  - settings
  - compose-multiplatform
applies_when:
  - Adding links to IDP account management from within the app
  - Needing the IDP base URL without introducing new environment variables
---

# Deriving IDP account URL from OIDC discovery URI

## Context

The app needed a "Manage account" link in the Settings screen that opens
the IDP (Pocket ID) account management page in the browser. The OIDC
discovery URI was already available (`OidcConfig.discoveryUri`) but there
was no separate config for the account management URL.

## Guidance

Derive the IDP base URL by stripping the OIDC discovery suffix from
`discoveryUri` using `removeSuffix()`:

```kotlin
data class OidcConfig(...) {
    val accountUrl: String
        get() = discoveryUri.removeSuffix("/.well-known/openid-configuration")
}
```

Key decisions:
- **Body property with `get()`**, not a constructor parameter — preserves
  the data class signature used by both platform entry points (Android
  `MainActivity.kt` and WasmJS `main.kt`)
- **`removeSuffix()`** is safe — returns the original string unchanged if
  the suffix is absent
- **Thread as `String`** through composable chain rather than injecting
  `OidcConfig` directly — keeps the Settings screen decoupled from auth
  config internals

## Why This Matters

Avoids introducing a new environment variable for a value that can be
deterministically derived from existing config. The OIDC discovery URI
always follows `<base>/.well-known/openid-configuration` per the OpenID
Connect Discovery spec (Section 4).

## When to Apply

- When you need the IDP base URL and only have the discovery URI
- When the IDP serves account settings at its base URL (Pocket ID does)
- If the IDP uses a different path for account management (e.g.,
  `/settings`), adjust the derivation accordingly

## Examples

Before (no account URL available):
```kotlin
// Settings screen shows profile info but no way to manage it
UserProfileSection(userProfile)
```

After (derived from existing config):
```kotlin
val uriHandler = LocalUriHandler.current
UserProfileSection(userProfile)
TextButton(
    onClick = { uriHandler.openUri(accountUrl) },
    modifier = Modifier.align(Alignment.CenterHorizontally),
) {
    Text(Res.string.settings_manage_account.resolve())
}
```
