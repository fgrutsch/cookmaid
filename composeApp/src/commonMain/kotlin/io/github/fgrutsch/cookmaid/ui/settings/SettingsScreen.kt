package io.github.fgrutsch.cookmaid.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.fgrutsch.cookmaid.ui.auth.UserProfile

/**
 * Settings screen displaying user profile info, theme toggle, and logout.
 *
 * @param viewModel the settings view model.
 * @param userProfile the authenticated user's profile.
 * @param onLogout called when the user logs out.
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, userProfile: UserProfile, onLogout: () -> Unit) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        SettingsContent(
            userProfile = userProfile,
            isDarkMode = isDarkMode,
            onToggleDarkMode = { viewModel.toggleDarkMode() },
            onLogout = onLogout,
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
        )
    }
}

@Composable
private fun SettingsContent(
    userProfile: UserProfile,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        UserProfileSection(userProfile)

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Dark Mode")
            Switch(checked = isDarkMode, onCheckedChange = { onToggleDarkMode() })
        }

        HorizontalDivider()

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign out")
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
                contentDescription = "Profile picture",
                modifier = Modifier.size(80.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile picture",
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
