---
title: Docker image build for Cookmaid (Ktor + WasmJS)
problem_type: feature
component: infrastructure
symptoms:
  - no containerized deployment available
  - WasmJS frontend assets not served by Ktor backend
  - OIDC environment variables not injected at container startup
technologies:
  - Docker
  - Kotlin Multiplatform
  - Ktor
  - WasmJS
  - Gradle
  - GitHub Actions
  - envsubst
  - eclipse-temurin
tags:
  - docker
  - containerization
  - ktor
  - wasmjs
  - static-files
  - ci-cd
  - github-actions
  - oidc
date: 2026-04-03
---

# Docker image for cookmaid (Ktor + WasmJS)

Single container image where Ktor serves both the REST API and the WasmJS
frontend as static files. OIDC configuration is injected into `index.html` via
`envsubst` at container startup.

## Architecture

Multi-stage build:
- **Build stage** (`gradle:8-jdk21-alpine`): runs `:server:installDist` and
  `:composeApp:wasmJsBrowserProductionWebpack`
- **Runtime stage** (`eclipse-temurin:21-jre-alpine`): minimal JRE + `gettext`
  package (provides `envsubst`), non-root user

## Solution

### Dockerfile

```dockerfile
# syntax=docker/dockerfile:1

FROM gradle:8-jdk21-alpine AS build
WORKDIR /workspace
COPY . .
RUN gradle :server:installDist :composeApp:wasmJsBrowserProductionWebpack \
    --no-daemon --parallel

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache gettext && \
    addgroup -S cookmaid && adduser -S cookmaid -G cookmaid

WORKDIR /app
COPY --from=build /workspace/server/build/install/server/ .
COPY --from=build \
    /workspace/composeApp/build/dist/wasmJs/productionExecutable/ \
    /app/web/
COPY --from=build /workspace/server/docker/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh && chown -R cookmaid:cookmaid /app

USER cookmaid

ENV WEB_DIR=/app/web
EXPOSE 8081

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/app/bin/server"]
```

### Entrypoint script — `server/docker/docker-entrypoint.sh`

```sh
#!/bin/sh
set -e

# Substitute OIDC environment variables into index.html at container startup.
# The production WasmJS build leaves these as empty strings; this fills them in.
envsubst '$OIDC_DISCOVERY_URI $OIDC_CLIENT_ID $OIDC_SCOPE' \
    < /app/web/index.html \
    > /app/web/index.html.tmp \
    && mv /app/web/index.html.tmp /app/web/index.html

exec "$@"
```

**Critical:** The explicit variable list in `envsubst` prevents accidentally
substituting `$`-patterns inside the compiled Kotlin/Wasm JS output.

### Ktor static file routing — `Application.kt`

Add before the `/api` route (order matters — `staticFiles` must come first):

```kotlin
import io.ktor.server.http.content.*
import java.io.File

private fun Application.configureRouting() {
    val webDir = System.getenv("WEB_DIR") ?: "web"
    routing {
        staticFiles("/", File(webDir)) {
            default("index.html")  // SPA fallback
        }
        route("/api") {
            authenticate(AUTH_JWT) { /* ... */ }
        }
    }
}
```

`WEB_DIR` defaults to `"web"` for local dev (directory absent → Ktor silently
serves nothing, API routes still work). Docker sets it to `/app/web`.

### `.dockerignore`

```
.git
.github
.gradle
.idea
*.iml
local.properties
sandbox/
docs/
androidApp/build/
composeApp/build/
server/build/
shared/build/
build/
```

Excludes `.git` (can add tens of MB), Android sources (not needed in the server
image), and all Gradle build outputs (rebuilt inside the container).

### GitHub Actions job

```yaml
docker:
  name: Docker
  runs-on: ubuntu-latest
  needs: [detekt, server, shared, android, web]   # only build after all checks pass
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
        tags: |
          ghcr.io/${{ github.repository }}:latest
          ghcr.io/${{ github.repository }}:${{ github.sha }}
        cache-from: type=gha
        cache-to: type=gha,mode=max
```

## Key Decisions

- **Ktor serves everything** — eliminates nginx; `ktor-server-core-jvm` includes
  `staticFiles` with no extra dependency
- **`installDist` over fat JAR** — Gradle `application` plugin produces a clean
  distribution (`bin/` + `lib/`) with no additional plugins required
- **`eclipse-temurin:21-jre-alpine`** — JRE-only (no JDK), Alpine base,
  official Adoptium image
- **Non-root user** — `cookmaid` user owns `/app`; entrypoint writes to
  `/app/web/` before `exec`, so chown covers the write path
- **`needs:` in CI** — prevents pushing a broken image if any upstream job fails
- **SHA tag** — enables pinned deployments and rollback alongside rolling `latest`

## Gotchas

- `gettext` must be explicitly installed on Alpine — `envsubst` is not in the
  base `eclipse-temurin:21-jre-alpine` image
- `staticFiles` must be registered **before** `/api` routes; otherwise the
  catch-all can interfere with 404 handling for API paths
- The `envsubst` variable list (`'$OIDC_DISCOVERY_URI $OIDC_CLIENT_ID $OIDC_SCOPE'`)
  must be explicit — Kotlin/Wasm JS output contains bare `$` characters that
  would be corrupted by an unrestricted substitution
- The `gradle:8-jdk21-alpine` base image ships Gradle 8.x; the wrapper
  downloads the project-configured version (Gradle 9), so the first build
  incurs a download. Using `eclipse-temurin:21-jdk-alpine` as the build base
  avoids this implicit mismatch
- `COPY . .` in the build stage is scoped by `.dockerignore`; without it, `.git`
  alone invalidates the layer cache on every commit
