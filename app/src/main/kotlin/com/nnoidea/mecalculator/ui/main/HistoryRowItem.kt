package com.nnoidea.mecalculator.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nnoidea.mecalculator.data.NumberGrouping
import com.nnoidea.mecalculator.domain.NumberFormatter

/**
 * Checks whether a history entry is stacked (formula on top, result underneath)
 * or inline (formula on left, result on right).
 */
internal fun isHistoryEntryStacked(
    entry: HistoryEntry,
    numberGrouping: NumberGrouping,
    maxDecimalPlaces: Int,
    availableWidthPx: Float,
    charWidthPx: Float,
    charHeightPx: Float,
    density: Density,
    textMeasurer: TextMeasurer,
): Boolean {
    if (availableWidthPx <= 0f) return false

    val formulaTokensSize = calculateTokensNaturalSize(
        tokens = entry.tokens,
        numberGrouping = numberGrouping,
        charWidthPx = charWidthPx,
        charHeightPx = charHeightPx,
        density = density,
    )
    val formulaPadX = with(density) { 10.dp.toPx() }
    val formulaTotalW = formulaTokensSize.width + (2 * formulaPadX).toInt() + 4

    val formattedResult = NumberFormatter.formatResultString(
        raw = entry.result,
        grouping = numberGrouping,
        maxDecimalPlaces = maxDecimalPlaces,
    )
    val resultLayout = textMeasurer.measure(
        text = formattedResult,
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            platformStyle = PlatformTextStyle(
                includeFontPadding = false,
            ),
        ),
        maxLines = 1,
        softWrap = false,
    )
    val resultPadX = with(density) { 12.dp.toPx() }
    val resultTotalW = resultLayout.size.width + (2 * resultPadX).toInt()

    val spacingPx = with(density) { 10.dp.toPx() }
    return (formulaTotalW + resultTotalW + spacingPx) > availableWidthPx
}

