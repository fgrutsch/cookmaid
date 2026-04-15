---
title: Cross-user state leak on logout via singleton-scoped client state
date: 2026-04-15
category: security-issues
module: composeApp/ui/auth
problem_type: security_issue
component: authentication
symptoms:
  - After logout, repository caches and singleton ViewModel state retained the previous user's data
  - Logging in as a different user briefly showed user A's recipes, shopping items, and meal plan entries
  - Only a natural refresh (pull-to-refresh, tab navigation) eventually overwrote the stale data
root_cause: scope_issue
resolution_type: code_fix
severity: medium
related_components:
  - shopping
  - catalog
  - mealplan
  - recipe
tags:
  - kotlin-multiplatform
  - koin
  - logout
  - session-state
  - mvi
  - structured-concurrency
---

# Cross-user state leak on logout via singleton-scoped client state

## Problem

In a Kotlin Multiplatform (Android / Web / Ktor) app using Koin for DI and
MVI for UI state, **every Koin `single {}` binding is app-scoped, not
user-scoped**. Repository caches and singleton ViewModels populated while
user A was signed in survived logout and were visible to user B on the
next login, until a refresh naturally overwrote them.

See fgrutsch/cookmaid#73 and fgrutsch/cookmaid#77 for the full fix.

## Symptoms

- `ApiShoppingListRepository.cachedLists` and
  `ApiCatalogItemRepository.cachedItems` held user A's data after logout.
