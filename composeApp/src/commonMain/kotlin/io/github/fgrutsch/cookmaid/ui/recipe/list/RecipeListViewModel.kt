package io.github.fgrutsch.cookmaid.ui.recipe.list

import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.common.MviViewModel
import io.github.fgrutsch.cookmaid.ui.common.addIngredientsToDefaultShoppingList
import io.github.fgrutsch.cookmaid.ui.common.addRecipeToMealPlan
import io.github.fgrutsch.cookmaid.ui.mealplan.MealPlanRepository
import io.github.fgrutsch.cookmaid.ui.recipe.RecipeRepository
import io.github.fgrutsch.cookmaid.ui.shopping.ShoppingListRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

@OptIn(FlowPreview::class)
class RecipeListViewModel(
    private val repository: RecipeRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val mealPlanRepository: MealPlanRepository,
) : MviViewModel<RecipeListState, RecipeListEvent, RecipeListEffect>(RecipeListState()) {

    private val searchQueryFlow = MutableStateFlow("")

    init {
        searchQueryFlow
            .drop(1)
            .debounce(300)
            .onEach { fetchFirstPage() }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: RecipeListEvent) {
        when (event) {
            is RecipeListEvent.LoadRecipes -> loadRecipes()
            is RecipeListEvent.Refresh -> refresh()
            is RecipeListEvent.LoadMore -> loadMore()
            is RecipeListEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is RecipeListEvent.SetSearchActive -> setSearchActive(event.active)
            is RecipeListEvent.SelectTag -> selectTag(event.tag)
            is RecipeListEvent.RollRandomRecipe -> rollRandomRecipe()
            is RecipeListEvent.ClearRandomRecipe -> updateState { copy(randomRecipe = null) }
            is RecipeListEvent.DeleteRecipe -> deleteRecipe(event.id)
            is RecipeListEvent.AddIngredientsToShoppingList -> addToShoppingList(event.ingredients)
            is RecipeListEvent.AddToMealPlan -> addToMealPlan(event.recipeId, event.day)
        }
    }

    private fun loadRecipes() {
        val firstLoad = !state.value.initialized
        launch {
            if (firstLoad) updateState { copy(isLoading = true) }
            val tags = repository.fetchTags()
            val s = state.value
            val search = s.searchQuery.takeIf { it.isNotBlank() }
            val page = repository.fetchPage(cursor = null, search = search, tag = s.selectedTag)
            updateState {
                copy(
                    initialized = true,
                    availableTags = tags,
                    recipes = page.items,
                    nextCursor = page.nextCursor,
                    hasMore = page.nextCursor != null,
                    isLoading = false,
                )
            }
        }
    }

    private fun refresh() {
        launch {
            updateState { copy(isRefreshing = true) }
            val tags = repository.fetchTags()
            val s = state.value
            val search = s.searchQuery.takeIf { it.isNotBlank() }
            val page = repository.fetchPage(cursor = null, search = search, tag = s.selectedTag)
            updateState {
                copy(
                    availableTags = tags,
                    recipes = page.items,
                    nextCursor = page.nextCursor,
                    hasMore = page.nextCursor != null,
                    isRefreshing = false,
                )
            }
        }
    }

    private fun fetchFirstPage() {
        val s = state.value
        val search = s.searchQuery.takeIf { it.isNotBlank() }
        launch {
            updateState { copy(isLoading = true) }
            val page = repository.fetchPage(cursor = null, search = search, tag = s.selectedTag)
            updateState {
                copy(
                    recipes = page.items,
                    nextCursor = page.nextCursor,
                    hasMore = page.nextCursor != null,
                    isLoading = false,
                )
            }
        }
    }

    private fun loadMore() {
        val current = state.value
        if (current.isLoading || current.isRefreshing || current.isLoadingMore || !current.hasMore) return
        val search = current.searchQuery.takeIf { it.isNotBlank() }
        launch {
            updateState { copy(isLoadingMore = true) }
            val page = repository.fetchPage(
                cursor = current.nextCursor,
                search = search,
                tag = current.selectedTag,
            )
            updateState {
                copy(
                    recipes = recipes + page.items,
                    nextCursor = page.nextCursor,
                    hasMore = page.nextCursor != null,
                    isLoadingMore = false,
                )
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        updateState { copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    private fun setSearchActive(active: Boolean) {
        if (active) {
            updateState { copy(searchActive = true) }
        } else {
            updateState { copy(searchActive = false, searchQuery = "") }
            searchQueryFlow.value = ""
            fetchFirstPage()
        }
    }

    private fun selectTag(tag: String?) {
        val selected = if (tag == state.value.selectedTag) null else tag
        updateState { copy(selectedTag = selected) }
        fetchFirstPage()
    }

    private fun rollRandomRecipe() {
        val recipes = state.value.recipes
        if (recipes.isEmpty()) return
        val current = state.value.randomRecipe
        val candidates = if (recipes.size > 1) recipes.filter { it.id != current?.id } else recipes
        updateState { copy(randomRecipe = candidates.random()) }
    }

    private fun deleteRecipe(id: Uuid) {
        launchOptimistic(
            optimisticUpdate = { copy(recipes = recipes.filter { it.id != id }) },
        ) {
            repository.delete(id)
        }
    }

    fun resolveRecipeIngredients(recipeId: Uuid): List<RecipeIngredient> {
        return state.value.recipes.find { it.id == recipeId }?.ingredients ?: emptyList()
    }

    fun resolveRecipeName(recipeId: Uuid): String {
        return state.value.recipes.find { it.id == recipeId }?.name ?: "Unknown recipe"
    }

    private fun addToShoppingList(ingredients: List<RecipeIngredient>) {
        launch {
            addIngredientsToDefaultShoppingList(shoppingListRepository, ingredients)
            sendEffect(RecipeListEffect.AddedToShoppingList)
        }
    }

    private fun addToMealPlan(recipeId: Uuid, day: LocalDate) {
        launch {
            addRecipeToMealPlan(recipeId, day, mealPlanRepository)
            sendEffect(RecipeListEffect.AddedToMealPlan)
        }
    }

    override fun onError(e: Exception) {
        updateState { copy(isLoading = false, isRefreshing = false, isLoadingMore = false) }
        sendEffect(RecipeListEffect.Error("Something went wrong. Please try again."))
    }
}
