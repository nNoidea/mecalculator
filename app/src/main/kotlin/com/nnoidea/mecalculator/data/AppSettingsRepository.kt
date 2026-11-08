package com.nnoidea.mecalculator.data

import android.content.Context
import android.content.pm.ActivityInfo
import com.nnoidea.mecalculator.ui.main.CursorState
import com.nnoidea.mecalculator.ui.main.HistoryEntry
import com.nnoidea.mecalculator.ui.main.HistorySide
import com.nnoidea.mecalculator.ui.main.MathToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NumberGrouping(
    val title: String,
    val example: String,
    val separator: String,
) {
    SPACE(title = "Space", example = "1 000 000", separator = " "),
    COMMA(title = "Comma", example = "1,000,000", separator = ","),
    NONE(title = "No grouping", example = "1000000", separator = ""),
}

enum class CopyOverMode(
    val title: String,
    val subtitle: String,
    val example: String,
) {
    RESULT(
        title = "Result",
        subtitle = "Copy the answer after pressing =",
        example = "5+5 → 10",
    ),
    FORMULA(
        title = "Keep the formula",
        subtitle = "Keep the expression after pressing =",
        example = "5+5 → 5+5",
    ),
    NOTHING(
        title = "Nothing (clean)",
        subtitle = "Clear the input box after pressing =",
        example = "5+5 → empty",
    ),
}

enum class ScreenOrientationMode(
    val title: String,
    val subtitle: String,
    val activityOrientation: Int,
) {
    FOLLOW_SYSTEM(
        title = "Follow system",
        subtitle = "Respect system auto-rotate setting",
        activityOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
    ),
    AUTO_ROTATE(
        title = "Auto-rotate",
        subtitle = "Always rotate with device sensor",
        activityOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
    ),
    PORTRAIT(
        title = "Always portrait",
        subtitle = "Lock in vertical orientation",
        activityOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
    ),
    LANDSCAPE(
        title = "Always landscape",
        subtitle = "Lock in horizontal orientation",
        activityOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
    ),
}

data class AppSettings(
    val numberGrouping: NumberGrouping = NumberGrouping.SPACE,
    val copyOverMode: CopyOverMode = CopyOverMode.RESULT,
    val screenOrientation: ScreenOrientationMode = ScreenOrientationMode.FOLLOW_SYSTEM,
    val arrowHoldDelayMs: Int = 250,
    val arrowRepeatIntervalMs: Int = 50,
    val maxDecimalPlaces: Int = 6,
    val keypadSlots: List<KeypadSlot> = KeypadCatalog.defaultSlots,
)

data class EditorState(
    val tokens: List<MathToken> = emptyList(),
    val cursor: CursorState = CursorState(),
    val inHistory: Boolean = false,
    val historyIndex: Int = 0,
    val historySide: HistorySide = HistorySide.RESULT,
)

object AppSettingsRepository {
    private const val PREFS_NAME = "mecalculator_settings"
    private const val KEY_GROUPING = "number_grouping"
    private const val KEY_COPY_OVER = "copy_over_mode"
    private const val KEY_ORIENTATION = "screen_orientation"
    private const val KEY_ARROW_HOLD_DELAY_MS = "arrow_hold_delay_ms"
    private const val KEY_ARROW_REPEAT_INTERVAL_MS = "arrow_repeat_interval_ms"
    private const val KEY_MAX_DECIMAL_PLACES = "max_decimal_places"
    private const val KEY_KEYPAD_SLOTS = "custom_keypad_slots_v1"
    private const val KEY_HISTORY = "persistent_history_v2"
    private const val KEY_ACTIVE_TOKENS = "persistent_active_tokens_v2"

    private const val ENTRY_PART_SEP = '\u001D'
    private const val ENTRY_SEP = '\n'

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _activeTokens = MutableStateFlow<List<MathToken>>(emptyList())
    val activeTokens: StateFlow<List<MathToken>> = _activeTokens.asStateFlow()

    private val _activeCursor = MutableStateFlow(CursorState())
    val activeCursor: StateFlow<CursorState> = _activeCursor.asStateFlow()

    private val _inHistory = MutableStateFlow(false)
    val inHistory: StateFlow<Boolean> = _inHistory.asStateFlow()

    private val _historyIndex = MutableStateFlow(0)
    val historyIndex: StateFlow<Int> = _historyIndex.asStateFlow()

