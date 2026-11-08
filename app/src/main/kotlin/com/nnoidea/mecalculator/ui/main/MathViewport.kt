package com.nnoidea.mecalculator.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.nnoidea.mecalculator.ui.util.HapticHelper
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * State holder for a zoomable and pannable mathematical viewport.
 * Encapsulates scale, panOffset, coordinate measurements, clamping, and inertial fling physics.
 */
@Stable
class MathViewportState(
    val padX: Float,
    val padY: Float,
    val centerHorizontalWhenFits: Boolean = false,
) {
    var scale by mutableFloatStateOf(1f)
    var panOffset by mutableStateOf(Offset(padX, padY))

    var viewportCoordinates by mutableStateOf<LayoutCoordinates?>(null)
    var viewportSize by mutableStateOf(IntSize.Zero)
    var contentSize by mutableStateOf(IntSize.Zero)

    var flingJob by mutableStateOf<Job?>(null)

    fun clampOffset(pan: Offset, targetScale: Float = scale): Offset {
        return clampViewportOffset(
            pan = pan,
            scale = targetScale,
            contentSize = contentSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
            centerHorizontalWhenFits = centerHorizontalWhenFits,
        )
    }

    fun launchFling(scope: CoroutineScope, vx: Float, vy: Float) {
        flingJob?.cancel()
        flingJob = scope.launch {
            var curVx = vx
            var curVy = vy
            val friction = 0.93f
            var lastTimeNanos = 0L

            while (kotlin.math.hypot(curVx, curVy) > 30f) {
                withFrameNanos { nowNanos ->
                    val dt = if (lastTimeNanos > 0L) {
                        ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.005f, 0.035f)
                    } else {
                        0.016f
                    }
                    lastTimeNanos = nowNanos

                    val nextPan = panOffset + Offset(curVx * dt, curVy * dt)
                    val clamped = clampOffset(nextPan, scale)

                    if (clamped.x == panOffset.x) curVx = 0f
                    if (clamped.y == panOffset.y) curVy = 0f

                    panOffset = clamped
                    curVx *= friction
                    curVy *= friction
                }
            }
        }
    }

    fun animateResetZoom(scope: CoroutineScope, targetPan: Offset = Offset(padX, padY)) {
        flingJob?.cancel()
        scope.launch {
            val startScale = scale
            val startPan = panOffset
            val clampedTarget = clampOffset(targetPan, 1f)

            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioLowBouncy,
                ),
            ) { progress, _ ->
                scale = startScale + (1f - startScale) * progress
                val interpPan = Offset(
                    startPan.x + (clampedTarget.x - startPan.x) * progress,
                    startPan.y + (clampedTarget.y - startPan.y) * progress,
                )
                panOffset = clampOffset(interpPan, scale)
            }
        }
    }
}

@Composable
fun rememberMathViewportState(
    padX: Float,
    padY: Float,
    centerHorizontalWhenFits: Boolean = false,
): MathViewportState {
    return remember(padX, padY, centerHorizontalWhenFits) {
        MathViewportState(padX, padY, centerHorizontalWhenFits)
    }
}

/**
 * Universal Single-Source-of-Truth viewport container for all mathematical formula and result displays:
 * - 2-finger multi-touch pinch zoom with soft dampening
 * - 1-finger panning with inertial fling physics
 * - Clamped boundaries to ensure content stays comfortably in viewport
 * - Floating animated zoom reset button
 */
