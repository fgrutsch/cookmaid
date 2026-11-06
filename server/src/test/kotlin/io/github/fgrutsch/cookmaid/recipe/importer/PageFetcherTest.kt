package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.common.ktor.RecipeImportException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.util.zip.GZIPOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PageFetcherTest {

    private val server: EmbeddedServer<*, *> = embeddedServer(Netty, port = 0) {
        routing {
            get("/ok") { call.respondText("<html>hello</html>", ContentType.Text.Html) }
            get("/relative") { redirect("/ok") }
            get("/loop") { redirect("/loop") }
            get("/to-private") { redirect("http://10.0.0.1/") }
            get("/to-private-literal") { redirect("http://169.254.169.254/latest/meta-data") }
            get("/to-decimal-literal") { redirect("http://2852039166/latest/meta-data") }
            get("/big") { call.respondText("x".repeat(2 * 1024 * 1024 + 1)) }
            get("/missing") { call.respond(HttpStatusCode.NotFound) }
            get("/no-location") { call.respond(HttpStatusCode.Found) }
            get("/gzip") {
                val gzipped = ByteArrayOutputStream().also { out ->
                    GZIPOutputStream(out).use { it.write("<html>zipped</html>".toByteArray()) }
                }.toByteArray()
                call.response.header(HttpHeaders.ContentEncoding, "gzip")
                call.respondBytes(gzipped, ContentType.Text.Html)
            }
        }
    }.start(wait = false)

    private val port = runBlocking { server.engine.resolvedConnectors().first().port }

    private val urlNotAllowed = "URL not allowed"

    private val loopbackAllowed = HttpPageFetcher(isAllowed = { it.isLoopbackAddress })

    private suspend fun RoutingContext.redirect(location: String) {
        call.response.header(HttpHeaders.Location, location)
        call.respond(HttpStatusCode.Found)
    }

    @AfterAll
    fun stop() {
        server.stop()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "127.0.0.1", "10.0.0.1", "172.16.0.1", "192.168.1.1", "169.254.169.254", "0.0.0.0",
            "224.0.0.1", "::1", "fc00::1", "fe80::1", "::ffff:127.0.0.1",
        ],
    )
    fun `non-public addresses are rejected`(ip: String) {
        assertFalse(isPublicAddress(InetAddress.getByName(ip)))
    }

    @ParameterizedTest
    @ValueSource(strings = ["93.184.216.34", "198.20.0.1", "2606:4700::1"])
    fun `public addresses are accepted`(ip: String) {
        assertTrue(isPublicAddress(InetAddress.getByName(ip)))
    }

    @Test
    fun `hostname resolving to loopback is blocked`() = runTest {
        assertFailsWith<RecipeImportException> { HttpPageFetcher().fetch("http://localhost:$port/ok") }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "http://127.0.0.1:PORT/ok", "http://[::1]:PORT/ok", "http://169.254.169.254/", "http://2130706433:PORT/ok",
        ],
    )
    fun `ip literal hosts are blocked`(url: String) = runTest {
        val error = assertFailsWith<RecipeImportException> {
            HttpPageFetcher().fetch(url.replace("PORT", port.toString()))
        }
        assertEquals(urlNotAllowed, error.message)
    }

    @ParameterizedTest
    @ValueSource(strings = ["ftp://example.com/recipe", "file:///etc/passwd", "not a url", "/relative"])
    fun `non-http urls are rejected`(url: String) = runTest {
        assertFailsWith<RecipeImportException> { HttpPageFetcher().fetch(url) }
    }

    @Test
    fun `allowed page is returned`() = runTest {
        assertEquals("<html>hello</html>", loopbackAllowed.fetch("http://127.0.0.1:$port/ok"))
    }

    @Test
    fun `unrequested gzip body is decoded`() = runTest {
        assertEquals("<html>zipped</html>", loopbackAllowed.fetch("http://127.0.0.1:$port/gzip"))
    }

    @Test
    fun `relative redirect is followed`() = runTest {
        assertEquals("<html>hello</html>", loopbackAllowed.fetch("http://127.0.0.1:$port/relative"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["/to-private-literal", "/to-decimal-literal"])
    fun `redirect to a private address is blocked`(path: String) = runTest {
        val error = assertFailsWith<RecipeImportException> { loopbackAllowed.fetch("http://127.0.0.1:$port$path") }
        assertEquals(urlNotAllowed, error.message)
    }

    @Test
    fun `redirect to a private hostname is blocked`() = runTest {
        assertFailsWith<RecipeImportException> { loopbackAllowed.fetch("http://127.0.0.1:$port/to-private") }
    }

    @Test
    fun `too many redirects are rejected`() = runTest {
        assertFailsWith<RecipeImportException> { loopbackAllowed.fetch("http://127.0.0.1:$port/loop") }
    }

    @Test
    fun `body over 2 MB is rejected`() = runTest {
        assertFailsWith<RecipeImportException> { loopbackAllowed.fetch("http://127.0.0.1:$port/big") }
    }

    @Test
    fun `redirect without location is rejected`() = runTest {
        assertFailsWith<RecipeImportException> { loopbackAllowed.fetch("http://127.0.0.1:$port/no-location") }
    }

    @Test
    fun `non-success status is rejected`() = runTest {
        assertFailsWith<RecipeImportException> { loopbackAllowed.fetch("http://127.0.0.1:$port/missing") }
    }
}