    private val _historySide = MutableStateFlow(HistorySide.RESULT)
    val historySide: StateFlow<HistorySide> = _historySide.asStateFlow()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val groupingName = prefs.getString(KEY_GROUPING, NumberGrouping.SPACE.name)
        val grouping = NumberGrouping.entries.find { it.name == groupingName } ?: NumberGrouping.SPACE

        val copyOverName = prefs.getString(KEY_COPY_OVER, CopyOverMode.RESULT.name)
        val copyOver = CopyOverMode.entries.find { it.name == copyOverName } ?: CopyOverMode.RESULT

        val orientationName = prefs.getString(KEY_ORIENTATION, ScreenOrientationMode.FOLLOW_SYSTEM.name)
        val orientation = ScreenOrientationMode.entries.find { it.name == orientationName }
            ?: ScreenOrientationMode.FOLLOW_SYSTEM

        val holdDelayMs = prefs.getInt(KEY_ARROW_HOLD_DELAY_MS, 250).coerceIn(50, 1500)
        val repeatIntervalMs = prefs.getInt(KEY_ARROW_REPEAT_INTERVAL_MS, 50).coerceIn(15, 500)
        val maxDecimals = prefs.getInt(KEY_MAX_DECIMAL_PLACES, 6).coerceIn(1, 20)

        val rawKeypad = prefs.getString(KEY_KEYPAD_SLOTS, null)
        val keypadSlots = if (rawKeypad != null) {
            deserializeKeypadSlots(rawKeypad)
        } else {
            KeypadCatalog.defaultSlots
        }

        _settings.value = AppSettings(
            numberGrouping = grouping,
            copyOverMode = copyOver,
            screenOrientation = orientation,
            arrowHoldDelayMs = holdDelayMs,
            arrowRepeatIntervalMs = repeatIntervalMs,
            maxDecimalPlaces = maxDecimals,
            keypadSlots = keypadSlots,
        )

        val rawHistory = prefs.getString(KEY_HISTORY, null)
        if (rawHistory != null) {
            val loaded = deserializeHistory(rawHistory)
            _history.value = loaded
            _historyIndex.value = (loaded.size - 1).coerceAtLeast(0)
        }

