package io.github.fgrutsch.cookmaid.ui.shopping

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.common_cancel
import cookmaid.composeapp.generated.resources.common_ok
import cookmaid.composeapp.generated.resources.common_save
import cookmaid.composeapp.generated.resources.shopping_list_name_label
import cookmaid.composeapp.generated.resources.shopping_quantity_label
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.ui.common.resolve

@Composable
internal fun EditItemDialog(
    item: ShoppingItem,
    onDismiss: () -> Unit,
    onSave: (ShoppingItem) -> Unit,
) {
    var quantity by remember { mutableStateOf(item.quantity?.let { formatQuantity(it) }.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.item.name) },
        text = {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text(Res.string.shopping_quantity_label.resolve()) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedQuantity = quantity.trim().toFloatOrNull()
                    onSave(item.copy(quantity = parsedQuantity))
                },
            ) {
                Text(Res.string.common_save.resolve())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Res.string.common_cancel.resolve()) }
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
                label = { Text(Res.string.shopping_list_name_label.resolve()) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(Res.string.common_ok.resolve())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Res.string.common_cancel.resolve()) }
        },
    )
}
