package io.github.fgrutsch.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.fgrutsch.catalog.Item
import io.github.fgrutsch.shopping.ShoppingItem

@Composable
internal fun EditItemDialog(
    item: ShoppingItem,
    categoryName: String,
    onDismiss: () -> Unit,
    onSave: (ShoppingItem) -> Unit,
) {
    var name by remember { mutableStateOf(item.item.name) }
    var quantity by remember { mutableStateOf(item.quantity?.let { formatQuantity(it) } ?: "") }

    val isCatalogItem = item.item is Item.CategorizedItem

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isCatalogItem) {
                    Text(
                        text = item.item.name,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (categoryName.isNotBlank()) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedQuantity = quantity.trim().toFloatOrNull()
                    val updatedItem = if (isCatalogItem) {
                        item.copy(quantity = parsedQuantity)
                    } else {
                        item.copy(item = Item.FreeTextItem(name = name.trim()), quantity = parsedQuantity)
                    }
                    onSave(updatedItem)
                },
                enabled = if (isCatalogItem) true else name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
internal fun ListNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("List name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
