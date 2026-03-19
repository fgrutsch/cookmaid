package io.github.fgrutsch.cookmaid.mealplan

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
import kotlin.test.assertTrue

class MealPlanRoutesTest : BaseIntegrationTest() {

    @Test
    fun `GET meal-plan returns 401 without token`() = integrationTest {
        val response = client.get("/api/meal-plan?from=2026-03-16&to=2026-03-22")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    @Suppress("LongMethod")
    fun `full meal plan lifecycle`() = integrationTest {
        val token = TestJwt.generateToken("mealplan-test-user")
        val client = jsonClient()

        // Register user
        client.post("/api/users/me") { bearerAuth(token) }

        // Initially empty
        val initial = client.get("/api/meal-plan?from=2026-03-16&to=2026-03-22") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.OK, initial.status)
        val initialItems = initial.body<List<MealPlanItem>>()
        assertTrue(initialItems.isEmpty())

        // Create note item
        val createNoteResponse = client.post("/api/meal-plan") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateMealPlanItemRequest(
                day = kotlinx.datetime.LocalDate(2026, 3, 16),
                note = "Quick sandwich",
            ))
        }
        assertEquals(HttpStatusCode.Created, createNoteResponse.status)
        val noteItem = createNoteResponse.body<MealPlanItem>()
        assertTrue(noteItem is MealPlanItem.Note)
        assertEquals("Quick sandwich", noteItem.name)

        // Create recipe — first need a recipe
        val createRecipeResponse = client.post("/api/recipes") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(io.github.fgrutsch.cookmaid.recipe.CreateRecipeRequest(name = "Pasta"))
        }
        val recipe = createRecipeResponse.body<io.github.fgrutsch.cookmaid.recipe.Recipe>()

        val createRecipeItemResponse = client.post("/api/meal-plan") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateMealPlanItemRequest(
                day = kotlinx.datetime.LocalDate(2026, 3, 17),
                recipeId = recipe.id,
            ))
        }
        assertEquals(HttpStatusCode.Created, createRecipeItemResponse.status)
        val recipeItem = createRecipeItemResponse.body<MealPlanItem>()
        assertTrue(recipeItem is MealPlanItem.Recipe)
        assertEquals("Pasta", recipeItem.recipeName)

        // List — should have 2 items
        val listResponse = client.get("/api/meal-plan?from=2026-03-16&to=2026-03-22") {
            bearerAuth(token)
        }
        assertEquals(2, listResponse.body<List<MealPlanItem>>().size)

        // Update note
        val updateResponse = client.put("/api/meal-plan/${noteItem.id}") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateMealPlanItemRequest(note = "Updated sandwich"))
        }
        assertEquals(HttpStatusCode.NoContent, updateResponse.status)

        // Delete
        val deleteResponse = client.delete("/api/meal-plan/${noteItem.id}") {
            bearerAuth(token)
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify deletion
        val afterDelete = client.get("/api/meal-plan?from=2026-03-16&to=2026-03-22") {
            bearerAuth(token)
        }
        assertEquals(1, afterDelete.body<List<MealPlanItem>>().size)
    }
}
