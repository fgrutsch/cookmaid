---
title: Logto offline access and native client - Plan
type: fix
date: 2026-11-05
artifact_contract: ce-unified-plan/v1
product_contract_source: ce-plan-bootstrap
execution: code
---

# Logto offline access and native client - Plan

## Goal Capsule

- **Objective:** Cookmaid users are no longer forced to log in again every 14 days. Android stays logged in as long as it is used. Web stays logged in for 30 days after each login.
- **Means:** request `offline_access` with `prompt=consent` on every login (KTD1, KTD2), and give Android its own Logto Native client (KTD3).
- **Authority:** issue https://github.com/fgrutsch/cookmaid/issues/190, then this plan, then repo `CLAUDE.md` conventions.
- **Execution profile:** one PR. Code, build config, dev/demo seeds, README.
- **Stop conditions:** stop and ask if local verification shows a refresh still failing with `invalid_grant` after the Logto session is deleted, or if Logto shows an interactive consent screen on login.
- **Who finishes:** the implementer ships the PR. The project owner does the prod Logto and env steps under Operational Notes, which happen outside the repo.

---

## Product Contract

### Summary

Every login asks Logto for an offline refresh token that does not depend on the Logto login session. Android switches to a dedicated Native client whose rotated refresh tokens get a new lifetime on each refresh. Web keeps the existing SPA client. Local dev and the demo mirror the same split.

### Problem Frame

Since the move from PocketID to Logto, users get sent to the login screen roughly every 14 days, even when they use the app daily. The app requests `openid profile email` without `offline_access`. Logto only issues a refresh token because the SPA app has "always issue refresh token" on, and those tokens are bound to the 14-day Logto session, which token refreshes don't extend. When the session ends, the refresh fails with `invalid_grant`, `ApiClient` wipes the tokens, and the user lands on the login screen.

Adding `offline_access` alone only fixes Android partly. Logto caps rotated refresh tokens of public SPA clients at the original token's remaining lifetime, so SPA tokens always expire 30 days after login. Native clients get a new lifetime on every rotation. The issue's evidence section has the local measurements (Logto 1.43.0).

### Requirements

**Login**

- R1. Every login, on Android and web, requests the `offline_access` scope and sends `prompt=consent`.
- R2. Existing login parameters (`resource` on the auth URL and token exchange) keep working unchanged.

**Clients**

- R3. The Android app authenticates with its own Logto Native client in both flavors. Web keeps the existing SPA client.
- R4. The server stays unchanged. It validates the API audience, not the client ID.

**Local dev and demo**

- R5. `dev/seed_logto.sql` and `demo/seed_logto.sql` each seed a Native app with redirect `cookmaid://callback` and the Cookmaid API permission granted.
- R6. Following the README's local setup produces a working dev login on web and Android with offline access.

### Success Criteria

