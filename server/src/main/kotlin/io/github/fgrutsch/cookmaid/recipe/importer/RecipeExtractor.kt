package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.recipe.RecipeRequest
import org.jsoup.nodes.Document

/**
 * One way of reading a recipe from a fetched page.
 */
fun interface RecipeExtractor {
    /**
     * @return the recipe found in [document], or null when this extractor finds none.
     * @throws io.github.fgrutsch.cookmaid.common.ktor.RecipeImportException if extraction itself fails.
     */
    suspend fun extract(document: Document): RecipeRequest?
}
