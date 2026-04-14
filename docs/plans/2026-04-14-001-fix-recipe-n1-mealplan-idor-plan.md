---
title: "fix: Batch-load recipe ingredients and validate meal plan recipe ownership"
type: fix
status: active
date: 2026-04-14
origin: docs/brainstorms/recipe-n+1-and-mealplan-idor-requirements.md
---

# fix: Batch-load recipe ingredients and validate meal plan recipe ownership

## Overview

Two server-side fixes: eliminate the N+1 query in `RecipeRepository.find()`
by batch-loading ingredients with `inList`, and close an IDOR gap in
`MealPlanService.create()` by validating recipe ownership before persisting.

## Problem Frame

1. `find()` calls `loadIngredients()` per recipe — page size 20 = 21 queries.
   `create()` also re-queries ingredients after insert.
2. `MealPlanService.create()` accepts any `recipeId` without ownership check,
   leaking another user's recipe name in the response (CWE-639).

(see origin: `docs/brainstorms/recipe-n+1-and-mealplan-idor-requirements.md`)

## Requirements Trace

- R1. `find()` batch-loads ingredients in a single query, grouped by recipe ID
- R2. `create()` returns ingredients from request data, skipping `loadIngredients()`
- R3. `find()` issues at most 2 DB queries regardless of page size
- R4. Response shape is unchanged
- R5. `MealPlanService.create()` validates `recipeId` ownership before persisting
- R6. Cross-user recipe references return 404
- R7. `recipeId = null` continues to work without checks

## Scope Boundaries

- `findById()` and `findRandom()` unchanged (single-recipe loads)
- `MealPlanService.update()` and `delete()` unchanged (already check ownership)
- Forward-only fix — no retroactive cleanup of existing cross-user references

## Context & Research

### Relevant Code and Patterns

- `RecipeRepository.loadIngredients()` — joins `RecipeIngredientsTable` →
  `CatalogItemsTable` → `ItemCategoriesTable`, filters by single recipeId.
  Batch version uses the same join with `inList` filter.
- `ShoppingListService.create()` — returns `ShoppingItem?`, null on ownership
  failure. Route handler checks null → 404. Exact pattern to follow for R5/R6.
- `MealPlanModule.kt` route handler — currently responds 201 unconditionally.
  Needs null check matching `ShoppingModule.kt` lines 70-76.
- Koin `singleOf(::XxxService)` auto-resolves constructor params. Adding
  `RecipeRepository` to `MealPlanService` constructor just works — both modules
  already loaded in `Application.kt`.
- `inList` not currently used in the codebase. Import:
  `org.jetbrains.exposed.v1.core.inList`.
- `TestJwt.generateToken(subject)` — call with different subjects for
  multi-user tests. `createUser(subject)` helper in service tests.

## Key Technical Decisions

- **Batch via `inList`**: Same join structure as `loadIngredients()` but with
  `WHERE recipe_id IN (...)` instead of `WHERE recipe_id = ...`. Group results
  in memory by recipe ID. One new private method `loadIngredientsBatch()`.
- **`create()` skips re-query**: The client sends full `Item.Catalog` objects
  with hydrated names in the request. `data.ingredients` already contains
  everything needed for the response. The locale the client used is
  authoritative for the create response.
- **`MealPlanService.create()` returns nullable**: Change return type to
  `MealPlanItem?`, return null on ownership failure. Matches
  `ShoppingListService` pattern.

## Implementation Units

- [ ] **Unit 1: Batch-load ingredients in RecipeRepository.find()**

  **Goal:** Replace per-recipe `loadIngredients()` with a single batch query
  in `find()`. Skip `loadIngredients()` in `create()`.

  **Requirements:** R1, R2, R3, R4

  **Dependencies:** None

  **Files:**
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRepository.kt`
  - Test: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/recipe/PostgresRecipeRepositoryTest.kt`

  **Approach:**
  - Add `loadIngredientsBatch(recipeIds: List<Uuid>, locale: SupportedLocale)`
    private method. Same join as `loadIngredients()` but filters with
    `RecipeIngredientsTable.recipeId inList recipeIds`. Returns
    `Map<Uuid, List<RecipeIngredient>>`.
  - In `find()`: if `pageRows` is empty, return early with no batch query.
    Otherwise collect `pageRows.map { it[RecipesTable.id] }`, call
    `loadIngredientsBatch(ids, locale)`, map each recipe using
    `ingredientsByRecipe[id].orEmpty()`.
  - In `create()`: build `Recipe` using `data.ingredients` directly instead
    of calling `loadIngredients()`.
  - Keep `loadIngredients()` for `findById()` and `findRandom()` (unchanged).

  **Patterns to follow:**
  - Existing `loadIngredients()` join structure and `RecipeIngredient` / `Item`
    construction pattern

  **Test scenarios:**
  - Happy path: `find()` with multiple recipes returns correct ingredients
    for each recipe (verify ingredient count and content per recipe)
  - Happy path: `create()` returns recipe with correct ingredients matching
    input data
  - Edge case: `find()` with recipes that have zero ingredients returns
    empty ingredient lists
  - Edge case: `find()` with mixed catalog and free-text ingredients maps
    correctly per recipe

  **Verification:**
  - All existing `PostgresRecipeRepositoryTest` tests pass
  - New tests verify batch loading returns correct ingredients per recipe
  - `create()` returns ingredients matching input data without re-querying

