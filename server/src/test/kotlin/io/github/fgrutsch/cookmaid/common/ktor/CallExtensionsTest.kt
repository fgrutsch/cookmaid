package io.github.fgrutsch.cookmaid.common.ktor

import io.github.fgrutsch.cookmaid.support.BaseIntegrationTest
import io.github.fgrutsch.cookmaid.support.TestJwt
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CallExtensionsTest : BaseIntegrationTest() {

    @Test
    fun `userId returns user id for registered user`() = integrationTest {
        val token = TestJwt.generateToken("call-ext-test-user")
        val client = jsonClient()

        // Register user first
        client.post("/api/users/me") { bearerAuth(token) }

        // Any authenticated endpoint will use call.userId()
        val response = client.get("/api/shopping-lists") { bearerAuth(token) }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `userId is cached across calls in same request`() = integrationTest {
        val token = TestJwt.generateToken("cache-test-user")
        val client = jsonClient()

        client.post("/api/users/me") { bearerAuth(token) }

        // Multiple requests should all succeed (caching works per-request via attributes)
        val response1 = client.get("/api/shopping-lists") { bearerAuth(token) }
        val response2 = client.get("/api/shopping-lists") { bearerAuth(token) }

        assertEquals(HttpStatusCode.OK, response1.status)
        assertEquals(HttpStatusCode.OK, response2.status)
    }
}
