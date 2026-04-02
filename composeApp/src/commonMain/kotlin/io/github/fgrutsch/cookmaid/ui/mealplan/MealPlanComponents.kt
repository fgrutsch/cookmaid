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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.common_add_to_shopping_list
import cookmaid.composeapp.generated.resources.ic_add
import cookmaid.composeapp.generated.resources.ic_keyboard_arrow_left
import cookmaid.composeapp.generated.resources.ic_keyboard_arrow_right
import cookmaid.composeapp.generated.resources.ic_menu_book
import cookmaid.composeapp.generated.resources.ic_more_vert
import cookmaid.composeapp.generated.resources.ic_notes
import cookmaid.composeapp.generated.resources.common_next_week
import cookmaid.composeapp.generated.resources.common_options
import cookmaid.composeapp.generated.resources.common_previous_week
import cookmaid.composeapp.generated.resources.day_friday
import cookmaid.composeapp.generated.resources.day_monday
import cookmaid.composeapp.generated.resources.day_saturday
import cookmaid.composeapp.generated.resources.day_sunday
import cookmaid.composeapp.generated.resources.day_thursday
import cookmaid.composeapp.generated.resources.day_tuesday
import cookmaid.composeapp.generated.resources.day_wednesday
import cookmaid.composeapp.generated.resources.meal_plan_add_item
import io.github.fgrutsch.cookmaid.mealplan.MealPlanDay
import io.github.fgrutsch.cookmaid.mealplan.MealPlanItem
import io.github.fgrutsch.cookmaid.mealplan.WEEK_END_OFFSET
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.common.SwipeItem
import io.github.fgrutsch.cookmaid.ui.common.formatShortDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import io.github.fgrutsch.cookmaid.ui.common.resolve
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.uuid.Uuid

internal val urlPattern = Regex("^https?://\\S+$", RegexOption.IGNORE_CASE)

internal fun isUrl(text: String): Boolean = urlPattern.matches(text.trim())

internal data class EditNoteState(
    val day: LocalDate,
    val itemId: Uuid,
    val currentName: String,
)

internal data class IngredientPickerState(
    val recipeName: String,
    val ingredients: List<RecipeIngredient>,
)

@Composable
internal fun MealPlanContent(
    state: MealPlanState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onAddItem: (LocalDate) -> Unit,
    onItemClick: (LocalDate, MealPlanItem) -> Unit,
    onDeleteItem: (Uuid, LocalDate) -> Unit,
    onAddToShoppingList: (MealPlanItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        WeekNavigationBar(
            weekStart = state.currentWeekStart,
            onPrevious = onPreviousWeek,
            onNext = onNextWeek,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.days, key = { it.date.toString() }) { day ->
                DayCard(
                    day = day,
                    onAddItem = { onAddItem(day.date) },
                    onItemClick = { item -> onItemClick(day.date, item) },
                    onDeleteItem = { itemId -> onDeleteItem(itemId, day.date) },
                    onAddToShoppingList = onAddToShoppingList,
                )
            }
        }
    }
}

@Composable
internal fun WeekNavigationBar(
    weekStart: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val weekEnd = weekStart.plus(WEEK_END_OFFSET, DateTimeUnit.DAY)
    val label = formatDateRange(weekStart, weekEnd)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                painterResource(Res.drawable.ic_keyboard_arrow_left),
                contentDescription = Res.string.common_previous_week.resolve(),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        IconButton(onClick = onNext) {
            Icon(
                painterResource(Res.drawable.ic_keyboard_arrow_right),
                contentDescription = Res.string.common_next_week.resolve(),
            )
        }
    }
}

@Composable
internal fun DayCard(
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
                    Icon(
                        painterResource(Res.drawable.ic_add),
                        contentDescription = Res.string.meal_plan_add_item.resolve(),
                    )
                }
            }

            if (day.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                day.items.forEach { item ->
                    SwipeItem(onDelete = { onDeleteItem(item.id) }) {
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
internal fun MealPlanItemRow(
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
        val icon: DrawableResource = when (item) {
            is MealPlanItem.Recipe -> Res.drawable.ic_menu_book
            is MealPlanItem.Note -> Res.drawable.ic_notes
        }
        Icon(
            painterResource(icon),
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
                    Icon(
                        painterResource(Res.drawable.ic_more_vert),
                        contentDescription = Res.string.common_options.resolve(),
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(Res.string.common_add_to_shopping_list.resolve()) },
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

@Composable
internal fun formatDayName(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> Res.string.day_monday.resolve()
    DayOfWeek.TUESDAY -> Res.string.day_tuesday.resolve()
    DayOfWeek.WEDNESDAY -> Res.string.day_wednesday.resolve()
    DayOfWeek.THURSDAY -> Res.string.day_thursday.resolve()
    DayOfWeek.FRIDAY -> Res.string.day_friday.resolve()
    DayOfWeek.SATURDAY -> Res.string.day_saturday.resolve()
    DayOfWeek.SUNDAY -> Res.string.day_sunday.resolve()
}

@Composable
internal fun formatDateRange(start: LocalDate, end: LocalDate): String {
    return "${formatShortDate(start)} - ${formatShortDate(end)}"
}
