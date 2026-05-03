# Random Recipe Duplicates Fix — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent duplicate recipes when re-rolling by tracking all previously shown recipe IDs and excluding them server-side.

**Architecture:** Change `excludeId` (single UUID) to `excludeIds` (list of UUIDs) across all layers. The client accumulates shown IDs in state; the server uses `NOT IN` to exclude them. When all recipes are exhausted, the server falls back to returning any random recipe.

**Tech Stack:** Kotlin, Ktor, Exposed, Compose Multiplatform, kotlinx.serialization

---

## File Map

- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRepository.kt` — change `findRandom` interface + impl to accept `List<Uuid>`
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeService.kt` — propagate signature change
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeModule.kt` — parse `excludeIds` query param
- Modify: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRoutesTest.kt` — update existing tests, add multi-exclude test
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/RecipeClient.kt` — change `fetchRandom` param
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/RecipeRepository.kt` — propagate signature change
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListContract.kt` — add `shownRecipeIds` to state
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListViewModel.kt` — accumulate shown IDs, reset on clear/tag change
- Modify: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/FakeRecipeRepository.kt` — update fake signature
- Modify: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListViewModelTest.kt` — add accumulation/reset tests

---

### Task 1: Server — Update Repository Interface and Implementation

**Files:**
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRepository.kt:104-122` (interface)
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRepository.kt:274-323` (implementation)

- [ ] **Step 1: Update the `RecipeRepository` interface**

Change the `findRandom` signature from single `excludeId: Uuid?` to `excludeIds: List<Uuid>`:

```kotlin
/**
 * Returns a random recipe for [userId], optionally filtered by [tag].
 *
 * @param userId the owner of the recipes.
 * @param tag optional tag filter.
 * @param excludeIds recipe IDs to exclude (for avoiding repeats).
 * @param locale the language code for catalog item names.
 * @return a random recipe, or null if no recipes match.
 */
suspend fun findRandom(userId: UserId, tag: String?, excludeIds: List<Uuid>, locale: SupportedLocale): Recipe?
```

- [ ] **Step 2: Update `PostgresRecipeRepository.findRandom`**

Replace the `NeqOp` single-exclude with `notInList` for multiple excludes. Remove the now-unused `NeqOp` and `QueryParameter` imports if they are no longer used elsewhere in the file.

```kotlin
override suspend fun findRandom(
    userId: UserId,
    tag: String?,
    excludeIds: List<Uuid>,
    locale: SupportedLocale,
): Recipe? = suspendTransaction {
    fun queryRandom(withExclusion: Boolean): Recipe? {
        var condition: Op<Boolean> = RecipesTable.userId eq userId.value

        if (!tag.isNullOrBlank()) {
            condition = condition and object : Op<Boolean>() {
                override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                    queryBuilder {
                        append(RecipesTable.tags)
                        append(" @> ARRAY[")
                        registerArgument(TextColumnType(), tag.trim())
                        append("]::text[]")
                    }
                }
            }
        }

        if (withExclusion && excludeIds.isNotEmpty()) {
            condition = condition and (RecipesTable.id notInList excludeIds)
        }

        val row = RecipesTable.selectAll()
            .where(condition)
            .orderBy(Random())
            .limit(1)
            .singleOrNull() ?: return null

        val id = row[RecipesTable.id]
        return Recipe(
            id = id,
            name = row[RecipesTable.name],
            description = row[RecipesTable.description],
            ingredients = loadIngredients(id, locale),
            steps = row[RecipesTable.steps],
            tags = row[RecipesTable.tags],
            servings = row[RecipesTable.servings],
        )
    }

    queryRandom(withExclusion = true)
        ?: if (excludeIds.isNotEmpty()) queryRandom(withExclusion = false) else null
}
```

- [ ] **Step 3: Clean up imports**

Check if `NeqOp` and `QueryParameter` are still used elsewhere in the file. If not, remove lines 17 and 20:
```
import org.jetbrains.exposed.v1.core.NeqOp
import org.jetbrains.exposed.v1.core.QueryParameter
```

Add `notInList` import if not already present (line 24 already has `inList` — check if `notInList` needs a separate import or is available from the same package). Exposed v1 provides `notInList` from `org.jetbrains.exposed.v1.core`:

```kotlin
import org.jetbrains.exposed.v1.core.notInList
```

- [ ] **Step 4: Verify it compiles**

Run: `./gradlew :server:compileKotlin 2>&1 | tail -5`

Expected: compilation errors in `RecipeService` and `RecipeModule` (they still pass single `Uuid?`). The repository itself should compile.

---

### Task 2: Server — Update Service and Route

