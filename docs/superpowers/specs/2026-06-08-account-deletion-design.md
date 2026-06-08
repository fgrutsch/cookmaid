# Account Deletion — Design

Date: 2026-06-08
Status: Approved (brainstorm)

## Goal

Let users delete their Cookmaid account and all associated data, satisfying
Google Play's account-deletion requirement (in-app path + public web URL).
The central Logto identity is left intact; users are pointed to Logto's
Account Center to delete it separately.

## Decisions

- **Logto identity: leave intact.** The server deletes only the Cookmaid
  `users` row (which cascades all app data) and logs the user out. The Logto
  login survives — it is the identity provider (same model as "Sign in with
  Google"), ready for a future multi-app setup. No server-side Logto
  Management API client is added.
- **Web URL: authed deeplink** `/delete-account` into the WasmJS app, running
  the real deletion flow in-browser. Submitted to Play Console as the
  "request deletion" URL.
- **Confirmation: dedicated screen + confirm dialog.** No type-to-confirm.
- **Compliance posture:** deleting the `users` row + all app data deletes the
  Cookmaid "app account". The flow also surfaces the Logto Account Center link
  so the user can delete the upstream identity themselves. This is disclosed
  in the Play Data Safety form. Partial deletion / disclosed retention is
  explicitly permitted by Google.

## Server: `DELETE /api/users/me`

- **Route** (`server/.../user/UserModule.kt`): `delete("/me")` → resolve
  `call.userId()` (existing extension; throws → 404 if not registered) →
  `service.delete(userId, oidcSubject)` → respond **204 No Content**.
  `oidcSubject` comes from the JWT principal (as in `post("/me")`).
- **Service** (`UserService.delete`): run in a `suspendTransaction`, call
  `repository.delete(userId)`; then **evict the cache entry** for that
  `oidcSubject` (the in-memory `ConcurrentHashMap` would otherwise serve a
  stale `UserId` for up to the 1-minute TTL); log at `info` with `userId`.
- **Repository** (`UserRepository.delete` + `PostgresUserRepository`):
  `UsersTable.deleteWhere { id eq userId.value }`.
- **Cascades already exist.** `recipes`, `meal_plan_items`, `shopping_lists`
  (+ their children) all declare `REFERENCES users(id) ON DELETE CASCADE`, so
  one row delete wipes all app data atomically. `catalog_items` are shared and
  correctly retained. **No new migration required.**

## Client UI (shared module → Android + Web)

- **`Route.DeleteAccount`** (`navigation/Route.kt`) as a `data object`,
  registered in `navConfig`'s polymorphic block (`navigation/NavConfig.kt`).
- **`UserClient.deleteAccount()`** (`ui/user/UserClient.kt`):
  `apiClient.httpClient.delete("/api/users/me")` — mirrors `getOrCreateUser()`.
- **`DeleteAccountViewModel`** (MVI, new): `State(deleting, deleted, error)`;
  `confirm()` → `launch { userClient.deleteAccount() }` → on success
  `deleted = true`; catch sets `error` (and does **not** mark deleted).
  Injected with `UserClient` (from `appModules` — survives logout).
  **Scope:** created via `remember { }` in the nav entry (like the `AddRecipe`
  VM), **not** in `sessionModules` — logout tears those down and would GC the
  VM mid-flow.
- **`DeleteAccountScreen`** + contract (`ui/deleteaccount/`):
  - Default state: explains what gets deleted; destructive
    "Delete my account" button → **confirm dialog**.
  - `state.deleted`: success state showing "Account deleted" copy + the Logto
    Account Center link (`accountUri`, opened via `LocalUriHandler`) + a
    **Finish** button.
  - `state.error`: inline error; user stays authenticated, can retry.
- **Settings entry** (`ui/settings/SettingsScreen.kt`): a destructive
  "Delete account" item → `backStack.add(Route.DeleteAccount)`. Reuses the
  same `accountUri` already plumbed into Settings.
- **`App.kt` nav entry**: `entry<Route.DeleteAccount>` constructs the VM via
  `remember { }`, passes `accountUri`, and wires `onFinish` → the existing
  logout callback.

## Logout ordering (critical)

The DELETE call requires a **valid bearer token**, so the session must not be
torn down first:

1. `confirm()` → server `DELETE /api/users/me` while still authenticated.
2. Success → screen shows the confirmation state. Tokens remain intact; the
   screen makes no further API calls (a stray call would 401/404).
3. **Finish** → existing `AuthViewModel.logout()` clears tokens, flips
   `status = Unauthenticated` + `user = null` synchronously, session modules
   unload, app returns to the login screen.

`DeleteAccountViewModel` never touches `AuthState`; it signals completion via
an `onFinish` callback wired in `App.kt`. Session teardown stays centralized in
`AuthViewModel`, per the project's logout discipline.

## Web deeplink + login survival (web-only)

- **`main.kt`** (`app/webApp/.../main.kt`): the entry already branches on
  `window.location.pathname` for `/callback`. In the non-callback branch, if
  `pathname == "/delete-account"`, write
  `localStorage["cookmaid.deeplink"] = "delete-account"` **before**
  `ComposeViewport { App(...) }`. The server already serves the app at this
  path via the static `default("index.html")` SPA fallback.
- **`App.kt`**: once `AuthState` becomes `Authenticated`, if the stash key is
  present, clear it and `backStack.add(Route.DeleteAccount)`. Handles both
  already-authenticated arrival (seed immediately) and logged-out arrival
  (stash survives the OIDC round-trip, seeded after `/callback`).
- Stashing *before* any redirect makes this robust regardless of whether the
  `multiplatform-oidc` web flow is popup- or full-redirect-based. Confirm which
  during implementation — a popup preserves the path anyway, making the stash
  belt-and-suspenders.
- **Android** needs no app-link; the Settings entry is its in-app path. Play
  only requires the *web* URL.

## Error handling

- **Client**: `launch {}` catch logs at `error`, rethrows
  `CancellationException` first, then sets `state.error`. Failure leaves the
  user authenticated — no logout, retry allowed.
- **Server**: 204 success; 404 if unregistered (via `call.userId()` →
  `UserNotRegisteredException`); 500 from the StatusPages catch-all. Re-deleting
  an already-deleted account → 404 (acceptable).

## Testing

- **Server**
  - `PostgresUserRepositoryTest`: insert a user + recipe/meal-plan/shopping
    rows, delete the user, assert all child rows are gone (cascade) **and** a
    second user's data is untouched (isolation).
  - `UserServiceTest`: delete calls the repo and evicts the cache entry.
  - `UserRoutesTest`: `DELETE /api/users/me` → 204; unregistered subject → 404.
- **Shared**
  - `DeleteAccountViewModelTest`: `confirm()` → `userClient.deleteAccount`
    called → `deleted = true`; failure path sets `error` and leaves
    `deleted = false`. Fake `UserClient` throws `IllegalStateException`.

## Non-code follow-ups (owner)

- Play Console Data Safety: submit `https://<host>/delete-account` as the
  account-deletion URL; declare account + associated data deletion; disclose
  that the central Logto login is deleted separately via Account Center.
- Update `docs/faq.md` with an account-deletion section.

## Explicitly out of scope (YAGNI)

- Type-to-confirm gating.
- Email/manual-request fallback (the authed web deeplink satisfies the policy).
- Server-side Logto Management API client / programmatic identity deletion.
- Android app-link / intent filter for the deeplink.
