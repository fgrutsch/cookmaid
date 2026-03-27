---
title: Add PWA manifest to Compose for Web (WasmJS) app
category: ui-bugs
tags: [pwa, manifest, wasmjs, compose-web]
date: 2026-03-27
related_issue: 14
related_pr: 23
---

# Add PWA Manifest to Compose for Web (WasmJS) App

## Problem

Web app not installable — no PWA manifest present.

## Solution

1. Generate icon sizes from logo.png:
   ```bash
   magick logo.png -resize 192x192 icon-192.png
   magick logo.png -resize 512x512 icon-512.png
   ```

2. Create `manifest.json` in `composeApp/src/wasmJsMain/resources/`:
   ```json
   {
     "name": "cookmaid",
     "short_name": "cookmaid",
     "start_url": "/",
     "display": "standalone",
     "theme_color": "#2D3E50",
     "icons": [
       { "src": "/icon-192.png", "sizes": "192x192", "type": "image/png" },
       { "src": "/icon-512.png", "sizes": "512x512", "type": "image/png" }
     ]
   }
   ```

3. Add to `index.html` `<head>`:
   ```html
   <link rel="manifest" href="/manifest.json">
   <meta name="theme-color" content="#2D3E50">
   ```

## Key Insight

- 192x192 and 512x512 are the minimum icon sizes for Chrome/Edge
  installability. No intermediate sizes needed.
- Use absolute paths (`/manifest.json`, `/icon-*.png`) for
  client-side routing compatibility.
- Install prompt requires a service worker — manifest alone enables
  metadata but not full installability.
