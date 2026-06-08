package io.github.fgrutsch.cookmaid.ui.deleteaccount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cookmaid.app.shared.generated.resources.Res
import cookmaid.app.shared.generated.resources.common_back
import cookmaid.app.shared.generated.resources.common_cancel
import cookmaid.app.shared.generated.resources.delete_account_button
import cookmaid.app.shared.generated.resources.delete_account_confirm
import cookmaid.app.shared.generated.resources.delete_account_confirm_message
import cookmaid.app.shared.generated.resources.delete_account_confirm_title
import cookmaid.app.shared.generated.resources.delete_account_deleted_message
import cookmaid.app.shared.generated.resources.delete_account_deleted_title
import cookmaid.app.shared.generated.resources.delete_account_error
import cookmaid.app.shared.generated.resources.delete_account_finish
import cookmaid.app.shared.generated.resources.delete_account_title
import cookmaid.app.shared.generated.resources.delete_account_warning
import cookmaid.app.shared.generated.resources.ic_arrow_back
import io.github.fgrutsch.cookmaid.ui.common.resolve
import org.jetbrains.compose.resources.painterResource

/**
 * Screen that lets the authenticated user permanently delete their account.
 * Shows a warning + confirm dialog; on success shows a confirmation and a
 * Finish action that triggers logout via [onFinish].
 *
 * @param viewModel the account-deletion view model.
 * @param onBack called when the user navigates back without deleting.
 * @param onFinish called after deletion completes (the caller logs the user out).
 */
@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var showConfirm by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(Res.string.delete_account_title.resolve()) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                navigationIcon = {
                    if (!state.deleted) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painterResource(Res.drawable.ic_arrow_back),
                                contentDescription = Res.string.common_back.resolve(),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.deleted) {
            DeletedContent(
                onFinish = onFinish,
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            )
        } else {
            DeleteContent(
                deleting = state.deleting,
                error = state.error,
                onDeleteClick = { showConfirm = true },
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            )
        }
    }

    if (showConfirm) {
        ConfirmDialog(
            onDismiss = { showConfirm = false },
            onConfirm = {
                showConfirm = false
                viewModel.onEvent(DeleteAccountEvent.Confirm)
            },
        )
    }
}

@Composable
private fun ConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Res.string.delete_account_confirm_title.resolve()) },
        text = { Text(Res.string.delete_account_confirm_message.resolve()) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(Res.string.delete_account_confirm.resolve())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Res.string.common_cancel.resolve())
            }
        },
    )
}

@Composable
private fun DeleteContent(
    deleting: Boolean,
    error: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = Res.string.delete_account_warning.resolve(),
            style = MaterialTheme.typography.bodyLarge,
        )

        if (error) {
            Text(
                text = Res.string.delete_account_error.resolve(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick = onDeleteClick,
            enabled = !deleting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            if (deleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onError,
                )
            } else {
                Text(Res.string.delete_account_button.resolve())
            }
        }
    }
}

@Composable
private fun DeletedContent(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = Res.string.delete_account_deleted_title.resolve(),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = Res.string.delete_account_deleted_message.resolve(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(Res.string.delete_account_finish.resolve())
        }
    }
}
