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
  - axion-release
date: 2026-04-05
---

# Docker image for cookmaid (Ktor + WasmJS)

Single container image where Ktor serves both the REST API and the WasmJS
frontend as static files. Artifacts are built on the host by Gradle; Docker
copies pre-built outputs into the runtime image. OIDC configuration is
injected into `index.html` via `envsubst` at container startup.

## Architecture

Host-built artifacts, runtime-only image:
- **Host Gradle tasks**: `:server:installDist` + `:composeApp:wasmJsBrowserDistribution`
- **Runtime stage** (`eclipse-temurin:21-jre-alpine`): minimal JRE + `gettext` package
  (provides `envsubst`), non-root user `cookmaid`
- **Entrypoint**: `docker/docker-entrypoint.sh` — runs `envsubst` on `index.html` then
  `exec`s the server binary

## Solution

### Gradle tasks — `build.gradle.kts` (root)

```kotlin
val dockerPrereqs = listOf(":server:installDist", ":composeApp:wasmJsBrowserDistribution")
val dockerPlatforms = "linux/amd64,linux/arm64"

tasks.register<Exec>("buildDockerImage") {
    group = "docker"
    description = "Build the cookmaid Docker image for the local architecture and load it into the daemon."
    dependsOn(dockerPrereqs)
    commandLine(
        "docker", "buildx", "build",
        "--load",
        "-f", "docker/Dockerfile",
        "-t", "cookmaid:${rootProject.version}",
        "-t", "cookmaid:latest",
        ".",
    )
}

tasks.register<Exec>("pushDockerImage") {
    group = "docker"
    description = "Build and push the cookmaid Docker image. Requires -Pdocker.registry=<registry>."
    dependsOn(dockerPrereqs)
    val registry = findProperty("docker.registry")?.toString() ?: ""
    val version = rootProject.version.toString()
    commandLine(
        "docker", "buildx", "build",
        "--platform", dockerPlatforms,
        "--push",
        "-f", "docker/Dockerfile",
        "-t", "$registry:$version",
        "-t", "$registry:latest",
        ".",
    )
}
```

`buildDockerImage` uses `--load` (no `--platform`) so it works with the default
docker driver and loads the image into the local daemon. `pushDockerImage` uses
`--platform` for multi-arch and `--push` (requires `docker-container` driver from
`setup-buildx-action` in CI).

### Dockerfile — `docker/Dockerfile`

```dockerfile
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache gettext && \
    addgroup -S cookmaid && adduser -S cookmaid -G cookmaid

WORKDIR /app
COPY server/build/install/server/ .
COPY composeApp/build/dist/wasmJs/productionExecutable/ /app/web/
COPY docker/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh && chown -R cookmaid:cookmaid /app

USER cookmaid

ENV WEB_DIR=/app/web
EXPOSE 8081

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["/app/bin/server"]
```

### Entrypoint script — `docker/docker-entrypoint.sh`

```sh
#!/bin/sh
set -e
envsubst '$OIDC_DISCOVERY_URI $OIDC_CLIENT_ID $OIDC_SCOPE' \
    < /app/web/index.html \
    > /app/web/index.html.tmp \
    && mv /app/web/index.html.tmp /app/web/index.html
exec "$@"
```

**Critical:** The explicit variable list prevents `envsubst` from corrupting
`$`-patterns inside compiled Kotlin/Wasm JS output.

### Ktor static file routing — `server/.../web/WebModule.kt`

```kotlin
fun Application.configureStaticFiles() {
    val webDir = environment.config.property("web.dir").getString()
    routing {
        staticFiles("/", File(webDir)) {
            default("index.html")
        }
    }
}
```

Call `configureStaticFiles()` **before** `configureRouting()` in `Application.kt`.
`web.dir` defaults to `"web"` in `application.yaml`; Docker sets `WEB_DIR=/app/web`
env var but the actual value is sourced from Ktor config, not the env var directly.
Test config must include `"web.dir" to "web"` in `MapApplicationConfig`.

### `.dockerignore`

```
.git
.github
.gradle
.idea
*.iml
local.properties
sandbox
docs
```

Keep this minimal with no negation patterns (see Gotchas). We only `COPY`
specific pre-built directories, so broad exclusions are safe.

### GitHub Actions — `.github/workflows/ci.yml`

