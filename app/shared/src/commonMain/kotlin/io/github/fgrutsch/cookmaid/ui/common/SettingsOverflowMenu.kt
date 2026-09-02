package io.github.fgrutsch.cookmaid.ui.common

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cookmaid.app.shared.generated.resources.Res
import cookmaid.app.shared.generated.resources.common_options
import cookmaid.app.shared.generated.resources.ic_more_vert
import cookmaid.app.shared.generated.resources.nav_settings
import org.jetbrains.compose.resources.painterResource

/**
 * Overflow menu holding the settings entry, for top-level screens whose app bar has no menu of
 * its own. Screens that already own an overflow menu add the settings item to theirs instead.
 *
 * Settings is not an action belonging to any one screen, and Material reserves the app bar's
 * visible slots for actions that are. Overflow is where global and secondary items belong, so
 * the entry point stays reachable everywhere without a gear competing for attention on
 * every screen.
 *
 * @param onSettingsClick called when the settings item is chosen.
 */
@Composable
fun SettingsOverflowMenu(onSettingsClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(painterResource(Res.drawable.ic_more_vert), contentDescription = Res.string.common_options.resolve())
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(Res.string.nav_settings.resolve()) },
            onClick = {
                expanded = false
                onSettingsClick()
            },
        )
    }
}
