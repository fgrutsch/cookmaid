package io.github.fgrutsch.cookmaid.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/**
 * Applies the app-wide Material3 theme with dynamic dark/light mode.
 *
 * @param isDark whether to use the dark color scheme.
 * @param hasDarkTopChrome whether the content draws a dark region behind the status bar — true
 *   for screens with a top app bar, false for the plain-white login and loading screens. The
 *   platform cannot infer this, and getting it wrong makes the status bar icons invisible.
 * @param content the composable content to theme.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    isDark: Boolean = false,
    hasDarkTopChrome: Boolean = true,
    content: @Composable () -> Unit,
) {
    // The app bar is the logo blue in both schemes, so any screen with one needs light status bar
    // icons whatever the theme. The login and loading screens have no bar and sit on the white
    // page, where light icons would vanish.
    SystemBarAppearance(lightIcons = isDark || hasDarkTopChrome)
    // Expressive rather than MaterialTheme: the springier motion scheme reaches every default
    // component, so checking off a shopping item — the app's most repeated interaction — gets
    // some character without touching the call site.
    MaterialExpressiveTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

/**
 * Colors shared by every top app bar. Use this rather than `TopAppBarDefaults.topAppBarColors()`
 * at the call site — a bar that sets its own colors will drift away from the rest of the chrome.
 *
 * The container is the logo's blue. In this direction the bar is not a tinted strip over the
 * content but one edge of a colour block that also includes the navigation bar, with the white
 * content field sitting between them. Being dark, it takes white ink — anything placed in the bar
 * has to take its colour from `onPrimaryContainer`, not from the surface roles.
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

/**
 * Colors for a card that holds list or detail content.
 *
 * Filled with `surfaceContainerLow`, which is navy-tinted: at these tones only the blue cast
 * separates a card from the page.
 *
 * @return the [CardColors] every content card should use.
 */
@Composable
fun appCardColors(): CardColors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
)
