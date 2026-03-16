package io.github.fgrutsch.cookmaid.ui.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.ui.catalog.CatalogItemRepository
import io.github.fgrutsch.cookmaid.recipe.Recipe
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.recipe.RecipeRepository
import io.github.fgrutsch.cookmaid.recipe.TagRepository
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
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AddRecipeViewModel(
    private val recipeRepository: RecipeRepository,
    private val tagRepository: TagRepository,
    private val catalogItemRepository: CatalogItemRepository,
    private val editRecipeId: String? = null,
) : ViewModel() {

    val isEditing = editRecipeId != null

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _nameTouched = MutableStateFlow(false)

    val nameError: StateFlow<Boolean> = combine(_name, _nameTouched) { name, touched ->
        touched && name.isBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _ingredients = MutableStateFlow<List<RecipeIngredient>>(emptyList())
    val ingredients: StateFlow<List<RecipeIngredient>> = _ingredients.asStateFlow()

    private val _steps = MutableStateFlow<List<String>>(emptyList())
    val steps: StateFlow<List<String>> = _steps.asStateFlow()

    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())
    val selectedTags: StateFlow<List<String>> = _selectedTags.asStateFlow()

    val availableTags: StateFlow<List<String>> = tagRepository.tags

    private val _ingredientQuery = MutableStateFlow("")
    val ingredientQuery: StateFlow<String> = _ingredientQuery.asStateFlow()

    val ingredientSuggestions: StateFlow<List<Item.CatalogItem>> = _ingredientQuery
        .debounce(150)
        .flatMapLatest { query ->
            flow { emit(catalogItemRepository.search(query)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (editRecipeId != null) {
            viewModelScope.launch {
                recipeRepository.getById(editRecipeId)?.let { recipe ->
                    _name.value = recipe.name
                    _ingredients.value = recipe.ingredients
                    _steps.value = recipe.steps
                    _selectedTags.value = recipe.tags
                }
            }
        }
    }

    fun setName(value: String) {
        _name.value = value
        if (!_nameTouched.value && value.isNotEmpty()) _nameTouched.value = true
    }

    fun updateIngredientQuery(query: String) {
        _ingredientQuery.value = query
    }

    fun addIngredient(item: Item, quantity: Float? = null) {
        if (item.name.isBlank()) return
        _ingredients.value = _ingredients.value + RecipeIngredient(item, quantity)
        _ingredientQuery.value = ""
    }

    fun updateIngredientQuantity(index: Int, quantity: Float?) {
        _ingredients.value = _ingredients.value.mapIndexed { i, ingredient ->
            if (i == index) ingredient.copy(quantity = quantity) else ingredient
        }
    }

    fun removeIngredient(index: Int) {
        _ingredients.value = _ingredients.value.filterIndexed { i, _ -> i != index }
    }

    fun addStep(step: String) {
        if (step.isBlank()) return
        _steps.value = _steps.value + step.trim()
    }

    fun removeStep(index: Int) {
        _steps.value = _steps.value.filterIndexed { i, _ -> i != index }
    }

    fun toggleTag(tag: String) {
        _selectedTags.value = if (tag in _selectedTags.value) {
            _selectedTags.value - tag
        } else {
            _selectedTags.value + tag
        }
    }

    fun createAndAddTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            tagRepository.add(trimmed)
        }
        if (trimmed !in _selectedTags.value) {
            _selectedTags.value = _selectedTags.value + trimmed
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun save(): Boolean {
        _nameTouched.value = true
        if (_name.value.isBlank()) return false
        val recipe = Recipe(
            id = editRecipeId ?: Uuid.random().toString(),
            name = _name.value.trim(),
            ingredients = _ingredients.value,
            steps = _steps.value,
            tags = _selectedTags.value,
        )
        viewModelScope.launch {
            if (isEditing) recipeRepository.update(recipe) else recipeRepository.add(recipe)
        }
        return true
    }
}
