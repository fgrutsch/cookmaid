---
title: "feat: Add IDP account link and fix settings layout overlap"
type: feat
status: active
date: 2026-05-02
---

# feat: Add IDP account link and fix settings layout overlap

## Overview

Add a "Manage account" button to the Settings screen that opens the IDP
(Pocket ID) account management page in the browser. Fix the dark mode and
language picker widgets that overlap on narrow screens.

## Problem Frame

Users cannot manage their IDP profile (picture, name, email) from within the
app. The Settings screen displays profile info but offers no way to edit it.
Additionally, the segmented button rows for dark mode and language selection
overflow on small-width screens because they sit in a horizontal `Row` with
the label.

## Requirements Trace

- R1. "Manage account" button opens IDP account page in browser
- R2. Dark mode picker does not overlap its label on small screens
- R3. Language picker does not overlap its label on small screens

## Scope Boundaries

- No new OIDC endpoints or server changes
- No new environment variables — derive account URL from existing `discoveryUri`
- No changes to authentication flow or token handling

## Context & Research

### Relevant Code and Patterns

- `composeApp/.../ui/settings/SettingsScreen.kt` — target file for all UI changes
- `composeApp/.../ui/auth/OidcConfig.kt` — add `accountUrl` computed property
- `composeApp/.../App.kt` line 248–254 — wires SettingsScreen; needs account URL
- `composeApp/.../ui/recipe/detail/RecipeDetailComponents.kt` line 126 — pattern
  for `LocalUriHandler.current` + `uriHandler.openUri()`
- `composeApp/.../ui/mealplan/MealPlanScreen.kt` line 52 — same pattern
- `OidcConfig` is a Koin singleton via `single { oidcConfig }` in `App.kt` line 79
- String resources: `values/strings.xml`, `values-de/strings.xml`
- `KoinSessionScopeTest.kt` — asserts module membership; no change needed since
  OidcConfig lives in the platform module, not appModules/sessionModules

### Test Patterns

- ViewModel tests in `composeApp/src/commonTest/` (e.g., `ShoppingListViewModelTest`)
- No existing SettingsScreen or SettingsViewModel tests
- Test fakes throw `IllegalStateException` per CLAUDE.md

## Key Technical Decisions

- **Derive account URL from discoveryUri**: Strip `/.well-known/openid-configuration`
  suffix to get the IDP base URL. This avoids introducing a new env var. Add a
  computed `accountUrl` body property on `OidcConfig` (not a constructor parameter,
  to preserve the existing data class signature used by both platform entry points).
  Pocket ID serves account settings at the base URL when authenticated; if the
  actual path differs, adjust the derivation (e.g., append `/settings`).
- **Pass accountUrl as String parameter**: Use `oidcConfig.accountUrl` from the
  `App` function parameter (already in scope) and thread it through `MainContent`
  → `AppNavDisplay` → `SettingsScreen` → `SettingsContent`. All three intermediate
  signatures gain an `accountUrl: String` parameter. Keeps the screen decoupled
  from auth config internals.
- **Column layout for pickers**: Replace the `Row` layout in `DarkModePicker`
  and `LanguagePicker` with a `Column` so the label sits above the segmented
  buttons. Applies at all widths — this is a recipe app, not a data-dense
  dashboard, so the stacked layout is acceptable even on wide viewports. Avoids
  breakpoint complexity.
- **TextButton for manage account**: Use a `TextButton` below the user profile
  section (before the divider), center-aligned to match the profile info area.
  Keeps the action lightweight and visually consistent.

## Open Questions

### Resolved During Planning

- **How to get the IDP base URL?** Strip `/.well-known/openid-configuration`
  from `discoveryUri`. All OIDC discovery URIs follow this convention.

### Deferred to Implementation

- None

## Implementation Units

