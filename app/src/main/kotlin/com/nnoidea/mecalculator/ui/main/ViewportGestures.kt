package com.nnoidea.mecalculator.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize

internal data class ViewportDelta(val deltaX: Float, val deltaY: Float)

/**
 * Clamps pan offset so the calculation can never be scrolled out of view:
 * - If scaled content is narrower than the viewport, horizontal pan is locked to padX.
 * - If scaled content is wider than the viewport, panX can only scroll between the left edge (padX)
 *   and the right edge (padX - (scaledW - viewW)).
 * - If scaled content is shorter than the viewport (e.g. single-line 1+1+1), vertical pan is locked
 *   to centered position (padY + (viewH - scaledH) / 2f).
 * - If scaled content is taller than the viewport, panY can only scroll between top edge (padY)
 *   and bottom edge (padY - (scaledH - viewH)).
 */
internal fun clampViewportOffset(
    pan: Offset,
    scale: Float,
    contentSize: IntSize,
    viewportSize: IntSize,
    padX: Float,
    padY: Float,
    centerHorizontalWhenFits: Boolean = false,
): Offset {
    val viewW = (viewportSize.width.toFloat() - 2 * padX).coerceAtLeast(0f)
    val viewH = (viewportSize.height.toFloat() - 2 * padY).coerceAtLeast(0f)
    if (viewW <= 0f || viewH <= 0f || contentSize.width <= 0 || contentSize.height <= 0) {
        val defX = if (centerHorizontalWhenFits && viewportSize.width > 0 && contentSize.width > 0) {
            (viewportSize.width.toFloat() - contentSize.width.toFloat() * scale) / 2f
        } else {
            padX
        }
        val defY = if (viewportSize.height > 0 && contentSize.height > 0) {
            (viewportSize.height.toFloat() - contentSize.height.toFloat() * scale) / 2f
        } else {
            padY
        }
        return Offset(defX, defY)
    }

    val scaledW = contentSize.width.toFloat() * scale
    val scaledH = contentSize.height.toFloat() * scale

    val clampedX = if (scaledW <= viewW + 0.5f) {
        if (centerHorizontalWhenFits) {
            (viewportSize.width.toFloat() - scaledW) / 2f
        } else {
            padX
        }
    } else {
        val minPanX = padX - (scaledW - viewW)
        val maxPanX = padX
        pan.x.coerceIn(minPanX, maxPanX)
    }

    val clampedY = if (scaledH <= viewH + 0.5f) {
        (viewportSize.height.toFloat() - scaledH) / 2f
    } else {
        val minPanY = padY - (scaledH - viewH)
        val maxPanY = padY
        pan.y.coerceIn(minPanY, maxPanY)
    }

    return Offset(clampedX, clampedY)
}

/**
 * Calculates zoom bounds:
 * - Allows zooming out until the entire calculation fits comfortably in the viewport.
 * - If the calculation already fits at 1.0x, minScale is 1.0x (cannot zoom out into empty void).
 */
internal fun computeZoomBounds(
    contentSize: IntSize,
    viewportSize: IntSize,
    padX: Float,
    padY: Float,
): Pair<Float, Float> {
    val viewW = (viewportSize.width.toFloat() - 2 * padX).coerceAtLeast(0f)
    val viewH = (viewportSize.height.toFloat() - 2 * padY).coerceAtLeast(0f)
    if (viewW <= 0f || viewH <= 0f || contentSize.width <= 0 || contentSize.height <= 0) {
        return 1f to 2.5f
    }

    val fitScale = kotlin.math.min(viewW / contentSize.width.toFloat(), viewH / contentSize.height.toFloat())
    val minScale = fitScale.coerceIn(0.15f, 1.0f)
    return minScale to 2.5f
}

internal fun computeAutoFrameDelta(
    cursorX: Float,
    cursorY: Float,
    cursorW: Float,
    cursorH: Float,
    viewW: Float,
    viewH: Float,
    padX: Float,
    rightMargin: Float,
    padY: Float,
    bottomMargin: Float,
): ViewportDelta {
    if (viewW <= 0f || viewH <= 0f) return ViewportDelta(0f, 0f)

    var deltaX = 0f
    if (cursorX < padX) {
        deltaX = padX - cursorX
    } else if (cursorX + cursorW > viewW - rightMargin) {
        deltaX = (viewW - rightMargin) - (cursorX + cursorW)
    }

    var deltaY = 0f
    if (cursorY < padY) {
        deltaY = padY - cursorY
    } else if (cursorY + cursorH > viewH - bottomMargin) {
        deltaY = (viewH - bottomMargin) - (cursorY + cursorH)
    }

    return ViewportDelta(deltaX, deltaY)
}

