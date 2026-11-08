package com.nnoidea.mecalculator.domain

import com.nnoidea.mecalculator.data.NumberGrouping
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatterTest {

    @Test
    fun `formats integer and decimal numbers with space comma or none`() {
        assertEquals("1 000 000", NumberFormatter.formatNumberString("1000000", NumberGrouping.SPACE))
        assertEquals("1,000,000", NumberFormatter.formatNumberString("1000000", NumberGrouping.COMMA))
        assertEquals("1000000", NumberFormatter.formatNumberString("1000000", NumberGrouping.NONE))

        // Fractional digits after '.' should not be grouped
        assertEquals("1 234 567.89012", NumberFormatter.formatNumberString("1234567.89012", NumberGrouping.SPACE))
    }

    @Test
    fun `limits trailing decimal digits and appends ellipsis when truncated`() {
        assertEquals(
            "1.69...",
            NumberFormatter.formatResultString(
                raw = "1.69696969696",
                grouping = NumberGrouping.SPACE,
                maxDecimalPlaces = 2,
            ),
        )
        assertEquals(
            "1.6969...",
            NumberFormatter.formatResultString(
                raw = "1.69696969696",
                grouping = NumberGrouping.SPACE,
                maxDecimalPlaces = 4,
            ),
        )
        // Exact decimal shorter than maxDecimalPlaces should not show ellipsis
        assertEquals(
            "1.5",
            NumberFormatter.formatResultString(
                raw = "1.5",
                grouping = NumberGrouping.SPACE,
                maxDecimalPlaces = 2,
            ),
        )
    }

    @Test
    fun `formats scientific notation exclusively with 10 exponent notation`() {
        assertEquals(
            "1.69...·10¹⁵",
            NumberFormatter.formatResultString(
                raw = "1.69696969696E+15",
                grouping = NumberGrouping.SPACE,
                maxDecimalPlaces = 2,
            ),
        )
        assertEquals(
            "2.5·10⁻⁸",
            NumberFormatter.formatResultString(
                raw = "2.5E-8",
                grouping = NumberGrouping.SPACE,
                maxDecimalPlaces = 4,
            ),
        )
        // Also supports caret and direct 10^ notation
        assertEquals(
            "1·10²¹",
            NumberFormatter.formatResultString(
                raw = "1*10^21",
                grouping = NumberGrouping.SPACE,
                maxDecimalPlaces = 4,
            ),
        )
        assertEquals(
            "1·10²¹",
            NumberFormatter.formatResultString(
                raw = "10^21",
                grouping = NumberGrouping.SPACE,
                maxDecimalPlaces = 4,
            ),
        )
        assertEquals(
            "1·10²¹",
            NumberFormatter.formatResultString(
                raw = "1E+21",
                grouping = NumberGrouping.SPACE,
                maxDecimalPlaces = 4,
            ),
        )
    }

    @Test
    fun `converts result string into 2D MathTokens with PowerToken`() {
        val tokens1E21 = NumberFormatter.resultStringToTokens("1E+21")
        assertEquals(5, tokens1E21.size)
        assertEquals(com.nnoidea.mecalculator.ui.main.MathToken.CharToken('1'), tokens1E21[0])
        assertEquals(com.nnoidea.mecalculator.ui.main.MathToken.CharToken('·'), tokens1E21[1])
        assertEquals(com.nnoidea.mecalculator.ui.main.MathToken.CharToken('1'), tokens1E21[2])
        assertEquals(com.nnoidea.mecalculator.ui.main.MathToken.CharToken('0'), tokens1E21[3])
        val pow = tokens1E21[4] as com.nnoidea.mecalculator.ui.main.MathToken.PowerToken
        assertEquals(
            listOf(
                com.nnoidea.mecalculator.ui.main.MathToken.CharToken('2'),
                com.nnoidea.mecalculator.ui.main.MathToken.CharToken('1'),
            ),
            pow.exp,
        )

        // Verify tokens evaluate back cleanly through ExactMathEngine
        val evaluated = ExactMathEngine.evaluateTokens(tokens1E21)
        assertEquals("1E+21", evaluated)

        // Verify unicode superscripts evaluate cleanly in ExactMathEngine
        assertEquals("1E+21", ExactMathEngine.evaluateExpressionString("1·10²¹"))
    }

    @Test
    fun `formats 5 to the 99th power and repeating decimals`() {
        val fivePow99 = ExactMathEngine.evaluateExpressionString("5^99")!!
        assertEquals(
            "1.57...·10⁶⁹",
            NumberFormatter.formatResultString(
                raw = fivePow99,
                grouping = NumberGrouping.SPACE,
                maxDecimalPlaces = 2,
            ),
        )

        val fiveDivThree = ExactMathEngine.evaluateExpressionString("5/3")!!
        assertEquals(
            "1.66...",
            NumberFormatter.formatResultString(
                raw = fiveDivThree,
                grouping = NumberGrouping.SPACE,
                maxDecimalPlaces = 2,
            ),
        )
    }

    @Test
    fun `live leading separators for char lists handles integers decimals and operators`() {
        val chars = "1234567.8901+1000".toList()
        val seps = NumberFormatter.getLeadingSeparatorsForCharList(chars, NumberGrouping.SPACE)
        val reconstructed = chars.indices.joinToString("") { i -> "${seps[i]}${chars[i]}" }
        assertEquals("1 234 567.8901+1 000", reconstructed)
    }

    @Test
    fun `formatResultTokens produces 2D tokens for integers, decimals, and scientific results`() {
        // Plain integer
        val intTokens = NumberFormatter.formatResultTokens("100000000", NumberGrouping.SPACE, 2)
        assertEquals(9, intTokens.size)
        assertEquals(com.nnoidea.mecalculator.ui.main.MathToken.CharToken('1'), intTokens.first())

        // Scientific result with truncation
        val sciTokens = NumberFormatter.formatResultTokens("1.616161616161616E+87", NumberGrouping.SPACE, 2)
        // Mantissa: '1', '.', '6', '1', '.', '.', '.'
        // Multiplier: '·', '1', '0'
        // Exponent: PowerToken(['8', '7'])
        val lastTok = sciTokens.last()
        org.junit.Assert.assertTrue(lastTok is com.nnoidea.mecalculator.ui.main.MathToken.PowerToken)
        val expTok = lastTok as com.nnoidea.mecalculator.ui.main.MathToken.PowerToken
        assertEquals(
            listOf(
                com.nnoidea.mecalculator.ui.main.MathToken.CharToken('8'),
                com.nnoidea.mecalculator.ui.main.MathToken.CharToken('7'),
            ),
            expTok.exp,
        )
    }
}
