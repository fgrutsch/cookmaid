package io.github.fgrutsch.cookmaid.ui.shopping

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.catalog.ItemCategory
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import io.github.fgrutsch.cookmaid.support.BaseViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest : BaseViewModelTest() {

    private val fakeRepo = FakeShoppingListRepository()
    private val fakeCatalog = FakeCatalogItemRepository()

    private fun TestScope.createLoadedViewModel(
        lists: List<ShoppingList> = listOf(ShoppingList(id = LIST_ID, name = "Groceries", default = true)),
        items: List<ShoppingItem> = emptyList(),
    ): ShoppingListViewModel {
        fakeRepo.lists = lists.toMutableList()
        if (items.isNotEmpty()) {
            fakeRepo.itemsByList[lists.first().id] = items.toMutableList()
        }
        val viewModel = ShoppingListViewModel(fakeRepo, fakeCatalog)
        viewModel.onEvent(ShoppingListEvent.LoadLists)
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `load lists selects default list`() = viewModelTest {
        val viewModel = createLoadedViewModel()

        assertEquals(1, viewModel.state.value.lists.size)
        assertEquals(LIST_ID, viewModel.state.value.selectedListId)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `load lists with no lists results in empty state`() = viewModelTest {
        val viewModel = createLoadedViewModel(lists = emptyList())

        assertTrue(viewModel.state.value.lists.isEmpty())
        assertNull(viewModel.state.value.selectedListId)
    }

    @Test
    fun `select list loads items`() = viewModelTest {
        val secondListId = Uuid.random()
        val lists = listOf(
            ShoppingList(id = LIST_ID, name = "Groceries", default = true),
            ShoppingList(id = secondListId, name = "Hardware"),
        )
        val item = ShoppingItem(id = Uuid.random(), item = Item.FreeText("Nails"), quantity = null)
        fakeRepo.itemsByList[secondListId] = mutableListOf(item)
        val viewModel = createLoadedViewModel(lists = lists)

        viewModel.onEvent(ShoppingListEvent.SelectList(secondListId))
        advanceUntilIdle()

        assertEquals(secondListId, viewModel.state.value.selectedListId)
        assertEquals(1, viewModel.state.value.items.size)
    }

    @Test
    fun `add free text item appends to list`() = viewModelTest {
        val viewModel = createLoadedViewModel()

        viewModel.onEvent(ShoppingListEvent.AddItem(Item.FreeText("Milk")))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals("Milk", viewModel.state.value.items.first().item.name)
    }

    @Test
    fun `add blank item is ignored`() = viewModelTest {
        val viewModel = createLoadedViewModel()

        viewModel.onEvent(ShoppingListEvent.AddItem(Item.FreeText("  ")))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.items.isEmpty())
    }

    @Test
    fun `toggle checked flips item state`() = viewModelTest {
        val itemId = Uuid.random()
        val item = ShoppingItem(id = itemId, item = Item.FreeText("Eggs"), quantity = null, checked = false)
        val viewModel = createLoadedViewModel(items = listOf(item))

        viewModel.onEvent(ShoppingListEvent.ToggleChecked(itemId))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.items.first().checked)
    }

    @Test
    fun `toggle checked twice restores original state`() = viewModelTest {
        val itemId = Uuid.random()
        val item = ShoppingItem(id = itemId, item = Item.FreeText("Eggs"), quantity = null, checked = false)
        val viewModel = createLoadedViewModel(items = listOf(item))

        viewModel.onEvent(ShoppingListEvent.ToggleChecked(itemId))
        viewModel.onEvent(ShoppingListEvent.ToggleChecked(itemId))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.items.first().checked)
    }

    @Test
    fun `delete item removes from list`() = viewModelTest {
        val itemId = Uuid.random()
        val item = ShoppingItem(id = itemId, item = Item.FreeText("Bread"), quantity = null)
        val viewModel = createLoadedViewModel(items = listOf(item))

        viewModel.onEvent(ShoppingListEvent.DeleteItem(itemId))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.items.isEmpty())
    }

    @Test
    fun `delete checked removes only checked items`() = viewModelTest {
        val unchecked = ShoppingItem(id = Uuid.random(), item = Item.FreeText("Milk"), quantity = null, checked = false)
        val checked = ShoppingItem(id = Uuid.random(), item = Item.FreeText("Eggs"), quantity = null, checked = true)
        val viewModel = createLoadedViewModel(items = listOf(unchecked, checked))

        viewModel.onEvent(ShoppingListEvent.DeleteChecked)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals("Milk", viewModel.state.value.items.first().item.name)
    }

    @Test
    fun `update item replaces in list`() = viewModelTest {
        val itemId = Uuid.random()
        val item = ShoppingItem(id = itemId, item = Item.FreeText("Milk"), quantity = null)
        val viewModel = createLoadedViewModel(items = listOf(item))

        val updated = item.copy(quantity = "2")
        viewModel.onEvent(ShoppingListEvent.UpdateItem(updated))
        advanceUntilIdle()

        assertEquals("2", viewModel.state.value.items.first().quantity)
    }

    @Test
    fun `create list adds and selects it`() = viewModelTest {
        val viewModel = createLoadedViewModel()

        viewModel.onEvent(ShoppingListEvent.CreateList("Party"))
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.lists.size)
        assertEquals("Party", viewModel.state.value.selectedList?.name)
    }

    @Test
    fun `create blank list is ignored`() = viewModelTest {
        val viewModel = createLoadedViewModel()

        viewModel.onEvent(ShoppingListEvent.CreateList("  "))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.lists.size)
    }

    @Test
    fun `rename list updates name`() = viewModelTest {
        val viewModel = createLoadedViewModel()

        viewModel.onEvent(ShoppingListEvent.RenameList(LIST_ID, "Weekly"))
        advanceUntilIdle()

        assertEquals("Weekly", viewModel.state.value.lists.first().name)
    }

    @Test
    fun `delete list falls back to default`() = viewModelTest {
        val defaultList = ShoppingList(id = LIST_ID, name = "Groceries", default = true)
        val secondId = Uuid.random()
        val secondList = ShoppingList(id = secondId, name = "Party")
        val viewModel = createLoadedViewModel(lists = listOf(defaultList, secondList))

        viewModel.onEvent(ShoppingListEvent.SelectList(secondId))
        advanceUntilIdle()

        viewModel.onEvent(ShoppingListEvent.DeleteList(secondId))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.lists.size)
        assertEquals(LIST_ID, viewModel.state.value.selectedListId)
    }

    @Test
    fun `unchecked and checked items are partitioned correctly`() = viewModelTest {
        val items = listOf(
            ShoppingItem(id = Uuid.random(), item = Item.FreeText("Milk"), quantity = null, checked = false),
            ShoppingItem(id = Uuid.random(), item = Item.FreeText("Eggs"), quantity = null, checked = true),
            ShoppingItem(id = Uuid.random(), item = Item.FreeText("Bread"), quantity = null, checked = false),
        )
        val viewModel = createLoadedViewModel(items = items)

        assertEquals(2, viewModel.state.value.uncheckedItems.size)
        assertEquals(1, viewModel.state.value.checkedItems.size)
    }

    @Test
    fun `add item by name resolves to catalog item on exact match`() = viewModelTest {
        val catalogItem = Item.Catalog(
            id = Uuid.random(),
            name = "Milk",
            category = ItemCategory(id = Uuid.random(), name = "Dairy"),
        )
        fakeCatalog.items = listOf(catalogItem)
        val viewModel = createLoadedViewModel()

        viewModel.onEvent(ShoppingListEvent.AddItemByName("Milk"))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(catalogItem.id, fakeRepo.lastAddedCatalogItemId)
        assertNull(fakeRepo.lastAddedFreeTextName)
    }

    @Test
    fun `add item by name case-insensitive resolves to catalog item`() = viewModelTest {
        val catalogItem = Item.Catalog(
            id = Uuid.random(),
            name = "Milk",
            category = ItemCategory(id = Uuid.random(), name = "Dairy"),
        )
        fakeCatalog.items = listOf(catalogItem)
        val viewModel = createLoadedViewModel()

        viewModel.onEvent(ShoppingListEvent.AddItemByName("milk"))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(catalogItem.id, fakeRepo.lastAddedCatalogItemId)
    }

    @Test
    fun `add item by name falls back to free text when no match`() = viewModelTest {
        fakeCatalog.items = emptyList()
        val viewModel = createLoadedViewModel()

        viewModel.onEvent(ShoppingListEvent.AddItemByName("Custom Item"))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.items.size)
        assertNull(fakeRepo.lastAddedCatalogItemId)
        assertEquals("Custom Item", fakeRepo.lastAddedFreeTextName)
    }

    @Test
    fun `add item by name with blank name is ignored`() = viewModelTest {
        val viewModel = createLoadedViewModel()

        viewModel.onEvent(ShoppingListEvent.AddItemByName("  "))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.items.isEmpty())
    }

    @Test
    fun `add item by name clears search query`() = viewModelTest {
        fakeCatalog.items = emptyList()
        val viewModel = createLoadedViewModel()

        viewModel.onEvent(ShoppingListEvent.UpdateSearchQuery("test"))
        advanceUntilIdle()

        viewModel.onEvent(ShoppingListEvent.AddItemByName("test"))
        advanceUntilIdle()

        assertEquals("", viewModel.state.value.searchQuery)
    }

    companion object {
        private val LIST_ID = Uuid.parse("00000000-0000-0000-0000-000000000001")
    }
}
