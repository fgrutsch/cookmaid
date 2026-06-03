package io.github.fgrutsch.cookmaid.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

private val SeedColor = Color(0xFF2D3E50)

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
    SystemBarAppearance(isDark = isDark)
    DynamicMaterialTheme(
        seedColor = SeedColor,
        isDark = isDark,
        style = PaletteStyle.Fidelity,
        animate = true,
        content = content,
    )
}