@Composable
internal fun HistoryRowItem(
    entry: HistoryEntry,
    numberGrouping: NumberGrouping,
    maxDecimalPlaces: Int,
    isRowSelected: Boolean,
    isLeftSelected: Boolean,
    isRightSelected: Boolean,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit,
) {
    val rowBg by animateColorAsState(
        targetValue = if (isRowSelected) {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
        } else {
            Color.Transparent
        },
        label = "rowBg",
    )

    val resultTokens = remember(entry.result, numberGrouping, maxDecimalPlaces) {
        NumberFormatter.formatResultTokens(
            raw = entry.result,
            grouping = numberGrouping,
            maxDecimalPlaces = maxDecimalPlaces,
        )
    }

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val charSample17 = remember(textMeasurer) {
        textMeasurer.measure(
            text = "0",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false,
                ),
            ),
        )
    }

    val charWidthPx = charSample17.size.width.toFloat()
    val charHeightPx = charSample17.size.height.toFloat()

    val estimatedFormulaContentSize = remember(entry.tokens, numberGrouping, charWidthPx, charHeightPx) {
        calculateTokensNaturalSize(
            tokens = entry.tokens,
            numberGrouping = numberGrouping,
            charWidthPx = charWidthPx,
            charHeightPx = charHeightPx,
            density = density,
        )
    }

    val estimatedResultContentSize = remember(resultTokens, numberGrouping, charWidthPx, charHeightPx) {
        calculateTokensNaturalSize(
            tokens = resultTokens,
            numberGrouping = numberGrouping,
            charWidthPx = charWidthPx * (15f / 17f),
            charHeightPx = charHeightPx * (15f / 17f),
            density = density,
        )
    }

    var measuredFormulaW by remember { mutableIntStateOf(0) }
    var measuredFormulaH by remember { mutableIntStateOf(0) }
    var measuredResultW by remember { mutableIntStateOf(0) }
    var measuredResultH by remember { mutableIntStateOf(0) }

    val formulaPadX = with(density) { 10.dp.toPx() }
    val formulaPadY = with(density) { 6.dp.toPx() }
    val resultPadX = with(density) { 12.dp.toPx() }

    val formulaContentW = if (measuredFormulaW > 0) measuredFormulaW else estimatedFormulaContentSize.width
    val formulaContentH = if (measuredFormulaH > 0) measuredFormulaH else estimatedFormulaContentSize.height
    val resultContentW = if (measuredResultW > 0) measuredResultW else estimatedResultContentSize.width

    val formulaTotalWidthPx = formulaContentW + (2 * formulaPadX).toInt() + 4
    val formulaTotalHeightPx = formulaContentH + (2 * formulaPadY).toInt()
    val resultTotalWidthPx = resultContentW + (2 * resultPadX).toInt()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        val totalAvailableWidth = maxWidth
        val availableWidthPx = with(density) { totalAvailableWidth.toPx() }
        val spacingPx = with(density) { 10.dp.toPx() }

        // Formula and Result fit horizontally side-by-side if their total width fits within available width.
        val canFitInline = (formulaTotalWidthPx + resultTotalWidthPx + spacingPx <= availableWidthPx)

        val formulaHeightDp = with(density) {
            formulaTotalHeightPx
                .coerceIn(with(density) { 36.dp.toPx() }.toInt(), with(density) { 110.dp.toPx() }.toInt())
                .toDp()
        }

        if (canFitInline) {
            // INLINE: Formula on the left, Result on the right. Both unclipped and natural width!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HistoryItemViewport(
                    tokens = entry.tokens,
                    numberGrouping = numberGrouping,
                    style = HistoryItemStyle.FORMULA,
                    isSelected = isLeftSelected,
                    onSelect = onSelectLeft,
                    modifier = Modifier
                        .width(with(density) { formulaTotalWidthPx.toDp() })
                        .height(formulaHeightDp),
                    onNaturalSizeMeasured = { w, h ->
                        measuredFormulaW = w
                        measuredFormulaH = h
                    },
                )

                HistoryItemViewport(
                    tokens = resultTokens,
                    numberGrouping = numberGrouping,
                    style = HistoryItemStyle.RESULT,
                    isSelected = isRightSelected,
                    onSelect = onSelectRight,
                    modifier = Modifier
                        .width(
                            with(density) {
                                resultTotalWidthPx
                                    .coerceAtLeast(with(density) { 44.dp.toPx() }.toInt())
                                    .toDp()
                            }
                        )
                        .height(34.dp),
                    onNaturalSizeMeasured = { w, h ->
                        measuredResultW = w
                        measuredResultH = h
                    },
                )
            }
        } else {
            // STACKED: Formula takes full horizontal width on top, answer block sits UNDER with separate '=' prefix
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HistoryItemViewport(
                    tokens = entry.tokens,
                    numberGrouping = numberGrouping,
                    style = HistoryItemStyle.FORMULA,
                    isSelected = isLeftSelected,
                    onSelect = onSelectLeft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(formulaHeightDp),
                    onNaturalSizeMeasured = { w, h ->
                        measuredFormulaW = w
                        measuredFormulaH = h
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "=",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        fontFamily = FontFamily.Monospace,
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val maxStackedResultWidthPx = (availableWidthPx - with(density) { 32.dp.toPx() }).toInt()
                    val stackedResultWidthDp = with(density) {
                        resultTotalWidthPx
                            .coerceIn(with(density) { 44.dp.toPx() }.toInt(), maxStackedResultWidthPx.coerceAtLeast(1))
                            .toDp()
                    }

                    HistoryItemViewport(
                        tokens = resultTokens,
                        numberGrouping = numberGrouping,
                        style = HistoryItemStyle.RESULT,
                        isSelected = isRightSelected,
                        onSelect = onSelectRight,
                        modifier = Modifier
                            .width(stackedResultWidthDp)
                            .height(34.dp),
                        onNaturalSizeMeasured = { w, h ->
                            measuredResultW = w
                            measuredResultH = h
                        },
                    )
                }
            }
        }
    }
}
