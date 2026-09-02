package io.github.fgrutsch.cookmaid.ui.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import cookmaid.app.shared.generated.resources.Res
import cookmaid.app.shared.generated.resources.common_add
import cookmaid.app.shared.generated.resources.ic_send
import cookmaid.app.shared.generated.resources.shopping_add_item
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.ui.common.NO_SUGGESTION
import io.github.fgrutsch.cookmaid.ui.common.SuggestionMenu
import io.github.fgrutsch.cookmaid.ui.common.resolve
import io.github.fgrutsch.cookmaid.ui.common.textFieldKeyboardControl
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddItemField(
    query: String,
    suggestions: List<Item.Catalog>,
    onQueryChange: (String) -> Unit,
    onAddFreeText: () -> Unit,
    onAddCatalogItem: (Item.Catalog) -> Unit,
) {
    val showSuggestions = suggestions.isNotEmpty() && query.isNotEmpty()
    var highlighted by remember(suggestions) { mutableStateOf(NO_SUGGESTION) }
    val commit = {
        val selected = suggestions.getOrNull(highlighted)
        if (selected != null) onAddCatalogItem(selected) else onAddFreeText()
    }

    ExposedDropdownMenuBox(
        expanded = showSuggestions,
        onExpandedChange = { },
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(Res.string.shopping_add_item.resolve()) },
            // Must precede menuAnchor: it installs its own onPreviewKeyEvent, and previews run
            // outer-to-inner, so anything after it never sees Enter or the arrow keys.
            modifier = Modifier
                .fillMaxWidth()
                .textFieldKeyboardControl(
                    onCommit = commit,
                    suggestionCount = if (showSuggestions) suggestions.size else 0,
                    highlighted = highlighted,
                    onHighlightedChange = { highlighted = it },
                )
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onAddFreeText) {
                        Icon(
                            painterResource(Res.drawable.ic_send),
                            contentDescription = Res.string.common_add.resolve(),
                        )
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
        SuggestionMenu(
            expanded = showSuggestions,
            suggestions = suggestions,
            highlighted = highlighted,
            onSelect = onAddCatalogItem,
        )
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
                    text = qty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
