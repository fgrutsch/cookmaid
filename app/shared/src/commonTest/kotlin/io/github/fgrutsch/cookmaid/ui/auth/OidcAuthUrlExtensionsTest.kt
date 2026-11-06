package io.github.fgrutsch.cookmaid.ui.auth

import io.ktor.http.URLBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OidcAuthUrlExtensionsTest {

    @Test
    fun `offline scope with resource adds prompt consent and resource`() {
        val url = URLBuilder("https://example.com/auth")

        url.appendLoginParameters(scope = "openid profile email offline_access", resource = "http://localhost:8081/api")

        assertEquals("consent", url.parameters["prompt"])
        assertEquals("http://localhost:8081/api", url.parameters["resource"])
    }

    @Test
    fun `offline scope without resource adds only prompt consent`() {
        val url = URLBuilder("https://example.com/auth")

        url.appendLoginParameters(scope = "openid profile email offline_access", resource = null)

        assertEquals("consent", url.parameters["prompt"])
        assertNull(url.parameters["resource"])
    }

    @Test
    fun `scope without offline access adds no prompt`() {
        val url = URLBuilder("https://example.com/auth")

        url.appendLoginParameters(scope = "openid profile email", resource = "http://localhost:8081/api")

        assertNull(url.parameters["prompt"])
        assertEquals("http://localhost:8081/api", url.parameters["resource"])
    }
}
