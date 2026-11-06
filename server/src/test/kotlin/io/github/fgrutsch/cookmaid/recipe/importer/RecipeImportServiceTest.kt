package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.support.testJsonLdExtractor
import io.github.fgrutsch.cookmaid.support.testRecipeImportConfig
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.common.ktor.RecipeImportException
import io.github.fgrutsch.cookmaid.common.llm.LlmConfig
import io.github.fgrutsch.cookmaid.recipe.RecipeRequest
import io.github.fgrutsch.cookmaid.user.UserId
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class RecipeImportServiceTest {

    private val userId = UserId(Uuid.random())
    private val url = "https://recipes.example/pasta"
    private var llmCalls = 0

    private val llmEngine = MockEngine {
        llmCalls++
        val content = """
            {"found":true,"name":"LLM Pasta","ingredients":[{"name":"Nudeln","quantity":"250 g"}],"steps":["Kochen"]}
        """.trimIndent()
        respond(
            """{"choices":[{"message":{"content":${JsonPrimitive(content)}}}]}""",
            HttpStatusCode.OK,
            headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }
    private val llm = LlmRecipeExtractor(
        LlmConfig("https://llm.example/v1", "", "m"),
        testRecipeImportConfig,
        llmEngine,
    )

    private val jsonLdPage = """
        <html><head><script type="application/ld+json">
        {"@type":"Recipe","name":"Pasta","recipeIngredient":["250 g Nudeln"],"recipeInstructions":"Kochen"}
        </script></head><body>Pasta</body></html>
    """.trimIndent()
    private val plainPage = "<html><head><title>Pasta</title></head><body>Some recipe text</body></html>"

    private fun service(page: String, llm: LlmRecipeExtractor? = this.llm) = RecipeImportService(
        { page },
        listOfNotNull(testJsonLdExtractor, llm),
        testRecipeImportConfig,
    )

    @Test
    fun `json-ld page never calls the llm`() = runTest {
        val draft = service(jsonLdPage).import(userId, url)

        assertEquals("Pasta", draft.name)
        assertEquals(0, llmCalls)
    }

    @Test
    fun `json-ld page imports without an llm`() = runTest {
        assertEquals("Pasta", service(jsonLdPage, llm = null).import(userId, url).name)
    }

    @Test
    fun `page without json-ld uses the llm once`() = runTest {
        val draft = service(plainPage).import(userId, url)

        assertEquals("LLM Pasta", draft.name)
        assertEquals(1, llmCalls)
    }

    @Test
    fun `page without json-ld and no llm finds no recipe`() = runTest {
        assertFailsWith<RecipeImportException> { service(plainPage, llm = null).import(userId, url) }
        assertEquals(0, llmCalls)
    }

    @Test
    fun `json-ld recipe without ingredients or steps falls back to the llm`() = runTest {
        val emptyRecipe = """<script type="application/ld+json">{"@type":"Recipe","name":"Empty"}</script>"""

        assertEquals("LLM Pasta", service(emptyRecipe).import(userId, url).name)
    }

    @Test
    fun `blank name is rejected`() = runTest {
        val blankName = """
            <script type="application/ld+json">{"@type":"Recipe","name":"  ","recipeInstructions":"x"}</script>
        """.trimIndent()

        assertFailsWith<RecipeImportException> { service(blankName, llm = null).import(userId, url) }
    }

    @Test
    fun `oversized results are capped`() = runTest {
        val ingredients = (1..150).joinToString(",") { "\"Zutat $it\"" }
        val longStep = "a".repeat(1_000)
        val page = """
            <script type="application/ld+json">
            {"@type":"Recipe","name":"${"n".repeat(300)}","recipeYield":"500",
             "recipeIngredient":[$ingredients],"recipeInstructions":["$longStep"]}
            </script>
        """.trimIndent()

        val draft = service(page, llm = null).import(userId, url)

        assertEquals(200, draft.name.length)
        assertEquals(100, draft.ingredients.size)
        assertEquals(500, draft.steps.single().length)
        assertNull(draft.servings)
        assertEquals(Item.FreeText("Zutat 1"), draft.ingredients.first().item)
    }

    @Test
    fun `blank name from the llm is rejected`() = runTest {
        val blankLlm = LlmRecipeExtractor(
            LlmConfig("https://llm.example/v1", "", "m"),
            testRecipeImportConfig,
            MockEngine {
                val content = """{"found":true,"name":"  ","ingredients":[],"steps":["Cook"]}"""
                respond(
                    """{"choices":[{"message":{"content":${JsonPrimitive(content)}}}]}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )

        assertFailsWith<RecipeImportException> { service(plainPage, blankLlm).import(userId, url) }
    }

    @Test
    fun `description is capped and out-of-range servings dropped`() = runTest {
        val page = """
            <script type="application/ld+json">
            {"@type":"Recipe","name":"Soup","description":"${"d".repeat(3_000)}","recipeYield":"0",
             "recipeInstructions":"Cook"}
            </script>
        """.trimIndent()

        val draft = service(page, llm = null).import(userId, url)

        assertEquals(2_000, draft.description?.length)
        assertNull(draft.servings)
    }

    private class FakeExtractor(
        override val priority: Int,
        private val result: RecipeRequest?,
        private val calls: MutableList<Int>,
    ) : RecipeExtractor {
        override suspend fun extract(document: Document): RecipeRequest? = result.also { calls += priority }
    }

    @Test
    fun `extractors run by priority and the first usable result wins`() = runTest {
        val calls = mutableListOf<Int>()
        val extractors = listOf(
            FakeExtractor(30, RecipeRequest(name = "Third", steps = listOf("c")), calls),
            FakeExtractor(20, RecipeRequest(name = "Second", steps = listOf("b")), calls),
            FakeExtractor(10, null, calls),
        )

        val draft = RecipeImportService({ plainPage }, extractors, testRecipeImportConfig).import(userId, url)

        assertEquals("Second", draft.name)
        assertEquals(listOf(10, 20), calls)
    }

    @Test
    fun `fetch failure propagates`() = runTest {
        val service = RecipeImportService(
            { throw RecipeImportException("Fetching page failed") },
            listOf(llm),
            testRecipeImportConfig,
        )

        assertFailsWith<RecipeImportException> { service.import(userId, url) }
    }
}
