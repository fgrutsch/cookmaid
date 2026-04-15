---
title: "fix: Remove redundant searchQueryFlow assignment in setSearchActive"
type: fix
status: active
date: 2026-04-15
---

# fix: Remove redundant searchQueryFlow assignment in setSearchActive

## Overview

Closing the search bar currently fires two identical recipe-list network requests:
one from the debounced `searchQueryFlow` reacting to the cleared query, and one
from an immediate `fetchFirstPage()` call. Remove the redundant
`searchQueryFlow.value = ""` assignment so exactly one request is issued.

## Problem Frame

In `RecipeListViewModel.setSearchActive(false)` the code both writes `""` to
`searchQueryFlow` (which triggers a debounced reload after 300 ms) and
synchronously calls `fetchFirstPage()`. The debounced request is pure waste —
the user already has the synchronous result on screen by the time it lands,
and it causes a duplicate server hit per close-search action.

## Requirements Trace

- R1. Closing the search bar triggers exactly one recipe list reload.
- R2. Debounced search (typing into the search field) continues to work as before.

## Scope Boundaries

- Not changing the `updateSearchQuery` typing path — it still uses the debounced flow.
- Not restructuring the debounce/flow setup.
- Not touching UI composables.

## Context & Research

### Relevant Code and Patterns

- `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListViewModel.kt:122-130` — the method being fixed.
- `fetchFirstPage()` (same file, lines 77-92) reads search from `state.value.searchQuery`, so after `updateState { copy(searchQuery = "") }` it correctly fetches the unfiltered first page.
- `selectTag()` (line 132) already uses the same pattern (mutate state, then call `fetchFirstPage()` without touching the flow) — that is the canonical shape to align with.

### Institutional Learnings

None directly applicable.

## Key Technical Decisions

- **Drop the flow write, keep the direct `fetchFirstPage()`** — the flow exists for debounced typing; synchronous user actions (close search, select tag) already bypass it everywhere else in this ViewModel. Matching that pattern is consistent and avoids the duplicate request.

## Implementation Units

- [ ] **Unit 1: Remove redundant flow assignment**

**Goal:** Close-search triggers exactly one reload.

**Requirements:** R1, R2

**Dependencies:** None

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/FakeRecipeRepository.kt` (add a `fetchPageCallCount` counter to enable the regression test)
- Test: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListViewModelTest.kt`

**Approach:**
- Delete line 127 (`searchQueryFlow.value = ""`) from `setSearchActive(false)`.
- Leave the `updateState { copy(searchActive = false, searchQuery = "") }` and `fetchFirstPage()` calls in place — they match the `selectTag` pattern.
- Add a simple `fetchPageCallCount` counter on `FakeRecipeRepository` (incremented in `fetchPage`) so the test can assert call count without elaborate mocking.

**Patterns to follow:**
- `selectTag()` in the same ViewModel — state update followed by direct `fetchFirstPage()`.

**Test scenarios:**
- Happy path: after opening search, typing a query, and setting search inactive, only one additional `fetchPage` call occurs (proves the debounced flow is no longer firing on close).
- Happy path: typing into the search query still triggers one `fetchPage` after the 300 ms debounce (regression guard — confirms the debounced path is intact).

**Verification:**
- `./gradlew :composeApp:allTests` passes.
- `./gradlew detektAll` passes.

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Debounced search regresses because we touched flow-adjacent code | Keep `updateSearchQuery` untouched; regression test for typing path. |

## Sources & References

- Related issue: #57
- Related PR: #72
