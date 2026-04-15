---
title: "fix: Clear user-scoped client state on logout"
type: fix
status: active
date: 2026-04-15
deepened: 2026-04-15
---

# fix: Clear user-scoped client state on logout

## Overview

On logout, the Koin graph and its singletons stay alive for the lifetime of
the app process / session. Per-user state leaks across the logout → login
boundary: cached lists in repositories and accumulated state in singleton
ViewModels. When a different user logs in on the same instance, the UI
briefly shows the previous user's data before an API refresh overwrites it.

This plan implements **Option A** from issue #73 — explicit resets — but
shapes the aggregator as a **global `SessionCleaner`** that owns the full
logout-time cleanup sequence: OIDC token removal, Ktor auth clearing,
repository cache clears, and singleton ViewModel resets. `OidcAuthHandler`
delegates its `logout()` to `SessionCleaner`, making the cleaner the
single grep-able place for "what dies when a user logs out".

Options B (Koin graph rebuild on auth transitions) and C (Koin user scope)
remain deferred.

## Problem Frame

Per-user state lives in several singletons:

- **OIDC / token state** — `TokenStore` (access / refresh / id tokens) and
  the Ktor `Auth` plugin's internal bearer cache (`httpClient.clearTokens()`).
- **Repository caches** — `ApiShoppingListRepository.cachedLists`,
  `ApiCatalogItemRepository.cachedItems` (catalog is global, so this is
  staleness rather than a security leak, but still worth clearing for
  consistency).
