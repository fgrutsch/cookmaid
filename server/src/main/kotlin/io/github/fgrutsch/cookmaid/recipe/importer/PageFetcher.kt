package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.common.ktor.RecipeImportException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.plugin
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Url
import io.ktor.http.charset
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Downloads a user-supplied recipe page.
 */
fun interface PageFetcher {
    /**
     * @return the page body.
     * @throws RecipeImportException if the page cannot or may not be fetched.
     */
    suspend fun fetch(url: String): String
}

/**
 * [PageFetcher] that only reaches public http(s) addresses, so users cannot make the
 * server call localhost, the Docker network, the LAN or cloud metadata. Every request,
 * including each redirect hop, is checked before it is sent. Bodies are capped in size
 * and requests in time.
 *
 * @param config the size, time and redirect limits.
 * @param isAllowed decides whether a resolved address may be contacted.
 * @param ioDispatcher runs the blocking DNS lookup.
 */
class HttpPageFetcher(
    private val config: RecipeImportConfig,
    private val isAllowed: (InetAddress) -> Boolean = ::isPublicAddress,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PageFetcher {

    companion object {
        private val ALLOWED_SCHEMES = setOf("http", "https")
    }

    private val logger = KotlinLogging.logger {}

    private val client = HttpClient(CIO) {
        expectSuccess = false
        install(UserAgent) { agent = "Mozilla/5.0 (compatible; Cookmaid recipe import)" }
        install(HttpTimeout) { requestTimeoutMillis = config.fetchTimeoutMillis }
        install(HttpSend) { maxSendCount = config.fetchMaxRedirects + 1 }
        // Some sites (e.g. lecker.de) send gzip even when it was not requested.
        install(ContentEncoding) {
            gzip()
            deflate()
        }
    }.apply {
        // Redirects re-enter HttpSend, so this also checks every redirect target.
        plugin(HttpSend).intercept { request ->
            validate(request.url.build())
            execute(request)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun fetch(url: String): String = try {
        client.prepareGet(url.trim()).execute { response ->
            if (!response.status.isSuccess()) throw RecipeImportException("Page returned ${response.status.value}")
            val bytes = response.bodyAsChannel().readRemaining(config.fetchMaxBytes + 1).readByteArray()
            if (bytes.size > config.fetchMaxBytes) throw RecipeImportException("Page too large")
            String(bytes, response.charset() ?: Charsets.UTF_8)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: RecipeImportException) {
        throw e
    } catch (e: Exception) {
        logger.debug { "Fetching page failed: ${e::class.simpleName}" }
        throw RecipeImportException("Fetching page failed", e)
    }

    private suspend fun validate(url: Url) {
        // shortcut: resolves separately from the connection, so DNS rebinding is not covered;
        // pin the resolved address if the importer is ever exposed to untrusted users.
        val addresses = withContext(ioDispatcher) { runCatching { InetAddress.getAllByName(url.host) }.getOrNull() }
        val allowed = url.protocol.name in ALLOWED_SCHEMES && !addresses.isNullOrEmpty() && addresses.all(isAllowed)
        if (!allowed) throw RecipeImportException("URL not allowed")
    }
}

/**
 * Whether [address] is public: not loopback, private, link-local (incl. cloud metadata),
 * wildcard, multicast or IPv6 unique-local.
 */
@Suppress("MagicNumber")
fun isPublicAddress(address: InetAddress): Boolean {
    val uniqueLocalV6 = address is Inet6Address && (address.address[0].toInt() and 0xFE) == 0xFC
    return !(
        address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress || uniqueLocalV6
        )
}
