package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.catalog.ItemCategory
import io.github.fgrutsch.cookmaid.mealplan.CreateMealPlanItemRequest
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import io.github.fgrutsch.cookmaid.mealplan.UpdateMealPlanItemRequest
import io.github.fgrutsch.cookmaid.recipe.CreateRecipeRequest
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipePage
import io.github.fgrutsch.cookmaid.recipe.UpdateRecipeRequest
import io.github.fgrutsch.cookmaid.shopping.CreateShoppingItemRequest
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import io.github.fgrutsch.cookmaid.support.BaseViewModelTest
import io.github.fgrutsch.cookmaid.ui.catalog.ApiCatalogItemRepository
import io.github.fgrutsch.cookmaid.ui.catalog.CatalogItemClient
import io.github.fgrutsch.cookmaid.ui.shopping.FakeCatalogItemRepository
import io.github.fgrutsch.cookmaid.ui.mealplan.ApiMealPlanRepository
import io.github.fgrutsch.cookmaid.ui.mealplan.FakeMealPlanRepository
import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanClient
import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanEvent
import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanViewModel
import io.github.fgrutsch.cookmaid.ui.recipe.ApiRecipeRepository
import io.github.fgrutsch.cookmaid.ui.recipe.FakeRecipeRepository
import io.github.fgrutsch.cookmaid.ui.recipe.RecipeClient
import io.github.fgrutsch.cookmaid.ui.recipe.list.RecipeListEvent
import io.github.fgrutsch.cookmaid.ui.recipe.list.RecipeListViewModel
import io.github.fgrutsch.cookmaid.ui.shopping.ApiShoppingListRepository
import io.github.fgrutsch.cookmaid.ui.shopping.FakeShoppingListRepository
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListClient
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListEvent
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListState
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.datetime.LocalDate
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class SessionCleanerTest : BaseViewModelTest() {

    private val httpClient = HttpClient {}

    private fun buildCleaner(
        tokenStore: TokenStore = FakeTokenStore(),
        shoppingRepo: ApiShoppingListRepository = ApiShoppingListRepository(StubShoppingClient()),
        catalogRepo: ApiCatalogItemRepository = ApiCatalogItemRepository(StubCatalogClient()),
        mealPlanRepo: ApiMealPlanRepository = ApiMealPlanRepository(StubMealPlanClient()),
        recipeRepo: ApiRecipeRepository = ApiRecipeRepository(StubRecipeClient()),
        recipeListViewModel: RecipeListViewModel = RecipeListViewModel(
            FakeRecipeRepository(),
            FakeShoppingListRepository(),
            FakeMealPlanRepository(),
        ),
        shoppingListViewModel: ShoppingListViewModel = ShoppingListViewModel(
            FakeShoppingListRepository(),
            FakeCatalogItemRepository(),
        ),
        mealPlanViewModel: MealPlanViewModel = MealPlanViewModel(
            FakeMealPlanRepository(),
            FakeRecipeRepository(),
            FakeShoppingListRepository(),
        ),
    ) = SessionCleaner(
        tokenStore = tokenStore,
        httpClient = httpClient,
        shoppingListRepository = shoppingRepo,
        catalogItemRepository = catalogRepo,
        mealPlanRepository = mealPlanRepo,
        recipeRepository = recipeRepo,
        recipeListViewModel = recipeListViewModel,
        shoppingListViewModel = shoppingListViewModel,
        mealPlanViewModel = mealPlanViewModel,
    )

    @Test
    fun `clearAll removes tokens`() = viewModelTest {
        val store = FakeTokenStore()
        store.saveTokens(accessToken = "A", refreshToken = "R", idToken = "I")
        val cleaner = buildCleaner(tokenStore = store)

        cleaner.clearAll()

        assertNull(store.getAccessToken())
        assertNull(store.getRefreshToken())
        assertNull(store.getIdToken())
    }

    @Test
    fun `clearAll empties repository caches`() = viewModelTest {
        val shoppingClient = StubShoppingClient(listOf(ShoppingList(id = Uuid.random(), name = "A")))
        val catalogClient = StubCatalogClient(listOf(catalogItem("Milk")))
        val shoppingRepo = ApiShoppingListRepository(shoppingClient)
        val catalogRepo = ApiCatalogItemRepository(catalogClient)
        shoppingRepo.getLists()
        catalogRepo.search("milk")
        assertEquals(1, shoppingClient.fetchListsCallCount)
        assertEquals(1, catalogClient.fetchAllCallCount)

        val cleaner = buildCleaner(shoppingRepo = shoppingRepo, catalogRepo = catalogRepo)
        cleaner.clearAll()

        // After clear, the next access re-fetches from the client.
        shoppingRepo.getLists()
        catalogRepo.search("milk")
        assertEquals(2, shoppingClient.fetchListsCallCount)
        assertEquals(2, catalogClient.fetchAllCallCount)
    }

    @Test
    fun `clearAll resets ViewModels to initial state`() = viewModelTest {
        val recipeListVm = RecipeListViewModel(
            FakeRecipeRepository().apply { recipes.add(recipe("Pasta")) },
            FakeShoppingListRepository(),
            FakeMealPlanRepository(),
        )
        val shoppingVm = ShoppingListViewModel(
            FakeShoppingListRepository().apply {
                lists = mutableListOf(ShoppingList(id = Uuid.random(), name = "Groceries", default = true))
            },
            FakeCatalogItemRepository(),
        )
        val mealPlanVm = MealPlanViewModel(
            FakeMealPlanRepository(),
            FakeRecipeRepository(),
            FakeShoppingListRepository(),
        )
        recipeListVm.onEvent(RecipeListEvent.LoadRecipes)
        shoppingVm.onEvent(ShoppingListEvent.LoadLists)
        mealPlanVm.onEvent(MealPlanEvent.Load)
        advanceUntilIdle()
        assertFalse(recipeListVm.state.value.recipes.isEmpty())

        val cleaner = buildCleaner(
            recipeListViewModel = recipeListVm,
            shoppingListViewModel = shoppingVm,
            mealPlanViewModel = mealPlanVm,
        )
        cleaner.clearAll()
        advanceUntilIdle()

        assertTrue(recipeListVm.state.value.recipes.isEmpty())
        assertEquals("", recipeListVm.state.value.searchQuery)
        assertEquals(ShoppingListState(), shoppingVm.state.value)
        assertTrue(mealPlanVm.state.value.days.isEmpty() || mealPlanVm.state.value.days.all { it.items.isEmpty() })
    }

    @Test
    fun `clearAll is idempotent`() = viewModelTest {
        val store = FakeTokenStore().apply {
            saveTokens(accessToken = "A", refreshToken = null, idToken = null)
        }
        val cleaner = buildCleaner(tokenStore = store)

        cleaner.clearAll()
        cleaner.clearAll()

        assertNull(store.getAccessToken())
    }

    @Test
    fun `clearAll completes even when token store throws`() = viewModelTest {
        // Token-store step fails; subsequent repo/VM steps must still run.
        val throwingStore = FakeTokenStore(failRemoveAccess = true).apply {
            saveTokens(accessToken = "A", refreshToken = "R", idToken = "I")
        }
        val shoppingClient = StubShoppingClient(listOf(ShoppingList(id = Uuid.random(), name = "A")))
        val shoppingRepo = ApiShoppingListRepository(shoppingClient)
        shoppingRepo.getLists()
        assertEquals(1, shoppingClient.fetchListsCallCount)

        val cleaner = buildCleaner(tokenStore = throwingStore, shoppingRepo = shoppingRepo)
        cleaner.clearAll() // must not throw

        // The later step still ran — cache is empty, next fetch hits the client again.
        shoppingRepo.getLists()
        assertEquals(2, shoppingClient.fetchListsCallCount)
    }

    companion object {
        private fun recipe(name: String) = Recipe(
            id = Uuid.random(),
            name = name,
            ingredients = emptyList(),
            steps = emptyList(),
            tags = emptyList(),
        )

        private fun catalogItem(name: String) = Item.Catalog(
            id = Uuid.random(),
            name = name,
            category = ItemCategory(id = Uuid.random(), name = "General"),
        )
    }
}

