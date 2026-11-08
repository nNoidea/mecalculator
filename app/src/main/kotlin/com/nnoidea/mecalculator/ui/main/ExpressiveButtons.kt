package com.nnoidea.mecalculator.ui.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nnoidea.mecalculator.ui.util.HapticHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ExpressiveCalcButton(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    restingCornerPercent: Int = 50,
    fontSize: TextUnit = 20.sp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onPressDown: (() -> Unit)? = null,
    onPressUp: (() -> Unit)? = null,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnPressDown by rememberUpdatedState(onPressDown)
    val currentOnPressUp by rememberUpdatedState(onPressUp)

    var hasLongClickedInCurrentPress by remember { mutableStateOf(false) }

    // Trigger crisp tactile feedback immediately on pointer press down!
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    hasLongClickedInCurrentPress = false
                    HapticHelper.performClick(view)
                    currentOnPressDown?.invoke()
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    hasLongClickedInCurrentPress = false
                    currentOnPressUp?.invoke()
                }
            }
        }
    }

    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 22 else restingCornerPercent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cornerMorph",
    )
    val shape = RoundedCornerShape(percent = cornerPercent)

    val stableOnLongClick: (() -> Unit)? = remember(onLongClick != null) {
        if (onLongClick != null) {
            {
                if (!hasLongClickedInCurrentPress) {
                    hasLongClickedInCurrentPress = true
                    HapticHelper.performHeavyClick(view)
                    currentOnLongClick?.invoke()
                }
            }
        } else {
            null
        }
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = contentColor),
                    onClick = { currentOnClick() },
                    onLongClick = stableOnLongClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Expressive arrow button that moves immediately on press, and after [holdDelayMs]
 * rapidly repeats movement every [repeatIntervalMs] with crisp detent tick haptics.
 */
@Composable
internal fun ExpressiveArrowButton(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    holdDelayMs: Int,
    repeatIntervalMs: Int,
    onMove: () -> Boolean,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentOnMove by rememberUpdatedState(onMove)
    val currentHoldDelay by rememberUpdatedState(holdDelayMs)
    val currentRepeatInterval by rememberUpdatedState(repeatIntervalMs)

    var didRepeat by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        var repeatJob: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    didRepeat = false
                    HapticHelper.performClick(view)
                    repeatJob?.cancel()
                    repeatJob = launch {
                        delay(currentHoldDelay.toLong().coerceAtLeast(50L))
                        while (isActive) {
                            didRepeat = true
                            val moved = currentOnMove()
                            if (moved) {
                                HapticHelper.performTick(view)
                            }
                            delay(currentRepeatInterval.toLong().coerceAtLeast(15L))
                        }
                    }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    repeatJob?.cancel()
                    repeatJob = null
                }
            }
        }
    }

    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 22 else 50,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "arrowCornerMorph",
    )
    val shape = RoundedCornerShape(percent = cornerPercent)

    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = contentColor),
                    onClick = {
                        if (!didRepeat) {
                            currentOnMove()
                        }
                        didRepeat = false
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
