package io.github.fgrutsch.cookmaid.ui.settings

import androidx.compose.ui.text.intl.Locale
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
                isDarkMode = settings.getBoolean(KEY_DARK_MODE, false),
                locale = settings.getStringOrNull(KEY_LOCALE)?.let { SupportedLocale.fromCode(it) },
            )
        }
    }

    override fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleDarkMode -> toggleDarkMode()
            is SettingsEvent.SetLocale -> setLocale(event.locale)
        }
    }

    fun effectiveLocale(): SupportedLocale =
        state.value.locale ?: SupportedLocale.fromCode(Locale.current.language)

    private fun toggleDarkMode() {
        val newValue = !state.value.isDarkMode
        settings.putBoolean(KEY_DARK_MODE, newValue)
        updateState { copy(isDarkMode = newValue) }
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
