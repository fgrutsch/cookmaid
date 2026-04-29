package io.github.fgrutsch.cookmaid.support

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig

val testConfigEntries: List<Pair<String, String>> = listOf(
    "database.url" to TestPostgres.jdbcUrl,
    "database.user" to TestPostgres.username,
    "database.password" to TestPostgres.password,
    "database.pool-size" to "5",
    "oidc.issuer" to TestJwt.issuer,
    "oidc.jwks-url" to TestJwt.jwksUrl,
    "oidc.client-id" to TestJwt.AUDIENCE,
    "web.dir" to "web",
)

val testConfig: ApplicationConfig = MapApplicationConfig(*testConfigEntries.toTypedArray())
