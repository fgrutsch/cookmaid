package io.github.fgrutsch.cookmaid.ui.recipe.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import cookmaid.app.shared.generated.resources.Res
import cookmaid.app.shared.generated.resources.common_add_to_meal_plan
import cookmaid.app.shared.generated.resources.common_add_to_shopping_list
import cookmaid.app.shared.generated.resources.common_back
import cookmaid.app.shared.generated.resources.common_cancel
import cookmaid.app.shared.generated.resources.common_delete
import cookmaid.app.shared.generated.resources.common_edit
import cookmaid.app.shared.generated.resources.common_options
import cookmaid.app.shared.generated.resources.ic_add
import cookmaid.app.shared.generated.resources.ic_arrow_back
import cookmaid.app.shared.generated.resources.ic_calendar_month
import cookmaid.app.shared.generated.resources.ic_close
import cookmaid.app.shared.generated.resources.ic_more_vert
import cookmaid.app.shared.generated.resources.ic_remove
import cookmaid.app.shared.generated.resources.ic_shopping_cart
import cookmaid.app.shared.generated.resources.recipe_delete_confirm_message
import cookmaid.app.shared.generated.resources.recipe_delete_confirm_title
import cookmaid.app.shared.generated.resources.recipe_detail_decrease_servings
import cookmaid.app.shared.generated.resources.recipe_detail_description
import cookmaid.app.shared.generated.resources.recipe_detail_increase_servings
import cookmaid.app.shared.generated.resources.recipe_detail_ingredients
import cookmaid.app.shared.generated.resources.recipe_detail_not_found
import cookmaid.app.shared.generated.resources.recipe_detail_steps
import cookmaid.app.shared.generated.resources.recipe_detail_tags
import cookmaid.app.shared.generated.resources.recipe_detail_title
import cookmaid.app.shared.generated.resources.recipe_edit_servings_label
import io.github.fgrutsch.cookmaid.common.SupportedLocale
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.common.LocalAppLocale
import io.github.fgrutsch.cookmaid.ui.common.resolve
import io.github.fgrutsch.cookmaid.ui.theme.appCardColors
import io.github.fgrutsch.cookmaid.ui.theme.appTopAppBarColors
import org.jetbrains.compose.resources.painterResource

private val FAB_CLEARANCE = 96.dp

@Composable
internal fun RecipeDetailTopBar(
    recipeName: String?,
    showMenu: Boolean,
    onBack: () -> Unit,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    actions: RecipeMenuActions,
) {
    TopAppBar(
        title = { Text(recipeName ?: Res.string.recipe_detail_title.resolve()) },
        colors = appTopAppBarColors(),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = Res.string.common_back.resolve())
            }
        },
        actions = {
            IconButton(onClick = onShowMenu) {
                Icon(
                    painterResource(Res.drawable.ic_more_vert),
                    contentDescription = Res.string.common_options.resolve(),
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text(Res.string.common_edit.resolve()) },
                    onClick = actions.onEdit,
                )
                DropdownMenuItem(
                    text = { Text(Res.string.common_delete.resolve()) },
                    onClick = actions.onDelete,
                )
            }
        },
    )
}

/**
 * The overflow menu holds only what manages the recipe record itself. Putting the recipe to use —
 * adding it to a list or a plan — belongs to [RecipeActionMenu], within thumb reach rather than
 * the far corner.
 */
internal data class RecipeMenuActions(
    val onEdit: () -> Unit,
    val onDelete: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecipeContent(
    recipe: Recipe,
    servings: Int?,
    onServingsChange: (Int) -> Unit,
    padding: PaddingValues,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            // Bottom clearance so the FAB menu does not sit on the last section.
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = FAB_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val description = recipe.description
        if (!description.isNullOrBlank()) {
            DescriptionSection(
                description = description,
                onLinkClick = { uriHandler.openUri(it) },
            )
        }
        // A recipe without configured servings scales to 0, which leaves quantities untouched.
        val baseServings = recipe.servings ?: 0
        val currentServings = servings ?: baseServings

        if (baseServings > 0) {
            ServingsStepper(servings = currentServings, onServingsChange = onServingsChange)
        }
        if (recipe.ingredients.isNotEmpty()) {
            IngredientsSection(
                ingredients = recipe.ingredients,
                servings = currentServings,
                baseServings = baseServings,
            )
        }
        if (recipe.steps.isNotEmpty()) {
            StepsSection(steps = recipe.steps)
        }
        if (recipe.tags.isNotEmpty()) {
            TagsSection(tags = recipe.tags)
        }
    }
}

private val urlPattern = Regex("https?://\\S+", RegexOption.IGNORE_CASE)

