package io.github.fgrutsch.cookmaid.ui.recipe.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.shopping.formatQuantity

@Composable
internal fun AddRecipeContent(
    state: AddRecipeState,
    ingredientQuantityInput: String,
    stepInput: String,
    padding: PaddingValues,
    onEvent: (AddRecipeEvent) -> Unit,
    onIngredientQuantityChange: (String) -> Unit,
    onStepInputChange: (String) -> Unit,
    onShowNewTagDialog: () -> Unit,
) {
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
        OutlinedTextField(
            value = state.description,
            onValueChange = { onEvent(AddRecipeEvent.SetDescription(it)) },
            label = { Text("Description (optional)") },
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        IngredientsSection(
            ingredients = state.ingredients,
            ingredientQuery = state.ingredientQuery,
            ingredientQuantityInput = ingredientQuantityInput,
            ingredientSuggestions = state.ingredientSuggestions,
            onEvent = onEvent,
            onQuantityInputChange = onIngredientQuantityChange,
            onQuantityInputClear = { onIngredientQuantityChange("") },
        )
        HorizontalDivider()
        StepsSection(
            steps = state.steps,
            stepInput = stepInput,
            onStepInputChange = onStepInputChange,
            onAddStep = { onEvent(AddRecipeEvent.AddStep(stepInput)); onStepInputChange("") },
            onRemoveStep = { onEvent(AddRecipeEvent.RemoveStep(it)) },
        )
        HorizontalDivider()
        TagsSection(
            availableTags = state.availableTags,
            selectedTags = state.selectedTags,
            onToggleTag = { onEvent(AddRecipeEvent.ToggleTag(it)) },
            onShowNewTagDialog = onShowNewTagDialog,
        )
    }
}

@Composable
internal fun IngredientsSection(
    ingredients: List<RecipeIngredient>,
    ingredientQuery: String,
    ingredientQuantityInput: String,
    ingredientSuggestions: List<Item.Catalog>,
    onEvent: (AddRecipeEvent) -> Unit,
    onQuantityInputChange: (String) -> Unit,
    onQuantityInputClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Ingredients",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        ingredients.forEachIndexed { index, ingredient ->
            IngredientRow(
                name = ingredient.item.name,
                quantity = ingredient.quantity,
                onQuantityChange = { onEvent(AddRecipeEvent.UpdateIngredientQuantity(index, it)) },
                onRemove = { onEvent(AddRecipeEvent.RemoveIngredient(index)) },
            )
        }
        IngredientAddField(
            query = ingredientQuery,
            quantityInput = ingredientQuantityInput,
            suggestions = ingredientSuggestions,
            onQueryChange = { onEvent(AddRecipeEvent.UpdateIngredientQuery(it)) },
            onQuantityChange = onQuantityInputChange,
            onAddFreeText = {
                if (ingredientQuery.isNotBlank()) {
                    onEvent(AddRecipeEvent.AddIngredient(
                        Item.FreeText(name = ingredientQuery.trim()),
                        ingredientQuantityInput.toFloatOrNull(),
                    ))
                    onQuantityInputClear()
                }
            },
            onAddCatalogItem = { item ->
                onEvent(AddRecipeEvent.AddIngredient(item, ingredientQuantityInput.toFloatOrNull()))
                onQuantityInputClear()
            },
        )
    }
}

@Composable
internal fun StepsSection(
    steps: List<String>,
    stepInput: String,
    onStepInputChange: (String) -> Unit,
    onAddStep: () -> Unit,
    onRemoveStep: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Steps",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        steps.forEachIndexed { index, step ->
            ListItem(
                headlineContent = { Text("${index + 1}. $step") },
                trailingContent = {
                    IconButton(onClick = { onRemoveStep(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                },
            )
        }
        OutlinedTextField(
            value = stepInput,
            onValueChange = onStepInputChange,
            label = { Text("Add step") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAddStep() }),
            trailingIcon = {
                if (stepInput.isNotBlank()) {
                    IconButton(onClick = onAddStep) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add step")
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagsSection(
    availableTags: List<String>,
    selectedTags: List<String>,
    onToggleTag: (String) -> Unit,
    onShowNewTagDialog: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Tags",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            availableTags.forEach { tag ->
                FilterChip(
                    selected = tag in selectedTags,
                    onClick = { onToggleTag(tag) },
                    label = { Text(tag) },
                )
            }
            IconButton(onClick = onShowNewTagDialog) {
                Icon(Icons.Default.Add, contentDescription = "Add tag")
            }
        }
    }
}

@Composable
internal fun IngredientRow(
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
        verticalAlignment = Alignment.CenterVertically,
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
internal fun IngredientAddField(
    query: String,
    quantityInput: String,
    suggestions: List<Item.Catalog>,
    onQueryChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onAddFreeText: () -> Unit,
    onAddCatalogItem: (Item.Catalog) -> Unit,
) {
    val showSuggestions = suggestions.isNotEmpty() && query.isNotEmpty()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
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
internal fun NewTagDialog(
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
