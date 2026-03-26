package io.github.fgrutsch.cookmaid.ui.recipe.list

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.common_added_to_meal_plan
import cookmaid.composeapp.generated.resources.common_added_to_shopping_list
import cookmaid.composeapp.generated.resources.recipe_list_add
import io.github.fgrutsch.cookmaid.ui.common.SuccessSnackbarHost
import io.github.fgrutsch.cookmaid.ui.common.resolve
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.rememberResourceEnvironment
import io.github.fgrutsch.cookmaid.ui.mealplan.DayPickerDialog
import io.github.fgrutsch.cookmaid.ui.mealplan.DayPickerViewModel
import io.github.fgrutsch.cookmaid.ui.mealplan.IngredientPickerDialog
import kotlin.uuid.Uuid
import org.koin.compose.koinInject

/**
 * Paginated recipe list with search, tag filtering, and pull-to-refresh.
 *
 * @param viewModel the recipe list view model.
 * @param onRecipeClick called when a recipe is tapped.
 * @param onAddRecipe called when the add FAB is tapped.
 */
@Composable
@Suppress("LongMethod")
fun RecipeListScreen(
    viewModel: RecipeListViewModel,
    onRecipeClick: (Uuid) -> Unit,
    onAddRecipe: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val onEvent = viewModel::onEvent
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    var ingredientPickerRecipeId by remember { mutableStateOf<Uuid?>(null) }
    var dayPickerRecipeId by remember { mutableStateOf<Uuid?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val env = rememberResourceEnvironment()

    val addRecipeMsg = Res.string.recipe_list_add.resolve()

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalItems - PAGINATION_THRESHOLD
        }
    }

    LaunchedEffect(Unit) {
        onEvent(RecipeListEvent.LoadRecipes)
        viewModel.effects.collect { effect ->
            when (effect) {
                is RecipeListEffect.AddedToShoppingList ->
                    snackbarHostState.showSnackbar(getString(env, Res.string.common_added_to_shopping_list))
                is RecipeListEffect.AddedToMealPlan ->
                    snackbarHostState.showSnackbar(getString(env, Res.string.common_added_to_meal_plan))
                is RecipeListEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onEvent(RecipeListEvent.LoadMore) }

    SuccessSnackbarHost(snackbarHostState) {
    Scaffold(
        topBar = {
            RecipeListTopBar(
                searchActive = state.searchActive,
                searchQuery = state.searchQuery,
                searchFocusRequester = searchFocusRequester,
                onSearchQueryChange = { onEvent(RecipeListEvent.UpdateSearchQuery(it)) },
                onSearchDismiss = { keyboardController?.hide() },
                onCloseSearch = { onEvent(RecipeListEvent.SetSearchActive(false)) },
                onOpenSearch = { onEvent(RecipeListEvent.SetSearchActive(true)) },
                onRandomRecipe = { onEvent(RecipeListEvent.RollRandomRecipe) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecipe) {
                Icon(Icons.Default.Add, contentDescription = addRecipeMsg)
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(RecipeListEvent.Refresh) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            RecipeListContent(
                state = state,
                listState = listState,
                onTagClick = { onEvent(RecipeListEvent.SelectTag(it)) },
                onRecipeClick = onRecipeClick,
                onAddToShoppingList = { ingredientPickerRecipeId = it },
                onAddToMealPlan = { dayPickerRecipeId = it },
            )
        }
    }
    }

    state.randomRecipe?.let { recipe ->
        RandomRecipeDialog(
            recipe = recipe,
            onView = {
                onEvent(RecipeListEvent.ClearRandomRecipe)
                onRecipeClick(recipe.id)
            },
            onReroll = { onEvent(RecipeListEvent.RollRandomRecipe) },
            onAddToShoppingList = if (recipe.ingredients.isNotEmpty()) {{
                onEvent(RecipeListEvent.ClearRandomRecipe)
                ingredientPickerRecipeId = recipe.id
            }} else null,
            onAddToMealPlan = {
                onEvent(RecipeListEvent.ClearRandomRecipe)
                dayPickerRecipeId = recipe.id
            },
            onDismiss = { onEvent(RecipeListEvent.ClearRandomRecipe) },
        )
    }

    ingredientPickerRecipeId?.let { recipeId ->
        val ingredients = viewModel.resolveRecipeIngredients(recipeId)
        if (ingredients.isNotEmpty()) {
            IngredientPickerDialog(
                recipeName = viewModel.resolveRecipeName(recipeId),
                ingredients = ingredients,
                onAdd = { selected ->
                    onEvent(RecipeListEvent.AddIngredientsToShoppingList(selected))
                    ingredientPickerRecipeId = null
                },
                onDismiss = { ingredientPickerRecipeId = null },
            )
        } else {
            ingredientPickerRecipeId = null
        }
    }

    dayPickerRecipeId?.let { recipeId ->
        DayPickerDialog(
            viewModel = koinInject<DayPickerViewModel>(),
            onSelect = { day ->
                onEvent(RecipeListEvent.AddToMealPlan(recipeId, day))
                dayPickerRecipeId = null
            },
            onDismiss = { dayPickerRecipeId = null },
        )
    }
}
