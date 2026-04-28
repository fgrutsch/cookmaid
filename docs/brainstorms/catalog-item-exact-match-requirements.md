---
date: 2026-04-28
topic: catalog-item-exact-match
---

# Auto-Resolve Catalog Items on Exact Name Match

## Problem Frame

When a user types an item name that exactly matches a catalog item (e.g. "Milk"
or "milk") and presses Enter without selecting from the dropdown, the item is
added as free text. This means identical items end up as both catalog references
and free text entries, breaking categorization, localization, and deduplication.

## Requirements

**Matching**
- R1. When the user submits a text input that exactly matches a catalog item
  name (case-insensitive), resolve it to the catalog item instead of creating
  a free text item.
- R2. Matching must respect the current locale — compare against the localized
  catalog name the user sees.
- R3. If multiple catalog items share the same localized name (unlikely but
  possible), pick the first match deterministically.

**Affected Surfaces**
- R4. Apply to shopping list item input (AddItemField in ShoppingListComponents).
- R5. Apply to recipe ingredient input (IngredientAddField in AddRecipeComponents).

**UX**
- R6. Resolution is silent — no confirmation dialog. The user sees the
  resolved catalog item appear (with its category) just as if they had
  selected it from the dropdown.

## Success Criteria

- Typing "milk" + Enter in shopping list or recipe adds a catalog `Item.Catalog`
  referencing the "Milk" catalog entry, not an `Item.FreeText("milk")`.
- Typing "xyznonexistent" + Enter still creates a free text item.
- Existing dropdown selection flow is unchanged.
- If multiple catalog items share the same localized name, the first match
  by catalog query order is consistently selected (verified by test).

## Scope Boundaries

- Server-side resolution is out of scope — resolve client-side using the
  already-cached catalog items.
- No fuzzy or partial matching — exact match only (trimmed, case-insensitive).
- No retroactive migration of existing free text items that match catalog names.

## Key Decisions

- **Client-side resolution**: The catalog items are already cached in memory
  on the client (`ApiCatalogItemRepository`). Resolving there avoids a new
  server endpoint and gives instant feedback.

## Dependencies / Assumptions

- Catalog item cache is populated before the user can submit items. The current
  lazy-load on first search satisfies this — search triggers before Enter.

## Outstanding Questions

### Deferred to Planning

- [Affects R1][Technical] Where exactly to insert the resolution logic — in the
  ViewModel or in the composable event handler.

## Next Steps

-> `/ce:plan` for structured implementation planning
