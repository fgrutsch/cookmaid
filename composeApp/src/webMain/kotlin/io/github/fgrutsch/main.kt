package io.github.fgrutsch

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.github.fgrutsch.ui.auth.LocalStorageSettingsStore
import io.github.fgrutsch.ui.auth.OidcConfig
import kotlinx.browser.window
import org.publicvalue.multiplatform.oidc.appsupport.PlatformCodeAuthFlow
import org.publicvalue.multiplatform.oidc.appsupport.WebCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsTokenStore
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

@OptIn(ExperimentalWasmJsInterop::class)
private val currentPath: String = js("window.location.pathname")

@OptIn(ExperimentalWasmJsInterop::class)
private val oidcDiscoveryUri: String = js("window.__CONFIG__.OIDC_DISCOVERY_URI")

@OptIn(ExperimentalWasmJsInterop::class)
private val oidcClientId: String = js("window.__CONFIG__.OIDC_CLIENT_ID")

@OptIn(ExperimentalWasmJsInterop::class)
private val oidcScope: String = js("window.__CONFIG__.OIDC_SCOPE")

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    if (currentPath.startsWith("/callback")) {
        PlatformCodeAuthFlow.handleRedirect()
    } else {
        val origin = window.location.origin
        ComposeViewport {
            App(
                apiBaseUrl = ApiBaseUrl(origin),
                oidcConfig = OidcConfig(
                    discoveryUri = oidcDiscoveryUri,
                    clientId = oidcClientId,
                    scope = oidcScope,
                    redirectUri = "$origin/callback",
                    postLogoutRedirectUri = "$origin/callback",
                ),
                codeAuthFlowFactory = WebCodeAuthFlowFactory(),
                tokenStore = SettingsTokenStore(LocalStorageSettingsStore()),
            )
        }
    }
}
