package io.github.fgrutsch.cookmaid.ui.recipe.list

import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.common.MviViewModel
import io.github.fgrutsch.cookmaid.ui.common.addIngredientsToDefaultShoppingList
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
            .debounce(SEARCH_DEBOUNCE_MILLIS)
            .onEach { fetchFirstPage() }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: RecipeListEvent) {
        when (event) {
            is RecipeListEvent.LoadRecipes -> loadWithTags(isRefresh = false)
            is RecipeListEvent.Refresh -> loadWithTags(isRefresh = true)
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

    private fun loadWithTags(isRefresh: Boolean) {
        launch {
            if (isRefresh) {
                updateState { copy(isRefreshing = true) }
            } else if (!state.value.initialized) {
                updateState { copy(isLoading = true) }
            }
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
        val canLoadMore = !current.isLoading && !current.isRefreshing && !current.isLoadingMore && current.hasMore
        if (!canLoadMore) return
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
        launch {
            updateState { copy(isLoadingRandom = true) }
            val current = state.value.randomRecipe
            val tag = state.value.selectedTag
            val recipe = repository.fetchRandom(
                tag = tag,
                excludeId = current?.id?.toString(),
            )
            updateState { copy(randomRecipe = recipe, isLoadingRandom = false) }
        }
    }

    private fun deleteRecipe(id: Uuid) {
        launchOptimistic(
            optimisticUpdate = { copy(recipes = recipes.filter { it.id != id }) },
        ) {
            repository.delete(id)
        }
    }

    fun resolveRecipeIngredients(recipeId: Uuid): List<RecipeIngredient> {
        return state.value.recipes.find { it.id == recipeId }?.ingredients.orEmpty()
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
            mealPlanRepository.create(day, recipeId = recipeId, note = null)
            sendEffect(RecipeListEffect.AddedToMealPlan)
        }
    }

    override fun onError(e: Exception) {
        updateState { copy(isLoading = false, isRefreshing = false, isLoadingMore = false, isLoadingRandom = false) }
        sendEffect(RecipeListEffect.Error("Something went wrong. Please try again."))
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
