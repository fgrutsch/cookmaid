package io.github.fgrutsch.cookmaid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.fgrutsch.cookmaid.ui.auth.OidcConfig
import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.AndroidDataStoreSettingsStore
import org.publicvalue.multiplatform.oidc.tokenstore.SettingsTokenStore

class MainActivity : ComponentActivity() {
    private val codeAuthFlowFactory = AndroidCodeAuthFlowFactory(useWebView = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        codeAuthFlowFactory.registerActivity(this)

        setContent {
            App(
                apiBaseUrl = ApiBaseUrl(BuildConfig.BASE_URL),
                oidcConfig = OidcConfig(
                    discoveryUri = BuildConfig.OIDC_DISCOVERY_URI,
                    clientId = BuildConfig.OIDC_CLIENT_ID,
                    scope = BuildConfig.OIDC_SCOPE,
                    redirectUri = "cookmaid://callback",
                    postLogoutRedirectUri = "cookmaid://callback",
                    accountUrl = BuildConfig.OIDC_ACCOUNT_URL,
                ),
                codeAuthFlowFactory = codeAuthFlowFactory,
                tokenStore = SettingsTokenStore(AndroidDataStoreSettingsStore(this)),
            )
        }
    }
}
