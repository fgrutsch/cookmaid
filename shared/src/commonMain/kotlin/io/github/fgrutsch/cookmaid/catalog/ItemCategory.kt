package io.github.fgrutsch.cookmaid.catalog

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ItemCategory(
    val id: Uuid,
    val name: String,
)
