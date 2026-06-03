package io.github.fgrutsch.cookmaid.ui.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OidcConfigTest {

    private val baseConfig = OidcConfig(
        discoveryUri = "https://example.com/.well-known/openid-configuration",
        clientId = "test-client",
        scope = "openid profile email",
        redirectUri = "http://localhost/callback",
        postLogoutRedirectUri = "http://localhost/callback",
        accountUri = "https://example.com/account",
    )

    @Test
    fun `resource defaults to null when not specified`() {
        assertNull(baseConfig.resource)
    }

    @Test
    fun `resource is set when specified`() {
        val config = baseConfig.copy(resource = "http://localhost:8081/api")

        assertEquals("http://localhost:8081/api", config.resource)
    }
}
