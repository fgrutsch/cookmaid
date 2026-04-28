---
title: "Client-side catalog item exact-match resolution pattern"
module: composeApp
date: 2026-04-28
problem_type: best_practice
component: development_workflow
severity: low
tags:
  - catalog
  - mvi
  - viewmodel
  - exact-match
  - compose
applies_when:
  - "Adding items by text input where a catalog of known items exists"
  - "Need to resolve user-typed text to structured entries before persisting"
---

# Client-side catalog item exact-match resolution

## Context

Users typing item names and pressing Enter bypassed catalog matching,
creating duplicate entries as both catalog references and free text. The
dropdown selection path worked correctly but the keyboard-submit path did not.

## Guidance

Add a `findExactMatch(name)` method to the repository interface that operates
on the full cached item list (not the truncated search results). In the
ViewModel, create a new event (e.g. `AddItemByName(name)`) that calls
`findExactMatch` and falls back to `Item.FreeText` if no match is found.

Key implementation details:
- Use `lowercase()` comparison (not `equals(ignoreCase=true)`) for consistent
  case-folding across locales — matches the existing `search()` pattern
- Clear UI state (query, suggestions) synchronously before `launch{}` to
  prevent double-submit during cold cache fetches
- Extract a shared `ensureCacheLoaded()` method to avoid duplicating the
  mutex + fetchAll pattern

## Why This Matters

Resolving in the ViewModel (not the composable) keeps logic testable, handles
the cold-cache edge case, and avoids the 5-result truncation problem with
the suggestion dropdown.

## Examples

The pattern is implemented in both `ShoppingListViewModel.addItemByName()`
and `AddRecipeViewModel.addIngredientByName()` — identical approach, different
surfaces.
