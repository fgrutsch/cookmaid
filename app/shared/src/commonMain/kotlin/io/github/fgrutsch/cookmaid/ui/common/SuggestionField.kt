package io.github.fgrutsch.cookmaid.ui.common

import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import io.github.fgrutsch.cookmaid.catalog.Item

/** No suggestion is highlighted; Enter commits whatever was typed. */
internal const val NO_SUGGESTION = -1

/**
 * Gives a text field hardware-keyboard control on desktop and web: arrow keys move the
 * highlight through [suggestionCount] suggestions, Escape clears it, and Enter commits.
 *
 * Handled keys are consumed, which keeps focus in the text field. Without that, arrow keys
 * move focus into the suggestion popup, where typing no longer reaches the field.
 *
 * @param onCommit called on Enter; the caller decides whether that means the highlighted
 *   suggestion or the typed text.
 * @param suggestionCount number of suggestions currently offered; 0 disables navigation.
 * @param highlighted index of the highlighted suggestion, or [NO_SUGGESTION].
 * @param onHighlightedChange called with the new highlight index.
 */
internal fun Modifier.textFieldKeyboardControl(
    onCommit: () -> Unit,
    suggestionCount: Int = 0,
    highlighted: Int = NO_SUGGESTION,
    onHighlightedChange: (Int) -> Unit = {},
): Modifier = onPreviewKeyEvent { event ->
    val navigable = suggestionCount > 0
    when {
        event.type != KeyEventType.KeyDown -> false
        navigable && event.key == Key.DirectionDown -> {
            onHighlightedChange((highlighted + 1).coerceAtMost(suggestionCount - 1))
            true
        }
        navigable && event.key == Key.DirectionUp -> {
            onHighlightedChange((highlighted - 1).coerceAtLeast(NO_SUGGESTION))
            true
        }
        navigable && event.key == Key.Escape -> {
            onHighlightedChange(NO_SUGGESTION)
            true
        }
        event.key == Key.Enter || event.key == Key.NumPadEnter -> {
            onCommit()
            true
        }
        else -> false
    }
}

/**
 * Autocomplete popup for a catalog-backed text field.
 *
 * @param expanded whether the popup is shown.
 * @param suggestions the catalog items to offer.
 * @param highlighted index of the keyboard-selected entry, or [NO_SUGGESTION].
 * @param onSelect called with the item the user picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExposedDropdownMenuBoxScope.SuggestionMenu(
    expanded: Boolean,
    suggestions: List<Item.Catalog>,
    highlighted: Int,
    onSelect: (Item.Catalog) -> Unit,
) {
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { }) {
        suggestions.forEachIndexed { index, catalogItem ->
            DropdownMenuItem(
                text = { Text(catalogItem.name) },
                onClick = { onSelect(catalogItem) },
                modifier = if (index == highlighted) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                } else {
                    Modifier
                },
            )
        }
    }
}
