package io.github.fgrutsch.cookmaid.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.auth_app_logo
import cookmaid.composeapp.generated.resources.auth_sign_in
import cookmaid.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import io.github.fgrutsch.cookmaid.ui.common.resolve

/**
 * Login screen that initiates the OIDC authentication flow.
 *
 * @param viewModel the authentication view model.
 */
@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()
    val loginError = state.loginError

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = Res.string.auth_app_logo.resolve(),
            modifier = Modifier.size(240.dp),
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { viewModel.onEvent(AuthEvent.Login) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Res.string.auth_sign_in.resolve())
        }

        if (loginError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = loginError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
