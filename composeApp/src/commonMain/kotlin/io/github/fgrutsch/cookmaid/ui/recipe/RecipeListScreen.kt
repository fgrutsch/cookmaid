package io.github.fgrutsch.cookmaid.ui.recipe

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.mealplan.DayPickerDialog
import io.github.fgrutsch.cookmaid.ui.common.SuccessSnackbarHost
import io.github.fgrutsch.cookmaid.ui.mealplan.IngredientPickerDialog

@Composable
fun RecipeListScreen(
    viewModel: RecipeListViewModel,
    onRecipeClick: (String) -> Unit,
    onAddRecipe: () -> Unit,
) {
    val recipes by viewModel.filteredRecipes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchActive by viewModel.searchActive.collectAsState()
    val randomRecipe by viewModel.randomRecipe.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    var ingredientPickerRecipeId by remember { mutableStateOf<String?>(null) }
    var dayPickerRecipeId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    SuccessSnackbarHost(snackbarHostState) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search recipes...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
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
                        Text("Recipes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                actions = {
                    if (searchActive) {
                        IconButton(onClick = { viewModel.setSearchActive(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    } else {
                        IconButton(onClick = { viewModel.setSearchActive(true) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { viewModel.rollRandomRecipe() }) {
                            Icon(Icons.Default.Casino, contentDescription = "Random recipe")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRecipe) {
                Icon(Icons.Default.Add, contentDescription = "Add recipe")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            if (tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = tag in selectedTags,
                            onClick = { viewModel.toggleTagFilter(tag) },
                            label = { Text(tag) },
                        )
                    }
                }
            }


            if (recipes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No recipes found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                val grouped = recipes.groupBy { recipe ->
                    recipe.tags.firstOrNull() ?: "Uncategorized"
                }.entries.sortedBy { if (it.key == "Uncategorized") "zzz" else it.key }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    grouped.forEach { (tag, tagRecipes) ->
                        item(key = "header-$tag") {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    start = 4.dp,
                                    top = if (tag == grouped.first().key) 0.dp else 8.dp,
                                ),
                            )
                        }
                        items(tagRecipes, key = { it.id }) { recipe ->
                            RecipeCard(
                                recipe = recipe,
                                onClick = { onRecipeClick(recipe.id) },
                                onAddToShoppingList = { ingredientPickerRecipeId = recipe.id },
                                onAddToMealPlan = { dayPickerRecipeId = recipe.id },
                            )
                        }
                    }
                }
            }
        }
    }
    }

    randomRecipe?.let { recipe ->
        RandomRecipeDialog(
            recipe = recipe,
            onView = {
                viewModel.clearRandomRecipe()
                onRecipeClick(recipe.id)
            },
            onReroll = { viewModel.rollRandomRecipe() },
            onAddToShoppingList = {
                viewModel.clearRandomRecipe()
                ingredientPickerRecipeId = recipe.id
            },
            onAddToMealPlan = {
                viewModel.clearRandomRecipe()
                dayPickerRecipeId = recipe.id
            },
            onDismiss = { viewModel.clearRandomRecipe() },
        )
    }

    ingredientPickerRecipeId?.let { recipeId ->
        val ingredients = viewModel.resolveRecipeIngredients(recipeId)
        if (ingredients.isNotEmpty()) {
            IngredientPickerDialog(
                recipeName = viewModel.resolveRecipeName(recipeId),
                ingredients = ingredients,
                onAdd = { selected ->
                    viewModel.addIngredientsToShoppingList(selected)
                    ingredientPickerRecipeId = null
                    scope.launch { snackbarHostState.showSnackbar("Added to shopping list") }
                },
                onDismiss = { ingredientPickerRecipeId = null },
            )
        } else {
            ingredientPickerRecipeId = null
        }
    }

    dayPickerRecipeId?.let { recipeId ->
        DayPickerDialog(
            resolveDayItems = { date -> viewModel.resolveMealPlanDayItems(date) },
            onSelect = { dayDate ->
                viewModel.addRecipeToMealPlan(recipeId, dayDate)
                dayPickerRecipeId = null
                scope.launch { snackbarHostState.showSnackbar("Added to meal plan") }
            },
            onDismiss = { dayPickerRecipeId = null },
        )
    }
}

@Composable
private fun RecipeCard(
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
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
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
                        "${recipe.ingredients.size} ingredients · ${recipe.steps.size} steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
                    DropdownMenuItem(
                        text = { Text("Add to meal plan") },
                        onClick = {
                            showMenu = false
                            onAddToMealPlan()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RandomRecipeDialog(
    recipe: Recipe,
    onView: () -> Unit,
    onReroll: () -> Unit,
    onAddToShoppingList: () -> Unit,
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
                    Icon(Icons.Default.Refresh, contentDescription = "Re-roll")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onView) { Text("View details") }
                TextButton(onClick = onAddToShoppingList) { Text("Add to shopping list") }
                TextButton(onClick = onAddToMealPlan) { Text("Add to meal plan") }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
