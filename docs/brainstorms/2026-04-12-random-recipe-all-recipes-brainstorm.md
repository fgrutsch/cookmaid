# Random Recipe From All Recipes

Date: 2026-04-12
Issue: #27

## What We're Building

Fix the random recipe feature so it picks from all user recipes, not
just the ones loaded in the current paginated view.

Currently `rollRandomRecipe()` in `RecipeListViewModel` picks from
`state.value.recipes` — only the pages fetched so far (page size 20).
Users with many recipes always get random picks from their first page.

## Why This Approach

Add a `GET /api/recipes/random` server endpoint that selects a random
recipe directly from the database. The client calls this endpoint
instead of picking from the in-memory list.

- Guarantees uniform distribution across all recipes
- No need to load all recipes client-side
- Consistent with the layered architecture (routes → service → repository)
- Single HTTP call, no pagination exhaustion

Alternative considered: fetch all recipe IDs client-side, then pick.
Rejected because it defeats the purpose of pagination and scales
poorly as recipe count grows.

## Key Decisions

- **Server-side random**: `ORDER BY RANDOM() LIMIT 1` in SQL,
  filtered by `userId` — simple, correct for small-to-medium
  datasets
- **Exclude current recipe**: pass optional `excludeId` query param
  so consecutive rolls don't repeat the same recipe
- **Return full Recipe**: the endpoint returns a complete `Recipe`
  object (same as list items) so the UI can display it immediately
  without a second fetch
- **Tag filtering**: support optional `tags` query param to respect
  the active tag filter when rolling random

## Open Questions

None — the fix is straightforward with a well-understood pattern.
