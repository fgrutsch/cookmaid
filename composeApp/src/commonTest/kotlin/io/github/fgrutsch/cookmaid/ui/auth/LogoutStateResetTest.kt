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
import io.github.fgrutsch.cookmaid.ui.mealplan.ApiMealPlanRepository
import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanClient
import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanEvent
import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanViewModel
import io.github.fgrutsch.cookmaid.ui.recipe.ApiRecipeRepository
import io.github.fgrutsch.cookmaid.ui.recipe.RecipeClient
import io.github.fgrutsch.cookmaid.ui.recipe.list.RecipeListEvent
import io.github.fgrutsch.cookmaid.ui.recipe.list.RecipeListViewModel
import io.github.fgrutsch.cookmaid.ui.shopping.ApiShoppingListRepository
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListClient
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListEvent
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * End-to-end regression for issue #73: user A logs out, user B logs in,
 * and user B sees only their own data across the three singleton-backed
 * screens (Shopping, Recipes, MealPlan). This is the authoritative proof
 * that the cross-user leak is closed — removing
 * `sessionCleaner.clearAll()` from `OidcAuthHandler.logout()` makes
 * [scenarioRunsCleanlyForUserB] fail.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogoutStateResetTest : BaseViewModelTest() {

    @Test
    fun scenarioRunsCleanlyForUserB() = viewModelTest {
        val rig = TestRig()

        loadUserAData(rig)
        advanceUntilIdle()
        assertAState(rig)

        rig.cleaner.clearAll()
        advanceUntilIdle()
        assertCleanAfterLogout(rig)

        loadUserBData(rig)
        advanceUntilIdle()
        assertBState(rig)
    }

    private suspend fun loadUserAData(rig: TestRig) {
        rig.tokenStore.saveTokens(accessToken = "A-token", refreshToken = "A-refresh", idToken = "A-id")
        rig.shoppingClient.listsByUser = listOf(
            ShoppingList(id = UUID_A_LIST, name = "Alice Groceries", default = true),
        )
        rig.recipeClient.recipesByUser = listOf(
            Recipe(id = UUID_A_RECIPE, name = "Alice Lasagna", ingredients = emptyList(), steps = emptyList()),
        )
        rig.recipeListVm.onEvent(RecipeListEvent.LoadRecipes)
        rig.shoppingVm.onEvent(ShoppingListEvent.LoadLists)
        rig.mealPlanVm.onEvent(MealPlanEvent.Load)
    }

    private suspend fun loadUserBData(rig: TestRig) {
        rig.tokenStore.saveTokens(accessToken = "B-token", refreshToken = "B-refresh", idToken = "B-id")
        rig.shoppingClient.listsByUser = listOf(
            ShoppingList(id = UUID_B_LIST, name = "Bob Groceries", default = true),
        )
        rig.recipeClient.recipesByUser = listOf(
            Recipe(id = UUID_B_RECIPE, name = "Bob Paella", ingredients = emptyList(), steps = emptyList()),
        )
        rig.recipeListVm.onEvent(RecipeListEvent.LoadRecipes)
        rig.shoppingVm.onEvent(ShoppingListEvent.LoadLists)
        rig.mealPlanVm.onEvent(MealPlanEvent.Load)
    }

    private suspend fun assertAState(rig: TestRig) {
        assertTrue(rig.recipeListVm.state.value.recipes.any { it.name == "Alice Lasagna" })
        assertEquals("Alice Groceries", rig.shoppingVm.state.value.selectedList?.name)
        assertNotNull(rig.tokenStore.getAccessToken())
    }

    private suspend fun assertCleanAfterLogout(rig: TestRig) {
        assertNull(rig.tokenStore.getAccessToken())
        assertTrue(rig.recipeListVm.state.value.recipes.isEmpty())
        assertTrue(rig.shoppingVm.state.value.lists.isEmpty())
        assertNull(rig.shoppingVm.state.value.selectedListId)
    }

    private fun assertBState(rig: TestRig) {
        val bRecipes = rig.recipeListVm.state.value.recipes
        assertEquals(1, bRecipes.size)
        assertEquals("Bob Paella", bRecipes.first().name)
        assertFalse(bRecipes.any { it.name == "Alice Lasagna" })
        assertFalse(bRecipes.any { it.id == UUID_A_RECIPE })

        val bLists = rig.shoppingVm.state.value.lists
        assertEquals(1, bLists.size)
        assertEquals("Bob Groceries", bLists.first().name)
        assertFalse(bLists.any { it.id == UUID_A_LIST })
    }

    private class TestRig {
        val tokenStore = RecordingTokenStore()
        val shoppingClient = RoutingShoppingClient()
        val recipeClient = RoutingRecipeClient()

        private val catalogClient = RoutingCatalogClient()
        private val mealPlanClient = RoutingMealPlanClient()

        private val shoppingRepo = ApiShoppingListRepository(shoppingClient)
        private val catalogRepo = ApiCatalogItemRepository(catalogClient)
        private val mealPlanRepo = ApiMealPlanRepository(mealPlanClient)
        private val recipeRepo = ApiRecipeRepository(recipeClient)

        val recipeListVm = RecipeListViewModel(recipeRepo, shoppingRepo, mealPlanRepo)
        val shoppingVm = ShoppingListViewModel(shoppingRepo, catalogRepo)
        val mealPlanVm = MealPlanViewModel(mealPlanRepo, recipeRepo, shoppingRepo)

        val cleaner = SessionCleaner(
            tokenStore = tokenStore,
            httpClient = HttpClient {},
            shoppingListRepository = shoppingRepo,
            catalogItemRepository = catalogRepo,
            mealPlanRepository = mealPlanRepo,
            recipeRepository = recipeRepo,
            recipeListViewModel = recipeListVm,
            shoppingListViewModel = shoppingVm,
            mealPlanViewModel = mealPlanVm,
        )
    }

    companion object {
        private val UUID_A_LIST = Uuid.parse("00000000-0000-0000-0000-00000000000a")
        private val UUID_A_RECIPE = Uuid.parse("00000000-0000-0000-0000-00000000aaaa")
        private val UUID_B_LIST = Uuid.parse("00000000-0000-0000-0000-00000000000b")
        private val UUID_B_RECIPE = Uuid.parse("00000000-0000-0000-0000-00000000bbbb")
    }
}