        val rawTokens = prefs.getString(KEY_ACTIVE_TOKENS, null)
        if (rawTokens != null) {
            val loadedTokens = deserializeTokens(rawTokens)
            val loadedCursor = CursorState(path = emptyList(), offset = loadedTokens.size)
            _activeTokens.value = loadedTokens
            _activeCursor.value = loadedCursor
            _editorState.value = EditorState(tokens = loadedTokens, cursor = loadedCursor)
        }
    }

    fun setNumberGrouping(context: Context, grouping: NumberGrouping) {
        _settings.value = _settings.value.copy(numberGrouping = grouping)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GROUPING, grouping.name)
            .apply()
    }

    fun setCopyOverMode(context: Context, mode: CopyOverMode) {
        _settings.value = _settings.value.copy(copyOverMode = mode)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COPY_OVER, mode.name)
            .apply()
    }

    fun setScreenOrientation(context: Context, orientation: ScreenOrientationMode) {
        _settings.value = _settings.value.copy(screenOrientation = orientation)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ORIENTATION, orientation.name)
            .apply()
    }

    fun setArrowHoldDelayMs(context: Context, delayMs: Int) {
        val clamped = delayMs.coerceIn(50, 1500)
        _settings.value = _settings.value.copy(arrowHoldDelayMs = clamped)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_ARROW_HOLD_DELAY_MS, clamped)
            .apply()
    }

    fun setArrowRepeatIntervalMs(context: Context, intervalMs: Int) {
        val clamped = intervalMs.coerceIn(15, 500)
        _settings.value = _settings.value.copy(arrowRepeatIntervalMs = clamped)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_ARROW_REPEAT_INTERVAL_MS, clamped)
            .apply()
    }

    fun setMaxDecimalPlaces(context: Context, places: Int) {
        val clamped = places.coerceIn(1, 20)
        _settings.value = _settings.value.copy(maxDecimalPlaces = clamped)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MAX_DECIMAL_PLACES, clamped)
            .apply()
    }

    fun setKeypadSlots(context: Context?, slots: List<KeypadSlot>) {
        val normalized = (0 until 24).map { idx ->
            slots.getOrElse(idx) { KeypadCatalog.defaultSlots[idx] }
        }
        _settings.value = _settings.value.copy(keypadSlots = normalized)
        if (context != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_KEYPAD_SLOTS, serializeKeypadSlots(normalized))
                .apply()
        }
    }

    fun resetKeypadSlots(context: Context?) {
        setKeypadSlots(context, KeypadCatalog.defaultSlots)
    }

    fun serializeKeypadSlots(slots: List<KeypadSlot>): String =
        slots.joinToString("\n") { slot ->
            when (slot) {
                is KeypadSlot.Key -> "K:${slot.keyId}"
                is KeypadSlot.Folder -> {
                    val safeTitle = slot.title.replace("\n", " ").replace("\u001F", "")
                    val keysJoined = (0 until 24).joinToString("\u001E") { i ->
                        slot.keyIds.getOrElse(i) { "" }
                    }
                    "D:$safeTitle\u001F$keysJoined"
                }
            }
        }

    fun deserializeKeypadSlots(raw: String): List<KeypadSlot> {
        if (raw.isBlank()) return KeypadCatalog.defaultSlots
        val lines = raw.split('\n')
        if (lines.size != 24) return KeypadCatalog.defaultSlots
        return lines.mapIndexed { idx, line ->
            when {
                line.startsWith("K:") -> KeypadSlot.Key(line.removePrefix("K:"))
                line.startsWith("D:") -> {
                    val payload = line.removePrefix("D:")
                    val parts = payload.split('\u001F', limit = 2)
                    if (parts.size == 2) {
                        val title = parts[0].ifBlank { "ƒ(x)" }
                        val rawKeys = parts[1].split('\u001E')
                        val keyIds = (0 until 24).map { i -> rawKeys.getOrElse(i) { "" } }
                        KeypadSlot.Folder(title = title, keyIds = keyIds)
                    } else {
                        KeypadCatalog.defaultSlots[idx]
                    }
                }
                else -> KeypadCatalog.defaultSlots[idx]
            }
        }
    }

    fun updateEditorState(
        context: Context?,
        tokens: List<MathToken> = _editorState.value.tokens,
        cursor: CursorState = _editorState.value.cursor,
        inHistory: Boolean = _editorState.value.inHistory,
        historyIndex: Int = _editorState.value.historyIndex,
        historySide: HistorySide = _editorState.value.historySide,
    ) {
        val tokensChanged = _editorState.value.tokens != tokens
        val newState = EditorState(tokens, cursor, inHistory, historyIndex, historySide)
        _editorState.value = newState
        _activeTokens.value = tokens
        _activeCursor.value = cursor
        _inHistory.value = inHistory
        _historyIndex.value = historyIndex
        _historySide.value = historySide

        if (tokensChanged && context != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_TOKENS, serializeTokens(tokens))
                .apply()
        }
    }

    fun addHistoryEntry(context: Context?, entry: HistoryEntry) {
        val updated = _history.value + entry
        _history.value = updated
        persistHistory(context, updated)
    }

    fun clearHistory(context: Context?) {
        _history.value = emptyList()
        persistHistory(context, emptyList())
        _historyIndex.value = 0
        _inHistory.value = false
        _historySide.value = HistorySide.RESULT
        updateEditorState(
            context = context,
            tokens = emptyList(),
            cursor = CursorState(path = emptyList(), offset = 0),
            inHistory = false,
            historyIndex = 0,
            historySide = HistorySide.RESULT,
        )
    }

    fun seedDemoData(context: Context?, inHistory: Boolean = false, demoRoute: String? = null) {
        val effectiveInHistory = inHistory || demoRoute == "history"
        val sampleHistory = listOf(
            HistoryEntry(
                tokens = "500·1.19".map { MathToken.CharToken(it) },
                result = "595",
            ),
            HistoryEntry(
                tokens = listOf(
                    MathToken.FractionToken(
                        num = "125·8".map { MathToken.CharToken(it) },
                        den = listOf(MathToken.CharToken('5')),
                    ),
                ),
                result = "200",
            ),
            HistoryEntry(
                tokens = listOf(
                    MathToken.CharToken('1'),
                    MathToken.CharToken('.'),
                    MathToken.CharToken('6'),
                    MathToken.CharToken('9'),
                    MathToken.CharToken('·'),
                    MathToken.CharToken('1'),
                    MathToken.CharToken('0'),
                    MathToken.PowerToken(exp = "15".map { MathToken.CharToken(it) }),
                    MathToken.CharToken('·'),
                    MathToken.CharToken('2'),
                ),
                result = "3.38 · 10¹⁵",
            ),
            HistoryEntry(
                tokens = listOf(
                    MathToken.SqrtToken(rad = "256".map { MathToken.CharToken(it) }),
                    MathToken.CharToken('+'),
                    MathToken.CharToken('3'),
                    MathToken.PowerToken(exp = listOf(MathToken.CharToken('4'))),
                ),
                result = "97",
            ),
        )
        _history.value = sampleHistory
        _historyIndex.value = sampleHistory.lastIndex
        _historySide.value = HistorySide.RESULT

        // 2x3 Folder keys: rows 0..1, cols 1..3
        // Row 0: col 1 = sin, col 2 = folder origin (✕), col 3 = cos
        // Row 1: col 1 = tan, col 2 = ln, col 3 = log
        val folderKeys2x3 = List(24) { i ->
            when (i) {
                1 -> "sin"
                3 -> "cos"
                5 -> "tan"
                6 -> "ln"
                7 -> "log"
                else -> ""
            }
        }

        val demoSlots = KeypadCatalog.defaultSlots.toMutableList().apply {
            this[2] = KeypadSlot.Folder(title = "extra", keyIds = folderKeys2x3)
        }

        // Active tokens depend on the demoRoute
        val active = if (demoRoute == "tall") {
            // A deep, multi-tier mathematical expression with nested fractions and roots
            listOf(
                MathToken.FractionToken(
                    num = listOf(
                        MathToken.SqrtToken(
                            rad = listOf(
                                MathToken.SqrtToken(
                                    rad = "125·125".map { MathToken.CharToken(it) },
                                ),
                            ),
                        ),
                        MathToken.CharToken('+'),
                        MathToken.FractionToken(
                            num = listOf(
                                MathToken.CharToken('3'),
                                MathToken.CharToken('·'),
                                MathToken.CharToken('1'),
                                MathToken.CharToken('0'),
                                MathToken.PowerToken(exp = listOf(MathToken.CharToken('4'))),
                            ),
                            den = listOf(
                                MathToken.SqrtToken(rad = "25".map { MathToken.CharToken(it) }),
                            ),
                        ),
                    ),
                    den = listOf(
                        MathToken.FractionToken(
                            num = listOf(
                                MathToken.SqrtToken(rad = "625".map { MathToken.CharToken(it) }),
                                MathToken.CharToken('+'),
                                MathToken.CharToken('5'),
                                MathToken.PowerToken(exp = listOf(MathToken.CharToken('2'))),
                            ),
                            den = listOf(
                                MathToken.CharToken('2'),
                                MathToken.CharToken('·'),
                                MathToken.CharToken('π'),
                            ),
                        ),
                    ),
                ),
            )
        } else {
            // Complex equation: fraction with square root and power, constants, and multiplication
            listOf(
                MathToken.FractionToken(
                    num = listOf(
                        MathToken.SqrtToken(rad = "144".map { MathToken.CharToken(it) }),
                        MathToken.CharToken('+'),
                        MathToken.CharToken('5'),
                        MathToken.PowerToken(exp = listOf(MathToken.CharToken('3'))),
                    ),
                    den = listOf(
                        MathToken.CharToken('2'),
                        MathToken.CharToken('·'),
                        MathToken.CharToken('π'),
                    ),
                ),
                MathToken.CharToken('·'),
                MathToken.SqrtToken(rad = "25".map { MathToken.CharToken(it) }),
            )
        }

        val newEditor = EditorState(
            tokens = active,
            cursor = CursorState(path = emptyList(), offset = active.size),
            inHistory = effectiveInHistory,
            historyIndex = sampleHistory.lastIndex,
            historySide = HistorySide.RESULT,
        )
        _editorState.value = newEditor
        _activeTokens.value = active
        _activeCursor.value = CursorState(path = emptyList(), offset = active.size)
        _inHistory.value = effectiveInHistory

        val finalSlots = if (demoRoute == "tall") KeypadCatalog.defaultSlots else demoSlots
        setKeypadSlots(context, finalSlots)

        persistHistory(context, sampleHistory)
        if (context != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_TOKENS, serializeTokens(active))
                .apply()
        }
    }

    private fun persistHistory(context: Context?, entries: List<HistoryEntry>) {
        if (context == null) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, serializeHistory(entries))
            .apply()
    }

    fun serializeTokens(tokens: List<MathToken>): String =
        TokenSerialization.serializeTokens(tokens)

    fun deserializeTokens(raw: String): List<MathToken> =
        TokenSerialization.deserializeTokens(raw)

    fun serializeHistory(entries: List<HistoryEntry>): String =
        TokenSerialization.serializeHistory(entries)

    fun deserializeHistory(raw: String): List<HistoryEntry> =
        TokenSerialization.deserializeHistory(raw)
}
