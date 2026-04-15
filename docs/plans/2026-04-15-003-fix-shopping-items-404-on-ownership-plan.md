---
title: "fix: Return 404 from GET shopping items for non-owned lists"
type: fix
status: active
date: 2026-04-15
---

# fix: Return 404 from GET shopping items for non-owned lists

## Overview

`GET /api/shopping-lists/{listId}/items` returns `200 OK []` when the caller
does not own the list — indistinguishable from an owned empty list, and
divergent from every other ownership check in the codebase (which all return
404). Align this endpoint with the project's established pattern.

## Problem Frame

`ShoppingListService.findItemsByListId()` returns `emptyList()` on ownership
failure. The route relays that unconditionally. The Android/web UIs never
hit this path today, but the API contract quietly leaks "list does not exist
for this user" as "list exists and is empty" — and the inconsistency makes
future routes susceptible to copying the wrong shape.

## Requirements Trace

- R1. `GET /api/shopping-lists/{id}/items` for a list not owned by the caller → 404.
- R2. `GET /api/shopping-lists/{id}/items` for an owned empty list → 200 `[]`.
- R3. Existing clients unaffected (they only call with owned list IDs).

## Scope Boundaries

- Only `ShoppingListService.findItemsByListId`. An audit of `server/src/main/kotlin` confirmed no other service returns `emptyList()` on ownership failure — collection endpoints like `findLists`, `RecipeService.find`, `findTags`, `MealPlanService.find` scope by `userId` directly and don't take a parent ID, so they can't leak ownership this way.
- Not restructuring the repository layer.

## Key Technical Decisions

- **Return `List<ShoppingItem>?` and route maps `null` → 404.** This is the canonical shape in the rest of the codebase (see CLAUDE.md server module guidance). `Boolean` doesn't fit here because success carries the list payload.

## Implementation Units

- [ ] **Unit 1: Nullable return on ownership failure**

**Goal:** Non-owned list returns 404; owned empty list returns 200 `[]`.

**Requirements:** R1, R2, R3

**Files:**
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingListService.kt`
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingModule.kt`
- Test: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingListServiceTest.kt`
- Test: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingRoutesTest.kt`

**Approach:**
- `ShoppingListService.findItemsByListId` signature: `List<ShoppingItem>?`; return `null` on `!isListOwner`.
- Update the KDoc `@return` accordingly.
- `ShoppingModule.kt` GET `/items` route: `if (items == null) respond(404) else respond(items)`.
- Rename existing service test `findItemsByListId returns empty for another users list` → `... returns null ...` and assert `assertNull(items)`.
- Add a routes test: two JWT subjects, user A creates a list, user B GETs `/items` on it → 404.

**Patterns to follow:**
- `ShoppingListService.addItem` (already returns `ShoppingItem?` and the route maps null → 404).
- `MealPlanService`-style two-subject JWT test in `ShoppingRoutesTest.kt` if present, or `TestJwt.generateToken(subject)` per CLAUDE.md.

**Test scenarios:**
- Service: owner → returns items (happy path).
- Service: non-owner → returns null (error path).
- Route: owner → 200 with items (happy path).
- Route: non-owner (other JWT subject) → 404 (error path, contract-level).
- Route: owned empty list → 200 `[]` (edge case — guards against "fix changes happy-path semantics").

**Verification:**
- `./gradlew :server:test` passes.
- `./gradlew detektAll` passes.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Android/web client relies on `200 []` behavior | Issue's AC confirms they only call with owned IDs; a grep of `composeApp` for `/items` confirms only owned-list flows. |

## Sources & References

- Issue: #45
- PR: #75
