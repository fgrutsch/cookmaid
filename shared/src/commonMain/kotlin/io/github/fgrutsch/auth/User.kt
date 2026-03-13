package io.github.fgrutsch.auth

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val oidcSubject: String,
)
