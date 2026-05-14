package io.github.fgrutsch.cookmaid.user

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.Uuid

@Serializable
data class User(
    val id: Uuid,
    @Transient val oidcSubject: String = "",
)
