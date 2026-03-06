package io.github.fgrutsch.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.catalog.CatalogRepository
import io.github.fgrutsch.catalog.Item
import io.github.fgrutsch.catalog.ItemCategoryRepository
import io.github.fgrutsch.shopping.ShoppingItem
import io.github.fgrutsch.shopping.ShoppingList
import io.github.fgrutsch.shopping.ShoppingListRepository
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ShoppingListViewModel(
    private val repository: ShoppingListRepository,
    private val catalogRepository: CatalogRepository,
    private val itemCategoryRepository: ItemCategoryRepository,
) : ViewModel() {

    val lists: StateFlow<List<ShoppingList>> = repository.lists

    private val _selectedListId = MutableStateFlow("")
    val selectedListId: StateFlow<String> = _selectedListId.asStateFlow()

    val selectedList: StateFlow<ShoppingList?> = combine(lists, _selectedListId) { lists, id ->
        lists.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categoryNames: StateFlow<Map<String, String>> = itemCategoryRepository.categories.map { categories ->
        categories.associate { it.id to it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val suggestions: StateFlow<List<Item.CategorizedItem>> = _searchQuery
        .debounce(150)
        .flatMapLatest { query ->
            flow { emit(catalogRepository.search(query)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        lists.onEach { allLists ->
            if (_selectedListId.value.isEmpty() || allLists.none { it.id == _selectedListId.value }) {
                _selectedListId.value = allLists.firstOrNull()?.id ?: ""
            }
        }.launchIn(viewModelScope)
    }

    fun selectList(listId: String) {
        _selectedListId.value = listId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addItem(item: Item) {
        if (item.name.isBlank()) return
        val listId = _selectedListId.value
        viewModelScope.launch {
            repository.addItem(
                listId,
                ShoppingItem(
                    id = Uuid.random().toString(),
                    item = item,
                    quantity = null,
                ),
            )
        }
        clearSearch()
    }

    fun updateItem(item: ShoppingItem) {
        viewModelScope.launch {
            repository.updateItem(_selectedListId.value, item)
        }
    }

    fun toggleChecked(id: String) {
        viewModelScope.launch {
            repository.toggleChecked(_selectedListId.value, id)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deleteItem(_selectedListId.value, id)
        }
    }

    fun deleteChecked() {
        viewModelScope.launch {
            repository.deleteChecked(_selectedListId.value)
        }
    }

    fun addList(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newList = repository.addList(name)
            _selectedListId.value = newList.id
        }
    }

    fun renameList(id: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameList(id, newName)
        }
    }

    fun deleteList(id: String) {
        viewModelScope.launch {
            repository.deleteList(id)
            if (_selectedListId.value == id) {
                _selectedListId.value = repository.lists.value.firstOrNull()?.id ?: ""
            }
        }
    }
}
