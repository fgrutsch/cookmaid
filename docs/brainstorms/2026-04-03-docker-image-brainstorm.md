---
date: 2026-04-03
topic: docker-image
---

# Docker Image

## What We're Building

A single, lightweight Docker image for cookmaid that bundles the Ktor backend
server (JVM) together with the WasmJS frontend (static files). The server serves
both the REST API and the static web assets. A startup script handles OIDC
environment variable substitution in `index.html` at container boot.

A new GitHub Actions `docker` job builds (and optionally pushes) the image on
every push to `main` and on pull requests.

## Why This Approach

Two approaches were considered:

**A — Ktor serves everything** (chosen): Multi-stage Dockerfile builds the Gradle
distribution and WasmJS assets, then copies them into a `eclipse-temurin:21-jre-alpine`
runtime image. Ktor adds `staticFiles` routing to serve the WasmJS output dir.
A shell entrypoint does `envsubst` on `index.html` before launching the JVM.
Single process, no nginx, smallest possible footprint.

**B — Nginx + Ktor**: Keep nginx as the frontend server (using the existing
`docker-entrypoint.sh`) and run Ktor as a sidecar. Requires supervisord or a
process manager, a heavier base image, and more configuration. Rejected because
the issue asks for a very lightweight image and Ktor can serve static files
natively.

## Key Decisions

- **Base image**: `eclipse-temurin:21-jre-alpine` — small, official, JRE-only.
- **Build stage**: `gradle:8-jdk21-alpine` using `installDist` to produce an
  unzipped application distribution at `server/build/install/server/`.
- **WasmJS task**: `:composeApp:wasmJsBrowserProductionWebpack` — output lands at
  `composeApp/build/dist/wasmJs/productionExecutable/`.
- **Static files path in container**: `/app/web/` — served by Ktor via
  `staticFiles("/", File("/app/web"))` with `index.html` as the default.
- **OIDC substitution**: Shell entrypoint (`server/docker/docker-entrypoint.sh`)
  runs `envsubst` on `/app/web/index.html` before `exec "$@"`.
- **Ktor static content**: Add `ktor-server-partial-content` and configure
  `staticFiles` in the server's Application.kt (or a dedicated module).
- **GitHub Actions**: New `docker` job on `main` push builds the image and
  pushes to GHCR (`ghcr.io/${{ github.repository }}:latest`). PR runs build-only
  (no push) to verify correctness.

## Open Questions

- Should the image be pushed to GHCR on every `main` push, or only on tagged
  releases? (Assumption: push on every `main` push for now; can be refined.)
- Should the `docker` job depend on `server` and `web` CI jobs passing first?

## Next Steps

→ `docs/plans/` for implementation details
