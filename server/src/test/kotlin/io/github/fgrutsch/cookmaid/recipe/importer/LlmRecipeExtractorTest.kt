package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.common.ktor.RecipeImportException
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
import io.ktor.server.config.MapApplicationConfig
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LlmRecipeExtractorTest {

    private val config = LlmConfig("https://llm.example/v1/", "secret", "test-model")

    private val noRecipe = """
        {"found":false,"name":null,"description":null,"servings":null,"ingredients":[],"steps":[]}
    """.trimIndent()

    private fun completion(content: String) =
        """{"choices":[{"message":{"role":"assistant","content":${JsonPrimitive(content)}}}]}"""

    private fun extractor(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        LlmRecipeExtractor(config, MockEngine(handler))

    private fun MockRequestHandleScope.ok(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun `request uses the openai contract with a strict schema`() = runTest {
        var captured: HttpRequestData? = null
        extractor { request ->
            captured = request
            ok(completion(noRecipe))
        }.extract("Some page")

        val request = assertNotNull(captured)
        assertEquals("https://llm.example/v1/chat/completions", request.url.toString())
        assertEquals("Bearer secret", request.headers[HttpHeaders.Authorization])
        val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals("test-model", body.getValue("model").jsonPrimitive.content)
        assertEquals("json_schema", body.getValue("response_format").jsonObject.getValue("type").jsonPrimitive.content)
        val userMessage = body.getValue("messages").jsonArray[1].jsonObject.getValue("content").jsonPrimitive.content
        assertEquals("<page>\nSome page\n</page>", userMessage)
    }

    @Test
    fun `blank api key sends no authorization header`() = runTest {
        var captured: HttpRequestData? = null
        LlmRecipeExtractor(config.copy(apiKey = ""), MockEngine { request ->
            captured = request
            ok(completion("""{"found":false,"ingredients":[],"steps":[]}"""))
        }).extract("page")

        assertNull(captured?.headers?.get(HttpHeaders.Authorization))
    }

    @Test
    fun `valid response maps to a draft`() = runTest {
        val recipe = extractor {
            ok(
                completion(
                    """
                    {"found":true,"name":"Pasta","description":"Quick","servings":2,
                     "ingredients":[{"name":"Spaghetti","quantity":"250 g"},{"name":"Salz","quantity":null}],
                     "steps":["Boil","Serve"]}
                    """,
                ),
            )
        }.extract("page")

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
            val recipe = """{"found":true,"name":"Soup","ingredients":[],"steps":["Cook"]}"""
            ok("""{"choices":[{"message":{"content":$recipe}}]}""")
        }.extract("page")

        assertEquals("Soup", recipe?.name)
    }

    @Test
    fun `no recipe answer returns null`() = runTest {
        assertNull(extractor { ok(completion("""{"found":false,"ingredients":[],"steps":[]}""")) }.extract("page"))
    }

    @Test
    fun `non-success status fails`() = runTest {
        assertFailsWith<RecipeImportException> {
            extractor { respond("quota exceeded", HttpStatusCode.TooManyRequests) }.extract("page")
        }
    }

    @Test
    fun `non-json content fails`() = runTest {
        assertFailsWith<RecipeImportException> {
            extractor { ok(completion("Sorry, I cannot help with that.")) }.extract("page")
        }
    }

    @Test
    fun `page text is truncated`() = runTest {
        var sent = ""
        extractor { request ->
            sent = (request.body as TextContent).text
            ok(completion("""{"found":false,"ingredients":[],"steps":[]}"""))
        }.extract("x".repeat(100_000))

        assertTrue(sent.length < 70_000)
    }

    @Test
    fun `config requires base url and model`() {
        assertNull(LlmConfig.from(MapApplicationConfig("llm.base-url" to "", "llm.model" to "m")))
        assertNull(LlmConfig.from(MapApplicationConfig("llm.base-url" to "https://x", "llm.model" to " ")))
        assertNull(LlmConfig.from(MapApplicationConfig()))
        assertEquals(
            LlmConfig("https://x", "", "m"),
            LlmConfig.from(MapApplicationConfig("llm.base-url" to "https://x", "llm.model" to "m")),
        )
    }
}
