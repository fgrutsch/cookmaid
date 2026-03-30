---
title: Switch quantity to text and add servings field
type: feat
status: active
date: 2026-03-30
---

# Switch Quantity to Text and Add Servings Field

Change `quantity` from `Float?` to `String?` in recipe ingredients and
shopping items so users can enter free-form text (e.g. "2 cups", "1 tbsp").
Add a nullable `servings` field (`Int?`) to recipes.

## Acceptance Criteria

- [ ] DB migration: ALTER `quantity` from REAL to TEXT in both tables
- [ ] DB migration: ADD `servings` (INTEGER, nullable) to `recipes`
- [ ] Exposed tables updated: `quantity` → `text`, add `servings`
- [ ] Shared DTOs: `quantity: Float?` → `String?` everywhere
- [ ] Shared DTOs: `Recipe` gains `servings: Int?`
- [ ] Request types: quantity → String?, add servings to recipe requests
- [ ] Server repository/service: handle String quantity and servings
- [ ] UI: remove float-only input filtering, accept free text for quantity
- [ ] UI: add servings input to recipe create/edit form
- [ ] UI: display servings on recipe detail screen
- [ ] Existing tests pass

## Affected Files

### Database
- New migration: `V7__quantity_text_and_servings.sql`

### Server (Exposed tables + repository)
- `server/.../recipe/RecipeRepository.kt` — table + queries
- `server/.../shopping/ShoppingListRepository.kt` — table + queries

### Shared DTOs
- `shared/.../recipe/Recipe.kt` — add servings
- `shared/.../recipe/RecipeIngredient.kt` — quantity type
- `shared/.../recipe/RecipeRequests.kt` — quantity + servings
- `shared/.../shopping/ShoppingItem.kt` — quantity type
- `shared/.../shopping/ShoppingRequests.kt` — quantity type

### UI
- `composeApp/.../recipe/edit/AddRecipeComponents.kt` — quantity input, servings input
- `composeApp/.../recipe/edit/AddRecipeViewModel.kt` — state changes
- `composeApp/.../recipe/edit/AddRecipeContract.kt` — state/event types
- `composeApp/.../recipe/edit/AddRecipeScreen.kt` — servings field
- `composeApp/.../recipe/detail/RecipeDetailScreen.kt` — display servings
- `composeApp/.../shopping/ShoppingListDialogs.kt` — quantity input
- `composeApp/.../shopping/ShoppingListComponents.kt` — quantity display
- Remove `formatQuantity()` helper (no longer needed)

## Sources

- Related issue: #24
