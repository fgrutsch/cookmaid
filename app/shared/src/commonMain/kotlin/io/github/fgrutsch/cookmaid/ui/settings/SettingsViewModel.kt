package io.github.fgrutsch.cookmaid.ui.settings

import com.russhwolf.settings.Settings
import io.github.fgrutsch.cookmaid.common.SupportedLocale
import io.github.fgrutsch.cookmaid.ui.common.MviViewModel

/**
 * ViewModel for app-wide settings (dark mode, language).
 * Persists preferences via [Settings] (SharedPreferences on Android,
 * localStorage on WasmJS).
 */
class SettingsViewModel : MviViewModel<SettingsState, SettingsEvent, Nothing>(SettingsState()) {

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LOCALE = "locale"
    }

    private val settings: Settings = Settings()

    init {
        updateState {
            copy(
                darkMode = settings.getStringOrNull(KEY_DARK_MODE)?.toBooleanStrictOrNull(),
                locale = settings.getStringOrNull(KEY_LOCALE)?.let { SupportedLocale.fromCode(it) },
            )
        }
    }

    override fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetDarkMode -> setDarkMode(event.enabled)
            is SettingsEvent.SetLocale -> setLocale(event.locale)
        }
    }

    private fun setDarkMode(enabled: Boolean?) {
        if (enabled != null) {
            settings.putString(KEY_DARK_MODE, enabled.toString())
        } else {
            settings.remove(KEY_DARK_MODE)
        }
        updateState { copy(darkMode = enabled) }
    }

    private fun setLocale(locale: SupportedLocale?) {
        if (locale != null) {
            settings.putString(KEY_LOCALE, locale.code)
        } else {
            settings.remove(KEY_LOCALE)
        }
        updateState { copy(locale = locale) }
    }
}
