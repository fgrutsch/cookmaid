package io.github.fgrutsch.cookmaid.user

import kotlin.uuid.Uuid

/**
 * Type-safe wrapper around a user's unique identifier.
 */
@JvmInline
value class UserId(val value: Uuid)
