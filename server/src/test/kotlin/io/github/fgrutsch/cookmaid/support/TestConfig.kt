package io.github.fgrutsch.cookmaid.support

import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig

val testConfig: ApplicationConfig = MapApplicationConfig(
    "database.url" to TestPostgres.jdbcUrl,
    "database.user" to TestPostgres.username,
    "database.password" to TestPostgres.password,
    "oidc.issuer" to TestJwt.issuer,
    "oidc.jwks-url" to TestJwt.jwksUrl,
)
