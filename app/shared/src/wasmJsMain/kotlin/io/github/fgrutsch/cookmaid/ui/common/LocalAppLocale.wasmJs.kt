package io.github.fgrutsch.cookmaid.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale

@Suppress("ClassNaming")
external object window {
    @Suppress("ObjectPropertyNaming")
    var __customLocale: String?
}

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object LocalAppLocale {
    private val LocalAppLocale = staticCompositionLocalOf { Locale.current }

    actual val current: String
        @Composable get() = LocalAppLocale.current.toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        window.__customLocale = value?.replace('_', '-')
        return LocalAppLocale.provides(Locale.current)
    }
}
