---
title: "feat: Use textarea for meal plan notes"
type: feat
status: active
date: 2026-04-14
origin: docs/brainstorms/2026-04-14-001-meal-plan-note-textarea-requirements.md
---

# feat: Use textarea for meal plan notes

## Overview

Change the meal plan note input from a single-line text field to a
multi-line textarea in both the add and edit dialogs, and display
multi-line notes properly in the meal plan item list.

## Problem Frame

Meal plan notes are single-line only. Users want multi-line notes for
ingredient lists, quick instructions, or multiple items per note.
(see origin: docs/brainstorms/2026-04-14-001-meal-plan-note-textarea-requirements.md)

## Requirements Trace

- R1. Replace single-line `OutlinedTextField` with multi-line in add and edit dialogs
- R2. Enter key inserts newline; submit via button only
- R3. Reasonable max visible height (`maxLines = 5`), scrollable beyond
- R4. Preserve existing trim-on-save behavior
- R5. Display multi-line notes correctly in `MealPlanItemRow`

## Scope Boundaries

- No rich text or markdown support
- No resizable/draggable input area
- No backend or data model changes (text column already handles newlines)
- URL detection (`isUrl()`) unchanged — multi-line notes won't match, acceptable

## Context & Research

### Relevant Code and Patterns

- `composeApp/.../mealplan/MealPlanDialogs.kt` — `AddMealPlanItemDialog` (line 132)
  and `EditNoteDialog` (line 178) both use `OutlinedTextField` with `singleLine = true`
- `composeApp/.../mealplan/MealPlanComponents.kt` — `MealPlanItemRow` (line 244)
  displays note text with `maxLines = 1`
- Keyboard: both dialogs use `ImeAction.Done` with `KeyboardActions(onDone = hide)`
- `LocalSoftwareKeyboardController` already imported and used

## Key Technical Decisions

- **Remove `singleLine = true`, add `maxLines = 5`**: Compose `OutlinedTextField`
  becomes multi-line when `singleLine` is removed. `maxLines = 5` caps visible
  height while allowing scroll for longer notes.
- **Remove `ImeAction.Done` keyboard options**: Multi-line text fields need Enter
  to insert newlines. Remove `keyboardOptions` and `keyboardActions` entirely —
  default behavior is correct for multi-line input.
- **Display with `maxLines = 2`**: In `MealPlanItemRow`, increase from `maxLines = 1`
  to `maxLines = 2` so multi-line notes show a preview without overwhelming
  the list layout.

## Implementation Units

- [ ] **Unit 1: Convert note input to multi-line in dialogs**

**Goal:** Make both add-note and edit-note dialogs accept multi-line input.

**Requirements:** R1, R2, R3

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/mealplan/MealPlanDialogs.kt`

**Approach:**
- In `AddMealPlanItemDialog` (line 132-140): remove `singleLine = true`,
  add `maxLines = 5`, remove `keyboardOptions` and `keyboardActions` params
- In `EditNoteDialog` (line 178-186): same changes
- Remove `keyboardController` from both `EditNoteDialog` and
  `AddMealPlanItemDialog` — it's only used in the note field's `onDone`.
  `RecipePickerTab` defines its own local `keyboardController`.
- R4 (trim-on-save): no UI change needed — trimming happens in
  `MealPlanViewModel` (`addNoteItem`/`updateNote`), not in the TextField.

**Patterns to follow:**
- `AddRecipeComponents.kt` recipe description field (lines 97-104) already uses
  `singleLine = false, maxLines = 4` without keyboard options — same approach

**Test expectation:** none — pure UI styling change, no behavioral logic.
Manual verification in browser/emulator.

**Verification:**
- Note input in both dialogs accepts Enter for newlines
- Input area grows up to 5 lines, then scrolls
- Submit still works via button only

- [ ] **Unit 2: Display multi-line notes in item list**

**Goal:** Show a 2-line preview of notes in the meal plan day view.

**Requirements:** R5

**Dependencies:** None (independent of Unit 1)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/mealplan/MealPlanComponents.kt`

**Approach:**
- In `MealPlanItemRow` (line 244-253), change `maxLines = 1` to `maxLines = 2`
  for the `MealPlanItem.Note` case only. Recipes keep `maxLines = 1`.
- Split the `Text` composable into a `when` that applies different `maxLines`
  per item type, or use a conditional `maxLines` value.

**Patterns to follow:**
- Existing `when (item)` pattern already used for icon selection (line 233)

**Test expectation:** none — pure UI display change. Manual verification.

**Verification:**
- Multi-line notes show up to 2 lines with ellipsis in the list
- Recipe names still show single-line
- Long single-line notes still truncate with ellipsis

## System-Wide Impact

- **Interaction graph:** No callbacks or middleware affected
- **API surface parity:** No API changes — `String` already handles newlines
- **Unchanged invariants:** `isUrl()` detection, trim-on-save, server-side
  handling all remain unchanged

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Dialog height may grow awkwardly on small screens | `maxLines = 5` caps growth; content scrolls beyond |

## Sources & References

- **Origin document:** [docs/brainstorms/2026-04-14-001-meal-plan-note-textarea-requirements.md](docs/brainstorms/2026-04-14-001-meal-plan-note-textarea-requirements.md)
- Related issue: #66
