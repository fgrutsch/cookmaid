package io.github.fgrutsch.ui.shopping

import io.github.fgrutsch.catalog.Item
import io.github.fgrutsch.shopping.ShoppingItem
import io.github.fgrutsch.ui.catalog.CatalogItemRepository
import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.ui.common.MviViewModel
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
            .debounce(150)
            .flatMapLatest { query ->
                flow { emit(catalogItemRepository.search(query)) }
            }
            .onEach { results -> updateState { copy(suggestions = results) } }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: ShoppingListEvent) {
        when (event) {
            is ShoppingListEvent.LoadLists -> loadLists()
            is ShoppingListEvent.SelectList -> selectList(event.listId)
            is ShoppingListEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is ShoppingListEvent.ClearSearch -> clearSearch()
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

    private fun loadLists() {
        launch {
            updateState { copy(isLoading = true) }
            val loaded = repository.getLists(refresh = true)
            val currentSelectedId = state.value.selectedListId
            val selectedId = if (currentSelectedId == null || loaded.none { it.id == currentSelectedId }) {
                (loaded.find { it.default } ?: loaded.firstOrNull())?.id
            } else {
                currentSelectedId
            }
            updateState { copy(lists = loaded, isLoading = false) }
            selectList(selectedId)
        }
    }

    private fun selectList(listId: Uuid?) {
        updateState { copy(selectedListId = listId) }
        if (listId != null) {
            launch {
                updateState { copy(isLoading = true) }
                val items = repository.loadItems(listId)
                updateState { copy(items = items, isLoading = false) }
            }
        } else {
            updateState { copy(items = emptyList()) }
        }
    }

    private fun updateSearchQuery(query: String) {
        updateState { copy(searchQuery = query) }
        searchQueryFlow.update { query }
    }

    private fun clearSearch() {
        updateState { copy(searchQuery = "", suggestions = emptyList()) }
        searchQueryFlow.update { "" }
    }

    private fun addItem(item: Item) {
        if (item.name.isBlank()) return
        val listId = state.value.selectedListId ?: return
        clearSearch()
        launch {
            val catalogItemId = (item as? Item.CatalogItem)?.id
            val freeTextName = (item as? Item.FreeTextItem)?.name
            val created = repository.addItem(listId, catalogItemId, freeTextName, null)
            updateState { copy(items = items + created) }
        }
    }

    private fun updateItem(item: ShoppingItem) {
        val listId = state.value.selectedListId ?: return
        updateState { copy(items = items.map { if (it.id == item.id) item else it }) }
        launch {
            repository.updateItem(listId, item.id, item.quantity, item.checked)
        }
    }

    private fun toggleChecked(id: Uuid) {
        val listId = state.value.selectedListId ?: return
        val item = state.value.items.find { it.id == id } ?: return
        val toggled = item.copy(checked = !item.checked)
        updateState { copy(items = items.map { if (it.id == id) toggled else it }) }
        launch {
            repository.updateItem(listId, id, toggled.quantity, toggled.checked)
        }
    }

    private fun deleteItem(id: Uuid) {
        val listId = state.value.selectedListId ?: return
        updateState { copy(items = items.filter { it.id != id }) }
        launch {
            repository.deleteItem(listId, id)
        }
    }

    private fun deleteChecked() {
        val listId = state.value.selectedListId ?: return
        updateState { copy(items = items.filter { !it.checked }) }
        launch {
            repository.deleteCheckedItems(listId)
        }
    }

    private fun createList(name: String) {
        if (name.isBlank()) return
        launch {
            val newList = repository.createList(name)
            val lists = repository.getLists()
            updateState { copy(lists = lists) }
            selectList(newList.id)
        }
    }

    private fun renameList(id: Uuid, newName: String) {
        if (newName.isBlank()) return
        launch {
            repository.updateList(id, newName)
            val lists = repository.getLists()
            updateState { copy(lists = lists) }
        }
    }

    private fun deleteList(id: Uuid) {
        launch {
            repository.deleteList(id)
            val updatedLists = repository.getLists()
            updateState { copy(lists = updatedLists) }
            if (state.value.selectedListId == id) {
                val fallback = updatedLists.find { it.default }?.id ?: updatedLists.firstOrNull()?.id
                selectList(fallback)
            }
        }
    }
}
