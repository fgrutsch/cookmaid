package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.support.testIngredientParser
import io.github.fgrutsch.cookmaid.support.testRecipeImportConfig
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MicrodataRecipeExtractorTest {

    private val extractor = MicrodataRecipeExtractor(testRecipeImportConfig, testIngredientParser)

    @Test
    fun `reads recipe properties from meta tags and visible text`() = runTest {
        // Shape taken from ichkoche.at: meta tags plus nested author and rating items.
        val html = """
            <div itemscope itemtype="https://schema.org/Recipe">
              <span itemprop="author" itemscope itemtype="https://schema.org/Person">
                <meta itemprop="name" content="ARGE Heumilch">
              </span>
              <h1 itemprop="name">Gebackener Kürbis</h1>
              <meta itemprop="description" content="Heumilch aufkochen.">
              <div itemprop="aggregateRating" itemscope itemtype="https://schema.org/AggregateRating">
                <meta itemprop="ratingValue" content="4.6">
              </div>
              <meta itemprop="recipeYield" content="4">
              <meta itemprop="recipeIngredient" content="200 g Hartk&auml;se">
              <meta itemprop="recipeIngredient" content="Prise Salz">
            </div>
        """.trimIndent()

        val recipe = extractor.extract(Jsoup.parse(html))

        assertNotNull(recipe)
        assertEquals("Gebackener Kürbis", recipe.name)
        assertEquals("Heumilch aufkochen.", recipe.description)
        assertEquals(4, recipe.servings)
        assertEquals(
            listOf(RecipeIngredient(Item.FreeText("Hartkäse"), "200 g"), RecipeIngredient(Item.FreeText("Prise Salz"))),
            recipe.ingredients,
        )
        assertEquals(emptyList(), recipe.steps)
    }

    @Test
    fun `reads steps from HowToStep items or list entries`() = runTest {
        val html = """
            <div itemscope itemtype="http://schema.org/Recipe">
              <span itemprop="name">Tee</span>
              <div itemprop="recipeInstructions">
                <div itemscope itemtype="https://schema.org/HowToStep"><span itemprop="text">Wasser kochen</span></div>
                <div itemscope itemtype="https://schema.org/HowToStep"><span itemprop="text">Ziehen lassen</span></div>
              </div>
            </div>
            <div itemscope itemtype="https://schema.org/Recipe"><span itemprop="name">Ignored second recipe</span></div>
        """.trimIndent()

        assertEquals(listOf("Wasser kochen", "Ziehen lassen"), extractor.extract(Jsoup.parse(html))?.steps)

        val listHtml = """
            <div itemscope itemtype="https://schema.org/Recipe"><span itemprop="name">Brot</span>
              <ol itemprop="recipeInstructions"><li>Kneten</li><li>Backen</li></ol></div>
        """.trimIndent()

        assertEquals(listOf("Kneten", "Backen"), extractor.extract(Jsoup.parse(listHtml))?.steps)
    }

    @Test
    fun `page without recipe microdata returns null`() = runTest {
        val html = """<div itemscope itemtype="https://schema.org/Person"><span itemprop="name">Max</span></div>"""

        assertNull(extractor.extract(Jsoup.parse(html)))
    }

    @Test
    fun `recipe without a name returns null`() = runTest {
        val html = """
            <div itemscope itemtype="https://schema.org/Recipe"><meta itemprop="recipeYield" content="2"></div>
        """.trimIndent()

        assertNull(extractor.extract(Jsoup.parse(html)))
    }
}
