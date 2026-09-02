package io.github.fgrutsch.cookmaid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * Applies the app-wide Material3 theme with dynamic dark/light mode.
 *
 * @param isDark whether to use the dark color scheme.
 * @param content the composable content to theme.
 */
@Composable
fun AppTheme(
    isDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Every screen's top region is a light surface in the light scheme and a dark one in the
    // dark scheme, so the status bar follows the theme and nothing per-screen is needed.
    SystemBarAppearance(lightIcons = isDark)
    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        content = content,
    )
}

/**
 * Colors shared by every top app bar. Use this rather than `TopAppBarDefaults.topAppBarColors()`
 * at the call site — a bar that sets its own colors will drift away from the rest of the chrome.
 *
 * The container is a neutral surface, not brand navy: a filled colored app bar is Material 2's
 * pattern, and Material 3 puts `surface` there. The brand shows up in the title and icons
 * instead, which is far less navy on screen for the same amount of identity.
 *
 * @return the [TopAppBarColors] every screen's top bar should use.
 */
@Composable
fun appTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    titleContentColor = MaterialTheme.colorScheme.primary,
    navigationIconContentColor = MaterialTheme.colorScheme.primary,
    actionIconContentColor = MaterialTheme.colorScheme.primary,
)
