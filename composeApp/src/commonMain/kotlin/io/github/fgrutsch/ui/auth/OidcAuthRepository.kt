package io.github.fgrutsch.ui.auth

import io.github.fgrutsch.auth.AuthRepository
import io.github.fgrutsch.auth.User
import io.github.fgrutsch.ui.user.UserClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.tokenstore.TokenStore
import org.publicvalue.multiplatform.oidc.tokenstore.removeTokens
import org.publicvalue.multiplatform.oidc.tokenstore.saveTokens

class OidcAuthRepository(
    private val oidcClient: OpenIdConnectClient,
    private val authFlowFactory: CodeAuthFlowFactory,
    private val tokenStore: TokenStore,
    private val userClient: UserClient,
) : AuthRepository {

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    var userProfile: UserProfile = UserProfile()
        private set

    suspend fun tryAutoLogin() {
        val user = userClient.getOrCreateUser()
        _currentUser.value = user
        _isAuthenticated.value = true
        userProfile = parseUserProfile(tokenStore.getIdToken())
    }

    override suspend fun login() {
        oidcClient.discover()
        val flow = authFlowFactory.createAuthFlow(oidcClient)
        val tokens = flow.getAccessToken()
        tokenStore.saveTokens(tokens)

        val user = userClient.getOrCreateUser()
        _currentUser.value = user
        _isAuthenticated.value = true
        userProfile = parseUserProfile(tokens.id_token)
    }

    override suspend fun logout() {
        try {
            val idToken = tokenStore.getIdToken()
            if (idToken != null) {
                val endSessionFlow = authFlowFactory.createEndSessionFlow(oidcClient)
                endSessionFlow.endSession(idToken)
            }
        } finally {
            tokenStore.removeTokens()
            _currentUser.value = null
            _isAuthenticated.value = false
            userProfile = UserProfile()
        }
    }
}
