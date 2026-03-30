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
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.common_add
import cookmaid.composeapp.generated.resources.common_cancel
import cookmaid.composeapp.generated.resources.common_quantity
import cookmaid.composeapp.generated.resources.common_remove
import cookmaid.composeapp.generated.resources.recipe_detail_ingredients
import cookmaid.composeapp.generated.resources.recipe_detail_steps
import cookmaid.composeapp.generated.resources.recipe_detail_tags
import cookmaid.composeapp.generated.resources.recipe_edit_add_ingredient
import cookmaid.composeapp.generated.resources.recipe_edit_add_step
import cookmaid.composeapp.generated.resources.recipe_edit_description_label
import cookmaid.composeapp.generated.resources.recipe_edit_name_label
import cookmaid.composeapp.generated.resources.recipe_edit_name_required
import cookmaid.composeapp.generated.resources.recipe_edit_new_tag_title
import cookmaid.composeapp.generated.resources.recipe_edit_servings_label
import cookmaid.composeapp.generated.resources.recipe_edit_tag_name_label
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.common.resolve

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
            label = { Text(Res.string.recipe_edit_name_label.resolve()) },
            singleLine = true,
            isError = state.nameError,
            supportingText = if (state.nameError) {
                { Text(Res.string.recipe_edit_name_required.resolve()) }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = { onEvent(AddRecipeEvent.SetDescription(it)) },
            label = { Text(Res.string.recipe_edit_description_label.resolve()) },
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        ServingsSelector(
            value = state.servings,
            onValueChange = { onEvent(AddRecipeEvent.SetServings(it)) },
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
            Res.string.recipe_detail_ingredients.resolve(),
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
                        ingredientQuantityInput.ifBlank { null },
                    ))
                    onQuantityInputClear()
                }
            },
            onAddCatalogItem = { item ->
                onEvent(AddRecipeEvent.AddIngredient(item, ingredientQuantityInput.ifBlank { null }))
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
            Res.string.recipe_detail_steps.resolve(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        steps.forEachIndexed { index, step ->
            ListItem(
                headlineContent = { Text("${index + 1}. $step") },
                trailingContent = {
                    IconButton(onClick = { onRemoveStep(index) }) {
                        Icon(Icons.Default.Close, contentDescription = Res.string.common_remove.resolve())
                    }
                },
            )
        }
        OutlinedTextField(
            value = stepInput,
            onValueChange = onStepInputChange,
            label = { Text(Res.string.recipe_edit_add_step.resolve()) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAddStep() }),
            trailingIcon = {
                if (stepInput.isNotBlank()) {
                    IconButton(onClick = onAddStep) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = Res.string.recipe_edit_add_step.resolve(),
                        )
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
            Res.string.recipe_detail_tags.resolve(),
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
                Icon(Icons.Default.Add, contentDescription = Res.string.common_add.resolve())
            }
        }
    }
}

@Composable
internal fun IngredientRow(
    name: String,
    quantity: String?,
    onQuantityChange: (String?) -> Unit,
    onRemove: () -> Unit,
) {
    var qtyText by remember(quantity) {
        mutableStateOf(quantity.orEmpty())
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
                qtyText = value
                onQuantityChange(qtyText.ifBlank { null })
            },
            label = { Text(Res.string.common_quantity.resolve()) },
            singleLine = true,
            modifier = Modifier.width(80.dp),
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = Res.string.common_remove.resolve())
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
                label = { Text(Res.string.recipe_edit_add_ingredient.resolve()) },
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
            onValueChange = onQuantityChange,
            label = { Text(Res.string.common_quantity.resolve()) },
            singleLine = true,
            modifier = Modifier.width(80.dp),
        )
        IconButton(onClick = onAddFreeText) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = Res.string.common_add.resolve())
        }
    }
}

@Composable
internal fun ServingsSelector(
    value: Int?,
    onValueChange: (Int?) -> Unit,
) {
    val current = value ?: 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            Res.string.recipe_edit_servings_label.resolve(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = { onValueChange(if (current > 1) current - 1 else null) },
                enabled = current > 0,
            ) {
                Icon(Icons.Default.Remove, contentDescription = Res.string.common_remove.resolve())
            }
            Text(
                text = if (current > 0) current.toString() else "–",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { onValueChange(current + 1) }) {
                Icon(Icons.Default.Add, contentDescription = Res.string.common_add.resolve())
            }
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
        title = { Text(Res.string.recipe_edit_new_tag_title.resolve()) },
        text = {
            OutlinedTextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text(Res.string.recipe_edit_tag_name_label.resolve()) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(tagName) },
                enabled = tagName.isNotBlank(),
            ) {
                Text(Res.string.common_add.resolve())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Res.string.common_cancel.resolve()) }
        },
    )
}
