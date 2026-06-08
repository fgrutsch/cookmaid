# Account Deletion — Design

Date: 2026-06-08
Status: Approved (brainstorm)

## Goal

Let users delete their Cookmaid account and all associated data, satisfying
Google Play's account-deletion requirement (in-app path + public web URL).

Scope is deliberately **data-only**: deletion removes everything Cookmaid
owns (the `users` row + all cascaded data). The identity provider (Logto, or
any OIDC provider) is **not** touched. This keeps the solution generic for an
open-source project — it works with any OIDC provider and requires no
provider management credentials.

## Decisions

- **Data-only deletion.** `DELETE /api/users/me` deletes the Cookmaid `users`
  row, which cascades all app data. The OIDC identity is left intact. This
  alone satisfies "delete the app account + associated data" for any
  deployment, regardless of identity provider.
- **No identity-provider deletion, no abstraction.** We do not call any IdP
  management API and do not introduce an `IdentityDeleter`-style interface now
  — a single-implementation abstraction would be premature. The seam can be
  added later if identity deletion is ever built (see Future).
- **Web URL: authed deeplink** `/delete-account` into the WasmJS app, running
  the real deletion flow in-browser. Submitted to Play Console as the
  "request deletion" URL.
- **Confirmation: dedicated screen + confirm dialog.** No type-to-confirm.
- **Compliance posture:** deleting the `users` row + all app data deletes the
  Cookmaid "app account". Disclosed in the Play Data Safety form. Partial
  deletion / disclosed retention is explicitly permitted by Google. The shared
  OIDC identity is a login mechanism (akin to "Sign in with Google") and is
  out of scope for this flow.

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
  - Default state: explains what gets deleted (scope the copy to the Cookmaid
    account/data); destructive "Delete my account" button → **confirm dialog**.
  - `state.deleted`: success state ("Account deleted") + a **Finish** button.
  - `state.error`: inline error; user stays authenticated, can retry.
- **Settings entry** (`ui/settings/SettingsScreen.kt`): a destructive
  "Delete account" item → `backStack.add(Route.DeleteAccount)`.
- **`App.kt` nav entry**: `entry<Route.DeleteAccount>` constructs the VM via
  `remember { }` and wires `onFinish` → the existing logout callback.

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
  account-deletion URL; declare account + associated data deletion.
- Update `docs/faq.md` with an account-deletion section.

## Future (documented, not built)

These are deliberately out of scope now and recorded so the decision is
captured, not implemented:

- **Identity-provider deletion.** If a deployment ever wants to also delete the
  user at the IdP, introduce a small provider seam at that point (e.g. an
  `IdentityDeleter` interface with a Logto implementation using the Management
  API, selected by config; default no-op). Do not build it speculatively.
- **Multi-app "delete everything" flow.** If Cookmaid's identity is ever shared
  across multiple apps, do **not** reference-count from individual apps. Follow
  the industry pattern (FusionAuth/Twitter/Atlassian): a central account
  deletion flow that fans out to each app via the IdP's `user.deleted` webhook,
  where each app purges its own data. `UserService.delete` becomes the purge
  handler that webhook calls — no rework of the core endpoint.
- **Per-app data deletion ("keep my other apps")** needs nothing extra: each
  app's `DELETE /api/users/me` already deletes only that app's data.

## Explicitly out of scope (YAGNI)

- IdP / Logto identity deletion, Management API, M2M client.
- `IdentityDeleter` (or any) provider abstraction — single-impl premature.
- Organization / role-based reference counting.
- Type-to-confirm gating.
- Email/manual-request fallback (the authed web deeplink satisfies the policy).
- Soft-delete grace period; re-authentication before delete.
- Android app-link / intent filter for the deeplink.
