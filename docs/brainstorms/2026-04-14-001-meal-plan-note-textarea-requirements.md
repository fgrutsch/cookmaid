# Meal Plan Note: Use Textarea

**Date**: 2026-04-14
**Issue**: #66
**Status**: Draft

## Problem

Meal plan notes are limited to single-line input. Users want multi-line
notes (e.g., ingredient lists, quick instructions, multiple items).

## Requirements

- Replace single-line `OutlinedTextField` with multi-line variant in:
  - `AddMealPlanItemDialog` (`composeApp/.../mealplan/MealPlanDialogs.kt`)
  - `EditNoteDialog` (`composeApp/.../mealplan/MealPlanDialogs.kt`)
- Support Enter key for newlines instead of submit
- Keep a reasonable max height (e.g., `maxLines = 5`, scrollable beyond)
- Preserve existing trim-on-save behavior
- Display multi-line notes correctly in `MealPlanItemRow`

## Non-Goals

- Rich text or markdown support
- Resizable/draggable input area
- Backend or data model changes (text column already supports newlines)

## Notes

- URL detection (`isUrl()`) only checks single-line notes — multi-line
  notes containing URLs are not treated as links. This is acceptable.
- Keyboard action changes from `ImeAction.Done` to default (Enter = newline).
  Submit via button only.
