package io.github.fgrutsch

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    companion object {
        private val postgres = PostgreSQLContainer("postgres:18.3-alpine").apply {
            start()
        }
    }

    @Test
    fun testRoot() = testApplication {
        environment {
            config = MapApplicationConfig(
                "oidc.issuer" to "http://localhost:1411",
                "oidc.jwks-url" to "http://localhost:1411/.well-known/jwks.json",
                "database.url" to postgres.jdbcUrl,
                "database.user" to postgres.username,
                "database.password" to postgres.password,
            )
        }
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
