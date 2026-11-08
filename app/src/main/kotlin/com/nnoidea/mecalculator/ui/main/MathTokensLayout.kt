package com.nnoidea.mecalculator.ui.main

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nnoidea.mecalculator.data.NumberGrouping
import com.nnoidea.mecalculator.domain.NumberFormatter

// --- RECURSIVE 2D COMPOSABLE MATH RENDERER WITH LIVE NUMBER GROUPING ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MathTokensRow(
    tokens: List<MathToken>,
    pathPrefix: List<CursorStep>,
    numberGrouping: NumberGrouping,
    cursor: CursorState,
    showCursor: Boolean,
    fontSize: TextUnit,
    contentColor: Color,
    accentColor: Color,
    wrapTokens: Boolean = false,
    onTokenClick: (CursorState) -> Unit = {},
    onCursorPositioned: ((LayoutCoordinates) -> Unit)? = null,
) {
    val isThisListFocused = showCursor && cursor.path == pathPrefix
    val levelChars = remember(tokens) {
        tokens.map { tok ->
            if (tok is MathToken.CharToken) tok.ch else '#'
        }
    }
    val leadingSeps = remember(levelChars, numberGrouping) {
        NumberFormatter.getLeadingSeparatorsForCharList(levelChars, numberGrouping)
    }

    val renderChildren: @Composable () -> Unit = {
        for (i in 0..tokens.size) {
            if (i < tokens.size) {
                val prefixSep = leadingSeps.getOrElse(i) { "" }
                if (prefixSep.isNotEmpty()) {
                    key("sep_${pathPrefix}_$i") {
                        Text(
                            text = prefixSep,
                            color = contentColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.layoutId("sep"),
                        )
                    }
                }
            }
            if (isThisListFocused && cursor.offset == i) {
                key("cursor_${pathPrefix}_$i") {
                    CursorBar(
                        color = accentColor,
                        height = (fontSize.value * 1.16f + 4f).dp,
                        blinkKey = cursor to tokens.size,
                        modifier = Modifier
                            .layoutId("cursor_bar")
                            .then(
                                if (onCursorPositioned != null) {
                                    Modifier.onGloballyPositioned(onCursorPositioned)
                                } else Modifier,
                            ),
                    )
                }
            }
            if (i < tokens.size) {
                key("tok_${pathPrefix}_$i") {
                    val isFocusedSubtree = showCursor &&
                        cursor.path.size > pathPrefix.size &&
                        cursor.path.take(pathPrefix.size) == pathPrefix &&
                        cursor.path[pathPrefix.size].tokenIndex == i
                    val tokenLayoutMod = if (isFocusedSubtree) Modifier.layoutId("cursor_token") else Modifier
                    val compositeLayoutMod = Modifier.layoutId(
                        if (isFocusedSubtree) "cursor_token" else "composite_token",
                    )

                    when (val tok = tokens[i]) {
                        is MathToken.CharToken -> {
                            Text(
                                text = "${tok.ch}",
                                color = contentColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSize,
                                fontWeight = FontWeight.SemiBold,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false,
                                    ),
                                ),
                                modifier = tokenLayoutMod
                                    .clickable {
                                        onTokenClick(CursorState(path = pathPrefix, offset = i + 1))
                                    }
                                    .padding(horizontal = 0.5.dp),
                            )
                        }
                        is MathToken.FuncToken -> {
                            Text(
                                text = "${tok.name}(",
                                color = contentColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSize,
                                fontWeight = FontWeight.SemiBold,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false,
                                    ),
                                ),
                                modifier = tokenLayoutMod
                                    .clickable {
                                        onTokenClick(CursorState(path = pathPrefix, offset = i + 1))
                                    }
                                    .padding(horizontal = 0.5.dp),
                            )
                        }
                        is MathToken.FractionToken -> {
                            val numPath = pathPrefix + CursorStep(i, SlotType.FRAC_NUM)
                            val denPath = pathPrefix + CursorStep(i, SlotType.FRAC_DEN)
                            val isNumFocused = showCursor && cursor.path == numPath
                            val isDenFocused = showCursor && cursor.path == denPath
                            val subFontSize = (fontSize.value * 0.78f).coerceAtLeast(13f).sp

                            FractionColumnLayout(
                                modifier = compositeLayoutMod
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                barColor = if (isFocusedSubtree) accentColor else contentColor.copy(alpha = 0.85f),
                                numerator = {
                                    SlotTokenRow(
                                        tokens = tok.num,
                                        slotPath = numPath,
                                        numberGrouping = numberGrouping,
                                        cursor = cursor,
                                        showCursor = showCursor,
                                        isDirectlyFocused = isNumFocused,
                                        fontSize = subFontSize,
                                        contentColor = contentColor,
                                        accentColor = accentColor,
                                        onTokenClick = onTokenClick,
                                        onCursorPositioned = onCursorPositioned,
                                    )
                                },
                                denominator = {
                                    SlotTokenRow(
                                        tokens = tok.den,
                                        slotPath = denPath,
                                        numberGrouping = numberGrouping,
                                        cursor = cursor,
                                        showCursor = showCursor,
                                        isDirectlyFocused = isDenFocused,
                                        fontSize = subFontSize,
                                        contentColor = contentColor,
                                        accentColor = accentColor,
                                        onTokenClick = onTokenClick,
                                        onCursorPositioned = onCursorPositioned,
                                    )
                                },
                            )
                        }
                        is MathToken.PowerToken -> {
                            val expPath = pathPrefix + CursorStep(i, SlotType.POW_EXP)
                            val isExpFocused = showCursor && cursor.path == expPath
                            val expFontSize = (fontSize.value * 0.68f).coerceAtLeast(12f).sp

                            Box(
                                modifier = compositeLayoutMod
                                    .padding(bottom = (fontSize.value * 0.36f).dp, end = 2.dp),
                            ) {
                                SlotTokenRow(
                                    tokens = tok.exp,
                                    slotPath = expPath,
                                    numberGrouping = numberGrouping,
                                    cursor = cursor,
                                    showCursor = showCursor,
                                    isDirectlyFocused = isExpFocused,
                                    fontSize = expFontSize,
                                    contentColor = contentColor,
                                    accentColor = accentColor,
                                    onTokenClick = onTokenClick,
                                    onCursorPositioned = onCursorPositioned,
                                )
                            }
                        }
                        is MathToken.SqrtToken -> {
                            val radPath = pathPrefix + CursorStep(i, SlotType.SQRT_RAD)
                            val isRadFocused = showCursor && cursor.path == radPath
                            val strokeDp = (fontSize.value * 0.075f).coerceIn(1.5f, 2.5f).dp

                            SqrtLayout(
                                modifier = compositeLayoutMod.padding(horizontal = 2.dp),
                                radicalColor = if (isFocusedSubtree) accentColor else contentColor,
                                strokeWidth = strokeDp,
                                content = {
                                    SlotTokenRow(
                                        tokens = tok.rad,
                                        slotPath = radPath,
                                        numberGrouping = numberGrouping,
                                        cursor = cursor,
                                        showCursor = showCursor,
                                        isDirectlyFocused = isRadFocused,
                                        fontSize = fontSize,
                                        contentColor = contentColor,
                                        accentColor = accentColor,
                                        onTokenClick = onTokenClick,
                                        onCursorPositioned = onCursorPositioned,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    val minRowHeight = (fontSize.value * 1.16f + 4f).dp
    if (wrapTokens && pathPrefix.isEmpty()) {
        FlowRow(
            modifier = Modifier.heightIn(min = minRowHeight),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            renderChildren()
        }
    } else {
        Row(
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .heightIn(min = minRowHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            renderChildren()
        }
    }
}

internal fun computeWindowedItemMaxWidth(
    isComposite: Boolean,
    hasBounded: Boolean,
    maxW: Int,
    totalRowIntrinsicWidth: Int,
    showLeft: Boolean,
    showRight: Boolean,
    leftEllWidth: Int,
    rightEllWidth: Int,
): Int {
    if (!isComposite || !hasBounded) return Constraints.Infinity
    if (totalRowIntrinsicWidth <= maxW) return maxW
    return (maxW -
        (if (showLeft) leftEllWidth else 0) -
        (if (showRight) rightEllWidth else 0)
        ).coerceAtLeast(0)
}

@Composable
internal fun FractionColumnLayout(
    modifier: Modifier = Modifier,
    barColor: Color,
    numerator: @Composable () -> Unit,
    denominator: @Composable () -> Unit,
) {
    val measurePolicy = remember {
        object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints,
            ): MeasureResult {
                val barPadPx = 2.dp.roundToPx()
                val barHeightPx = 2.dp.roundToPx().coerceAtLeast(1)
                val minBarWidthPx = 14.dp.roundToPx()
                val slotConstraints = Constraints(
                    minWidth = 0,
                    maxWidth = constraints.maxWidth,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight,
                )
                val numPlaceable = measurables[0].measure(slotConstraints)
                val denPlaceable = measurables[1].measure(slotConstraints)
                val fracW = maxOf(
                    numPlaceable.width,
                    denPlaceable.width,
                    minBarWidthPx,
                    constraints.minWidth,
                ).coerceAtMost(constraints.maxWidth)
                val barPlaceable = measurables[2].measure(Constraints.fixed(fracW, barHeightPx))
                val totalH = (numPlaceable.height + barPadPx * 2 + barHeightPx + denPlaceable.height)
                    .coerceIn(constraints.minHeight, constraints.maxHeight)

                return layout(fracW, totalH) {
                    numPlaceable.placeRelative((fracW - numPlaceable.width) / 2, 0)
                    barPlaceable.placeRelative(0, numPlaceable.height + barPadPx)
                    denPlaceable.placeRelative(
                        (fracW - denPlaceable.width) / 2,
                        numPlaceable.height + barPadPx * 2 + barHeightPx,
                    )
                }
            }

            override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int,
            ): Int = maxOf(
                measurables[0].maxIntrinsicWidth(height),
                measurables[1].maxIntrinsicWidth(height),
                14.dp.roundToPx(),
            )

            override fun IntrinsicMeasureScope.minIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int,
            ): Int = maxOf(
                measurables[0].minIntrinsicWidth(height),
                measurables[1].minIntrinsicWidth(height),
                14.dp.roundToPx(),
            )

            override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int,
            ): Int =
                measurables[0].maxIntrinsicHeight(width) +
                    measurables[1].maxIntrinsicHeight(width) +
                    6.dp.roundToPx()

            override fun IntrinsicMeasureScope.minIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int,
            ): Int =
                measurables[0].minIntrinsicHeight(width) +
                    measurables[1].minIntrinsicHeight(width) +
                    6.dp.roundToPx()
        }
    }

    Layout(
        modifier = modifier,
        content = {
            numerator()
            denominator()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(1.dp))
                    .background(barColor),
            )
        },
        measurePolicy = measurePolicy,
    )
}

