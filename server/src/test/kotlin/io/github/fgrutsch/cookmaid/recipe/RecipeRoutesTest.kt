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
        }.body<List<Item.Catalog>>()
        val catalogItem = catalogItems.first()

        // Create a recipe with ingredients, steps, and tags
        val createResponse = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                RecipeRequest(
                    name = "Spaghetti Bolognese",
                    ingredients = listOf(
                        RecipeIngredient(catalogItem, "400"),
                        RecipeIngredient(Item.FreeText("Spaghetti"), "500"),
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
        val returnedCatalogItem = recipe.ingredients.map { it.item }.filterIsInstance<Item.Catalog>().first()
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
                RecipeRequest(
                    name = "Bolognese Pasta",
                    ingredients = listOf(RecipeIngredient(Item.FreeText("Penne"), "400")),
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
            setBody(RecipeRequest(name = "Caesar Salad"))
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

    @Test
    fun `GET recipes random returns 401 without token`() = integrationTest {
        val response = client.get("/api/recipes/random")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET recipes random returns 404 when no recipes`() = integrationTest {
        val token = TestJwt.generateToken("random-empty-user")
        val client = jsonClient()

        client.post("/api/users/me") { bearerAuth(token) }

        val response = client.get("/api/recipes/random") { bearerAuth(token) }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET recipes random returns a recipe`() = integrationTest {
        val token = TestJwt.generateToken("random-user")
        val client = jsonClient()

        client.post("/api/users/me") { bearerAuth(token) }
        val created = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Random Test Recipe"))
        }.body<Recipe>()

        val response = client.get("/api/recipes/random") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, response.status)
        val recipe = response.body<Recipe>()
        assertEquals(created.id, recipe.id)
        assertEquals("Random Test Recipe", recipe.name)
    }

    @Test
    fun `GET recipes random with excludeIds returns different recipe when possible`() = integrationTest {
        val token = TestJwt.generateToken("random-exclude-user")
        val client = jsonClient()

        client.post("/api/users/me") { bearerAuth(token) }
        val recipe1 = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Recipe A"))
        }.body<Recipe>()
        val recipe2 = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Recipe B"))
        }.body<Recipe>()

        val response = client.get("/api/recipes/random?excludeIds=${recipe1.id}") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val result = response.body<Recipe>()
        assertEquals(recipe2.id, result.id)
    }

    @Test
    fun `GET recipes random with excludeIds falls back when only one recipe`() = integrationTest {
        val token = TestJwt.generateToken("random-fallback-user")
        val client = jsonClient()

        client.post("/api/users/me") { bearerAuth(token) }
        val only = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Only Recipe"))
        }.body<Recipe>()

        val response = client.get("/api/recipes/random?excludeIds=${only.id}") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val result = response.body<Recipe>()
        assertEquals(only.id, result.id)
    }

    @Test
    fun `GET recipes random with multiple excludeIds excludes all`() = integrationTest {
        val token = TestJwt.generateToken("random-multi-exclude-user")
        val client = jsonClient()

        client.post("/api/users/me") { bearerAuth(token) }
        val recipe1 = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Recipe A"))
        }.body<Recipe>()
        val recipe2 = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Recipe B"))
        }.body<Recipe>()
        val recipe3 = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Recipe C"))
        }.body<Recipe>()

        val response = client.get("/api/recipes/random?excludeIds=${recipe1.id},${recipe2.id}") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val result = response.body<Recipe>()
        assertEquals(recipe3.id, result.id)
    }

    @Test
    fun `GET recipes random with all recipes excluded falls back`() = integrationTest {
        val token = TestJwt.generateToken("random-all-excluded-user")
        val client = jsonClient()

        client.post("/api/users/me") { bearerAuth(token) }
        val recipe1 = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Recipe A"))
        }.body<Recipe>()
        val recipe2 = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Recipe B"))
        }.body<Recipe>()

        val response = client.get("/api/recipes/random?excludeIds=${recipe1.id},${recipe2.id}") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val result = response.body<Recipe>()
        assertTrue(result.id == recipe1.id || result.id == recipe2.id)
    }

    @Test
    fun `GET recipes random with tag filter`() = integrationTest {
        val token = TestJwt.generateToken("random-tag-user")
        val client = jsonClient()

        client.post("/api/users/me") { bearerAuth(token) }
        client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Italian Dish", tags = listOf("Italian")))
        }.body<Recipe>()
        client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Mexican Dish", tags = listOf("Mexican")))
        }.body<Recipe>()

        val response = client.get("/api/recipes/random?tag=Italian") { bearerAuth(token) }
        assertEquals(HttpStatusCode.OK, response.status)
        val result = response.body<Recipe>()
        assertEquals("Italian Dish", result.name)
    }

    @Test
    fun `GET recipes random with tag returns 404 when no match`() = integrationTest {
        val token = TestJwt.generateToken("random-notag-user")
        val client = jsonClient()

        client.post("/api/users/me") { bearerAuth(token) }
        client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(RecipeRequest(name = "Untagged Recipe"))
        }

        val response = client.get("/api/recipes/random?tag=NonExistent") { bearerAuth(token) }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
