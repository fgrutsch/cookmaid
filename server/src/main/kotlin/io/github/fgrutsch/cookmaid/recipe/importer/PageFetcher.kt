package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.common.ktor.RecipeImportException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.charset
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.io.readByteArray
import okhttp3.Dns
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.Proxy
import java.net.URI
import java.net.URISyntaxException
import java.net.UnknownHostException

/**
 * Fetches user-supplied recipe pages while refusing anything that is not a public
 * http(s) address. Every resolved IP is checked by a custom [Dns] (so OkHttp only
 * connects to vetted addresses), IP-literal hosts are checked up front because
 * OkHttp never asks [Dns] for them, and redirects are followed manually so each
 * hop is re-validated. Bodies are capped in size and requests in time.
 *
 * @param isAllowed decides whether an address may be contacted.
 */
class PageFetcher(private val isAllowed: (InetAddress) -> Boolean = ::isPublicAddress) {

    companion object {
        private const val MAX_BYTES = 2L * 1024 * 1024
        private const val MAX_REDIRECTS = 5
        private const val TIMEOUT_MILLIS = 10_000L
        private val REDIRECT_STATUSES = 300..399
        private val ALLOWED_SCHEMES = setOf("http", "https")
        private val IPV4_LITERAL = Regex("""\d{1,3}(\.\d{1,3}){3}""")
    }

    private val logger = KotlinLogging.logger {}

    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        followRedirects = false
        install(UserAgent) { agent = "Mozilla/5.0 (compatible; Cookmaid recipe import)" }
        install(HttpTimeout) { requestTimeoutMillis = TIMEOUT_MILLIS }
        engine {
            config {
                followRedirects(false)
                followSslRedirects(false)
                proxy(Proxy.NO_PROXY)
                dns { host ->
                    Dns.SYSTEM.lookup(host).also { addresses ->
                        if (addresses.any { !isAllowed(it) }) throw UnknownHostException("Blocked address")
                    }
                }
            }
        }
    }

    /**
     * Downloads the page at [url], following up to five redirects.
     *
     * @return the page body decoded with its declared charset (UTF-8 by default).
     * @throws RecipeImportException if the URL is invalid or blocked, the page is
     *   unreachable, too large, or redirects too often.
     */
    suspend fun fetch(url: String): String {
        var current = validate(url)
        repeat(MAX_REDIRECTS + 1) {
            when (val result = request(current)) {
                is FetchResult.Page -> return result.body
                is FetchResult.Redirect -> current = validate(result.location)
            }
        }
        throw RecipeImportException("Too many redirects")
    }

    private fun validate(url: String): URI {
        val uri = try {
            URI(url.trim())
        } catch (e: URISyntaxException) {
            throw RecipeImportException("Invalid URL", e)
        }
        val host = uri.host?.removePrefix("[")?.removeSuffix("]")
        val scheme = uri.scheme?.lowercase()
        val blocked = host.isNullOrBlank() || scheme !in ALLOWED_SCHEMES ||
            (isIpLiteral(host) && !isAllowed(InetAddress.getByName(host)))
        if (blocked) throw RecipeImportException("URL not allowed")
        return uri
    }

    private fun isIpLiteral(host: String): Boolean = host.contains(':') || IPV4_LITERAL.matches(host)

    @Suppress("TooGenericExceptionCaught")
    private suspend fun request(uri: URI): FetchResult = try {
        client.prepareGet(uri.toString()).execute { response -> toResult(uri, response) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: RecipeImportException) {
        throw e
    } catch (e: Exception) {
        logger.debug { "Fetching page failed: ${e::class.simpleName}" }
        throw RecipeImportException("Fetching page failed", e)
    }

    private suspend fun toResult(uri: URI, response: HttpResponse): FetchResult {
        val status = response.status.value
        val location = response.headers[HttpHeaders.Location]
        return when {
            status in REDIRECT_STATUSES && location != null -> FetchResult.Redirect(uri.resolve(location).toString())
            !response.status.isSuccess() -> throw IOException("Unexpected status $status")
            else -> {
                val bytes = response.bodyAsChannel().readRemaining(MAX_BYTES + 1).readByteArray()
                if (bytes.size > MAX_BYTES) throw RecipeImportException("Page too large")
                FetchResult.Page(String(bytes, response.charset() ?: Charsets.UTF_8))
            }
        }
    }

    private sealed interface FetchResult {
        data class Page(val body: String) : FetchResult
        data class Redirect(val location: String) : FetchResult
    }
}

/**
 * Whether [address] is a public unicast address that user-supplied URLs may reach.
 * Rejects loopback, private, link-local (incl. cloud metadata), CGNAT, multicast,
 * reserved ranges, IPv6 unique-local, and IPv4 addresses embedded in IPv6 forms
 * (IPv4-compatible, NAT64) that point at any of those.
 */
@Suppress("MagicNumber")
fun isPublicAddress(address: InetAddress): Boolean {
    val special = address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
        address.isSiteLocalAddress || address.isMulticastAddress
    val bytes = address.address.map { it.toInt() and 0xFF }
    return !special && when (address) {
        is Inet4Address -> !isReservedIpv4(bytes)
        is Inet6Address -> isPublicIpv6(address, bytes)
        else -> false
    }
}

@Suppress("MagicNumber")
private fun isReservedIpv4(b: List<Int>): Boolean =
    b[0] == 0 || b[0] >= 224 || // "this network", multicast, reserved, broadcast
        (b[0] == 100 && b[1] in 64..127) || // CGNAT
        (b[0] == 192 && b[1] == 0 && b[2] == 0) || // IETF protocol assignments
        (b[0] == 198 && b[1] in 18..19) // benchmarking

@Suppress("MagicNumber")
private fun isPublicIpv6(address: Inet6Address, b: List<Int>): Boolean {
    val uniqueLocal = b[0] and 0xFE == 0xFC
    val nat64 = b.subList(0, 4) == listOf(0x00, 0x64, 0xFF, 0x9B) && b.subList(4, 12).all { it == 0 }
    val embedsIpv4 = address.isIPv4CompatibleAddress || nat64
    return !uniqueLocal &&
        (!embedsIpv4 || isPublicAddress(InetAddress.getByAddress(address.address.copyOfRange(12, 16))))
}
