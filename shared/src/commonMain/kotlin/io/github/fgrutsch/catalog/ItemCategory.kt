package io.github.fgrutsch.catalog

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ItemCategory(
    val id: Uuid,
    val name: String,
)
