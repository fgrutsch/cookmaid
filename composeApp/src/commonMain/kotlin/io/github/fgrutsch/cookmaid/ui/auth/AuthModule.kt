package io.github.fgrutsch.cookmaid.ui.auth

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient

val authModule = module {
    single<OpenIdConnectClient> { createOidcClient(get()) }
    singleOf(::ApiClient)
    singleOf(::OidcAuthHandler) bind AuthHandler::class
    singleOf(::AuthViewModel)
}
