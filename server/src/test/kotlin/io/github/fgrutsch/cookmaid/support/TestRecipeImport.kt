package io.github.fgrutsch.cookmaid.support

import io.github.fgrutsch.cookmaid.recipe.importer.IngredientParser
import io.github.fgrutsch.cookmaid.recipe.importer.JsonLdRecipeExtractor
import io.github.fgrutsch.cookmaid.recipe.importer.RecipeImportConfig

// Mirrors the recipe-import defaults in application.yaml.
val testRecipeImportConfig = RecipeImportConfig(
    fetchTimeoutMillis = 10_000,
    fetchMaxBytes = 2L * 1024 * 1024,
    fetchMaxRedirects = 5,
    llmTimeoutMillis = 60_000,
    llmMaxPageChars = 60_000,
    maxNameLength = 200,
    maxDescriptionLength = 2_000,
    maxLineLength = 500,
    maxItems = 100,
    maxServings = 100,
    ingredientUnits = listOf(
        "g", "kg", "mg", "ml", "cl", "dl", "l", "el", "tl", "msp", "prise", "prisen", "pck", "päckchen", "packung",
        "dose", "dosen", "becher", "bund", "stück", "stk", "zehe", "zehen", "zweig", "zweige", "blatt", "scheibe",
        "scheiben", "tasse", "tassen", "esslöffel", "teelöffel", "schuss", "tbsp", "tsp", "tablespoon",
        "tablespoons", "teaspoon", "teaspoons", "cup", "cups", "oz", "lb", "lbs", "pinch", "clove", "cloves", "can",
        "cans", "slice", "slices",
    ),
)

val testIngredientParser = IngredientParser(testRecipeImportConfig)

val testJsonLdExtractor = JsonLdRecipeExtractor(testRecipeImportConfig, testIngredientParser)
