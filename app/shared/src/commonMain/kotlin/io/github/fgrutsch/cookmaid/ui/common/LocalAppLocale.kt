package io.github.fgrutsch.cookmaid.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * Composition local for overriding the app locale at runtime.
 * Platform-specific implementations handle updating the system locale
 * so that `stringResource()` picks up the correct translations.
 *
 * https://kotlinlang.org/docs/multiplatform/compose-resource-environment.html#locale
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}
