package com.nnoidea.mecalculator.ui.main

/**
 * 2D Hierarchical Math Token AST.
 * Represents the mathematical expressions rendered recursively by the layout engine.
 */
sealed interface MathToken {
    data class CharToken(val ch: Char) : MathToken
    data class FuncToken(val name: String) : MathToken
    data class FractionToken(
        val num: List<MathToken>,
        val den: List<MathToken>,
    ) : MathToken
    data class PowerToken(
        val exp: List<MathToken>,
    ) : MathToken
    data class SqrtToken(
        val rad: List<MathToken>,
    ) : MathToken
}

enum class SlotType {
    FRAC_NUM,
    FRAC_DEN,
    POW_EXP,
    SQRT_RAD,
}

data class CursorStep(
    val tokenIndex: Int,
    val slot: SlotType,
)

data class CursorState(
    val path: List<CursorStep> = emptyList(),
    val offset: Int = 0,
)
