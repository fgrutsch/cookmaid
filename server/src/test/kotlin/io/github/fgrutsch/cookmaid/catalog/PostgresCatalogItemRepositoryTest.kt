package io.github.fgrutsch.cookmaid.catalog

import io.github.fgrutsch.cookmaid.support.BaseTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class PostgresCatalogItemRepositoryTest : BaseTest() {

    @Test
    fun `findAll returns all seeded items`() = runTest {
        val repository = getKoin().get<CatalogItemRepository>()

        val items = repository.findAll()

        assertTrue(items.size > 100)
    }

    @Test
    fun `findAll returns items with category names`() = runTest {
        val repository = getKoin().get<CatalogItemRepository>()

        val items = repository.findAll()
        val apple = items.find { it.name == "Apples" }

        assertNotNull(apple)
        assertEquals("Fruits", apple.category.name)
    }

    @Test
    fun `findById returns item when it exists`() = runTest {
        val repository = getKoin().get<CatalogItemRepository>()
        val allItems = repository.findAll()
        val first = allItems.first()

        val found = repository.findById(first.id)

        assertNotNull(found)
        assertEquals(first.name, found.name)
        assertEquals(first.category.name, found.category.name)
    }

    @Test
    fun `findById returns null when item does not exist`() = runTest {
        val repository = getKoin().get<CatalogItemRepository>()

        val found = repository.findById(Uuid.random())

        assertNull(found)
    }
}
