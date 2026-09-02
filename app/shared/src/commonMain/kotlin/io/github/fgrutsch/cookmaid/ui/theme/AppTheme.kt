package io.github.fgrutsch.cookmaid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * Applies the app-wide Material3 theme with dynamic dark/light mode.
 *
 * @param isDark whether to use the dark color scheme.
 * @param hasDarkTopChrome whether the content draws a dark region behind the status bar — true
 *   for screens with a brand-navy top app bar, false for the plain-surface login and loading
 *   screens. Drives the status bar icon color, which the platform cannot infer on its own.
 * @param content the composable content to theme.
 */
@Composable
fun AppTheme(
    isDark: Boolean = false,
    hasDarkTopChrome: Boolean = true,
    content: @Composable () -> Unit,
) {
    SystemBarAppearance(lightIcons = isDark || hasDarkTopChrome)
    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        content = content,
    )
}

/**
 * Colors shared by every top app bar, so app bars stay brand navy instead of blending into
 * the surface. Use this rather than `TopAppBarDefaults.topAppBarColors()` at the call site —
 * a bar that sets its own container color will drift away from the rest of the chrome.
 *
 * @return the [TopAppBarColors] every screen's top bar should use.
 */
@Composable
fun appTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
)
