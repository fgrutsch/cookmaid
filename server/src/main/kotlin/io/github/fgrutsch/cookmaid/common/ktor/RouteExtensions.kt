package io.github.fgrutsch.cookmaid.common.ktor

import io.ktor.http.Parameters
import io.ktor.server.util.getOrFail
import kotlin.uuid.Uuid

fun Parameters.uuid(name: String): Uuid = Uuid.parse(getOrFail(name))