```yaml
docker:
  name: Docker
  runs-on: ubuntu-latest
  needs: [detekt, server, shared, android, composeApp]
  permissions:
    contents: read
    packages: write
  steps:
    - uses: actions/checkout@v5
    - uses: jdx/mise-action@v2
    - uses: gradle/actions/setup-gradle@v4
    - uses: docker/setup-qemu-action@v3
    - uses: docker/setup-buildx-action@v3
    - name: Build Docker image
      if: "!startsWith(github.ref, 'refs/tags/')"
      run: ./gradlew buildDockerImage
    - uses: docker/login-action@v3
      if: startsWith(github.ref, 'refs/tags/')
      with:
        registry: ghcr.io
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    - name: Push to GHCR
      if: startsWith(github.ref, 'refs/tags/')
      run: ./gradlew pushDockerImage -Pdocker.registry=ghcr.io/${{ github.repository }}
```

`setup-qemu-action` + `setup-buildx-action` creates a `docker-container` driver
that supports `--platform linux/amd64,linux/arm64`. Both are required for
`pushDockerImage`; `buildDockerImage` with `--load` works without them.

### Versioning — axion-release-plugin

```toml
# gradle/libs.versions.toml — camelCase key is required for alias accessor
[versions]
axionRelease = "1.21.1"

[plugins]
axionRelease = { id = "pl.allegro.tech.build.axion-release", version.ref = "axionRelease" }
```

```kotlin
// build.gradle.kts (root)
plugins {
    alias(libs.plugins.axionRelease)
    // ...
}

allprojects {
    version = rootProject.scmVersion.version
}
```

Version is derived from git tags (`v1.0.0` → `1.0.0`). Get current version:
`./gradlew -q currentVersion -Prelease.quiet`

## Key Decisions

- **Host-built artifacts** — avoids multi-stage Docker complexity; Gradle cache
  on the host/CI runner is faster than rebuilding inside Docker on every push
- **`installDist` over fat JAR** — `application` plugin produces `bin/` + `lib/`
  with no extra plugins; matches the Kotlin/Ktor convention
- **`eclipse-temurin:21-jre-alpine`** — JRE-only (no JDK), minimal Alpine base
- **Non-root user** — `cookmaid` user owns `/app`; entrypoint writes to
  `/app/web/` (covered by the `chown -R` in the Dockerfile)
- **Push on `v*` tags only** — avoids pushing broken images on every main commit;
  tags represent intentional releases
- **Two Gradle tasks** — `buildDockerImage` (local dev, `--load`) and
  `pushDockerImage` (CI, multi-arch `--push`) keep behavior consistent between
  local and CI

## Gotchas

### `wasmJsBrowserDistribution` vs `wasmJsBrowserProductionWebpack`

Use `wasmJsBrowserDistribution`. The other task outputs only the JS/WASM bundle
to `build/kotlin-webpack/` with no `index.html`. `wasmJsBrowserDistribution`
produces the complete distribution including `index.html`, all WASM, and
resources to `build/dist/wasmJs/productionExecutable/`. The Dockerfile `COPY`
path must match this output directory exactly.

### `.dockerignore` negation patterns broken with BuildKit

BuildKit cannot traverse into a directory once it has been excluded. Both patterns fail:

```
# Does NOT work
composeApp/
!composeApp/build/dist/

# Does NOT work either
composeApp/**
!composeApp/build/dist/**
```

Since the Dockerfile only copies specific pre-built subdirectories, the simplest
fix is to not exclude `composeApp/` at all — or keep `.dockerignore` minimal with
only safe, non-negated exclusions.

### Multi-platform build requires docker-container driver

`--platform linux/amd64,linux/arm64` fails with the default `docker` driver:

```
ERROR: Multi-platform build is not supported for the docker driver.
Switch to a different driver, or turn on the containerd image store.
```

For local builds: use `docker buildx build --load` (no `--platform`) — loads
native-arch image into the local daemon, works with the default driver.

For CI multi-arch push: `setup-qemu-action` + `setup-buildx-action` creates a
`docker-container` driver that supports multi-platform builds and `--push`.

### `gettext` must be installed explicitly on Alpine

`envsubst` is not in `eclipse-temurin:21-jre-alpine`. Install with:
`apk add --no-cache gettext`

### `staticFiles` must precede `/api` routes

Register the static files route before any API routes in Ktor. If `/api` comes
first, the catch-all behavior can interfere with 404 handling.

### Test config must include `web.dir`

`WebModule.kt` reads `web.dir` from Ktor config. Any `testApplication {}` that
loads a `MapApplicationConfig` must include `"web.dir" to "web"` or all server
integration tests will fail with `ApplicationConfigurationException`.

### Gradle version catalog alias: camelCase key required

The Gradle Kotlin DSL generates `libs.plugins.axionRelease` from the TOML key.
Hyphenated keys like `axion-release` do **not** auto-convert; the accessor simply
doesn't exist and you get an unresolved reference. Use camelCase in the TOML:
`axionRelease = { id = "pl.allegro.tech.build.axion-release", ... }`.
