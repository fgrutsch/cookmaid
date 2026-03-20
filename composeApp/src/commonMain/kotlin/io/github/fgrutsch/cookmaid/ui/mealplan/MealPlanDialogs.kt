package io.github.fgrutsch.cookmaid.ui.mealplan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.fgrutsch.cookmaid.mealplan.WEEK_END_OFFSET
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.common.formatShortDate
import io.github.fgrutsch.cookmaid.ui.shopping.formatQuantity
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

@Composable
fun AddMealPlanItemDialog(
    day: LocalDate,
    recipeSearchResults: List<Recipe>,
    onSearchRecipes: (String) -> Unit,
    onAddRecipe: (recipeId: Uuid) -> Unit,
    onAddNote: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val options = listOf("Recipe", "Note")

    var noteName by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to ${formatDayNameShort(day)}") },
        text = {
            Column {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) {
                            Text(label)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                when (selectedTab) {
                    0 -> RecipePickerTab(
                        results = recipeSearchResults,
                        onSearchChanged = onSearchRecipes,
                        onSelect = onAddRecipe,
                    )
                    1 -> OutlinedTextField(
                        value = noteName,
                        onValueChange = { noteName = it },
                        label = { Text("Note or URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    )
                }
            }
        },
        confirmButton = {
            if (selectedTab == 1) {
                TextButton(
                    onClick = { if (noteName.isNotBlank()) onAddNote(noteName) },
                    enabled = noteName.isNotBlank(),
                ) { Text("Add") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun EditNoteDialog(
    currentName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit note") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Note or URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name) },
                enabled = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun IngredientPickerDialog(
    recipeName: String,
    ingredients: List<RecipeIngredient>,
    onAdd: (List<RecipeIngredient>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(emptySet<RecipeIngredient>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(recipeName) },
        text = {
            Column {
                IngredientPickerSelectAllRow(
                    allSelected = selected.size == ingredients.size,
                    onToggleAll = {
                        selected = if (selected.size == ingredients.size) emptySet() else ingredients.toSet()
                    },
                )
                IngredientPickerList(
                    ingredients = ingredients,
                    selected = selected,
                    onToggle = { ingredient, isSelected ->
                        selected = if (isSelected) selected - ingredient else selected + ingredient
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(selected.toList()) },
                enabled = selected.isNotEmpty(),
            ) {
                Text("Add (${selected.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun IngredientPickerSelectAllRow(
    allSelected: Boolean,
    onToggleAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onToggleAll) {
            Text(if (allSelected) "Deselect all" else "Select all")
        }
    }
}

@Composable
private fun IngredientPickerList(
    ingredients: List<RecipeIngredient>,
    selected: Set<RecipeIngredient>,
    onToggle: (RecipeIngredient, Boolean) -> Unit,
) {
    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
        items(ingredients) { ingredient ->
            val isSelected = ingredient in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(ingredient, isSelected) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle(ingredient, isSelected) },
                )
                Text(
                    text = ingredient.item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                ingredient.quantity?.let { qty ->
                    Text(
                        text = formatQuantity(qty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipePickerTab(
    results: List<Recipe>,
    onSearchChanged: (String) -> Unit,
    onSelect: (Uuid) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { onSearchChanged("") }

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearchChanged(it)
            },
            placeholder = { Text("Search recipes...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
            items(results, key = { it.id }) { recipe ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(recipe.id) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                ) {
                    Text(recipe.name, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun DayPickerDialog(
    viewModel: DayPickerViewModel,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val weekEnd = state.weekStart.plus(WEEK_END_OFFSET, DateTimeUnit.DAY)
    val days = (0..WEEK_END_OFFSET).map { state.weekStart.plus(it, DateTimeUnit.DAY) }

    LaunchedEffect(Unit) { viewModel.onEvent(DayPickerEvent.Load) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to meal plan") },
        text = {
            Column {
                DayPickerWeekNavigation(
                    weekStart = state.weekStart,
                    weekEnd = weekEnd,
                    onPrevious = { viewModel.onEvent(DayPickerEvent.PreviousWeek) },
                    onNext = { viewModel.onEvent(DayPickerEvent.NextWeek) },
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                    )
                } else {
                    DayPickerDayList(
                        days = days,
                        today = today,
                        resolveDayItems = { date -> state.itemsByDay[date].orEmpty() },
                        onSelect = onSelect,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DayPickerWeekNavigation(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
        }
        Text(
            text = "${formatShortDate(weekStart)} - ${formatShortDate(weekEnd)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
        }
    }
}

@Composable
private fun DayPickerDayList(
    days: List<LocalDate>,
    today: LocalDate,
    resolveDayItems: (LocalDate) -> List<String>,
    onSelect: (LocalDate) -> Unit,
) {
    days.forEach { date ->
        val items = resolveDayItems(date)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(date) }
                .padding(vertical = 8.dp, horizontal = 4.dp),
        ) {
            Text(
                text = formatDayNameShort(date),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (date == today) FontWeight.Bold else null,
                color = if (date == today) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (items.isNotEmpty()) {
                Text(
                    text = items.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatDayNameShort(date: LocalDate): String {
    val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$dayName, ${formatShortDate(date)}"
}
