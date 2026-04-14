---
title: "IDOR in MealPlanService.create() — missing recipe ownership validation"
date: 2026-04-14
category: security-issues
module: mealplan
problem_type: security_issue
component: service_object
symptoms:
  - "MealPlanService.create() accepts any recipeId without ownership check"
  - "Authenticated user can reference another user's recipe UUID, leaking the recipe name"
root_cause: missing_validation
resolution_type: code_fix
severity: high
tags:
  - idor
  - cwe-639
  - ownership-validation
  - meal-plan
  - recipe
  - authorization
---

# IDOR in MealPlanService.create() — missing recipe ownership validation

## Problem

`MealPlanService.create()` accepted any `recipeId` without verifying it
belonged to the calling user. An authenticated user could reference another
user's recipe UUID in a meal plan item, leaking the recipe name in the
response (CWE-639 / Insecure Direct Object Reference).

## Symptoms

- `POST /api/meal-plan` with another user's `recipeId` returned 201 with
  the recipe name in the response body
- No authorization check between the requesting user and the recipe owner

## What Didn't Work

No failed approaches — the existing `isOwner()` pattern from
`ShoppingListService` was directly applicable.

## Solution

Injected `RecipeRepository` into `MealPlanService` and added an ownership
check before creating the meal plan item. Changed the return type to
nullable, matching the existing pattern in `ShoppingListService`.

```kotlin
// MealPlanService.kt
class MealPlanService(
    private val recipeRepository: RecipeRepository,
    private val repository: MealPlanRepository,
) {
    suspend fun create(userId: UserId, day: LocalDate, recipeId: Uuid?, note: String?): MealPlanItem? {
        if (recipeId != null && !recipeRepository.isOwner(userId, recipeId)) return null
        return repository.create(userId, day, recipeId, note)
    }
}
```

Route handler updated to check null -> 404:

```kotlin
// MealPlanModule.kt
post {
    val body = call.receive<CreateMealPlanItemRequest>()
    val item = service.create(call.userId(), body.day, body.recipeId, body.note)
    if (item == null) {
        call.respond(HttpStatusCode.NotFound)
    } else {
        call.respond(HttpStatusCode.Created, item)
    }
}
```

Koin auto-wires `RecipeRepository` into the constructor — no module
config changes needed.

**Files changed:**
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/mealplan/MealPlanService.kt`
- `server/src/main/kotlin/io/github/fgrutsch/cookmaid/mealplan/MealPlanModule.kt`

## Why This Works

The `isOwner()` check verifies a row exists in `RecipesTable` matching both
the recipe ID and the user ID. Non-existent recipes also return false,
covering both cross-user and invalid ID cases. Returning 404 (not 403)
prevents leaking resource existence — consistent with the project's
ownership pattern.

## Prevention

- Any service method that accepts a resource ID from user input must
  validate ownership before acting on it
- Follow the established pattern: `repository.isOwner(userId, id)` ->
  return null from service -> route responds 404
- When adding new fields that reference cross-entity IDs (e.g., `recipeId`
  on meal plans), check ownership at the service layer
- Write multi-user integration tests using `TestJwt.generateToken(subject)`
  with different subjects to verify cross-user access is blocked

## Related Issues

- GitHub issue: #43
- Plan: `docs/plans/2026-04-14-001-fix-recipe-n1-mealplan-idor-plan.md`
- Pattern reference: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/shopping/ShoppingListService.kt`
