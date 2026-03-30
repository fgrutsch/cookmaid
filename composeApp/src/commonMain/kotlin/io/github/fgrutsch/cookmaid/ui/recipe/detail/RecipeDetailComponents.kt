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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.common_add_to_meal_plan
import cookmaid.composeapp.generated.resources.common_add_to_shopping_list
import cookmaid.composeapp.generated.resources.common_back
import cookmaid.composeapp.generated.resources.common_delete
import cookmaid.composeapp.generated.resources.common_edit
import cookmaid.composeapp.generated.resources.common_options
import cookmaid.composeapp.generated.resources.recipe_detail_description
import cookmaid.composeapp.generated.resources.recipe_detail_ingredients
import cookmaid.composeapp.generated.resources.recipe_detail_not_found
import cookmaid.composeapp.generated.resources.recipe_detail_steps
import cookmaid.composeapp.generated.resources.recipe_detail_tags
import cookmaid.composeapp.generated.resources.recipe_detail_title
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.common.resolve

@Composable
internal fun RecipeDetailTopBar(
    recipeName: String?,
    showMenu: Boolean,
    hasIngredients: Boolean,
    onBack: () -> Unit,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    actions: RecipeMenuActions,
) {
    TopAppBar(
        title = { Text(recipeName ?: Res.string.recipe_detail_title.resolve()) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Res.string.common_back.resolve())
            }
        },
        actions = {
            IconButton(onClick = onShowMenu) {
                Icon(Icons.Default.MoreVert, contentDescription = Res.string.common_options.resolve())
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
                if (hasIngredients) {
                    DropdownMenuItem(
                        text = { Text(Res.string.common_add_to_shopping_list.resolve()) },
                        onClick = actions.onAddToShoppingList,
                    )
                }
                DropdownMenuItem(
                    text = { Text(Res.string.common_add_to_meal_plan.resolve()) },
                    onClick = actions.onAddToMealPlan,
                )
            }
        },
    )
}

internal data class RecipeMenuActions(
    val onEdit: () -> Unit,
    val onDelete: () -> Unit,
    val onAddToShoppingList: () -> Unit,
    val onAddToMealPlan: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecipeContent(recipe: Recipe, padding: PaddingValues) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val description = recipe.description
        if (!description.isNullOrBlank()) {
            DescriptionSection(
                description = description,
                onLinkClick = { uriHandler.openUri(it) },
            )
        }
        recipe.servings?.let { servings ->
            Text(
                "$servings servings",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (recipe.tags.isNotEmpty()) {
            TagsSection(tags = recipe.tags)
        }
        if (recipe.ingredients.isNotEmpty()) {
            IngredientsSection(ingredients = recipe.ingredients)
        }
        if (recipe.steps.isNotEmpty()) {
            StepsSection(steps = recipe.steps)
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

@Composable
internal fun IngredientsSection(ingredients: List<RecipeIngredient>) {
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
                        qty,
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
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
