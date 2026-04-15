---
title: "fix: Disable android:allowBackup to exclude OIDC tokens from device backups"
type: fix
status: active
date: 2026-04-15
---

# fix: Disable android:allowBackup to exclude OIDC tokens from device backups

## Overview

`android:allowBackup="true"` allows the DataStore-backed OIDC token store to
be exfiltrated via `adb backup` (Android < 12) or restored onto a different
device from Google Drive backup, enabling account takeover via long-lived
refresh tokens. Disable backup entirely (issue's Option A — explicitly
requested).

## Requirements Trace

- R1. Token DataStore files are not included in device backups.
- R2. App builds and login flow works after the change.
- R3. No user-visible behavior change on fresh install (tokens re-acquired via OIDC login).

## Scope Boundaries

- Not introducing backup rules XML (Option B). The user selected Option A.
- Not touching the DataStore token storage code.

## Key Technical Decisions

- **Disable backup entirely** (`allowBackup="false"`). Tokens are re-acquired by
  OIDC login on fresh install; no user data is lost. Simpler than maintaining
  two separate backup rules files (pre-/post-Android-12).

## Implementation Units

- [ ] **Unit 1: Flip allowBackup to false**

**Goal:** Exclude app data (including tokens) from Android device backups.

**Requirements:** R1, R2, R3

**Files:**
- Modify: `androidApp/src/main/AndroidManifest.xml`

**Approach:**
- Change `android:allowBackup="true"` to `android:allowBackup="false"` on the
  `<application>` element (line 5).

**Test scenarios:**
- Test expectation: none — single-attribute XML change, no behavioral code.

**Verification:**
- `./gradlew :androidApp:assembleDebug` builds successfully.
- `./gradlew detektAll` passes (unlikely to apply to XML but confirms no regression).

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Users lose app preferences on device restore | Acceptable — tokens are the security-sensitive payload; app state is otherwise minimal and restored from server on login. |

## Sources & References

- Issue: #52
- PR: #74
