package io.github.fgrutsch.cookmaid.user

import io.github.fgrutsch.cookmaid.support.BaseIntegrationTest
import io.github.fgrutsch.cookmaid.support.TestJwt
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserRoutesTest : BaseIntegrationTest() {

    @Test
    fun `POST users me returns 401 without token`() = integrationTest {
        val response = client.post("/api/users/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST users me creates and returns user`() = integrationTest {
        val token = TestJwt.generateToken("test-subject")

        val response = jsonClient().post("/api/users/me") {
            bearerAuth(token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val user = response.body<User>()
        assertNotNull(user.id)
        assertEquals("test-subject", user.oidcSubject)
    }

    @Test
    fun `POST users me is idempotent`() = integrationTest {
        val token = TestJwt.generateToken("test-subject")

        val first = jsonClient().post("/api/users/me") { bearerAuth(token) }.body<User>()
        val second = jsonClient().post("/api/users/me") { bearerAuth(token) }.body<User>()

        assertEquals(first, second)
    }
}
