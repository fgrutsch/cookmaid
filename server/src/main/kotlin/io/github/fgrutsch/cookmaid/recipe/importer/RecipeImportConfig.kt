package io.github.fgrutsch.cookmaid.recipe.importer

import io.ktor.server.config.ApplicationConfig

/**
 * Tuning for recipe import, read from `recipe.import` in `application.yaml`. The limits protect
 * the server from slow or oversized pages and bound what an LLM call costs.
 *
 * @property fetchTimeoutMillis time limit for downloading a page.
 * @property fetchMaxBytes size limit for a downloaded page, after decompression.
 * @property llmTimeoutMillis time limit for one LLM extraction.
 * @property llmMaxPageChars how much page text is sent to the LLM.
 * @property maxItems cap for the number of ingredients and of steps in a draft.
 * @property ingredientUnits words recognised as units after an amount, e.g. "g" in "200 g Mehl".
 */
data class RecipeImportConfig(
    val fetchTimeoutMillis: Long,
    val fetchMaxBytes: Long,
    val llmTimeoutMillis: Long,
    val llmMaxPageChars: Int,
    val maxItems: Int,
    val ingredientUnits: List<String>,
) {
    companion object {
        /**
         * Reads `recipe.import.*`; a missing or non-numeric value fails at startup.
         */
        fun from(config: ApplicationConfig): RecipeImportConfig {
            fun value(key: String) = config.property("recipe.import.$key").getString().trim()
            return RecipeImportConfig(
                fetchTimeoutMillis = value("fetch.timeout-millis").toLong(),
                fetchMaxBytes = value("fetch.max-bytes").toLong(),
                llmTimeoutMillis = value("llm.timeout-millis").toLong(),
                llmMaxPageChars = value("llm.max-page-chars").toInt(),
                maxItems = value("max-items").toInt(),
                ingredientUnits = value("ingredient-units").split(",").map { it.trim() }.filter { it.isNotBlank() },
            )
        }
    }
}
