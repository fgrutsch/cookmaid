---
title: "feat: Auto-resolve catalog items on exact name match"
type: feat
status: active
date: 2026-04-28
origin: docs/brainstorms/catalog-item-exact-match-requirements.md
---

# feat: Auto-resolve catalog items on exact name match

## Overview

When a user types an item name and presses Enter, resolve it to a catalog item
if the text exactly matches a catalog item name (case-insensitive, trimmed).
This prevents duplicate entries as both catalog references and free text,
preserving categorization and localization.

## Problem Frame

Users who type "Milk" and press Enter instead of selecting from the dropdown
create `Item.FreeText("Milk")` even though a catalog entry exists. This breaks
category grouping, localization, and deduplication across shopping lists and
recipes. (See origin: `docs/brainstorms/catalog-item-exact-match-requirements.md`)

## Requirements Trace

- R1. Exact case-insensitive match resolves to catalog item instead of free text
- R2. Match against the localized catalog name (current session locale)
- R3. Deterministic first-match on duplicate localized names
- R4. Apply to shopping list item input
- R5. Apply to recipe ingredient input
- R6. Silent resolution — no confirmation dialog

## Scope Boundaries

- Client-side resolution only — no new server endpoints
- Exact match only (trimmed, case-insensitive) — no fuzzy/partial matching
- No retroactive migration of existing free text items

## Context & Research

### Relevant Code and Patterns

- `CatalogItemRepository` interface (`composeApp/.../ui/catalog/CatalogItemRepository.kt`)
  exposes only `search(query)` which does substring matching capped at 5 results.
  `ApiCatalogItemRepository` caches the full catalog in-memory after first search.
- Both `ShoppingListScreen` and `AddRecipeComponents` construct `Item.FreeText`
  directly in the composable `onAddFreeText` lambda — the ViewModel receives
  a fully-constructed `Item`.
- MVI pattern: logic belongs in the ViewModel, not the composable.
- CLAUDE.md debounce warning: "do not also mutate the flow from a synchronous
  handler that calls the same side-effect directly" — the new event must not
  re-trigger the debounced search flow.
- `FakeCatalogItemRepository` exists in test sources and must be updated for
  any interface changes.

### Institutional Learnings

No `docs/solutions/` directory exists. Relevant patterns from CLAUDE.md noted above.

## Key Technical Decisions

- **Resolve in ViewModel, not composable**: The composable currently constructs
  `Item.FreeText` directly. Moving resolution to the ViewModel makes it testable,
  handles the cold-cache edge case (cache not yet populated), and avoids the
  5-result truncation problem with `state.suggestions`.
  (See origin: Outstanding Questions — resolved here.)

- **Add `findExactMatch` to CatalogItemRepository**: A dedicated method avoids
  reusing `search()` which truncates at 5 results and does substring matching.
  The new method operates on the full cached list with `equals(ignoreCase = true)`.

- **New events instead of modifying existing ones**: Add `AddItemByName(name)`
  and `AddIngredientByName(name, quantity)` events that accept raw text and let
  the ViewModel resolve. Keeps the existing `AddItem(Item)` / `AddIngredient(Item)`
  events unchanged for the dropdown selection path.

## Open Questions

### Resolved During Planning

- **Where to place resolution logic?** → ViewModel. Testable, handles cold cache,
  follows MVI convention. The composable sends raw text; the ViewModel resolves.
- **Cold-cache edge case?** → `findExactMatch` ensures cache is loaded (same
  mutex-guarded lazy-load pattern as `search()`). No additional eager loading needed.
- **Truncation risk from `search()`?** → Avoided by using `findExactMatch` on the
  full cached list instead of the 5-result `search()`.

### Deferred to Implementation

- Exact method/property naming — follow existing ViewModel conventions at
  implementation time.

## Implementation Units

- [ ] **Unit 1: Add `findExactMatch` to CatalogItemRepository**

  **Goal:** Provide a method to look up a catalog item by exact localized name.

  **Requirements:** R1, R2, R3

  **Dependencies:** None

  **Files:**
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/catalog/CatalogItemRepository.kt`
  - Modify: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/shopping/FakeCatalogItemRepository.kt`

  **Approach:**
  - Add `suspend fun findExactMatch(name: String): Item.Catalog?` to the interface.
  - In `ApiCatalogItemRepository`: ensure cache is populated (same `mutex` +
    `fetchAll` pattern as `search()`), then `firstOrNull` with
    `equals(name.trim(), ignoreCase = true)`.
  - In `FakeCatalogItemRepository`: same filtering logic on `items` list.

  **Patterns to follow:**
  - `ApiCatalogItemRepository.search()` for cache-loading pattern
  - `FakeCatalogItemRepository.search()` for fake implementation pattern

  **Test scenarios:**
  - Happy path: `findExactMatch("Milk")` returns the catalog item named "Milk"
  - Happy path: `findExactMatch("milk")` returns the catalog item named "Milk"
    (case-insensitive)
  - Happy path: `findExactMatch(" milk ")` returns "Milk" (trimmed)
  - Edge case: `findExactMatch("mil")` returns null (partial match, not exact)
  - Edge case: `findExactMatch("")` returns null
  - Edge case: `findExactMatch("xyznonexistent")` returns null
  - Edge case: two catalog items with same localized name → returns first by
    list order (deterministic)

  **Verification:**
  - Interface compiles, both implementations pass unit tests.

