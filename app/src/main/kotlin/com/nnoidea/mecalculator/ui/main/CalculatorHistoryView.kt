package com.nnoidea.mecalculator.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.nnoidea.mecalculator.data.NumberGrouping
import com.nnoidea.mecalculator.ui.util.HapticHelper
import kotlinx.coroutines.launch

/**
 * Scrolls so that [index] is brought fully into the visible viewport if it is currently outside or clipped.
 */
suspend fun LazyListState.scrollItemIntoViewIfNeeded(index: Int) {
    if (index < 0) return
    val info = layoutInfo
    val visibleItems = info.visibleItemsInfo
    if (visibleItems.isEmpty()) {
        scrollToItem(index)
        return
    }

    val viewportStart = info.viewportStartOffset
    val viewportEnd = info.viewportEndOffset
    val spacing = info.mainAxisItemSpacing
    val fastSpec = tween<Float>(durationMillis = 75, easing = FastOutSlowInEasing)

    val target = visibleItems.find { it.index == index }
    if (target != null) {
        val itemTop = target.offset
        val itemBottom = target.offset + target.size
        when {
            itemTop < viewportStart -> {
                animateScrollBy((itemTop - viewportStart).toFloat(), fastSpec)
            }
            itemBottom > viewportEnd -> {
                animateScrollBy((itemBottom - viewportEnd).toFloat(), fastSpec)
            }
        }
        return
    }

    val firstVisible = visibleItems.first()
    val lastVisible = visibleItems.last()

    if (index < firstVisible.index) {
        if (index == firstVisible.index - 1) {
            val estDelta = (firstVisible.offset - viewportStart) - firstVisible.size - spacing
            animateScrollBy(estDelta.toFloat(), fastSpec)
            val settled = layoutInfo.visibleItemsInfo.find { it.index == index }
            if (settled != null && settled.offset != viewportStart) {
                scrollBy((settled.offset - viewportStart).toFloat())
            } else if (settled == null) {
                scrollToItem(index, 0)
            }
        } else {
            animateScrollToItem(index, 0)
        }
    } else if (index > lastVisible.index) {
        if (index == lastVisible.index + 1) {
            val estDelta = (lastVisible.offset + lastVisible.size - viewportEnd) + lastVisible.size + spacing
            animateScrollBy(estDelta.toFloat(), fastSpec)
            val settled = layoutInfo.visibleItemsInfo.find { it.index == index }
            if (settled != null) {
                val overflow = (settled.offset + settled.size) - layoutInfo.viewportEndOffset
                if (overflow != 0) {
                    scrollBy(overflow.toFloat())
                }
            }
        } else {
            val viewportHeight = viewportEnd - viewportStart
            val targetOffset = -(viewportHeight - lastVisible.size).coerceAtLeast(0)
            animateScrollToItem(index, targetOffset)
            val settled = layoutInfo.visibleItemsInfo.find { it.index == index }
            if (settled != null) {
                val overflow = (settled.offset + settled.size) - layoutInfo.viewportEndOffset
                if (overflow != 0) {
                    scrollBy(overflow.toFloat())
                }
            }
        }
    }
}

/**
 * Scrolls so that [targetIdx] (the last calculation item) rests cleanly at the bottom edge of the visible viewport,
 * keeping the settings button just below the bottom fold until the user scrolls down further.
 */
suspend fun LazyListState.scrollToLastHistoryItem(targetIdx: Int) {
    if (targetIdx < 0) return
    val info = layoutInfo
    val target = info.visibleItemsInfo.find { it.index == targetIdx }
    if (target != null) {
        val diff = (target.offset + target.size) - info.viewportEndOffset
        if (diff != 0) {
            animateScrollBy(diff.toFloat(), tween(120, easing = FastOutSlowInEasing))
        }
    } else {
        animateScrollToItem(targetIdx)
        val settled = layoutInfo.visibleItemsInfo.find { it.index == targetIdx }
        if (settled != null) {
            val overflow = (settled.offset + settled.size) - layoutInfo.viewportEndOffset
            if (overflow != 0) {
                scrollBy(overflow.toFloat())
            }
        }
    }
}

