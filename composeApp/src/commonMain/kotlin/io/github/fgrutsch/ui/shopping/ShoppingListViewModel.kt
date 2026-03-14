package io.github.fgrutsch.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.catalog.Item
import io.github.fgrutsch.shopping.ShoppingItem
import io.github.fgrutsch.shopping.ShoppingList
import io.github.fgrutsch.ui.catalog.CatalogItemRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ShoppingListViewModel(
    private val repository: ShoppingListRepository,
    private val catalogItemRepository: CatalogItemRepository,
) : ViewModel() {

    private val _lists = MutableStateFlow<List<ShoppingList>>(emptyList())
    val lists: StateFlow<List<ShoppingList>> = _lists.asStateFlow()

    private val _selectedListId = MutableStateFlow<Uuid?>(null)
    val selectedListId: StateFlow<Uuid?> = _selectedListId.asStateFlow()

    val selectedList: StateFlow<ShoppingList?> =
        combine(_lists, _selectedListId) { lists, id ->
            lists.find { it.id == id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _items = MutableStateFlow<List<ShoppingItem>>(emptyList())
    val items: StateFlow<List<ShoppingItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val suggestions: StateFlow<List<Item.CatalogItem>> = _searchQuery
        .debounce(150)
        .flatMapLatest { query ->
            flow { emit(catalogItemRepository.search(query)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadLists() {
        viewModelScope.launch {
            _isLoading.value = true
            val loaded = repository.loadLists()
            _lists.value = loaded
            if (_selectedListId.value == null || loaded.none { it.id == _selectedListId.value }) {
                val defaultList = loaded.find { it.default } ?: loaded.firstOrNull()
                selectList(defaultList?.id)
            }
            _isLoading.value = false
        }
    }

    fun selectList(listId: Uuid?) {
        _selectedListId.value = listId
        if (listId != null) {
            viewModelScope.launch {
                _isLoading.value = true
                _items.value = repository.loadItems(listId)
                _isLoading.value = false
            }
        } else {
            _items.value = emptyList()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun addItem(item: Item) {
        if (item.name.isBlank()) return
        val listId = _selectedListId.value ?: return
        viewModelScope.launch {
            val catalogItemId = (item as? Item.CatalogItem)?.id
            val freeTextName = (item as? Item.FreeTextItem)?.name
            val created = repository.addItem(listId, catalogItemId, freeTextName, null)
            _items.update { it + created }
        }
        clearSearch()
    }

    fun updateItem(item: ShoppingItem) {
        val listId = _selectedListId.value ?: return
        _items.update { items ->
            items.map { if (it.id == item.id) item else it }
        }
        viewModelScope.launch {
            repository.updateItem(listId, item.id, item.quantity, item.checked)
        }
    }

    fun toggleChecked(id: Uuid) {
        val listId = _selectedListId.value ?: return
        val item = _items.value.find { it.id == id } ?: return
        val toggled = item.copy(checked = !item.checked)
        _items.update { items ->
            items.map { if (it.id == id) toggled else it }
        }
        viewModelScope.launch {
            repository.updateItem(listId, id, toggled.quantity, toggled.checked)
        }
    }

    fun deleteItem(id: Uuid) {
        val listId = _selectedListId.value ?: return
        _items.update { items -> items.filter { it.id != id } }
        viewModelScope.launch {
            repository.deleteItem(listId, id)
        }
    }

    fun deleteChecked() {
        val listId = _selectedListId.value ?: return
        _items.update { items -> items.filter { !it.checked } }
        viewModelScope.launch {
            repository.deleteCheckedItems(listId)
        }
    }

    fun createList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newList = repository.createList(name)
            _lists.value = repository.cachedLists
            selectList(newList.id)
        }
    }

    fun renameList(id: Uuid, newName: String) {
        if (newName.isBlank()) return
        val list = _lists.value.find { it.id == id } ?: return
        viewModelScope.launch {
            repository.updateList(id, newName)
            _lists.value = repository.cachedLists
        }
    }

    fun deleteList(id: Uuid) {
        viewModelScope.launch {
            repository.deleteList(id)
            _lists.value = repository.cachedLists
            if (_selectedListId.value == id) {
                val defaultList = _lists.value.find { it.default }
                selectList(defaultList?.id ?: _lists.value.firstOrNull()?.id)
            }
        }
    }
}
