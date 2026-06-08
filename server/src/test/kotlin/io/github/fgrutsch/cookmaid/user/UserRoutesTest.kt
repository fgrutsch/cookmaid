package io.github.fgrutsch.cookmaid.user

import io.github.fgrutsch.cookmaid.support.BaseIntegrationTest
import io.github.fgrutsch.cookmaid.support.TestJwt
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
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
    fun `POST users me returns 401 with wrong audience`() = integrationTest {
        val token = TestJwt.generateToken("test-subject", audience = "wrong-client")

        val response = client.post("/api/users/me") {
            bearerAuth(token)
        }

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
    }

    @Test
    fun `POST users me is idempotent`() = integrationTest {
        val token = TestJwt.generateToken("test-subject")

        val first = jsonClient().post("/api/users/me") { bearerAuth(token) }.body<User>()
        val second = jsonClient().post("/api/users/me") { bearerAuth(token) }.body<User>()

        assertEquals(first, second)
    }

    @Test
    fun `DELETE users me returns 401 without token`() = integrationTest {
        val response = client.delete("/api/users/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE users me returns 401 when not registered`() = integrationTest {
        val token = TestJwt.generateToken("unregistered-delete-subject")

        val response = client.delete("/api/users/me") { bearerAuth(token) }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE users me deletes the registered user`() = integrationTest {
        val token = TestJwt.generateToken("test-subject")
        jsonClient().post("/api/users/me") { bearerAuth(token) } // register

        val response = client.delete("/api/users/me") { bearerAuth(token) }
        assertEquals(HttpStatusCode.NoContent, response.status)

        // user is gone: a second delete is now unauthorized (not registered)
        val second = client.delete("/api/users/me") { bearerAuth(token) }
        assertEquals(HttpStatusCode.Unauthorized, second.status)
    }
}
