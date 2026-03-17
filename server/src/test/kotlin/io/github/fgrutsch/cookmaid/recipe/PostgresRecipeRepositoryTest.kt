package io.github.fgrutsch.cookmaid.recipe

import io.github.fgrutsch.cookmaid.catalog.CatalogItemRepository
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.support.BaseTest
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

    private suspend fun createUser(subject: String = "test-subject"): Uuid {
        val userRepo = getKoin().get<UserRepository>()
        return userRepo.create(subject).id
    }

    @Test
    fun `create creates a new recipe with all fields`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()

        val recipe = repo.create(
            userId, "Pasta",
            ingredients = listOf(RecipeIngredient(Item.FreeTextItem("Spaghetti"), 500f)),
            steps = listOf("Boil water", "Cook pasta"),
            tags = listOf("Noodles"),
        )

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

        val recipe = repo.create(userId, "  Pasta  ", emptyList(), emptyList(), emptyList())

        assertEquals("Pasta", recipe.name)
    }

    @Test
    fun `create with catalog item ingredient`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val catalogRepo = getKoin().get<CatalogItemRepository>()
        val userId = createUser()
        val catalogItem = catalogRepo.findAll().first()

        val recipe = repo.create(
            userId, "Pasta",
            ingredients = listOf(RecipeIngredient(catalogItem, 400f)),
            steps = emptyList(),
            tags = emptyList(),
        )

        val ingredient = recipe.ingredients.first()
        val item = ingredient.item as Item.CatalogItem
        assertEquals(catalogItem.name, item.name)
        assertEquals(catalogItem.category.name, item.category.name)
    }

    @Test
    fun `findByUserId returns recipes for user`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()
        repo.create(userId, "Pasta", emptyList(), emptyList(), emptyList())
        repo.create(userId, "Salad", emptyList(), emptyList(), emptyList())

        val page = repo.findByUserId(userId, cursor = null, limit = 20, search = null, tag = null)

        assertEquals(2, page.items.size)
    }

    @Test
    fun `findByUserId returns empty for user with no recipes`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()

        assertTrue(repo.findByUserId(userId, cursor = null, limit = 20, search = null, tag = null).items.isEmpty())
    }

    @Test
    fun `findById returns recipe with all children`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()
        val recipe = repo.create(
            userId, "Pasta",
            ingredients = listOf(RecipeIngredient(Item.FreeTextItem("Noodles"), 500f)),
            steps = listOf("Step 1", "Step 2"),
            tags = listOf("Italian"),
        )

        val found = repo.findById(recipe.id)

        assertNotNull(found)
        assertEquals(recipe.id, found.id)
        assertEquals(1, found.ingredients.size)
        assertEquals(2, found.steps.size)
        assertEquals(1, found.tags.size)
    }

    @Test
    fun `findById returns null for nonexistent id`() = runTest {
        val repo = getKoin().get<RecipeRepository>()

        assertNull(repo.findById(Uuid.random()))
    }

    @Test
    fun `update replaces name, ingredients, steps, and tags`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()
        val recipe = repo.create(
            userId, "Old",
            ingredients = listOf(RecipeIngredient(Item.FreeTextItem("Old item"), null)),
            steps = listOf("Old step"),
            tags = listOf("Old tag"),
        )

        repo.update(
            recipe.id, "New",
            ingredients = listOf(
                RecipeIngredient(Item.FreeTextItem("New item 1"), 1f),
                RecipeIngredient(Item.FreeTextItem("New item 2"), 2f),
            ),
            steps = listOf("New step 1", "New step 2", "New step 3"),
            tags = listOf("New tag"),
        )

        val updated = repo.findById(recipe.id)
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
        val recipe = repo.create(
            userId, "To Delete",
            ingredients = listOf(RecipeIngredient(Item.FreeTextItem("Item"), null)),
            steps = listOf("Step"),
            tags = listOf("Tag"),
        )

        repo.delete(recipe.id)

        assertNull(repo.findById(recipe.id))
    }

    @Test
    fun `steps preserve order`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()

        val recipe = repo.create(
            userId, "Pasta",
            ingredients = emptyList(),
            steps = listOf("First", "Second", "Third"),
            tags = emptyList(),
        )

        val found = repo.findById(recipe.id)
        assertNotNull(found)
        assertEquals(listOf("First", "Second", "Third"), found.steps)
    }

    @Test
    fun `isOwnedByUser returns true for own recipe`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser()
        val recipe = repo.create(userId, "My Recipe", emptyList(), emptyList(), emptyList())

        assertTrue(repo.isOwnedByUser(userId, recipe.id))
    }

    @Test
    fun `isOwnedByUser returns false for another users recipe`() = runTest {
        val repo = getKoin().get<RecipeRepository>()
        val userId = createUser("user-1")
        val otherUserId = createUser("user-2")
        val recipe = repo.create(userId, "My Recipe", emptyList(), emptyList(), emptyList())

        assertFalse(repo.isOwnedByUser(otherUserId, recipe.id))
    }
}
