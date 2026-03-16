package io.github.fgrutsch.cookmaid.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {
    val isDarkMode: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun toggleDarkMode() {
        isDarkMode.value = !isDarkMode.value
    }
}
