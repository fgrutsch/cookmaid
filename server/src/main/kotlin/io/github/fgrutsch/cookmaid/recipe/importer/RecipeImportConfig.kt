package io.github.fgrutsch.cookmaid.recipe.importer

import io.ktor.server.config.ApplicationConfig

/**
 * Tuning for recipe import, read from the `recipe-import` section of `application.yaml`.
 * The limits guard the server against oversized or hostile pages.
 *
 * @property fetchTimeoutMillis time limit for downloading a page.
 * @property fetchMaxBytes size limit for a downloaded page, after decompression.
 * @property fetchMaxRedirects how many redirects a page may take.
 * @property llmTimeoutMillis time limit for one LLM extraction.
 * @property llmMaxPageChars how much page text is sent to the LLM.
 * @property maxNameLength cap for the recipe name.
 * @property maxDescriptionLength cap for the description.
 * @property maxLineLength cap for each ingredient and step.
 * @property maxItems cap for the number of ingredients and of steps.
 * @property maxServings servings above this are dropped as implausible.
 * @property ingredientUnits words recognised as units after an amount, e.g. "g" in "200 g Mehl".
 */
data class RecipeImportConfig(
    val fetchTimeoutMillis: Long,
    val fetchMaxBytes: Long,
    val fetchMaxRedirects: Int,
    val llmTimeoutMillis: Long,
    val llmMaxPageChars: Int,
    val maxNameLength: Int,
    val maxDescriptionLength: Int,
    val maxLineLength: Int,
    val maxItems: Int,
    val maxServings: Int,
    val ingredientUnits: List<String>,
) {
    companion object {
        /**
         * Reads `recipe-import.*`; a missing or non-numeric value fails at startup.
         */
        fun from(config: ApplicationConfig): RecipeImportConfig {
            fun value(key: String) = config.property("recipe-import.$key").getString().trim()
            return RecipeImportConfig(
                fetchTimeoutMillis = value("fetch.timeout-millis").toLong(),
                fetchMaxBytes = value("fetch.max-bytes").toLong(),
                fetchMaxRedirects = value("fetch.max-redirects").toInt(),
                llmTimeoutMillis = value("llm.timeout-millis").toLong(),
                llmMaxPageChars = value("llm.max-page-chars").toInt(),
                maxNameLength = value("limits.max-name-length").toInt(),
                maxDescriptionLength = value("limits.max-description-length").toInt(),
                maxLineLength = value("limits.max-line-length").toInt(),
                maxItems = value("limits.max-items").toInt(),
                maxServings = value("limits.max-servings").toInt(),
                ingredientUnits = value("ingredient-units").split(",").map { it.trim() }.filter { it.isNotBlank() },
            )
        }
    }
}
