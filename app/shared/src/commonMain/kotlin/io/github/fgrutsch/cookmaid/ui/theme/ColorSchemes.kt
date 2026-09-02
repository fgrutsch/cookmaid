package io.github.fgrutsch.cookmaid.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Hand-authored color schemes built from two of the logo's colors: navy `#2D3E50` (hexagon
 * frame, wordmark) and orange `#E2732E` (hexagon frame, "MAID").
 *
 * These replace seed-generated palettes because Material's generator reads only the hue off a
 * seed and caps chroma itself, so no literal brand hex can reach the UI — and a single seed
 * cannot express two brand colors at once.
 *
 * Two colors, two jobs, so the user can learn the rule without being taught it:
 * - navy — structure. Top app bars, buttons, accent icons, the bottom-nav indicator. Furniture.
 * - orange — action, via `tertiary`. At most one orange element per screen (the recipe list FAB,
 *   the sign-in button). A second one costs orange its meaning and makes it decoration.
 *
 * The logo's sage deliberately has no role here. A color that surfaces in one or two places
 * reads as an inconsistency rather than a system; give sage a job first (a real state worth
 * naming), then a role.
 *
 * Every role is set explicitly. Omitting one silently falls back to Material's purple baseline,
 * which shows up as an out-of-place violet somewhere far from this file. Every on/container pair
 * below clears WCAG AA (4.5:1); the lowest is orange `onTertiary` at 5.7:1.
 */

// Brand navy — #2D3E50 is Navy26 itself
private val Navy20 = Color(0xFF1B2A38)
private val Navy26 = Color(0xFF2D3E50)
private val Navy40 = Color(0xFF4A5F76)
private val Navy80 = Color(0xFFB4C6DC)
private val Navy90 = Color(0xFFD6E4F5)

// Logo orange — #E2732E is Orange60 itself
private val Orange10 = Color(0xFF2A1000)
private val Orange20 = Color(0xFF4E1B00)
private val Orange30 = Color(0xFF7A3200)
private val Orange60 = Color(0xFFE2732E)
private val Orange80 = Color(0xFFFFA766)
private val Orange90 = Color(0xFFFFDBC8)

// Neutral surfaces — barely-tinted cool grey, kept low so surfaces read as paper
private val Neutral4 = Color(0xFF0D0E10)
private val Neutral6 = Color(0xFF121315)
private val Neutral10 = Color(0xFF1A1C1D)
private val Neutral12 = Color(0xFF1F2021)
private val Neutral17 = Color(0xFF292A2C)
private val Neutral20 = Color(0xFF2F3032)
private val Neutral22 = Color(0xFF343537)
private val Neutral24 = Color(0xFF38393B)
private val Neutral87 = Color(0xFFDBD9DC)
private val Neutral90 = Color(0xFFE3E2E4)
private val Neutral92 = Color(0xFFE9E8EA)
private val Neutral94 = Color(0xFFEFEDF0)
private val Neutral95 = Color(0xFFF2F0F2)
private val Neutral96 = Color(0xFFF4F3F5)
private val Neutral98 = Color(0xFFFAF9FB)
private val Neutral100 = Color(0xFFFFFFFF)

// Neutral variant — same cool hue, a touch more chroma for outlines
private val NeutralVar30 = Color(0xFF44474C)
private val NeutralVar50 = Color(0xFF74777C)
private val NeutralVar60 = Color(0xFF8E9196)
private val NeutralVar80 = Color(0xFFC4C6CC)

/**
 * Light scheme. `secondaryContainer` is [Navy80] rather than the lighter [Navy90] Material's
 * ramp would suggest: as the bottom-nav indicator it sits on `surfaceContainer`, and [Navy90]
 * only reaches 1.1:1 against it, so the selected pill disappears.
 *
 * [Navy26] fills both `primary` and `primaryContainer` on purpose: buttons and
 * app bars are meant to read as the same brand navy, so they share one value rather than
 * drifting apart as two near-identical tones.
 *
 * `tertiary` is the saturated logo orange with a dark `onTertiary`, rather than the dark accent
 * tone Material would normally put there. It exists to be filled (the FAB), not to be read as
 * text on a light surface — orange on paper only reaches 3:1, so keep it off small text.
 */
internal val LightColors = lightColorScheme(
    primary = Navy26,
    onPrimary = Neutral100,
    primaryContainer = Navy26,
    onPrimaryContainer = Neutral100,
    inversePrimary = Navy80,
    secondary = Navy40,
    onSecondary = Neutral100,
    secondaryContainer = Navy80,
    onSecondaryContainer = Navy20,
    tertiary = Orange60,
    onTertiary = Orange10,
    tertiaryContainer = Orange90,
    onTertiaryContainer = Orange20,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = Neutral90,
    onSurfaceVariant = NeutralVar30,
    surfaceTint = Navy26,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    error = Color(0xFFBA1A1A),
    onError = Neutral100,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = NeutralVar50,
    outlineVariant = NeutralVar80,
    scrim = Color(0xFF000000),
    surfaceBright = Neutral98,
    surfaceDim = Neutral87,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
)

/**
 * Dark scheme. `primaryContainer` stays a dark navy here rather than mirroring the light
 * scheme's `primary`, so app bars remain dark chrome instead of turning into a bright band.
 * Orange lightens to its tone-80 variant so it stays visible against dark surfaces.
 */
internal val DarkColors = darkColorScheme(
    primary = Navy80,
    onPrimary = Navy20,
    primaryContainer = Navy20,
    onPrimaryContainer = Navy90,
    inversePrimary = Navy40,
    secondary = Navy80,
    onSecondary = Navy20,
    secondaryContainer = Navy40,
    onSecondaryContainer = Navy90,
    tertiary = Orange80,
    onTertiary = Orange20,
    tertiaryContainer = Orange30,
    onTertiaryContainer = Orange90,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = NeutralVar30,
    onSurfaceVariant = NeutralVar80,
    surfaceTint = Navy80,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = NeutralVar60,
    outlineVariant = NeutralVar30,
    scrim = Color(0xFF000000),
    surfaceBright = Neutral24,
    surfaceDim = Neutral6,
    surfaceContainerLowest = Neutral4,
    surfaceContainerLow = Neutral10,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
)
