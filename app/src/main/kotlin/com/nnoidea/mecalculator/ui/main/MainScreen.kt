package com.nnoidea.mecalculator.ui.main

import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.nnoidea.mecalculator.Settings
import com.nnoidea.mecalculator.data.AppSettingsRepository
import com.nnoidea.mecalculator.data.CopyOverMode
import com.nnoidea.mecalculator.data.EditorState
import com.nnoidea.mecalculator.data.KeyCategory
import com.nnoidea.mecalculator.data.KeypadCatalog
import com.nnoidea.mecalculator.data.KeypadSlot
import com.nnoidea.mecalculator.data.NumberGrouping
import com.nnoidea.mecalculator.domain.ExactMathEngine
import com.nnoidea.mecalculator.domain.NumberFormatter
import com.nnoidea.mecalculator.ui.theme.MECalculatorTheme
import com.nnoidea.mecalculator.ui.util.HapticHelper
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    initialActiveFolderSlotIndex: Int? = null,
    initialZoomScale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AppSettingsRepository.init(context)
    }
    val settings by AppSettingsRepository.settings.collectAsState()
    val persistedHistory by AppSettingsRepository.history.collectAsState()
    val editorState by AppSettingsRepository.editorState.collectAsState()

    CalculatorScreen(
        modifier = modifier,
        numberGrouping = settings.numberGrouping,
        copyOverMode = settings.copyOverMode,
        arrowHoldDelayMs = settings.arrowHoldDelayMs,
        arrowRepeatIntervalMs = settings.arrowRepeatIntervalMs,
        maxDecimalPlaces = settings.maxDecimalPlaces,
        keypadSlots = settings.keypadSlots,
        initialActiveFolderSlotIndex = initialActiveFolderSlotIndex,
        initialZoomScale = initialZoomScale,
        externalEditorState = editorState,
        externalHistory = persistedHistory,
        onAddHistory = { entry -> AppSettingsRepository.addHistoryEntry(context, entry) },
        onClearHistory = { AppSettingsRepository.clearHistory(context) },
        onEditorStateChange = { tokens, cursor, inHistory, historyIndex, historySide ->
            AppSettingsRepository.updateEditorState(
                context = context,
                tokens = tokens,
                cursor = cursor,
                inHistory = inHistory,
                historyIndex = historyIndex,
                historySide = historySide,
            )
        },
        onOpenSettings = { onItemClick(Settings) },
    )
}

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
    numberGrouping: NumberGrouping = NumberGrouping.SPACE,
    copyOverMode: CopyOverMode = CopyOverMode.RESULT,
    arrowHoldDelayMs: Int = 250,
    arrowRepeatIntervalMs: Int = 50,
    maxDecimalPlaces: Int = 6,
    keypadSlots: List<KeypadSlot> = KeypadCatalog.defaultSlots,
    initialActiveFolderSlotIndex: Int? = null,
    initialZoomScale: Float = 1f,
    initialInHistory: Boolean = false,
    initialHistorySide: HistorySide = HistorySide.RESULT,
    externalEditorState: EditorState? = null,
    externalHistory: List<HistoryEntry>? = null,
    externalTokens: List<MathToken>? = null,
    externalCursor: CursorState? = null,
    externalInHistory: Boolean? = null,
    externalHistoryIndex: Int? = null,
    externalHistorySide: HistorySide? = null,
    onAddHistory: ((HistoryEntry) -> Unit)? = null,
    onClearHistory: (() -> Unit)? = null,
    onEditorStateChange: ((List<MathToken>, CursorState, Boolean, Int, HistorySide) -> Unit)? = null,
    onOpenSettings: () -> Unit = {},
) {
    val localHistory = remember {
        mutableStateListOf(
            HistoryEntry(
                tokens = "12500+87500".map { MathToken.CharToken(it) },
                result = "100000",
            ),
            HistoryEntry(
                tokens = listOf(
                    MathToken.FractionToken(
                        num = "3000000".map { MathToken.CharToken(it) },
                        den = listOf(MathToken.CharToken('3')),
                    ),
                ),
                result = "1000000",
            ),
            HistoryEntry(
                tokens = listOf(
                    MathToken.CharToken('2'),
                    MathToken.PowerToken(exp = "10".map { MathToken.CharToken(it) }),
                    MathToken.CharToken('·'),
                    MathToken.CharToken('1'),
                    MathToken.CharToken('0'),
                    MathToken.CharToken('0'),
                    MathToken.CharToken('0'),
                ),
                result = "1024000",
            ),
        )
    }

    var localEditorState by remember {
        mutableStateOf(
            externalEditorState ?: EditorState(
                tokens = externalTokens ?: listOf(
                    MathToken.FractionToken(
                        num = "1500000".map { MathToken.CharToken(it) },
                        den = listOf(MathToken.CharToken('3')),
                    ),
                    MathToken.CharToken('+'),
                    MathToken.CharToken('2'),
                    MathToken.PowerToken(exp = listOf(MathToken.CharToken('3'))),
                ),
                cursor = externalCursor ?: CursorState(
                    path = listOf(CursorStep(tokenIndex = 0, slot = SlotType.FRAC_DEN)),
                    offset = 1,
                ),
                inHistory = externalInHistory ?: initialInHistory,
                historyIndex = externalHistoryIndex ?: ((externalHistory ?: emptyList()).size - 1).coerceAtLeast(0),
                historySide = externalHistorySide ?: initialHistorySide,
            ),
        )
    }

    LaunchedEffect(externalEditorState) {
        if (externalEditorState != null && externalEditorState != localEditorState) {
            localEditorState = externalEditorState
        }
    }

    LaunchedEffect(externalTokens, externalCursor, externalInHistory, externalHistoryIndex, externalHistorySide) {
        if (externalTokens != null || externalCursor != null || externalInHistory != null || externalHistoryIndex != null || externalHistorySide != null) {
            localEditorState = localEditorState.copy(
                tokens = externalTokens ?: localEditorState.tokens,
                cursor = externalCursor ?: localEditorState.cursor,
                inHistory = externalInHistory ?: localEditorState.inHistory,
                historyIndex = externalHistoryIndex ?: localEditorState.historyIndex,
                historySide = externalHistorySide ?: localEditorState.historySide,
            )
        }
    }

    val history = externalHistory ?: localHistory
    val tokens = localEditorState.tokens
    val cursor = localEditorState.cursor
    val inHistory = localEditorState.inHistory
    val historyIndex = localEditorState.historyIndex
    val historySide = localEditorState.historySide

    var resetZoomTrigger by remember { mutableIntStateOf(0) }

    fun updateState(
        newTokens: List<MathToken> = localEditorState.tokens,
        newCursor: CursorState = localEditorState.cursor,
        newInHistory: Boolean = localEditorState.inHistory,
        newHistoryIndex: Int = localEditorState.historyIndex,
        newHistorySide: HistorySide = localEditorState.historySide,
    ) {
        val newState = EditorState(newTokens, newCursor, newInHistory, newHistoryIndex, newHistorySide)
        localEditorState = newState
        if (onEditorStateChange != null) {
            onEditorStateChange(newTokens, newCursor, newInHistory, newHistoryIndex, newHistorySide)
        }
    }

    val historyListState = rememberLazyListState()

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val configuration = LocalConfiguration.current
    val availableHistoryWidthPx = with(density) { (configuration.screenWidthDp.dp - 24.dp).toPx() }

    val charSample17 = remember(textMeasurer) {
        textMeasurer.measure(
            text = "0",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
    val charWidthPx = charSample17.size.width.toFloat()
    val charHeightPx = charSample17.size.height.toFloat()

    // Focus the last calculation item when history changes
    LaunchedEffect(history.size) {
        if (history.isNotEmpty() && !inHistory) {
            historyListState.scrollToLastHistoryItem(history.lastIndex)
        }
    }

    // Only scroll when the selected item is out of view (pushes it in at the top or bottom edge)
    LaunchedEffect(inHistory, historyIndex) {
        if (inHistory && history.isNotEmpty()) {
            val targetIdx = historyIndex.coerceIn(0, history.lastIndex)
            historyListState.scrollItemIntoViewIfNeeded(targetIdx)
        }
    }

    fun getResultTokensToPrepend(isOp: Boolean): List<MathToken>? {
        if (!isOp) return null
        val curTokens = localEditorState.tokens
        val curInHistory = localEditorState.inHistory
        val curHistoryIndex = localEditorState.historyIndex
        if (curTokens.isNotEmpty()) return null
        if (history.isEmpty()) return null

        val targetResult = if (curInHistory && history.isNotEmpty()) {
            history.getOrNull(curHistoryIndex)?.result ?: history.last().result
        } else {
            history.lastOrNull()?.result
        } ?: return null

        val resultTokens = NumberFormatter.resultStringToTokens(targetResult)
        return if (resultTokens.isNotEmpty()) resultTokens else null
    }

    fun insertTokensAtCursor(toInsert: List<MathToken>) {
        val (newRoot, newCursor) = insertTokensAt(localEditorState.tokens, localEditorState.cursor, toInsert)
        updateState(
            newTokens = newRoot,
            newCursor = newCursor,
            newInHistory = false,
        )
    }

    fun insertChars(text: String) {
        val curTokens = localEditorState.tokens
        val curCursor = localEditorState.cursor
        val isOp = text in listOf("+", "−", "-", "×", "·", "%", "/", "*")
        val prependTokens = getResultTokensToPrepend(isOp)
        val baseTokens = prependTokens ?: curTokens
        val baseCursor = if (prependTokens != null) CursorState(path = emptyList(), offset = prependTokens.size) else curCursor
        val (newRoot, newCursor) = insertTokensAt(baseTokens, baseCursor, NumberFormatter.resultStringToTokens(text))
        updateState(
            newTokens = newRoot,
            newCursor = newCursor,
            newInHistory = false,
        )
    }

    fun insertFunc(name: String) {
        insertTokensAtCursor(listOf(MathToken.FuncToken(name)))
    }

    fun insertFraction() {
        val curTokens = localEditorState.tokens
        val curCursor = localEditorState.cursor
        val prependTokens = getResultTokensToPrepend(isOp = true)
        val baseTokens = prependTokens ?: curTokens
        val baseCursor = if (prependTokens != null) CursorState(path = emptyList(), offset = prependTokens.size) else curCursor
        val (newRoot, newCursor) = insertFractionAt(baseTokens, baseCursor)
        updateState(
            newTokens = newRoot,
            newCursor = newCursor,
            newInHistory = false,
        )
    }

    fun insertPower() {
        val curTokens = localEditorState.tokens
        val curCursor = localEditorState.cursor
        val prependTokens = getResultTokensToPrepend(isOp = true)
        val baseTokens = prependTokens ?: curTokens
        val baseCursor = if (prependTokens != null) CursorState(path = emptyList(), offset = prependTokens.size) else curCursor
        val (newRoot, newCursor) = insertPowerAt(baseTokens, baseCursor)
        if (newRoot != curTokens || newCursor != curCursor) {
            updateState(
                newTokens = newRoot,
                newCursor = newCursor,
                newInHistory = false,
            )
        }
    }

    fun insertSqrt() {
        val (newRoot, newCursor) = insertSqrtAt(localEditorState.tokens, localEditorState.cursor)
        updateState(
            newTokens = newRoot,
            newCursor = newCursor,
            newInHistory = false,
        )
    }

    fun handleBackspace() {
        if (localEditorState.inHistory) {
            updateState(newInHistory = false)
            return
        }
        val (newRoot, newCursor) = backspaceAt(localEditorState.tokens, localEditorState.cursor)
        updateState(
            newTokens = newRoot,
            newCursor = newCursor,
        )
    }

    val currentOnClearHistory by rememberUpdatedState(onClearHistory)
    val deleteHandler = remember {
        DeleteKeypressHandler(
            getTokens = { localEditorState.tokens },
            onClearTokens = {
                resetZoomTrigger++
                updateState(
                    newTokens = emptyList(),
                    newCursor = CursorState(path = emptyList(), offset = 0),
                    newInHistory = false,
                )
            },
            onClearHistory = {
                resetZoomTrigger++
                updateState(
                    newTokens = emptyList(),
                    newCursor = CursorState(path = emptyList(), offset = 0),
                    newInHistory = false,
                    newHistoryIndex = 0,
                    newHistorySide = HistorySide.RESULT,
                )
                if (currentOnClearHistory != null) {
                    currentOnClearHistory?.invoke()
                } else {
                    localHistory.clear()
                }
            },
        )
    }

    fun handleDeletePressDown() {
        deleteHandler.onPressDown()
    }

    fun handleDeletePressUp() {
        deleteHandler.onPressUp()
    }

    fun handleLongPressDelete() {
        deleteHandler.onLongPress()
    }

    fun moveCursor(dir: String): Boolean {
        val curInHistory = localEditorState.inHistory
        val curTokens = localEditorState.tokens
        val curCursor = localEditorState.cursor
        val curHistoryIndex = localEditorState.historyIndex
        val curHistorySide = localEditorState.historySide

        if (curInHistory) {
            val navResult = computeNextHistorySelection(
                dir = dir,
                currentIndex = curHistoryIndex,
                currentSide = curHistorySide,
                historySize = history.size,
                isEntryStacked = { idx ->
                    isHistoryEntryStacked(
                        entry = history[idx],
                        numberGrouping = numberGrouping,
                        maxDecimalPlaces = maxDecimalPlaces,
                        availableWidthPx = availableHistoryWidthPx,
                        charWidthPx = charWidthPx,
                        charHeightPx = charHeightPx,
                        density = density,
                        textMeasurer = textMeasurer,
                    )
                },
            )

            return when (navResult) {
                is HistoryNavResult.StayInHistory -> {
                    updateState(
                        newHistoryIndex = navResult.index,
                        newHistorySide = navResult.side,
                    )
                    true
                }
                is HistoryNavResult.ExitToEditor -> {
                    updateState(newInHistory = false)
                    true
                }
                is HistoryNavResult.Unhandled -> false
            }
        }

        val (nextCursor, moved) = moveCursorInTokens(curTokens, curCursor, dir)
        if (moved) {
            updateState(newCursor = nextCursor)
            return true
        }
        if (dir == "UP" && history.isNotEmpty()) {
            val lastIdx = history.size - 1
            updateState(
                newInHistory = true,
                newHistoryIndex = lastIdx,
                newHistorySide = HistorySide.RESULT,
            )
            return true
        }
        return false
    }

    fun handleEqualsOrOk() {
        val curInHistory = localEditorState.inHistory
        val curTokens = localEditorState.tokens
        val curCursor = localEditorState.cursor
        val curHistoryIndex = localEditorState.historyIndex
        val curHistorySide = localEditorState.historySide

        if (curInHistory && history.isNotEmpty() && curTokens.isEmpty()) {
            val item = history[curHistoryIndex.coerceIn(0, history.lastIndex)]
            if (curHistorySide == HistorySide.FORMULA) {
                insertTokensAtCursor(item.tokens)
            } else {
                insertTokensAtCursor(NumberFormatter.resultStringToTokens(item.result))
            }
        } else {
            val evaluated = ExactMathEngine.evaluateTokens(curTokens)
            if (curTokens.isNotEmpty() && evaluated != null) {
                val newEntry = HistoryEntry(tokens = curTokens.toList(), result = evaluated)
                if (onAddHistory != null) {
                    onAddHistory(newEntry)
                } else {
                    localHistory.add(newEntry)
                }
                when (copyOverMode) {
                    CopyOverMode.RESULT -> {
                        resetZoomTrigger++
                        val newTokens = NumberFormatter.resultStringToTokens(evaluated)
                        updateState(
                            newTokens = newTokens,
                            newCursor = CursorState(path = emptyList(), offset = newTokens.size),
                            newInHistory = false,
                        )
                    }
                    CopyOverMode.FORMULA -> {
                        // Formula stays in the editor -> preserve the user's active zoom and pan
                        updateState(
                            newTokens = curTokens,
                            newCursor = CursorState(path = emptyList(), offset = curTokens.size),
                            newInHistory = false,
                        )
                    }
                    CopyOverMode.NOTHING -> {
                        resetZoomTrigger++
                        updateState(
                            newTokens = emptyList(),
                            newCursor = CursorState(path = emptyList(), offset = 0),
                            newInHistory = false,
                        )
                    }
                }
            }
        }
    }

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val rootWindowInsets = if (isLandscape) {
        WindowInsets.displayCutout
    } else {
        WindowInsets.systemBars
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            .union(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (isLandscape) {
            // LANDSCAPE: Fullscreen Left Viewport (History + Input) | Right Numberpad
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(rootWindowInsets)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Left Column: History on top, Input Field at bottom
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        CalculatorHistoryView(
                            modifier = Modifier.fillMaxSize(),
                            history = history,
                            historyListState = historyListState,
                            inHistory = inHistory,
                            historyIndex = historyIndex,
                            historySide = historySide,
                            numberGrouping = numberGrouping,
                            maxDecimalPlaces = maxDecimalPlaces,
                            onSelectHistory = { idx, side ->
                                updateState(
                                    newInHistory = true,
                                    newHistoryIndex = idx,
                                    newHistorySide = side,
                                )
                            },
                            onOpenSettings = onOpenSettings,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        CalculatorInputField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            tokens = tokens,
                            cursor = cursor,
                            inHistory = inHistory,
                            numberGrouping = numberGrouping,
                            onFocusInput = {
                                updateState(
                                    newCursor = CursorState(path = emptyList(), offset = tokens.size),
                                    newInHistory = false,
                                )
                            },
                            onTokenClick = { newCursor ->
                                updateState(
                                    newCursor = newCursor,
                                    newInHistory = false,
                                )
                            },
                            resetZoomTrigger = resetZoomTrigger,
                            initialZoomScale = initialZoomScale,
                        )
                    }
                }

                // Right Column: Keypad
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    CalculatorKeypad(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        keypadSlots = keypadSlots,
                        inHistory = inHistory,
                        isEditorEmpty = tokens.isEmpty(),
                        arrowHoldDelayMs = arrowHoldDelayMs,
                        arrowRepeatIntervalMs = arrowRepeatIntervalMs,
                        initialActiveFolderSlotIndex = initialActiveFolderSlotIndex,
                        onInsertChars = { text -> insertChars(text) },
                        onInsertFunc = { name -> insertFunc(name) },
                        onInsertPower = { insertPower() },
                        onInsertFraction = { insertFraction() },
                        onInsertSqrt = { insertSqrt() },
                        onMoveCursor = { dir -> moveCursor(dir) },
                        onEqualsOrOk = { handleEqualsOrOk() },
                        onBackspace = { handleBackspace() },
                        onLongPressDelete = { handleLongPressDelete() },
                        onDeletePressDown = { handleDeletePressDown() },
                        onDeletePressUp = { handleDeletePressUp() },
                    )
                }
            }
        } else {
            // PORTRAIT: Top History | Bottom Unified Input Field + Numberpad Deck
            // The Keypad height is kept stable at 50% of screen height so it NEVER shrinks.
            // As the input field grows taller (e.g. multi-level fractions), it expands UPWARDS into the history area.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(rootWindowInsets),
            ) {
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val totalH = maxHeight - navBarBottom
                val keypadH = totalH * 0.50f
                val maxInputH = (totalH - keypadH - 72.dp).coerceAtLeast(140.dp)

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Viewport: History & Settings (absorbs remaining space)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        CalculatorHistoryView(
                            modifier = Modifier.fillMaxSize(),
                            history = history,
                            historyListState = historyListState,
                            inHistory = inHistory,
                            historyIndex = historyIndex,
                            historySide = historySide,
                            numberGrouping = numberGrouping,
                            maxDecimalPlaces = maxDecimalPlaces,
                            onSelectHistory = { idx, side ->
                                updateState(
                                    newInHistory = true,
                                    newHistoryIndex = idx,
                                    newHistorySide = side,
                                )
                            },
                            onOpenSettings = onOpenSettings,
                        )
                    }

                    // Bottom Deck: Input Field + Keypad Surface with Rounded Top Corners
                    // wrapContentHeight allows the deck to grow UPWARDS without squishing the keypad
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                        ) {
                            CalculatorInputField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp, max = maxInputH)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                tokens = tokens,
                                cursor = cursor,
                                inHistory = inHistory,
                                numberGrouping = numberGrouping,
                                onFocusInput = {
                                    updateState(
                                        newCursor = CursorState(path = emptyList(), offset = tokens.size),
                                        newInHistory = false,
                                    )
                                },
                                onTokenClick = { newCursor ->
                                    updateState(
                                        newCursor = newCursor,
                                        newInHistory = false,
                                    )
                                },
                                resetZoomTrigger = resetZoomTrigger,
                                initialZoomScale = initialZoomScale,
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            CalculatorKeypad(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(keypadH),
                                keypadSlots = keypadSlots,
                                inHistory = inHistory,
                                isEditorEmpty = tokens.isEmpty(),
                                arrowHoldDelayMs = arrowHoldDelayMs,
                                arrowRepeatIntervalMs = arrowRepeatIntervalMs,
                                initialActiveFolderSlotIndex = initialActiveFolderSlotIndex,
                                onInsertChars = { text -> insertChars(text) },
                                onInsertFunc = { name -> insertFunc(name) },
                                onInsertPower = { insertPower() },
                                onInsertFraction = { insertFraction() },
                                onInsertSqrt = { insertSqrt() },
                                onMoveCursor = { dir -> moveCursor(dir) },
                                onEqualsOrOk = { handleEqualsOrOk() },
                                onBackspace = { handleBackspace() },
                                onLongPressDelete = { handleLongPressDelete() },
                                onDeletePressDown = { handleDeletePressDown() },
                                onDeletePressUp = { handleDeletePressUp() },
                            )
                        }
                    }
                }
            }
        }
    }
}


