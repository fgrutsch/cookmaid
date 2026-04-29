package io.github.fgrutsch.cookmaid

import io.github.fgrutsch.cookmaid.support.BaseIntegrationTest
import io.github.fgrutsch.cookmaid.support.TestJwt
import io.github.fgrutsch.cookmaid.support.testConfigEntries
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.koin.core.context.stopKoin
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun `service-worker js has no-cache header`(@TempDir webDir: File) {
        File(webDir, "service-worker.js").writeText("// sw")
        staticFileTest(webDir) {
            val response = client.get("/service-worker.js")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("no-cache", response.headers["Cache-Control"])
        }
    }

    @Test
    fun `other static files do not have no-cache header`(@TempDir webDir: File) {
        File(webDir, "index.html").writeText("<html></html>")
        staticFileTest(webDir) {
            val response = client.get("/index.html")
            assertEquals(HttpStatusCode.OK, response.status)
            assertNull(response.headers["Cache-Control"])
        }
    }

    private fun staticFileTest(webDir: File, block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) =
        testApplication {
            environment {
                val entries = testConfigEntries.filter { it.first != "web.dir" } +
                    ("web.dir" to webDir.absolutePath)
                config = MapApplicationConfig(*entries.toTypedArray())
            }
            application { module() }
            block()
            stopKoin()
        }
}
