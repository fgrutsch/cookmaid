---
title: Add a Favicon
type: feat
status: active
date: 2026-03-27
---

# Add a Favicon

Add a favicon to the Cookmaid web application (WasmJS target) so browsers
display a site icon in tabs, bookmarks, and history.

## Acceptance Criteria

- [ ] Favicon visible in browser tab when running the web app
- [ ] Uses the existing Cookmaid logo as source
- [ ] `<link rel="icon">` tag added to `index.html`

## Context

- Web entry point: `composeApp/src/wasmJsMain/resources/index.html`
- Static resources dir: `composeApp/src/wasmJsMain/resources/`
- Existing logo: `composeApp/src/commonMain/composeResources/drawable/logo.png`
- No Gradle/webpack changes needed — `wasmJsProcessResources` copies
  everything from the resources directory to build output

## MVP

### 1. Create favicon file

Convert the existing `logo.png` to a `favicon.ico` (or use a PNG favicon).
Place it in `composeApp/src/wasmJsMain/resources/favicon.ico`.

### 2. Update index.html

Add to `<head>` in `composeApp/src/wasmJsMain/resources/index.html`:

```html
<link rel="icon" type="image/x-icon" href="favicon.ico">
```

## Sources

- Related issue: #13
