package com.nnoidea.mecalculator.ui.main

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nnoidea.mecalculator.data.NumberGrouping
import com.nnoidea.mecalculator.domain.NumberFormatter

internal fun calculateTokensNaturalSize(
    tokens: List<MathToken>,
    numberGrouping: NumberGrouping,
    charWidthPx: Float,
    charHeightPx: Float,
    density: Density,
    isSubtree: Boolean = false,
): IntSize {
    if (tokens.isEmpty()) return IntSize(0, charHeightPx.toInt())

    val tokenCharW = if (isSubtree) charWidthPx * (13f / 17f) else charWidthPx
    val tokenCharH = if (isSubtree) charHeightPx * (13f / 17f) else charHeightPx
    val tokenPadX = with(density) { 1.dp.toPx() }

    val levelChars = tokens.map { tok ->
        if (tok is MathToken.CharToken) tok.ch else '#'
    }
    val leadingSeps = NumberFormatter.getLeadingSeparatorsForCharList(levelChars, numberGrouping)

    var totalW = 0f
    var maxH = tokenCharH

    for (i in tokens.indices) {
        val prefixSep = leadingSeps.getOrElse(i) { "" }
        if (prefixSep.isNotEmpty()) {
            totalW += prefixSep.length * tokenCharW
        }

        when (val tok = tokens[i]) {
            is MathToken.CharToken -> {
                totalW += tokenCharW + tokenPadX
                if (tokenCharH > maxH) maxH = tokenCharH
            }
            is MathToken.FuncToken -> {
                val funcStrLen = tok.name.length + 1
                totalW += funcStrLen * tokenCharW + tokenPadX
                if (tokenCharH > maxH) maxH = tokenCharH
            }
            is MathToken.FractionToken -> {
                val numSize = calculateTokensNaturalSize(
                    tokens = tok.num,
                    numberGrouping = numberGrouping,
                    charWidthPx = charWidthPx,
                    charHeightPx = charHeightPx,
                    density = density,
                    isSubtree = true,
                )
                val denSize = calculateTokensNaturalSize(
                    tokens = tok.den,
                    numberGrouping = numberGrouping,
                    charWidthPx = charWidthPx,
                    charHeightPx = charHeightPx,
                    density = density,
                    isSubtree = true,
                )
                val fracPadX = with(density) { 8.dp.toPx() }
                val barThickness = with(density) { 1.5.dp.toPx() }
                val fracPadY = with(density) { 4.dp.toPx() }

                val fracW = kotlin.math.max(numSize.width, denSize.width) + fracPadX
                val fracH = numSize.height + denSize.height + barThickness + fracPadY
                totalW += fracW
                if (fracH > maxH) maxH = fracH
            }
            is MathToken.PowerToken -> {
                val expSize = calculateTokensNaturalSize(
                    tokens = tok.exp,
                    numberGrouping = numberGrouping,
                    charWidthPx = charWidthPx,
                    charHeightPx = charHeightPx,
                    density = density,
                    isSubtree = true,
                )
                val expPadX = with(density) { 2.dp.toPx() }
                val powerOffset = with(density) { (17f * 0.36f).dp.toPx() }

                val powW = expSize.width + expPadX
                val powH = tokenCharH + powerOffset
                totalW += powW
                if (powH > maxH) maxH = powH
            }
            is MathToken.SqrtToken -> {
                val radSize = calculateTokensNaturalSize(
                    tokens = tok.rad,
                    numberGrouping = numberGrouping,
                    charWidthPx = charWidthPx,
                    charHeightPx = charHeightPx,
                    density = density,
                    isSubtree = true,
                )
                val radLeftPadding = with(density) { 2.dp.toPx() }
                val rightOverhang = with(density) { 3.dp.toPx() }
                val topGap = with(density) { 2.5.dp.toPx() }
                val strokePx = with(density) { 1.6.dp.toPx() }
                val symW = (radSize.height * 0.38f).coerceIn(
                    with(density) { 10.dp.toPx() },
                    with(density) { 24.dp.toPx() },
                )
                val sqrtW = symW + radLeftPadding + radSize.width + rightOverhang
                val sqrtH = radSize.height + topGap + strokePx + with(density) { 1.dp.toPx() }
                totalW += sqrtW
                if (sqrtH > maxH) maxH = sqrtH
            }
        }
    }

    return IntSize(kotlin.math.ceil(totalW).toInt(), kotlin.math.ceil(maxH).toInt())
}

/**
 * Finds the relative vertical center (Y in px) of the first digit of an expression
 * within the unscaled content height of MathTokensRow.
 */
internal fun findFirstDigitCenterY(
    tokens: List<MathToken>,
    totalContentHeight: Float,
    charHeightPx: Float,
    density: Density,
): Float {
    if (tokens.isEmpty()) return totalContentHeight / 2f

    return when (val first = tokens.first()) {
        is MathToken.CharToken, is MathToken.FuncToken, is MathToken.PowerToken -> {
            totalContentHeight / 2f
        }
        is MathToken.FractionToken -> {
            val numSize = calculateTokensNaturalSize(
                tokens = first.num,
                numberGrouping = NumberGrouping.NONE,
                charWidthPx = charHeightPx * 0.6f,
                charHeightPx = charHeightPx,
                density = density,
                isSubtree = true,
            )
            val denSize = calculateTokensNaturalSize(
                tokens = first.den,
                numberGrouping = NumberGrouping.NONE,
                charWidthPx = charHeightPx * 0.6f,
                charHeightPx = charHeightPx,
                density = density,
                isSubtree = true,
            )
            val barThickness = with(density) { 1.5.dp.toPx() }
            val fracPadY = with(density) { 4.dp.toPx() }
            val fracH = numSize.height + denSize.height + barThickness + fracPadY

            val fracTopInRow = (totalContentHeight - fracH) / 2f
            val numFirstDigitY = findFirstDigitCenterY(
                tokens = first.num,
                totalContentHeight = numSize.height.toFloat(),
                charHeightPx = charHeightPx,
                density = density,
            )
            fracTopInRow + numFirstDigitY
        }
        is MathToken.SqrtToken -> {
            val radSize = calculateTokensNaturalSize(
                tokens = first.rad,
                numberGrouping = NumberGrouping.NONE,
                charWidthPx = charHeightPx * 0.6f,
                charHeightPx = charHeightPx,
                density = density,
                isSubtree = true,
            )
            val topGap = with(density) { 2.5.dp.toPx() }
            val strokePx = with(density) { 1.6.dp.toPx() }
            val topPad = topGap + strokePx
            val sqrtH = radSize.height + topPad + with(density) { 1.dp.toPx() }
            val sqrtTopInRow = (totalContentHeight - sqrtH) / 2f
            val radFirstDigitY = findFirstDigitCenterY(
                tokens = first.rad,
                totalContentHeight = radSize.height.toFloat(),
                charHeightPx = charHeightPx,
                density = density,
            )
            sqrtTopInRow + topPad + radFirstDigitY
        }
    }
}
