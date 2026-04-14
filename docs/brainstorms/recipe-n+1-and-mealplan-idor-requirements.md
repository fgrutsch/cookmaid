---
date: 2026-04-14
topic: recipe-n+1-and-mealplan-idor
---

# Fix Recipe N+1 Query and Meal Plan IDOR

## Problem Frame

Two backend issues affecting performance and security:

1. `RecipeRepository.find()` calls `loadIngredients()` once per recipe,
   producing N+1 DB queries per page load. At page size 20 = 21 queries.
   `create()` also re-queries ingredients immediately after insert.
2. `MealPlanService.create()` does not validate that a referenced `recipeId`
   belongs to the calling user. An authenticated user can reference another
   user's recipe, leaking its name in the response (IDOR / CWE-639).

## Requirements

**N+1 query fix (Issue #42)**
- R1. `find()` batch-loads all ingredients for the page in a single query,
  grouped by recipe ID in memory.
- R2. `create()` returns the recipe with ingredients populated from the
  request data, avoiding the `loadIngredients()` call after insert.
- R3. `find()` issues at most 2 DB queries regardless of page size
  (1 recipe page + 1 batch ingredient load).
- R4. Response shape is unchanged.

**Meal plan IDOR fix (Issue #43)**
- R5. `MealPlanService.create()` validates that `recipeId` (when non-null)
  belongs to the calling user before persisting, using
  `RecipeRepository.isOwner(userId, recipeId)`.
- R6. Cross-user recipe references return 404 (not 403), consistent with
  existing ownership patterns.
- R7. `recipeId = null` (note-only items) continues to work without checks.

## Success Criteria

- All existing recipe and meal plan tests pass.
- Integration test (routes layer, two distinct JWT subjects) covers the
  cross-user recipe reference case for #43.
- No additional queries introduced in `findById()` or `findRandom()` paths
  (single-recipe lookups keep the existing single `loadIngredients` call).

## Scope Boundaries

- `findById()` and `findRandom()` load a single recipe — N+1 is not relevant
  there; no changes needed.
- `MealPlanService.create()` is updated to validate recipe ownership (R5);
  `update()` and `delete()` already check ownership and are not changed.
- No changes to the meal plan `update()` flow — it carries no `recipeId`.
  If a `recipeId` parameter is ever added, the same `isOwner()` check must apply.
- Existing meal plan items that may reference cross-user recipes are not
  retroactively cleaned up. The fix is forward-only.

## Next Steps

-> /ce:plan for structured implementation planning