**Files:**
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeService.kt:61-71`
- Modify: `server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeModule.kt:45-49`

- [ ] **Step 1: Update `RecipeService.findRandom`**

```kotlin
/**
 * Returns a random recipe for [userId], optionally filtered by [tag].
 *
 * @param userId the owner of the recipes.
 * @param tag optional tag filter.
 * @param excludeIds recipe IDs to exclude (for avoiding repeats).
 * @param locale the language code for catalog item names.
 * @return a random recipe, or null if no recipes match.
 */
suspend fun findRandom(userId: UserId, tag: String?, excludeIds: List<Uuid>, locale: SupportedLocale): Recipe? {
    return repository.findRandom(userId, tag, excludeIds, locale)
}
```

- [ ] **Step 2: Update route in `RecipeModule.kt`**

Parse `excludeIds` as a comma-separated list of UUIDs:

```kotlin
get("/random") {
    val tag = call.request.queryParameters["tag"]
    val excludeIds = call.request.queryParameters["excludeIds"]
        ?.split(",")
        ?.filter { it.isNotBlank() }
        ?.map { Uuid.parse(it.trim()) }
        ?: emptyList()
    val recipe = service.findRandom(call.userId(), tag, excludeIds, call.locale())
    if (recipe != null) call.respond(recipe) else call.respond(HttpStatusCode.NotFound)
}
```

- [ ] **Step 3: Verify server compiles**

Run: `./gradlew :server:compileKotlin 2>&1 | tail -5`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRepository.kt \
       server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeService.kt \
       server/src/main/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeModule.kt
git commit -m "feat: change excludeId to excludeIds list in random recipe API [skip ci]"
```

---

### Task 3: Server — Update Tests

**Files:**
- Modify: `server/src/test/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRoutesTest.kt:173-254`

- [ ] **Step 1: Update existing `excludeId` test to use `excludeIds`**

Change test at line 173 — rename and update the query parameter:

```kotlin
@Test
fun `GET recipes random with excludeIds returns different recipe when possible`() = integrationTest {
    val token = TestJwt.generateToken("random-exclude-user")
    val client = jsonClient()

    client.post("/api/users/me") { bearerAuth(token) }
    val recipe1 = client.post("/api/recipes") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(RecipeRequest(name = "Recipe A"))
    }.body<Recipe>()
    val recipe2 = client.post("/api/recipes") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(RecipeRequest(name = "Recipe B"))
    }.body<Recipe>()

    val response = client.get("/api/recipes/random?excludeIds=${recipe1.id}") {
        bearerAuth(token)
    }
    assertEquals(HttpStatusCode.OK, response.status)
    val result = response.body<Recipe>()
    assertEquals(recipe2.id, result.id)
}
```

- [ ] **Step 2: Update existing fallback test to use `excludeIds`**

Change test at line 198:

```kotlin
@Test
fun `GET recipes random with excludeIds falls back when only one recipe`() = integrationTest {
    val token = TestJwt.generateToken("random-fallback-user")
    val client = jsonClient()

    client.post("/api/users/me") { bearerAuth(token) }
    val only = client.post("/api/recipes") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(RecipeRequest(name = "Only Recipe"))
    }.body<Recipe>()

    val response = client.get("/api/recipes/random?excludeIds=${only.id}") {
        bearerAuth(token)
    }
    assertEquals(HttpStatusCode.OK, response.status)
    val result = response.body<Recipe>()
    assertEquals(only.id, result.id)
}
```

- [ ] **Step 3: Add new test for multiple excludeIds**

Add after the fallback test:

```kotlin
@Test
fun `GET recipes random with multiple excludeIds excludes all`() = integrationTest {
    val token = TestJwt.generateToken("random-multi-exclude-user")
    val client = jsonClient()

    client.post("/api/users/me") { bearerAuth(token) }
    val recipe1 = client.post("/api/recipes") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(RecipeRequest(name = "Recipe A"))
    }.body<Recipe>()
    val recipe2 = client.post("/api/recipes") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(RecipeRequest(name = "Recipe B"))
    }.body<Recipe>()
    val recipe3 = client.post("/api/recipes") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(RecipeRequest(name = "Recipe C"))
    }.body<Recipe>()

    val response = client.get("/api/recipes/random?excludeIds=${recipe1.id},${recipe2.id}") {
        bearerAuth(token)
    }
    assertEquals(HttpStatusCode.OK, response.status)
    val result = response.body<Recipe>()
    assertEquals(recipe3.id, result.id)
}
```

- [ ] **Step 4: Add test for all recipes excluded (fallback)**

