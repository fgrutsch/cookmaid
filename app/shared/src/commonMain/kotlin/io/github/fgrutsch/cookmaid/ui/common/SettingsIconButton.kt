package io.github.fgrutsch.cookmaid.ui.common

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import cookmaid.app.shared.generated.resources.Res
import cookmaid.app.shared.generated.resources.ic_settings
import cookmaid.app.shared.generated.resources.nav_settings
import org.jetbrains.compose.resources.painterResource

/**
 * The settings entry point, for the `navigationIcon` slot of a top-level screen's app bar.
 *
 * Settings used to be the fourth bottom-nav tab, spending a quarter of the navigation bar on
 * something opened rarely. It lives here instead: top-level screens have no back button, so
 * their `navigationIcon` slot is free, and the three remaining tabs are all high-frequency.
 *
 * @param onClick called when the gear is tapped.
 */
@Composable
fun SettingsIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(Res.drawable.ic_settings),
            contentDescription = Res.string.nav_settings.resolve(),
        )
    }
}