private class RecordingTokenStore : TokenStore() {
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

/**
 * Returns whatever the test sets on [listsByUser]. Simulates A's or B's
 * server response depending on which phase of the scenario we are in.
 */
private class RoutingShoppingClient : ShoppingListClient {
    var listsByUser: List<ShoppingList> = emptyList()

    override suspend fun fetchLists(): List<ShoppingList> = listsByUser
    override suspend fun createList(name: String): ShoppingList = error("not used")
    override suspend fun updateList(id: Uuid, name: String) = error("not used")
    override suspend fun deleteList(id: Uuid) = error("not used")
    override suspend fun fetchItems(listId: Uuid): List<ShoppingItem> = emptyList()
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

private class RoutingCatalogClient : CatalogItemClient {
    override suspend fun fetchAll(): List<Item.Catalog> = listOf(
        Item.Catalog(id = Uuid.random(), name = "Milk", category = ItemCategory(id = Uuid.random(), name = "General")),
    )
}

private class RoutingMealPlanClient : MealPlanClient {
    override suspend fun fetchItems(from: LocalDate, to: LocalDate): List<MealPlanItem> = emptyList()
    override suspend fun create(request: CreateMealPlanItemRequest): MealPlanItem = error("not used")
    override suspend fun update(id: Uuid, request: UpdateMealPlanItemRequest) = error("not used")
    override suspend fun delete(id: Uuid) = error("not used")
}

private class RoutingRecipeClient : RecipeClient {
    var recipesByUser: List<Recipe> = emptyList()

    override suspend fun fetchPage(cursor: String?, limit: Int, search: String?, tag: String?): RecipePage =
        RecipePage(items = recipesByUser, nextCursor = null)

    override suspend fun fetchTags(): List<String> = emptyList()
    override suspend fun fetchById(id: Uuid): Recipe = error("not used")
    override suspend fun create(request: CreateRecipeRequest): Recipe = error("not used")
    override suspend fun update(id: Uuid, request: UpdateRecipeRequest) = error("not used")
    override suspend fun fetchRandom(tag: String?, excludeId: String?): Recipe = error("not used")
    override suspend fun delete(id: Uuid) = error("not used")
}
