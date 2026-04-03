---
title: "feat: Build Docker image"
type: feat
status: active
date: 2026-04-03
origin: docs/brainstorms/2026-04-03-docker-image-brainstorm.md
---

# feat: Build Docker image

## Overview

Package cookmaid into a single, lightweight Docker image. The Ktor server serves
both the REST API (`/api/*`) and the WasmJS frontend as static files. A shell
entrypoint substitutes OIDC environment variables into `index.html` at container
startup. GitHub Actions gets a `docker` job to verify and publish the image.

## Proposed Solution

Multi-stage Dockerfile:

1. **Build stage** (`gradle:8-jdk21-alpine`): run `installDist` + `wasmJsBrowserProductionWebpack`
2. **Runtime stage** (`eclipse-temurin:21-jre-alpine`): copy server distribution
   and WasmJS output, install `envsubst` (gettext), add entrypoint script

Ktor's built-in static file routing (part of `ktor-server-core-jvm`) serves the
WasmJS assets from `/app/web/`. The entrypoint script mutates `index.html` with
runtime OIDC values before `exec`-ing the server start script.

(see brainstorm: docs/brainstorms/2026-04-03-docker-image-brainstorm.md)

## Technical Considerations

- `staticFiles` in Ktor 3.x is provided by `ktor-server-core-jvm` — no new
  dependency needed.
- `wasmJsProcessResources` expands OIDC values from `local.properties` at build
  time. In Docker builds `local.properties` is absent so all three vars expand to
  empty strings; `envsubst` fills them at runtime.
- The Gradle `application` plugin's `installDist` task produces
  `server/build/install/server/{bin/,lib/}` — the clearest artifact to copy into
  the image (no fat-JAR plugin required).
- `eclipse-temurin:21-jre-alpine` ships without `envsubst`; install `gettext`
  (Alpine package) in the runtime stage.
- Static file routing must be registered **before** the `/api` route so the catch-all
  doesn't shadow API paths. Order matters in Ktor routing.
- The existing `composeApp/docker/docker-entrypoint.sh` targets nginx paths; a new
  script at `server/docker/docker-entrypoint.sh` is needed for the Ktor image.

## Acceptance Criteria

- [ ] `docker build -t cookmaid .` succeeds from the project root
- [ ] Container starts and `GET /` returns the WasmJS `index.html`
- [ ] `OIDC_DISCOVERY_URI`, `OIDC_CLIENT_ID`, `OIDC_SCOPE` are substituted in
      `index.html` at container startup
- [ ] `GET /api/*` routes are still reachable (not served as static files)
- [ ] GitHub Actions `docker` job builds the image on every push and PR
- [ ] Image is pushed to GHCR only on pushes to `main`

## Implementation Steps

### 1. Add Ktor static file routing — `Application.kt`

In `configureRouting()`, add a `staticFiles` block **before** the `/api` route:

```kotlin
// server/src/main/kotlin/io/github/fgrutsch/cookmaid/Application.kt
private fun Application.configureRouting() {
    val webDir = System.getenv("WEB_DIR") ?: "web"
    routing {
        staticFiles("/", File(webDir)) {
            default("index.html")
        }
        route("/api") {
            authenticate(AUTH_JWT) {
                userRoutes()
                catalogRoutes()
                shoppingRoutes()
                recipeRoutes()
                mealPlanRoutes()
            }
        }
    }
}
```

`WEB_DIR` defaults to `web` for local dev; Docker sets it to `/app/web`.

### 2. Add `java.io.File` import to `Application.kt`

```kotlin
import java.io.File
```

### 3. Create `server/docker/docker-entrypoint.sh`

```sh
#!/bin/sh
set -e

envsubst '$OIDC_DISCOVERY_URI $OIDC_CLIENT_ID $OIDC_SCOPE' \
    < /app/web/index.html \
    > /app/web/index.html.tmp \
    && mv /app/web/index.html.tmp /app/web/index.html

exec "$@"
```

### 4. Create `Dockerfile` at project root

```dockerfile
# syntax=docker/dockerfile:1

# ── Build stage ────────────────────────────────────────────────────────────────
FROM gradle:8-jdk21-alpine AS build
WORKDIR /workspace
COPY . .
RUN gradle :server:installDist :composeApp:wasmJsBrowserProductionWebpack \
    --no-daemon --parallel --configuration-cache

# ── Runtime stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache gettext

WORKDIR /app
COPY --from=build /workspace/server/build/install/server/ .
COPY --from=build \
    /workspace/composeApp/build/dist/wasmJs/productionExecutable/ \
    /app/web/
COPY server/docker/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENV WEB_DIR=/app/web
EXPOSE 8081

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/app/bin/server"]
```

### 5. Add `docker` job to `.github/workflows/ci.yml`

```yaml
docker:
  name: Docker
  runs-on: ubuntu-latest
  permissions:
    contents: read
    packages: write
  steps:
    - uses: actions/checkout@v5
    - uses: docker/setup-buildx-action@v3
    - uses: docker/login-action@v3
      if: github.ref == 'refs/heads/main'
      with:
        registry: ghcr.io
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    - uses: docker/build-push-action@v6
      with:
        context: .
        push: ${{ github.ref == 'refs/heads/main' }}
        tags: ghcr.io/${{ github.repository }}:latest
        cache-from: type=gha
        cache-to: type=gha,mode=max
```

## Dependencies & Risks

- **Gradle build cache in Docker**: `--configuration-cache` may need cache-busting
  if Kotlin/Wasm config changes; monitor first run times.
- **WasmJS task name**: confirm `wasmJsBrowserProductionWebpack` is the correct
  production task name (vs `wasmJsBrowserDevelopmentRun`).
- **Static routing order**: if `/api` is registered before `staticFiles`, API
  routes still work but the static catch-all may not match `/`. Keep `staticFiles`
  first.
- **`WEB_DIR` in tests**: existing server integration tests don't serve static
  files; they will skip the `staticFiles` block because the directory doesn't
  exist. Ktor silently ignores missing directories — verify this is safe.

## Sources & References

- **Origin brainstorm:** [docs/brainstorms/2026-04-03-docker-image-brainstorm.md](../brainstorms/2026-04-03-docker-image-brainstorm.md)
  — key decisions: single image, Ktor serves static files, `eclipse-temurin:21-jre-alpine`,
  `envsubst` entrypoint, GHCR push on main only
- Server entry point: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/Application.kt`
- Server config: `server/src/main/resources/application.yaml`
- Existing entrypoint pattern: `composeApp/docker/docker-entrypoint.sh`
- CI workflow: `.github/workflows/ci.yml`