```kotlin
@Test
fun `GET recipes random with all recipes excluded falls back`() = integrationTest {
    val token = TestJwt.generateToken("random-all-excluded-user")
    val client = jsonClient()

    client.post("/api/users/me") { bearerAuth(token) }
    val recipe1 = client.post("/api/recipes") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(RecipeRequest(name = "Recipe A"))
    }.body<Recipe>()
    val recipe2 = client.post("/api/recipes") {
        bearerAuth(token)
        contentType(ContentType.Application.Json)
        setBody(RecipeRequest(name = "Recipe B"))
    }.body<Recipe>()

    val response = client.get("/api/recipes/random?excludeIds=${recipe1.id},${recipe2.id}") {
        bearerAuth(token)
    }
    assertEquals(HttpStatusCode.OK, response.status)
    val result = response.body<Recipe>()
    assertTrue(result.id == recipe1.id || result.id == recipe2.id)
}
```

- [ ] **Step 5: Run server tests**

Run: `./gradlew :server:test --tests "io.github.fgrutsch.cookmaid.recipe.RecipeRoutesTest" 2>&1 | tail -10`

Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add server/src/test/kotlin/io/github/fgrutsch/cookmaid/recipe/RecipeRoutesTest.kt
git commit -m "test: update server random recipe tests for excludeIds [skip ci]"
```

---

### Task 4: Client — Update RecipeClient and RecipeRepository

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/RecipeClient.kt:51-55`
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/RecipeRepository.kt:86-93,146-152`

- [ ] **Step 1: Update `RecipeClient.fetchRandom`**

Change parameter from single `excludeId: String?` to `excludeIds: List<String>`:

```kotlin
suspend fun fetchRandom(tag: String?, excludeIds: List<String>): Recipe =
    apiClient.httpClient.get("$base/random") {
        tag?.let { parameter("tag", it) }
        if (excludeIds.isNotEmpty()) {
            parameter("excludeIds", excludeIds.joinToString(","))
        }
    }.body()
```

- [ ] **Step 2: Update `RecipeRepository` interface**

Change `fetchRandom` signature at line 93:

```kotlin
/**
 * Returns a random recipe, optionally filtered by [tag] and excluding [excludeIds].
 *
 * @param tag optional tag to filter by.
 * @param excludeIds recipe IDs to exclude (for avoiding repeats).
 * @return a random [Recipe], or null if none match.
 */
suspend fun fetchRandom(tag: String? = null, excludeIds: List<String> = emptyList()): Recipe?
```

- [ ] **Step 3: Update `ApiRecipeRepository.fetchRandom`**

Change implementation at line 146:

```kotlin
override suspend fun fetchRandom(tag: String?, excludeIds: List<String>): Recipe? {
    return try {
        client.fetchRandom(tag, excludeIds)
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.NotFound) null else throw e
    }
}
```

- [ ] **Step 4: Verify composeApp compiles**

Run: `./gradlew :composeApp:compileKotlinWasmJs 2>&1 | tail -5`

Expected: compilation errors in `RecipeListViewModel` (still passing single `excludeId`). The client/repository should compile.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/RecipeClient.kt \
       composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/RecipeRepository.kt
git commit -m "feat: change excludeId to excludeIds in client RecipeRepository [skip ci]"
```

---

### Task 5: Client — Update State, ViewModel, and Fake

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListContract.kt:8-22`
- Modify: `composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListViewModel.kt:45,131-148`
- Modify: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/FakeRecipeRepository.kt:75-84`

- [ ] **Step 1: Add `shownRecipeIds` to `RecipeListState`**

Add the field after `isLoadingRandom`:

```kotlin
data class RecipeListState(
    val initialized: Boolean = false,
    val recipes: List<Recipe> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val searchActive: Boolean = false,
    val availableTags: List<String> = emptyList(),
    val selectedTag: String? = null,
    val randomRecipe: Recipe? = null,
    val isLoadingRandom: Boolean = false,
    val shownRecipeIds: Set<String> = emptySet(),
)
```

- [ ] **Step 2: Update `rollRandomRecipe` in ViewModel**

Accumulate shown IDs and pass them as `excludeIds`:

```kotlin
private fun rollRandomRecipe() {
    launch {
        updateState { copy(isLoadingRandom = true) }
        val tag = state.value.selectedTag
        val recipe = repository.fetchRandom(
            tag = tag,
            excludeIds = state.value.shownRecipeIds.toList(),
        )
        updateState {
            copy(
                randomRecipe = recipe,
                isLoadingRandom = false,
                shownRecipeIds = if (recipe != null) shownRecipeIds + recipe.id.toString() else shownRecipeIds,
            )
        }
    }
}
```

- [ ] **Step 3: Update `ClearRandomRecipe` handler**

Reset `shownRecipeIds` when clearing (line 45 in ViewModel):

