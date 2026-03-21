package io.github.fgrutsch.cookmaid.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * A shopping or recipe item, either picked from the catalog or entered as free text.
 */
@Serializable
sealed interface Item {
    val name: String

    @Serializable
    @SerialName("catalog")
    data class Catalog(
        val id: Uuid,
        override val name: String,
        val category: ItemCategory,
    ) : Item

    @Serializable
    @SerialName("free_text")
    data class FreeText(
        override val name: String,
    ) : Item
}
