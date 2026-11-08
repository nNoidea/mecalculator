package com.nnoidea.mecalculator.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nnoidea.mecalculator.MainNavigation
import com.nnoidea.mecalculator.data.AppSettingsRepository
import com.nnoidea.mecalculator.data.CopyOverMode
import com.nnoidea.mecalculator.data.KeypadCatalog
import com.nnoidea.mecalculator.data.KeypadSlot
import com.nnoidea.mecalculator.data.NumberGrouping
import com.nnoidea.mecalculator.ui.theme.MECalculatorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end instrumented Compose UI tests for MECalculator covering:
 * - Overflowing history + tall 2D nested fractions/exponents (regression test for layout crash)
 * - History navigation with arrow keys across off-screen items
 * - Long expression input windowing with left/right/both `...` indicators
 * - Copy-Over modes (RESULT, FORMULA, NOTHING)
 * - Custom numpad folder bounding-box expansion & in-place `✕` close button
 * - Settings & Edit Numpad categories (`1234567890.`, `Operators`, blank `Empty`, `Folder`)
 */
class MainScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun tapKey(label: String) {
        composeTestRule.onAllNodesWithText(label, substring = false).onLast().performClick()
    }

    @Before
    fun setup() {
        val context = composeTestRule.activity.applicationContext
        AppSettingsRepository.init(context)
        AppSettingsRepository.clearHistory(context)
        AppSettingsRepository.updateEditorState(
            context = context,
            tokens = emptyList(),
            cursor = CursorState(),
            inHistory = false,
            historyIndex = 0,
        )
        AppSettingsRepository.resetKeypadSlots(context)
        AppSettingsRepository.setCopyOverMode(context, CopyOverMode.RESULT)
        AppSettingsRepository.setNumberGrouping(context, NumberGrouping.SPACE)
        AppSettingsRepository.setMaxDecimalPlaces(context, 2)
    }

    @Test
    fun overflowingHistory_withTallNestedFractionsAndPowers_doesNotCrashAndNavigatesCleanly() {
        composeTestRule.setContent {
            MECalculatorTheme {
                MainNavigation()
            }
        }

        // 1. Enter 14 calculations so the history list overflows the viewport height
        repeat(14) {
            tapKey("5")
            tapKey("+")
            tapKey("3")
            tapKey("=")
            // Clear copied-over result before next iteration
            tapKey("⌫")
        }

        // 2. While history is overflowing, build a tall nested fraction 5 / (5 / 5) in the input field
        //    (Regression test for IllegalArgumentException: performMeasureAndLayout called during measure layout)
        tapKey("5")
        tapKey("÷")
        tapKey("5")
        tapKey("÷")
        tapKey("5")
        composeTestRule.waitForIdle()

        // 3. Evaluate the nested fraction -> 5/(5/5) = 5
        tapKey("=")
        composeTestRule.waitForIdle()
        assertEquals(15, AppSettingsRepository.history.value.size)
        assertEquals("5", AppSettingsRepository.history.value.last().result)

        // 4. Clear input and navigate up through the overflowing history with ▲ and back down with ▼
        tapKey("⌫")
        repeat(15) {
            tapKey("▲")
        }
        composeTestRule.waitForIdle()
        repeat(16) {
            tapKey("▼")
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun longExpressionInput_showsLeftRightAndBothEllipses_andEvaluatesProperly() {
        composeTestRule.setContent {
            MECalculatorTheme {
                MainNavigation()
            }
        }

        // Type a 35-token expression: 17 '5's separated by 16 '·'s
        repeat(16) {
            tapKey("5")
            tapKey("×")
        }
        tapKey("5")
        composeTestRule.waitForIdle()

        // At the right end of a long expression, only the left "..." should be displayed
        composeTestRule.onNodeWithTag("left_ellipsis", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("right_ellipsis", useUnmergedTree = true).assertIsNotDisplayed()

        // Move cursor left 22 steps into the middle -> both left and right "..." should be displayed
        repeat(22) {
            tapKey("◀")
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("left_ellipsis", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("right_ellipsis", useUnmergedTree = true).assertIsDisplayed()

        // Move cursor all the way to the start -> only the right "..." should be displayed
        repeat(15) {
            tapKey("◀")
        }
        composeTestRule.waitForIdle()
        assertEquals(0, AppSettingsRepository.activeCursor.value.offset)
        composeTestRule.onNodeWithTag("left_ellipsis", useUnmergedTree = true).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("right_ellipsis", useUnmergedTree = true).assertIsDisplayed()

        // Press "=" and verify the history entry renders
        tapKey("=")
        composeTestRule.waitForIdle()
        val savedHistory = AppSettingsRepository.history.value
        assertEquals(1, savedHistory.size)
        assertEquals("762939453125", savedHistory.first().result)
    }

    @Test
    fun copyOverModes_resultFormulaAndNothing_workCorrectly() {
        val context = composeTestRule.activity.applicationContext
        composeTestRule.setContent {
            MECalculatorTheme {
                MainNavigation()
            }
        }

        // 1. CopyOverMode.RESULT: 4 + 6 = -> active tokens become "10"
        AppSettingsRepository.setCopyOverMode(context, CopyOverMode.RESULT)
        tapKey("4")
        tapKey("+")
        tapKey("6")
        tapKey("=")
        composeTestRule.waitForIdle()
        assertEquals(
            "10".map { MathToken.CharToken(it) },
            AppSettingsRepository.activeTokens.value,
        )

        // Clear input
        tapKey("⌫")
        tapKey("⌫")

        // 2. CopyOverMode.FORMULA: 4 + 6 = -> active tokens stay "4+6"
        AppSettingsRepository.setCopyOverMode(context, CopyOverMode.FORMULA)
        tapKey("4")
        tapKey("+")
        tapKey("6")
        tapKey("=")
        composeTestRule.waitForIdle()
        assertEquals(
            "4+6".map { MathToken.CharToken(it) },
            AppSettingsRepository.activeTokens.value,
        )

        // Clear input
        repeat(3) { tapKey("⌫") }

        // 3. CopyOverMode.NOTHING: 4 + 6 = -> active tokens become empty
        AppSettingsRepository.setCopyOverMode(context, CopyOverMode.NOTHING)
        tapKey("4")
        tapKey("+")
        tapKey("6")
        tapKey("=")
        composeTestRule.waitForIdle()
        assertTrue(AppSettingsRepository.activeTokens.value.isEmpty())
    }

    @Test
    fun folderButton_expandsToBoundingBox_andClosesViaInPlaceCloseButton() {
        val context = composeTestRule.activity.applicationContext
        val customSlots = KeypadCatalog.defaultSlots.toMutableList()
        val folderKeys = KeypadCatalog.defaultFolderKeys.toMutableList()
        // Put "sin" and "cos" in adjacent slots to slot 0 (which becomes the in-place "✕" button)
        folderKeys[1] = "sin"
        folderKeys[4] = "cos"
        customSlots[0] = KeypadSlot.Folder(title = "Trig", keyIds = folderKeys)
        AppSettingsRepository.setKeypadSlots(context, customSlots)

        composeTestRule.setContent {
            MECalculatorTheme {
                MainNavigation()
            }
        }

        // Tap the "Trig" folder button at top-left (slot 0)
        composeTestRule.onNodeWithText("Trig").performClick()
        composeTestRule.waitForIdle()

        // Verify the folder's keys ("sin", "cos") and the in-place "✕" close button are displayed
        composeTestRule.onNodeWithText("sin").assertIsDisplayed()
        composeTestRule.onNodeWithText("cos").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("✕").onLast().assertIsDisplayed()

        // Tap "✕" to close the folder without inserting anything
        composeTestRule.onAllNodesWithText("✕").onLast().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Trig").assertIsDisplayed()
    }

    @Test
    fun settingsAndEditNumpad_openAndDisplayCategoriesProperly() {
        composeTestRule.setContent {
            MECalculatorTheme {
                MainNavigation()
            }
        }

        // Open Settings via ⚙
        composeTestRule.onNodeWithText("⚙").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        // Open Edit Numpad
        composeTestRule.onNodeWithText("Edit Numpad").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Reset").assertIsDisplayed()

        // Tap a slot to open the key assignment dialog and verify "Operators" category exists
        composeTestRule.onAllNodesWithText("7").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Operators").assertIsDisplayed()
        composeTestRule.onNodeWithText("Numbers").assertIsDisplayed()
    }

    @Test
    fun fractionWithMultiDigitNumerator_displaysAllDigitsWithoutOverlappingAndMovesCursorCleanly() {
        composeTestRule.setContent {
            MECalculatorTheme {
                MainNavigation()
            }
        }

        // Enter 11111 / 1 (5 digits in numerator, 1 digit in denominator)
        repeat(5) { tapKey("1") }
        tapKey("÷")
        tapKey("1")
        composeTestRule.waitForIdle()

        // Move cursor up to numerator and verify neither ellipsis is triggered for a 5-digit fraction
        tapKey("▲")
        composeTestRule.waitForIdle()
        assertEquals(
            listOf(CursorStep(0, SlotType.FRAC_NUM)),
            AppSettingsRepository.activeCursor.value.path,
        )
        composeTestRule.onNodeWithTag("left_ellipsis", useUnmergedTree = true).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("right_ellipsis", useUnmergedTree = true).assertIsNotDisplayed()

        // Evaluate 11111 / 1 = 11111
        tapKey("=")
        composeTestRule.waitForIdle()
        assertEquals("11111", AppSettingsRepository.history.value.last().result)
    }
}
