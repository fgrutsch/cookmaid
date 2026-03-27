---
title: Add favicon to Compose for Web (WasmJS) app
category: ui-bugs
tags: [favicon, wasmjs, compose-web, static-assets]
date: 2026-03-27
related_issue: 13
related_pr: 22
---

# Add Favicon to Compose for Web (WasmJS) App

## Problem

Browser tab shows generic icon — no favicon configured for the web app.

## Solution

1. Convert existing logo to multi-size `favicon.ico`:
   ```bash
   magick logo.png -define icon:auto-resize=48,32,16 favicon.ico
   ```

2. Place in `composeApp/src/wasmJsMain/resources/favicon.ico`

3. Add link tag to `composeApp/src/wasmJsMain/resources/index.html`:
   ```html
   <link rel="icon" type="image/x-icon" href="favicon.ico">
   ```

## Key Insight

No Gradle or webpack changes needed. The `wasmJsProcessResources` task
automatically copies everything from `src/wasmJsMain/resources/` to
the build output, and the webpack dev server serves it from there.

## Prevention

When adding static web assets to the WasmJS target, just drop them in
`composeApp/src/wasmJsMain/resources/` — they'll be served automatically.
