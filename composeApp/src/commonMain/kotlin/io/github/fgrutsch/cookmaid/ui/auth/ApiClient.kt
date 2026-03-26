package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.ApiBaseUrl
import io.github.fgrutsch.cookmaid.common.SupportedLocale
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.ktor.oidcBearer
import org.publicvalue.multiplatform.oidc.tokenstore.TokenRefreshHandler
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore

class ApiClient(
    baseUrl: ApiBaseUrl,
    tokenStore: TokenStore,
    oidcClient: OpenIdConnectClient,
    private val effectiveLocale: () -> SupportedLocale,
) {
    private val refreshHandler = TokenRefreshHandler(tokenStore)

    val httpClient = HttpClient {
        expectSuccess = true
        defaultRequest {
            url(baseUrl.value)
            header(HttpHeaders.AcceptLanguage, effectiveLocale().code)
        }
        install(ContentNegotiation) { json() }
        install(Auth) {
            oidcBearer(
                tokenStore = tokenStore,
                refreshHandler = refreshHandler,
                client = oidcClient,
            )
        }
    }
}
