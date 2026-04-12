---
title: "Random recipe selection limited to first loaded page"
date: 2026-04-12
category: logic-errors
tags:
  - pagination
  - randomization
  - client-server
  - kotlin-multiplatform
severity: medium
components:
  - composeApp/ui/recipe/list/RecipeListViewModel
  - server/recipe/RecipeRepository
  - server/recipe/RecipeModule
root_cause: >
  Client-side random selection operated on paginated in-memory list
  (state.value.recipes, max 20 items) instead of the full dataset
resolution_type: server-side-endpoint
---

# Random recipe selection limited to first loaded page

## Problem

The "random recipe" feature picked from `state.value.recipes` in
`RecipeListViewModel` — only the pages fetched so far via infinite
scroll (page size 20). Users with many recipes always got random
picks from their first page.

## Root Cause

`rollRandomRecipe()` called `.random()` on the in-memory list
populated by cursor-based pagination. The list was a partial view,
not the full dataset.

## Solution

Added a server-side `GET /api/recipes/random` endpoint using
PostgreSQL `ORDER BY RANDOM() LIMIT 1`, scoped to the authenticated
user's recipes.

### Server

- **Repository**: `findRandom(userId, tag, excludeId, locale)` builds
  a WHERE clause with userId filter, optional tag array containment
  (`@> ARRAY[?]::text[]`), optional `NeqOp` for excludeId. Uses
  Exposed's `Random()` for ordering. Fallback: if excludeId filters
  out the only candidate, retries without exclusion.
- **Service**: thin pass-through — no ownership check needed since the
  query already filters by userId.
- **Route**: `get("/random")` registered before `route("/{recipeId}")`
  to prevent Ktor matching "random" as a UUID path parameter.

### Client

- **RecipeClient**: `fetchRandom(tag, excludeId)` — GET with query
  params.
- **ApiRecipeRepository**: catches `ClientRequestException` 404 and
  returns null.
- **ViewModel**: replaced sync in-memory pick with async `launch {}`
  calling `repository.fetchRandom`, with `isLoadingRandom` state.
- **UI**: `RandomRecipeDialog` shows `CircularProgressIndicator` on
  the reroll button during network call.

### Key Design Decisions

- `ORDER BY RANDOM() LIMIT 1` — simple, correct for per-user recipe
  collections (tens to hundreds of rows).
- `excludeId` parameter prevents showing the same recipe on reroll.
  Fallback handles the single-recipe edge case.
- Route ordering avoids Ktor path parameter collision.

## Prevention

- Any feature requiring the full dataset (random, aggregation,
  global search) must be a **server-side endpoint**. The client
  should never assume its loaded subset is complete.
- Code review red flag: `.random()`, `.filter()`, `.count()` on a
  list populated by a paginated API call.
- Decision heuristic: "Would this produce a different result if the
  user had 1000 items vs 10?" If yes, it belongs on the server.

## References

- Issue: #27
- PR: #64
