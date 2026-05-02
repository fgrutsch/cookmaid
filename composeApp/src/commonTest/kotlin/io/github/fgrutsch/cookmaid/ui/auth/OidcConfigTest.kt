package io.github.fgrutsch.cookmaid.ui.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class OidcConfigTest {

    @Test
    fun `accountUrl strips well-known openid-configuration suffix`() {
        val config = oidcConfig("https://auth.example.com/.well-known/openid-configuration")
        assertEquals("https://auth.example.com", config.accountUrl)
    }

    @Test
    fun `accountUrl returns discoveryUri unchanged when suffix is absent`() {
        val config = oidcConfig("https://auth.example.com")
        assertEquals("https://auth.example.com", config.accountUrl)
    }

    private fun oidcConfig(discoveryUri: String) = OidcConfig(
        discoveryUri = discoveryUri,
        clientId = "test",
        scope = "openid",
        redirectUri = "https://app/callback",
        postLogoutRedirectUri = "https://app/callback",
    )
}
