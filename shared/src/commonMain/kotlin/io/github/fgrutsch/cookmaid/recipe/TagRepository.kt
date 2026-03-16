package io.github.fgrutsch.cookmaid.recipe

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface TagRepository {
    val tags: StateFlow<List<String>>
    suspend fun add(tag: String)
    suspend fun delete(tag: String)
}

class InMemoryTagRepository : TagRepository {
    private val _tags = MutableStateFlow(
        listOf("Breakfast", "Meat", "Noodles", "Salad", "Vegetarian"),
    )
    override val tags: StateFlow<List<String>> = _tags.asStateFlow()

    override suspend fun add(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isBlank()) return
        _tags.update { current ->
            if (current.any { it.equals(trimmed, ignoreCase = true) }) current
            else (current + trimmed).sorted()
        }
    }

    override suspend fun delete(tag: String) {
        _tags.update { current -> current.filter { it != tag } }
    }
}
