package io.github.fgrutsch.ui.recipe

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import io.github.fgrutsch.catalog.Item
import io.github.fgrutsch.ui.shopping.formatQuantity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRecipeScreen(
    viewModel: AddRecipeViewModel,
    onBack: () -> Unit,
) {
    val name by viewModel.name.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val steps by viewModel.steps.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val nameError by viewModel.nameError.collectAsState()
    val ingredientQuery by viewModel.ingredientQuery.collectAsState()
    val ingredientSuggestions by viewModel.ingredientSuggestions.collectAsState()

    var stepInput by remember { mutableStateOf("") }
    var ingredientQuantityInput by remember { mutableStateOf("") }
    var showNewTagDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit Recipe" else "Add Recipe") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { if (viewModel.save()) onBack() }) {
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
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.setName(it) },
                label = { Text("Recipe name") },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) {{ Text("Name is required") }} else null,
                modifier = Modifier.fillMaxWidth(),
            )

            // Tags
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    availableTags.forEach { tag ->
                        FilterChip(
                            selected = tag in selectedTags,
                            onClick = { viewModel.toggleTag(tag) },
                            label = { Text(tag) },
                        )
                    }
                    IconButton(onClick = { showNewTagDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                }
            }

            HorizontalDivider()

            // Ingredients
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ingredients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                ingredients.forEachIndexed { index, ingredient ->
                    IngredientRow(
                        name = ingredient.item.name,
                        quantity = ingredient.quantity,
                        onQuantityChange = { viewModel.updateIngredientQuantity(index, it) },
                        onRemove = { viewModel.removeIngredient(index) },
                    )
                }
                IngredientAddField(
                    query = ingredientQuery,
                    quantityInput = ingredientQuantityInput,
                    suggestions = ingredientSuggestions,
                    onQueryChange = { viewModel.updateIngredientQuery(it) },
                    onQuantityChange = { ingredientQuantityInput = it },
                    onAddFreeText = {
                        if (ingredientQuery.isNotBlank()) {
                            viewModel.addIngredient(
                                Item.FreeTextItem(name = ingredientQuery.trim()),
                                ingredientQuantityInput.toFloatOrNull(),
                            )
                            ingredientQuantityInput = ""
                        }
                    },
                    onAddCatalogItem = { item ->
                        viewModel.addIngredient(item, ingredientQuantityInput.toFloatOrNull())
                        ingredientQuantityInput = ""
                    },
                )
            }

            HorizontalDivider()

            // Steps
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Steps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                steps.forEachIndexed { index, step ->
                    ListItem(
                        headlineContent = { Text("${index + 1}. $step") },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeStep(index) }) {
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
                        viewModel.addStep(stepInput)
                        stepInput = ""
                    }),
                    trailingIcon = {
                        if (stepInput.isNotBlank()) {
                            IconButton(onClick = {
                                viewModel.addStep(stepInput)
                                stepInput = ""
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add step")
                            }
                        }
                    },
                )
            }
        }
    }

    if (showNewTagDialog) {
        NewTagDialog(
            onDismiss = { showNewTagDialog = false },
            onConfirm = { tag ->
                viewModel.createAndAddTag(tag)
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
        mutableStateOf(quantity?.let { formatQuantity(it) } ?: "")
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
    suggestions: List<Item.CategorizedItem>,
    onQueryChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onAddFreeText: () -> Unit,
    onAddCatalogItem: (Item.CategorizedItem) -> Unit,
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
