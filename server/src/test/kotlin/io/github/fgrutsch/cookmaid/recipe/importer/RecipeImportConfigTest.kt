package io.github.fgrutsch.cookmaid.recipe.importer

import io.github.fgrutsch.cookmaid.support.testRecipeImportConfig
import io.ktor.server.config.yaml.YamlConfig
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RecipeImportConfigTest {

    @Test
    fun `application yaml defaults load and match the test config`() {
        // OIDC_AUDIENCE has no default; Ktor resolves env placeholders from system properties too.
        System.setProperty("OIDC_AUDIENCE", "test")
        try {
            val yaml = assertNotNull(YamlConfig("application.yaml"))

            assertEquals(testRecipeImportConfig, RecipeImportConfig.from(yaml))
        } finally {
            System.clearProperty("OIDC_AUDIENCE")
        }
    }
}
