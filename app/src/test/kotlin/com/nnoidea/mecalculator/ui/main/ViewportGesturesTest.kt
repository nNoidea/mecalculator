package com.nnoidea.mecalculator.ui.main

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportGesturesTest {

    @Test
    fun `clampViewportOffset locks vertical pan when content fits within viewport`() {
        val padX = 12f
        val padY = 8f
        val contentH = 100
        val contentW = 300
        // Viewport has expanded tall enough to fit the content plus top and bottom padding
        val viewportH = contentH + (2 * padY).toInt() // 116
        val viewportW = 400

        val contentSize = IntSize(contentW, contentH)
        val viewportSize = IntSize(viewportW, viewportH)

        // User attempts to pan up or down
        val offsetTryUp = Offset(12f, -50f)
        val offsetTryDown = Offset(12f, 50f)

        val clampedUp = clampViewportOffset(
            pan = offsetTryUp,
            scale = 1.0f,
            contentSize = contentSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )

        val clampedDown = clampViewportOffset(
            pan = offsetTryDown,
            scale = 1.0f,
            contentSize = contentSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )

        // When content fits, clamped Y must be locked to (viewportH - contentH) / 2 = 8f (padY)
        // Vertical scroll is completely disabled.
        assertEquals(padY, clampedUp.y, 0.01f)
        assertEquals(padY, clampedDown.y, 0.01f)
    }

    @Test
    fun `clampViewportOffset enables vertical pan only when content exceeds maximum viewport height`() {
        val padX = 12f
        val padY = 8f
        // Huge content exceeding the maximum allowed viewport height (e.g. 5-level nested fraction)
        val contentH = 400
        val contentW = 300
        val maxViewportH = 200 // Viewport capped at maxInputH

        val contentSize = IntSize(contentW, contentH)
        val viewportSize = IntSize(400, maxViewportH)

        val viewH = maxViewportH - 2 * padY // 184f
        val minExpectedPanY = padY - (contentH - viewH) // 8 - (400 - 184) = 8 - 216 = -208f
        val maxExpectedPanY = padY // 8f

        // When user scrolls all the way down (content top at padY)
        val clampedTop = clampViewportOffset(
            pan = Offset(12f, 100f),
            scale = 1.0f,
            contentSize = contentSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )
        assertEquals(maxExpectedPanY, clampedTop.y, 0.01f)

        // When user scrolls all the way up (content bottom at viewport bottom - padY)
        val clampedBottom = clampViewportOffset(
            pan = Offset(12f, -500f),
            scale = 1.0f,
            contentSize = contentSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )
        assertEquals(minExpectedPanY, clampedBottom.y, 0.01f)

        // When user scrolls midway
        val clampedMid = clampViewportOffset(
            pan = Offset(12f, -50f),
            scale = 1.0f,
            contentSize = contentSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )
        assertEquals(-50f, clampedMid.y, 0.01f)
    }

    @Test
    fun `clampViewportOffset correctly centers result pills horizontally`() {
        val padX = 12f
        val padY = 4f
        val contentW = 100
        val contentH = 30
        val viewportW = 200
        val viewportH = 38

        val contentSize = IntSize(contentW, contentH)
        val viewportSize = IntSize(viewportW, viewportH)

        val clamped = clampViewportOffset(
            pan = Offset(0f, 0f),
            scale = 1.0f,
            contentSize = contentSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
            centerHorizontalWhenFits = true,
        )

        // Expected centered X: (200 - 100) / 2 = 50f
        assertEquals(50f, clamped.x, 0.01f)
    }

    @Test
    fun `computeZoomBounds prevents zooming out into void when content fits`() {
        val padX = 12f
        val padY = 8f
        val contentSize = IntSize(200, 100)
        val viewportSize = IntSize(400, 200)

        val (minScale, maxScale) = computeZoomBounds(
            contentSize = contentSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )

        assertEquals(1.0f, minScale, 0.01f)
        assertEquals(2.5f, maxScale, 0.01f)
    }

    @Test
    fun `computeZoomBounds allows zooming out when content exceeds viewport`() {
        val padX = 12f
        val padY = 8f
        val contentSize = IntSize(800, 400)
        val viewportSize = IntSize(400, 200)

        val (minScale, maxScale) = computeZoomBounds(
            contentSize = contentSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )

        assertTrue(minScale < 1.0f)
        assertTrue(minScale >= 0.15f)
        assertEquals(2.5f, maxScale, 0.01f)
    }
}
