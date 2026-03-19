package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.support.BaseIntegrationTest
import io.github.fgrutsch.cookmaid.support.TestJwt
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RecipeRoutesTest : BaseIntegrationTest() {

    @Test
    fun `GET recipes returns 401 without token`() = integrationTest {
        val response = client.get("/api/recipes")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    @Suppress("LongMethod")
    fun `full recipe lifecycle`() = integrationTest {
        val token = TestJwt.generateToken("recipe-test-user")
        val client = jsonClient()

        // Register user
        client.post("/api/users/me") { bearerAuth(token) }

        // Initially no recipes
        val initialRecipes = client.get("/api/recipes") {
            bearerAuth(token)
        }.body<RecipePage>()
        assertTrue(initialRecipes.items.isEmpty())

        // Fetch a catalog item for use in ingredients
        val catalogItems = client.get("/api/catalog-items") {
            bearerAuth(token)
        }.body<List<Item.CatalogItem>>()
        val catalogItem = catalogItems.first()

        // Create a recipe with ingredients, steps, and tags
        val createResponse = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                CreateRecipeRequest(
                    name = "Spaghetti Bolognese",
                    ingredients = listOf(
                        RecipeIngredient(catalogItem, 400f),
                        RecipeIngredient(Item.FreeTextItem("Spaghetti"), 500f),
                    ),
                    steps = listOf("Cook pasta", "Make sauce", "Combine"),
                    tags = listOf("Noodles", "Meat"),
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        val recipe = createResponse.body<Recipe>()
        assertNotNull(recipe.id)
        assertEquals("Spaghetti Bolognese", recipe.name)
        assertEquals(2, recipe.ingredients.size)
        assertEquals(3, recipe.steps.size)
        assertEquals(listOf("Noodles", "Meat"), recipe.tags)

        // Verify catalog item is hydrated
        val returnedCatalogItem = recipe.ingredients.map { it.item }.filterIsInstance<Item.CatalogItem>().first()
        assertEquals(catalogItem.name, returnedCatalogItem.name)
        assertEquals(catalogItem.category.name, returnedCatalogItem.category.name)

        // Get recipe by id
        val getResponse = client.get("/api/recipes/${recipe.id}") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val fetched = getResponse.body<Recipe>()
        assertEquals(recipe.id, fetched.id)
        assertEquals(2, fetched.ingredients.size)
        assertEquals(3, fetched.steps.size)

        // Update recipe — change name, ingredients, steps, tags
        val updateResponse = client.put("/api/recipes/${recipe.id}") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                UpdateRecipeRequest(
                    name = "Bolognese Pasta",
                    ingredients = listOf(RecipeIngredient(Item.FreeTextItem("Penne"), 400f)),
                    steps = listOf("Cook penne", "Make sauce", "Mix together", "Serve"),
                    tags = listOf("Noodles"),
                ),
            )
        }
        assertEquals(HttpStatusCode.NoContent, updateResponse.status)

        // Verify update
        val updated = client.get("/api/recipes/${recipe.id}") {
            bearerAuth(token)
        }.body<Recipe>()
        assertEquals("Bolognese Pasta", updated.name)
        assertEquals(1, updated.ingredients.size)
        assertEquals("Penne", updated.ingredients.first().item.name)
        assertEquals(4, updated.steps.size)
        assertEquals(listOf("Noodles"), updated.tags)

        // Create second recipe and delete it
        val recipe2 = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateRecipeRequest(name = "Caesar Salad"))
        }.body<Recipe>()

        val deleteResponse = client.delete("/api/recipes/${recipe2.id}") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify only first recipe remains
        val finalRecipes = client.get("/api/recipes") {
            bearerAuth(token)
        }.body<RecipePage>()
        assertEquals(1, finalRecipes.items.size)
        assertEquals("Bolognese Pasta", finalRecipes.items.first().name)
    }
}
