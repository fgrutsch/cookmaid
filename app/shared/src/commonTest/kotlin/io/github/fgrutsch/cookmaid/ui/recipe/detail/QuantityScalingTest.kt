package io.github.fgrutsch.cookmaid.ui.recipe.detail

import kotlin.test.Test
import kotlin.test.assertEquals

class QuantityScalingTest {

    @Test
    fun `scales whole numbers and keeps the unit`() {
        assertEquals("400ml", scaleQuantity("200ml", servings = 4, baseServings = 2))
        assertEquals("400 ml", scaleQuantity("200 ml", servings = 4, baseServings = 2))
        assertEquals("6", scaleQuantity("2", servings = 6, baseServings = 2))
    }

    @Test
    fun `renders halves as mixed fractions`() {
        assertEquals("2 1/2", scaleQuantity("5", servings = 1, baseServings = 2))
        assertEquals("1/2 TL", scaleQuantity("1 TL", servings = 1, baseServings = 2))
    }

    @Test
    fun `scales fraction input`() {
        assertEquals("1 TL", scaleQuantity("1/2 TL", servings = 4, baseServings = 2))
        assertEquals("5 cups", scaleQuantity("2 1/2 cups", servings = 4, baseServings = 2))
        assertEquals("1/4 tsp", scaleQuantity("1/2 tsp", servings = 1, baseServings = 2))
    }

    @Test
    fun `scales decimal input with dot or comma`() {
        assertEquals("3 kg", scaleQuantity("1,5 kg", servings = 4, baseServings = 2))
        assertEquals("1 1/2 l", scaleQuantity("0.5 l", servings = 3, baseServings = 1))
    }

    @Test
    fun `keeps thirds exact`() {
        assertEquals("66 2/3 ml", scaleQuantity("200 ml", servings = 1, baseServings = 3))
    }

    @Test
    fun `leaves text without a leading amount untouched`() {
        assertEquals("a pinch", scaleQuantity("a pinch", servings = 4, baseServings = 2))
        assertEquals("", scaleQuantity("", servings = 4, baseServings = 2))
    }

    @Test
    fun `returns the original text when there is nothing to scale`() {
        assertEquals("1,5 kg", scaleQuantity("1,5 kg", servings = 2, baseServings = 2))
        assertEquals("1,5 kg", scaleQuantity("1,5 kg", servings = 2, baseServings = 0))
    }
}
