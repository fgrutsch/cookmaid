---
title: "N+1 query in RecipeRepository.find() — batch-load ingredients with inList"
date: 2026-04-14
category: performance-issues
module: recipe
problem_type: performance_issue
component: database
symptoms:
  - "find() issues N+1 DB queries — one per recipe for ingredients"
  - "create() re-queries ingredients after insert despite having input data"
root_cause: missing_include
resolution_type: code_fix
severity: medium
tags:
  - n-plus-one
  - exposed-orm
  - batch-loading
  - inlist
  - recipe
  - ingredients
---

# N+1 query in RecipeRepository.find() — batch-load ingredients with inList

## Problem

`RecipeRepository.find()` called `loadIngredients()` once per recipe in the
result page, producing N+1 queries (page size 20 = 21 queries). Additionally,
`create()` re-queried ingredients after insert even though the input data
already contained everything needed for the response.

## Symptoms

- `find()` issues one extra DB query per recipe to load ingredients
- `create()` performs an unnecessary round-trip after insert

## What Didn't Work

No failed approaches — the fix was straightforward once the pattern was
identified. The existing `loadIngredients()` join structure was reusable.

## Solution

Added `loadIngredientsBatch(recipeIds, locale)` — same join as
`loadIngredients()` but with `WHERE recipe_id IN (...)` via Exposed's
`inList` operator. Results grouped in memory by recipe ID.

```kotlin
// Before (N+1): called per recipe inside find()
val ingredients = loadIngredients(id, locale)

// After (batch): single query for all recipes on the page
val ids = pageRows.map { it[RecipesTable.id] }
val ingredientsByRecipe = loadIngredientsBatch(ids, locale)
// then per recipe:
val ingredients = ingredientsByRecipe[id].orEmpty()
```

For `create()`, replaced the post-insert `loadIngredients()` call with
`data.ingredients` directly — the client already sends hydrated ingredient
objects in the request.

**Files changed:**
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRepository.kt`

## Why This Works

`inList` generates a single `WHERE recipe_id IN (id1, id2, ...)` query
instead of N individual `WHERE recipe_id = idN` queries. The join structure
(RecipeIngredientsTable -> CatalogItemsTable -> ItemCategoriesTable) is
identical to `loadIngredients()`, so the result mapping is the same.

For `create()`, the client sends `Item.Catalog` objects with hydrated names
in the request body. The locale the client used is authoritative for the
create response, so re-querying is unnecessary.

## Prevention

- When loading child entities for a list of parents, always batch with
  `inList` rather than iterating with individual queries
- After insert operations, check whether the response can be built from
  input data instead of re-querying
- Keep `loadIngredients()` for single-entity methods (`findById`,
  `findRandom`) where N+1 is not a concern

## Related Issues

- GitHub issue: #42
- Plan: `docs/plans/2026-04-14-001-fix-recipe-n1-mealplan-idor-plan.md`
