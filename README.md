<div align="center">
<img src="docs/images/cookmaid_logo.png" alt="Cookmaid logo" width="300"/>

[![Github Actions CI Workflow](https://img.shields.io/github/v/tag/fgrutsch/cookmaid?logo=Github&style=for-the-badge)](https://github.com/fgrutsch/cookmaid/releases)
[![Github Actions CI Workflow](https://img.shields.io/github/actions/workflow/status/fgrutsch/cookmaid/ci.yml?logo=Github&style=for-the-badge)](https://github.com/fgrutsch/cookmaid/actions/workflows/ci.yml?query=branch%3Amain)
[![Codecov](https://img.shields.io/codecov/c/github/fgrutsch/cookmaid/main?logo=Codecov&style=for-the-badge)](https://codecov.io/gh/fgrutsch/cookmaid)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)
[![Google Play](https://img.shields.io/badge/Google%20Play-Download-3A5A67?logo=googleplay&logoColor=white&style=for-the-badge)](https://play.google.com/store/apps/details?id=io.github.fgrutsch.cookmaid)
</div>

Cookmaid is a self-hosted meal planning app. Manage your recipes, plan meals for the week,
and generate shopping lists — available on Android and as a Progressive Web App.
The Android app is on
[Google Play](https://play.google.com/store/apps/details?id=io.github.fgrutsch.cookmaid).

<p align="center">
  <img src="docs/images/shopping_list.png" alt="Shopping list" height="360"/>
  <img src="docs/images/recipe_list.png" alt="Recipe list" height="360"/>
  <img src="docs/images/recipe_detail.png" alt="Recipe detail" height="360"/>
  <img src="docs/images/mealplan.png" alt="Meal plan" height="360"/>
  <img src="docs/images/account.png" alt="Account" height="360"/>
</p>

See the [FAQ & Feature Guide](docs/faq.md) for usage tips and common workflows.

## Try It

The [`demo`](demo) directory contains a self-contained Docker Compose
setup with PostgreSQL, [Logto](https://logto.io/) (OIDC provider), and
Cookmaid — everything you need to try the app locally.

```shell
cd demo
docker compose up -d
```

1. Wait ~15 seconds for Logto to initialize and seed
2. Open http://localhost:3002 and complete the one-time Logto admin setup wizard
3. Open http://localhost:8081 and log in with `testuser` / `CookmaidTest2026!`

To stop everything: `docker compose down` (add `-v` to also remove data).

### Optional: LLM fallback for recipe import

Recipe import reads the schema.org data most recipe sites embed and needs no LLM.
For pages without it, Cookmaid can ask any OpenAI-compatible chat-completions API.
Set all three variables to enable it; leave them unset to disable:

| Variable | Meaning |
|---|---|
| `LLM_BASE_URL` | API base; `/chat/completions` is appended |
| `LLM_API_KEY` | Bearer token (may be empty for a local Ollama) |
| `LLM_MODEL` | Model id of that provider |

Examples:

- **OpenAI:** `LLM_BASE_URL=https://api.openai.com/v1`
- **Ollama:** `LLM_BASE_URL=http://<host>:11434/v1`, `LLM_MODEL=qwen2.5:7b`. Use a model of
  about 7B parameters or more; 3B models miss many recipes.
- **Cloudflare Workers AI:**
  `LLM_BASE_URL=https://api.cloudflare.com/client/v4/accounts/<account-id>/ai/v1`,
  `LLM_MODEL=@cf/google/gemma-4-26b-a4b-it`, API token with Workers AI permissions.
  The Workers Free plan includes 10,000 neurons/day and fails (rather than bills)
  beyond that.

Import timeouts, size limits and the ingredient unit list sit under `recipe.import` in
`server/src/main/resources/application.yaml`; each value is overridable via its
`RECIPE_IMPORT_*` env var.


## Tech Stack

- Kotlin + Compose Multiplatform (Android, WasmJS)
- Ktor server (JVM)
- Exposed ORM + Flyway migrations on PostgreSQL
- Koin for DI, kotlinx.serialization + kotlinx.datetime
- OIDC auth via [Logto](https://logto.io/)

Pinned versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Project Structure

- **`core/`** — Multiplatform library (Android, JVM, WasmJS). Data models
  and DTOs shared across client and server.
- **`app/shared/`** — Compose Multiplatform UI library (Android, WasmJS).
- **`app/webApp/`** — WasmJS application entry point. Progressive Web App
  (manifest, service worker, maskable icons).
- **`app/androidApp/`** — Android application entry point.
- **`server/`** — Ktor backend (JVM). Serves API + WasmJS static files.
- **`dev/`** — Docker Compose setup for local infrastructure
- **`docker/`** — Production Dockerfile + entrypoint

## Local Development Setup

### Prerequisites

- JDK 17+ (runtime container uses JDK 21)
- Docker & Docker Compose

### 1. Start Infrastructure

```shell
cd dev
docker compose up -d
```

This starts:
- **PostgreSQL** on port 5432 (databases: `cookmaid`, `logto`)
- **Logto** (OIDC provider) on http://localhost:3001 (admin console on http://localhost:3002)

A seed container automatically configures the Logto application, API
resource, sign-in experience, and a test user — no manual setup needed.

Log in with `testuser` / `CookmaidTest2026!`.
Logto admin console: http://localhost:3002 (one-time setup wizard on first use).

### 2. Configure local settings

Dev OIDC settings live in `local.properties` (NOT checked into VCS). Add the following:

```properties
sdk.dir=<path-to-android-sdk>

oidc.discoveryUri=http://localhost:3001/oidc/.well-known/openid-configuration
oidc.webClientId=cookmaid-dev-web
oidc.nativeClientId=cookmaid-dev-native
oidc.scope=openid profile email offline_access
oidc.accountUri=http://localhost:3001/account/security
oidc.resource=http://localhost:8081/api
oidc.audience=http://localhost:8081/api
```

`oidc.webClientId` is the web (SPA) app, `oidc.nativeClientId` the native (Android) app.

The server reads its OIDC config from `application.yaml`. `oidc.issuer`
and `oidc.jwks-url` default to the local Logto instance;
`oidc.audience` defaults to the local API resource indicator.

### 3. Run

```shell
# Run server (port 8081)
./gradlew :server:run

# Run web app (Wasm, port 8080) — injects dev OIDC config from local.properties
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun

# Run Android app
./gradlew :app:androidApp:installDevDebug
```

### 4. Run Tests

```shell
# All tests
./gradlew test

# Per module
./gradlew :server:test
./gradlew :core:allTests
./gradlew :app:shared:allTests
```

### Android prod builds

The `prod` flavor reads its deployment values from a gitignored `prod.properties` in the repo root.
The release workflow writes it from the `ANDROID_PROD_PROPERTIES` repository variable; prod release
builds fail if a required key is missing.

```properties
baseUrl=https://cookmaid.example.com
oidc.discoveryUri=https://sso.example.com/oidc/.well-known/openid-configuration
oidc.clientId=<native-app-client-id>
oidc.scope=openid profile email offline_access
oidc.accountUri=https://sso.example.com/account/security
oidc.resource=https://cookmaid.example.com/api
```
