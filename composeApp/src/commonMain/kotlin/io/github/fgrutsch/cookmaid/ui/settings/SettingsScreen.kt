package io.github.fgrutsch.cookmaid.ui.settings

import io.github.fgrutsch.cookmaid.common.SupportedLocale
import cookmaid.composeapp.generated.resources.settings_language_de
import cookmaid.composeapp.generated.resources.settings_language_en
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import cookmaid.composeapp.generated.resources.settings_dark_mode_dark
import cookmaid.composeapp.generated.resources.settings_dark_mode_light
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.ic_person
import cookmaid.composeapp.generated.resources.settings_dark_mode
import cookmaid.composeapp.generated.resources.settings_language
import cookmaid.composeapp.generated.resources.settings_manage_account
import cookmaid.composeapp.generated.resources.settings_profile_picture
import cookmaid.composeapp.generated.resources.settings_sign_out
import cookmaid.composeapp.generated.resources.settings_title
import io.github.fgrutsch.cookmaid.BuildKonfig
import io.github.fgrutsch.cookmaid.ui.auth.UserProfile
import io.github.fgrutsch.cookmaid.ui.common.resolve
import org.jetbrains.compose.resources.painterResource

/**
 * Settings screen displaying user profile info, theme toggle, and logout.
 *
 * @param viewModel the settings view model.
 * @param userProfile the authenticated user's profile.
 * @param accountUrl the IDP account management URL to open in the browser.
 * @param onLogout called when the user logs out.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    userProfile: UserProfile,
    accountUrl: String,
    onLogout: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Res.string.settings_title.resolve()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        SettingsContent(
            userProfile = userProfile,
            accountUrl = accountUrl,
            darkMode = state.darkMode,
            onDarkModeSelected = { viewModel.onEvent(SettingsEvent.SetDarkMode(it)) },
            locale = state.locale,
            onLocaleSelected = { viewModel.onEvent(SettingsEvent.SetLocale(it)) },
            onLogout = onLogout,
            appVersion = BuildKonfig.APP_VERSION,
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        )
    }
}

@Composable
private fun SettingsContent(
    userProfile: UserProfile,
    accountUrl: String,
    darkMode: Boolean?,
    onDarkModeSelected: (Boolean?) -> Unit,
    locale: SupportedLocale?,
    onLocaleSelected: (SupportedLocale?) -> Unit,
    onLogout: () -> Unit,
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        UserProfileSection(userProfile)

        TextButton(
            onClick = { uriHandler.openUri(accountUrl) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(Res.string.settings_manage_account.resolve())
        }

        HorizontalDivider()

        DarkModePicker(
            selectedMode = darkMode,
            onSelected = onDarkModeSelected,
        )

        LanguagePicker(
            selectedLocale = locale,
            onSelected = onLocaleSelected,
        )

        HorizontalDivider()

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Res.string.settings_sign_out.resolve())
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "v$appVersion",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DarkModePicker(
    selectedMode: Boolean?,
    onSelected: (Boolean?) -> Unit,
) {
    val options: List<Boolean?> = listOf(null, false, true)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(Res.string.settings_dark_mode.resolve())
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = selectedMode == mode,
                    onClick = { onSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index,
                        options.size,
                    ),
                ) {
                    Text(
                        when (mode) {
                            null -> "Auto"
                            false -> Res.string.settings_dark_mode_light.resolve()
                            true -> Res.string.settings_dark_mode_dark.resolve()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguagePicker(
    selectedLocale: SupportedLocale?,
    onSelected: (SupportedLocale?) -> Unit,
) {
    val options: List<SupportedLocale?> = listOf(null) + SupportedLocale.entries
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(Res.string.settings_language.resolve())
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, locale ->
                SegmentedButton(
                    selected = selectedLocale == locale,
                    onClick = { onSelected(locale) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index,
                        options.size,
                    ),
                ) {
                    Text(
                        when (locale) {
                            null -> "Auto"
                            SupportedLocale.EN -> Res.string.settings_language_en.resolve()
                            SupportedLocale.DE -> Res.string.settings_language_de.resolve()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserProfileSection(userProfile: UserProfile) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!userProfile.pictureUrl.isNullOrBlank()) {
            AsyncImage(
                model = userProfile.pictureUrl,
                contentDescription = Res.string.settings_profile_picture.resolve(),
                modifier = Modifier.size(80.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.ic_person),
                contentDescription = Res.string.settings_profile_picture.resolve(),
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!userProfile.name.isNullOrBlank()) {
            Text(
                text = userProfile.name,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (!userProfile.email.isNullOrBlank()) {
            Text(
                text = userProfile.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