@Composable
internal fun CalculatorHistoryView(
    modifier: Modifier = Modifier,
    history: List<HistoryEntry>,
    historyListState: LazyListState,
    inHistory: Boolean,
    historyIndex: Int,
    historySide: HistorySide,
    numberGrouping: NumberGrouping,
    maxDecimalPlaces: Int,
    onSelectHistory: (Int, HistorySide) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val maxButtonHeight = 92.dp
    val snapThresholdPx = with(density) { 98.dp.toPx() }

    val pullHeightAnim = remember { Animatable(0f) }
    var hasSnappedHaptic by remember { mutableStateOf(false) }
    var isPullGestureActive by remember { mutableStateOf(false) }

    // When scrolling stops, snap to open settings or spring back down to 0
    LaunchedEffect(historyListState.isScrollInProgress) {
        if (!historyListState.isScrollInProgress) {
            isPullGestureActive = false
            if (pullHeightAnim.value > 0f) {
                val current = pullHeightAnim.value
                if (current >= snapThresholdPx) {
                    HapticHelper.performClick(view)
                    pullHeightAnim.snapTo(0f)
                    hasSnappedHaptic = false
                    onOpenSettings()
                } else {
                    hasSnappedHaptic = false
                    pullHeightAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.76f,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
            }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If button is currently pulled up and user scrolls down (available.y > 0), shrink the button first
                val current = pullHeightAnim.value
                if (current > 0f && available.y > 0f) {
                    val consumedY = available.y.coerceAtMost(current)
                    val newHeight = (current - consumedY).coerceAtLeast(0f)
                    coroutineScope.launch { pullHeightAnim.snapTo(newHeight) }
                    if (newHeight < snapThresholdPx) {
                        hasSnappedHaptic = false
                    }
                    if (newHeight == 0f) {
                        isPullGestureActive = false
                    }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // CRITICAL: NEVER transfer fling/momentum from history scrolling to the settings button!
                // Only direct finger dragging (UserInput) when already at the bottom is allowed to pull the button.
                if (source != NestedScrollSource.UserInput) {
                    return Offset.Zero
                }

                // When at the bottom of history, user pulls UP (available.y < 0)
                if (available.y < 0f) {
                    isPullGestureActive = true
                    val pullDelta = -available.y * 0.55f
                    val current = pullHeightAnim.value
                    val newHeight = (current + pullDelta).coerceAtMost(snapThresholdPx * 1.35f)
                    coroutineScope.launch { pullHeightAnim.snapTo(newHeight) }

                    if (newHeight >= snapThresholdPx && !hasSnappedHaptic) {
                        HapticHelper.performTick(view)
                        hasSnappedHaptic = true
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (isPullGestureActive && pullHeightAnim.value > 0f) {
                    val current = pullHeightAnim.value
                    isPullGestureActive = false
                    if (current >= snapThresholdPx) {
                        HapticHelper.performClick(view)
                        pullHeightAnim.snapTo(0f)
                        hasSnappedHaptic = false
                        onOpenSettings()
                        return available
                    } else {
                        hasSnappedHaptic = false
                        pullHeightAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.76f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (isPullGestureActive && pullHeightAnim.value > 0f) {
                    val current = pullHeightAnim.value
                    isPullGestureActive = false
                    if (current >= snapThresholdPx) {
                        HapticHelper.performClick(view)
                        pullHeightAnim.snapTo(0f)
                        hasSnappedHaptic = false
                        onOpenSettings()
                        return available
                    } else {
                        hasSnappedHaptic = false
                        pullHeightAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.76f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                    }
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier.nestedScroll(nestedScrollConnection),
    ) {
        var historyHeightPx by remember { mutableIntStateOf(0) }
        var prevHistoryHeightPx by remember { mutableIntStateOf(0) }
        LaunchedEffect(historyHeightPx) {
            val prev = prevHistoryHeightPx
            if (prev > 0 && historyHeightPx in 1 until prev) {
                // Push the history window upwards when the input field below grows taller
                val delta = (prev - historyHeightPx).toFloat()
                historyListState.scrollBy(delta)
            }
            if (historyHeightPx > 0) {
                prevHistoryHeightPx = historyHeightPx
            }
        }

        // History List directly on background - ONLY calculations!
        LazyColumn(
            state = historyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .onSizeChanged { newSize ->
                    historyHeightPx = newSize.height
                },
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
            contentPadding = PaddingValues(top = 16.dp, bottom = 4.dp),
        ) {
            itemsIndexed(history) { idx, item ->
                val isRowSelected = inHistory && historyIndex == idx
                val isLeftSelected = isRowSelected && historySide == HistorySide.FORMULA
                val isRightSelected = isRowSelected && historySide == HistorySide.RESULT

                HistoryRowItem(
                    entry = item,
                    numberGrouping = numberGrouping,
                    maxDecimalPlaces = maxDecimalPlaces,
                    isRowSelected = isRowSelected,
                    isLeftSelected = isLeftSelected,
                    isRightSelected = isRightSelected,
                    onSelectLeft = {
                        HapticHelper.performClick(view)
                        onSelectHistory(idx, HistorySide.FORMULA)
                    },
                    onSelectRight = {
                        HapticHelper.performClick(view)
                        onSelectHistory(idx, HistorySide.RESULT)
                    },
                )
            }
        }

        // Expressive Pull-to-Reveal Settings Button anchored at bottom center
        val currentPullPx = pullHeightAnim.value
        val displayHeight = with(density) { currentPullPx.toDp() }.coerceIn(0.dp, maxButtonHeight)
        val isSnapped = currentPullPx >= snapThresholdPx
        val contentAlpha = ((displayHeight - 20.dp) / (maxButtonHeight - 20.dp)).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            ExpressivePullSettingsButton(
                height = displayHeight,
                isSnapped = isSnapped,
                contentAlpha = contentAlpha,
                onOpenSettings = {
                    HapticHelper.performClick(view)
                    coroutineScope.launch {
                        pullHeightAnim.snapTo(0f)
                        hasSnappedHaptic = false
                        onOpenSettings()
                    }
                },
            )
        }
    }
}
