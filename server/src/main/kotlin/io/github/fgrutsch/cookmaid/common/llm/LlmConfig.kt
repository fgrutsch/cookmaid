package io.github.fgrutsch.cookmaid.common.llm

import io.ktor.server.config.ApplicationConfig

/**
 * Connection settings for any OpenAI-compatible chat-completions provider.
 *
 * @property baseUrl API base, e.g. `https://api.openai.com/v1`; `/chat/completions` is appended.
 * @property apiKey bearer token; blank for keyless local providers.
 * @property model provider model id.
 */
data class LlmConfig(val baseUrl: String, val apiKey: String, val model: String) {
    companion object {
        /**
         * @return the configured LLM, or null when `llm.base-url` or `llm.model` is blank —
         *   the LLM fallback is optional.
         */
        fun from(config: ApplicationConfig): LlmConfig? {
            fun value(key: String) = config.propertyOrNull(key)?.getString()?.trim().orEmpty()
            val baseUrl = value("llm.base-url")
            val model = value("llm.model")
            return if (baseUrl.isBlank() || model.isBlank()) null else LlmConfig(baseUrl, value("llm.api-key"), model)
        }
    }
}
