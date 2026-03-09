package io.github.fgrutsch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle

private val SeedColor = Color(0xFFBD4C00)

@Composable
fun AppTheme(
    isDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    DynamicMaterialTheme(
        seedColor = SeedColor,
        isDark = isDark,
        style = PaletteStyle.TonalSpot,
        animate = true,
        content = content,
    )
}
