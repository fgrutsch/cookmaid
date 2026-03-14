package io.github.fgrutsch.common.ktor

import io.ktor.http.Parameters
import org.junit.jupiter.api.Test
import io.ktor.server.plugins.MissingRequestParameterException
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class RouteExtensionsTest {

    @Test
    fun `uuid parses valid UUID parameter`() {
        val expected = Uuid.random()
        val params = Parameters.build { append("id", expected.toString()) }

        assertEquals(expected, params.uuid("id"))
    }

    @Test
    fun `uuid throws for missing parameter`() {
        val params = Parameters.Empty

        assertThrows<MissingRequestParameterException> {
            params.uuid("id")
        }
    }

    @Test
    fun `uuid throws for invalid UUID`() {
        val params = Parameters.build { append("id", "not-a-uuid") }

        assertThrows<IllegalArgumentException> {
            params.uuid("id")
        }
    }
}
