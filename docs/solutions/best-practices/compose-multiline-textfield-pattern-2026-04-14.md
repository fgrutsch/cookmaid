---
title: Compose OutlinedTextField multi-line pattern
date: 2026-04-14
category: best-practices
module: composeApp
problem_type: best_practice
component: tooling
severity: low
applies_when:
  - Converting a single-line text input to multi-line in Compose Multiplatform
  - Adding textarea-style input to a dialog or form
tags:
  - compose
  - outlinedtextfield
  - multi-line
  - textarea
  - meal-plan
---

# Compose OutlinedTextField multi-line pattern

## Context

Compose `OutlinedTextField` defaults to single-line input. Converting to
multi-line requires removing keyboard action configuration and adjusting
the `singleLine`/`maxLines` parameters.

## Guidance

Replace `singleLine = true` with `singleLine = false, maxLines = N`.
Remove `keyboardOptions` (ImeAction.Done) and `keyboardActions` — the
default multi-line behavior uses Enter for newlines, which is correct.

Existing pattern in this codebase:
`AddRecipeComponents.kt` recipe description field (lines 97-104).

## Why This Matters

- `ImeAction.Done` conflicts with multi-line: it prevents Enter from
  inserting newlines
- `keyboardController?.hide()` in `onDone` is no longer needed when
  submit is button-only
- Trim-on-save happens in the ViewModel, not the TextField — no UI
  change needed for whitespace handling

## When to Apply

- Converting any single-line `OutlinedTextField` to multi-line
- Adding new multi-line text inputs to dialogs

## Examples

Before:
```kotlin
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    singleLine = true,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
)
```

After:
```kotlin
OutlinedTextField(
    value = text,
    onValueChange = { text = it },
    singleLine = false,
    maxLines = 5,
)
```

## Related

- Issue #66 — meal plan note textarea
