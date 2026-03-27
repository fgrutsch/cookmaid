---
title: Add PWA Manifest
type: feat
status: active
date: 2026-03-27
---

# Add PWA Manifest

Add a `manifest.json` for Progressive Web App support so the web app
can be installed on devices and shows proper branding in app switchers.

## Acceptance Criteria

- [ ] `manifest.json` in web resources with correct metadata
- [ ] Multiple icon sizes generated from logo.png
- [ ] `<link rel="manifest">` tag in `index.html`
- [ ] `<meta name="theme-color">` tag in `index.html`
- [ ] App installable from browser (shows install prompt)

## Context

- Static resources: `composeApp/src/wasmJsMain/resources/`
- App name: "cookmaid"
- Description: "An app for managing shopping lists, recipes and meal planning"
- Primary color: #2D3E50
- Source logo: `composeApp/src/commonMain/composeResources/drawable/logo.png` (720x720)
- Existing favicon: `favicon.png` (128x128 "CM" letter icon)

## MVP

### 1. Generate icon set from logo.png

Sizes needed: 192x192, 512x512 (minimum for PWA installability).

### 2. Create manifest.json

Standard PWA manifest with name, icons, colors, display mode.

### 3. Update index.html

Add manifest link and theme-color meta tag.

## Sources

- Related issue: #14
