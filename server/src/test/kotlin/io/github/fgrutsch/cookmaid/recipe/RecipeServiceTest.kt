package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.common.SupportedLocale
import io.github.fgrutsch.cookmaid.support.BaseTest
import io.github.fgrutsch.cookmaid.user.UserId
import io.github.fgrutsch.cookmaid.user.UserRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class RecipeServiceTest : BaseTest() {

    private suspend fun createUser(subject: String = "test-subject"): UserId {
        val userRepo = getKoin().get<UserRepository>()
        return UserId(userRepo.create(subject).id)
    }

    private suspend fun createUserWithRecipe(subject: String = "test-subject"): Pair<UserId, Recipe> {
        val userId = createUser(subject)
        val service = getKoin().get<RecipeService>()
        val data = RecipeData(
            name = "Test Recipe",
            description = null,
            ingredients = listOf(RecipeIngredient(Item.FreeText("Flour"), "200")),
            steps = listOf("Mix", "Bake"),
            tags = listOf("Baking"),
            servings = null,
        )
        val recipe = service.create(userId, data, SupportedLocale.EN)
        return userId to recipe
    }

    @Test
    fun `findByUser returns recipes`() = runTest {
        val service = getKoin().get<RecipeService>()
        val (userId, _) = createUserWithRecipe()

        val page = service.find(
            userId, cursor = null, limit = 20, search = null, tag = null, locale = SupportedLocale.EN,
        )
        assertEquals(1, page.items.size)
    }

    @Test
    fun `findById returns recipe for own recipe`() = runTest {
        val service = getKoin().get<RecipeService>()
        val (userId, recipe) = createUserWithRecipe()

        val found = service.findById(userId, recipe.id, SupportedLocale.EN)

        assertNotNull(found)
        assertEquals(recipe.id, found.id)
    }

    @Test
    fun `findById returns null for another users recipe`() = runTest {
        val service = getKoin().get<RecipeService>()
        val (_, recipe) = createUserWithRecipe("user-1")
        val otherUserId = createUser("user-2")

        assertNull(service.findById(otherUserId, recipe.id, SupportedLocale.EN))
    }

    @Test
    fun `create creates a recipe`() = runTest {
        val service = getKoin().get<RecipeService>()
        val userId = createUser()

        val recipe = service.create(
            userId, RecipeData("Pasta", null, emptyList(), emptyList(), emptyList(), servings = null), SupportedLocale.EN,
        )

        assertEquals("Pasta", recipe.name)
    }

    @Test
    fun `update succeeds for own recipe`() = runTest {
        val service = getKoin().get<RecipeService>()
        val (userId, recipe) = createUserWithRecipe()

        val request = RecipeData("New Name", null, emptyList(), emptyList(), emptyList(), servings = null)
        val result = service.update(userId, recipe.id, request)

        assertTrue(result)
    }

    @Test
    fun `update fails for another users recipe`() = runTest {
        val service = getKoin().get<RecipeService>()
        val (_, recipe) = createUserWithRecipe("user-1")
        val otherUserId = createUser("user-2")

        val request = RecipeData("Hacked", null, emptyList(), emptyList(), emptyList(), servings = null)
        assertFalse(service.update(otherUserId, recipe.id, request))
    }

    @Test
    fun `delete succeeds for own recipe`() = runTest {
        val service = getKoin().get<RecipeService>()
        val (userId, recipe) = createUserWithRecipe()

        assertTrue(service.delete(userId, recipe.id))
    }

    @Test
    fun `delete fails for another users recipe`() = runTest {
        val service = getKoin().get<RecipeService>()
        val (_, recipe) = createUserWithRecipe("user-1")
        val otherUserId = createUser("user-2")

        assertFalse(service.delete(otherUserId, recipe.id))
    }

    @Test
    fun `delete returns false for nonexistent recipe`() = runTest {
        val service = getKoin().get<RecipeService>()
        val userId = createUser()

        assertFalse(service.delete(userId, Uuid.random()))
    }
}