```kotlin
is RecipeListEvent.ClearRandomRecipe -> updateState { copy(randomRecipe = null, shownRecipeIds = emptySet()) }
```

- [ ] **Step 4: Update `selectTag` to reset `shownRecipeIds`**

```kotlin
private fun selectTag(tag: String?) {
    val selected = if (tag == state.value.selectedTag) null else tag
    updateState { copy(selectedTag = selected, shownRecipeIds = emptySet()) }
    fetchFirstPage()
}
```

- [ ] **Step 5: Update `FakeRecipeRepository.fetchRandom`**

```kotlin
override suspend fun fetchRandom(tag: String?, excludeIds: List<String>): Recipe? {
    var filtered = recipes.toList()
    if (!tag.isNullOrBlank()) {
        filtered = filtered.filter { tag in it.tags }
    }
    if (excludeIds.isNotEmpty()) {
        filtered = filtered.filter { it.id.toString() !in excludeIds }
    }
    return filtered.randomOrNull()
}
```

- [ ] **Step 6: Verify everything compiles**

Run: `./gradlew :composeApp:compileKotlinWasmJs 2>&1 | tail -5`

Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListContract.kt \
       composeApp/src/commonMain/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListViewModel.kt \
       composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/FakeRecipeRepository.kt
git commit -m "feat: track shown recipe IDs to prevent duplicates on re-roll [skip ci]"
```

---

### Task 6: Client — Update ViewModel Tests

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListViewModelTest.kt`

- [ ] **Step 1: Add test — shownRecipeIds accumulates across re-rolls**

Add after the existing `clear random recipe resets state` test (after line 168):

```kotlin
@Test
fun `roll random recipe accumulates shownRecipeIds`() = viewModelTest {
    val viewModel = createLoadedViewModel(
        recipes = listOf(recipe("Pasta"), recipe("Salad"), recipe("Soup")),
    )

    viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
    advanceUntilIdle()
    val first = viewModel.state.value.randomRecipe
    assertNotNull(first)
    assertEquals(1, viewModel.state.value.shownRecipeIds.size)
    assertTrue(viewModel.state.value.shownRecipeIds.contains(first.id.toString()))

    viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
    advanceUntilIdle()
    val second = viewModel.state.value.randomRecipe
    assertNotNull(second)
    assertEquals(2, viewModel.state.value.shownRecipeIds.size)
    assertTrue(viewModel.state.value.shownRecipeIds.contains(first.id.toString()))
    assertTrue(viewModel.state.value.shownRecipeIds.contains(second.id.toString()))
}
```

- [ ] **Step 2: Add test — ClearRandomRecipe resets shownRecipeIds**

```kotlin
@Test
fun `clear random recipe resets shownRecipeIds`() = viewModelTest {
    val viewModel = createLoadedViewModel(recipes = listOf(recipe("Pasta"), recipe("Salad")))

    viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
    advanceUntilIdle()
    assertTrue(viewModel.state.value.shownRecipeIds.isNotEmpty())

    viewModel.onEvent(RecipeListEvent.ClearRandomRecipe)
    advanceUntilIdle()
    assertTrue(viewModel.state.value.shownRecipeIds.isEmpty())
}
```

- [ ] **Step 3: Add test — tag change resets shownRecipeIds**

```kotlin
@Test
fun `select tag resets shownRecipeIds`() = viewModelTest {
    val viewModel = createLoadedViewModel(
        recipes = listOf(
            recipe("Pasta", tags = listOf("Italian")),
            recipe("Tacos", tags = listOf("Mexican")),
        ),
        tags = listOf("Italian", "Mexican"),
    )

    viewModel.onEvent(RecipeListEvent.RollRandomRecipe)
    advanceUntilIdle()
    assertTrue(viewModel.state.value.shownRecipeIds.isNotEmpty())

    viewModel.onEvent(RecipeListEvent.SelectTag("Italian"))
    advanceUntilIdle()
    assertTrue(viewModel.state.value.shownRecipeIds.isEmpty())
}
```

- [ ] **Step 4: Run client tests**

Run: `./gradlew :composeApp:allTests 2>&1 | tail -10`

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonTest/kotlin/io/github/fgrutsch/cookmaid/ui/recipe/list/RecipeListViewModelTest.kt
git commit -m "test: add ViewModel tests for shownRecipeIds tracking [skip ci]"
```

---

### Task 7: Final Verification

- [ ] **Step 1: Run all tests**

Run: `./gradlew test 2>&1 | tail -20`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run linter**

Run: `./gradlew detektAll 2>&1 | tail -10`

Expected: BUILD SUCCESSFUL (no new detekt issues)

- [ ] **Step 3: Final commit if any remaining changes**

```bash
git status
# Only commit if there are uncommitted changes
```
