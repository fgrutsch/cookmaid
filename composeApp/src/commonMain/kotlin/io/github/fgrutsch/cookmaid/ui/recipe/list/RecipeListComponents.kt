package io.github.fgrutsch.cookmaid.ui.recipe.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.common_add_to_meal_plan
import cookmaid.composeapp.generated.resources.common_add_to_shopping_list
import cookmaid.composeapp.generated.resources.common_close
import cookmaid.composeapp.generated.resources.common_options
import cookmaid.composeapp.generated.resources.common_search
import cookmaid.composeapp.generated.resources.common_search_recipes
import cookmaid.composeapp.generated.resources.recipe_list_close_search
import cookmaid.composeapp.generated.resources.recipe_list_empty
import cookmaid.composeapp.generated.resources.recipe_list_random
import cookmaid.composeapp.generated.resources.recipe_list_reroll
import cookmaid.composeapp.generated.resources.recipe_list_title
import cookmaid.composeapp.generated.resources.recipe_card_summary
import cookmaid.composeapp.generated.resources.recipe_list_view_details
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.ui.common.resolve
import kotlin.uuid.Uuid

internal const val PAGINATION_THRESHOLD = 5

@Composable
internal fun RecipeListTopBar(
    searchActive: Boolean,
    searchQuery: String,
    searchFocusRequester: FocusRequester,
    onSearchQueryChange: (String) -> Unit,
    onSearchDismiss: () -> Unit,
    onCloseSearch: () -> Unit,
    onOpenSearch: () -> Unit,
    onRandomRecipe: () -> Unit,
) {
    TopAppBar(
        title = {
            if (searchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(Res.string.common_search_recipes.resolve()) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchDismiss() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                LaunchedEffect(Unit) {
                    searchFocusRequester.requestFocus()
                }
            } else {
                Text(Res.string.recipe_list_title.resolve())
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        actions = {
            if (searchActive) {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Default.Close, contentDescription = Res.string.recipe_list_close_search.resolve())
                }
            } else {
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Default.Search, contentDescription = Res.string.common_search.resolve())
                }
                IconButton(onClick = onRandomRecipe) {
                    Icon(Icons.Default.Casino, contentDescription = Res.string.recipe_list_random.resolve())
                }
            }
        },
    )
}

@Composable
internal fun TagFilterRow(
    tags: List<String>,
    selectedTag: String?,
    onTagClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            FilterChip(
                selected = tag == selectedTag,
                onClick = { onTagClick(tag) },
                label = { Text(tag) },
            )
        }
    }
}

@Composable
internal fun RecipeListContent(
    state: RecipeListState,
    listState: LazyListState,
    onTagClick: (String) -> Unit,
    onRecipeClick: (Uuid) -> Unit,
    onAddToShoppingList: (Uuid) -> Unit,
    onAddToMealPlan: (Uuid) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (state.availableTags.isNotEmpty()) {
            TagFilterRow(
                tags = state.availableTags,
                selectedTag = state.selectedTag,
                onTagClick = onTagClick,
            )
        }

        if (state.isLoading && state.recipes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.recipes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(Res.string.recipe_list_empty.resolve(), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.recipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onClick = { onRecipeClick(recipe.id) },
                        onAddToShoppingList = { onAddToShoppingList(recipe.id) },
                        onAddToMealPlan = { onAddToMealPlan(recipe.id) },
                    )
                }
                if (state.isLoadingMore) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onAddToShoppingList: () -> Unit,
    onAddToMealPlan: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (recipe.ingredients.isNotEmpty()) {
                    Text(
                        Res.string.recipe_card_summary.resolve(
                            recipe.ingredients.size,
                            recipe.steps.size,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            RecipeCardMenu(
                showMenu = showMenu,
                hasIngredients = recipe.ingredients.isNotEmpty(),
                onShowMenu = { showMenu = true },
                onDismissMenu = { showMenu = false },
                onAddToShoppingList = { showMenu = false; onAddToShoppingList() },
                onAddToMealPlan = { showMenu = false; onAddToMealPlan() },
            )
        }
    }
}

@Composable
internal fun RecipeCardMenu(
    showMenu: Boolean,
    hasIngredients: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onAddToShoppingList: () -> Unit,
    onAddToMealPlan: () -> Unit,
) {
    Box {
        IconButton(onClick = onShowMenu) {
            Icon(Icons.Default.MoreVert, contentDescription = Res.string.common_options.resolve())
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = onDismissMenu) {
            if (hasIngredients) {
                DropdownMenuItem(
                    text = { Text(Res.string.common_add_to_shopping_list.resolve()) },
                    onClick = onAddToShoppingList,
                )
            }
            DropdownMenuItem(
                text = { Text(Res.string.common_add_to_meal_plan.resolve()) },
                onClick = onAddToMealPlan,
            )
        }
    }
}

@Composable
internal fun RandomRecipeDialog(
    recipe: Recipe,
    onView: () -> Unit,
    onReroll: () -> Unit,
    onAddToShoppingList: (() -> Unit)?,
    onAddToMealPlan: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(recipe.name, modifier = Modifier.weight(1f))
                IconButton(onClick = onReroll) {
                    Icon(Icons.Default.Refresh, contentDescription = Res.string.recipe_list_reroll.resolve())
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onView) { Text(Res.string.recipe_list_view_details.resolve()) }
                onAddToShoppingList?.let {
                    TextButton(onClick = it) { Text(Res.string.common_add_to_shopping_list.resolve()) }
                }
                TextButton(onClick = onAddToMealPlan) { Text(Res.string.common_add_to_meal_plan.resolve()) }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Res.string.common_close.resolve()) }
        },
    )
}
