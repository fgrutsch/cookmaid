---
title: Cross-user state leak on logout — Koin session-scope rebuild
date: 2026-04-19
category: security-issues
module: composeApp
problem_type: security_issue
component: authentication
symptoms:
  - Previous user's shopping lists, recipes, and meal plans visible after logout and re-login as different user
  - Singleton ViewModels and repository caches retained stale data across authentication boundaries
root_cause: scope_issue
resolution_type: code_fix
severity: high
tags:
  - koin
  - logout
  - session-scope
  - cross-user-leak
  - compose-multiplatform
  - singleton-lifecycle
---

# Cross-user state leak on logout — Koin session-scope rebuild

## Problem

On a shared device, logging out and logging in as a different user left the
previous user's data visible. Singleton ViewModels (`ShoppingListViewModel`,
`RecipeListViewModel`, `MealPlanViewModel`) and repository caches
(`ApiShoppingListRepository.cachedLists`, `ApiCatalogItemRepository.cachedItems`)
survived the logout because Koin registered them as app-scoped singletons.

## Symptoms

- User A logs out, User B logs in — User B sees User A's shopping lists
- Repository in-memory caches (`cachedLists`, `cachedItems`) not cleared
- Singleton VM state (`MutableStateFlow`) retained across auth transitions

## What Didn't Work

- **Option A — explicit `SessionCleaner` with per-singleton `clear()`/`resetState()`**:
  Shipped first (~1100 lines). Required every new user-scoped singleton to be
  manually registered in `SessionCleaner`. Fragile — easy to forget, verbose,
  and required splitting HTTP clients into interface/impl pairs solely for test
  substitution. The "register here" contract was documentation-only (CLAUDE.md
  note), with no compile-time or test-time enforcement.

## Solution

Split Koin modules by lifetime: `appModules` (auth infra, settings, user client
— survives logout) and `sessionModules` (repositories + singleton VMs — rebuilt
per user). In `App.kt`, reset session beans on every identity change:

```kotlin
// Above key(settingsState.locale) so language change does NOT wipe session state.
// remember (not DisposableEffect) runs synchronously before children compose.
val koin = getKoin()
remember(authState.user?.id) {
    koin.unloadModules(sessionModules)
    koin.loadModules(sessionModules)
}
```

`AuthViewModel.logout()` flips `status = Unauthenticated` + `user = null`
synchronously *before* launching the async `authHandler.logout()`, so the
Compose re-key fires immediately:

```kotlin
private fun logout() {
    updateState {
        copy(status = AuthState.Status.Unauthenticated, user = null,
             profile = UserProfile(), loginError = null)
    }
    launch {
        try { authHandler.logout() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { e.printStackTrace() }
    }
}
```

`OidcAuthHandler.logout()` still calls `tokenStore.removeTokens()` +
`httpClient.clearTokens()` to invalidate the bearer on the surviving
`ApiClient`.

## Why This Works

The root cause was scope mismatch: user-scoped state lived in app-scoped
singletons. `unloadModules` disposes existing singleton instances;
`loadModules` re-registers definitions so the next `koinInject<T>()` returns
a fresh instance. `key(authState.user?.id)` on the content subtree forces
children to re-compose and call `koinInject` again.

This replaces manual cleanup (~1100 lines) with a structural fix (~190 lines).
Adding a new user-scoped bean = put it in a module registered under
`sessionModules`. No cleanup code, no registration list.

## Prevention

- **Module placement rule**: any repository or singleton VM that holds or
  caches user data belongs in a module listed under `sessionModules` in
  `KoinModules.kt`. Auth infra and device-wide settings go in `appModules`.
- **Module-membership test**: `KoinSessionScopeTest` asserts both lists
  contain exactly the expected modules — catches miscategorization at CI.
- **Mechanism test**: same test verifies `unloadModules + loadModules`
  produces fresh singleton instances (not the same object reference).
- **Synchronous-reset test**: `AuthViewModelTest` asserts logout flips
  identity before the async handler runs — prevents the
  `Authenticated && user == null` flicker window.

## Related Issues

- [#73](https://github.com/fgrutsch/cookmaid/issues/73) — original bug report
- [#47](https://github.com/fgrutsch/cookmaid/issues/47) — why VMs are singletons
  (paginated list state preservation across tab switches)
