package io.github.fgrutsch.cookmaid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.fgrutsch.cookmaid.catalog.ItemCategory

/**
 * Accent colours for shopping list categories, so the list takes its colour from what is in it
 * rather than from the chrome around it.
 *
 * Every hue here is one the logo illustration already uses — sampled from it, then darkened or
 * lightened until it clears 4.5:1 against the page. The logo's own tones are drawn for a white
 * background and are far too light to read as text, so what carries over is the hue, not the
 * literal value.
 *
 * Categories map to seven families rather than sixteen individual colours. Sixteen hues would be
 * a rainbow, and the family is the distinction that is actually useful while shopping — you move
 * through a store by department.
 */

// Fresh produce — the apron sage, pushed greener.
private val ProduceLight = Color(0xFF3E7E5C)
private val ProduceDark = Color(0xFF458C66)

// Bread, grains and baking — the wooden spoon and the golden hair.
private val BakeryLight = Color(0xFF8A6D2B)
private val BakeryDark = Color(0xFF9B7B31)

// Meat — the tomato in the basket.
private val ProteinLight = Color(0xFFC8462F)
private val ProteinDark = Color(0xFFD35943)

// Fish — the cooler end of the logo's frame.
private val FishLight = Color(0xFF377B86)
private val FishDark = Color(0xFF3D8994)

// Chilled and frozen — the light blue of the shirt.
private val ChilledLight = Color(0xFF3E77A0)
private val ChilledDark = Color(0xFF4585B2)

// Pantry staples — the hexagon frame's orange.
private val PantryLight = Color(0xFFB25A1F)
private val PantryDark = Color(0xFFC56423)

// Everything that is not food — the brand navy, muted.
private val OtherLight = Color(0xFF50768E)
private val OtherDark = Color(0xFF59849E)

private enum class CategoryFamily(val light: Color, val dark: Color) {
    Produce(ProduceLight, ProduceDark),
    Bakery(BakeryLight, BakeryDark),
    Protein(ProteinLight, ProteinDark),
    Fish(FishLight, FishDark),
    Chilled(ChilledLight, ChilledDark),
    Pantry(PantryLight, PantryDark),
    Other(OtherLight, OtherDark),
}

/**
 * Category ids are seeded server-side with fixed UUIDs (`V2__create_item_categories.sql`), while
 * their names arrive localised. Keying on the id is what keeps the colours stable in German.
 */
private val familyByCategoryId = mapOf(
    "00000000-0000-0000-0000-000000000001" to CategoryFamily.Produce, // Fruits
    "00000000-0000-0000-0000-000000000002" to CategoryFamily.Produce, // Vegetables
    "00000000-0000-0000-0000-000000000003" to CategoryFamily.Bakery, // Bread
    "00000000-0000-0000-0000-000000000004" to CategoryFamily.Chilled, // Refrigerated Products
    "00000000-0000-0000-0000-000000000005" to CategoryFamily.Protein, // Meat
    "00000000-0000-0000-0000-000000000006" to CategoryFamily.Fish, // Fish
    "00000000-0000-0000-0000-000000000007" to CategoryFamily.Bakery, // Grains & Pasta
    "00000000-0000-0000-0000-000000000008" to CategoryFamily.Other, // Beverages
    "00000000-0000-0000-0000-000000000009" to CategoryFamily.Other, // Snacks
    "00000000-0000-0000-0000-000000000010" to CategoryFamily.Chilled, // Frozen
    "00000000-0000-0000-0000-000000000011" to CategoryFamily.Pantry, // Canned & Jarred
    "00000000-0000-0000-0000-000000000012" to CategoryFamily.Pantry, // Condiments & Sauces
    "00000000-0000-0000-0000-000000000013" to CategoryFamily.Pantry, // Spices & Herbs
    "00000000-0000-0000-0000-000000000014" to CategoryFamily.Pantry, // Oils & Vinegars
    "00000000-0000-0000-0000-000000000015" to CategoryFamily.Bakery, // Baking
    "00000000-0000-0000-0000-000000000016" to CategoryFamily.Other, // Household
)

private const val DARK_SURFACE_LUMINANCE = 0.5f

/**
 * The accent colour for [category], resolved for the active theme.
 *
 * @param category the item's category, or null for free-text items that have none.
 * @return the family colour, or the neutral [CategoryFamily.Other] for an absent or unmapped
 *   category — a category added server-side without a mapping here stays legible rather than
 *   falling back to something unreadable.
 */
@Composable
fun categoryColor(category: ItemCategory?): Color {
    val family = category?.let { familyByCategoryId[it.id.toString()] } ?: CategoryFamily.Other
    // Read the scheme rather than threading isDark down from AppTheme: the surface tone already
    // says which scheme is active, and it cannot drift out of sync with the theme.
    val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE
    return if (isDark) family.dark else family.light
}
