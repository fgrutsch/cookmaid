package io.github.fgrutsch.cookmaid.ui.shopping

import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.shopping.ShoppingItem
import io.github.fgrutsch.cookmaid.shopping.ShoppingList
import io.github.fgrutsch.cookmaid.ui.catalog.CatalogItemRepository
import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.cookmaid.ui.common.MviViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ShoppingListViewModel(
    private val repository: ShoppingListRepository,
    private val catalogItemRepository: CatalogItemRepository,
) : MviViewModel<ShoppingListState, ShoppingListEvent, ShoppingListEffect>(ShoppingListState()) {

    private val searchQueryFlow = MutableStateFlow("")

    init {
        searchQueryFlow
            .debounce(SEARCH_DEBOUNCE_MILLIS)
            .flatMapLatest { query ->
                flow { emit(catalogItemRepository.search(query)) }
            }
            .onEach { results -> updateState { copy(suggestions = results) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: ShoppingListEvent) {
        when (event) {
            is ShoppingListEvent.LoadLists -> fetchLists(isRefresh = false)
            is ShoppingListEvent.Refresh -> fetchLists(isRefresh = true)
            is ShoppingListEvent.SelectList -> loadItems(event.listId)
            is ShoppingListEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is ShoppingListEvent.ClearSearch -> updateSearchQuery("")
            is ShoppingListEvent.AddItem -> addItem(event.item)
            is ShoppingListEvent.UpdateItem -> updateItem(event.item)
            is ShoppingListEvent.ToggleChecked -> toggleChecked(event.itemId)
            is ShoppingListEvent.DeleteItem -> deleteItem(event.itemId)
            is ShoppingListEvent.DeleteChecked -> deleteChecked()
            is ShoppingListEvent.CreateList -> createList(event.name)
            is ShoppingListEvent.RenameList -> renameList(event.listId, event.newName)
            is ShoppingListEvent.DeleteList -> deleteList(event.listId)
        }
    }

    private fun fetchLists(isRefresh: Boolean) {
        launch {
            if (isRefresh) {
                updateState { copy(isRefreshing = true) }
            } else if (!state.value.initialized) {
                updateState { copy(isLoading = true) }
            }
            val loaded = repository.getLists(refresh = true)
            val selectedId = resolveSelectedListId(loaded)
            updateState {
                copy(
                    initialized = true,
                    lists = loaded,
                    isLoading = false,
                    isRefreshing = false,
                )
            }
            loadItems(selectedId)
        }
    }

    private fun loadItems(listId: Uuid?) {
        updateState { copy(selectedListId = listId) }
        if (listId != null) {
            launch {
                val items = repository.loadItems(listId)
                updateState { copy(items = items) }
            }
        } else {
            updateState { copy(items = emptyList()) }
        }
    }

    private fun resolveSelectedListId(lists: List<ShoppingList>): Uuid? {
        val currentSelectedId = state.value.selectedListId
        return if (currentSelectedId != null && lists.any { it.id == currentSelectedId }) {
            currentSelectedId
        } else {
            (lists.find { it.default } ?: lists.firstOrNull())?.id
        }
    }

    private fun updateSearchQuery(query: String) {
        updateState { copy(searchQuery = query) }
        searchQueryFlow.update { query }
    }

    private fun addItem(item: Item) {
        if (item.name.isBlank()) return
        val listId = state.value.selectedListId ?: return
        updateSearchQuery("")
        launch {
            val catalogItemId = (item as? Item.CatalogItem)?.id
            val freeTextName = (item as? Item.FreeTextItem)?.name
            val created = repository.addItem(listId, catalogItemId, freeTextName, null)
            updateState { copy(items = items + created) }
        }
    }

    private fun updateItem(item: ShoppingItem) {
        val listId = state.value.selectedListId ?: return
        launchOptimistic(
            optimisticUpdate = { copy(items = items.map { if (it.id == item.id) item else it }) },
        ) {
            repository.updateItem(listId, item.id, item.quantity, item.checked)
        }
    }

    private fun toggleChecked(id: Uuid) {
        val listId = state.value.selectedListId ?: return
        val item = state.value.items.find { it.id == id } ?: return
        val toggled = item.copy(checked = !item.checked)
        launchOptimistic(
            optimisticUpdate = { copy(items = items.map { if (it.id == id) toggled else it }) },
        ) {
            repository.updateItem(listId, id, toggled.quantity, toggled.checked)
        }
    }

    private fun deleteItem(id: Uuid) {
        val listId = state.value.selectedListId ?: return
        launchOptimistic(
            optimisticUpdate = { copy(items = items.filter { it.id != id }) },
        ) {
            repository.deleteItem(listId, id)
        }
    }

    private fun deleteChecked() {
        val listId = state.value.selectedListId ?: return
        launchOptimistic(
            optimisticUpdate = { copy(items = items.filter { !it.checked }) },
        ) {
            repository.deleteCheckedItems(listId)
        }
    }

    private fun createList(name: String) {
        if (name.isBlank()) return
        launch {
            val newList = repository.createList(name)
            updateState { copy(lists = lists + newList) }
            loadItems(newList.id)
        }
    }

    private fun renameList(id: Uuid, newName: String) {
        if (newName.isBlank()) return
        launchOptimistic(
            optimisticUpdate = { copy(lists = lists.map { if (it.id == id) it.copy(name = newName.trim()) else it }) },
        ) {
            repository.updateList(id, newName)
        }
    }

    private fun deleteList(id: Uuid) {
        val fallback = state.value.lists.filter { it.id != id }.let { remaining ->
            remaining.find { it.default }?.id ?: remaining.firstOrNull()?.id
        }
        launchOptimistic(
            optimisticUpdate = { copy(lists = lists.filter { it.id != id }) },
        ) {
            repository.deleteList(id)
            if (state.value.selectedListId == id) {
                loadItems(fallback)
            }
        }
    }

    override fun onError(e: Exception) {
        updateState { copy(isLoading = false, isRefreshing = false) }
        sendEffect(ShoppingListEffect.Error("Something went wrong. Please try again."))
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 150L
    }
}