private class FakeTokenStore(val failRemoveAccess: Boolean = false) : TokenStore() {
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var idToken: String? = null

    private val accessFlow = MutableStateFlow<String?>(null)
    private val refreshFlow = MutableStateFlow<String?>(null)
    private val idFlow = MutableStateFlow<String?>(null)

    override val accessTokenFlow: StateFlow<String?> = accessFlow
    override val refreshTokenFlow: StateFlow<String?> = refreshFlow
    override val idTokenFlow: StateFlow<String?> = idFlow

    override suspend fun getAccessToken(): String? = accessToken
    override suspend fun getRefreshToken(): String? = refreshToken
    override suspend fun getIdToken(): String? = idToken

    override suspend fun removeAccessToken() {
        if (failRemoveAccess) throw IllegalStateException("simulated token store failure")
        accessToken = null
        accessFlow.value = null
    }

    override suspend fun removeRefreshToken() {
        refreshToken = null
        refreshFlow.value = null
    }

    override suspend fun removeIdToken() {
        idToken = null
        idFlow.value = null
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String?, idToken: String?) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.idToken = idToken
        accessFlow.value = accessToken
        refreshFlow.value = refreshToken
        idFlow.value = idToken
    }
}

private class StubShoppingClient(
    private val lists: List<ShoppingList> = emptyList(),
) : ShoppingListClient {
    var fetchListsCallCount: Int = 0

    override suspend fun fetchLists(): List<ShoppingList> {
        fetchListsCallCount++
        return lists
    }

    override suspend fun createList(name: String): ShoppingList = error("not used")
    override suspend fun updateList(id: Uuid, name: String) = error("not used")
    override suspend fun deleteList(id: Uuid) = error("not used")
    override suspend fun fetchItems(listId: Uuid): List<ShoppingItem> = error("not used")
    override suspend fun addItems(listId: Uuid, items: List<CreateShoppingItemRequest>): List<ShoppingItem> =
        error("not used")

    override suspend fun addItem(
        listId: Uuid,
        catalogItemId: Uuid?,
        freeTextName: String?,
        quantity: String?,
    ): ShoppingItem = error("not used")

    override suspend fun updateItem(listId: Uuid, itemId: Uuid, quantity: String?, checked: Boolean) =
        error("not used")

    override suspend fun deleteItem(listId: Uuid, itemId: Uuid) = error("not used")
    override suspend fun deleteCheckedItems(listId: Uuid) = error("not used")
}

