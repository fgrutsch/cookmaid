package io.github.fgrutsch.cookmaid.common.llm

import io.ktor.server.config.ApplicationConfig

/**
 * Connection settings for any OpenAI-compatible chat-completions provider. The LLM is
 * optional: when base URL or model is blank, [enabled] is false and features skip it.
 *
 * @property baseUrl API base, e.g. `https://api.openai.com/v1`; `/chat/completions` is appended.
 * @property apiKey bearer token; blank for keyless local providers.
 * @property model provider model id.
 */
data class LlmConfig(val baseUrl: String, val apiKey: String, val model: String) {

    companion object {
        /**
         * Reads `llm.base-url`, `llm.api-key` and `llm.model`; missing keys are blank.
         */
        fun from(config: ApplicationConfig): LlmConfig {
            fun value(key: String) = config.propertyOrNull(key)?.getString()?.trim().orEmpty()
            return LlmConfig(value("llm.base-url"), value("llm.api-key"), value("llm.model"))
        }
    }

    val enabled: Boolean get() = baseUrl.isNotBlank() && model.isNotBlank()
}