@Preview(name = "Portrait Mode (Editing)", showBackground = true, widthDp = 390, heightDp = 820)
@Composable
fun CalculatorEditingPreview() {
    MECalculatorTheme(darkTheme = true, dynamicColor = false) {
        CalculatorScreen(
            numberGrouping = NumberGrouping.SPACE,
            initialInHistory = false,
        )
    }
}

@Preview(name = "Portrait Mode (History Focused)", showBackground = true, widthDp = 390, heightDp = 820)
@Composable
fun CalculatorHistoryFocusedPreview() {
    MECalculatorTheme(darkTheme = true, dynamicColor = false) {
        CalculatorScreen(
            numberGrouping = NumberGrouping.SPACE,
            initialInHistory = true,
            initialHistorySide = HistorySide.FORMULA,
        )
    }
}

@Preview(name = "Portrait Mode (Deep Fractions)", showBackground = true, widthDp = 390, heightDp = 820)
@Composable
fun CalculatorDeepFractionsPreview() {
    MECalculatorTheme(darkTheme = true, dynamicColor = false) {
        val nested = listOf(
            MathToken.FractionToken(
                num = "125".map { MathToken.CharToken(it) },
                den = listOf(
                    MathToken.FractionToken(
                        num = "52".map { MathToken.CharToken(it) },
                        den = listOf(
                            MathToken.FractionToken(
                                num = "8".map { MathToken.CharToken(it) },
                                den = listOf(
                                    MathToken.FractionToken(
                                        num = "4".map { MathToken.CharToken(it) },
                                        den = listOf(
                                            MathToken.FractionToken(
                                                num = "2".map { MathToken.CharToken(it) },
                                                den = listOf(MathToken.CharToken('1')),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        CalculatorScreen(
            numberGrouping = NumberGrouping.SPACE,
            initialInHistory = false,
            externalTokens = nested,
            externalCursor = CursorState(),
        )
    }
}

@Preview(name = "Portrait Mode (Long Calculation)", showBackground = true, widthDp = 390, heightDp = 820)
@Composable
fun CalculatorLongExpressionPreview() {
    MECalculatorTheme(darkTheme = true, dynamicColor = false) {
        val longTokens = "123456789+987654321·456789-123456789+987654321".map { MathToken.CharToken(it) }
        CalculatorScreen(
            numberGrouping = NumberGrouping.SPACE,
            initialInHistory = false,
            externalTokens = longTokens,
            externalCursor = CursorState(offset = longTokens.size),
        )
    }
}

@Preview(name = "Landscape Mode (Left Viewport / Right Keypad)", showBackground = true, widthDp = 820, heightDp = 390)
@Composable
fun CalculatorLandscapePreview() {
    MECalculatorTheme(darkTheme = true, dynamicColor = false) {
        CalculatorScreen(
            numberGrouping = NumberGrouping.SPACE,
            initialInHistory = true,
            initialHistorySide = HistorySide.FORMULA,
        )
    }
}

@Preview(name = "Portrait Mode (History User Cases)", showBackground = true, widthDp = 390, heightDp = 820)
@Composable
fun CalculatorHistoryUserCasesPreview() {
    MECalculatorTheme(darkTheme = true, dynamicColor = false) {
        val userHistory = listOf(
            HistoryEntry(
                tokens = "5·5".map { MathToken.CharToken(it) },
                result = "25",
            ),
            HistoryEntry(
                tokens = "5·5+5+5+5".map { MathToken.CharToken(it) },
                result = "40",
            ),
            HistoryEntry(
                tokens = listOf(
                    MathToken.CharToken('('),
                    MathToken.CharToken('5'),
                    MathToken.CharToken('+'),
                    MathToken.CharToken('5'),
                    MathToken.CharToken('+'),
                    MathToken.CharToken('5'),
                    MathToken.CharToken('+'),
                    MathToken.FractionToken(
                        num = listOf(MathToken.CharToken('5')),
                        den = listOf(MathToken.CharToken('2')),
                    ),
                    MathToken.CharToken(')'),
                    MathToken.CharToken('+'),
                    MathToken.CharToken('5'),
                ),
                result = "22.5",
            ),
        )
        CalculatorScreen(
            numberGrouping = NumberGrouping.NONE,
            initialInHistory = true,
            initialHistorySide = HistorySide.FORMULA,
            externalHistory = userHistory,
        )
    }
}
