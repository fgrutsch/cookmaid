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
    "oidc.audience" to TestJwt.AUDIENCE,
    "web.dir" to "web",
    "csp.connect-src" to TestJwt.issuer,
    "llm.base-url" to "",
    "llm.api-key" to "",
    "llm.model" to "",
    "recipe-import.fetch.timeout-millis" to testRecipeImportConfig.fetchTimeoutMillis.toString(),
    "recipe-import.fetch.max-bytes" to testRecipeImportConfig.fetchMaxBytes.toString(),
    "recipe-import.fetch.max-redirects" to testRecipeImportConfig.fetchMaxRedirects.toString(),
    "recipe-import.llm.timeout-millis" to testRecipeImportConfig.llmTimeoutMillis.toString(),
    "recipe-import.llm.max-page-chars" to testRecipeImportConfig.llmMaxPageChars.toString(),
    "recipe-import.limits.max-name-length" to testRecipeImportConfig.maxNameLength.toString(),
    "recipe-import.limits.max-description-length" to testRecipeImportConfig.maxDescriptionLength.toString(),
    "recipe-import.limits.max-line-length" to testRecipeImportConfig.maxLineLength.toString(),
    "recipe-import.limits.max-items" to testRecipeImportConfig.maxItems.toString(),
    "recipe-import.limits.max-servings" to testRecipeImportConfig.maxServings.toString(),
    "recipe-import.ingredient-units" to testRecipeImportConfig.ingredientUnits.joinToString(","),
)

val testConfig: ApplicationConfig = MapApplicationConfig(*testConfigEntries.toTypedArray())
