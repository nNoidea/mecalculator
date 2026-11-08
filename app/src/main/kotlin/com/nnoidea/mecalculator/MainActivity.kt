package com.nnoidea.mecalculator

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.nnoidea.mecalculator.data.AppSettingsRepository
import com.nnoidea.mecalculator.ui.theme.MECalculatorTheme

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PAGE_ROUTE = "extra_page_route"
        const val EXTRA_DEMO_MODE = "extra_demo_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppSettingsRepository.init(this)

        val pageRoute = intent.getStringExtra(EXTRA_PAGE_ROUTE)
        val isDemo = intent.getBooleanExtra(EXTRA_DEMO_MODE, false) || pageRoute != null

        if (isDemo) {
            AppSettingsRepository.seedDemoData(this, demoRoute = pageRoute)
        }

        val initialScreen: Any = when (pageRoute) {
            "settings" -> Settings
            "customize", "edit_numpad" -> EditNumpad
            else -> Main
        }

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        unlockHighRefreshRate()

        applyImmersiveBars(resources.configuration.orientation)

        setContent {
            val settings by AppSettingsRepository.settings.collectAsState()
            val configuration = LocalConfiguration.current

            LaunchedEffect(settings.screenOrientation) {
                if (requestedOrientation != settings.screenOrientation.activityOrientation) {
                    requestedOrientation = settings.screenOrientation.activityOrientation
                }
            }

            LaunchedEffect(configuration.orientation) {
                applyImmersiveBars(configuration.orientation)
            }

            MECalculatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainNavigation(
                        initialScreen = initialScreen,
                        demoRoute = pageRoute,
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveBars(resources.configuration.orientation)
        }
    }

    private fun applyImmersiveBars(orientation: Int) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * Explicitly requests the display's maximum supported refresh rate (e.g. 90Hz, 120Hz, 144Hz+)
     * so that Android OEMs do not throttle Compose animations down to 60Hz.
     */
    private fun unlockHighRefreshRate() {
        val displayObj = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
        val modes = displayObj?.supportedModes ?: emptyArray()
        val maxRefreshMode = modes.maxByOrNull { it.refreshRate }
        if (maxRefreshMode != null && maxRefreshMode.refreshRate > 60f) {
            val params = window.attributes
            params.preferredDisplayModeId = maxRefreshMode.modeId
            window.attributes = params
        }
    }
}
