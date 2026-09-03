package io.github.fgrutsch.cookmaid.ui.recipe.edit

import androidx.lifecycle.viewModelScope
import io.github.fgrutsch.cookmaid.catalog.Item
import io.github.fgrutsch.cookmaid.recipe.RecipeIngredient
import io.github.fgrutsch.cookmaid.ui.catalog.CatalogItemRepository
import io.github.fgrutsch.cookmaid.ui.common.MviViewModel
import io.github.fgrutsch.cookmaid.ui.recipe.RecipeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AddRecipeViewModel(
    private val recipeRepository: RecipeRepository,
    private val catalogItemRepository: CatalogItemRepository,
    private val editRecipeId: Uuid? = null,
) : MviViewModel<AddRecipeState, AddRecipeEvent, AddRecipeEffect>(
    AddRecipeState(isEditing = editRecipeId != null),
) {

    private val ingredientQueryFlow = MutableStateFlow("")

    init {
        ingredientQueryFlow
            .debounce(150.milliseconds)
            .flatMapLatest { query -> flow { emit(catalogItemRepository.search(query)) } }
            .onEach { results -> updateState { copy(ingredientSuggestions = results) } }
            .launchIn(viewModelScope)
    }

    // A flat dispatcher: its complexity is the number of event types and nothing else, with no
    // nested logic to untangle. Splitting it would cost the exhaustive `when` that makes adding
    // an event a compile error rather than a silent no-op.
    @Suppress("CyclomaticComplexMethod")
    override fun handleEvent(event: AddRecipeEvent) {
        when (event) {
            is AddRecipeEvent.Load -> load()
            is AddRecipeEvent.SetName -> setName(event.value)
            is AddRecipeEvent.SetDescription -> updateState { copy(description = event.value) }
            is AddRecipeEvent.UpdateIngredientQuery -> updateIngredientQuery(event.query)
            is AddRecipeEvent.AddIngredient -> addIngredient(event.item, event.quantity)
            is AddRecipeEvent.AddIngredientByName -> addIngredientByName(event.name, event.quantity)
            is AddRecipeEvent.UpdateIngredientQuantity -> updateIngredientQuantity(event.index, event.quantity)
            is AddRecipeEvent.UpdateIngredientName -> updateIngredientName(event.index, event.name)
            is AddRecipeEvent.SetServings -> updateState { copy(servings = event.value) }
            is AddRecipeEvent.RemoveIngredient -> updateState {
                copy(ingredients = ingredients.filterIndexed { i, _ -> i != event.index })
            }
            is AddRecipeEvent.AddStep -> addStep(event.step)
            is AddRecipeEvent.UpdateStep -> updateState {
                copy(steps = steps.mapIndexed { i, s -> if (i == event.index) event.step else s })
            }
            is AddRecipeEvent.RemoveStep -> updateState {
                copy(steps = steps.filterIndexed { i, _ -> i != event.index })
            }
            is AddRecipeEvent.ToggleTag -> toggleTag(event.tag)
            is AddRecipeEvent.CreateAndAddTag -> createAndAddTag(event.tag)
            is AddRecipeEvent.Save -> save()
        }
    }

    private fun load() {
        launch {
            updateState { copy(isLoading = true) }
            val tags = recipeRepository.fetchTags()
            updateState { copy(availableTags = tags) }

            if (editRecipeId != null) {
                recipeRepository.getById(editRecipeId)?.let { recipe ->
                    updateState {
                        copy(
                            name = recipe.name,
                            description = recipe.description.orEmpty(),
                            ingredients = recipe.ingredients,
                            steps = recipe.steps,
                            selectedTags = recipe.tags,
                            servings = recipe.servings,
                        )
                    }
                }
            }
            updateState { copy(isLoading = false) }
        }
    }

    private fun setName(value: String) {
        updateState { copy(name = value, nameError = false) }
    }

    private fun updateIngredientQuery(query: String) {
        updateState { copy(ingredientQuery = query) }
        ingredientQueryFlow.value = query
    }

    private fun addIngredient(item: Item, quantity: String?) {
        if (item.name.isBlank()) return
        updateState {
            copy(
                ingredients = ingredients + RecipeIngredient(item, quantity),
                ingredientQuery = "",
                ingredientSuggestions = emptyList(),
            )
        }
        ingredientQueryFlow.value = ""
    }

    private fun addIngredientByName(name: String, quantity: String?) {
        if (name.isBlank()) return
        updateState { copy(ingredientQuery = "", ingredientSuggestions = emptyList()) }
        ingredientQueryFlow.value = ""
        launch {
            val resolved = catalogItemRepository.findExactMatch(name) ?: Item.FreeText(name = name.trim())
            updateState { copy(ingredients = ingredients + RecipeIngredient(resolved, quantity)) }
        }
    }

    private fun updateIngredientQuantity(index: Int, quantity: String?) {
        updateState {
            copy(ingredients = ingredients.mapIndexed { i, ing ->
                if (i == index) ing.copy(quantity = quantity) else ing
            })
        }
    }

    /**
     * Renames the free-text ingredient at [index].
     *
     * Guarded to free-text items: a catalog ingredient's name is a localised reference to a
     * catalog row, so overwriting it with typed text would silently detach the row from the
     * catalog and lose its category.
     */
    private fun updateIngredientName(index: Int, name: String) {
        updateState {
            copy(ingredients = ingredients.mapIndexed { i, ing ->
                if (i == index && ing.item is Item.FreeText) ing.copy(item = Item.FreeText(name)) else ing
            })
        }
    }

    private fun addStep(step: String) {
        if (step.isBlank()) return
        updateState { copy(steps = steps + step.trim()) }
    }

    private fun toggleTag(tag: String) {
        updateState {
            val updated = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
            copy(selectedTags = updated)
        }
    }

    private fun createAndAddTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isBlank()) return
        updateState {
            copy(
                availableTags = if (trimmed !in availableTags) (availableTags + trimmed).sorted() else availableTags,
                selectedTags = if (trimmed !in selectedTags) selectedTags + trimmed else selectedTags,
            )
        }
    }

    private fun save() {
        if (state.value.name.isBlank()) {
            updateState { copy(nameError = true) }
            return
        }
        val s = state.value
        val description = s.description.trim().ifBlank { null }
        launch {
            if (s.isEditing) {
                recipeRepository.update(
                    requireNotNull(editRecipeId),
                    s.name.trim(),
                    description,
                    s.ingredients,
                    s.steps,
                    s.selectedTags,
                    s.servings,
                )
            } else {
                recipeRepository.create(
                    s.name.trim(),
                    description,
                    s.ingredients,
                    s.steps,
                    s.selectedTags,
                    s.servings,
                )
            }
            sendEffect(AddRecipeEffect.Saved)
        }
    }

    override fun onError(e: Exception) {
        updateState { copy(isLoading = false) }
        sendEffect(AddRecipeEffect.Error)
    }

}
