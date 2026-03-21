package io.github.fgrutsch.cookmaid.mealplan

import io.github.fgrutsch.cookmaid.recipe.RecipeData
import io.github.fgrutsch.cookmaid.recipe.RecipeRepository
import io.github.fgrutsch.cookmaid.support.BaseTest
import io.github.fgrutsch.cookmaid.user.UserId
import io.github.fgrutsch.cookmaid.user.UserRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class MealPlanServiceTest : BaseTest() {

    private val monday = LocalDate(2026, 3, 16)
    private val tuesday = LocalDate(2026, 3, 17)
    private val sunday = LocalDate(2026, 3, 22)

    private suspend fun createUser(subject: String = "test-subject"): UserId {
        val userRepo = getKoin().get<UserRepository>()
        return UserId(userRepo.create(subject).id)
    }

    private suspend fun createRecipe(userId: UserId): Uuid {
        val recipeRepo = getKoin().get<RecipeRepository>()
        return recipeRepo.create(userId, RecipeData("Test Recipe", null, emptyList(), emptyList(), emptyList())).id
    }

    @Test
    fun `create note item`() = runTest {
        val service = getKoin().get<MealPlanService>()
        val userId = createUser()

        val item = service.create(userId, monday, recipeId = null, note = "Quick lunch")

        assertNotNull(item.id)
        assertTrue(item is MealPlanItem.Note)
        assertEquals("Quick lunch", item.name)
        assertEquals(monday, item.day)
    }

    @Test
    fun `create recipe item`() = runTest {
        val service = getKoin().get<MealPlanService>()
        val userId = createUser()
        val recipeId = createRecipe(userId)

        val item = service.create(userId, tuesday, recipeId = recipeId, note = null)

        assertTrue(item is MealPlanItem.Recipe)
        assertEquals(recipeId, item.recipeId)
        assertEquals("Test Recipe", item.recipeName)
        assertEquals(tuesday, item.day)
    }

    @Test
    fun `find returns items in date range`() = runTest {
        val service = getKoin().get<MealPlanService>()
        val userId = createUser()

        service.create(userId, monday, recipeId = null, note = "Monday meal")
        service.create(userId, tuesday, recipeId = null, note = "Tuesday meal")
        service.create(userId, sunday, recipeId = null, note = "Sunday meal")

        val items = service.find(userId, monday, sunday)
        assertEquals(3, items.size)
    }

    @Test
    fun `find excludes items outside range`() = runTest {
        val service = getKoin().get<MealPlanService>()
        val userId = createUser()

        service.create(userId, monday, recipeId = null, note = "Monday meal")
        service.create(userId, sunday, recipeId = null, note = "Sunday meal")

        val items = service.find(userId, tuesday, LocalDate(2026, 3, 21))
        assertEquals(0, items.size)
    }

    @Test
    fun `find does not return other users items`() = runTest {
        val service = getKoin().get<MealPlanService>()
        val userId1 = createUser("user-1")
        val userId2 = createUser("user-2")

        service.create(userId1, monday, recipeId = null, note = "User 1 meal")
        service.create(userId2, monday, recipeId = null, note = "User 2 meal")

        val items = service.find(userId1, monday, sunday)
        assertEquals(1, items.size)
        assertEquals("User 1 meal", (items.first() as MealPlanItem.Note).name)
    }

    @Test
    fun `update note`() = runTest {
        val service = getKoin().get<MealPlanService>()
        val userId = createUser()
        val item = service.create(userId, monday, recipeId = null, note = "Old note")

        val result = service.update(userId, item.id, day = null, note = "New note")

        assertTrue(result)
        val items = service.find(userId, monday, monday)
        assertEquals("New note", (items.first() as MealPlanItem.Note).name)
    }

    @Test
    fun `update fails for another users item`() = runTest {
        val service = getKoin().get<MealPlanService>()
        val userId = createUser("owner")
        val otherUserId = createUser("other")
        val item = service.create(userId, monday, recipeId = null, note = "My note")

        assertFalse(service.update(otherUserId, item.id, day = null, note = "Hacked"))
    }

    @Test
    fun `delete removes item`() = runTest {
        val service = getKoin().get<MealPlanService>()
        val userId = createUser()
        val item = service.create(userId, monday, recipeId = null, note = "To delete")

        assertTrue(service.delete(userId, item.id))
        assertTrue(service.find(userId, monday, monday).isEmpty())
    }

    @Test
    fun `delete fails for another users item`() = runTest {
        val service = getKoin().get<MealPlanService>()
        val userId = createUser("owner")
        val otherUserId = createUser("other")
        val item = service.create(userId, monday, recipeId = null, note = "My item")

        assertFalse(service.delete(otherUserId, item.id))
    }
}
