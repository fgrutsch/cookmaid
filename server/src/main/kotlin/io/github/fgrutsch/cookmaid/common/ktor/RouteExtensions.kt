package io.github.fgrutsch.cookmaid.common.ktor

import io.ktor.http.*
import io.ktor.server.util.*
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.uuid.Uuid

fun Parameters.int(name: String): Int? = get(name)?.toIntOrNull()

fun Parameters.uuid(name: String): Uuid = Uuid.parse(getOrFail(name))

fun Parameters.localDate(name: String): LocalDate = LocalDate.parse(getOrFail(name))

fun Parameters.instant(name: String): Instant? = get(name)?.let { Instant.fromEpochMilliseconds(it.toLong()) }
