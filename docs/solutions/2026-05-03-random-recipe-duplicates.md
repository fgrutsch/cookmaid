# Random Recipe Duplicates Fix

## Problem

Re-rolling random recipes frequently returned duplicates because only the
currently displayed recipe was excluded. Previously shown recipes could
reappear immediately.

## Solution

Changed from single `excludeId` to multi-value `excludeIds` across all layers.

### Server

- `GET /api/recipes/random?excludeIds=uuid1,uuid2,...` — comma-separated list
- Repository uses Exposed `notInList` to exclude all provided IDs
- Fallback: when all recipes are excluded, returns any random recipe

### Client

- `RecipeListState.shownRecipeIds: Set<String>` accumulates every shown
  recipe ID
- Each re-roll passes all accumulated IDs as `excludeIds`
- `shownRecipeIds` resets on dialog close (`ClearRandomRecipe`) or tag change

## Key Decisions

- **Stateless server**: No server-side session tracking. The client owns the
  exclusion list, keeping the API stateless.
- **No upper bound on excludeIds**: The list is bounded by the user's recipe
  count, which is small enough for `NOT IN`.
- **Fallback over 404**: When all recipes are exhausted, the server returns
  a random recipe rather than 404, so the re-roll button always works.

## Gotchas

- Tag changes reset `shownRecipeIds` because the recipe pool changes.
- `Uuid.parse()` on malformed input throws `IllegalArgumentException`,
  which `StatusPages` maps to 400.
