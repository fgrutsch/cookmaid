package io.github.fgrutsch.catalog

import kotlinx.serialization.Serializable

@Serializable
data class ItemCategory(val id: String, val name: String)
