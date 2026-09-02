package io.github.fgrutsch.cookmaid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cookmaid.app.shared.generated.resources.Res
import cookmaid.app.shared.generated.resources.outfit_bold
import cookmaid.app.shared.generated.resources.outfit_semibold
import org.jetbrains.compose.resources.Font

/**
 * The app type scale: Outfit on the display, headline and title roles, the platform default on
 * body and label.
 *
 * Only the headings get a bundled face. Outfit is geometric and echoes the logo wordmark, which
 * is what carries brand at a glance, while body text is where the platform font earns its
 * keep — it is hinted for the screen it is running on and costs no download. Two weights ship
 * rather than a variable font: Compose resources map a requested [FontWeight] onto a static
 * instance reliably on every target, whereas variable axis selection does not.
 *
 * Weight lives here rather than at the call sites. A screen asking for `titleMedium` gets the
 * right weight without restating it, and `FontWeight.Bold` still steps up to the 700 face where
 * a section heading wants more.
 *
 * Tracking goes negative on the large roles. Material's defaults are tuned for Roboto, and a
 * geometric face at display sizes looks loose and dated carrying them unchanged.
 *
 * @return the [Typography] for [AppTheme].
 */
@Composable
fun appTypography(): Typography {
    val heading = FontFamily(
        Font(Res.font.outfit_semibold, FontWeight.SemiBold),
        Font(Res.font.outfit_bold, FontWeight.Bold),
    )
    val base = Typography()

    return base.copy(
        displayLarge = base.displayLarge.copy(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
        ),
        displayMedium = base.displayMedium.copy(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        displaySmall = base.displaySmall.copy(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.25).sp,
        ),
        headlineLarge = base.headlineLarge.copy(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.25).sp,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = heading,
            fontWeight = FontWeight.Bold,
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = heading,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.25).sp,
        ),
        titleMedium = base.titleMedium.copy(
            fontFamily = heading,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
        titleSmall = base.titleSmall.copy(
            fontFamily = heading,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
    )
}
