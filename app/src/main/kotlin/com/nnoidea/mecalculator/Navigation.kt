package com.nnoidea.mecalculator

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.nnoidea.mecalculator.ui.main.MainScreen
import com.nnoidea.mecalculator.ui.settings.EditNumpadScreen
import com.nnoidea.mecalculator.ui.settings.SettingsScreen
import kotlinx.coroutines.CancellationException

@Composable
fun MainNavigation(
    initialScreen: Any = Main,
    demoRoute: String? = null,
) {
    var currentScreen by remember { mutableStateOf<Any>(initialScreen) }

    // Offset progress: 0f = fully open on screen, 1f = fully slid offscreen to the right
    val initialSettingsOpen = initialScreen == Settings || initialScreen == EditNumpad
    val initialEditOpen = initialScreen == EditNumpad
    val settingsSlideOut = remember { Animatable(if (initialSettingsOpen) 0f else 1f) }
    val editNumpadSlideOut = remember { Animatable(if (initialEditOpen) 0f else 1f) }

    // Handle programmatic screen changes (e.g. clicking Settings button or UI onBack)
    LaunchedEffect(currentScreen) {
        val target = if (currentScreen == Settings || currentScreen == EditNumpad) 0f else 1f
        if (settingsSlideOut.targetValue != target) {
            settingsSlideOut.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = if (target == 0f) 200 else 180,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    LaunchedEffect(currentScreen) {
        val target = if (currentScreen == EditNumpad) 0f else 1f
        if (editNumpadSlideOut.targetValue != target) {
            editNumpadSlideOut.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = if (target == 0f) 200 else 180,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    // Predictive back for EditNumpad -> Settings
    PredictiveBackHandler(enabled = currentScreen == EditNumpad) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                // Slide out to the right in real-time as user drags back
                editNumpadSlideOut.snapTo(backEvent.progress.coerceIn(0f, 1f))
            }
            currentScreen = Settings
            editNumpadSlideOut.animateTo(1f, animationSpec = tween(150, easing = FastOutSlowInEasing))
        } catch (e: CancellationException) {
            editNumpadSlideOut.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
        }
    }

    // Predictive back for Settings -> Main
    PredictiveBackHandler(enabled = currentScreen == Settings) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                // Slide out to the right in real-time as user drags back
                settingsSlideOut.snapTo(backEvent.progress.coerceIn(0f, 1f))
            }
            currentScreen = Main
            settingsSlideOut.animateTo(1f, animationSpec = tween(150, easing = FastOutSlowInEasing))
        } catch (e: CancellationException) {
            settingsSlideOut.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()

        // 1. Base Screen: MainScreen (always mounted underneath, full screen, 100% opaque)
        MainScreen(
            initialActiveFolderSlotIndex = if (demoRoute == "folder") 2 else null,
            initialZoomScale = if (demoRoute == "tall") 0.78f else 1.0f,
            onItemClick = { navKey ->
                currentScreen = navKey
            },
        )

        // 2. Settings Screen (standard full screen, slides in/out horizontally from right)
        if (settingsSlideOut.value < 1f || currentScreen == Settings || currentScreen == EditNumpad) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = settingsSlideOut.value * screenWidthPx
                    },
            ) {
                SettingsScreen(
                    onBack = { currentScreen = Main },
                    onOpenEditNumpad = { currentScreen = EditNumpad },
                )
            }
        }

        // 3. Edit Numpad Screen (standard full screen, slides in/out horizontally from right)
        if (editNumpadSlideOut.value < 1f || currentScreen == EditNumpad) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = editNumpadSlideOut.value * screenWidthPx
                    },
            ) {
                EditNumpadScreen(
                    onBack = { currentScreen = Settings },
                )
            }
        }
    }
}