@Composable
internal fun MathViewport(
    state: MathViewportState,
    modifier: Modifier = Modifier,
    allowVerticalPan: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    onTap: (() -> Unit)? = null,
    onDown: (() -> Unit)? = null,
    showResetButton: Boolean = true,
    resetButtonAlignment: Alignment = Alignment.TopEnd,
    resetButtonPadding: Dp = 8.dp,
    resetButtonSize: Dp = 32.dp,
    resetButtonIconSize: Dp = 18.dp,
    onResetZoom: (() -> Unit)? = null,
    onContentSizeChanged: ((IntSize) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val measurePolicy = remember(state.padX, state.padY, showResetButton, resetButtonPadding, resetButtonAlignment) {
        MeasurePolicy { measurables, constraints ->
            val canvasMeasurable = measurables.firstOrNull { it.layoutId == "canvas" } ?: measurables.first()
            val canvasPlaceable = canvasMeasurable.measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = Constraints.Infinity,
                    minHeight = 0,
                    maxHeight = Constraints.Infinity,
                ),
            )

            val desiredW = canvasPlaceable.width + (state.padX * 2f).roundToInt()
            val desiredH = canvasPlaceable.height + (state.padY * 2f).roundToInt()

            val layoutW = desiredW.coerceIn(constraints.minWidth, constraints.maxWidth)
            val layoutH = desiredH.coerceIn(constraints.minHeight, constraints.maxHeight)

            val resetMeasurable = measurables.firstOrNull { it.layoutId == "reset_button" }
            val resetPlaceable = resetMeasurable?.measure(Constraints())

            layout(layoutW, layoutH) {
                canvasPlaceable.placeRelative(0, 0)
                if (resetPlaceable != null) {
                    val padPx = resetButtonPadding.roundToPx()
                    val resetX = when (resetButtonAlignment) {
                        Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> padPx
                        Alignment.TopCenter, Alignment.Center, Alignment.BottomCenter -> (layoutW - resetPlaceable.width) / 2
                        else -> (layoutW - resetPlaceable.width - padPx).coerceAtLeast(0)
                    }
                    val resetY = when (resetButtonAlignment) {
                        Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd -> padPx
                        Alignment.CenterStart, Alignment.Center, Alignment.CenterEnd -> (layoutH - resetPlaceable.height) / 2
                        else -> (layoutH - resetPlaceable.height - padPx).coerceAtLeast(0)
                    }
                    resetPlaceable.placeRelative(resetX, resetY)
                }
            }
        }
    }

    Layout(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { newVpSize ->
                if (newVpSize != state.viewportSize) {
                    state.viewportSize = newVpSize
                    if (newVpSize.width > 0 && newVpSize.height > 0 && state.contentSize.width > 0 && state.contentSize.height > 0) {
                        val (minScale, maxScale) = computeZoomBounds(
                            contentSize = state.contentSize,
                            viewportSize = newVpSize,
                            padX = state.padX,
                            padY = state.padY,
                        )
                        val newScale = state.scale.coerceIn(minScale, maxScale)
                        state.scale = newScale
                        state.panOffset = state.clampOffset(state.panOffset, newScale)
                    }
                }
            }
            .onGloballyPositioned { coordinates ->
                state.viewportCoordinates = coordinates
                val newVpSize = coordinates.size
                if (newVpSize != state.viewportSize) {
                    state.viewportSize = newVpSize
                    if (newVpSize.width > 0 && newVpSize.height > 0 && state.contentSize.width > 0 && state.contentSize.height > 0) {
                        val (minScale, maxScale) = computeZoomBounds(
                            contentSize = state.contentSize,
                            viewportSize = newVpSize,
                            padX = state.padX,
                            padY = state.padY,
                        )
                        val newScale = state.scale.coerceIn(minScale, maxScale)
                        state.scale = newScale
                        state.panOffset = state.clampOffset(state.panOffset, newScale)
                    }
                }
            }
            .pointerInput(state.contentSize, state.viewportSize, allowVerticalPan) {
                detectViewportGestures(
                    allowVerticalPan = allowVerticalPan,
                    onDown = {
                        state.flingJob?.cancel()
                        onDown?.invoke()
                    },
                    onTap = onTap,
                    onGesture = { centroid, panChange, zoomChange ->
                        state.flingJob?.cancel()
                        val (minScale, maxScale) = computeZoomBounds(
                            contentSize = state.contentSize,
                            viewportSize = state.viewportSize,
                            padX = state.padX,
                            padY = state.padY,
                        )
                        val oldScale = state.scale
                        val newScale = (oldScale * zoomChange).coerceIn(minScale, maxScale)
                        val updatedPan = centroid - (centroid - state.panOffset) * (newScale / oldScale) + panChange
                        state.scale = newScale
                        state.panOffset = state.clampOffset(updatedPan, newScale)
                    },
                    onFling = { vx, vy ->
                        state.launchFling(coroutineScope, vx, vy)
                    },
                )
            },
        measurePolicy = measurePolicy,
        content = {
            // Transformed 2D Math Canvas (origin at 0,0 so panOffset strictly governs screen position)
            Box(
                modifier = Modifier
                    .layoutId("canvas")
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                    .graphicsLayer {
                        scaleX = state.scale
                        scaleY = state.scale
                        translationX = state.panOffset.x
                        translationY = state.panOffset.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
                contentAlignment = Alignment.TopStart,
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                        .onSizeChanged { size ->
                            state.contentSize = size
                            onContentSizeChanged?.invoke(size)
                            if (state.viewportSize.width > 0 && state.viewportSize.height > 0 && size.width > 0 && size.height > 0) {
                                val (minScale, maxScale) = computeZoomBounds(
                                    contentSize = size,
                                    viewportSize = state.viewportSize,
                                    padX = state.padX,
                                    padY = state.padY,
                                )
                                val newScale = state.scale.coerceIn(minScale, maxScale)
                                state.scale = newScale
                                state.panOffset = state.clampOffset(state.panOffset, newScale)
                            }
                        },
                ) {
                    content()
                }
            }

            // Expressive Floating Reset Button
            if (showResetButton) {
                ViewportZoomResetButton(
                    state = state,
                    scope = coroutineScope,
                    sizeDp = resetButtonSize,
                    iconSizeDp = resetButtonIconSize,
                    modifier = Modifier.layoutId("reset_button"),
                    onResetZoom = onResetZoom,
                )
            }
        },
    )
}

@Composable
internal fun ViewportZoomResetButton(
    state: MathViewportState,
    scope: CoroutineScope,
    sizeDp: Dp,
    iconSizeDp: Dp,
    modifier: Modifier = Modifier,
    onResetZoom: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val isZoomChanged = kotlin.math.abs(state.scale - 1f) > 0.05f
    val resetInteractionSource = remember { MutableInteractionSource() }
    val resetPressed by resetInteractionSource.collectIsPressedAsState()
    val resetScale by animateFloatAsState(
        targetValue = if (resetPressed) 0.88f else 1f,
        label = "resetScale",
    )

    AnimatedVisibility(
        visible = isZoomChanged,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = resetScale
                    scaleY = resetScale
                }
                .clickable(
                    interactionSource = resetInteractionSource,
                    indication = null,
                    onClick = {
                        HapticHelper.performClick(view)
                        state.animateResetZoom(scope)
                        onResetZoom?.invoke()
                    },
                ),
        ) {
            Box(
                modifier = Modifier.size(sizeDp),
                contentAlignment = Alignment.Center,
            ) {
                ZoomResetIcon(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(iconSizeDp),
                )
            }
        }
    }
}
