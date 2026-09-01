package io.github.fgrutsch.cookmaid.ui.recipe.detail

/** Digit budget per number, keeping the exact integer arithmetic below well inside [Long]. */
private const val MAX_DIGITS = 6
private const val MAX_DECIMALS = 3

/** Scaled amounts are rounded to two decimals. */
private const val PRECISION = 100L
private const val DECIMAL_BASE = 10L

private val mixedPattern = Regex("""^\s*(?:(\d{1,$MAX_DIGITS})\s+)?(\d{1,$MAX_DIGITS})\s*/\s*(\d{1,$MAX_DIGITS})""")
private val decimalPattern = Regex("""^\s*(\d{1,$MAX_DIGITS})(?:([.,])(\d{1,$MAX_DECIMALS}))?""")

/**
 * The leading amount of a quantity as an exact fraction, how much of the original text it
 * consumed, and the decimal separator it was written with (null when it carried none).
 */
private data class Amount(
    val numerator: Long,
    val denominator: Long,
    val length: Int,
    val separator: Char?,
)

/**
 * Rewrites the leading amount of a free-text [quantity] for a different serving count,
 * leaving the rest of the text (unit, notes) untouched.
 *
 * Amounts may be written as whole numbers, decimals (`1.5`, `1,5`), fractions (`1/2`)
 * or mixed fractions (`2 1/2`). The result is a decimal rounded to two places, written with
 * the separator the input used, or [decimalSeparator] when the input had none. Text without a
 * leading amount (`a pinch`) is returned unchanged, as is any non-positive serving count.
 *
 * @param quantity the free-text quantity as entered by the user.
 * @param servings the serving count the recipe is being scaled to.
 * @param baseServings the serving count the recipe was written for.
 * @param decimalSeparator the separator to use when the input amount has no decimals of its own.
 * @return the scaled quantity text.
 */
fun scaleQuantity(
    quantity: String,
    servings: Int,
    baseServings: Int,
    decimalSeparator: Char = '.',
): String {
    if (servings <= 0 || baseServings <= 0 || servings == baseServings) return quantity
    return parseAmount(quantity)?.let { amount ->
        val hundredths = divideRounded(amount.numerator * servings * PRECISION, amount.denominator * baseServings)
        formatAmount(hundredths, amount.separator ?: decimalSeparator) + quantity.substring(amount.length)
    } ?: quantity
}

private fun parseAmount(quantity: String): Amount? =
    mixedPattern.find(quantity)?.toMixedAmount() ?: decimalPattern.find(quantity)?.toDecimalAmount()

private fun MatchResult.toMixedAmount(): Amount? {
    val whole = groupValues[1].toLongOrNull() ?: 0L
    val numerator = groupValues[2].toLong()
    val denominator = groupValues[3].toLong()
    return if (denominator > 0) {
        Amount(whole * denominator + numerator, denominator, value.length, separator = null)
    } else {
        null
    }
}

private fun MatchResult.toDecimalAmount(): Amount {
    val decimals = groupValues[3]
    val denominator = (1..decimals.length).fold(1L) { acc, _ -> acc * DECIMAL_BASE }
    val numerator = groupValues[1].toLong() * denominator + (decimals.toLongOrNull() ?: 0L)
    return Amount(numerator, denominator, value.length, separator = groupValues[2].firstOrNull())
}

private fun divideRounded(dividend: Long, divisor: Long): Long = (dividend + divisor / 2) / divisor

private fun formatAmount(hundredths: Long, separator: Char): String {
    val whole = hundredths / PRECISION
    val decimals = hundredths % PRECISION
    return when {
        decimals == 0L -> whole.toString()
        decimals % DECIMAL_BASE == 0L -> "$whole$separator${decimals / DECIMAL_BASE}"
        else -> "$whole$separator${decimals.toString().padStart(2, '0')}"
    }
}