- [ ] **Unit 1: Add accountUrl to OidcConfig**

  **Goal:** Compute the IDP account management URL from the discovery URI.

  **Requirements:** R1

  **Dependencies:** None

  **Files:**
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/OidcConfig.kt`
  - Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/auth/OidcConfigTest.kt`

  **Approach:**
  - Add a computed `val accountUrl: String` body property (not constructor
    parameter) that removes `/.well-known/openid-configuration` from
    `discoveryUri`
  - Use `removeSuffix()` which is safe (returns original if suffix absent)

  **Patterns to follow:**
  - Existing `OidcConfig` data class style

  **Test scenarios:**
  - Happy path: discoveryUri ending in `/.well-known/openid-configuration`
    produces the base URL
  - Edge case: discoveryUri without the suffix returns the original URL unchanged

  **Verification:**
  - `./gradlew :composeApp:allTests` passes

- [ ] **Unit 2: Thread accountUrl to SettingsScreen and add manage account button**

  **Goal:** Display a "Manage account" button below the user profile that opens
  the IDP account page in the browser.

  **Requirements:** R1

  **Dependencies:** Unit 1

  **Files:**
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/settings/SettingsScreen.kt`
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/App.kt`
  - Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`
  - Modify: `composeApp/src/commonMain/composeResources/values-de/strings.xml`

  **Approach:**
  - In `App.kt`: use `oidcConfig.accountUrl` (already available as function
    parameter), pass through `MainContent` → `AppNavDisplay` → `SettingsScreen`
    (all three gain an `accountUrl: String` parameter)
  - In `SettingsScreen`: add `accountUrl: String` parameter, pass to
    `SettingsContent`
  - In `SettingsContent`: add a `TextButton` after `UserProfileSection` that
    calls `uriHandler.openUri(accountUrl)` using `LocalUriHandler.current`
  - Add string resources: EN `"Manage account"`, DE `"Konto verwalten"`

  **Patterns to follow:**
  - `RecipeDetailComponents.kt` line 126 for `LocalUriHandler` usage
  - Existing parameter threading in `App.kt` → `MainContent` → `AppNavDisplay`

  **Test scenarios:**
  - Test expectation: none — pure UI wiring with no testable logic beyond
    what Unit 1 covers. The composable delegates to `uriHandler.openUri()`.

  **Verification:**
  - App compiles: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
  - Button visible in Settings screen, opens IDP URL on click

- [ ] **Unit 3: Fix dark mode and language picker layout for small screens**

  **Goal:** Prevent label/segmented-button overlap on narrow screens.

  **Requirements:** R2, R3

  **Dependencies:** None (can be done in parallel with Units 1–2)

  **Files:**
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/settings/SettingsScreen.kt`

  **Approach:**
  - In `DarkModePicker`: replace `Row(Arrangement.SpaceBetween)` with a
    `Column` so the label sits above the `SingleChoiceSegmentedButtonRow`
  - In `LanguagePicker`: same change
  - Keep the `SingleChoiceSegmentedButtonRow` at `fillMaxWidth()` so buttons
    expand to full width under the label

  **Patterns to follow:**
  - Existing `Column` usage in `UserProfileSection` and `SettingsContent`

  **Test scenarios:**
  - Test expectation: none — pure layout change with no behavioral logic.

  **Verification:**
  - No overlap visible on narrow viewport (e.g., 320px width in browser)
  - Normal-width viewport still looks correct

## System-Wide Impact

- **Interaction graph:** `SettingsScreen` gains a new outbound link via
  `LocalUriHandler`. No callbacks or middleware affected.
- **Error propagation:** `openUri()` delegates to the platform — no app-level
  error handling needed.
- **State lifecycle risks:** None — no new state introduced.
- **API surface parity:** Android and WasmJS both use `LocalUriHandler` which
  maps to platform browser/intent. No platform-specific code needed.
- **Unchanged invariants:** `OidcConfig` remains a data class; the new property
  is derived, not stored.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Discovery URI doesn't follow `/.well-known/openid-configuration` convention | `removeSuffix()` is safe — returns original if suffix absent. Works for any OIDC provider. |

## Sources & References

- Related issue: #38
