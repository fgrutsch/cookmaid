---
title: "Android allowBackup exposes OIDC tokens stored in DataStore"
date: 2026-04-15
problem_type: security-issue
track: bug
category: security-issues
module: androidApp
component: AndroidManifest
tags:
  - android
  - security
  - oidc
  - backup
---

# Android allowBackup exposes OIDC tokens stored in DataStore

## Problem

With `android:allowBackup="true"` (Android's default) and no backup exclusion
rules, the DataStore-backed OIDC token store is included in device backups.
Access/refresh tokens can be extracted via `adb backup` (Android <12) or
restored onto a different device from Google Drive, enabling account takeover
without re-authentication.

## Solution

Set `android:allowBackup="false"` on the `<application>` element in
`androidApp/src/main/AndroidManifest.xml`. Tokens are re-acquired via OIDC
login on fresh install; no user data is lost.

```xml
<application
    android:allowBackup="false"
    ...>
```

## Why This Works

Disabling backup excludes all app-private storage (DataStore, SharedPreferences,
databases, files) from both `adb backup` and Auto Backup to Google Drive.
Because the app's only durable local state is tokens and cache, losing backup
is invisible to users — next launch re-authenticates.

The alternative (Option B) is `android:fullBackupContent` + `android:dataExtractionRules`
with explicit exclusions, but it requires two XML files (pre-/post-Android-12)
and it's easy to miss a future credential-storage location.

## Prevention

- Android apps that store any authentication material (tokens, session IDs,
  biometric keys) default to `allowBackup="false"` unless there's a clear
  reason to enable it.
- When enabling backup is required, pair it with `dataExtractionRules` (API 31+)
  AND `fullBackupContent` (API <31) and explicitly exclude the credential store.

## Related

- Issue: #52
- PR: #74
