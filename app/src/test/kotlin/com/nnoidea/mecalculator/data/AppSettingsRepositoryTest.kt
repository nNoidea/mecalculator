package com.nnoidea.mecalculator.data

import com.nnoidea.mecalculator.ui.main.HistoryEntry
import com.nnoidea.mecalculator.ui.main.MathToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsRepositoryTest {

    @Test
    fun serializeAndDeserializeTokens_roundTripsRecursiveNestedFractionsAndPowers() {
        val original = listOf(
            MathToken.FractionToken(
                num = listOf(
                    MathToken.FractionToken(
                        num = "1500000".map { MathToken.CharToken(it) },
                        den = listOf(MathToken.CharToken('3')),
                    ),
                ),
                den = listOf(MathToken.CharToken('5')),
            ),
            MathToken.CharToken('+'),
            MathToken.CharToken('2'),
            MathToken.PowerToken(exp = "10".map { MathToken.CharToken(it) }),
            MathToken.CharToken('−'),
            MathToken.CharToken('5'),
        )

        val encoded = AppSettingsRepository.serializeTokens(original)
        val decoded = AppSettingsRepository.deserializeTokens(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun serializeAndDeserializeHistory_roundTripsMultipleEntries() {
        val entries = listOf(
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
                ),
                result = "1024",
            ),
        )

        val encoded = AppSettingsRepository.serializeHistory(entries)
        val decoded = AppSettingsRepository.deserializeHistory(encoded)

        assertEquals(entries, decoded)
    }

    @Test
    fun deserializeEmptyHistory_returnsEmptyList() {
        assertTrue(AppSettingsRepository.deserializeHistory("").isEmpty())
        assertTrue(AppSettingsRepository.deserializeTokens("").isEmpty())
    }

    @Test
    fun serializeAndDeserializeTokens_roundTripsFuncTokens() {
        val original = listOf(
            MathToken.FuncToken("sin"),
            MathToken.CharToken('3'),
            MathToken.CharToken('0'),
            MathToken.CharToken(')'),
            MathToken.CharToken('+'),
            MathToken.FuncToken("√"),
            MathToken.CharToken('9'),
            MathToken.CharToken(')'),
        )

        val encoded = AppSettingsRepository.serializeTokens(original)
        val decoded = AppSettingsRepository.deserializeTokens(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun serializeAndDeserializeTokens_roundTripsSqrtTokens() {
        val original = listOf(
            MathToken.SqrtToken(rad = "144".map { MathToken.CharToken(it) }),
            MathToken.CharToken('+'),
            MathToken.SqrtToken(
                rad = listOf(
                    MathToken.SqrtToken(rad = "16".map { MathToken.CharToken(it) }),
                ),
            ),
        )

        val encoded = AppSettingsRepository.serializeTokens(original)
        val decoded = AppSettingsRepository.deserializeTokens(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun serializeAndDeserializeKeypadSlots_roundTripsCustomGridAndFolders() {
        val customSlots = KeypadCatalog.defaultSlots.toMutableList()
        // Swap slot 0 and slot 1
        val tmp = customSlots[0]
        customSlots[0] = customSlots[1]
        customSlots[1] = tmp
        // Put a scientific function in slot 2
        customSlots[2] = KeypadSlot.Key("sin")
        // Put a 4x6 Folder in slot 3
        customSlots[3] = KeypadSlot.Folder(
            title = "Sci",
            keyIds = KeypadCatalog.defaultFolderKeys,
        )

        val encoded = AppSettingsRepository.serializeKeypadSlots(customSlots)
        val decoded = AppSettingsRepository.deserializeKeypadSlots(encoded)

        assertEquals(24, decoded.size)
        assertEquals(customSlots, decoded)
    }

    @Test
    fun screenOrientationAndCopyOverModes_containExpectedEntries() {
        assertEquals(
            listOf(
                ScreenOrientationMode.FOLLOW_SYSTEM,
                ScreenOrientationMode.AUTO_ROTATE,
                ScreenOrientationMode.PORTRAIT,
                ScreenOrientationMode.LANDSCAPE,
            ),
            ScreenOrientationMode.entries,
        )
        assertEquals(
            listOf(
                CopyOverMode.RESULT,
                CopyOverMode.FORMULA,
                CopyOverMode.NOTHING,
            ),
            CopyOverMode.entries,
        )
    }

    @Test
    fun keypadCatalog_hasOrderedNumbersDedicatedOperatorsAndBlankEmptyKey() {
        val numberKeys = KeypadCatalog.allKeys
            .filter { it.category == KeyCategory.NUMBERS }
            .map { it.id }

        // Numbers must be ordered 1..9, 0, . (not calculator row order 789456123)
        assertEquals(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "."),
            numberKeys,
        )

        // Operators must be in their own category
        val operatorKeys = KeypadCatalog.allKeys
            .filter { it.category == KeyCategory.OPERATORS }
            .map { it.id }
        assertEquals(
            listOf("+", "−", "×", "÷", "xʸ", "(", ")", "=", "%"),
            operatorKeys,
        )

        // Empty key label must be literally blank (no dot)
        assertEquals("", KeypadCatalog.labelFor(""))

        // Every new folder must start completely empty (24 "" slots)
        assertEquals(24, KeypadCatalog.defaultFolderKeys.size)
        assertTrue(KeypadCatalog.defaultFolderKeys.all { it.isEmpty() })
    }

    @Test
    fun deserializeMalformedData_neverCrashesAndFallsBackSafely() {
        // Malformed token / history strings should not throw exceptions
        val badTokens = AppSettingsRepository.deserializeTokens("UNKNOWN_TYPE:123\u001FFRAC:only_one_part")
        assertTrue(badTokens.isEmpty())

        val badHistory = AppSettingsRepository.deserializeHistory("malformed_row_without_tab\n\t")
        assertTrue(badHistory.isEmpty())

        // Malformed keypad config falls back to 24 default slots
        val fallbackSlots = AppSettingsRepository.deserializeKeypadSlots("not_twenty_four_slots")
        assertEquals(24, fallbackSlots.size)
        assertEquals(KeypadCatalog.defaultSlots, fallbackSlots)
    }

    @Test
    fun serializeAndDeserializeLargeHistory_preservesAllEntriesWithoutLoss() {
        val largeHistory = (1..50).map { i ->
            HistoryEntry(
                tokens = "$i+$i".map { MathToken.CharToken(it) },
                result = "${i * 2}",
            )
        }
        val encoded = AppSettingsRepository.serializeHistory(largeHistory)
        val decoded = AppSettingsRepository.deserializeHistory(encoded)
        assertEquals(50, decoded.size)
        assertEquals(largeHistory, decoded)
    }

    @Test
    fun clearHistory_resetsHistoryStateAndClearsEditorState() {
        AppSettingsRepository.addHistoryEntry(
            null,
            HistoryEntry(
                tokens = "12·5".map { MathToken.CharToken(it) },
                result = "60",
            ),
        )
        AppSettingsRepository.updateEditorState(
            null,
            tokens = "12·5".map { MathToken.CharToken(it) },
            cursor = com.nnoidea.mecalculator.ui.main.CursorState(offset = 4),
            inHistory = true,
            historyIndex = 0,
            historySide = com.nnoidea.mecalculator.ui.main.HistorySide.FORMULA,
        )

        assertEquals(1, AppSettingsRepository.history.value.size)
        assertEquals(4, AppSettingsRepository.editorState.value.tokens.size)
        assertTrue(AppSettingsRepository.editorState.value.inHistory)

        AppSettingsRepository.clearHistory(null)

        assertTrue(AppSettingsRepository.history.value.isEmpty())
        assertEquals(0, AppSettingsRepository.historyIndex.value)
        org.junit.Assert.assertFalse(AppSettingsRepository.inHistory.value)
        assertEquals(com.nnoidea.mecalculator.ui.main.HistorySide.RESULT, AppSettingsRepository.historySide.value)
        assertTrue(AppSettingsRepository.editorState.value.tokens.isEmpty())
        assertEquals(0, AppSettingsRepository.editorState.value.cursor.offset)
        org.junit.Assert.assertFalse(AppSettingsRepository.editorState.value.inHistory)
    }
}