- With the Logto session deleted, a token refresh still succeeds on both clients (the issue's local verification, repeated after the change).
- After a refresh, the Native client's refresh-token expiry moves forward. The SPA client's expiry stays at login + 30 days.

### Scope Boundaries

- No token migration. Android users log in once after updating, because the old SPA-issued refresh token is not valid for the new client. Web users log in once more when their current session-bound token expires.
- No change to refresh or token-wipe handling in `ApiClient`.
- No server change (R4).
- Non-goal, considered and not built: failing the Gradle build when `oidc.androidClientId` is missing. The dev flavor's config also runs on CI and prod builds that have no `local.properties`, and the sibling `oidc.*` keys already behave the same way. Revisit if the Android dev setup moves to a task that only runs locally.

#### Deferred to Follow-Up Work

- Revert the interim Logto session TTL increase in prod once this has shipped and users have logged in again (optional, per the issue).

---

## Planning Contract

### Key Technical Decisions

- KTD1. **`offline_access` lives in the configured scope, not in code.** Scope already flows from config on every platform: the Android `OIDC_SCOPE` build field, the web `${OIDC_SCOPE}` placeholder substituted by `envsubst` or `injectLocalOidcConfig`, and `oidc.scope` in `local.properties`. Adding the value there keeps one source of truth per environment.
- KTD2. **`OidcAuthHandler.login()` appends `prompt=consent` to the authorization URL whenever the configured scope contains `offline_access`.** oidc-provider (which Logto is built on) ignores `offline_access` without `prompt=consent`, and Native clients don't have the SPA's "always issue refresh token" fallback. It is tied to the scope, not sent always, because Cookmaid is self-hosted against any OIDC provider (`docs/faq.md` names Keycloak), and an unconditional `prompt=consent` can make other providers show a consent screen on every login. Today `configureAuthUrl` is only passed when `resource` is set. It must apply in both branches.
- KTD3. **Separate Native client ID for Android, including dev.** The dev flavor reads a new `local.properties` key (`oidc.androidClientId`) instead of sharing `oidc.clientId` with web. The dev SPA app drops `cookmaid://callback` from its redirect and post-logout URIs, so dev matches prod. The prod flavor hardcodes the prod Native client ID like it does today.
- KTD4. **Native seed apps mirror the SPA app's token settings** (`rotateRefreshToken: true`, `refreshTokenTtlInDays: 30`, `grantTypes` authorization_code + refresh_token), with type `Native` and no CORS origins. With rotation on, every refresh pushes expiry out by 30 days, which gives the sliding expiry in the Objective.

### Sources

- Issue https://github.com/fgrutsch/cookmaid/issues/190: root cause, measurements, the Native vs SPA rationale.
- Logto docs: refresh tokens requested without `offline_access` are bound to the 14-day session, and "always issue refresh token" applies to web and SPA apps only ([application data structure](https://docs.logto.io/zh-CN/integrate-logto/application-data-structure), [applications](https://docs.logto.io/docs/references/applications)).
- `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/ApiClient.kt`: `onRefreshFailed = { tokenStore.removeTokens() }` is the wipe that sends users to the login screen.

---

## Implementation Units

### U1. Send prompt=consent with offline access on login

- **Goal:** every authorization request that asks for `offline_access` carries `prompt=consent`, with or without `resource` (KTD2).
- **Requirements:** R1, R2
- **Dependencies:** none
- **Files:**
  - `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/OidcAuthHandler.kt`
  - `app/shared/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/auth/OidcResourceExtensions.kt` (or a sibling file, if a helper is extracted)
  - `app/shared/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/auth/` (new test for the auth-URL parameters)
- **Approach:**
  1. Pass `configureAuthUrl` in both branches of `login()`. It appends `prompt=consent` when `oidcConfig.scope` contains `offline_access` as a space-separated token (KTD2), and appends `resource` when one is set.
  2. Leave `configureTokenExchange` as it is: resource only.
  3. To make this testable, extract the auth-URL parameter logic into a small top-level function next to `appendResourceToFormBody` and call it from `login()`. `OidcAuthHandler` has no tests today because `CodeAuthFlowFactory` needs a platform activity or browser.
- **Patterns to follow:** `appendResourceToFormBody` in `OidcResourceExtensions.kt` (top-level extension with KDoc).
- **Test scenarios:** this seam covers which parameters get added, but not that the oidc library actually sends them. U4's smoke test covers that.
  - Scope `openid profile email offline_access` with resource `http://localhost:8081/api`: the parameters contain `prompt=consent` and `resource=http://localhost:8081/api`.
  - Same scope with resource null: the parameters contain `prompt=consent` and no `resource`.
  - Scope `openid profile email`: no `prompt` parameter is added.
- **Verification:** the new tests pass, and a dev web login URL shows `prompt=consent`.

### U2. Android Native client and offline scope in build config

- **Goal:** both Android flavors use a Native client ID, and prod requests `offline_access`.
- **Requirements:** R1, R3
- **Dependencies:** none. The prod Native client ID must exist in prod Logto before merge (see Operational Notes).
- **Files:**
  - `app/androidApp/build.gradle.kts`
- **Approach:**
  1. Dev flavor: `OIDC_CLIENT_ID` reads `oidc.androidClientId` from `local.properties` (KTD3). It keeps reading `oidc.scope`, which gains `offline_access` through the README (U3).
  2. Prod flavor: `OIDC_CLIENT_ID` becomes the prod Native app's ID, and `OIDC_SCOPE` becomes `openid profile email offline_access` (KTD1).
- **Test expectation:** none, build config only. U4's smoke test covers it.
- **Verification:** `:app:androidApp:assembleDebug` builds, and the generated `BuildConfig` for each flavor carries the new values.

### U3. Seed Native apps for dev and demo, update README and demo scope

- **Goal:** a fresh local or demo Logto has a Native app for Android, and the documented local config requests offline access.
- **Requirements:** R5, R6, R1
- **Dependencies:** none
- **Files:**
  - `dev/seed_logto.sql`
  - `demo/seed_logto.sql`
  - `demo/docker-compose.yml`
  - `README.md`
- **Approach:**
  1. In each seed file, add a Native application (`cookmaid-dev-native` / `cookmaid-demo-native`, which fits Logto's varchar(21) IDs) with settings per KTD4, redirect and post-logout URIs `cookmaid://callback`, and a row in `application_user_consent_resource_scopes` granting the existing API scope. Keep `ON CONFLICT DO NOTHING`.
  2. In `dev/seed_logto.sql`, remove `cookmaid://callback` from the SPA app's redirect and post-logout URIs (KTD3).
  3. In `demo/docker-compose.yml`, set `OIDC_SCOPE` to `openid profile email offline_access`.
  4. In the README's local.properties block, add `offline_access` to `oidc.scope`, add `oidc.androidClientId=cookmaid-dev-native`, and note that the seed only runs on first Postgres start, so an existing dev database needs a reset to pick up the new app.
- **Patterns to follow:** the existing SPA app and consent-scope inserts in the same seed files.
- **Test expectation:** none, seed data and docs. U4 covers it.
- **Verification:** after a clean `dev` compose start, the Logto console lists both apps, and the Native app has the API permission.

### U4. Local end-to-end verification

- **Goal:** confirm the Success Criteria against local Logto before the PR merges.
- **Requirements:** R1, R3, R6
- **Dependencies:** U1, U2, U3
- **Files:** none
- **Execution note:** this is mostly config. Prove it with a runtime smoke test against local Logto, not more unit tests.
- **Approach:**
  1. Reset the dev stack so the new seed runs, and update `local.properties` per the README.
  2. Log in on web (SPA) and on the Android dev flavor (Native).
  3. Delete the user's Logto session (admin console or `oidc_model_instances`), then trigger an API call that forces a refresh.
- **Test scenarios:**
  - Covers the first success criterion: with the session deleted, the refresh succeeds and the user stays logged in on both clients.
  - The login shows no interactive consent screen.
  - Native client: the refresh-token expiry after a refresh is the refresh time + 30 days.
  - Native client: after a refresh, the Grant entry in `oidc_model_instances` has no fixed expiry that ends before the refresh token's. A fixed Grant cap would bring the forced re-login back.
- **Verification:** all four scenarios hold. Otherwise hit the Goal Capsule stop condition.

---

## Operational Notes

These prod steps happen outside the repo, in this order:

1. Create a Logto **Native** app in prod. Redirect and post-logout URI `cookmaid://callback`, refresh token rotation on, 30-day refresh-token TTL, and the Cookmaid API resource permission. Put its client ID into U2 before merge.
2. Add `offline_access` to the server container's `OIDC_SCOPE` env var at deploy. Web picks it up through `envsubst` at container start.
3. Release the Android build.
4. Once users have updated, remove `cookmaid://callback` from the prod SPA app's redirect and post-logout URIs. Until then, old Android builds still sign in through the SPA client.
5. Optional, later: revert the interim Logto session TTL increase (Deferred to Follow-Up Work).

Web prod keeps the session-bound behaviour until step 2, because `prompt=consent` is only sent once the scope contains `offline_access`.

Incident response changes. Offline refresh tokens survive the end of the Logto session, so deleting a session no longer cuts off a leaked token. To lock out a compromised account, revoke the user's grants or tokens in Logto, or suspend the user. Client logout clears only local tokens.

---

## Verification Contract

| Gate | Command / check |
|---|---|
| Shared tests | `mise exec -- ./gradlew :app:shared:allTests` |
| Android build | `mise exec -- ./gradlew :app:androidApp:assembleDebug` |
| Lint | `mise exec -- ./gradlew detektAll` |
| Runtime | U4 smoke test against local Logto |

## Definition of Done

- U1 to U3 merged in one PR, with the U1 tests passing and detekt clean.
- U4 scenarios verified locally.
- The README local setup matches the seeds (client IDs, scope).
- The prod Native client ID in `app/androidApp/build.gradle.kts` is the real prod app's ID, not a placeholder.
- No leftover code from abandoned attempts in the diff.
