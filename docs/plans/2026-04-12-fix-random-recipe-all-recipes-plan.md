---
title: "fix: random recipe picks from all recipes via server endpoint"
type: fix
status: active
date: 2026-04-12
origin: docs/brainstorms/2026-04-12-random-recipe-all-recipes-brainstorm.md
---

# fix: random recipe picks from all recipes via server endpoint

Random recipe only picks from loaded pages (page size 20). Add a
server-side `GET /api/recipes/random` endpoint so it picks from all
user recipes.

## Acceptance Criteria

- [x] `GET /api/recipes/random` endpoint returns a random `Recipe`
  for the authenticated user
- [x] Optional `excludeId` query param prevents immediate repeats
- [x] Optional `tag` query param respects active tag filter
- [x] Returns 404 when user has zero recipes (or none matching tag)
- [x] If `excludeId` filters out the only candidate, ignore the
  exclusion and return it anyway (no empty result when recipes exist)
- [x] Client calls server endpoint instead of picking from
  in-memory list
- [x] Loading state shown during network call (replaces what was
  a synchronous in-memory operation)
- [x] All existing tests pass
- [x] Server and client tests added

## Context

### Server changes (layered: route → service → repository)

#### RecipeRepository.kt — add `findRandom`

```kotlin
suspend fun findRandom(
    userId: UserId,
    tag: String? = null,
    excludeId: Uuid? = null,
): Recipe?
```

SQL: `SELECT ... WHERE user_id = ? [AND tag = ?] [AND id != ?]
ORDER BY RANDOM() LIMIT 1`. Returns null when no match.

If `excludeId` filters out the only candidate, fall back: first
query with exclusion, if null then second query without exclusion.

#### RecipeService.kt — add `findRandom`

```kotlin
suspend fun findRandom(
    userId: UserId,
    tag: String? = null,
    excludeId: Uuid? = null,
): Recipe?
```

Thin pass-through to repository — no ownership check needed since
the query already filters by `userId`.

#### RecipeModule.kt — add route

```kotlin
get("/random") {
    val tag = call.queryParameters["tag"]
    val excludeId = call.queryParameters["excludeId"]
        ?.let { Uuid.parse(it) }
    val recipe = recipeService.findRandom(
        call.userId(), tag, excludeId
    )
    if (recipe != null) call.respond(recipe)
    else call.respond(HttpStatusCode.NotFound)
}
```

Register **before** `get("/{recipeId}")` to avoid path collision
where "random" is interpreted as a recipe ID.

### Client changes

#### RecipeClient.kt — add `fetchRandom`

```kotlin
suspend fun fetchRandom(tag: String? = null, excludeId: String? = null): Recipe
```

`GET /api/recipes/random?tag=...&excludeId=...`

#### RecipeRepository.kt (composeApp) — add `fetchRandom`

```kotlin
suspend fun fetchRandom(tag: String? = null, excludeId: String? = null): Recipe
```

#### RecipeListViewModel.kt — update `rollRandomRecipe`

Replace in-memory pick with server call:

```kotlin
private fun rollRandomRecipe() {
    launch {
        updateState { copy(isLoadingRandom = true) }
        try {
            val current = state.value.randomRecipe
            val tag = state.value.selectedTag
            val recipe = recipeRepository.fetchRandom(
                tag = tag,
                excludeId = current?.id?.toString(),
            )
            updateState { copy(randomRecipe = recipe, isLoadingRandom = false) }
        } catch (e: Exception) {
            updateState { copy(isLoadingRandom = false) }
        }
    }
}
```

#### RecipeListContract.kt — add loading state

```kotlin
data class RecipeListState(
    // ... existing fields
    val isLoadingRandom: Boolean = false,
)
```

### Design decisions (see brainstorm)

- Server-side `ORDER BY RANDOM() LIMIT 1` — simple, correct for
  this dataset size
- `excludeId` avoids consecutive repeats — single param sufficient
- Full `Recipe` returned — no second fetch needed
- `tag` param (singular) matches existing list endpoint convention

### Test files

- `server/src/test/kotlin/.../recipe/RecipeRoutesTest.kt` — add
  random endpoint tests (200, 404, excludeId, tag filter)
- `composeApp/src/commonTest/.../recipe/list/RecipeListViewModelTest.kt`
  — update random recipe tests to use fake repository
- `composeApp/src/commonTest/.../recipe/FakeRecipeRepository.kt`
  — add `fetchRandom` implementation

## Sources

- **Origin brainstorm:**
  [docs/brainstorms/2026-04-12-random-recipe-all-recipes-brainstorm.md](docs/brainstorms/2026-04-12-random-recipe-all-recipes-brainstorm.md)
- Existing routes: `server/.../recipe/RecipeModule.kt:26-80`
- Existing service: `server/.../recipe/RecipeService.kt`
- Existing repository: `server/.../recipe/RecipeRepository.kt`
- Client ViewModel: `composeApp/.../recipe/list/RecipeListViewModel.kt:138-144`
- Related issue: #27
