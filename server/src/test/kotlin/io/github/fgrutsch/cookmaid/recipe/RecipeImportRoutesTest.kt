package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.recipe.importer.RecipeImportService
import io.github.fgrutsch.cookmaid.support.BaseIntegrationTest
import io.github.fgrutsch.cookmaid.support.TestJwt
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import org.junit.jupiter.api.Test
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecipeImportRoutesTest : BaseIntegrationTest() {

    private val recipePage = """
        <script type="application/ld+json">
        {"@type":"Recipe","name":"Pancakes","recipeIngredient":["200 g Mehl"],"recipeInstructions":"Mix"}
        </script>
    """.trimIndent()

    private suspend fun ApplicationTestBuilder.registeredToken(): String {
        val token = TestJwt.generateToken("import-test-user")
        client.post("/api/users/me") { bearerAuth(token) }
        return token
    }

    private suspend fun ApplicationTestBuilder.postImport(token: String, body: String) =
        jsonClient().post("/api/recipes/import") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    @Test
    fun `import returns 401 without token`() = integrationTest {
        assertEquals(HttpStatusCode.Unauthorized, client.post("/api/recipes/import").status)
    }

    @Test
    fun `import returns a draft without saving it`() = integrationTest {
        startApplication()
        loadKoinModules(module { single { RecipeImportService({ recipePage }, null) } })
        val token = registeredToken()

        val response = postImport(token, """{"url":"https://recipes.example/pancakes"}""")

        assertEquals(HttpStatusCode.OK, response.status)
        val draft = response.body<RecipeRequest>()
        assertEquals("Pancakes", draft.name)
        assertEquals("200 g", draft.ingredients.single().quantity)
        val saved = jsonClient().get("/api/recipes") { bearerAuth(token) }.body<RecipePage>()
        assertTrue(saved.items.isEmpty())
    }

    @Test
    fun `blocked url returns 422 with empty body`() = integrationTest {
        val token = registeredToken()

        val response = postImport(token, """{"url":"http://127.0.0.1:8081/secret"}""")

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals("", response.bodyAsText())
    }

    @Test
    fun `missing or blank url returns 400`() = integrationTest {
        val token = registeredToken()

        assertEquals(HttpStatusCode.BadRequest, postImport(token, "{}").status)
        assertEquals(HttpStatusCode.BadRequest, postImport(token, """{"url":"  "}""").status)
    }
}
