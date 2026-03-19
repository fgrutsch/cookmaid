package io.github.fgrutsch.cookmaid.ui.recipe.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.fgrutsch.cookmaid.ui.common.SuccessSnackbarHost
import io.github.fgrutsch.cookmaid.ui.mealplan.DayPickerDialog
import io.github.fgrutsch.cookmaid.ui.mealplan.IngredientPickerDialog
import io.github.fgrutsch.cookmaid.ui.shopping.formatQuantity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val onEvent = viewModel::onEvent
    var showMenu by remember { mutableStateOf(false) }
    var showIngredientPicker by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        onEvent(RecipeDetailEvent.Load)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RecipeDetailEffect.Deleted -> onBack()
                is RecipeDetailEffect.AddedToShoppingList ->
                    snackbarHostState.showSnackbar("Added to shopping list")
                is RecipeDetailEffect.AddedToMealPlan ->
                    snackbarHostState.showSnackbar("Added to meal plan")
                is RecipeDetailEffect.Error ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    SuccessSnackbarHost(snackbarHostState) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.recipe?.name ?: "Recipe") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onEvent(RecipeDetailEvent.Delete)
                            },
                        )
                        if (state.recipe?.ingredients?.isNotEmpty() == true) {
                            DropdownMenuItem(
                                text = { Text("Add to shopping list") },
                                onClick = {
                                    showMenu = false
                                    showIngredientPicker = true
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Add to meal plan") },
                            onClick = {
                                showMenu = false
                                showDayPicker = true
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        state.recipe?.let { r ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (r.tags.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Tags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            r.tags.forEach { tag ->
                                AssistChip(onClick = {}, label = { Text(tag) })
                            }
                        }
                    }
                }

                if (r.ingredients.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Ingredients",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        r.ingredients.forEach { ingredient ->
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
                                Text(ingredient.item.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                ingredient.quantity?.let { qty ->
                                    Text(
                                        formatQuantity(qty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                if (r.steps.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Steps",
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
                            r.steps.forEachIndexed { index, step ->
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
            }
        } ?: Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Recipe not found")
        }
    }
    }

    if (showIngredientPicker) {
        state.recipe?.let { r ->
            if (r.ingredients.isNotEmpty()) {
                IngredientPickerDialog(
                    recipeName = r.name,
                    ingredients = r.ingredients,
                    onAdd = { selected ->
                        onEvent(RecipeDetailEvent.AddIngredientsToShoppingList(selected))
                        showIngredientPicker = false
                    },
                    onDismiss = { showIngredientPicker = false },
                )
            } else {
                showIngredientPicker = false
            }
        }
    }

    if (showDayPicker) {
        state.recipe?.let { recipe ->
            DayPickerDialog(
                resolveDayItems = { emptyList() },
                onSelect = { day ->
                    onEvent(RecipeDetailEvent.AddToMealPlan(recipe.id, day))
                    showDayPicker = false
                },
                onDismiss = { showDayPicker = false },
            )
        }
    }
}
