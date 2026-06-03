package io.github.fgrutsch.cookmaid.common

/**
 * Supported languages for catalog item and category translations.
 */
enum class SupportedLocale(val code: String) {
    EN("en"),
    DE("de");

    companion object {
        /**
         * Resolves a language code to a [SupportedLocale], falling back to [EN].
         *
         * @param code the language code (e.g. "en", "de").
         * @return the matching locale or [EN] if unsupported.
         */
        fun fromCode(code: String): SupportedLocale =
            entries.find { it.code == code.lowercase() } ?: EN
    }
}
