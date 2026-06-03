package io.github.fgrutsch.cookmaid.ui.settings

import io.github.fgrutsch.cookmaid.common.SupportedLocale

/**
 * @param darkMode Dark mode override, or null to follow system preference.
 * @param locale Locale override, or null to follow device language.
 */
data class SettingsState(
    val darkMode: Boolean? = null,
    val locale: SupportedLocale? = null,
) {
    /**
     * Returns the effective dark mode value, falling back to the system theme.
     *
     * @param systemDarkMode the system's current dark mode setting.
     */
    fun effectiveDarkMode(systemDarkMode: Boolean): Boolean =
        darkMode ?: systemDarkMode

    /**
     * Returns the effective locale, falling back to the device language.
     */
    fun effectiveLocale(): SupportedLocale =
        locale ?: SupportedLocale.fromCode(
            androidx.compose.ui.text.intl.Locale.current.language,
        )
}

sealed interface SettingsEvent {
    data class SetDarkMode(val enabled: Boolean?) : SettingsEvent
    data class SetLocale(val locale: SupportedLocale?) : SettingsEvent
}
