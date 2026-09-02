package io.github.fgrutsch.cookmaid.ui.theme

import androidx.compose.runtime.Composable

/**
 * Matches the platform status bar icons to whatever the app draws behind them.
 *
 * @param lightIcons true when the region behind the status bar is dark.
 */
@Composable
expect fun SystemBarAppearance(lightIcons: Boolean)
