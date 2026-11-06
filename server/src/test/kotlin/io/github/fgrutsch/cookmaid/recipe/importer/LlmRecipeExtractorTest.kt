package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.support.testIngredientParser
import io.github.fgrutsch.cookmaid.support.testRecipeImportConfig
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.common.ktor.RecipeImportException
import io.github.fgrutsch.cookmaid.common.llm.LlmConfig
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LlmRecipeExtractorTest {

    private val config = LlmConfig("https://llm.example/v1/", "secret", "test-model")

    private val noRecipe = """
        {"found":false,"name":null,"description":null,"servings":null,"ingredientLines":[],"stepLines":[]}
    """.trimIndent()

    private fun doc(text: String) = Jsoup.parse("<html><body>$text</body></html>")

    private fun completion(content: String) =
        """{"choices":[{"message":{"role":"assistant","content":${JsonPrimitive(content)}}}]}"""

    private fun extractor(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        LlmRecipeExtractor(config, testRecipeImportConfig, testIngredientParser, MockEngine(handler))

    private fun MockRequestHandleScope.ok(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun `request uses the openai contract with a strict schema`() = runTest {
        var captured: HttpRequestData? = null
        extractor { request ->
            captured = request
            ok(completion(noRecipe))
        }.extract(doc("Some page"))

        val request = assertNotNull(captured)
        assertEquals("https://llm.example/v1/chat/completions", request.url.toString())
        assertEquals("Bearer secret", request.headers[HttpHeaders.Authorization])
        val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals("test-model", body.getValue("model").jsonPrimitive.content)
        assertEquals("json_schema", body.getValue("response_format").jsonObject.getValue("type").jsonPrimitive.content)
        val userMessage = body.getValue("messages").jsonArray[1].jsonObject.getValue("content").jsonPrimitive.content
        assertEquals("<page>\n[0] Some page\n</page>", userMessage)
    }

    @Test
    fun `disabled config returns null without calling the provider`() = runTest {
        var called = false
        val engine = MockEngine {
            called = true
            ok("{}")
        }
        val extractor = LlmRecipeExtractor(LlmConfig("", "", ""), testRecipeImportConfig, testIngredientParser, engine)

        assertNull(extractor.extract(doc("page")))
        assertFalse(called)
    }

    @Test
    fun `page text keeps one line per list item and drops navigation`() = runTest {
        var sent = ""
        extractor { request ->
            sent = (request.body as TextContent).text
            ok(completion(noRecipe))
        }.extract(
            Jsoup.parse(
                """
                <html><head><title>Schmarrn</title><script>var x = 1;</script></head><body>
                <nav>Hauptseite Zufällige Seite</nav>
                <ul><li>etwas Zitronenschale</li><li>1,5 g Salz</li></ul>
                </body></html>
                """.trimIndent(),
            ),
        )

        val page = Json.parseToJsonElement(sent).jsonObject.getValue("messages").jsonArray[1]
            .jsonObject.getValue("content").jsonPrimitive.content
        assertEquals("<page>\n[0] Schmarrn\n[1] etwas Zitronenschale\n[2] 1,5 g Salz\n</page>", page)
    }

    @Test
    fun `blank api key sends no authorization header`() = runTest {
        var captured: HttpRequestData? = null
        val engine = MockEngine { request ->
            captured = request
            ok(completion("""{"found":false,"ingredientLines":[],"stepLines":[]}"""))
        }
        val keyless = config.copy(apiKey = "")
        val extractor = LlmRecipeExtractor(keyless, testRecipeImportConfig, testIngredientParser, engine)
        extractor.extract(doc("page"))

        assertNull(captured?.headers?.get(HttpHeaders.Authorization))
    }

    @Test
    fun `valid response maps to a draft`() = runTest {
        val recipe = extractor {
            ok(
                completion(
                    """
                    {"found":true,"name":"Pasta","description":"Quick","servings":2,
                     "ingredientLines":[0,1,1,99],
                     "stepLines":[2,3]}
                    """,
                ),
            )
        }.extract(doc("<ul><li>250 g Spaghetti</li><li>Salz</li></ul><p>Boil</p><p>Serve</p>"))

        assertNotNull(recipe)
        assertEquals("Pasta", recipe.name)
        assertEquals(2, recipe.servings)
        assertEquals(
            listOf(RecipeIngredient(Item.FreeText("Spaghetti"), "250 g"), RecipeIngredient(Item.FreeText("Salz"))),
            recipe.ingredients,
        )
        assertEquals(listOf("Boil", "Serve"), recipe.steps)
    }

    @Test
    fun `content returned as an object is accepted`() = runTest {
        val recipe = extractor {
            val recipe = """{"found":true,"name":"Soup","ingredientLines":[0],"stepLines":[]}"""
            ok("""{"choices":[{"message":{"content":$recipe}}]}""")
        }.extract(doc("page"))

        assertEquals("Soup", recipe?.name)
    }

    @Test
    fun `recipe without a page ingredient line returns null`() = runTest {
        val recipe = """{"found":true,"name":"Gulasch","ingredientLines":[42],"stepLines":[0]}"""
        assertNull(extractor { ok(completion(recipe)) }.extract(doc("Rezept hier herunterladen")))
    }

    @Test
    fun `headings and step lines are not ingredients`() = runTest {
        val recipe = extractor {
            ok(completion("""{"found":true,"name":"Soup","ingredientLines":[0,1,2],"stepLines":[2]}"""))
        }.extract(doc("<p>Gewürze:</p><p>Salz</p><p>Salz in den Topf geben</p>"))

        assertEquals(listOf(RecipeIngredient(Item.FreeText("Salz"))), recipe?.ingredients)
        assertEquals(listOf("Salz in den Topf geben"), recipe?.steps)
    }

    @Test
    fun `no recipe answer returns null`() = runTest {
        assertNull(extractor { ok(completion(noRecipe)) }.extract(doc("page")))
    }

    @Test
    fun `non-success status fails`() = runTest {
        assertFailsWith<RecipeImportException> {
            extractor { respond("quota exceeded", HttpStatusCode.TooManyRequests) }.extract(doc("page"))
        }
    }

    @Test
    fun `non-json content fails`() = runTest {
        assertFailsWith<RecipeImportException> {
            extractor { ok(completion("Sorry, I cannot help with that.")) }.extract(doc("page"))
        }
    }

    @Test
    fun `page text is truncated`() = runTest {
        var sent = ""
        extractor { request ->
            sent = (request.body as TextContent).text
            ok(completion("""{"found":false,"ingredientLines":[],"stepLines":[]}"""))
        }.extract(doc("x".repeat(100_000)))

        assertTrue(sent.length < 70_000)
    }
}
