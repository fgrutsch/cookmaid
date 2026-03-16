package io.github.fgrutsch.cookmaid.ui.auth

import org.publicvalue.multiplatform.oidc.OpenIdConnectClient

data class OidcConfig(
    val discoveryUri: String,
    val clientId: String,
    val scope: String,
    val redirectUri: String,
    val postLogoutRedirectUri: String,
)

fun createOidcClient(config: OidcConfig): OpenIdConnectClient =
    OpenIdConnectClient(discoveryUri = config.discoveryUri) {
        clientId = config.clientId
        scope = config.scope
        redirectUri = config.redirectUri
        postLogoutRedirectUri = config.postLogoutRedirectUri
    }
