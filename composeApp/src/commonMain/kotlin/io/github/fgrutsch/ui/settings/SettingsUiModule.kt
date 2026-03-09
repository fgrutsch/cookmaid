package io.github.fgrutsch.ui.settings

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val settingsUiModule = module {
    singleOf(::SettingsViewModel)
}
