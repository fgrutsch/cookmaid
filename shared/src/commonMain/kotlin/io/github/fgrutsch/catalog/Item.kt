package io.github.fgrutsch.catalog

import kotlinx.serialization.Serializable

@Serializable
sealed interface Item {
    val name: String

    @Serializable
    data class CategorizedItem(
        val id: String,
        override val name: String,
        val category: String,
    ) : Item

    @Serializable
    data class FreeTextItem(
        override val name: String,
    ) : Item
}