- [ ] **Unit 2: Validate recipe ownership in MealPlanService.create()**

  **Goal:** Add `RecipeRepository` to `MealPlanService`, check `isOwner()`
  before creating a meal plan item with a non-null `recipeId`.

  **Requirements:** R5, R6, R7

  **Dependencies:** None (independent of Unit 1)

  **Files:**
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/mealplan/MealPlanService.kt`
  - Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/mealplan/MealPlanModule.kt`
  - Test: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/mealplan/MealPlanServiceTest.kt`

  **Approach:**
  - Add `RecipeRepository` as second constructor parameter of
    `MealPlanService`. Koin auto-wires from existing `recipeModule`.
  - In `create()`: if `recipeId != null && !recipeRepository.isOwner(userId, recipeId)` return null.
  - Change return type from `MealPlanItem` to `MealPlanItem?`.
  - Update route handler in `MealPlanModule.kt` to check null → respond 404,
    following `ShoppingModule.kt` pattern.

  **Patterns to follow:**
  - `ShoppingListService.create()` — nullable return on ownership failure
  - `ShoppingModule.kt` `addItem` route — null-check pattern
  - `MealPlanService.update()` / `delete()` — existing isOwner guard style

  **Test scenarios:**
  - Happy path: create meal plan item with own recipe succeeds (201)
  - Happy path: create meal plan item with `recipeId = null` (note) succeeds
  - Error path: create meal plan item with another user's recipeId returns null / 404
  - Edge case: create meal plan item with non-existent recipeId returns null / 404

  **Verification:**
  - All existing `MealPlanServiceTest` tests pass
  - New ownership test uses two distinct users (createUser with different subjects)

- [ ] **Unit 3: Integration test for cross-user recipe IDOR**

  **Goal:** Route-layer integration test proving cross-user recipe reference
  returns 404.

  **Requirements:** R5, R6

  **Dependencies:** Unit 2

  **Files:**
  - Modify: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/mealplan/MealPlanRoutesTest.kt`

  **Approach:**
  - Generate two JWT tokens with different subjects via `TestJwt.generateToken()`.
  - Register both users via `POST /api/users/me`.
  - Create a recipe under user A.
  - User B attempts `POST /api/meal-plan` referencing user A's recipe.
  - Assert 404 response.
  - Also verify user A can still reference their own recipe (201).

  **Patterns to follow:**
  - Existing `MealPlanRoutesTest` HTTP test structure
  - `BaseIntegrationTest` + `jsonClient()` helper
  - `TestJwt.generateToken(subject)` for distinct identities

  **Test scenarios:**
  - Integration: user B's POST /api/meal-plan with user A's recipeId → 404
  - Integration: user A's POST /api/meal-plan with own recipeId → 201

  **Verification:**
  - Test passes with two distinct authenticated users
  - Existing meal plan integration tests still pass

## System-Wide Impact

- **Interaction graph:** `MealPlanService` gains a dependency on
  `RecipeRepository`. No circular dependency — meal plan depends on recipe,
  not vice versa.
- **Error propagation:** Ownership failure returns null from service → 404
  from route. Consistent with existing patterns.
- **API surface parity:** Response shapes unchanged. The only behavioral
  change is a new 404 for cross-user recipe references on create.
- **Unchanged invariants:** `findById()`, `findRandom()`, `update()`,
  `delete()` are not modified. All existing API behavior preserved.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| `inList` with empty list | `find()` always has ≥1 recipe before batch loading; guard with early return if `pageRows.isEmpty()` |
| TOCTOU between isOwner and INSERT | Acceptable — same-user operation, low probability. FK violation on concurrent delete would bubble as 500, same as any concurrent delete scenario |

## Sources & References

- **Origin document:** [docs/brainstorms/recipe-n+1-and-mealplan-idor-requirements.md](docs/brainstorms/recipe-n+1-and-mealplan-idor-requirements.md)
- Related issues: #42, #43
- Pattern reference: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingListService.kt`
- Pattern reference: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingModule.kt`
