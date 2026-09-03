package io.github.fgrutsch.cookmaid.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The placeholder a list shows when it has nothing in it.
 *
 * Shared so every empty list reads the same way: what is missing, then what to do about it. The
 * screens had drifted to one line here and two there.
 *
 * @param title what is empty.
 * @param subtitle how to fill it.
 */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