@Composable
internal fun SqrtLayout(
    modifier: Modifier = Modifier,
    radicalColor: Color,
    strokeWidth: Dp = 1.6.dp,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val strokePx = with(density) { strokeWidth.toPx() }
    val topGap = with(density) { 2.5.dp.toPx() }
    val radLeftPadding = with(density) { 2.dp.toPx() }
    val rightOverhang = with(density) { 3.dp.toPx() }
    val downwardTip = with(density) { 3.dp.toPx() }

    Layout(
        modifier = modifier.drawBehind {
            val totalW = size.width
            val totalH = size.height
            val topPad = topGap + strokePx
            val contentH = (totalH - topPad - 1.dp.toPx()).coerceAtLeast(1f)
            val symW = (contentH * 0.38f).coerceIn(10.dp.toPx(), 24.dp.toPx())

            val topY = strokePx / 2f
            val botY = totalH - strokePx / 2f - 1.dp.toPx()
            val midY = botY - (botY - topY) * 0.45f
            val startX = 1.dp.toPx()
            val vertexX = symW * 0.38f
            val topX = symW
            val endX = totalW - 1.dp.toPx()
            val tipY = topY + downwardTip

            val path = Path().apply {
                moveTo(startX, midY)
                lineTo(startX + symW * 0.12f, midY - 2.dp.toPx())
                lineTo(vertexX, botY)
                lineTo(topX, topY)
                lineTo(endX, topY)
                lineTo(endX, tipY)
            }

            drawPath(
                path = path,
                color = radicalColor,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        },
        content = content,
    ) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        val contentW = placeable.width
        val contentH = placeable.height

        val symW = (contentH * 0.38f).coerceIn(
            with(density) { 10.dp.toPx() },
            with(density) { 24.dp.toPx() },
        )
        val topPad = (topGap + strokePx).roundToInt()
        val totalW = (symW + radLeftPadding + contentW + rightOverhang).roundToInt()
        val totalH = contentH + topPad + with(density) { 1.dp.toPx() }.roundToInt()

        layout(totalW, totalH) {
            val contentX = (symW + radLeftPadding).roundToInt()
            placeable.placeRelative(contentX, topPad)
        }
    }
}

