package com.nnoidea.mecalculator.domain

import com.nnoidea.mecalculator.ui.main.CursorState
import com.nnoidea.mecalculator.ui.main.MathToken
import com.nnoidea.mecalculator.ui.main.backspaceAt
import com.nnoidea.mecalculator.ui.main.insertFractionAt
import com.nnoidea.mecalculator.ui.main.insertPowerAt
import com.nnoidea.mecalculator.ui.main.insertTokensAt
import com.nnoidea.mecalculator.ui.main.moveCursorInTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExactMathEngineTest {

    private fun chars(s: String): List<MathToken> = s.map { MathToken.CharToken(it) }
    private fun frac(num: List<MathToken>, den: List<MathToken>) = MathToken.FractionToken(num, den)
    private fun pow(exp: List<MathToken>) = MathToken.PowerToken(exp)

    /**
     * Helper that simulates a user pressing calculator buttons in sequence.
     */
    private class KeypadSimulator {
        var tokens: List<MathToken> = emptyList()
        var cursor: CursorState = CursorState()

        fun type(text: String): KeypadSimulator {
            val (t, c) = insertTokensAt(tokens, cursor, text.map { MathToken.CharToken(it) })
            tokens = t
            cursor = c
            return this
        }

        fun func(name: String): KeypadSimulator {
            val (t, c) = insertTokensAt(tokens, cursor, listOf(MathToken.FuncToken(name)))
            tokens = t
            cursor = c
            return this
        }

        fun div(): KeypadSimulator {
            val (t, c) = insertFractionAt(tokens, cursor)
            tokens = t
            cursor = c
            return this
        }

        fun pow(): KeypadSimulator {
            val (t, c) = insertPowerAt(tokens, cursor)
            tokens = t
            cursor = c
            return this
        }

        fun move(dir: String): KeypadSimulator {
            val (c, _) = moveCursorInTokens(tokens, cursor, dir)
            cursor = c
            return this
        }

        fun backspace(): KeypadSimulator {
            val (t, c) = backspaceAt(tokens, cursor)
            tokens = t
            cursor = c
            return this
        }

        fun eval(): String? = ExactMathEngine.evaluateTokens(tokens)
    }

    @Test
    fun `no floating point drift on decimals and fractions`() {
        assertEquals("0.3", ExactMathEngine.evaluateExpressionString("0.1 + 0.2"))
        assertEquals("1", ExactMathEngine.evaluateExpressionString("(1/3) + (2/3)"))
        assertEquals("1", ExactMathEngine.evaluateExpressionString("(1/7) * 7"))
    }

    @Test
    fun `basic calculations evaluate correctly`() {
        fun evalTokens(tokens: List<MathToken>): String? = ExactMathEngine.evaluateTokens(tokens)

        // 1. Addition
        assertEquals("10", evalTokens(listOf(MathToken.CharToken('5'), MathToken.CharToken('+'), MathToken.CharToken('5'))))
        assertEquals("17", evalTokens(listOf(MathToken.CharToken('1'), MathToken.CharToken('2'), MathToken.CharToken('+'), MathToken.CharToken('5'))))

        // 2. Subtraction with ASCII '-' and Unicode '−'
        assertEquals("7", evalTokens(listOf(MathToken.CharToken('1'), MathToken.CharToken('2'), MathToken.CharToken('−'), MathToken.CharToken('5'))))
        assertEquals("7", evalTokens(listOf(MathToken.CharToken('1'), MathToken.CharToken('2'), MathToken.CharToken('-'), MathToken.CharToken('5'))))

        // 3. Multiplication with '·', '×', '*'
        assertEquals("60", evalTokens(listOf(MathToken.CharToken('1'), MathToken.CharToken('2'), MathToken.CharToken('·'), MathToken.CharToken('5'))))
        assertEquals("60", evalTokens(listOf(MathToken.CharToken('1'), MathToken.CharToken('2'), MathToken.CharToken('×'), MathToken.CharToken('5'))))
        assertEquals("60", evalTokens(listOf(MathToken.CharToken('1'), MathToken.CharToken('2'), MathToken.CharToken('*'), MathToken.CharToken('5'))))

        // 4. Division with FractionToken
        val fracDiv = listOf(
            MathToken.FractionToken(
                num = listOf(MathToken.CharToken('1'), MathToken.CharToken('2')),
                den = listOf(MathToken.CharToken('4')),
            ),
        )
        assertEquals("3", evalTokens(fracDiv))

        // 5. Division with CharToken '/'
        assertEquals("3", evalTokens(listOf(MathToken.CharToken('1'), MathToken.CharToken('2'), MathToken.CharToken('/'), MathToken.CharToken('4'))))
        assertEquals("3", evalTokens(listOf(MathToken.CharToken('1'), MathToken.CharToken('2'), MathToken.CharToken('÷'), MathToken.CharToken('4'))))
    }

    @Test
    fun `dot multiplication symbol evaluates accurately`() {
        assertEquals("125", ExactMathEngine.evaluateExpressionString("5·5·5"))
        assertEquals("100", ExactMathEngine.evaluateExpressionString("12·8+4"))
        assertEquals("10", ExactMathEngine.evaluateExpressionString("2.5·4"))
        assertEquals("125", ExactMathEngine.evaluateTokens(chars("5·5·5")))
    }

    @Test
    fun `implicit multiplication with parentheses and numbers`() {
        assertEquals("125", ExactMathEngine.evaluateExpressionString("(5)(5)5"))
        assertEquals("125", ExactMathEngine.evaluateExpressionString("5(5)(5)"))
        assertEquals("125", ExactMathEngine.evaluateExpressionString("5(5)5"))
        assertEquals("125", ExactMathEngine.evaluateExpressionString("(5)5(5)"))
        assertEquals("45", ExactMathEngine.evaluateExpressionString("(2+3)(4+5)"))
        assertEquals("70", ExactMathEngine.evaluateExpressionString("2(3+4)5"))
        assertEquals("120", ExactMathEngine.evaluateExpressionString("((2)(3))((4)(5))"))
        assertEquals("1", ExactMathEngine.evaluateExpressionString("(0.5)(0.5)4"))

        // Token-level implicit multiplication: (5)(5)5
        assertEquals("125", ExactMathEngine.evaluateTokens(chars("(5)(5)5")))
        assertEquals("125", ExactMathEngine.evaluateTokens(chars("5(5)(5)")))
        assertEquals("125", KeypadSimulator().type("(5)(5)5").eval())
    }

    @Test
    fun `implicit multiplication between fractions powers parentheses and digits`() {
        // 5 * (3/5) = 3
        assertEquals(
            "3",
            ExactMathEngine.evaluateTokens(
                listOf(MathToken.CharToken('5'), frac(chars("3"), chars("5"))),
            ),
        )
        // (3/5) * 5 = 3
        assertEquals(
            "3",
            ExactMathEngine.evaluateTokens(
                listOf(frac(chars("3"), chars("5")), MathToken.CharToken('5')),
            ),
        )
        // (2/3)(3/2) = 1
        assertEquals(
            "1",
            ExactMathEngine.evaluateTokens(
                listOf(frac(chars("2"), chars("3")), frac(chars("3"), chars("2"))),
            ),
        )
        // (5)(2/5) = 2
        assertEquals(
            "2",
            ExactMathEngine.evaluateTokens(
                chars("(5)") + frac(chars("2"), chars("5")),
            ),
        )
        // 2^3 followed by 5 -> (2^3) * 5 = 40
        assertEquals(
            "40",
            ExactMathEngine.evaluateTokens(
                listOf(MathToken.CharToken('2'), pow(chars("3")), MathToken.CharToken('5')),
            ),
        )
        // 2^3 followed by (5) -> (2^3) * (5) = 40
        assertEquals(
            "40",
            ExactMathEngine.evaluateTokens(
                listOf(MathToken.CharToken('2'), pow(chars("3"))) + chars("(5)"),
            ),
        )
    }

    @Test
    fun `chained exponents - exponent of an exponent`() {
        // 2^(3^2) = 512
        val twoPowThreePowTwo = listOf(
            MathToken.CharToken('2'),
            pow(listOf(MathToken.CharToken('3'), pow(chars("2")))),
        )
        assertEquals("512", ExactMathEngine.evaluateTokens(twoPowThreePowTwo))

        // 2^(5^2) = 33554432
        val twoPowFivePowTwo = listOf(
            MathToken.CharToken('2'),
            pow(listOf(MathToken.CharToken('5'), pow(chars("2")))),
        )
        assertEquals("33554432", ExactMathEngine.evaluateTokens(twoPowFivePowTwo))

        // 2^(2^(2^2)) = 2^16 = 65536 (3 levels of chained exponents)
        val threeLevelPow = listOf(
            MathToken.CharToken('2'),
            pow(
                listOf(
                    MathToken.CharToken('2'),
                    pow(
                        listOf(
                            MathToken.CharToken('2'),
                            pow(chars("2")),
                        ),
                    ),
                ),
            ),
        )
        assertEquals("65536", ExactMathEngine.evaluateTokens(threeLevelPow))

        // 3^(3^3) = 3^27 = 7625597484987
        val threePowThreePowThree = listOf(
            MathToken.CharToken('3'),
            pow(listOf(MathToken.CharToken('3'), pow(chars("3")))),
        )
        assertEquals("7625597484987", ExactMathEngine.evaluateTokens(threePowThreePowThree))

        // 2^(5^5) = 2^3125 (huge exponent)
        val twoPowFivePowFive = listOf(
            MathToken.CharToken('2'),
            pow(listOf(MathToken.CharToken('5'), pow(chars("5")))),
        )
        assertNotNull(ExactMathEngine.evaluateTokens(twoPowFivePowFive))
    }

    @Test
    fun `deeply nested fractions in numerator and denominator`() {
        // (5/5)/5 = 0.2
        val fracInNum = listOf(
            frac(
                num = listOf(frac(chars("5"), chars("5"))),
                den = chars("5"),
            ),
        )
        assertEquals("0.2", ExactMathEngine.evaluateTokens(fracInNum))

        // 5/(5/5) = 5
        val fracInDen = listOf(
            frac(
                num = chars("5"),
                den = listOf(frac(chars("5"), chars("5"))),
            ),
        )
        assertEquals("5", ExactMathEngine.evaluateTokens(fracInDen))

        // (((100/2)/5)/2) = 5 (3 levels in numerator)
        val tripleNumFrac = listOf(
            frac(
                num = listOf(
                    frac(
                        num = listOf(frac(chars("100"), chars("2"))),
                        den = chars("5"),
                    ),
                ),
                den = chars("2"),
            ),
        )
        assertEquals("5", ExactMathEngine.evaluateTokens(tripleNumFrac))

        // 1 / (1 / 8) = 8 (2 levels in denominator)
        val doubleDenFrac = listOf(
            frac(
                num = chars("1"),
                den = listOf(frac(chars("1"), chars("8"))),
            ),
        )
        assertEquals("8", ExactMathEngine.evaluateTokens(doubleDenFrac))

        // 1 / (1 / (1 / 8)) = 0.125 (3 levels in denominator)
        val tripleDenFrac = listOf(
            frac(
                num = chars("1"),
                den = listOf(
                    frac(
                        num = chars("1"),
                        den = listOf(frac(chars("1"), chars("8"))),
                    ),
                ),
            ),
        )
        assertEquals("0.125", ExactMathEngine.evaluateTokens(tripleDenFrac))

        // ((1/2) + (1/3)) / (5/6) = 1
        val complexFrac = listOf(
            frac(
                num = listOf(
                    frac(chars("1"), chars("2")),
                    MathToken.CharToken('+'),
                    frac(chars("1"), chars("3")),
                ),
                den = listOf(frac(chars("5"), chars("6"))),
            ),
        )
        assertEquals("1", ExactMathEngine.evaluateTokens(complexFrac))

        // Continued fraction: 1 + 1 / (1 + 1 / (1 + 1/2)) = 1.6
        val continuedFrac = chars("1+") + frac(
            num = chars("1"),
            den = chars("1+") + frac(
                num = chars("1"),
                den = chars("1+") + frac(chars("1"), chars("2")),
            ),
        )
        assertEquals("1.6", ExactMathEngine.evaluateTokens(continuedFrac))
    }

    @Test
    fun `combined chained fractions and exponents`() {
        // 2^(6/2) = 8
        assertEquals(
            "8",
            ExactMathEngine.evaluateTokens(
                listOf(MathToken.CharToken('2'), pow(listOf(frac(chars("6"), chars("2"))))),
            ),
        )

        // 9^(1/2) = 3
        assertEquals(
            "3",
            ExactMathEngine.evaluateTokens(
                listOf(MathToken.CharToken('9'), pow(listOf(frac(chars("1"), chars("2"))))),
            ),
        )

        // (2^10) / (4^5) = 1
        assertEquals(
            "1",
            ExactMathEngine.evaluateTokens(
                listOf(
                    frac(
                        num = listOf(MathToken.CharToken('2'), pow(chars("10"))),
                        den = listOf(MathToken.CharToken('4'), pow(chars("5"))),
                    ),
                ),
            ),
        )

        // (2/3)^3 · (3/2)^3 = 1
        assertEquals(
            "1",
            ExactMathEngine.evaluateTokens(
                listOf(
                    frac(chars("2"), chars("3")),
                    pow(chars("3")),
                    MathToken.CharToken('·'),
                    frac(chars("3"), chars("2")),
                    pow(chars("3")),
                ),
            ),
        )

        // (2^(3^(4/2))) / 512 = 1
        assertEquals(
            "1",
            ExactMathEngine.evaluateTokens(
                listOf(
                    frac(
                        num = listOf(
                            MathToken.CharToken('2'),
                            pow(
                                listOf(
                                    MathToken.CharToken('3'),
                                    pow(listOf(frac(chars("4"), chars("2")))),
                                ),
                            ),
                        ),
                        den = chars("512"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `keypad simulation - chaining fractions and exponents via button presses`() {
        // 1. Type 5, ÷, 5, ▶, ÷, 5 -> (5/5)/5 = 0.2
        assertEquals(
            "0.2",
            KeypadSimulator()
                .type("5").div().type("5")
                .move("RIGHT").div().type("5")
                .eval(),
        )

        // 2. Type ÷, 5, ÷, 5, ▼, 5 -> (5/5)/5 = 0.2
        assertEquals(
            "0.2",
            KeypadSimulator()
                .div().type("5").div().type("5")
                .move("DOWN").type("5")
                .eval(),
        )

        // 3. Type 5, ÷, 5, ÷, 5 -> 5/(5/5) = 5
        assertEquals(
            "5",
            KeypadSimulator()
                .type("5").div().type("5").div().type("5")
                .eval(),
        )

        // 4. Type 2, xʸ, 3, xʸ, 2 -> 2^(3^2) = 512
        assertEquals(
            "512",
            KeypadSimulator()
                .type("2").pow().type("3").pow().type("2")
                .eval(),
        )

        // 5. Type 9, xʸ, 1, ÷, 2 -> 9^(1/2) = 3
        assertEquals(
            "3",
            KeypadSimulator()
                .type("9").pow().type("1").div().type("2")
                .eval(),
        )

        // 6. Type 2, xʸ, 5, xʸ, 2, ▼, ▼, ·, (5)(5) -> 2^(5^2) * 25 = 838860800
        assertEquals(
            "838860800",
            KeypadSimulator()
                .type("2").pow().type("5").pow().type("2")
                .move("DOWN").move("DOWN")
                .type("·(5)(5)")
                .eval(),
        )

        // 7. Backspace unwrapping inside nested fraction: type 5, ÷, 5, ÷, ⌫ -> back to 5/5 = 1
        assertEquals(
            "1",
            KeypadSimulator()
                .type("5").div().type("5").div().backspace()
                .eval(),
        )
    }

    @Test
    fun `operator precedence and powers`() {
        assertEquals("36", ExactMathEngine.evaluateExpressionString("12 + 8 * 3"))
        assertEquals("4", ExactMathEngine.evaluateExpressionString("(-2)^2"))
        assertEquals("512", ExactMathEngine.evaluateExpressionString("2^(3^2)"))
    }

    @Test
    fun `scientific functions and constants via EvalEx`() {
        // √(81) = 9, and unclosed √(81 also auto-balances to 9
        assertEquals("9", KeypadSimulator().func("√").type("81)").eval())
        assertEquals("9", KeypadSimulator().func("√").type("81").eval())

        // 5 * √(16) = 20 (implicit multiplication before function!)
        assertEquals("20", KeypadSimulator().type("5").func("√").type("16)").eval())

        // sin(30), cos(60), tan(45) (EvalEx trig functions use degrees by default)
        assertEquals("0.5", KeypadSimulator().func("sin").type("30)").eval())
        assertEquals("0.5", KeypadSimulator().func("cos").type("60)").eval())
        assertEquals("1", KeypadSimulator().func("tan").type("45)").eval())

        // asin(0.5) = 30, acos(0.5) = 60, atan(1) = 45
        assertEquals("30", KeypadSimulator().func("asin").type("0.5)").eval())
        assertEquals("60", KeypadSimulator().func("acos").type("0.5)").eval())
        assertEquals("45", KeypadSimulator().func("atan").type("1)").eval())

        // log(1000) = 3 (base-10), ln(e) = 1
        assertEquals("3", KeypadSimulator().func("log").type("1000)").eval())
        assertEquals("1", KeypadSimulator().func("ln").type("e)").eval())

        // abs(-42) = 42, fact(5) = 120
        assertEquals("42", KeypadSimulator().func("abs").type("−42)").eval())
        assertEquals("120", KeypadSimulator().func("fact").type("5)").eval())

        // Constants and percent: 50% * 200 = 100, 2π / π = 2
        assertEquals("100", KeypadSimulator().type("50%·200").eval())
        assertEquals("2", KeypadSimulator().type("2π").div().type("π").eval())
    }

    @Test
    fun `2D SqrtToken evaluation with numbers fitting under the wing`() {
        // √81 = 9
        val sqrt81 = listOf(MathToken.SqrtToken(rad = "81".map { MathToken.CharToken(it) }))
        assertEquals("9", ExactMathEngine.evaluateTokens(sqrt81))

        // 5 * √16 = 20 (implicit multiply)
        val fiveSqrt16 = listOf(
            MathToken.CharToken('5'),
            MathToken.SqrtToken(rad = "16".map { MathToken.CharToken(it) }),
        )
        assertEquals("20", ExactMathEngine.evaluateTokens(fiveSqrt16))

        // Nested radical: √(√16) = 2
        val nestedSqrt = listOf(
            MathToken.SqrtToken(
                rad = listOf(
                    MathToken.SqrtToken(rad = "16".map { MathToken.CharToken(it) }),
                ),
            ),
        )
        assertEquals("2", ExactMathEngine.evaluateTokens(nestedSqrt))

        // Radical with internal sum: √(16 + 9) = 5
        val sqrtSum = listOf(
            MathToken.SqrtToken(
                rad = listOf(
                    MathToken.CharToken('1'),
                    MathToken.CharToken('6'),
                    MathToken.CharToken('+'),
                    MathToken.CharToken('9'),
                ),
            ),
        )
        assertEquals("5", ExactMathEngine.evaluateTokens(sqrtSum))
    }

    @Test
    fun `division by zero returns null safely`() {
        assertNull(ExactMathEngine.evaluateExpressionString("10 / 0"))
    }
}
