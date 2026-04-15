---
title: "Don't reset a debounced StateFlow when a synchronous trigger also runs the same side-effect"
date: 2026-04-15
problem_type: logic-error
track: knowledge
category: best-practices
module: composeApp
component: RecipeListViewModel
tags:
  - kotlin
  - coroutines
  - stateflow
  - debounce
  - mvi
---

# Don't reset a debounced StateFlow when a synchronous trigger also runs the same side-effect

## Context

MVI ViewModels frequently pair a debounced `MutableStateFlow` with a synchronous
imperative action that produces the *same* side-effect (e.g. "refetch the list").
Writing to the flow AND calling the action in the same code path fires the
side-effect twice — once synchronously, once after the debounce window.

This bit us in `RecipeListViewModel.setSearchActive(false)`: closing the search
bar both wrote `""` to `searchQueryFlow` and called `fetchFirstPage()`. The
flow's subscriber debounced 300 ms then also called `fetchFirstPage()`, so every
close produced two identical network requests.

## Guidance

When a handler runs a side-effect synchronously, do not also mutate the flow
that drives the debounced path to the same side-effect. Let the flow serve only
the user-typing path; let synchronous user actions (close search, clear filter,
select tag) bypass the flow and call the side-effect directly.

Before:
```kotlin
private fun setSearchActive(active: Boolean) {
    if (active) {
        updateState { copy(searchActive = true) }
    } else {
        updateState { copy(searchActive = false, searchQuery = "") }
        searchQueryFlow.value = ""   // triggers debounced fetchFirstPage 300ms later
        fetchFirstPage()             // also fetches synchronously — duplicate request
    }
}
```

After:
```kotlin
private fun setSearchActive(active: Boolean) {
    if (active) {
        updateState { copy(searchActive = true) }
    } else {
        updateState { copy(searchActive = false, searchQuery = "") }
        fetchFirstPage()
    }
}
```

`fetchFirstPage()` reads the search term from `state.value.searchQuery`, which
is already cleared by `updateState`, so the direct call is correct on its own.

## Why This Matters

- Every duplicate request wastes a server round-trip per user action.
- Races: a slow second response can overwrite a faster first, producing UI flicker.
- `MutableStateFlow` emits on distinct values — so the double-fire only happens
  when the flow value actually changes, making it easy to miss in local testing
  (it requires a prior non-empty search).

## When to Apply

Any ViewModel where a `MutableStateFlow` + `.debounce(...)` chain triggers the
same side-effect that a synchronous handler in the same class calls directly.
Look for: `flow.value = X` and a direct call to the same function on the next
line.

The canonical shape in this codebase is already in `selectTag()` — mutate
state, call the side-effect, leave the flow alone.

## Examples

Test the regression with a call-count assertion rather than only checking state:

```kotlin
// FakeRecipeRepository gains a simple counter
var fetchPageCallCount: Int = 0
override suspend fun fetchPage(...) {
    fetchPageCallCount++
    // ...
}

@Test
fun `closing search triggers exactly one recipe fetch`() = viewModelTest {
    val viewModel = createLoadedViewModel(recipes = listOf(recipe("Pasta")))
    viewModel.onEvent(RecipeListEvent.UpdateSearchQuery("pa"))
    advanceUntilIdle()
    val afterSearch = fakeRecipeRepo.fetchPageCallCount

    viewModel.onEvent(RecipeListEvent.SetSearchActive(false))
    advanceUntilIdle()

    assertEquals(1, fakeRecipeRepo.fetchPageCallCount - afterSearch)
}
```

State-only assertions pass in both the buggy and fixed versions. A call-count
counter on the fake is the minimum tool needed to catch "one side-effect, two
fires."

## Related

- Issue: #57
- PR: #72
