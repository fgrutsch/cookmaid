package io.github.fgrutsch.cookmaid

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.fgrutsch.cookmaid.ui.auth.LocalStorageSettingsStore
import io.github.fgrutsch.cookmaid.ui.auth.OidcConfig
import io.github.fgrutsch.cookmaid.navigation.Deeplink
import kotlinx.browser.window
import org.publicvalue.multiplatform.oidc.appsupport.PlatformCodeAuthFlow
import org.publicvalue.multiplatform.oidc.appsupport.WebCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsTokenStore
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

@OptIn(ExperimentalWasmJsInterop::class)
private val oidcDiscoveryUri: String = js("window.__CONFIG__.OIDC_DISCOVERY_URI")

@OptIn(ExperimentalWasmJsInterop::class)
private val oidcClientId: String = js("window.__CONFIG__.OIDC_CLIENT_ID")

@OptIn(ExperimentalWasmJsInterop::class)
private val oidcScope: String = js("window.__CONFIG__.OIDC_SCOPE")

@OptIn(ExperimentalWasmJsInterop::class)
private val oidcAccountUri: String = js("window.__CONFIG__.OIDC_ACCOUNT_URI")

@OptIn(ExperimentalWasmJsInterop::class)
private val oidcResource: String = js("window.__CONFIG__.OIDC_RESOURCE")

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    if (window.location.pathname.startsWith("/callback")) {
        PlatformCodeAuthFlow.handleRedirect()
    } else {
        val startDeeplink = Deeplink.DELETE_ACCOUNT
            .takeIf { window.location.pathname == "/$it" }
        val origin = window.location.origin
        ComposeViewport {
            App(
                apiBaseUrl = ApiBaseUrl(origin),
                startDeeplink = startDeeplink,
                oidcConfig = OidcConfig(
                    discoveryUri = oidcDiscoveryUri,
                    clientId = oidcClientId,
                    scope = oidcScope,
                    redirectUri = "$origin/callback",
                    postLogoutRedirectUri = "$origin/callback",
                    accountUri = oidcAccountUri,
                    resource = oidcResource.ifBlank { null },
                ),
                codeAuthFlowFactory = WebCodeAuthFlowFactory(),
                tokenStore = SettingsTokenStore(LocalStorageSettingsStore()),
            )
        }
    }
}
