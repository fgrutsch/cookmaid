package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.support.testIngredientParser
import io.github.fgrutsch.cookmaid.support.testJsonLdExtractor
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class JsonLdRecipeExtractorTest {

    private fun page(vararg jsonLd: String) = Jsoup.parse(
        "<html><head>" + jsonLd.joinToString("") { "<script type=\"application/ld+json\">$it</script>" } +
            "</head><body></body></html>",
    )

    @Test
    fun `top-level recipe fills every field`() = runTest {
        val recipe = testJsonLdExtractor.extract(
            page(
                """
                {"@context":"https://schema.org","@type":"Recipe","name":"Pancakes","description":"Fluffy",
                 "recipeYield":"4","recipeIngredient":["200 g Mehl","2 Eier"],
                 "recipeInstructions":[{"@type":"HowToStep","text":"Mix"},{"@type":"HowToStep","text":"Fry"}]}
                """,
            ),
        )

        assertNotNull(recipe)
        assertEquals("Pancakes", recipe.name)
        assertEquals("Fluffy", recipe.description)
        assertEquals(4, recipe.servings)
        assertEquals(
            listOf(RecipeIngredient(Item.FreeText("Mehl"), "200 g"), RecipeIngredient(Item.FreeText("Eier"), "2")),
            recipe.ingredients,
        )
        assertEquals(listOf("Mix", "Fry"), recipe.steps)
    }

    @Test
    fun `recipe inside graph is found`() = runTest {
        val recipe = testJsonLdExtractor.extract(
            page(
                """
                {"@graph":[{"@type":"WebPage","name":"Page"},{"@type":"Organization","name":"Org"},
                 {"@type":"Recipe","name":"Soup","recipeInstructions":"Boil"}]}
                """,
            ),
        )

        assertEquals("Soup", recipe?.name)
    }

    @Test
    fun `recipe under mainEntity with array type is found`() = runTest {
        val recipe = testJsonLdExtractor.extract(
            page("""{"@type":"WebPage","mainEntity":{"@type":["Recipe","NewsArticle"],"name":"Stew"}}"""),
        )

        assertEquals("Stew", recipe?.name)
    }

    @Test
    fun `instructions as sections and strings become a flat step list`() = runTest {
        val recipe = testJsonLdExtractor.extract(
            page(
                """
                {"@type":"Recipe","name":"Cake","recipeInstructions":[
                  {"@type":"HowToSection","name":"Dough","itemListElement":[
                    {"@type":"HowToStep","text":"Knead"},{"@type":"HowToStep","text":"Rest"}]},
                  "Bake"]}
                """,
            ),
        )

        assertEquals(listOf("Knead", "Rest", "Bake"), recipe?.steps)
    }

    @Test
    fun `instructions as one text block are split into lines`() = runTest {
        val recipe = testJsonLdExtractor.extract(
            page("""{"@type":"Recipe","name":"Tea","recipeInstructions":"Boil water\nSteep\n\nServe"}"""),
        )

        assertEquals(listOf("Boil water", "Steep", "Serve"), recipe?.steps)
    }

    @ParameterizedTest
    @ValueSource(strings = ["4", "\"4\"", "\"4 servings\"", "[\"4\",\"4 portions\"]"])
    fun `recipeYield shapes give servings`(yield: String) = runTest {
        val recipe = testJsonLdExtractor.extract(page("""{"@type":"Recipe","name":"X","recipeYield":$yield}"""))

        assertEquals(4, recipe?.servings)
    }

    @Test
    fun `malformed block is skipped`() = runTest {
        val recipe = testJsonLdExtractor.extract(page("{not json", """{"@type":"Recipe","name":"Salad"}"""))

        assertEquals("Salad", recipe?.name)
    }

    @Test
    fun `entities and tags are decoded to plain text`() = runTest {
        val recipe = testJsonLdExtractor.extract(
            page("""{"@type":"Recipe","name":"Mac &amp; Cheese","description":"<p>Very <b>good</b></p>"}"""),
        )

        assertEquals("Mac & Cheese", recipe?.name)
        assertEquals("Very good", recipe?.description)
    }

    @Test
    fun `unusual shapes fall back gracefully`() = runTest {
        val recipe = testJsonLdExtractor.extract(
            page(
                """{"@type":{"name":"Thing"},"name":"Not a recipe"}""",
                """
                {"@type":"Recipe","name":"Bread","recipeYield":{"value":2},"recipeIngredient":"500 g Mehl",
                 "recipeInstructions":[{"@type":"HowToStep","name":"Knead"}]}
                """,
            ),
        )

        assertNotNull(recipe)
        assertEquals("Bread", recipe.name)
        assertNull(recipe.servings)
        assertEquals(listOf(RecipeIngredient(Item.FreeText("Mehl"), "500 g")), recipe.ingredients)
        assertEquals(listOf("Knead"), recipe.steps)
    }

    @Test
    fun `page without recipe returns null`() = runTest {
        assertNull(testJsonLdExtractor.extract(page("""{"@type":"WebPage","name":"Blog"}""")))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "200 g Butter|Butter|200 g",
            "200g Butter|Butter|200g",
            "1/2 TL Salz|Salz|1/2 TL",
            "½ cup sugar|sugar|½ cup",
            "1 ½ cups milk|milk|1 ½ cups",
            "2-3 Zehen Knoblauch|Knoblauch|2-3 Zehen",
            "1,5 l Wasser|Wasser|1,5 l",
            "2 Eier|Eier|2",
        ],
    )
    fun `ingredient lines split into quantity and name`(line: String, name: String, quantity: String) = runTest {
        assertEquals(RecipeIngredient(Item.FreeText(name), quantity), testIngredientParser.parse(line))
    }

    @Test
    fun `ingredient line without amount keeps full text`() = runTest {
        assertEquals(
            RecipeIngredient(Item.FreeText("Salz und Pfeffer")),
            testIngredientParser.parse("Salz und Pfeffer"),
        )
    }

    @Test
    fun `hostile digit-run ingredient lines parse quickly`() = runTest {
        val lines = List(4_000) { "\"${"1".repeat(500)}\"" }.joinToString(",")
        val elapsed = measureTime {
            testJsonLdExtractor.extract(page("""{"@type":"Recipe","name":"X","recipeIngredient":[$lines]}"""))
        }

        assertTrue(elapsed < 2.seconds, "took $elapsed")
    }
}
