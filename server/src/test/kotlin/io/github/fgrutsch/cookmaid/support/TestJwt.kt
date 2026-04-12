package io.github.fgrutsch.cookmaid.support

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import java.util.Date

object TestJwt {
    private val rsaKey: RSAKey = RSAKeyGenerator(2048)
        .keyID("test-key")
        .generate()

    private val signer = RSASSASigner(rsaKey)

    private val jwksServer = embeddedServer(Netty, port = 0) {
        routing {
            get("/.well-known/jwks.json") {
                val jwks = """{"keys":[${rsaKey.toPublicJWK().toJSONString()}]}"""
                call.respondText(jwks, io.ktor.http.ContentType.Application.Json)
            }
        }
    }.start(wait = false)

    private val port = runBlocking { jwksServer.engine.resolvedConnectors().first().port }

    val issuer: String = "http://localhost:$port"
    val jwksUrl: String = "$issuer/.well-known/jwks.json"
    const val AUDIENCE: String = "test-client-id"

    fun generateToken(subject: String, audience: String = AUDIENCE): String {
        val claims = JWTClaimsSet.Builder()
            .subject(subject)
            .issuer(issuer)
            .audience(audience)
            .expirationTime(Date(System.currentTimeMillis() + 60_000))
            .build()

        return SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).build(),
            claims,
        ).apply { sign(signer) }.serialize()
    }
}
