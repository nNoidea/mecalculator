package com.nnoidea.mecalculator.ui.settings

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nnoidea.mecalculator.data.AppSettingsRepository
import com.nnoidea.mecalculator.data.CopyOverMode
import com.nnoidea.mecalculator.data.NumberGrouping
import com.nnoidea.mecalculator.data.ScreenOrientationMode
import com.nnoidea.mecalculator.domain.NumberFormatter
import com.nnoidea.mecalculator.ui.theme.MECalculatorTheme
import com.nnoidea.mecalculator.ui.util.HapticHelper
import kotlin.math.roundToInt

private val ArrowBackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "AutoMirrored.Filled.ArrowBack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true,
    ).apply {
        path(fill = SolidColor(androidx.compose.ui.graphics.Color.White)) {
            moveTo(20f, 11f)
            horizontalLineTo(7.83f)
            lineToRelative(5.59f, -5.59f)
            lineTo(12f, 4f)
            lineToRelative(-8f, 8f)
            lineToRelative(8f, 8f)
            lineToRelative(1.41f, -1.41f)
            lineTo(7.83f, 13f)
            horizontalLineTo(20f)
            verticalLineToRelative(-2f)
            close()
        }
    }.build()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenEditNumpad: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) {
        AppSettingsRepository.init(context)
    }
    val settings by AppSettingsRepository.settings.collectAsState()

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.systemBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    ExpressiveSmallButton(
                        onClick = {
                            HapticHelper.performClick(view)
                            onBack()
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(42.dp),
                    ) {
                        Icon(
                            imageVector = ArrowBackIcon,
                            contentDescription = "Back",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val cutoutStart = innerPadding.calculateStartPadding(layoutDirection)
        val cutoutEnd = innerPadding.calculateEndPadding(layoutDirection)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = cutoutStart,
                    end = cutoutEnd,
                    top = innerPadding.calculateTopPadding(),
                )
                .verticalScroll(scrollState)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp + innerPadding.calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // 0. EDIT NUMPAD (4x6 + Folders)
            SettingsSection(title = "Numpad Layout") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            HapticHelper.performClick(view)
                            onOpenEditNumpad()
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Edit Numpad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Customize →",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // 1. COPY OVER (After pressing '=')
            SettingsSection(title = "Copy Over") {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    CopyOverMode.entries.forEach { option ->
                        val selected = settings.copyOverMode == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticHelper.performClick(view)
                                    AppSettingsRepository.setCopyOverMode(context, option)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = {
                                        HapticHelper.performClick(view)
                                        AppSettingsRepository.setCopyOverMode(context, option)
                                    },
                                )
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = option.example,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            // 2. SCREEN ORIENTATION
            SettingsSection(title = "Screen Orientation") {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    ScreenOrientationMode.entries.forEach { option ->
                        val selected = settings.screenOrientation == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticHelper.performClick(view)
                                    AppSettingsRepository.setScreenOrientation(context, option)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = {
                                    HapticHelper.performClick(view)
                                    AppSettingsRepository.setScreenOrientation(context, option)
                                },
                            )
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // 3. ARROW KEY HOLD & REPEAT DELAY (MS)
            SettingsSection(title = "Arrow Keys Hold & Fast Move") {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    DelaySettingRow(
                        title = "Hold Delay before Fast Move",
                        valueMs = settings.arrowHoldDelayMs,
                        minMs = 50,
                        maxMs = 800,
                        stepMs = 10,
                        onValueChange = { newMs ->
                            if (newMs != settings.arrowHoldDelayMs) {
                                HapticHelper.performTick(view)
                                AppSettingsRepository.setArrowHoldDelayMs(context, newMs)
                            }
                        },
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )

                    DelaySettingRow(
                        title = "Fast Move Step Delay",
                        valueMs = settings.arrowRepeatIntervalMs,
                        minMs = 15,
                        maxMs = 200,
                        stepMs = 5,
                        onValueChange = { newMs ->
                            if (newMs != settings.arrowRepeatIntervalMs) {
                                HapticHelper.performTick(view)
                                AppSettingsRepository.setArrowRepeatIntervalMs(context, newMs)
                            }
                        },
                    )
                }
            }

            // 4. MAX TRAILING DECIMALS
            SettingsSection(title = "Max Trailing Decimals") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Trailing digits limit",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ExpressiveSmallButton(
                            onClick = {
                                HapticHelper.performClick(view)
                                val next = (settings.maxDecimalPlaces - 1).coerceAtLeast(1)
                                AppSettingsRepository.setMaxDecimalPlaces(context, next)
                            },
                            modifier = Modifier.size(38.dp),
                        ) {
                            Text(
                                text = "−",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${settings.maxDecimalPlaces}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }

                        ExpressiveSmallButton(
                            onClick = {
                                HapticHelper.performClick(view)
                                val next = (settings.maxDecimalPlaces + 1).coerceAtMost(20)
                                AppSettingsRepository.setMaxDecimalPlaces(context, next)
                            },
                            modifier = Modifier.size(38.dp),
                        ) {
                            Text(
                                text = "+",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            // 5. NUMBER GROUPING
            SettingsSection(title = "Number Grouping") {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    NumberGrouping.entries.forEach { option ->
                        val selected = settings.numberGrouping == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HapticHelper.performClick(view)
                                    AppSettingsRepository.setNumberGrouping(context, option)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = {
                                        HapticHelper.performClick(view)
                                        AppSettingsRepository.setNumberGrouping(context, option)
                                    },
                                )
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = option.example,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
    }
}

@Composable
private fun DelaySettingRow(
    title: String,
    valueMs: Int,
    minMs: Int,
    maxMs: Int,
    stepMs: Int,
    onValueChange: (Int) -> Unit,
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExpressiveSmallButton(
                onClick = {
                    HapticHelper.performClick(view)
                    val next = (valueMs - stepMs).coerceAtLeast(minMs)
                    onValueChange(next)
                },
                modifier = Modifier.size(38.dp),
            ) {
                Text(
                    text = "−",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$valueMs ms",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            ExpressiveSmallButton(
                onClick = {
                    HapticHelper.performClick(view)
                    val next = (valueMs + stepMs).coerceAtMost(maxMs)
                    onValueChange(next)
                },
                modifier = Modifier.size(38.dp),
            ) {
                Text(
                    text = "+",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ExpressiveSmallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 16.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "smallBtnCorner",
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "smallBtnScale",
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(corner))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 820)
@Composable
fun SettingsScreenPreview() {
    MECalculatorTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreen(onBack = {})
    }
}
