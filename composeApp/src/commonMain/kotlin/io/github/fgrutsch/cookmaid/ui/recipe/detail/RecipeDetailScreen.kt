package io.github.fgrutsch.cookmaid.ui.recipe.detail

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.fgrutsch.cookmaid.ui.common.SuccessSnackbarHost
import io.github.fgrutsch.cookmaid.ui.mealplan.DayPickerDialog
import io.github.fgrutsch.cookmaid.ui.mealplan.DayPickerViewModel
import io.github.fgrutsch.cookmaid.ui.mealplan.IngredientPickerDialog
import org.koin.compose.koinInject

/**
 * Recipe detail screen showing description, tags, ingredients, and steps
 * with actions for edit, delete, add to shopping list / meal plan.
 *
 * @param viewModel the recipe detail view model.
 * @param onBack called when navigating back.
 * @param onEdit called when the edit action is selected.
 */
@Composable
@Suppress("LongMethod")
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val onEvent = viewModel::onEvent
    var showMenu by remember { mutableStateOf(false) }
    var showIngredientPicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        onEvent(RecipeDetailEvent.Load)
        viewModel.effects.collect { effect ->
            when (effect) {
                is RecipeDetailEffect.Deleted -> onBack()
                is RecipeDetailEffect.AddedToShoppingList -> snackbarHostState.showSnackbar("Added to shopping list")
                is RecipeDetailEffect.AddedToMealPlan -> snackbarHostState.showSnackbar("Added to meal plan")
                is RecipeDetailEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    SuccessSnackbarHost(snackbarHostState) {
    Scaffold(
        topBar = {
            RecipeDetailTopBar(
                recipeName = state.recipe?.name,
                showMenu = showMenu,
                hasIngredients = state.recipe?.ingredients?.isNotEmpty() == true,
                onBack = onBack,
                onShowMenu = { showMenu = true },
                onDismissMenu = { showMenu = false },
                actions = RecipeMenuActions(
                    onEdit = { showMenu = false; onEdit() },
                    onDelete = { showMenu = false; onEvent(RecipeDetailEvent.Delete) },
                    onAddToShoppingList = { showMenu = false; showIngredientPicker = true },
                    onAddToMealPlan = { showMenu = false; showDayPicker = true },
                ),
            )
        },
    ) { padding ->
        state.recipe?.let { r ->
            RecipeContent(recipe = r, padding = padding)
        } ?: RecipeNotFound(padding = padding)
    }
    }

    if (showIngredientPicker) {
        state.recipe?.let { r ->
            if (r.ingredients.isNotEmpty()) {
                IngredientPickerDialog(
                    recipeName = r.name,
                    ingredients = r.ingredients,
                    onAdd = { selected ->
                        onEvent(RecipeDetailEvent.AddIngredientsToShoppingList(selected))
                        showIngredientPicker = false
                    },
                    onDismiss = { showIngredientPicker = false },
                )
            } else {
                showIngredientPicker = false
            }
        }
    }

    if (showDayPicker) {
        state.recipe?.let { recipe ->
            DayPickerDialog(
                viewModel = koinInject<DayPickerViewModel>(),
                onSelect = { day ->
                    onEvent(RecipeDetailEvent.AddToMealPlan(recipe.id, day))
                    showDayPicker = false
                },
                onDismiss = { showDayPicker = false },
            )
        }
    }
}