@Composable
internal fun DescriptionSection(description: String, onLinkClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            Res.string.recipe_detail_description.resolve(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        val linkColor = MaterialTheme.colorScheme.primary
        val annotated = buildAnnotatedString {
            append(description)
            urlPattern.findAll(description).forEach { match ->
                addStyle(
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    match.range.first,
                    match.range.last + 1,
                )
                addLink(
                    LinkAnnotation.Clickable("URL") { onLinkClick(match.value) },
                    match.range.first,
                    match.range.last + 1,
                )
            }
        }
        Text(
            text = annotated,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagsSection(tags: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            Res.string.recipe_detail_tags.resolve(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tags.forEach { tag ->
                AssistChip(onClick = {}, label = { Text(tag) })
            }
        }
    }
}

/**
 * Read-only servings control for the detail screen. Distinct from the edit screen's
 * `ServingsSelector`, which can unset servings entirely; here the count never drops below 1.
 */
@Composable
private fun ServingsStepper(servings: Int, onServingsChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            Res.string.recipe_edit_servings_label.resolve(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onServingsChange(servings - 1) },
                enabled = servings > 1,
            ) {
                Icon(
                    painterResource(Res.drawable.ic_remove),
                    contentDescription = Res.string.recipe_detail_decrease_servings.resolve(),
                )
            }
            Text(
                servings.toString(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 32.dp),
            )
            IconButton(onClick = { onServingsChange(servings + 1) }) {
                Icon(
                    painterResource(Res.drawable.ic_add),
                    contentDescription = Res.string.recipe_detail_increase_servings.resolve(),
                )
            }
        }
    }
}

@Composable
internal fun IngredientsSection(ingredients: List<RecipeIngredient>, servings: Int, baseServings: Int) {
    // Amounts the user wrote without decimals fall back to the separator their language uses.
    // LocalAppLocale carries a full locale ("de_DE", "de-AT"), so compare the language subtag.
    val language = LocalAppLocale.current.substringBefore('_').substringBefore('-')
    val decimalSeparator = if (SupportedLocale.fromCode(language) == SupportedLocale.DE) ',' else '.'

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            Res.string.recipe_detail_ingredients.resolve(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        ingredients.forEach { ingredient ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Text(
                    "•",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    ingredient.item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                ingredient.quantity?.let { qty ->
                    Text(
                        scaleQuantity(qty, servings, baseServings, decimalSeparator),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun StepsSection(steps: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            Res.string.recipe_detail_steps.resolve(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = appCardColors(),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                steps.forEachIndexed { index, step ->
                    ListItem(
                        headlineContent = { Text(step) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecipeNotFound(padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(Res.string.recipe_detail_not_found.resolve())
    }
}

/**
 * The recipe's forward actions, as an expanding FAB menu.
 *
 * These used to live in the overflow menu. Tap count is unchanged — two either way — but the
 * target sits in the thumb zone rather than the opposite corner, and the actions are visible
 * rather than hidden behind a kebab that also holds delete.
 *
 * @param expanded whether the menu is open.
 * @param onExpandedChange called when the toggle is tapped.
 * @param hasIngredients whether to offer the shopping list action; a recipe without ingredients
 *   has nothing to add.
 * @param onAddToShoppingList called when the shopping list action is chosen.
 * @param onAddToMealPlan called when the meal plan action is chosen.
 * @param modifier placement within the screen; the menu brings its own edge padding.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RecipeActionMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    hasIngredients: Boolean,
    onAddToShoppingList: () -> Unit,
    onAddToMealPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Hoisted: the container colour lambda is driven by the expand animation and is not a
    // composable scope, so MaterialTheme cannot be read inside it.
    val containerColor = MaterialTheme.colorScheme.tertiary
    val contentColor = MaterialTheme.colorScheme.onTertiary

    FloatingActionButtonMenu(
        modifier = modifier,
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = onExpandedChange,
                containerColor = { containerColor },
            ) {
                Icon(
                    painterResource(if (expanded) Res.drawable.ic_close else Res.drawable.ic_add),
                    contentDescription = Res.string.common_options.resolve(),
                    tint = contentColor,
                )
            }
        },
    ) {
        if (hasIngredients) {
            FloatingActionButtonMenuItem(
                onClick = onAddToShoppingList,
                icon = { Icon(painterResource(Res.drawable.ic_shopping_cart), contentDescription = null) },
                text = { Text(Res.string.common_add_to_shopping_list.resolve()) },
            )
        }
        FloatingActionButtonMenuItem(
            onClick = onAddToMealPlan,
            icon = { Icon(painterResource(Res.drawable.ic_calendar_month), contentDescription = null) },
            text = { Text(Res.string.common_add_to_meal_plan.resolve()) },
        )
    }
}

/**
 * Confirms deleting a recipe.
 *
 * Deleting takes the recipe's ingredients and steps with it and cannot be undone, so it is the
 * most destructive per-item action in the app — it was firing straight from the overflow menu on
 * a single tap.
 *
 * @param onConfirm called when the user confirms the deletion.
 * @param onDismiss called when the dialog is dismissed.
 */
@Composable
internal fun DeleteRecipeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Res.string.recipe_delete_confirm_title.resolve()) },
        text = { Text(Res.string.recipe_delete_confirm_message.resolve()) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(Res.string.common_delete.resolve()) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Res.string.common_cancel.resolve()) }
        },
    )
}
