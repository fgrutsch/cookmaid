package io.github.fgrutsch.cookmaid.support

import io.github.fgrutsch.cookmaid.recipe.importer.IngredientParser
import io.github.fgrutsch.cookmaid.recipe.importer.JsonLdRecipeExtractor
import io.github.fgrutsch.cookmaid.recipe.importer.RecipeImportConfig

// Mirrors recipe.import defaults in application.yaml; RecipeImportConfigTest fails on drift.
val testRecipeImportConfig = RecipeImportConfig(
    fetchTimeoutMillis = 10_000,
    fetchMaxBytes = 2L * 1024 * 1024,
    llmTimeoutMillis = 60_000,
    llmMaxPageChars = 60_000,
    maxItems = 100,
    ingredientUnits = listOf(
        "g", "gr", "gramm", "gram", "grams", "kg", "kilogramm", "kilogram", "kilograms", "dag", "mg", "ml",
        "milliliter", "milliliters", "millilitre", "millilitres", "cl", "dl", "l", "liter", "liters", "litre",
        "litres", "el", "esslöffel", "tl", "teelöffel", "msp", "messerspitze", "messerspitzen", "prise", "prisen",
        "pck", "päckchen", "packung", "packungen", "dose", "dosen", "becher", "bund", "stück", "stk", "st",
        "stange", "stangen", "stg", "stiel", "stiele", "zehe", "zehen", "zweig", "zweige", "blatt", "blätter",
        "scheibe", "scheiben", "tasse", "tassen", "glas", "gläser", "flasche", "flaschen", "tube", "tuben",
        "würfel", "beutel", "tropfen", "handvoll", "knolle", "knollen", "kugel", "kugeln", "schuss", "spritzer",
        "tbsp", "tsp", "tablespoon", "tablespoons", "teaspoon", "teaspoons", "cup", "cups", "oz", "ounce",
        "ounces", "lb", "lbs", "pound", "pounds", "pint", "pints", "quart", "quarts", "pinch", "pinches", "dash",
        "dashes", "clove", "cloves", "can", "cans", "slice", "slices", "stick", "sticks", "sheet", "sheets",
        "bunch", "bunches", "handful", "handfuls", "sprig", "sprigs", "package", "packages",
    ),
)

val testIngredientParser = IngredientParser(testRecipeImportConfig)

val testJsonLdExtractor = JsonLdRecipeExtractor(testRecipeImportConfig, testIngredientParser)