- [ ] **Unit 2: Shopping list — resolve on submit**

  **Goal:** When the user presses Enter/send in shopping list input, resolve
  exact catalog match before adding.

  **Requirements:** R1, R2, R3, R4, R6

  **Dependencies:** Unit 1

  **Files:**
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/shopping/ShoppingListContract.kt`
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/shopping/ShoppingListViewModel.kt`
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/shopping/ShoppingListScreen.kt`
  - Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/shopping/ShoppingListViewModelTest.kt`

  **Approach:**
  - Add `AddItemByName(val name: String)` event to `ShoppingListEvent`.
  - In ViewModel: handle the new event by calling
    `catalogItemRepository.findExactMatch(name)`. If match found, delegate to
    existing `addItem(match)`. If null, delegate to `addItem(Item.FreeText(name.trim()))`.
  - In `ShoppingListScreen`: change `onAddFreeText` lambda to dispatch
    `AddItemByName(state.searchQuery)` instead of constructing `Item.FreeText`.
  - The existing `AddItem(Item.Catalog)` event (dropdown selection) stays unchanged.

  **Patterns to follow:**
  - `ShoppingListViewModel.addItem()` for the existing add flow
  - `ShoppingListViewModel.selectTag()` for synchronous action that reads
    `state.value` directly (avoids double-fire with debounced flow)

  **Test scenarios:**
  - Happy path: dispatch `AddItemByName("Milk")` with "Milk" in fake catalog →
    item added as `Item.Catalog`
  - Happy path: dispatch `AddItemByName("milk")` (lowercase) → resolves to
    catalog item (case-insensitive)
  - Happy path: dispatch `AddItemByName("xyznonexistent")` → item added as
    `Item.FreeText`
  - Edge case: dispatch `AddItemByName("")` → no item added (blank guard)
  - Edge case: dispatch `AddItemByName(" milk ")` → resolves to catalog item
    (trimmed)
  - Integration: existing `AddItem(Item.Catalog)` from dropdown selection still
    works unchanged

  **Verification:**
  - All test scenarios pass. Existing shopping list tests still pass.

- [ ] **Unit 3: Recipe ingredients — resolve on submit**

  **Goal:** When the user presses Enter/send in recipe ingredient input, resolve
  exact catalog match before adding.

  **Requirements:** R1, R2, R3, R5, R6

  **Dependencies:** Unit 1

  **Files:**
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/edit/AddRecipeContract.kt`
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/edit/AddRecipeViewModel.kt`
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/edit/AddRecipeComponents.kt`
  - Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/edit/AddRecipeViewModelTest.kt`

  **Approach:**
  - Add `AddIngredientByName(val name: String, val quantity: String?)` event to
    `AddRecipeEvent`.
  - In ViewModel: handle by calling `findExactMatch(name)`, then delegate to
    existing `addIngredient(match ?: Item.FreeText(name.trim()), quantity)`.
  - In `AddRecipeComponents`: change `onAddFreeText` lambda to dispatch
    `AddIngredientByName(ingredientQuery, quantity)` instead of constructing
    `Item.FreeText`. Preserve the existing `onQuantityInputClear()` call after
    dispatching the event.
  - The existing `AddIngredient(Item.Catalog, quantity)` event stays unchanged.

  **Patterns to follow:**
  - `AddRecipeViewModel.addIngredient()` for existing add flow
  - Unit 2's approach (identical pattern, different surface)

  **Test scenarios:**
  - Happy path: dispatch `AddIngredientByName("Milk", "200ml")` with "Milk" in
    fake catalog → ingredient added as `Item.Catalog` with quantity
  - Happy path: dispatch `AddIngredientByName("xyznonexistent", null)` →
    ingredient added as `Item.FreeText`
  - Edge case: dispatch `AddIngredientByName("", null)` → no ingredient added
  - Edge case: dispatch `AddIngredientByName("milk", "1L")` → resolves to
    catalog item (case-insensitive), quantity preserved
  - Integration: existing `AddIngredient(Item.Catalog, quantity)` from dropdown
    still works unchanged

  **Verification:**
  - All test scenarios pass. Existing recipe tests still pass.

## System-Wide Impact

- **Interaction graph:** Only composable → ViewModel → CatalogItemRepository.
  No callbacks, middleware, or observers affected.
- **Error propagation:** `findExactMatch` cannot fail in a user-visible way — on
  error or cold cache, the existing `FreeText` fallback applies.
- **State lifecycle risks:** None. The catalog cache is session-scoped (Koin
  `sessionModules`). No new state introduced.
- **API surface parity:** No server changes. The client sends `catalogItemId`
  or `freeTextName` as before — only the decision of which to send changes.
- **Unchanged invariants:** Server API, database schema, `Item` sealed interface,
  existing `AddItem`/`AddIngredient` events all unchanged.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Cold cache on very fast paste+Enter | `findExactMatch` loads cache if empty (same lazy pattern as `search()`). Worst case: brief wait on first submit. |
| Locale mismatch if user changes locale mid-session | Pre-existing issue — catalog cache is session-scoped, not locale-scoped. Out of scope per requirements. |

## Sources & References

- **Origin document:** [docs/brainstorms/catalog-item-exact-match-requirements.md](docs/brainstorms/catalog-item-exact-match-requirements.md)
- Related code: `CatalogItemRepository`, `ShoppingListViewModel`, `AddRecipeViewModel`
- Related issue: #80