private class StubCatalogClient(
    private val items: List<Item.Catalog> = emptyList(),
) : CatalogItemClient {
    var fetchAllCallCount: Int = 0

    override suspend fun fetchAll(): List<Item.Catalog> {
        fetchAllCallCount++
        return items
    }
}

private class StubMealPlanClient : MealPlanClient {
    override suspend fun fetchItems(from: LocalDate, to: LocalDate): List<MealPlanItem> = emptyList()
    override suspend fun create(request: CreateMealPlanItemRequest): MealPlanItem = error("not used")
    override suspend fun update(id: Uuid, request: UpdateMealPlanItemRequest) = error("not used")
    override suspend fun delete(id: Uuid) = error("not used")
}

private class StubRecipeClient : RecipeClient {
    override suspend fun fetchPage(cursor: String?, limit: Int, search: String?, tag: String?): RecipePage =
        RecipePage(items = emptyList(), nextCursor = null)

    override suspend fun fetchTags(): List<String> = emptyList()
    override suspend fun fetchById(id: Uuid): Recipe = error("not used")
    override suspend fun create(request: CreateRecipeRequest): Recipe = error("not used")
    override suspend fun update(id: Uuid, request: UpdateRecipeRequest) = error("not used")
    override suspend fun fetchRandom(tag: String?, excludeId: String?): Recipe = error("not used")
    override suspend fun delete(id: Uuid) = error("not used")
}
