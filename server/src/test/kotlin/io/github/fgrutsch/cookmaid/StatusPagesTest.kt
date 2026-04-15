package io.github.fgrutsch.cookmaid

import io.github.fgrutsch.cookmaid.common.ktor.ErrorResponse
import io.github.fgrutsch.cookmaid.support.BaseIntegrationTest
import io.github.fgrutsch.cookmaid.support.TestJwt
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StatusPagesTest : BaseIntegrationTest() {

    @Test
    fun `unregistered user accessing protected endpoint gets 401 with structured error`() = integrationTest {
        val token = TestJwt.generateToken("unregistered-subject")
        val client = jsonClient()

        val response = client.get("/api/shopping-lists") { bearerAuth(token) }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(ErrorResponse("user_not_registered"), response.body<ErrorResponse>())
    }

    @Test
    fun `invalid UUID path parameter returns 400`() = integrationTest {
        val token = TestJwt.generateToken("uuid-test-user")
        val client = jsonClient()
        client.post("/api/users/me") { bearerAuth(token) }

        val response = client.get("/api/shopping-lists/not-a-uuid/items") { bearerAuth(token) }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `invalid date query parameter returns 400`() = integrationTest {
        val token = TestJwt.generateToken("date-test-user")
        val client = jsonClient()
        client.post("/api/users/me") { bearerAuth(token) }

        val response = client.get("/api/meal-plan?from=not-a-date&to=2026-01-01") { bearerAuth(token) }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `missing required query parameter returns 400`() = integrationTest {
        val token = TestJwt.generateToken("missing-param-user")
        val client = jsonClient()
        client.post("/api/users/me") { bearerAuth(token) }

        val response = client.get("/api/meal-plan") { bearerAuth(token) }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
