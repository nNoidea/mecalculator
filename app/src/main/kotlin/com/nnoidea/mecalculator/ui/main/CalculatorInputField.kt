package com.nnoidea.mecalculator.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nnoidea.mecalculator.data.NumberGrouping
import com.nnoidea.mecalculator.ui.util.HapticHelper

/**
 * The active editor deck viewport for building and evaluating mathematical expressions:
 * - Wraps the single-source-of-truth [MathViewport]
 * - Blinking cursor positioning and auto-framing when typing
 * - 26.sp interactive tokens with slot navigation
 */
@Composable
internal fun CalculatorInputField(
    tokens: List<MathToken>,
    cursor: CursorState,
    inHistory: Boolean,
    numberGrouping: NumberGrouping,
    onFocusInput: () -> Unit,
    onTokenClick: (CursorState) -> Unit,
    modifier: Modifier = Modifier,
    resetZoomTrigger: Int = 0,
    initialZoomScale: Float = 1f,
) {
    val view = LocalView.current
    val density = LocalDensity.current

    val padX = with(density) { 12.dp.toPx() }
    val padY = with(density) { 8.dp.toPx() }
    val rightMargin = with(density) { 24.dp.toPx() }
    val bottomMargin = with(density) { 12.dp.toPx() }

    val viewportState = rememberMathViewportState(padX, padY)

    LaunchedEffect(initialZoomScale) {
        if (initialZoomScale != 1f) {
            viewportState.scale = initialZoomScale
            viewportState.panOffset = viewportState.clampOffset(viewportState.panOffset, initialZoomScale)
        }
    }

    var cursorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var positionedCursor by remember { mutableStateOf<CursorState?>(null) }
    var needsAutoFrame by remember { mutableStateOf(true) }

    // Request auto-framing whenever cursor or tokens change
    LaunchedEffect(cursor, tokens) {
        needsAutoFrame = true
    }

    // Reset zoom when tokens become empty (cleared)
    LaunchedEffect(tokens.isEmpty()) {
        if (tokens.isEmpty()) {
            viewportState.scale = 1f
            viewportState.panOffset = viewportState.clampOffset(Offset(padX, padY), 1f)
            needsAutoFrame = true
        }
    }

    // Reset zoom when explicit reset trigger fires (equals pressed or long-press clear)
    LaunchedEffect(resetZoomTrigger) {
        if (resetZoomTrigger > 0) {
            viewportState.scale = 1f
            viewportState.panOffset = viewportState.clampOffset(Offset(padX, padY), 1f)
            needsAutoFrame = true
        }
    }

    // Auto-frame active cursor when requested and layout coordinates are ready for the current cursor
    LaunchedEffect(
        needsAutoFrame,
        cursorCoordinates,
        positionedCursor,
        viewportState.viewportCoordinates,
        viewportState.contentSize,
    ) {
        if (!needsAutoFrame) return@LaunchedEffect
        if (positionedCursor != cursor) return@LaunchedEffect
        val vCoords = viewportState.viewportCoordinates
        val cCoords = cursorCoordinates
        if (vCoords != null && vCoords.isAttached && cCoords != null && cCoords.isAttached &&
            viewportState.viewportSize.width > 0 && viewportState.viewportSize.height > 0 &&
            viewportState.contentSize.width > 0 && viewportState.contentSize.height > 0
        ) {
            val cursorInViewport = vCoords.localPositionOf(cCoords, Offset.Zero)
            val cursorW = cCoords.size.width.toFloat()
            val cursorH = cCoords.size.height.toFloat()
            val viewW = viewportState.viewportSize.width.toFloat()
            val viewH = viewportState.viewportSize.height.toFloat()

            val delta = computeAutoFrameDelta(
                cursorX = cursorInViewport.x,
                cursorY = cursorInViewport.y,
                cursorW = cursorW,
                cursorH = cursorH,
                viewW = viewW,
                viewH = viewH,
                padX = padX,
                rightMargin = rightMargin,
                padY = padY,
                bottomMargin = bottomMargin,
            )

            val targetPan = viewportState.panOffset + Offset(delta.deltaX, delta.deltaY)
            val clamped = viewportState.clampOffset(targetPan, viewportState.scale)
            if (kotlin.math.abs(clamped.x - viewportState.panOffset.x) > 0.5f ||
                kotlin.math.abs(clamped.y - viewportState.panOffset.y) > 0.5f
            ) {
                viewportState.panOffset = clamped
            }
            needsAutoFrame = false
        }
    }

    MathViewport(
        state = viewportState,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    HapticHelper.performClick(view)
                    onFocusInput()
                },
            ),
        allowVerticalPan = true,
        contentAlignment = Alignment.TopStart,
        showResetButton = true,
        resetButtonAlignment = Alignment.TopEnd,
        resetButtonPadding = 8.dp,
        onResetZoom = {
            needsAutoFrame = true
        },
    ) {
        MathTokensRow(
            tokens = tokens,
            pathPrefix = emptyList(),
            numberGrouping = numberGrouping,
            cursor = cursor,
            showCursor = !inHistory,
            fontSize = 26.sp,
            contentColor = MaterialTheme.colorScheme.onSurface,
            accentColor = MaterialTheme.colorScheme.primary,
            onTokenClick = { newCursor ->
                HapticHelper.performClick(view)
                onTokenClick(newCursor)
            },
            onCursorPositioned = { coordinates ->
                cursorCoordinates = coordinates
                positionedCursor = cursor
            },
        )
    }
}
