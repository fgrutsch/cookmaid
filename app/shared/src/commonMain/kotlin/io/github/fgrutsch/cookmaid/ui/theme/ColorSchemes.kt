package io.github.fgrutsch.cookmaid.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Hand-authored colour schemes — the expressive direction.
 *
 * Colour comes from blocking, not from tinting. The content field is plain white and the chrome
 * is the logo's blue at full strength, top and bottom, so it does the job a brand colour is for
 * instead of registering as a 1.06:1 wash.
 *
 * Two brand values, sampled from the full-resolution artwork: blue `#3A5A67` from the hexagon
 * frame and wordmark, and orange `#E2732E` from the frame's lower half. The blue supplies
 * `primary` and — desaturated, as Material intends — `secondary`. Orange is `tertiary`, the one
 * role Material reserves for a contrasting accent, and it stays the only warm colour in the app
 * so that orange means exactly one thing: this is the action.
 *
 * `secondaryContainer` is the same pale blue in both schemes on purpose — it is the navigation
 * indicator, and it sits on the blue chrome rather than on the page, so it does not need to
 * follow the page between light and dark.
 *
 * Every role is set explicitly. Omitting one silently falls back to Material's purple baseline,
 * which shows up as an out-of-place violet somewhere far from this file.
 */

// The logo's blue — chrome, accent text and buttons.
//
// Measured off the hexagon border rather than eyedropped. The artwork is rendered, not a flat
// fill, so the border spans 1269 distinct values; sampling a single pixel returns whichever
// noise it lands on. Restricted to the border above the character, #3A5A67 is the mode at 26.4%
// and the mean is #3B5A67 — one unit apart, so the cluster is symmetric rather than a gradient.
//
// Note this is not #2D3E50, the hex declared in the web config, manifest and favicon. That value
// appears in zero pixels and the nearest present colour is some 30 units away in RGB, well
// outside the distribution. It is also markedly darker: 11:1 against white where this is 7.4:1,
// which is why the chrome read as heavy when it was used.
private val Frame36 = Color(0xFF3A5A67)
private val FrameDark = Color(0xFF283F48)
private val Navy14 = Color(0xFF14212E)
private val Navy45 = Color(0xFF5C6C7A)
private val Navy55 = Color(0xFF6B7A88)
private val Navy70 = Color(0xFFA9B8C6)
private val Navy75 = Color(0xFFA8C0D8)
private val Navy85 = Color(0xFFC7D2DD)

// Logo orange — the action colour.
private val Orange10 = Color(0xFF2A1000)
private val Orange20 = Color(0xFF4E1B00)
private val Orange30 = Color(0xFF7A3200)
private val Orange60 = Color(0xFFE2732E)
private val Orange75 = Color(0xFFFF9A55)
private val Orange90 = Color(0xFFFFDBC8)

// Secondary — the logo blue's hue held, chroma dropped. Material's `secondary` is a
// desaturated relative of `primary`, not an independent colour, and `secondaryContainer` is the
// navigation indicator. Gold used to sit there: it read as a second accent competing with the
// orange, only 14 degrees away from it in hue at the same saturation. One warm accent is enough,
// and Material reserves that job for `tertiary`.
private val Blue35 = Color(0xFF456070)
private val Blue85 = Color(0xFFCFDCE2)

// Content field — pure white in light, so a tinted well reads without darkening the page.
private val Ground = Color(0xFFFFFFFF)
private val Well = Color(0xFFEEF2F7)
private val WellDeep = Color(0xFFE4EBF3)
private val WellDeeper = Color(0xFFD9E3EC)
private val WellVariant = Color(0xFFDCE5EF)
private val GroundDark = Color(0xFF0E141A)
private val GroundDarkest = Color(0xFF080C10)
private val WellDark = Color(0xFF1A2530)
private val WellDarkDeep = Color(0xFF243140)
private val WellDarkDeeper = Color(0xFF2E3C4C)
private val InkDark = Color(0xFFDCE5EE)
private val InkDarkMuted = Color(0xFF9BAAB8)

/**
 * Light scheme. `primary` and `primaryContainer` are the same value: the app bar and the
 * navigation bar are one block, a button is that block at small scale, and the same blue reads
 * as accent text on white at 7.4:1.
 */
internal val LightColors = lightColorScheme(
    primary = Frame36,
    onPrimary = Ground,
    primaryContainer = Frame36,
    onPrimaryContainer = Ground,
    inversePrimary = Navy75,
    secondary = Blue35,
    onSecondary = Ground,
    secondaryContainer = Blue85,
    onSecondaryContainer = Navy14,
    tertiary = Orange60,
    onTertiary = Orange10,
    tertiaryContainer = Orange90,
    onTertiaryContainer = Orange20,
    background = Ground,
    onBackground = Navy14,
    surface = Ground,
    onSurface = Navy14,
    surfaceVariant = WellVariant,
    onSurfaceVariant = Navy45,
    surfaceTint = Frame36,
    inverseSurface = FrameDark,
    inverseOnSurface = Well,
    error = Color(0xFFBA1A1A),
    onError = Ground,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Navy55,
    outlineVariant = Navy85,
    scrim = Color(0xFF000000),
    surfaceBright = Ground,
    surfaceDim = WellDeeper,
    surfaceContainerLowest = Ground,
    surfaceContainerLow = Well,
    surfaceContainer = WellDeep,
    surfaceContainerHigh = WellDeeper,
    surfaceContainerHighest = WellDeeper,
)

/**
 * Dark scheme. The chrome darkens to [FrameDark] — the logo blue's hue and saturation held, its
 * lightness taken down far enough to sit on a near-black field without glowing off it.
 */
internal val DarkColors = darkColorScheme(
    primary = Navy75,
    onPrimary = Navy14,
    primaryContainer = FrameDark,
    onPrimaryContainer = Ground,
    inversePrimary = Frame36,
    secondary = Blue85,
    onSecondary = Navy14,
    secondaryContainer = Blue85,
    onSecondaryContainer = Navy14,
    tertiary = Orange75,
    onTertiary = Orange20,
    tertiaryContainer = Orange30,
    onTertiaryContainer = Orange90,
    background = GroundDark,
    onBackground = InkDark,
    surface = GroundDark,
    onSurface = InkDark,
    surfaceVariant = WellDarkDeeper,
    onSurfaceVariant = InkDarkMuted,
    surfaceTint = Navy75,
    inverseSurface = InkDark,
    inverseOnSurface = FrameDark,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Navy70,
    outlineVariant = WellDarkDeeper,
    scrim = Color(0xFF000000),
    surfaceBright = WellDarkDeeper,
    surfaceDim = GroundDark,
    surfaceContainerLowest = GroundDarkest,
    surfaceContainerLow = WellDark,
    surfaceContainer = WellDarkDeep,
    surfaceContainerHigh = WellDarkDeeper,
    surfaceContainerHighest = WellDarkDeeper,
)
