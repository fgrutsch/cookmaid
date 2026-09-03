package io.github.fgrutsch.cookmaid.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The spinner a screen shows while it has nothing to display yet.
 *
 * Shared for the same reason as [EmptyState]: it is the other half of every list screen's
 * first-load state, and the three of them plus the auth gate had four copies of the same block.
 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