- **Singleton ViewModels** — `RecipeListViewModel`, `ShoppingListViewModel`,
  `MealPlanViewModel` are singletons by design (see #47) to preserve
  paginated list state across tab switches. After logout they retain the
  previous user's recipes, shopping items, and meal plan entries.
- **AuthViewModel identity** — `AuthState.user` (User UUID) and
  `AuthState.profile` (name / email / picture from the ID token). Cleared
  today, but *after* the suspend handler returns, so it lingers through
  the cleanup window.

`SettingsViewModel` holds only user-agnostic preferences (locale / theme)
and is explicitly out of scope. `OpenIdConnectClient`'s discovery state
is per-issuer, not per-user — nothing to clear there.

Flow today:

1. User clicks logout in `SettingsScreen`.
2. `AuthViewModel.logout()` → `authHandler.logout()`.
3. `OidcAuthHandler.logout()` removes tokens + clears Ktor auth.
4. `AuthViewModel` transitions state to `Unauthenticated` and nulls user
   / profile.
5. Repositories and ViewModels still hold user A's data until a natural
   refresh overwrites it on the next login.

## Requirements Trace

- **R1.** After logout, `ApiShoppingListRepository.cachedLists` is empty.
- **R2.** After logout, `ApiCatalogItemRepository.cachedItems` is empty.
- **R3.** After logout, the three list ViewModels return to their initial
  state (no stale recipes / shopping items / meal plan entries).
- **R4.** User A → logout → login as User B: no flash of A's data in the
  Shopping, Recipes, or MealPlan screens. Proven by an end-to-end test
  that actually simulates B's login and asserts B's loaded data shows
  only B's records.
- **R5.** Test coverage for the clear-on-logout flow — per-target unit
  tests plus the end-to-end regression test.
- **R6.** A single failing sub-cleanup does not abort the rest of the
  logout. Tokens and HTTP auth are always cleared.

## Scope Boundaries

- Not touching Koin scope rewiring (Option C).
- Not rebuilding the Koin graph on auth transitions (Option B).
- `SettingsViewModel` untouched — locale / theme are user-agnostic.
- `OpenIdConnectClient` not reset — discovery state is per-issuer.
- No in-flight coroutine cancellation — see Risks.
- No CLAUDE.md or external doc updates.

## Context & Research

### Relevant Code and Patterns

- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/OidcAuthHandler.kt`
  — current logout path: removes tokens, clears Ktor tokens. Will delegate
  to `SessionCleaner`.
- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/AuthHandler.kt`
  — minimal `AuthHandler` interface used by `AuthViewModel`. Unchanged.
- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/AuthViewModel.kt`
  — `logout()` clears identity *after* `authHandler.logout()` returns;
  plan moves this reset to run *before* the cleaner fires.
- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/ApiClient.kt`
  — holds the `HttpClient` whose `Auth` plugin caches bearer tokens.
  `httpClient.clearTokens()` is the existing invocation.
- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/shopping/ShoppingListRepository.kt`
  — holds `cachedLists` behind a `Mutex`. New `clear()` must use the same
  mutex.
- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/catalog/CatalogItemRepository.kt`
  — holds `cachedItems` behind a `Mutex`. Same pattern.
- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/mealplan/MealPlanRepository.kt`
  and `.../recipe/RecipeRepository.kt` — no cache today; get no-op
  `clear()` methods to future-proof the aggregator (see Key Decisions).
- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/common/MviViewModel.kt`
  — `updateState { reducer }` is the canonical way to mutate state. VM
  resets use it; `launchOptimistic` is the rollback gotcha (see Risks).
- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListContract.kt`
  — `RecipeListState()` no-arg constructor is the initial state.
- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/shopping/ShoppingListContract.kt`
  — `ShoppingListState()` no-arg constructor is the initial state.
- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/mealplan/MealPlanContract.kt`
  — `MealPlanState(currentWeekStart = mondayOfWeek(today))` — the reset
  re-computes `currentWeekStart` from the clock, matching construction.
- `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/auth/FakeAuthHandler.kt`
  — existing fake pattern. New `FakeSessionCleaner` follows the same
  conventions (no `RuntimeException`, `= Unit` for no-op overrides).

### Institutional Learnings

- `docs/solutions/` does not exist in this repo yet; no prior learnings.
- CLAUDE.md — `composeApp` section confirms singleton ViewModels (#47),
  `launchOptimistic` / `updateState` as mutation primitives, test fakes
  throw `IllegalStateException`, and no-op overrides use `= Unit`.
- CLAUDE.md "Debounced MutableStateFlow inputs" — regression test with a
  call-count counter on the repository fake, not state-only assertions.

### External References

None — internal architectural change against established Koin and MVI
conventions already in use.

## Key Technical Decisions

- **Global `SessionCleaner` owns the entire logout cleanup sequence.**
  It removes tokens, clears Ktor auth, and resets every user-scoped
  singleton. `OidcAuthHandler.logout()` delegates to it one-liner;
  `AuthViewModel.logout()` does not need to change its call to
  `authHandler.logout()`. This gives us one file to open when asking
  "what dies when a user logs out?".
- **`SessionCleaner` is a concrete class, not an interface.** One
  consumer (`OidcAuthHandler`), one implementation. A concrete class
  avoids the speculative-generality smell that would otherwise make the
  aggregator look like ceremony.
- **Identity-state cleared before data-state cleanup.** The current
  `AuthViewModel.logout()` sets `user = null` and
  `profile = UserProfile()` *after* `authHandler.logout()` returns. Move
  the identity reset to fire *before* the suspend call, so any composable
  observing `AuthState` sees the unauthenticated identity throughout the
  cleanup window. The final `status = Unauthenticated` transition still
  happens after the cleaner returns.
- **Best-effort cleanup semantics.** `SessionCleaner.clearAll()` wraps
  each individual step in a try / catch, logs exceptions, and continues.
  A failure in one sub-cleanup must not leave tokens cached or later
  cleanups un-run — that would defeat the fix's own purpose. Token /
  HTTP-auth clearing runs first and is the most critical from a security
  standpoint.
- **Do not widen repository interfaces with `clear()`.** Add `clear()`
  only to the concrete `ApiShoppingListRepository`,
  `ApiCatalogItemRepository`, `ApiMealPlanRepository`, and
  `ApiRecipeRepository` classes. `SessionCleaner` depends on concrete
  types (they are singletons — DI-fine). Fakes in `commonTest` do not
  need to add `clear()` — repository interface tests don't exercise the
  cleaner.
- **No-op `clear()` on `ApiMealPlanRepository` / `ApiRecipeRepository`.**
  These don't cache today, but registering them in `SessionCleaner`
  now costs one empty method each and removes the trap-door for the
  next person who adds a cache.
- **`SessionCleaner` registered in a dedicated `sessionModule`.** Not in
  `authModule` — that would force `authModule` to import concrete types
  from `shopping`, `catalog`, `recipe`, and `mealplan` packages,
  contradicting the aggregator's own goal of keeping auth unaware of
  feature packages.
- **ViewModel `resetState()` uses `updateState { InitialState() }`, not
  a job-cancel sweep.** See Risks for the `launchOptimistic` caveat.
- **`MealPlanViewModel.resetState()` re-computes `currentWeekStart`**
  from the system clock, matching the constructor. A "week" is not user
  state, but resetting to the current week matches the fresh-login UX.

## Open Questions

### Resolved During Planning

- *Where to hook cleanup?* — Global `SessionCleaner` invoked by
  `OidcAuthHandler.logout()`.
- *Interface-based or concrete?* — Concrete class; one consumer.
- *Do we cancel in-flight coroutines?* — No (see Risks).
- *Fail-fast or best-effort error handling?* — Best-effort with logging.
- *Do we widen repository interfaces?* — No; concrete-only `clear()`.
- *Do we clear `OpenIdConnectClient` state?* — No; it's per-issuer.

### Deferred to Implementation

- *Exact method names* — pick names that read well at the cleaner's
  call sites (`clear()` on repos, `resetState()` on VMs).
- *Logging mechanism in the best-effort catch blocks* — the app has no
  central logger yet; for now, printing stack traces in debug builds is
  acceptable. Do not swallow silently.

## High-Level Technical Design

> *This illustrates the intended approach and is directional guidance
> for review, not implementation specification.*

```
AuthViewModel.logout()
  ├─ updateState { user = null, profile = UserProfile() }   (new — identity reset first)
  ├─ authHandler.logout()
  │    └─> OidcAuthHandler.logout()   (becomes one-liner)
  │          └─ sessionCleaner.clearAll()
  │                ├─ tokenStore.removeTokens()
  │                ├─ apiClient.httpClient.clearTokens()
  │                ├─ shoppingListRepository.clear()    (concrete impl)
  │                ├─ catalogItemRepository.clear()     (concrete impl)
  │                ├─ mealPlanRepository.clear()        (no-op today)
  │                ├─ recipeRepository.clear()          (no-op today)
  │                ├─ recipeListViewModel.resetState()
  │                ├─ shoppingListViewModel.resetState()
  │                └─ mealPlanViewModel.resetState()
  │                (each step in its own try/catch — best-effort)
  └─ updateState { status = Unauthenticated, loginError = null }
```

## Implementation Units

- [ ] **Unit 1: Add `clear()` to the caching repositories**

**Goal:** Give the four `Api*Repository` singletons a public `clear()`
method. The two caching repos drop their caches; the two non-caching repos
get no-op bodies.

**Requirements:** R1, R2

**Dependencies:** None

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/shopping/ShoppingListRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/catalog/CatalogItemRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/mealplan/MealPlanRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/RecipeRepository.kt`
- Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/shopping/ApiShoppingListRepositoryTest.kt` (new)
- Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/catalog/ApiCatalogItemRepositoryTest.kt` (new)

**Approach:**
- Add `clear()` **only to the concrete impls** — do not widen the
  `*Repository` interfaces. KDoc the contract: "Drops any in-memory
  cache; next call fetches from server."
- `ApiShoppingListRepository.clear()`: suspend, inside
  `mutex.withLock {}`, set `cachedLists = emptyList()`.
- `ApiCatalogItemRepository.clear()`: suspend, inside `mutex.withLock {}`,
  set `cachedItems = emptyList()`.
- `ApiMealPlanRepository.clear()`, `ApiRecipeRepository.clear()`:
  non-suspend no-ops today. KDoc explains they exist so
  `SessionCleaner` registers every user-scoped repo (future cache
  additions have a home).
- Test fakes (`FakeShoppingListRepository`, `FakeCatalogItemRepository`)
  do **not** need modification — they implement the interface, not the
  concrete class, and the cleaner takes concrete types.

**Patterns to follow:**
- Existing mutex-guarded mutations in each `Api*Repository`.

**Test scenarios:**
- Happy path (ApiShoppingListRepository): after `getLists()` populates
  the cache, `clear()` empties it; the next `getLists(refresh = false)`
  re-fetches from the client.
- Happy path (ApiCatalogItemRepository): after `search("milk")` populates
  `cachedItems`, `clear()` drops them; the next `search("milk")` triggers
  another `client.fetchAll()`.
- Edge case: calling `clear()` on an empty cache is a no-op (does not
  throw, does not call the client).
- The two no-op `clear()` methods (MealPlan / Recipe) compile and can be
  invoked without effect — no dedicated test needed.

**Verification:**
- All four concrete repos expose `clear()` at the concrete type level.
- Detekt passes (`./gradlew detektAll`).
- New repository tests pass.

---

- [ ] **Unit 2: Add `resetState()` to the three singleton ViewModels**

**Goal:** Give `RecipeListViewModel`, `ShoppingListViewModel`, and
`MealPlanViewModel` a public method that resets their state to the
initial value.

**Requirements:** R3

**Dependencies:** None

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/shopping/ShoppingListViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/mealplan/MealPlanViewModel.kt`
- Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListViewModelTest.kt` (extend)
- Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/shopping/ShoppingListViewModelTest.kt` (extend)
- Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/mealplan/MealPlanViewModelTest.kt` (new if absent)

**Approach:**
- Add a public `resetState()` (non-suspend) on each VM. Each calls
  `updateState { InitialState() }` with the VM's initial-state
  constructor.
- `MealPlanViewModel.resetState()` re-computes
  `currentWeekStart = mondayOfWeek(Clock.System.todayIn(TimeZone.currentSystemDefault()))`
  inside the reset so the reset doesn't pin the view to a stale week.
- `RecipeListViewModel` also resets `searchQueryFlow.value = ""`.
  Because `RecipeListViewModel` has `.drop(1)` on the flow, the *first*
  value is dropped — but this VM is a singleton and `init` already ran,
  so this assignment *does* emit through the debounce chain. That's
  acceptable: downstream it calls `fetchFirstPage()` which will 401
  (tokens already cleared by `SessionCleaner`'s earlier steps) and land
  in `onError`, which does **not** overwrite list fields.
- `ShoppingListViewModel` also resets `searchQueryFlow.value = ""`.
  **Note:** this VM has no `.drop(1)` barrier. The emitted empty string
  flows to `catalogItemRepository.search("")` which returns
  `emptyList()` via `ApiCatalogItemRepository`'s client-side blank-query
  guard (no network call). Document this in code.
- Do **not** null out the coroutine scope or cancel children. See Risks
  for the `launchOptimistic` race window.

**Patterns to follow:**
- Existing `updateState { copy(...) }` usage across MVI ViewModels.
- CLAUDE.md "Debounced MutableStateFlow inputs" — do not double-fire
  side-effects from synchronous handlers.
- Existing test pattern with `BaseViewModelTest` + `advanceUntilIdle()`.

**Test scenarios:**
- Happy path (each VM): drive the VM into a populated state (call
  `LoadRecipes`, `LoadLists`, `Load`), call `resetState()`, assert
  `state.value == InitialState()` (equality on the data class).
- `MealPlanViewModel.resetState()` sets `currentWeekStart` to the monday
  of the current week. Assert the invariant
  `state.currentWeekStart == mondayOfWeek(Clock.System.todayIn(TimeZone.currentSystemDefault()))`
  evaluated at test execution time — no clock injection needed.
- `RecipeListViewModel.resetState()` clears `searchQuery`,
  `selectedTag`, `randomRecipe`, and all loading flags.
- `ShoppingListViewModel.resetState()` clears `selectedListId`, `items`,
  `lists`, and `suggestions`.
- Edge case: calling `resetState()` on an already-initial VM is an
  observable no-op.

**Verification:**
- Each VM's test asserts the post-reset state equals a freshly
  constructed `XyzState()`.
- Detekt passes.

---

- [ ] **Unit 3: Introduce `SessionCleaner` and wire it into logout**

**Goal:** Implement the global `SessionCleaner` that owns the full
logout cleanup sequence, register it in a dedicated `sessionModule`, and
delegate `OidcAuthHandler.logout()` to it.

**Requirements:** R1, R2, R3, R6

**Dependencies:** Unit 1, Unit 2

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/SessionCleaner.kt`
- Create: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/SessionModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/OidcAuthHandler.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/AuthViewModel.kt`
- Modify: wherever Koin modules are loaded (e.g. `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/di/AppModules.kt`
  or `App.kt` — grep for `modules(...)` and add `sessionModule` to the list).
- Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/auth/SessionCleanerTest.kt` (new)
- Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/auth/FakeSessionCleaner.kt` (new — follows `FakeAuthHandler` pattern for other tests)

**Approach:**
- Define `SessionCleaner` as a **concrete class** (no interface) in the
  `ui.auth` package. Constructor takes `TokenStore`, `ApiClient`, the
  four concrete repository types, and the three concrete ViewModel
  types. Single method: `suspend fun clearAll()`.
- `clearAll()` runs the steps in order, each in its own try / catch so
  a single failure does not abort the rest:
  1. `tokenStore.removeTokens()` *(most critical — first)*
  2. `apiClient.httpClient.clearTokens()`
  3. `shoppingListRepository.clear()`
  4. `catalogItemRepository.clear()`
  5. `mealPlanRepository.clear()` (no-op today)
  6. `recipeRepository.clear()` (no-op today)
  7. `recipeListViewModel.resetState()`
  8. `shoppingListViewModel.resetState()`
  9. `mealPlanViewModel.resetState()`
  On catch: print the stack trace (no central logger exists yet) and
  continue.
- Shrink `OidcAuthHandler.logout()` to:
  `suspend fun logout() = sessionCleaner.clearAll()`. Remove the direct
  `tokenStore.removeTokens()` and `apiClient.httpClient.clearTokens()`
  calls — the cleaner owns them now.
- Modify `AuthViewModel.logout()`: emit
  `updateState { copy(user = null, profile = UserProfile()) }` **before**
  calling `authHandler.logout()`. The final
  `updateState { copy(status = Unauthenticated, loginError = null) }`
  remains after the suspend call.
- Create `SessionModule.kt` with a new `sessionModule` exposing
  `single { SessionCleaner(get(), get(), get(), get(), get(), get(), get(), get(), get()) }`.
  Register `sessionModule` wherever the app assembles its Koin modules
  (next to `authModule`, `shoppingModule`, etc.).
- Leave `OidcAuthHandler` in `authModule` — its constructor gains a
  `SessionCleaner` parameter, which Koin resolves from `sessionModule`.

**Patterns to follow:**
- Koin binding style — `single { ... }` with explicit `get()` list is
  fine here because `SessionCleaner` has nine dependencies and
  `singleOf(::SessionCleaner)` readability breaks down.
- Fake conventions for `FakeSessionCleaner` — throws
  `IllegalStateException` if `shouldFail`, `= Unit` for no-op overrides.

**Test scenarios:**
- Happy path: `SessionCleaner.clearAll()` invokes each step in the
  documented order. Verify with call-count counters and a captured
  sequence on spy / fake collaborators.
- Happy path: `tokenStore.removeTokens()` runs **before** any repository
  or ViewModel cleanup — asserts the critical-step-first ordering.
- Error path: when step N throws, steps N+1 through 9 still execute
  (best-effort). Verify via spies that record calls even when one
  throws.
- Error path: exceptions do not propagate out of `clearAll()` — assert
  no exception escapes the method.
- Integration: `OidcAuthHandler.logout()` calls `SessionCleaner.clearAll()`.
- Integration: `AuthViewModel.logout()` emits the identity-reset
  `updateState` *before* calling `authHandler.logout()`. Verify with a
  spy on the AuthHandler that records state snapshots at the moment of
  call.
- Edge case: calling `clearAll()` twice in a row is idempotent.

**Verification:**
- Koin graph resolves (`./gradlew :composeApp:allTests`).
- Detekt passes.
- Removing the best-effort try / catch from any step causes the
  error-path tests to fail (confirm locally).

---

- [ ] **Unit 4: End-to-end User-A-to-User-B regression test**

**Goal:** Prove R4 end-to-end: after A logs out and B logs in, no trace
of A's data appears in the three singleton-backed views, and the
loaded state contains only B's records.

**Requirements:** R4, R5

**Dependencies:** Unit 3

**Files:**
- Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/auth/LogoutStateResetTest.kt` (new)

**Approach:**
- Construct real instances of the three singleton VMs, the four repos,
  `TokenStore` (fake), `ApiClient` (fake), and a real `SessionCleaner`
  wiring them together.
- **Phase 1 — populate as User A:** seed the repository fakes with A's
  data and drive each VM to load it. Assert state is non-initial for
  each VM and caches are populated in the repos.
- **Phase 2 — logout:** call `sessionCleaner.clearAll()`. Assert tokens
  are gone, HTTP auth is cleared, repo caches are empty, all three VM
  states equal their respective initial states.
- **Phase 3 — login as User B:** seed the repository fakes with B's
  distinct data (different list IDs, different recipes, different meal
  plan items). Drive each VM to load. Assert the loaded state contains
  only B's records — no A artifacts present anywhere.

**Patterns to follow:**
- `BaseViewModelTest` + `runTest` as used in existing `*ViewModelTest`
  classes.
- Fakes already available in `composeApp/src/commonTest/kotlin/`.

**Test scenarios:**
- Happy path (phases 1 → 2 → 3): populate A, clear, load B, assert B
  only.
- Integration: the same flow through `AuthViewModel.logout()` (not
  direct `SessionCleaner.clearAll()`), to prove the wire-up survives
  future refactors. This is the authoritative regression test for R4.
- Error path: if `SessionCleaner` fails partway during Phase 2, Phase 3
  still shows only B's data (best-effort semantics hold end-to-end).

**Verification:**
- All tests green.
- Removing the `sessionCleaner.clearAll()` delegation from
  `OidcAuthHandler.logout()` causes the integration test to fail.

## System-Wide Impact

- **Interaction graph:** `SessionCleaner` is a new aggregator singleton
  depending on `TokenStore`, `ApiClient`, four repos, and three VMs.
  `OidcAuthHandler` depends on `SessionCleaner`. No new reverse deps —
  the feature packages remain unaware of auth. Lives in `sessionModule`
  (new top-level Koin module) to keep `authModule` imports clean.
- **Error propagation:** `SessionCleaner.clearAll()` is best-effort and
  never throws. A broken sub-step surfaces as a logged stack trace in
  debug builds; the logout flow completes regardless. `AuthViewModel`
  always reaches the `Unauthenticated` state transition after the
  suspend call returns.
- **State lifecycle risks:** In-flight coroutines in the singleton VMs
  are **not** cancelled. `launchOptimistic` rollbacks are the main race
  window — see Risks.
- **API surface parity:** No HTTP/API surface changes — client-side only.
- **Integration coverage:** Unit 4 proves the end-to-end flow; per-unit
  tests isolate each participant.
- **Unchanged invariants:**
  - `AuthHandler` interface signature (`tryAutoLogin`, `login`, `logout`)
    is unchanged — `OidcAuthHandler.logout()` still does the work, just
    via delegation.
  - `SettingsViewModel` is untouched.
  - Tab-state persistence across tab switches (#47) is preserved —
    `resetState()` only runs on logout, not tab nav.
  - The OIDC discovery cache on `OpenIdConnectClient` survives logout,
    as intended.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| In-flight VM coroutine completes after `resetState()` and repopulates state (rare race). | Tokens are removed first, so the call will 401. MVI `onError` handlers don't overwrite list data. If observed, escalate to cancelling VM child jobs — out of scope here. |
| `launchOptimistic` rollback restores a pre-logout snapshot. `launchOptimistic` captures `state.value` *before* the optimistic update. If a user triggers an optimistic delete/toggle, then logs out, and the request fails (likely — 401), the catch block does `state.update { snapshot }` — restoring A's data. | Accept as a known low-probability race. The window is small (between optimistic update and async failure). Follow-up: introduce a generation counter or scope cancellation. Add a `docs/solutions/` note if observed in the wild. |
| New user-scoped singleton added later and forgotten in `SessionCleaner`. | Acknowledged — Option A's documented con. KDoc on `SessionCleaner` says "register new user-scoped singletons here." Option B (Koin rebuild) becomes attractive if this list grows. |
| `searchQueryFlow` re-emits empty value on VM reset and triggers a side-effect. | Covered by Unit 2 tests. `RecipeListViewModel`'s path 401s and `onError` does not overwrite lists. `ShoppingListViewModel`'s path hits the blank-query guard and returns empty — no network call. |
| `SessionCleaner`'s best-effort catch swallows an exception that would have exposed a real bug. | Log stack traces in the catch block. Tests assert that steps complete and state is clean, not that no exception was ever thrown — a broken step still leaves evidence in the logs and visible test assertions. |
| `MealPlanViewModel.resetState()` reads the system clock; tests could be flaky across midnight. | Assert the invariant `currentWeekStart == mondayOfWeek(today)` evaluated at test time, not a hard-coded date. No clock injection needed. |

## Documentation / Operational Notes

- KDoc on `SessionCleaner` names the seam explicitly ("register new
  user-scoped singletons here"). No external docs change required.
- No rollout / migration / monitoring impact — client-side only.

## Sources & References

- Origin issue: fgrutsch/cookmaid#73
- Related PR: fgrutsch/cookmaid#77 (this pipeline)
- Related issue: fgrutsch/cookmaid#47 (paginated list state preservation
  — why the ViewModels are singletons)
- Document review findings: pipeline-local review of this plan dated
  2026-04-15.
- Relevant files: see Context & Research above.
