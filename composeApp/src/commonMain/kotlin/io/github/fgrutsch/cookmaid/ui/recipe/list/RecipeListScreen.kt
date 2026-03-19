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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.runtime.derivedStateOf
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
import kotlin.uuid.Uuid
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.ui.common.SuccessSnackbarHost
import io.github.fgrutsch.cookmaid.ui.mealplan.DayPickerDialog
import io.github.fgrutsch.cookmaid.ui.mealplan.IngredientPickerDialog

@Composable
fun RecipeListScreen(
    viewModel: RecipeListViewModel,
    onRecipeClick: (Uuid) -> Unit,
    onAddRecipe: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val onEvent = viewModel::onEvent
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    var ingredientPickerRecipeId by remember { mutableStateOf<Uuid?>(null) }
    var dayPickerRecipeId by remember { mutableStateOf<Uuid?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalItems - 5
        }
    }

    LaunchedEffect(Unit) {
        onEvent(RecipeListEvent.LoadRecipes)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RecipeListEffect.AddedToShoppingList ->
                    snackbarHostState.showSnackbar("Added to shopping list")
                is RecipeListEffect.AddedToMealPlan ->
                    snackbarHostState.showSnackbar("Added to meal plan")
                is RecipeListEffect.Error ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onEvent(RecipeListEvent.LoadMore)
    }

    SuccessSnackbarHost(snackbarHostState) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.searchActive) {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = { onEvent(RecipeListEvent.UpdateSearchQuery(it)) },
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
                    if (state.searchActive) {
                        IconButton(onClick = { onEvent(RecipeListEvent.SetSearchActive(false)) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    } else {
                        IconButton(onClick = { onEvent(RecipeListEvent.SetSearchActive(true)) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { onEvent(RecipeListEvent.RollRandomRecipe) }) {
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
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(RecipeListEvent.Refresh) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.availableTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.availableTags.forEach { tag ->
                        FilterChip(
                            selected = tag == state.selectedTag,
                            onClick = { onEvent(RecipeListEvent.SelectTag(tag)) },
                            label = { Text(tag) },
                        )
                    }
                }
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
                    Text("No recipes found", style = MaterialTheme.typography.bodyLarge)
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
                            onAddToShoppingList = { ingredientPickerRecipeId = recipe.id },
                            onAddToMealPlan = { dayPickerRecipeId = recipe.id },
                        )
                    }
                    if (state.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
        }
    }
    }

    state.randomRecipe?.let { recipe ->
        RandomRecipeDialog(
            recipe = recipe,
            onView = {
                onEvent(RecipeListEvent.ClearRandomRecipe)
                onRecipeClick(recipe.id)
            },
            onReroll = { onEvent(RecipeListEvent.RollRandomRecipe) },
            onAddToShoppingList = if (recipe.ingredients.isNotEmpty()) {{
                onEvent(RecipeListEvent.ClearRandomRecipe)
                ingredientPickerRecipeId = recipe.id
            }} else null,
            onAddToMealPlan = {
                onEvent(RecipeListEvent.ClearRandomRecipe)
                dayPickerRecipeId = recipe.id
            },
            onDismiss = { onEvent(RecipeListEvent.ClearRandomRecipe) },
        )
    }

    ingredientPickerRecipeId?.let { recipeId ->
        val ingredients = viewModel.resolveRecipeIngredients(recipeId)
        if (ingredients.isNotEmpty()) {
            IngredientPickerDialog(
                recipeName = viewModel.resolveRecipeName(recipeId),
                ingredients = ingredients,
                onAdd = { selected ->
                    onEvent(RecipeListEvent.AddIngredientsToShoppingList(selected))
                    ingredientPickerRecipeId = null
                },
                onDismiss = { ingredientPickerRecipeId = null },
            )
        } else {
            ingredientPickerRecipeId = null
        }
    }

    dayPickerRecipeId?.let { recipeId ->
        DayPickerDialog(
            resolveDayItems = { emptyList() },
            onSelect = { day ->
                onEvent(RecipeListEvent.AddToMealPlan(recipeId, day))
                dayPickerRecipeId = null
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
                    if (recipe.ingredients.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Add to shopping list") },
                            onClick = {
                                showMenu = false
                                onAddToShoppingList()
                            },
                        )
                    }
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
                    Icon(Icons.Default.Refresh, contentDescription = "Re-roll")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onView) { Text("View details") }
                onAddToShoppingList?.let {
                    TextButton(onClick = it) { Text("Add to shopping list") }
                }
                TextButton(onClick = onAddToMealPlan) { Text("Add to meal plan") }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
