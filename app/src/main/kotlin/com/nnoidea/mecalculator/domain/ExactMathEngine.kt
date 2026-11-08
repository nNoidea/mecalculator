package com.nnoidea.mecalculator.domain

import com.ezylang.evalex.Expression
import com.ezylang.evalex.config.ExpressionConfiguration
import com.nnoidea.mecalculator.ui.main.MathToken
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Thin wrapper around open-source **EvalEx** (`com.ezylang:EvalEx`).
 * All expression parsing, operator precedence, parentheses, scientific functions,
 * and 34-digit IEEE-754R `BigDecimal` (`DECIMAL128`) evaluation are handled directly by EvalEx.
 */
object ExactMathEngine {

    private val EVALEX_CONFIG = ExpressionConfiguration.builder()
        .mathContext(MathContext.DECIMAL128)
        .stripTrailingZeros(true)
        .build()

    private val EULER_E = BigDecimal("2.718281828459045235360287471352662")

    private val IMPLICIT_MUL_PAREN_TO_PAREN = Regex("""\)(?=\()""")
    private val IMPLICIT_MUL_NUM_BEFORE_PAREN = Regex("""(?<![A-Za-z_0-9.])(\d+(?:\.\d+)?|\.\d+)(?=\()""")
    private val IMPLICIT_MUL_AFTER_PAREN = Regex("""(?<=\))(?=[\d.])""")
    private val SUPERSCRIPT_RUN = Regex("""[⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻]+""")

    private fun normalizeSuperscripts(input: String): String {
        return SUPERSCRIPT_RUN.replace(input) { match ->
            val mapped = match.value.map { ch ->
                when (ch) {
                    '⁰' -> '0'; '¹' -> '1'; '²' -> '2'; '³' -> '3'; '⁴' -> '4'
                    '⁵' -> '5'; '⁶' -> '6'; '⁷' -> '7'; '⁸' -> '8'; '⁹' -> '9'
                    '⁻' -> '-'; '⁺' -> '+'; else -> ch
                }
            }.joinToString("")
            "^($mapped)"
        }
    }

    fun evaluateTokens(tokens: List<MathToken>): String? {
        val expr = tokensToExpressionString(tokens).trim()
        if (expr.isEmpty()) return null
        return evaluateExpressionString(expr)
    }

    fun tokensToExpressionString(tokens: List<MathToken>): String {
        val raw = buildString {
            for (i in tokens.indices) {
                val tok = tokens[i]
                if (i > 0 && needsImplicitMultiply(tokens[i - 1], tok)) {
                    append("*")
                }
                when (tok) {
                    is MathToken.CharToken -> when (tok.ch) {
                        '×', '·', '⋅', '•' -> append("*")
                        '÷' -> append("/")
                        '−' -> append("-")
                        'π' -> append("(PI)")
                        'e' -> append("(e)")
                        '%' -> append("/100")
                        '√' -> append("SQRT(")
                        else -> append(tok.ch)
                    }
                    is MathToken.FuncToken -> {
                        val evalExName = when (tok.name) {
                            "√" -> "SQRT"
                            "sin" -> "SIN"
                            "cos" -> "COS"
                            "tan" -> "TAN"
                            "asin" -> "ASIN"
                            "acos" -> "ACOS"
                            "atan" -> "ATAN"
                            "log" -> "LOG10"
                            "ln" -> "LOG"
                            "abs" -> "ABS"
                            "fact" -> "FACT"
                            else -> tok.name.uppercase()
                        }
                        append(evalExName).append("(")
                    }
                    is MathToken.FractionToken -> {
                        val n = tokensToExpressionString(tok.num).trim()
                        val d = tokensToExpressionString(tok.den).trim()
                        if (n.isEmpty() || d.isEmpty()) return ""
                        append("(($n)/($d))")
                    }
                    is MathToken.PowerToken -> {
                        val e = tokensToExpressionString(tok.exp).trim()
                        if (e.isEmpty()) return ""
                        append("^($e)")
                    }
                    is MathToken.SqrtToken -> {
                        val r = tokensToExpressionString(tok.rad).trim()
                        if (r.isEmpty()) return ""
                        append("SQRT($r)")
                    }
                }
            }
        }
        return balanceParentheses(raw)
    }

    private fun balanceParentheses(expr: String): String {
        if (expr.isEmpty()) return expr
        var open = 0
        for (ch in expr) {
            if (ch == '(') open++
            else if (ch == ')' && open > 0) open--
        }
        return if (open > 0) expr + ")".repeat(open) else expr
    }

    private fun needsImplicitMultiply(prev: MathToken, curr: MathToken): Boolean {
        val prevIsDigitOrDot = prev is MathToken.CharToken && (prev.ch.isDigit() || prev.ch == '.')
        val currIsDigitOrDot = curr is MathToken.CharToken && (curr.ch.isDigit() || curr.ch == '.')
        if (prevIsDigitOrDot && currIsDigitOrDot) {
            return false
        }

        val prevIsValueEnd = when (prev) {
            is MathToken.CharToken ->
                prev.ch.isDigit() || prev.ch == '.' || prev.ch == ')' ||
                    prev.ch == 'π' || prev.ch == 'e' || prev.ch == '%'
            is MathToken.FractionToken, is MathToken.PowerToken, is MathToken.SqrtToken -> true
            is MathToken.FuncToken -> false
        }
        val currIsValueStart = when (curr) {
            is MathToken.CharToken ->
                curr.ch.isDigit() || curr.ch == '.' || curr.ch == '(' ||
                    curr.ch == 'π' || curr.ch == 'e'
            is MathToken.FractionToken, is MathToken.FuncToken, is MathToken.SqrtToken -> true
            is MathToken.PowerToken -> false
        }
        return prevIsValueEnd && currIsValueStart
    }

    fun evaluateExpressionString(raw: String): String? = try {
        val normalized = balanceParentheses(
            normalizeSuperscripts(raw)
                .replace("×", "*")
                .replace("·", "*")
                .replace("⋅", "*")
                .replace("•", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("π", "(PI)")
                .replace("%", "/100")
                .replace(" ", "")
                .replace(",", "")
                .trim()
                .let { IMPLICIT_MUL_PAREN_TO_PAREN.replace(it, ")*") }
                .let { IMPLICIT_MUL_NUM_BEFORE_PAREN.replace(it, "$1*") }
                .let { IMPLICIT_MUL_AFTER_PAREN.replace(it, "*") },
        )

        if (normalized.isEmpty()) {
            null
        } else {
            val result = Expression(normalized, EVALEX_CONFIG)
                .with("e", EULER_E)
                .evaluate()
                .numberValue
            formatResult(result)
        }
    } catch (_: Exception) {
        null
    }

    private fun formatResult(value: BigDecimal): String {
        val stripped = value
            .round(MathContext(14, RoundingMode.HALF_UP))
            .stripTrailingZeros()
        val abs = stripped.abs()
        return if (abs == BigDecimal.ZERO || (abs >= BigDecimal("0.000001") && abs < BigDecimal("1000000000000000"))) {
            stripped.toPlainString()
        } else {
            stripped.toString()
        }
    }
}