- `RecipeListViewModel`, `ShoppingListViewModel`, `MealPlanViewModel` —
  intentionally singletons to preserve paginated state across tab switches
  (see #47) — retained A's recipes, lists, and meal-plan entries.
- Between logout and user B's first API fetch, the UI showed A's data.

## What Didn't Work

- **Relying on refresh-on-login alone.** `ShoppingListViewModel.fetchLists`
  calls `repository.getLists(refresh = true)` on load events, which does
  overwrite the cache — but the visible *flash* of A's data still occurred
  between login and the fetch returning.
- **Letting `AuthViewModel.logout()` do the cleanup.** `AuthViewModel`
  only reset its own identity fields. It had no handle on the other
  singletons, and leaking concrete types into it would have created a
  wide dependency surface.
- **Wiring cleanup into `OidcAuthHandler.logout()` inline.** Adding the
  repo / VM dependencies directly to the handler leaked shopping, catalog,
  recipe, and mealplan types into the auth package, defeating the
  separation that `AuthHandler` is supposed to preserve.

## Solution

Introduce a **global `SessionCleaner`** that owns the entire logout
cleanup sequence — OIDC tokens, Ktor auth state, repository caches, and
singleton ViewModel resets. `OidcAuthHandler.logout()` delegates to it.
`SessionCleaner` lives in its own `sessionModule` so `authModule` does
not need to import feature-package types.

Shape:

```kotlin
class SessionCleaner(
    private val tokenStore: TokenStore,
    private val httpClient: HttpClient,
    private val shoppingListRepository: ApiShoppingListRepository,
    private val catalogItemRepository: ApiCatalogItemRepository,
    private val mealPlanRepository: ApiMealPlanRepository,
    private val recipeRepository: ApiRecipeRepository,
    private val recipeListViewModel: RecipeListViewModel,
    private val shoppingListViewModel: ShoppingListViewModel,
    private val mealPlanViewModel: MealPlanViewModel,
) {
    suspend fun clearAll() {
        runStep { tokenStore.removeTokens() }           // 1. most critical
        runStep { httpClient.clearTokens() }            // 2. Ktor bearer cache
        runStep { shoppingListRepository.clear() }
        runStep { catalogItemRepository.clear() }
        runStep { mealPlanRepository.clear() }
        runStep { recipeRepository.clear() }
        runStep { recipeListViewModel.resetState() }
        runStep { shoppingListViewModel.resetState() }
        runStep { mealPlanViewModel.resetState() }
    }

    private suspend fun runStep(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e                                     // honor structured concurrency
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            e.printStackTrace()                         // best-effort, never throws
        }
    }
}
```

Key implementation details:

1. **Tokens first.** `tokenStore.removeTokens()` runs before any repo /
   VM cleanup so a racing in-flight request will 401 rather than
   repopulate a just-cleared cache.

2. **Rethrow `CancellationException`.** The bare `catch (e: Exception)`
   would swallow it, violating Kotlin structured concurrency — matches
   the pattern already used in `MviViewModel.launch` / `launchOptimistic`.

3. **Synchronous identity reset in `AuthViewModel.logout()`.** Transition
   `status`, `user`, and `profile` together *before* launching the
   cleanup coroutine — otherwise observers see the invariant break
   `status == Authenticated && user == null` for the duration of the
   suspend call.

   ```kotlin
   private fun logout() {
       updateState {
           copy(
               status = AuthState.Status.Unauthenticated,
               user = null,
               profile = UserProfile(),
               loginError = null,
           )
       }
       launch { authHandler.logout() }   // best-effort cleanup
   }
   ```

4. **Client interfaces for testability.** The four HTTP clients
   (`ShoppingListClient`, `CatalogItemClient`, `MealPlanClient`,
   `RecipeClient`) were split into `interface` + `Api*Client` impl so
   repository-level tests can inject fake clients without booting the
   full OIDC / Ktor stack.

## Why This Works

- **Koin singletons are app-scoped, not user-scoped.** Option A from the
  issue (explicit clears) is the smallest diff that works without
  rebuilding the Koin graph (Option B) or introducing a Koin user scope
  (Option C).
- **A single aggregator is grep-able.** One file to open when asking
  "what dies on logout?" — the KDoc says "register new user-scoped
  singletons here" so the next person adding a cache has an obvious
  home.
- **Best-effort cleanup preserves the security invariant.** Each step
  runs in its own try/catch so a single failure can't skip subsequent
  critical steps. Tokens clear first so even if a later step throws,
  the auth state is gone.
- **Identity reset runs synchronously.** Any composable that reads
  `AuthState` during the cleanup window sees consistent state
  (`Unauthenticated`, null user) throughout.

## Prevention

- **Audit every `catch (e: Exception)` in suspend code** for a missing
  `CancellationException` rethrow. In Kotlin, `CancellationException`
  extends `IllegalStateException` extends `Exception`, so the bare catch
  silently swallows cancellation. Project convention (see
  `MviViewModel.launch` / `launchOptimistic`) is to catch and rethrow
  it first:

  ```kotlin
  try {
      block()
  } catch (e: CancellationException) {
      throw e
  } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      // handle
  }
  ```

- **Document singleton-scope hazards.** In MVI apps, any singleton that
  accumulates per-user state is a logout-leak waiting to happen. Pattern
  to follow: central `SessionCleaner`, KDoc naming it as *the* place to
  register new singletons, end-to-end regression test that populates
  state as user A, logs out, logs in as B, and asserts B sees only B's
  data.

- **Make the regression test exercise the real wire-up.** The test
  should drive the flow through the actual entry points
  (`AuthViewModel.logout()` → `OidcAuthHandler.logout()` →
  `SessionCleaner.clearAll()`), not invoke the cleaner directly. A
  developer could otherwise delete the delegation in `OidcAuthHandler`
  and the "regression" test would still pass.

- **`launchOptimistic` rollback is a known race window** — its catch
  block unconditionally does `state.update { snapshot }`, which can
  restore pre-logout state into a just-cleared ViewModel if a user
  triggered an optimistic action before logging out and the request
  fails post-logout. Accepted as out-of-scope here; fix requires a
  generation counter or scope cancellation. Worth a TODO in the
  `MviViewModel` KDoc for the next person who hits it.

- **Extract client-as-interface when you need repo-level tests.** The
  `ShoppingListClient` → `interface + ApiShoppingListClient` split made
  `ApiShoppingListRepositoryTest` / `ApiCatalogItemRepositoryTest`
  trivial. Matches the existing `Api*Repository` naming convention.

## Related Issues

- fgrutsch/cookmaid#73 — original bug report (Option A / B / C analysis
  in issue body)
- fgrutsch/cookmaid#77 — this fix's PR
- fgrutsch/cookmaid#47 — why the list ViewModels are singletons in the
  first place (paginated state preservation across tab switches)
- Plan: `docs/plans/2026-04-15-001-fix-clear-user-state-on-logout-plan.md`
- Review artifact: `.context/compound-engineering/ce-review/20260415-202123-b36da19f/summary.md`
