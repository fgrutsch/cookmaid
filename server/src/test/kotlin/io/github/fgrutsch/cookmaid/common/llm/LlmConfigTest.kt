package io.github.fgrutsch.cookmaid.common.llm

import io.ktor.server.config.MapApplicationConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LlmConfigTest {

    @Test
    fun `config requires base url and model`() {
        assertNull(LlmConfig.from(MapApplicationConfig("llm.base-url" to "", "llm.model" to "m")))
        assertNull(LlmConfig.from(MapApplicationConfig("llm.base-url" to "https://x", "llm.model" to " ")))
        assertNull(LlmConfig.from(MapApplicationConfig()))
        assertEquals(
            LlmConfig("https://x", "", "m"),
            LlmConfig.from(MapApplicationConfig("llm.base-url" to "https://x", "llm.model" to "m")),
        )
    }
}
