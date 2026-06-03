package io.github.fgrutsch.cookmaid.ui.common

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Resolves this [StringResource] to a localized string.
 */
@Composable
fun StringResource.resolve(): String = stringResource(this)

/**
 * Resolves this [StringResource] to a localized string with format arguments.
 *
 * @param args the format arguments.
 */
@Composable
fun StringResource.resolve(vararg args: Any): String = stringResource(this, *args)
