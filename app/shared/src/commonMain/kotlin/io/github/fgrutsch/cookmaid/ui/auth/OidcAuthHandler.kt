package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.ui.user.UserClient
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.ktor.clearTokens
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.removeTokens
import org.publicvalue.multiplatform.oidc.tokenstore.saveTokens

class OidcAuthHandler(
    private val oidcConfig: OidcConfig,
    private val oidcClient: OpenIdConnectClient,
    private val authFlowFactory: CodeAuthFlowFactory,
    private val tokenStore: TokenStore,
    private val userClient: UserClient,
    private val apiClient: ApiClient,
) : AuthHandler {

    override suspend fun tryAutoLogin(): AuthResult {
        val user = userClient.getOrCreateUser()
        val profile = parseUserProfile(tokenStore.getIdToken())
        return AuthResult(user, profile)
    }

    override suspend fun login(): AuthResult {
        oidcClient.discover()
        val flow = authFlowFactory.createAuthFlow(oidcClient)
        val resource = oidcConfig.resource
        val tokens = if (resource != null) {
            flow.getAccessToken(
                configureAuthUrl = {
                    parameters.append("resource", resource)
                },
                configureTokenExchange = {
                    appendResourceToFormBody(resource)
                }
            )
        } else {
            flow.getAccessToken()
        }
        tokenStore.saveTokens(tokens)

        val user = userClient.getOrCreateUser()
        val profile = parseUserProfile(tokens.id_token)
        return AuthResult(user, profile)
    }

    override suspend fun logout() {
        tokenStore.removeTokens()
        apiClient.httpClient.clearTokens()
    }
}