/**
 * Custom viewport gesture detector that cleanly differentiates between single taps on numbers,
 * 2-finger pinching/dragging, and parent scroll handling:
 * - Runs in PointerEventPass.Initial so multi-touch pinch gestures are detected and consumed
 *   BEFORE child clickables (on numbers) can intercept them.
 * - Single-finger drags exceeding touchSlop are consumed for panning (canceling child clickables).
 * - When [allowVerticalPan] is false (e.g. inside a scrollable LazyColumn), vertical drags are
 *   left unconsumed so the parent list scrolls smoothly, while horizontal drags and 2-finger zoom work!
 * - Single taps (movement < touchSlop) invoke [onTap] if provided.
 */
internal suspend fun PointerInputScope.detectViewportGestures(
    allowVerticalPan: Boolean = true,
    onDown: () -> Unit = {},
    onTap: (() -> Unit)? = null,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onFling: (vx: Float, vy: Float) -> Unit = { _, _ -> },
) {
    awaitEachGesture {
        var pan = Offset.Zero
        var pastTouchSlop = false
        var isHorizontalDrag = false
        var isVerticalDrag = false
        val touchSlop = viewConfiguration.touchSlop
        val velocityTracker = VelocityTracker()

        val firstDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        onDown()
        velocityTracker.addPosition(firstDown.uptimeMillis, firstDown.position)

        do {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val downPointers = event.changes.filter { it.pressed }
            val pointerCount = downPointers.size

            val primaryChange = downPointers.firstOrNull()
            if (primaryChange != null) {
                velocityTracker.addPosition(primaryChange.uptimeMillis, primaryChange.position)
            }

            if (pointerCount >= 2) {
                // Multi-touch: pinch-to-zoom with subtle dampening
                pastTouchSlop = true
                val rawZoom = event.calculateZoom()
                val dampenedZoom = if (rawZoom != 1f) 1f + (rawZoom - 1f) * 0.88f else 1f
                val panChange = event.calculatePan()
                val centroid = event.calculateCentroid(useCurrent = false)

                if (dampenedZoom != 1f || panChange != Offset.Zero) {
                    onGesture(centroid, panChange, dampenedZoom)
                }

                event.changes.forEach { it.consume() }
            } else if (pointerCount == 1) {
                val panChange = event.calculatePan()
                if (!pastTouchSlop) {
                    pan += panChange
                    val dist = pan.getDistance()
                    if (dist > touchSlop) {
                        pastTouchSlop = true
                        if (kotlin.math.abs(pan.x) >= kotlin.math.abs(pan.y)) {
                            isHorizontalDrag = true
                        } else {
                            isVerticalDrag = true
                        }
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    if (allowVerticalPan) {
                        if (panChange != Offset.Zero) {
                            onGesture(centroid, panChange, 1f)
                        }
                        event.changes.forEach { it.consume() }
                    } else if (isHorizontalDrag) {
                        if (panChange.x != 0f) {
                            onGesture(centroid, Offset(panChange.x, 0f), 1f)
                        }
                        event.changes.forEach { it.consume() }
                    } else if (isVerticalDrag) {
                        // Unfocused vertical drag: leave unconsumed so parent LazyColumn scrolls smoothly!
                    }
                }
            }
        } while (event.changes.any { it.pressed })

        if (!pastTouchSlop) {
            onTap?.invoke()
        } else {
            val velocity = velocityTracker.calculateVelocity()
            val vx = velocity.x.coerceIn(-3500f, 3500f)
            val vy = if (allowVerticalPan) velocity.y.coerceIn(-3500f, 3500f) else 0f
            if (kotlin.math.hypot(vx, vy) > 80f) {
                onFling(vx, vy)
            }
        }
    }
}

@Composable
internal fun ZoomResetIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val resetPath = remember {
        PathParser().parsePathString(
            "M12,5V1L7,6l5,5V7c3.31,0,6,2.69,6,6s-2.69,6-6,6s-6-2.69-6-6H4c0,4.42,3.58,8,8,8s8-3.58,8-8S16.42,5,12,5z"
        ).toPath()
    }

    Canvas(modifier = modifier) {
        val scaleFactor = size.minDimension / 24f
        val offsetX = (size.width - 24f * scaleFactor) / 2f
        val offsetY = (size.height - 24f * scaleFactor) / 2f

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scaleFactor, scaleFactor, pivot = Offset.Zero)
        }) {
            drawPath(path = resetPath, color = color)
        }
    }
}
