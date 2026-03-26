package io.github.fgrutsch.cookmaid.ui.settings

import io.github.fgrutsch.cookmaid.common.SupportedLocale

/**
 * @param isDarkMode Whether dark mode is enabled or not.
 * @param locale Whether a local has ben set, if null use auto detection.
 */
data class SettingsState(
    val isDarkMode: Boolean = false,
    val locale: SupportedLocale? = null,
)

sealed interface SettingsEvent {
    data object ToggleDarkMode : SettingsEvent
    data class SetLocale(val locale: SupportedLocale?) : SettingsEvent
}
