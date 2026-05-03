# Random Recipe Duplicates Fix

## Problem

When re-rolling random recipes, users frequently see duplicates because the
`excludeId` parameter only excludes the currently displayed recipe. Previously
shown recipes can reappear immediately.

## Solution

Change from single `excludeId` to multi-value `excludeIds`. The client
accumulates all shown recipe IDs and sends them on each re-roll. The server
excludes all of them. When every recipe has been shown, the server falls back
to returning any random recipe (restarting the cycle).

## Server Changes

### Route (`RecipeModule.kt`)

- Replace query parameter `excludeId` (single `Uuid?`) with `excludeIds`
  (comma-separated list of UUIDs, parsed to `List<Uuid>`).
- Pass the list to `RecipeService.findRandom()`.

### Service (`RecipeService.kt`)

- Change `findRandom` signature: `excludeId: Uuid?` becomes
  `excludeIds: List<Uuid>`.

### Repository (`RecipeRepository.kt`)

- Change `findRandom` signature: `excludeId: Uuid?` becomes
  `excludeIds: List<Uuid>`.
- Replace single `NeqOp` with Exposed's `notInList(excludeIds)` when
  `excludeIds` is non-empty.
- Fallback: if `excludeIds` is non-empty and no results, retry without
  exclusion (same pattern as current single-exclude fallback).

## Client Changes

### State (`RecipeListContract.kt`)

- Add `shownRecipeIds: Set<String> = emptySet()` to `RecipeListState`.

### ViewModel (`RecipeListViewModel.kt`)

- `rollRandomRecipe()`: after fetching, add the new recipe's ID to
  `shownRecipeIds`. Pass all `shownRecipeIds` as `excludeIds`.
- `ClearRandomRecipe`: reset `shownRecipeIds` to empty.
- Tag filter change (`selectTag`): reset `shownRecipeIds` (different pool).

### Client (`RecipeClient.kt`)

- Change `fetchRandom(tag, excludeId)` to
  `fetchRandom(tag, excludeIds: List<String>)`.
- Send `excludeIds` as comma-separated query parameter (only when non-empty).

### Repository (`RecipeRepository.kt` in composeApp)

- Propagate the signature change through the repository interface and
  implementation.

## Edge Cases

- **All recipes shown**: Server fallback returns any random recipe. Client
  does NOT reset `shownRecipeIds` — the server handles exhaustion gracefully.
- **Single recipe**: Fallback returns the same recipe. Same behavior as today.
- **Tag change**: Resets `shownRecipeIds` since the recipe pool changes.
- **Empty excludeIds list**: Server treats as no exclusion (same as today
  with null `excludeId`).

## Testing

### Server

- Existing tests updated for `excludeIds` parameter name.
- New test: multiple `excludeIds` excludes all specified recipes.
- Fallback test: all recipes excluded returns a random recipe anyway.

### Client

- ViewModel test: `shownRecipeIds` accumulates across re-rolls.
- ViewModel test: `ClearRandomRecipe` resets `shownRecipeIds`.
- ViewModel test: tag change resets `shownRecipeIds`.
