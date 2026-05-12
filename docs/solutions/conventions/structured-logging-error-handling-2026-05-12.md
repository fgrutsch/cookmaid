---
title: Structured logging and error handling conventions
date: 2026-05-12
category: conventions
module: server, composeApp
problem_type: convention
component: tooling
severity: medium
applies_when:
  - Adding new server service methods that create, update, or delete entities
  - Adding new ViewModel error handling or catch blocks
  - Working with CancellationException in suspend code
tags:
  - logging
  - error-handling
  - kotlin-logging
  - mvi-viewmodel
  - cancellation-exception
  - ktor
  - statusPages
---

# Structured logging and error handling conventions

## Context

The codebase had almost no logging — only one info log in `UserService.getOrCreate`.
All server CUD operations, ownership check failures, and the StatusPages catch-all
were silent. On the client, `MviViewModel.onError` was a no-op by default, and
`AuthViewModel.initialize()` swallowed `CancellationException`. These gaps made
production debugging nearly impossible.

## Guidance

### Server: kotlin-logging in services

Add `private val logger = KotlinLogging.logger {}` inside the class body (not
companion object, not top-level) for proper SLF4J logger names.

```kotlin
class RecipeService(private val repository: RecipeRepository) {

    private val logger = KotlinLogging.logger {}

    suspend fun create(userId: UserId, data: RecipeRequest, locale: SupportedLocale): Recipe {
        val recipe = repository.create(userId, data, locale)
        logger.info { "Recipe created: userId=$userId, recipeId=${recipe.id}" }
        return recipe
    }

    suspend fun delete(userId: UserId, recipeId: Uuid): Boolean {
        if (!repository.isOwner(userId, recipeId)) {
            logger.debug { "Ownership check failed: userId=$userId, recipeId=$recipeId" }
            return false
        }
        repository.delete(recipeId)
        logger.info { "Recipe deleted: userId=$userId, recipeId=$recipeId" }
        return true
    }
}
```

Log levels:
- **info** — successful create, update, delete (with userId and entity ID)
- **debug** — ownership check failures, validation failures (expected control-flow)
- **error** — unhandled exceptions (StatusPages catch-all)

### Server: StatusPages exception logging

The StatusPages catch-all is the only place to log unhandled exceptions. Use the
`cause` parameter (not `_`) and log at error level:

```kotlin
exception<Throwable> { call, cause ->
    logger.error(cause) { "Unhandled exception" }
    call.respond(HttpStatusCode.InternalServerError)
}
```

For 400-level handlers (`IllegalArgumentException`, `MissingRequestParameterException`),
log at debug level.

### Client: MviViewModel error logging

Log in the `launch`/`launchOptimistic` catch blocks, **not** in `onError`. Subclasses
override `onError` without calling `super`, so base-class logging in `onError` would
be bypassed.

```kotlin
abstract class MviViewModel<S, E, F>(initialState: S) : ViewModel() {

    private val logger = KotlinLogging.logger {}

    protected fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                logger.error(e) { "Unhandled error in ${this@MviViewModel::class.simpleName}" }
                onError(e)
            }
        }
    }
}
```

### CancellationException rethrow pattern

Every `catch (e: Exception)` in suspend code must rethrow `CancellationException`
first. In Kotlin, `CancellationException` extends `Exception`, so a bare catch
silently breaks scope teardown:

```kotlin
try { block() }
catch (e: CancellationException) { throw e }
catch (@Suppress("TooGenericExceptionCaught") e: Exception) { /* handle */ }
```

## Why This Matters

- Silent exceptions make production incidents invisible — the StatusPages catch-all
  returning 500 with no logging was the most critical gap
- MviViewModel's no-op `onError` meant all client exceptions were silently discarded
- Swallowed `CancellationException` breaks structured concurrency — scope teardown
  never completes, potentially leaking resources

## When to Apply

- Any new server service method that mutates data
- Any new ViewModel or suspend catch block
- Any inner try/catch inside `MviViewModel.launch {}` that catches `Exception`

## Examples

Replace `e.printStackTrace()` with structured logging:

```kotlin
// Before
catch (e: Exception) {
    e.printStackTrace()
}

// After
catch (e: Exception) {
    logger.error(e) { "Logout cleanup failed" }
}
```

## Related

- Issue: [#15](https://github.com/fgrutsch/cookmaid/issues/15)
- `kotlin-logging` library: `gradle/libs.versions.toml` (already in version catalog)
- Detekt rules: `PrintStackTrace: active: true`, `SwallowedException: active: true`
