package com.nnoidea.mecalculator.domain

import com.nnoidea.mecalculator.data.NumberGrouping
import com.nnoidea.mecalculator.ui.main.MathToken

object NumberFormatter {

    private val SUPERSCRIPT_MAP = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
        '⁻' to '−', '⁺' to '+',
    )

    /**
     * Given a list of characters (e.g. inside the root expression, fraction numerator/denominator,
     * or exponent), returns a list of display strings where each element corresponds to the character
     * at that index, optionally prefixed or suffixed with a group separator so cursor indices remain 1:1!
     *
     * Specifically, if a digit is the last digit of a 3-digit thousand group in the integer part of a number,
     * its display string is `"{digit}{separator}"` (or we attach the separator BEFORE the next group's first digit
     * so typing a cursor right after a digit sits before the separator).
     */
    fun getLeadingSeparatorsForCharList(
        chars: List<Char>,
        grouping: NumberGrouping,
    ): List<String> {
        val result = MutableList(chars.size) { "" }
        if (grouping == NumberGrouping.NONE || grouping.separator.isEmpty() || chars.isEmpty()) {
            return result
        }

        var i = 0
        while (i < chars.size) {
            if (chars[i].isDigit()) {
                // Find the contiguous run of integer digits
                val start = i
                while (i < chars.size && chars[i].isDigit()) {
                    i++
                }
                val end = i // exclusive

                // Check if this digit run is the fractional part of a decimal (i.e. preceded by '.')
                val isAfterDecimalPoint = start > 0 && chars[start - 1] == '.'
                if (!isAfterDecimalPoint) {
                    val len = end - start
                    if (len > 3) {
                        for (pos in (start + 1) until end) {
                            val digitsRemaining = end - pos
                            if (digitsRemaining % 3 == 0) {
                                result[pos] = grouping.separator
                            }
                        }
                    }
                }
            } else {
                i++
            }
        }
        return result
    }

    /**
     * Formats a plain result string (like `"1000000.25"` or `"-45000"`) using the chosen [NumberGrouping].
     */
    fun formatNumberString(raw: String, grouping: NumberGrouping): String {
        if (grouping == NumberGrouping.NONE || grouping.separator.isEmpty() || raw.isEmpty()) {
            return raw
        }
        val chars = raw.toList()
        val seps = getLeadingSeparatorsForCharList(chars, grouping)
        return buildString {
            for (idx in chars.indices) {
                append(seps[idx])
                append(chars[idx])
            }
        }
    }

    fun splitScientificNotation(raw: String): Pair<String, String?> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "" to null

        // 1. Standard E/e notation: e.g. "1.69E+15", "1E21", "2.5e-8"
        val eIdx = trimmed.indexOfFirst { it == 'E' || it == 'e' }
        if (eIdx >= 0) {
            val mantissa = trimmed.substring(0, eIdx).ifEmpty { "1" }
            val rawExp = trimmed.substring(eIdx + 1).removePrefix("+")
            return mantissa to rawExp
        }

        // 2. Caret notation with base 10: e.g. "1.69*10^15", "1×10^21", "10^21", "*10^21"
        val caretMatch = Regex("""^(.*?)(?:[*×·⋅])?10\^([\+\-−]?\d+)$""").matchEntire(trimmed)
        if (caretMatch != null) {
            val rawMantissa = caretMatch.groupValues[1].trim().trimEnd('*', '×', '·', '⋅', ' ')
            val mantissa = if (rawMantissa.isEmpty() || rawMantissa == "1") "1" else rawMantissa
            val rawExp = caretMatch.groupValues[2].replace('−', '-').removePrefix("+")
            return mantissa to rawExp
        }

        // 3. Superscript notation with base 10: e.g. "1.69·10¹⁵", "10²¹", "1·10²¹"
        val superMatch = Regex("""^(.*?)(?:[*×·⋅])?10([⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻]+)$""").matchEntire(trimmed)
        if (superMatch != null) {
            val rawMantissa = superMatch.groupValues[1].trim().trimEnd('*', '×', '·', '⋅', ' ')
            val mantissa = if (rawMantissa.isEmpty() || rawMantissa == "1") "1" else rawMantissa
            val rawExp = superMatch.groupValues[2].map { SUPERSCRIPT_MAP[it] ?: it }.joinToString("")
                .replace('−', '-')
                .removePrefix("+")
            return mantissa to rawExp
        }

        return trimmed to null
    }

    /**
     * Converts any raw result string (e.g. "1E+21", "1.69E15", "1*10^21", "1·10²¹", "42", "-3.14")
     * into a LaTeX-like 2D [MathToken] list.
     * Scientific notation is automatically decomposed into mantissa tokens + [·, 1, 0, PowerToken(exp)].
     */
    fun resultStringToTokens(raw: String): List<MathToken> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()

        val (mantissa, rawExp) = splitScientificNotation(trimmed)
        val tokens = mutableListOf<MathToken>()

        // 1. Mantissa tokens
        for (ch in mantissa) {
            when (ch) {
                '-' -> tokens.add(MathToken.CharToken('−'))
                ' ' -> {}
                else -> tokens.add(MathToken.CharToken(ch))
            }
        }

        // 2. Exponential part if present: " · 10" + PowerToken(exp)
        if (rawExp != null) {
            tokens.add(MathToken.CharToken('·'))
            tokens.add(MathToken.CharToken('1'))
            tokens.add(MathToken.CharToken('0'))

            val cleanExp = rawExp.removePrefix("+")
            val expTokens = mutableListOf<MathToken>()
            for (ch in cleanExp) {
                when (ch) {
                    '-', '−' -> expTokens.add(MathToken.CharToken('−'))
                    '+' -> {}
                    ' ' -> {}
                    else -> expTokens.add(MathToken.CharToken(ch))
                }
            }
            tokens.add(MathToken.PowerToken(exp = expTokens))
        }

        return tokens
    }

    /**
     * Formats a raw calculation result string and returns a LaTeX-like 2D [MathToken] list,
     * applying trailing decimal limit, truncation ellipsis, and exponential formatting.
     */
    fun formatResultTokens(
        raw: String,
        grouping: NumberGrouping,
        maxDecimalPlaces: Int = 6,
    ): List<MathToken> {
        if (raw.isEmpty()) return emptyList()
        val formattedStr = formatResultString(raw, grouping, maxDecimalPlaces)
        return resultStringToTokens(formattedStr)
    }

    /**
     * Formats a calculator result string for the results/history view:
     * - Applies [grouping] to the integer part.
     * - Limits trailing decimal digits to [maxDecimalPlaces], appending `"..."` when truncated
     *   (e.g. `"1.69696969696"` with `maxDecimalPlaces = 2` -> `"1.69..."`).
     * - Formats exponential notation using standard `·10ⁿ` notation (e.g. `"1.69·10¹⁵"`).
     */
    fun formatResultString(
        raw: String,
        grouping: NumberGrouping,
        maxDecimalPlaces: Int = 6,
    ): String {
        if (raw.isEmpty()) return raw

        val (mantissa, rawExp) = splitScientificNotation(raw)

        val dotIdx = mantissa.indexOf('.')
        val intPart = if (dotIdx >= 0) mantissa.substring(0, dotIdx) else mantissa
        val fracPart = if (dotIdx >= 0) mantissa.substring(dotIdx + 1) else ""

        val groupedInt = formatNumberString(intPart, grouping)
        val clampedMax = maxDecimalPlaces.coerceAtLeast(1)

        val formattedMantissa = when {
            fracPart.isEmpty() -> groupedInt
            fracPart.length > clampedMax -> "$groupedInt.${fracPart.take(clampedMax)}..."
            else -> "$groupedInt.$fracPart"
        }

        if (rawExp == null) {
            return formattedMantissa
        }

        val cleanExpInt = rawExp.removePrefix("+").toIntOrNull()
        val expStr = cleanExpInt?.toString() ?: rawExp.removePrefix("+")
        val formattedExp = "·10${toSuperscript(expStr)}"

        return formattedMantissa + formattedExp
    }

    private fun toSuperscript(text: String): String = buildString {
        for (ch in text) {
            append(
                when (ch) {
                    '0' -> '⁰'
                    '1' -> '¹'
                    '2' -> '²'
                    '3' -> '³'
                    '4' -> '⁴'
                    '5' -> '⁵'
                    '6' -> '⁶'
                    '7' -> '⁷'
                    '8' -> '⁸'
                    '9' -> '⁹'
                    '-', '−' -> '⁻'
                    '+' -> '⁺'
                    else -> ch
                },
            )
        }
    }
}
