package io.github.fgrutsch.cookmaid.ui.auth

import io.github.fgrutsch.cookmaid.ui.user.UserClient
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.removeTokens
import org.publicvalue.multiplatform.oidc.tokenstore.saveTokens

class OidcAuthHandler(
    private val oidcClient: OpenIdConnectClient,
    private val authFlowFactory: CodeAuthFlowFactory,
    private val tokenStore: TokenStore,
    private val userClient: UserClient,
) : AuthHandler {

    override suspend fun tryAutoLogin(): AuthResult {
        val user = userClient.getOrCreateUser()
        val profile = parseUserProfile(tokenStore.getIdToken())
        return AuthResult(user, profile)
    }

    override suspend fun login(): AuthResult {
        oidcClient.discover()
        val flow = authFlowFactory.createAuthFlow(oidcClient)
        val tokens = flow.getAccessToken()
        tokenStore.saveTokens(tokens)

        val user = userClient.getOrCreateUser()
        val profile = parseUserProfile(tokens.id_token)
        return AuthResult(user, profile)
    }

    override suspend fun logout() {
        tokenStore.removeTokens()
    }
}
