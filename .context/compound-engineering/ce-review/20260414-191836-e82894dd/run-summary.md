# Code Review Run Summary

- **Run ID:** 20260414-191836-e82894dd
- **Mode:** autofix
- **Plan:** docs/plans/2026-04-14-001-fix-recipe-n1-mealplan-idor-plan.md
- **Branch:** fgrutsch/fix/idor-meal-plan-create-does-not-validate-recipe-own-issue-43
- **Date:** 2026-04-14

## Reviewers Dispatched

| Reviewer | Status | Findings |
|----------|--------|----------|
| correctness-reviewer | completed | 3 |
| testing-reviewer | completed | 2 |
| security-reviewer | completed | 0 (advisory only) |
| project-standards-reviewer | completed | 1 |

## Findings Summary

### Applied (safe_auto)

1. **P2 — Absolute package references in MealPlanRoutesTest.kt**
   - `autofix_class: safe_auto`
   - Replaced `io.github.fgrutsch.cookmaid.recipe.CreateRecipeRequest` and
     `io.github.fgrutsch.cookmaid.recipe.Recipe` inline references with
     proper imports
   - Commit: `style: replace absolute package references with imports in MealPlanRoutesTest`

### Unresolved (downstream)

2. **P1 — `create()` returns client-supplied ingredient names instead of DB-resolved locale names**
   - `autofix_class: gated_auto` → `owner: downstream-resolver`
   - `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRepository.kt:243`
   - The plan intentionally chose this behavior ("The locale the client used
     is authoritative for the create response"). This is a design decision,
     not a bug. The reviewer flagged it because subsequent `find()` calls
     would return DB-resolved names, creating inconsistency between create
     and list responses for the same recipe.
   - **Assessment:** Plan-intentional. Low practical impact — client sends
     the locale-appropriate name it displayed. No action needed unless
     locale-switching on create is supported in the future.

3. **P2 — `find()` batch-load path lacks ingredient content assertions**
   - `autofix_class: manual` → `owner: downstream-resolver`
   - `server/src/test/kotlin/io/github/fgrutsch/cookmaid/recipe/PostgresRecipeRepositoryTest.kt`
   - Existing tests verify ingredient counts but do not assert ingredient
     names or categories after batch loading.

4. **P2 — `loadIngredientsBatch` Catalog branch not exercised via `find()` test**
   - `autofix_class: manual` → `owner: downstream-resolver`
   - Tests create recipes with catalog ingredients via `create()` but
     `find()` tests only assert list size, not ingredient type/content.

### Advisory

5. **P3 — TOCTOU race between isOwner check and INSERT**
   - `autofix_class: advisory`
   - Plan acknowledges this risk and accepts it. Same-user operation,
     low probability. FK violation on concurrent delete bubbles as 500.

6. **Security residual — `update()` could gain recipeId field in future**
   - `autofix_class: advisory`
   - If `UpdateMealPlanItemRequest` ever adds a `recipeId` field,
     ownership must be validated there too. Currently not a concern.

7. **Security residual — `findById()` does not filter by userId**
   - `autofix_class: advisory`
   - Pre-existing pattern. Not in scope for this PR.

## Conclusion

1 safe_auto fix applied. 1 gated finding assessed as plan-intentional (no
action). 2 manual testing findings for downstream consideration. 3 advisory
items noted.
