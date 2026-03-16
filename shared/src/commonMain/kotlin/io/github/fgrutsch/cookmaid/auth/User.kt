package io.github.fgrutsch.cookmaid.auth

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class User(
    val id: Uuid,
    val oidcSubject: String,
)
