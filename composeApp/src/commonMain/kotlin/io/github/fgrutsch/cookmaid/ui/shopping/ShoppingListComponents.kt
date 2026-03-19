package io.github.fgrutsch.cookmaid.ui.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddItemField(
    query: String,
    suggestions: List<Item.CatalogItem>,
    onQueryChange: (String) -> Unit,
    onAddFreeText: () -> Unit,
    onAddCatalogItem: (Item.CatalogItem) -> Unit,
) {
    val showSuggestions = suggestions.isNotEmpty() && query.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = showSuggestions,
        onExpandedChange = { },
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Add item...") },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAddFreeText() }),
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onAddFreeText) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Add")
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
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
}

@Composable
internal fun ShoppingItemRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = item.item.name,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color = if (item.checked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            )
        },
        modifier = Modifier.clickable(onClick = onToggle),
        leadingContent = {
            Checkbox(checked = item.checked, onCheckedChange = { onToggle() })
        },
        trailingContent = item.quantity?.let { qty ->
            {
                Text(
                    text = formatQuantity(qty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}


internal fun formatQuantity(quantity: Float): String {
    return if (quantity == quantity.toLong().toFloat()) {
        quantity.toLong().toString()
    } else {
        quantity.toString()
    }
}
