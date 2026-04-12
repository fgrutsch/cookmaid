# JWT Audience Validation

Date: 2026-04-12
Issue: #40

## What We're Building

Add JWT audience (`aud`) claim validation to the Ktor auth verifier
so tokens issued for other Pocket-ID clients are rejected.

Currently `AuthModule.kt` validates issuer + signature + subject
presence but never checks audience. Any valid token from the same
Pocket-ID instance — even one issued for a different app — is
accepted. This is a textbook audience confusion vulnerability
(CWE-287).

## Why This Approach

Single, minimal change: add `withAudience(oidcClientId)` to the
existing verifier block. The client ID is already available as an
env var (`OIDC_CLIENT_ID`) — it just needs to be threaded into
the server config.

- No new dependencies
- No architectural changes
- Follows existing pattern for `oidc.issuer` / `oidc.jwks-url`

## Key Decisions

- **Config key**: `oidc.client-id` in `application.yaml`, mapped
  from `OIDC_CLIENT_ID` env var — consistent with existing OIDC
  config naming
- **Single audience value**: use `withAudience(oidcClientId)` (not
  a list) since only one client ID is relevant
- **Test update**: `TestJwt.generateToken` must include the `aud`
  claim so existing integration tests keep passing
- **No negative test**: a dedicated "wrong audience rejected" test
  is nice-to-have but the auth0 library's audience check is
  well-tested — keep scope minimal

## Open Questions

None — the issue provides clear acceptance criteria and the fix
is straightforward.
