package io.github.fgrutsch.cookmaid.common.llm

import io.ktor.server.config.MapApplicationConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LlmConfigTest {

    @Test
    fun `enabled requires base url and model`() {
        assertFalse(LlmConfig.from(MapApplicationConfig("llm.base-url" to "", "llm.model" to "m")).enabled)
        assertFalse(LlmConfig.from(MapApplicationConfig("llm.base-url" to "https://x", "llm.model" to " ")).enabled)
        assertFalse(LlmConfig.from(MapApplicationConfig()).enabled)
        assertTrue(LlmConfig.from(MapApplicationConfig("llm.base-url" to "https://x", "llm.model" to "m")).enabled)
    }

    @Test
    fun `missing api key is blank`() {
        assertEquals(
            LlmConfig("https://x", "", "m"),
            LlmConfig.from(MapApplicationConfig("llm.base-url" to " https://x ", "llm.model" to "m")),
        )
    }
}