@Composable
internal fun SlotTokenRow(
    tokens: List<MathToken>,
    slotPath: List<CursorStep>,
    numberGrouping: NumberGrouping,
    cursor: CursorState,
    showCursor: Boolean,
    isDirectlyFocused: Boolean,
    fontSize: TextUnit,
    contentColor: Color,
    accentColor: Color,
    onTokenClick: (CursorState) -> Unit,
    onCursorPositioned: ((LayoutCoordinates) -> Unit)? = null,
) {
    val slotBg = if (isDirectlyFocused) {
        accentColor.copy(alpha = 0.18f)
    } else if (tokens.isEmpty()) {
        contentColor.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }
    val slotBorderMod = if (isDirectlyFocused) {
        Modifier.border(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.55f),
            shape = RoundedCornerShape(6.dp),
        )
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(slotBg)
            .then(slotBorderMod)
            .clickable {
                onTokenClick(CursorState(path = slotPath, offset = tokens.size))
            }
            .padding(horizontal = 4.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (tokens.isEmpty()) {
            if (isDirectlyFocused) {
                CursorBar(
                    color = accentColor,
                    height = (fontSize.value * 1.16f + 3f).dp,
                    blinkKey = cursor to tokens.size,
                    modifier = Modifier.then(
                        if (onCursorPositioned != null) {
                            Modifier.onGloballyPositioned(onCursorPositioned)
                        } else Modifier,
                    ),
                )
            } else {
                Text(
                    text = "□",
                    color = contentColor.copy(alpha = 0.45f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            MathTokensRow(
                tokens = tokens,
                pathPrefix = slotPath,
                numberGrouping = numberGrouping,
                cursor = cursor,
                showCursor = showCursor,
                fontSize = fontSize,
                contentColor = contentColor,
                accentColor = accentColor,
                onTokenClick = onTokenClick,
                onCursorPositioned = onCursorPositioned,
            )
        }
    }
}

@Composable
internal fun CursorBar(
    color: Color,
    height: Dp,
    blinkKey: Any? = null,
    modifier: Modifier = Modifier,
) {
    var cursorVisible by remember { mutableStateOf(true) }
    DisposableEffect(blinkKey) {
        cursorVisible = true
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                cursorVisible = !cursorVisible
                handler.postDelayed(this, 520L)
            }
        }
        handler.postDelayed(runnable, 520L)
        onDispose {
            handler.removeCallbacks(runnable)
        }
    }

    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .width(2.5.dp)
            .height(height)
            .graphicsLayer {
                alpha = if (cursorVisible) 1f else 0.18f
            }
            .drawBehind {
                val radius = size.width / 2f
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(radius, radius),
                )
            },
    )
}
