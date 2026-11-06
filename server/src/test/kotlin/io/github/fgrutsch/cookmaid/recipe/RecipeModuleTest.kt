package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.recipe.importer.JsonLdRecipeExtractor
import io.github.fgrutsch.cookmaid.recipe.importer.LlmRecipeExtractor
import io.github.fgrutsch.cookmaid.recipe.importer.MicrodataRecipeExtractor
import io.github.fgrutsch.cookmaid.recipe.importer.RecipeExtractor
import io.github.fgrutsch.cookmaid.support.BaseTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RecipeModuleTest : BaseTest() {

    @Test
    fun `all recipe extractors are bound`() {
        val extractors = getKoin().getAll<RecipeExtractor>().map { it::class }.toSet()

        assertEquals(
            setOf(JsonLdRecipeExtractor::class, MicrodataRecipeExtractor::class, LlmRecipeExtractor::class),
            extractors,
        )
    }
}
