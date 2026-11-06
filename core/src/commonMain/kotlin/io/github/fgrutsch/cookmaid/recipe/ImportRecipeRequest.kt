package io.github.fgrutsch.cookmaid.recipe

import kotlinx.serialization.Serializable

@Serializable
data class ImportRecipeRequest(val url: String = "")
