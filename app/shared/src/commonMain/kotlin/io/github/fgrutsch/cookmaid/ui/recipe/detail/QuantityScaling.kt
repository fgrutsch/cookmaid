package io.github.fgrutsch.cookmaid.ui.recipe.detail

/** Digit budget per number, keeping the exact integer arithmetic below well inside [Long]. */
private const val MAX_DIGITS = 6
private const val MAX_DECIMALS = 3
private const val DECIMAL_BASE = 10L

private val mixedPattern = Regex("""^\s*(?:(\d{1,$MAX_DIGITS})\s+)?(\d{1,$MAX_DIGITS})\s*/\s*(\d{1,$MAX_DIGITS})""")
private val decimalPattern = Regex("""^\s*(\d{1,$MAX_DIGITS})(?:[.,](\d{1,$MAX_DECIMALS}))?""")

/**
 * The leading amount of a quantity as an exact fraction, plus how much of the
 * original text it consumed.
 */
private data class Amount(val numerator: Long, val denominator: Long, val length: Int)

/**
 * Rewrites the leading amount of a free-text [quantity] for a different serving count,
 * leaving the rest of the text (unit, notes) untouched.
 *
 * Amounts may be written as whole numbers, decimals (`1.5`, `1,5`), fractions (`1/2`)
 * or mixed fractions (`2 1/2`); the result is rendered as a mixed fraction. Text without
 * a leading amount (`a pinch`) is returned unchanged, as is any non-positive serving count.
 *
 * @param quantity the free-text quantity as entered by the user.
 * @param servings the serving count the recipe is being scaled to.
 * @param baseServings the serving count the recipe was written for.
 * @return the scaled quantity text.
 */
fun scaleQuantity(quantity: String, servings: Int, baseServings: Int): String {
    if (servings <= 0 || baseServings <= 0 || servings == baseServings) return quantity
    return parseAmount(quantity)?.let { amount ->
        val scaled = formatAmount(amount.numerator * servings, amount.denominator * baseServings)
        scaled + quantity.substring(amount.length)
    } ?: quantity
}

private fun parseAmount(quantity: String): Amount? =
    mixedPattern.find(quantity)?.toMixedAmount() ?: decimalPattern.find(quantity)?.toDecimalAmount()

private fun MatchResult.toMixedAmount(): Amount? {
    val whole = groupValues[1].toLongOrNull() ?: 0L
    val numerator = groupValues[2].toLong()
    val denominator = groupValues[3].toLong()
    return if (denominator > 0) Amount(whole * denominator + numerator, denominator, value.length) else null
}

private fun MatchResult.toDecimalAmount(): Amount {
    val decimals = groupValues[2]
    val denominator = (1..decimals.length).fold(1L) { acc, _ -> acc * DECIMAL_BASE }
    val numerator = groupValues[1].toLong() * denominator + (decimals.toLongOrNull() ?: 0L)
    return Amount(numerator, denominator, value.length)
}

private fun formatAmount(numerator: Long, denominator: Long): String {
    val divisor = gcd(numerator, denominator)
    val reducedNumerator = numerator / divisor
    val reducedDenominator = denominator / divisor
    val whole = reducedNumerator / reducedDenominator
    val remainder = reducedNumerator % reducedDenominator
    return when {
        remainder == 0L -> whole.toString()
        whole == 0L -> "$remainder/$reducedDenominator"
        else -> "$whole $remainder/$reducedDenominator"
    }
}

private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
