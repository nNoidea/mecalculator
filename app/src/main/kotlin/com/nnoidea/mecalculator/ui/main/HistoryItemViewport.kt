package com.nnoidea.mecalculator.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nnoidea.mecalculator.data.NumberGrouping
import com.nnoidea.mecalculator.ui.util.HapticHelper

enum class HistoryItemStyle {
    FORMULA,
    RESULT,
}

/**
 * Unified Single-Source-of-Truth history item display for both:
 * 1. [HistoryItemStyle.FORMULA]: Expressions with outline selection border & first-digit baseline centering.
 * 2. [HistoryItemStyle.RESULT]: Evaluated answer pill with solid tint and 2D math layout.
 */
@Composable
internal fun HistoryItemViewport(
    tokens: List<MathToken>,
    numberGrouping: NumberGrouping,
    style: HistoryItemStyle,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    onNaturalSizeMeasured: ((width: Int, height: Int) -> Unit)? = null,
) {
    val view = LocalView.current
    val density = LocalDensity.current

    val isResult = style == HistoryItemStyle.RESULT
    val padX = with(density) { (if (isResult) 12.dp else 10.dp).toPx() }
    val padY = with(density) { (if (isResult) 4.dp else 6.dp).toPx() }

    val viewportState = rememberMathViewportState(padX, padY, centerHorizontalWhenFits = isResult)

    fun computeDefaultPan(vpSize: IntSize, cSize: IntSize, currentScale: Float): Offset {
        val viewW = (vpSize.width.toFloat() - 2 * padX).coerceAtLeast(0f)
        val viewH = (vpSize.height.toFloat() - 2 * padY).coerceAtLeast(0f)
        if (viewH <= 0f || cSize.height <= 0) {
            return viewportState.clampOffset(Offset(padX, padY), currentScale)
        }

        val scaledW = cSize.width.toFloat() * currentScale
        val scaledH = cSize.height.toFloat() * currentScale

        val defaultX = if (scaledW <= viewW && isResult) {
            (vpSize.width.toFloat() - scaledW) / 2f
        } else {
            padX
        }

        val clampedY = if (scaledH <= viewH) {
            (vpSize.height.toFloat() - scaledH) / 2f
        } else if (style == HistoryItemStyle.FORMULA) {
            val charHeightPx = with(density) { 20.sp.toPx() }
            val firstDigitCenterY = findFirstDigitCenterY(
                tokens = tokens,
                totalContentHeight = cSize.height.toFloat(),
                charHeightPx = charHeightPx,
                density = density,
            )
            val scaledFirstDigitY = firstDigitCenterY * currentScale
            val targetPanY = (vpSize.height.toFloat() / 2f) - scaledFirstDigitY
            val minPanY = padY - (scaledH - viewH)
            val maxPanY = padY
            targetPanY.coerceIn(minPanY, maxPanY)
        } else {
            (vpSize.height.toFloat() - scaledH) / 2f
        }

        return Offset(defaultX, clampedY)
    }

    LaunchedEffect(tokens) {
        viewportState.scale = 1f
        if (viewportState.viewportSize.width > 0 && viewportState.viewportSize.height > 0 &&
            viewportState.contentSize.width > 0 && viewportState.contentSize.height > 0
        ) {
            viewportState.panOffset = computeDefaultPan(
                viewportState.viewportSize,
                viewportState.contentSize,
                1f,
            )
        }
    }

    val cornerRadius by animateDpAsState(
        targetValue = when (style) {
            HistoryItemStyle.FORMULA -> if (isSelected) 12.dp else 16.dp
            HistoryItemStyle.RESULT -> if (isSelected) 10.dp else 14.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "historyItemCorner",
    )

    val bgColor by animateColorAsState(
        targetValue = when (style) {
            HistoryItemStyle.FORMULA -> if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f)
            }
            HistoryItemStyle.RESULT -> if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            }
        },
        label = "historyItemBg",
    )

    val contentColor by animateColorAsState(
        targetValue = when (style) {
            HistoryItemStyle.FORMULA -> if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            HistoryItemStyle.RESULT -> if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            }
        },
        label = "historyItemFg",
    )

    val accentColor by animateColorAsState(
        targetValue = when (style) {
            HistoryItemStyle.FORMULA -> if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            HistoryItemStyle.RESULT -> if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            }
        },
        label = "historyItemAccent",
    )

    val borderColor = when (style) {
        HistoryItemStyle.FORMULA -> if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        HistoryItemStyle.RESULT -> Color.Transparent
    }
    val borderWidth = when (style) {
        HistoryItemStyle.FORMULA -> if (isSelected) 1.5.dp else 0.dp
        HistoryItemStyle.RESULT -> 0.dp
    }

    val allowVerticalPan = isSelected && (viewportState.contentSize.height * viewportState.scale >
        (viewportState.viewportSize.height - 2 * padY).coerceAtLeast(0f))

    val fontSize = if (style == HistoryItemStyle.FORMULA) 17.sp else 15.sp

    MathViewport(
        state = viewportState,
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius),
            )
            .background(bgColor),
        allowVerticalPan = allowVerticalPan,
        contentAlignment = Alignment.TopStart,
        onTap = {
            HapticHelper.performClick(view)
            onSelect()
        },
        showResetButton = true,
        resetButtonAlignment = if (style == HistoryItemStyle.FORMULA) Alignment.TopEnd else Alignment.CenterEnd,
        resetButtonSize = if (style == HistoryItemStyle.FORMULA) 24.dp else 20.dp,
        resetButtonIconSize = if (style == HistoryItemStyle.FORMULA) 14.dp else 12.dp,
        resetButtonPadding = if (style == HistoryItemStyle.FORMULA) 6.dp else 4.dp,
        onContentSizeChanged = { size ->
            onNaturalSizeMeasured?.invoke(size.width, size.height)
            if (viewportState.viewportSize.width > 0 && viewportState.viewportSize.height > 0) {
                viewportState.panOffset = computeDefaultPan(
                    viewportState.viewportSize,
                    size,
                    viewportState.scale,
                )
            }
        },
    ) {
        MathTokensRow(
            tokens = tokens,
            pathPrefix = emptyList(),
            numberGrouping = numberGrouping,
            cursor = CursorState(),
            showCursor = false,
            fontSize = fontSize,
            contentColor = contentColor,
            accentColor = accentColor,
            wrapTokens = false,
            onTokenClick = {
                HapticHelper.performClick(view)
                onSelect()
            },
        )
    }
}
