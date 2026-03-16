package io.github.fgrutsch.cookmaid.ui.mealplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Today
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fgrutsch.cookmaid.mealplan.MealPlanDay
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import kotlinx.coroutines.launch
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.common.formatShortDate
import io.github.fgrutsch.cookmaid.ui.common.SuccessSnackbarHost
import io.github.fgrutsch.cookmaid.ui.common.SwipeToDeleteItem
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private val urlPattern = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)

private fun isUrl(text: String): Boolean = urlPattern.matches(text.trim())

@Composable
fun MealPlanScreen(
    viewModel: MealPlanViewModel,
    onRecipeClick: (String) -> Unit,
) {
    val currentWeek by viewModel.currentWeek.collectAsState()
    val weekStart by viewModel.currentWeekStart.collectAsState()
    val uriHandler = LocalUriHandler.current

    var addItemForDay by remember { mutableStateOf<LocalDate?>(null) }
    var editingNote by remember { mutableStateOf<EditNoteState?>(null) }
    var ingredientPickerState by remember { mutableStateOf<IngredientPickerState?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                    IconButton(onClick = { viewModel.goToCurrentWeek() }) {
                        Icon(Icons.Default.Today, contentDescription = "Go to current week")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            WeekNavigationBar(
                weekStart = weekStart,
                onPrevious = { viewModel.previousWeek() },
                onNext = { viewModel.nextWeek() },
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(currentWeek.days, key = { it.date.toString() }) { day ->
                    DayCard(
                        day = day,
                        onAddItem = { addItemForDay = day.date },
                        onItemClick = { item ->
                            when (item) {
                                is MealPlanItem.RecipeItem -> onRecipeClick(item.recipeId)
                                is MealPlanItem.NoteItem -> {
                                    if (isUrl(item.name)) {
                                        uriHandler.openUri(item.name.trim())
                                    } else {
                                        editingNote = EditNoteState(day.date, item.id, item.name)
                                    }
                                }
                            }
                        },
                        onDeleteItem = { itemId -> viewModel.removeItem(day.date, itemId) },
                        onAddToShoppingList = { item ->
                            if (item is MealPlanItem.RecipeItem) {
                                val ingredients = viewModel.resolveRecipeIngredients(item.recipeId)
                                if (ingredients.isNotEmpty()) {
                                    ingredientPickerState = IngredientPickerState(
                                        recipeName = viewModel.resolveRecipeName(item.recipeId),
                                        ingredients = ingredients,
                                    )
                                }
                            }
                        },
                        resolveRecipeName = { recipeId -> viewModel.resolveRecipeName(recipeId) },
                    )
                }
            }
        }
    }
    }

    addItemForDay?.let { dayDate ->
        AddMealPlanItemDialog(
            dayDate = dayDate,
            recipes = viewModel.recipes.value,
            onAddRecipe = { recipeId ->
                viewModel.addRecipeItem(dayDate, recipeId)
                addItemForDay = null
            },
            onAddNote = { name ->
                viewModel.addNoteItem(dayDate, name)
                addItemForDay = null
            },
            onDismiss = { addItemForDay = null },
        )
    }

    editingNote?.let { state ->
        EditNoteDialog(
            currentName = state.currentName,
            onSave = { newName ->
                viewModel.updateNoteItem(state.dayDate, state.itemId, newName)
                editingNote = null
            },
            onDismiss = { editingNote = null },
        )
    }

    ingredientPickerState?.let { state ->
        IngredientPickerDialog(
            recipeName = state.recipeName,
            ingredients = state.ingredients,
            onAdd = { selectedIngredients ->
                viewModel.addIngredientsToShoppingList(selectedIngredients)
                ingredientPickerState = null
                scope.launch { snackbarHostState.showSnackbar("Added to shopping list") }
            },
            onDismiss = { ingredientPickerState = null },
        )
    }
}

private data class EditNoteState(
    val dayDate: LocalDate,
    val itemId: String,
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
    onDeleteItem: (String) -> Unit,
    onAddToShoppingList: (MealPlanItem) -> Unit,
    resolveRecipeName: (String) -> String,
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
                            resolveRecipeName = resolveRecipeName,
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
    resolveRecipeName: (String) -> String,
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
            is MealPlanItem.RecipeItem -> Icons.AutoMirrored.Filled.MenuBook
            is MealPlanItem.NoteItem -> Icons.AutoMirrored.Filled.Notes
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
                is MealPlanItem.RecipeItem -> resolveRecipeName(item.recipeId)
                is MealPlanItem.NoteItem -> item.name
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (item is MealPlanItem.RecipeItem) {
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
