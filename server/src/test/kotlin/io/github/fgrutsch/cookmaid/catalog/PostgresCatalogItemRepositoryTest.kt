package io.github.fgrutsch.cookmaid.catalog

import io.github.fgrutsch.cookmaid.common.SupportedLocale
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

        val items = repository.findAll(SupportedLocale.EN)

        assertTrue(items.size > 100)
    }

    @Test
    fun `findAll returns items with category names`() = runTest {
        val repository = getKoin().get<CatalogItemRepository>()

        val items = repository.findAll(SupportedLocale.EN)
        val apple = items.find { it.name == "Apples" }

        assertNotNull(apple)
        assertEquals("Fruits", apple.category.name)
    }

    @Test
    fun `findById returns item when it exists`() = runTest {
        val repository = getKoin().get<CatalogItemRepository>()
        val allItems = repository.findAll(SupportedLocale.EN)
        val first = allItems.first()

        val found = repository.findById(first.id, SupportedLocale.EN)

        assertNotNull(found)
        assertEquals(first.name, found.name)
        assertEquals(first.category.name, found.category.name)
    }

    @Test
    fun `findById returns null when item does not exist`() = runTest {
        val repository = getKoin().get<CatalogItemRepository>()

        val found = repository.findById(Uuid.random(), SupportedLocale.EN)

        assertNull(found)
    }
}
