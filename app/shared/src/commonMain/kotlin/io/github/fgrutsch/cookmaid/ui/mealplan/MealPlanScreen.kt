package io.github.fgrutsch.cookmaid.ui.mealplan

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import cookmaid.app.shared.generated.resources.Res
import cookmaid.app.shared.generated.resources.common_added_to_shopping_list
import cookmaid.app.shared.generated.resources.common_error
import cookmaid.app.shared.generated.resources.ic_today
import cookmaid.app.shared.generated.resources.meal_plan_go_to_current
import cookmaid.app.shared.generated.resources.meal_plan_title
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import io.github.fgrutsch.cookmaid.ui.common.SuccessSnackbarHost
import io.github.fgrutsch.cookmaid.ui.common.resolve
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.rememberResourceEnvironment
import kotlinx.datetime.LocalDate
import kotlin.uuid.Uuid

/**
 * Weekly meal plan screen with day cards, item management, and
 * week navigation.
 *
 * @param viewModel the meal plan view model.
 * @param onRecipeClick called when a recipe item is tapped.
 */
@Composable
@Suppress("LongMethod")
fun MealPlanScreen(
    viewModel: MealPlanViewModel,
    onRecipeClick: (Uuid) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val onEvent = viewModel::onEvent
    val uriHandler = LocalUriHandler.current

    var addItemForDay by remember { mutableStateOf<LocalDate?>(null) }
    var editingNote by remember { mutableStateOf<EditNoteState?>(null) }
    var ingredientPickerState by remember { mutableStateOf<IngredientPickerState?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val env = rememberResourceEnvironment()
    LaunchedEffect(Unit) {
        onEvent(MealPlanEvent.Load)
        viewModel.effects.collect { effect ->
            when (effect) {
                is MealPlanEffect.IngredientsAdded ->
                    snackbarHostState.showSnackbar(getString(env, Res.string.common_added_to_shopping_list))
                is MealPlanEffect.ShowIngredientPicker ->
                    ingredientPickerState = IngredientPickerState(effect.recipeName, effect.ingredients)
                is MealPlanEffect.Error ->
                    snackbarHostState.showSnackbar(getString(env, Res.string.common_error))
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SuccessSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(Res.string.meal_plan_title.resolve()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    IconButton(onClick = { onEvent(MealPlanEvent.GoToCurrentWeek) }) {
                        Icon(
                            painterResource(Res.drawable.ic_today),
                            contentDescription = Res.string.meal_plan_go_to_current.resolve(),
                        )
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(MealPlanEvent.Refresh) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            MealPlanContent(
                state = state,
                onPreviousWeek = { onEvent(MealPlanEvent.PreviousWeek) },
                onNextWeek = { onEvent(MealPlanEvent.NextWeek) },
                onAddItem = { addItemForDay = it },
                onItemClick = { day, item ->
                    when (item) {
                        is MealPlanItem.Recipe -> onRecipeClick(item.recipeId)
                        is MealPlanItem.Note -> {
                            if (isUrl(item.name)) {
                                uriHandler.openUri(item.name.trim())
                            } else {
                                editingNote = EditNoteState(day, item.id, item.name)
                            }
                        }
                    }
                },
                onDeleteItem = { itemId, day -> onEvent(MealPlanEvent.DeleteItem(itemId, day)) },
                onAddToShoppingList = { item ->
                    if (item is MealPlanItem.Recipe) {
                        onEvent(MealPlanEvent.AddRecipeToShoppingList(item.recipeId, item.recipeName))
                    }
                },
            )
        }
    }

    addItemForDay?.let { day ->
        AddMealPlanItemDialog(
            day = day,
            recipeSearchResults = state.recipeSearchResults,
            onSearchRecipes = { onEvent(MealPlanEvent.SearchRecipes(it)) },
            onAddRecipe = { recipeId ->
                onEvent(MealPlanEvent.AddRecipeItem(day, recipeId))
                addItemForDay = null
            },
            onAddNote = { name ->
                onEvent(MealPlanEvent.AddNoteItem(day, name))
                addItemForDay = null
            },
            onDismiss = { addItemForDay = null },
        )
    }

    editingNote?.let { noteState ->
        EditNoteDialog(
            currentName = noteState.currentName,
            onSave = { newName ->
                onEvent(MealPlanEvent.UpdateNote(noteState.itemId, noteState.day, newName))
                editingNote = null
            },
            onDismiss = { editingNote = null },
        )
    }

    ingredientPickerState?.let { pickerState ->
        IngredientPickerDialog(
            recipeName = pickerState.recipeName,
            ingredients = pickerState.ingredients,
            onAdd = { selectedIngredients ->
                onEvent(MealPlanEvent.AddIngredientsToShoppingList(selectedIngredients))
                ingredientPickerState = null
            },
            onDismiss = { ingredientPickerState = null },
        )
    }
}
