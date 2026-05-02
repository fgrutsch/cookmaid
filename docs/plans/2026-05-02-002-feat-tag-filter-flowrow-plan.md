---
title: "feat: Replace horizontal tag filter with FlowRow wrapping layout"
type: feat
status: active
date: 2026-05-02
---

# feat: Replace horizontal tag filter with FlowRow wrapping layout

## Overview

Replace the horizontal scrollable `Row` of `FilterChip` components in the
recipe list screen with a `FlowRow` wrapping layout. Shows all tags at a
glance without horizontal scrolling.

## Problem Frame

When users have many recipe tags, the horizontal scrollable chip row
requires tedious scrolling to find and select a tag. Tags beyond the
visible area are not discoverable at a glance. `FlowRow` wraps chips to
multiple lines, showing all tags simultaneously.

## Requirements Trace

- R1. All tags visible without horizontal scrolling
- R2. Existing single-select toggle behavior preserved
- R3. Works correctly on narrow and wide screens

## Scope Boundaries

- No changes to tag data model, API, or ViewModel logic
- No multi-select tag filtering (stays single-select)
- No tag search/autocomplete

## Context & Research

### Relevant Code and Patterns

- `composeApp/.../ui/recipe/list/RecipeListComponents.kt` lines 141-161 —
  current `TagFilterRow` with horizontal `Row` + `horizontalScroll`
- `composeApp/.../ui/recipe/list/RecipeListContract.kt` — `availableTags`,
  `selectedTag`, `SelectTag` event
- `composeApp/.../ui/recipe/list/RecipeListViewModel.kt` — `selectTag()`
  toggle logic, unchanged
- `composeApp/.../ui/recipe/list/RecipeListScreen.kt` — wires tag click

## Key Technical Decisions

- **FlowRow over BottomSheet/Dropdown**: Shows all tags without an extra
  tap. Material Design best practice for filter chips. `FlowRow` is
  available in `androidx.compose.foundation.layout`.
- **Replace in-place**: Modify `TagFilterRow` composable only. No
  signature changes needed — it already receives `availableTags`,
  `selectedTag`, and `onTagClick`.

## Implementation Units

- [ ] **Unit 1: Replace Row with FlowRow in TagFilterRow**

  **Goal:** Show all tag filter chips in a wrapping layout.

  **Requirements:** R1, R2, R3

  **Dependencies:** None

  **Files:**
  - Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListComponents.kt`

  **Approach:**
  - Replace `Row(modifier = Modifier.horizontalScroll(...))` with
    `FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp))`
  - Keep `FilterChip` components and their click behavior unchanged
  - Remove `rememberScrollState` import if unused elsewhere
  - Add `FlowRow` import from `androidx.compose.foundation.layout`

  **Patterns to follow:**
  - Existing `FilterChip` usage in the same file

  **Test scenarios:**
  - Test expectation: none — pure layout change. ViewModel logic and
    toggle behavior are unchanged and already tested in
    `RecipeListViewModelTest`.

  **Verification:**
  - Tags wrap to multiple lines when they exceed screen width
  - Single-select toggle still works
  - `./gradlew detektAll` passes

## System-Wide Impact

- **Interaction graph:** No changes — same events, same ViewModel
- **State lifecycle:** No changes
- **API surface parity:** No changes

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| FlowRow may look different with very few tags (1-2) | Acceptable — single row when few tags, wraps only when needed |

## Sources & References

- Related issue: #28
