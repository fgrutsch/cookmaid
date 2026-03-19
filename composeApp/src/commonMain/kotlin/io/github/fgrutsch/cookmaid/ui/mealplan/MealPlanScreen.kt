package io.github.fgrutsch.cookmaid.ui.mealplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fgrutsch.cookmaid.mealplan.MealPlanDay
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.common.SuccessSnackbarHost
import io.github.fgrutsch.cookmaid.ui.common.SwipeToDeleteItem
import io.github.fgrutsch.cookmaid.ui.common.formatShortDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.uuid.Uuid

private val urlPattern = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)

private fun isUrl(text: String): Boolean = urlPattern.matches(text.trim())

@Composable
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

    LaunchedEffect(Unit) {
        onEvent(MealPlanEvent.Load)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MealPlanEffect.IngredientsAdded ->
                    snackbarHostState.showSnackbar("Added to shopping list")
                is MealPlanEffect.ShowIngredientPicker ->
                    ingredientPickerState = IngredientPickerState(effect.recipeName, effect.ingredients)
                is MealPlanEffect.Error ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    SuccessSnackbarHost(snackbarHostState) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Plan") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    IconButton(onClick = { onEvent(MealPlanEvent.GoToCurrentWeek) }) {
                        Icon(Icons.Default.Today, contentDescription = "Go to current week")
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { onEvent(MealPlanEvent.Refresh) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            WeekNavigationBar(
                weekStart = state.currentWeekStart,
                onPrevious = { onEvent(MealPlanEvent.PreviousWeek) },
                onNext = { onEvent(MealPlanEvent.NextWeek) },
            )

            LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.days, key = { it.date.toString() }) { day ->
                        DayCard(
                            day = day,
                            onAddItem = { addItemForDay = day.date },
                            onItemClick = { item ->
                                when (item) {
                                    is MealPlanItem.Recipe -> onRecipeClick(item.recipeId)
                                    is MealPlanItem.Note -> {
                                        if (isUrl(item.name)) {
                                            uriHandler.openUri(item.name.trim())
                                        } else {
                                            editingNote = EditNoteState(day.date, item.id, item.name)
                                        }
                                    }
                                }
                            },
                            onDeleteItem = { itemId -> onEvent(MealPlanEvent.DeleteItem(itemId, day.date)) },
                            onAddToShoppingList = { item ->
                                if (item is MealPlanItem.Recipe) {
                                    onEvent(MealPlanEvent.AddRecipeToShoppingList(item.recipeId, item.recipeName))
                                }
                            },
                        )
                    }
                }
        }
        }
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
                viewModel.addIngredientsToShoppingList(selectedIngredients)
                ingredientPickerState = null
            },
            onDismiss = { ingredientPickerState = null },
        )
    }
}

private data class EditNoteState(
    val day: LocalDate,
    val itemId: Uuid,
    val currentName: String,
)

private data class IngredientPickerState(
    val recipeName: String,
    val ingredients: List<RecipeIngredient>,
)

@Composable
private fun WeekNavigationBar(
    weekStart: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)
    val label = formatDateRange(weekStart, weekEnd)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
        }
    }
}

@Composable
private fun DayCard(
    day: MealPlanDay,
    onAddItem: () -> Unit,
    onItemClick: (MealPlanItem) -> Unit,
    onDeleteItem: (Uuid) -> Unit,
    onAddToShoppingList: (MealPlanItem) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatDayName(day.date.dayOfWeek),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = formatShortDate(day.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onAddItem) {
                    Icon(Icons.Default.Add, contentDescription = "Add item")
                }
            }

            if (day.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                day.items.forEach { item ->
                    SwipeToDeleteItem(onDelete = { onDeleteItem(item.id) }) {
                        MealPlanItemRow(
                            item = item,
                            onClick = { onItemClick(item) },
                            onAddToShoppingList = { onAddToShoppingList(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealPlanItemRow(
    item: MealPlanItem,
    onClick: () -> Unit,
    onAddToShoppingList: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = when (item) {
            is MealPlanItem.Recipe -> Icons.AutoMirrored.Filled.MenuBook
            is MealPlanItem.Note -> Icons.AutoMirrored.Filled.Notes
        }
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = when (item) {
                is MealPlanItem.Recipe -> item.recipeName
                is MealPlanItem.Note -> item.name
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (item is MealPlanItem.Recipe) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Add to shopping list") },
                        onClick = {
                            showMenu = false
                            onAddToShoppingList()
                        },
                    )
                }
            }
        }
    }
}

private fun formatDayName(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "Monday"
    DayOfWeek.TUESDAY -> "Tuesday"
    DayOfWeek.WEDNESDAY -> "Wednesday"
    DayOfWeek.THURSDAY -> "Thursday"
    DayOfWeek.FRIDAY -> "Friday"
    DayOfWeek.SATURDAY -> "Saturday"
    DayOfWeek.SUNDAY -> "Sunday"
}

private fun formatDateRange(start: LocalDate, end: LocalDate): String {
    return "${formatShortDate(start)} - ${formatShortDate(end)}"
}
