package io.github.fgrutsch.cookmaid.ui.recipe.edit

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cookmaid.composeapp.generated.resources.Res
import cookmaid.composeapp.generated.resources.common_back
import cookmaid.composeapp.generated.resources.ic_arrow_back
import cookmaid.composeapp.generated.resources.ic_check
import cookmaid.composeapp.generated.resources.common_save
import cookmaid.composeapp.generated.resources.recipe_add_title
import cookmaid.composeapp.generated.resources.recipe_edit_title
import io.github.fgrutsch.cookmaid.ui.common.resolve
import org.jetbrains.compose.resources.painterResource

/**
 * Form screen for creating or editing a recipe.
 *
 * @param viewModel the add/edit recipe view model.
 * @param onBack called when navigating back after save or cancel.
 */
@Composable
@Suppress("LongMethod")
fun AddRecipeScreen(
    viewModel: AddRecipeViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val onEvent = viewModel::onEvent

    var stepInput by remember { mutableStateOf("") }
    var ingredientQuantityInput by remember { mutableStateOf("") }
    var showNewTagDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        onEvent(AddRecipeEvent.Load)
        viewModel.effects.collect { effect ->
            when (effect) {
                is AddRecipeEffect.Saved -> onBack()
                is AddRecipeEffect.Error -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) Res.string.recipe_edit_title.resolve()
                        else Res.string.recipe_add_title.resolve(),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(Res.drawable.ic_arrow_back),
                            contentDescription = Res.string.common_back.resolve(),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onEvent(AddRecipeEvent.Save) }) {
                        Icon(
                            painterResource(Res.drawable.ic_check),
                            contentDescription = Res.string.common_save.resolve(),
                        )
                    }
                },
            )
        },
    ) { padding ->
        AddRecipeContent(
            state = state,
            ingredientQuantityInput = ingredientQuantityInput,
            stepInput = stepInput,
            padding = padding,
            onEvent = onEvent,
            onIngredientQuantityChange = { ingredientQuantityInput = it },
            onStepInputChange = { stepInput = it },
            onShowNewTagDialog = { showNewTagDialog = true },
        )
    }

    if (showNewTagDialog) {
        NewTagDialog(
            onDismiss = { showNewTagDialog = false },
            onConfirm = { tag ->
                onEvent(AddRecipeEvent.CreateAndAddTag(tag))
                showNewTagDialog = false
            },
        )
    }
}
