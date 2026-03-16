package io.github.fgrutsch.cookmaid.catalog

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed interface Item {
    val name: String

    @Serializable
    data class CatalogItem(
        val id: Uuid,
        override val name: String,
        val category: ItemCategory,
    ) : Item

    @Serializable
    data class FreeTextItem(
        override val name: String,
    ) : Item
}
