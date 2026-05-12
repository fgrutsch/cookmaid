---
title: "feat: Add project documentation — FAQ, screenshots, Docker deployment guide"
type: feat
status: active
date: 2026-05-12
---

# feat: Add project documentation — FAQ, screenshots, Docker deployment guide

## Summary

Add user-facing documentation: a FAQ/guide (`docs/faq.md`) covering common
workflows, 3-4 screenshots of main screens in the README, a production Docker
Compose example showing how to self-host Cookmaid, and a CLAUDE.md note to
exclude the FAQ from AI maintenance scope.

## Requirements

- R1. Update README with screenshots of 3-4 main screens
- R2. Add `docs/faq.md` with feature guide and common workflows
- R3. Link FAQ from README
- R4. Add CLAUDE.md note: FAQ is user-maintained, not AI-maintained
- R5. Add production Docker Compose example to README or a separate file,
  showing how to run Cookmaid with all required env vars

## Scope Boundaries

- No code changes — documentation only
- No changes to the dev Docker Compose setup
- Screenshots are taken manually by the user and placed in `docs/images/`;
  the plan creates placeholder references

### Deferred to Follow-Up Work

- Automated screenshot generation (e.g., via Playwright or Compose Preview)
- Full deployment guide for Kubernetes, Fly.io, etc.

## Context & Research

### Relevant Code and Patterns

- Existing README at `README.md` — has logo, badges, tech stack, project
  structure, local dev setup, and test commands
- Docker production setup: `docker/Dockerfile` (eclipse-temurin:21-jre-alpine,
  port 8081, entrypoint runs envsubst for OIDC vars)
- Dev Docker Compose: `dev/docker-compose.yml` (PostgreSQL, PocketID, nginx)
- Docker image published to `ghcr.io/fgrutsch/cookmaid` on `v*` tags
- Required env vars at runtime: `DATABASE_URL`, `DATABASE_USER`,
  `DATABASE_PASSWORD`, `OIDC_ISSUER`, `OIDC_JWKS_URL`, `OIDC_DISCOVERY_URI`,
  `OIDC_CLIENT_ID`, `OIDC_SCOPE`, `OIDC_ACCOUNT_URI`
  (`WEB_DIR` has a default in the Dockerfile — no need to set it)
- Main screens: Recipe List, Recipe Detail, Meal Plan, Shopping List,
  Add/Edit Recipe, Settings

## Key Technical Decisions

- **Screenshot format**: PNG, placed in `docs/images/` alongside existing
  logo/icon files. Use descriptive names like `screenshot-recipes.png`.
- **Docker Compose example**: Include a `docker-compose.yml` snippet directly
  in the README under a new "Production Deployment" section, rather than a
  separate file. Keeps everything discoverable in one place.
- **FAQ structure**: Organized by feature area (Recipes, Meal Planning,
  Shopping List, General). Written for end-users, not developers.

## Open Questions

### Deferred to Implementation

- Exact screenshots depend on current app state and user data — the user
  will take them manually and place them in `docs/images/`

## Implementation Units

### U1. Add screenshots to README

**Goal:** Add 3-4 screenshots of main screens to the README.

**Requirements:** R1

**Dependencies:** None

**Files:**
- Modify: `README.md`
- Create: `docs/images/screenshot-recipes.png` (user-provided)
- Create: `docs/images/screenshot-meal-plan.png` (user-provided)
- Create: `docs/images/screenshot-shopping-list.png` (user-provided)
- Create: `docs/images/screenshot-recipe-detail.png` (user-provided)

**Approach:**
- Add a "Screenshots" section after the intro paragraph and before "Tech Stack"
- Use a responsive grid layout (HTML table or side-by-side images) so
  screenshots display well on GitHub
- Reference images as `docs/images/screenshot-*.png`
- Create placeholder image files or document the expected filenames for the
  user to replace

**Patterns to follow:**
- Existing logo reference: `<img src="docs/images/cookmaid_logo.png" ...>`

**Test expectation:** none — static documentation, visual verification only

**Verification:**
- README renders correctly on GitHub with screenshot section
- Image references point to files in `docs/images/`

### U2. Create FAQ / feature guide

**Goal:** Create `docs/faq.md` with a user-facing guide to Cookmaid features
and common workflows.

**Requirements:** R2

**Dependencies:** None

**Files:**
- Create: `docs/faq.md`

**Approach:**
- Organize by feature area: Recipes, Meal Planning, Shopping List, General
- Cover workflows: creating a recipe, planning meals for the week, generating
  a shopping list, editing/deleting items, using tags/filters
- Written for end-users (non-technical), concise Q&A format
- Include a brief intro explaining what Cookmaid is

**Test expectation:** none — static documentation

**Verification:**
- `docs/faq.md` exists with sections covering all main features
- Language is non-technical and user-friendly

### U3. Add Docker production deployment section to README

**Goal:** Add a "Production Deployment" section to the README with a Docker
Compose example showing how to self-host Cookmaid.

**Requirements:** R5

**Dependencies:** None

**Files:**
- Modify: `README.md`

**Approach:**
- Add section after "Run Tests" (end of local dev setup)
- Include a complete `docker-compose.yml` example with:
  - Cookmaid service using `ghcr.io/fgrutsch/cookmaid:<version>`
  - PostgreSQL service
  - Env vars grouped by purpose with inline comments:
    - Server-side (application.yaml): `DATABASE_URL`, `DATABASE_USER`,
      `DATABASE_PASSWORD`, `OIDC_ISSUER`, `OIDC_JWKS_URL`, `OIDC_CLIENT_ID`
    - Client-side entrypoint injection (envsubst): `OIDC_DISCOVERY_URI`,
      `OIDC_CLIENT_ID`, `OIDC_SCOPE`, `OIDC_ACCOUNT_URI`
    - Note: `OIDC_CLIENT_ID` is consumed by both server and entrypoint
  - Port mapping (8081)
  - Health checks
- Add notes on OIDC provider setup (PocketID or other OIDC-compliant provider)
- Reference the required env vars from CLAUDE.md

**Patterns to follow:**
- Existing dev compose structure in `dev/docker-compose.yml`
- Dockerfile env var expectations from `docker/Dockerfile` and
  `docker/docker-entrypoint.sh`

**Test expectation:** none — static documentation, manual verification that
the compose example is syntactically valid

**Verification:**
- README has a "Production Deployment" section with a working Docker Compose
  snippet
- All required env vars are documented

### U4. Link FAQ from README and update CLAUDE.md

**Goal:** Link the new FAQ from the README and add a CLAUDE.md note excluding
FAQ from AI maintenance scope.

**Requirements:** R3, R4

**Dependencies:** U2

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`

**Approach:**
- Add a link to `docs/faq.md` in the README, either in the intro or as a
  dedicated "Documentation" section
- Add a note in CLAUDE.md under an appropriate section stating that
  `docs/faq.md` is user-maintained documentation and should not be
  auto-updated or kept in sync by AI agents

**Test expectation:** none — static documentation

**Verification:**
- README links to `docs/faq.md`
- CLAUDE.md contains a note about FAQ maintenance scope

## Sources & References

- Related issue: #17
- Docker image: `ghcr.io/fgrutsch/cookmaid`
- Production Dockerfile: `docker/Dockerfile`
- Dev compose: `dev/docker-compose.yml`
