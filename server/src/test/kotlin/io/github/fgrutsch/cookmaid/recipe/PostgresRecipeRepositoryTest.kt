package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.catalog.CatalogItemRepository
import io.github.fgrutsch.cookmaid.common.SupportedLocale
import io.github.fgrutsch.cookmaid.catalog.Item
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

class PostgresRecipeRepositoryTest : BaseTest() {

    private suspend fun createUser(subject: String = "test-subject"): UserId {
        val userRepo = getKoin().get<UserRepository>()
        return UserId(userRepo.create(subject).id)
    }

    private fun data(
        name: String = "Pasta",
        description: String? = null,
        ingredients: List<RecipeIngredient> = emptyList(),
        steps: List<String> = emptyList(),
        tags: List<String> = emptyList(),
    ) = RecipeData(name, description, ingredients, steps, tags)

    @Test
    fun `create creates a new recipe with all fields`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()

        val recipe = repo.create(userId, data(
            ingredients = listOf(RecipeIngredient(Item.FreeText("Spaghetti"), 500f)),
            steps = listOf("Boil water", "Cook pasta"),
            tags = listOf("Noodles"),
        ), SupportedLocale.EN)

        assertNotNull(recipe.id)
        assertEquals("Pasta", recipe.name)
        assertEquals(1, recipe.ingredients.size)
        assertEquals("Spaghetti", recipe.ingredients.first().item.name)
        assertEquals(500f, recipe.ingredients.first().quantity)
        assertEquals(2, recipe.steps.size)
        assertEquals("Boil water", recipe.steps.first())
        assertEquals(listOf("Noodles"), recipe.tags)
    }

    @Test
    fun `create trims whitespace from name`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()

        val recipe = repo.create(userId, data(name = "  Pasta  "), SupportedLocale.EN)

        assertEquals("Pasta", recipe.name)
    }

    @Test
    fun `create with catalog item ingredient`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val catalogRepo = getKoin().get<CatalogItemRepository>()
        val userId = createUser()
        val catalogItem = catalogRepo.findAll(SupportedLocale.EN).first()

        val recipe = repo.create(userId, data(
            ingredients = listOf(RecipeIngredient(catalogItem, 400f)),
        ), SupportedLocale.EN)

        val ingredient = recipe.ingredients.first()
        val item = ingredient.item as Item.Catalog
        assertEquals(catalogItem.name, item.name)
        assertEquals(catalogItem.category.name, item.category.name)
    }

    @Test
    fun `find returns recipes for user`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()
        repo.create(userId, data(), SupportedLocale.EN)
        repo.create(userId, data(name = "Salad"), SupportedLocale.EN)

        val page = repo.find(userId, cursor = null, limit = 20, search = null, tag = null, locale = SupportedLocale.EN)

        assertEquals(2, page.items.size)
    }

    @Test
    fun `find returns empty for user with no recipes`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()

        val page = repo.find(userId, cursor = null, limit = 20, search = null, tag = null, locale = SupportedLocale.EN)
        assertTrue(page.items.isEmpty())
    }

    @Test
    fun `findById returns recipe with all children`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()
        val recipe = repo.create(userId, data(
            ingredients = listOf(RecipeIngredient(Item.FreeText("Noodles"), 500f)),
            steps = listOf("Step 1", "Step 2"),
            tags = listOf("Italian"),
        ), SupportedLocale.EN)

        val found = repo.findById(recipe.id, SupportedLocale.EN)

        assertNotNull(found)
        assertEquals(recipe.id, found.id)
        assertEquals(1, found.ingredients.size)
        assertEquals(2, found.steps.size)
        assertEquals(1, found.tags.size)
    }

    @Test
    fun `findById returns null for nonexistent id`() = runTest {
        val repo = getKoin().get<RecipeRepository>()

        assertNull(repo.findById(Uuid.random(), SupportedLocale.EN))
    }

    @Test
    fun `update replaces name, ingredients, steps, and tags`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()
        val recipe = repo.create(userId, data(
            name = "Old",
            ingredients = listOf(RecipeIngredient(Item.FreeText("Old item"), null)),
            steps = listOf("Old step"),
            tags = listOf("Old tag"),
        ), SupportedLocale.EN)

        repo.update(recipe.id, data(
            name = "New",
            ingredients = listOf(
                RecipeIngredient(Item.FreeText("New item 1"), 1f),
                RecipeIngredient(Item.FreeText("New item 2"), 2f),
            ),
            steps = listOf("New step 1", "New step 2", "New step 3"),
            tags = listOf("New tag"),
        ))

        val updated = repo.findById(recipe.id, SupportedLocale.EN)
        assertNotNull(updated)
        assertEquals("New", updated.name)
        assertEquals(2, updated.ingredients.size)
        assertEquals(3, updated.steps.size)
        assertEquals(listOf("New tag"), updated.tags)
    }

    @Test
    fun `delete removes the recipe and all children`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()
        val recipe = repo.create(userId, data(
            ingredients = listOf(RecipeIngredient(Item.FreeText("Item"), null)),
            steps = listOf("Step"),
            tags = listOf("Tag"),
        ), SupportedLocale.EN)

        repo.delete(recipe.id)

        assertNull(repo.findById(recipe.id, SupportedLocale.EN))
    }

    @Test
    fun `steps preserve order`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()

        val recipe = repo.create(userId, data(steps = listOf("First", "Second", "Third")), SupportedLocale.EN)

        val found = repo.findById(recipe.id, SupportedLocale.EN)
        assertNotNull(found)
        assertEquals(listOf("First", "Second", "Third"), found.steps)
    }

    @Test
    fun `isOwner returns true for own recipe`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()
        val recipe = repo.create(userId, data(), SupportedLocale.EN)

        assertTrue(repo.isOwner(userId, recipe.id))
    }

    @Test
    fun `isOwner returns false for another users recipe`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser("user-1")
        val otherUserId = createUser("user-2")
        val recipe = repo.create(userId, data(), SupportedLocale.EN)

        assertFalse(repo.isOwner(otherUserId, recipe.id))
    }
}
