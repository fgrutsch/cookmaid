package io.github.fgrutsch.cookmaid.ui.recipe.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.ui.shopping.formatQuantity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRecipeScreen(
    viewModel: AddRecipeViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val onEvent = viewModel::onEvent

    var stepInput by remember { mutableStateOf("") }
    var ingredientQuantityInput by remember { mutableStateOf("") }
    var showNewTagDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        onEvent(AddRecipeEvent.Load)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AddRecipeEffect.Saved -> onBack()
                is AddRecipeEffect.Error ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Recipe" else "Add Recipe") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(AddRecipeEvent.Save) }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onEvent(AddRecipeEvent.SetName(it)) },
                label = { Text("Recipe name") },
                singleLine = true,
                isError = state.nameError,
                supportingText = if (state.nameError) {{ Text("Name is required") }} else null,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ingredients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                state.ingredients.forEachIndexed { index, ingredient ->
                    IngredientRow(
                        name = ingredient.item.name,
                        quantity = ingredient.quantity,
                        onQuantityChange = { onEvent(AddRecipeEvent.UpdateIngredientQuantity(index, it)) },
                        onRemove = { onEvent(AddRecipeEvent.RemoveIngredient(index)) },
                    )
                }
                IngredientAddField(
                    query = state.ingredientQuery,
                    quantityInput = ingredientQuantityInput,
                    suggestions = state.ingredientSuggestions,
                    onQueryChange = { onEvent(AddRecipeEvent.UpdateIngredientQuery(it)) },
                    onQuantityChange = { ingredientQuantityInput = it },
                    onAddFreeText = {
                        if (state.ingredientQuery.isNotBlank()) {
                            onEvent(AddRecipeEvent.AddIngredient(
                                Item.FreeTextItem(name = state.ingredientQuery.trim()),
                                ingredientQuantityInput.toFloatOrNull(),
                            ))
                            ingredientQuantityInput = ""
                        }
                    },
                    onAddCatalogItem = { item ->
                        onEvent(AddRecipeEvent.AddIngredient(item, ingredientQuantityInput.toFloatOrNull()))
                        ingredientQuantityInput = ""
                    },
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                state.steps.forEachIndexed { index, step ->
                    ListItem(
                        headlineContent = { Text("${index + 1}. $step") },
                        trailingContent = {
                            IconButton(onClick = { onEvent(AddRecipeEvent.RemoveStep(index)) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        },
                    )
                }
                OutlinedTextField(
                    value = stepInput,
                    onValueChange = { stepInput = it },
                    label = { Text("Add step") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        onEvent(AddRecipeEvent.AddStep(stepInput))
                        stepInput = ""
                    }),
                    trailingIcon = {
                        if (stepInput.isNotBlank()) {
                            IconButton(onClick = {
                                onEvent(AddRecipeEvent.AddStep(stepInput))
                                stepInput = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add step")
                            }
                        }
                    },
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.availableTags.forEach { tag ->
                        FilterChip(
                            selected = tag in state.selectedTags,
                            onClick = { onEvent(AddRecipeEvent.ToggleTag(tag)) },
                            label = { Text(tag) },
                        )
                    }
                    IconButton(onClick = { showNewTagDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                }
            }
        }
    }

    if (showNewTagDialog) {
        NewTagDialog(
            onDismiss = { showNewTagDialog = false },
            onConfirm = { tag ->
                onEvent(AddRecipeEvent.CreateAndAddTag(tag))
                showNewTagDialog = false
            },
        )
    }
}

@Composable
private fun IngredientRow(
    name: String,
    quantity: Float?,
    onQuantityChange: (Float?) -> Unit,
    onRemove: () -> Unit,
) {
    var qtyText by remember(quantity) {
        mutableStateOf(quantity?.let { formatQuantity(it) }.orEmpty())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = qtyText,
            onValueChange = { value ->
                qtyText = value.filter { it.isDigit() || it == '.' }
                onQuantityChange(qtyText.toFloatOrNull())
            },
            label = { Text("Qty") },
            singleLine = true,
            modifier = Modifier.width(80.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove")
        }
    }
}

@Composable
private fun IngredientAddField(
    query: String,
    quantityInput: String,
    suggestions: List<Item.CatalogItem>,
    onQueryChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onAddFreeText: () -> Unit,
    onAddCatalogItem: (Item.CatalogItem) -> Unit,
) {
    val showSuggestions = suggestions.isNotEmpty() && query.isNotEmpty()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = showSuggestions,
            onExpandedChange = { },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text("Add ingredient") },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddFreeText() }),
            )
            ExposedDropdownMenu(
                expanded = showSuggestions,
                onDismissRequest = { },
            ) {
                suggestions.forEach { catalogItem ->
                    DropdownMenuItem(
                        text = { Text(catalogItem.name) },
                        onClick = { onAddCatalogItem(catalogItem) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = quantityInput,
            onValueChange = { value -> onQuantityChange(value.filter { it.isDigit() || it == '.' }) },
            label = { Text("Qty") },
            singleLine = true,
            modifier = Modifier.width(80.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        IconButton(onClick = onAddFreeText) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add")
        }
    }
}

@Composable
private fun NewTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var tagName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Tag") },
        text = {
            OutlinedTextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text("Tag name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(tagName) },
                enabled = tagName.isNotBlank(),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
