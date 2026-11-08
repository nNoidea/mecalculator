package com.nnoidea.mecalculator.ui.main

import android.graphics.BlurMaskFilter
import android.graphics.Paint as AndroidPaint
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nnoidea.mecalculator.data.KeypadCatalog
import com.nnoidea.mecalculator.data.KeypadSlot
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException

@Composable
internal fun CalculatorKeypad(
    modifier: Modifier = Modifier,
    keypadSlots: List<KeypadSlot>,
    inHistory: Boolean,
    isEditorEmpty: Boolean = true,
    arrowHoldDelayMs: Int,
    arrowRepeatIntervalMs: Int,
    initialActiveFolderSlotIndex: Int? = null,
    onInsertChars: (String) -> Unit,
    onInsertFunc: (String) -> Unit,
    onInsertPower: () -> Unit,
    onInsertFraction: () -> Unit,
    onInsertSqrt: () -> Unit,
    onMoveCursor: (String) -> Boolean,
    onEqualsOrOk: () -> Unit,
    onBackspace: () -> Unit,
    onLongPressDelete: () -> Unit,
    onDeletePressDown: (() -> Unit)? = null,
    onDeletePressUp: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val shadowPaint = remember {
        AndroidPaint().apply {
            isAntiAlias = true
            style = AndroidPaint.Style.FILL
        }
    }

    var activeFolderSlotIndex by remember { mutableStateOf(initialActiveFolderSlotIndex) }
    var lastFolderSlotIndex by remember { mutableStateOf(initialActiveFolderSlotIndex) }

    if (activeFolderSlotIndex != null) {
        val currentSlot = keypadSlots.getOrNull(activeFolderSlotIndex!!)
        if (currentSlot !is KeypadSlot.Folder) {
            activeFolderSlotIndex = null
        } else {
            lastFolderSlotIndex = activeFolderSlotIndex
        }
    }

    val folderProgress = remember { Animatable(if (initialActiveFolderSlotIndex != null) 1f else 0f) }

    LaunchedEffect(activeFolderSlotIndex) {
        if (activeFolderSlotIndex != null) {
            lastFolderSlotIndex = activeFolderSlotIndex
            folderProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        } else {
            folderProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    PredictiveBackHandler(enabled = activeFolderSlotIndex != null) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                val targetFraction = (1f - FastOutSlowInEasing.transform(backEvent.progress)).coerceIn(0f, 1f)
                folderProgress.snapTo(targetFraction)
            }
            activeFolderSlotIndex = null
        } catch (e: CancellationException) {
            folderProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
    }

    val openFraction = folderProgress.value

    BoxWithConstraints(modifier = modifier) {
        val totalW = maxWidth
        val totalH = maxHeight
        val gap = 4.dp
        val cellW = (totalW - gap * 3) / 4f
        val cellH = (totalH - gap * 5) / 6f
        val stepW = cellW + gap
        val stepH = cellH + gap
        val buttonCornerRadius = cellH / 2f

        // Main 4x6 Numpad Grid
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            for (row in 0 until 6) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    for (col in 0 until 4) {
                        val idx = row * 4 + col
                        val slot = keypadSlots.getOrElse(idx) { KeypadSlot.Key("") }
                        when (slot) {
                            is KeypadSlot.Folder -> {
                                val isThisOpen = activeFolderSlotIndex == idx
                                val colors = keyColorsFor("", isFolder = true)
                                ExpressiveCalcButton(
                                    label = if (isThisOpen) "✕" else slot.title,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    containerColor = colors.containerColor,
                                    contentColor = colors.contentColor,
                                    fontSize = 18.sp,
                                    onClick = {
                                        activeFolderSlotIndex = if (isThisOpen) null else idx
                                    },
                                )
                            }
                            is KeypadSlot.Key -> {
                                KeypadKeySlotButton(
                                    keyId = slot.keyId,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    inHistory = inHistory,
                                    isEditorEmpty = isEditorEmpty,
                                    arrowHoldDelayMs = arrowHoldDelayMs,
                                    arrowRepeatIntervalMs = arrowRepeatIntervalMs,
                                    onInsertChars = onInsertChars,
                                    onInsertFunc = onInsertFunc,
                                    onInsertPower = onInsertPower,
                                    onInsertFraction = onInsertFraction,
                                    onInsertSqrt = onInsertSqrt,
                                    onMoveCursor = onMoveCursor,
                                    onEqualsOrOk = onEqualsOrOk,
                                    onBackspace = onBackspace,
                                    onLongPressDelete = onLongPressDelete,
                                    onDeletePressDown = onDeletePressDown,
                                    onDeletePressUp = onDeletePressUp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expanding Folder Overlay (expands from the folder button only as far as its non-empty keys!)
        val folderIdx = activeFolderSlotIndex ?: lastFolderSlotIndex
        val folderSlot = folderIdx?.let { keypadSlots.getOrNull(it) as? KeypadSlot.Folder }
        if (folderIdx != null && folderSlot != null && openFraction > 0.01f) {
            // Dismiss open folder when tapping anywhere outside the expanded folder
            if (activeFolderSlotIndex != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { activeFolderSlotIndex = null },
                        ),
                )
            }

            val folderRow = folderIdx / 4
            val folderCol = folderIdx % 4

            var minRow = folderRow
            var maxRow = folderRow
            var minCol = folderCol
            var maxCol = folderCol

            for (i in 0 until 24) {
                if (i != folderIdx && folderSlot.keyIds.getOrElse(i) { "" }.isNotEmpty()) {
                    val r = i / 4
                    val c = i % 4
                    if (r < minRow) minRow = r
                    if (r > maxRow) maxRow = r
                    if (c < minCol) minCol = c
                    if (c > maxCol) maxCol = c
                }
            }

            val collapsedX = stepW * folderCol
            val collapsedY = stepH * folderRow
            val collapsedW = cellW
            val collapsedH = cellH

            val expandedX = stepW * minCol
            val expandedY = stepH * minRow
            val expandedW = cellW * (maxCol - minCol + 1) + gap * (maxCol - minCol)
            val expandedH = cellH * (maxRow - minRow + 1) + gap * (maxRow - minRow)

            val currentX = collapsedX + (expandedX - collapsedX) * openFraction
            val currentY = collapsedY + (expandedY - collapsedY) * openFraction
            val currentW = collapsedW + (expandedW - collapsedW) * openFraction
            val currentH = collapsedH + (expandedH - collapsedH) * openFraction

            // Soft ambient drop shadow under the open folder background to provide a tangible lift feel
            if (openFraction > 0.02f) {
                val shadowBlurPx = with(density) { 10.dp.toPx() } * openFraction
                val shadowOffsetYPx = with(density) { 4.dp.toPx() } * openFraction
                val cornerRadiusPx = with(density) { buttonCornerRadius.toPx() }
                val shadowAlpha = (0.50f * openFraction).coerceIn(0f, 0.50f)

                Box(
                    modifier = Modifier
                        .offset(x = currentX, y = currentY)
                        .size(width = currentW, height = currentH)
                        .drawBehind {
                            shadowPaint.color = android.graphics.Color.argb(
                                (shadowAlpha * 255).roundToInt(),
                                0,
                                0,
                                0,
                            )
                            shadowPaint.maskFilter = BlurMaskFilter(
                                shadowBlurPx.coerceAtLeast(1f),
                                BlurMaskFilter.Blur.NORMAL,
                            )
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawRoundRect(
                                    0f,
                                    shadowOffsetYPx,
                                    size.width,
                                    size.height + shadowOffsetYPx,
                                    cornerRadiusPx,
                                    cornerRadiusPx,
                                    shadowPaint,
                                )
                            }
                        },
                )
            }

            Surface(
                modifier = Modifier
                    .offset(x = currentX, y = currentY)
                    .size(width = currentW, height = currentH),
                shape = RoundedCornerShape(buttonCornerRadius),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 8.dp * openFraction,
                tonalElevation = 4.dp * openFraction,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    for (r in minRow..maxRow) {
                        for (c in minCol..maxCol) {
                            val idx = r * 4 + c
                            val itemX = stepW * c - currentX
                            val itemY = stepH * r - currentY
                            val itemMod = Modifier
                                .offset(x = itemX, y = itemY)
                                .size(width = cellW, height = cellH)

                            if (idx == folderIdx) {
                                // The folder button itself turns into an X to close the folder
                                val colors = keyColorsFor("✕")
                                ExpressiveCalcButton(
                                    label = "✕",
                                    modifier = itemMod,
                                    containerColor = colors.containerColor,
                                    contentColor = colors.contentColor,
                                    fontSize = 22.sp,
                                    onClick = { activeFolderSlotIndex = null },
                                )
                            } else {
                                val keyId = folderSlot.keyIds.getOrElse(idx) { "" }
                                KeypadKeySlotButton(
                                    keyId = keyId,
                                    modifier = itemMod,
                                    inHistory = inHistory,
                                    isEditorEmpty = isEditorEmpty,
                                    arrowHoldDelayMs = arrowHoldDelayMs,
                                    arrowRepeatIntervalMs = arrowRepeatIntervalMs,
                                    onInsertChars = { text ->
                                        onInsertChars(text)
                                        activeFolderSlotIndex = null
                                    },
                                    onInsertFunc = { fn ->
                                        onInsertFunc(fn)
                                        activeFolderSlotIndex = null
                                    },
                                    onInsertPower = {
                                        onInsertPower()
                                        activeFolderSlotIndex = null
                                    },
                                    onInsertFraction = {
                                        onInsertFraction()
                                        activeFolderSlotIndex = null
                                    },
                                    onInsertSqrt = {
                                        onInsertSqrt()
                                        activeFolderSlotIndex = null
                                    },
                                    onMoveCursor = onMoveCursor,
                                    onEqualsOrOk = {
                                        onEqualsOrOk()
                                        activeFolderSlotIndex = null
                                    },
                                    onBackspace = onBackspace,
                                    onLongPressDelete = onLongPressDelete,
                                    onDeletePressDown = onDeletePressDown,
                                    onDeletePressUp = onDeletePressUp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun KeypadKeySlotButton(
    keyId: String,
    modifier: Modifier = Modifier,
    inHistory: Boolean,
    isEditorEmpty: Boolean = true,
    arrowHoldDelayMs: Int,
    arrowRepeatIntervalMs: Int,
    onInsertChars: (String) -> Unit,
    onInsertFunc: (String) -> Unit,
    onInsertPower: () -> Unit,
    onInsertFraction: () -> Unit,
    onInsertSqrt: () -> Unit,
    onMoveCursor: (String) -> Boolean,
    onEqualsOrOk: () -> Unit,
    onBackspace: () -> Unit,
    onLongPressDelete: () -> Unit,
    onDeletePressDown: (() -> Unit)? = null,
    onDeletePressUp: (() -> Unit)? = null,
    onEmptySlotClick: (() -> Unit)? = null,
) {
    val colors = keyColorsFor(keyId)
    when (keyId) {
        "" -> {
            Box(
                modifier = modifier
                    .then(
                        if (onEmptySlotClick != null) {
                            Modifier.clickable(onClick = onEmptySlotClick)
                        } else {
                            Modifier
                        },
                    ),
            )
        }
        "▲" -> {
            ExpressiveArrowButton(
                label = "▲",
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                holdDelayMs = arrowHoldDelayMs,
                repeatIntervalMs = arrowRepeatIntervalMs,
                onMove = { onMoveCursor("UP") },
            )
        }
        "▼" -> {
            ExpressiveArrowButton(
                label = "▼",
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                holdDelayMs = arrowHoldDelayMs,
                repeatIntervalMs = arrowRepeatIntervalMs,
                onMove = { onMoveCursor("DOWN") },
            )
        }
        "◀" -> {
            ExpressiveArrowButton(
                label = "◀",
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                holdDelayMs = arrowHoldDelayMs,
                repeatIntervalMs = arrowRepeatIntervalMs,
                onMove = { onMoveCursor("LEFT") },
            )
        }
        "▶" -> {
            ExpressiveArrowButton(
                label = "▶",
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                holdDelayMs = arrowHoldDelayMs,
                repeatIntervalMs = arrowRepeatIntervalMs,
                onMove = { onMoveCursor("RIGHT") },
            )
        }
        "=" -> {
            val isOk = inHistory && isEditorEmpty
            ExpressiveCalcButton(
                label = if (isOk) "OK" else "=",
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                fontSize = if (isOk) 20.sp else 26.sp,
                onClick = onEqualsOrOk,
            )
        }
        "⌫" -> {
            ExpressiveCalcButton(
                label = "⌫",
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                fontSize = 21.sp,
                onClick = onBackspace,
                onLongClick = onLongPressDelete,
                onPressDown = onDeletePressDown,
                onPressUp = onDeletePressUp,
            )
        }
        "÷" -> {
            ExpressiveCalcButton(
                label = "÷",
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                fontSize = 26.sp,
                onClick = onInsertFraction,
            )
        }
        "xʸ" -> {
            ExpressiveCalcButton(
                label = "xʸ",
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                fontSize = 22.sp,
                onClick = onInsertPower,
            )
        }
        "√" -> {
            ExpressiveCalcButton(
                label = "√",
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                fontSize = 22.sp,
                onClick = onInsertSqrt,
            )
        }
        "×", "·" -> {
            // Display '×' on the numpad button, but insert '·' (middle dot) into the expression!
            ExpressiveCalcButton(
                label = "×",
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                fontSize = 26.sp,
                onClick = { onInsertChars("·") },
            )
        }
        "sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "abs", "fact" -> {
            val label = KeypadCatalog.labelFor(keyId)
            ExpressiveCalcButton(
                label = label,
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                fontSize = if (label.length >= 3) 17.sp else 21.sp,
                onClick = { onInsertFunc(keyId) },
            )
        }
        else -> {
            val label = KeypadCatalog.labelFor(keyId)
            ExpressiveCalcButton(
                label = label,
                modifier = modifier,
                containerColor = colors.containerColor,
                contentColor = colors.contentColor,
                fontSize = if (label.length >= 3) 17.sp else 24.sp,
                onClick = { onInsertChars(keyId) },
            )
        }
    }
}
