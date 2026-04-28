package io.github.fgrutsch.cookmaid

import io.github.fgrutsch.cookmaid.support.BaseIntegrationTest
import io.github.fgrutsch.cookmaid.support.TestJwt
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ApplicationTest : BaseIntegrationTest() {

    @Test
    fun testRoot() = integrationTest {
        val response = client.get("/")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `responses contain security headers`() = integrationTest {
        val response = client.get("/")
        assertEquals("DENY", response.headers["X-Frame-Options"])
        assertEquals("nosniff", response.headers["X-Content-Type-Options"])
        assertEquals(
            "default-src 'self'; script-src 'self' 'unsafe-inline' 'wasm-unsafe-eval'; worker-src 'self' blob:; " +
                "style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; " +
                "connect-src 'self' ${TestJwt.issuer}; object-src 'none'",
            response.headers["Content-Security-Policy"],
        )
    }
}
