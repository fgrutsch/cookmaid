package io.github.fgrutsch.cookmaid.ui.recipe.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cookmaid.app.shared.generated.resources.Res
import cookmaid.app.shared.generated.resources.common_added_to_meal_plan
import cookmaid.app.shared.generated.resources.common_added_to_shopping_list
import cookmaid.app.shared.generated.resources.common_error
import io.github.fgrutsch.cookmaid.ui.common.SuccessSnackbarHost
import io.github.fgrutsch.cookmaid.ui.mealplan.DayPickerDialog
import io.github.fgrutsch.cookmaid.ui.mealplan.DayPickerViewModel
import io.github.fgrutsch.cookmaid.ui.mealplan.IngredientPickerDialog
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.rememberResourceEnvironment
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
    var fabExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val env = rememberResourceEnvironment()
    LaunchedEffect(Unit) {
        onEvent(RecipeDetailEvent.Load)
        viewModel.effects.collect { effect ->
            when (effect) {
                is RecipeDetailEffect.Deleted -> onBack()
                is RecipeDetailEffect.AddedToShoppingList ->
                    snackbarHostState.showSnackbar(getString(env, Res.string.common_added_to_shopping_list))
                is RecipeDetailEffect.AddedToMealPlan ->
                    snackbarHostState.showSnackbar(getString(env, Res.string.common_added_to_meal_plan))
                is RecipeDetailEffect.Error ->
                    snackbarHostState.showSnackbar(getString(env, Res.string.common_error))
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SuccessSnackbarHost(snackbarHostState) },
        topBar = {
            RecipeDetailTopBar(
                recipeName = state.recipe?.name,
                showMenu = showMenu,
                onBack = onBack,
                onShowMenu = { showMenu = true },
                onDismissMenu = { showMenu = false },
                actions = RecipeMenuActions(
                    onEdit = { showMenu = false; onEdit() },
                    onDelete = { showMenu = false; onEvent(RecipeDetailEvent.Delete) },
                ),
            )
        },
    ) { padding ->
        // Not in Scaffold's floatingActionButton slot: that slot adds its own 16dp, and
        // FloatingActionButtonMenu already carries FabMenuPaddingHorizontal internally, so the
        // button ended up 32dp from the edge while the recipe list's plain FAB sits at 16dp.
        Box(modifier = Modifier.fillMaxSize()) {
            state.recipe?.let { r ->
                RecipeContent(
                    recipe = r,
                    servings = state.servings,
                    onServingsChange = { onEvent(RecipeDetailEvent.SetServings(it)) },
                    padding = padding,
                )
            } ?: RecipeNotFound(padding = padding)

            RecipeActionMenu(
                expanded = fabExpanded,
                onExpandedChange = { fabExpanded = it },
                hasIngredients = state.recipe?.ingredients?.isNotEmpty() == true,
                onAddToShoppingList = { fabExpanded = false; showIngredientPicker = true },
                onAddToMealPlan = { fabExpanded = false; showDayPicker = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = padding.calculateBottomPadding()),
            )
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
