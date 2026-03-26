package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.ui.settings.SettingsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient

val authModule = module {
    single<OpenIdConnectClient> { createOidcClient(get()) }
    single { ApiClient(get(), get(), get()) { get<SettingsViewModel>().effectiveLocale() } }
    singleOf(::OidcAuthHandler) bind AuthHandler::class
    singleOf(::AuthViewModel)
}
