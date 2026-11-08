package com.nnoidea.mecalculator.ui.main

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import com.nnoidea.mecalculator.data.NumberGrouping
import com.nnoidea.mecalculator.domain.ExactMathEngine
import com.nnoidea.mecalculator.domain.NumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenCursorOperationsTest {

    private fun chars(s: String): List<MathToken> = s.map { MathToken.CharToken(it) }

    @Test
    fun `up arrow escapes to history only when moveCursorInTokens returns moved false`() {
        var tokens: List<MathToken> = emptyList()
        var cursor = CursorState()

        // 1. Empty top-level input -> UP cannot move inside tokens (moved == false -> escape to history)
        val (_, movedEmpty) = moveCursorInTokens(tokens, cursor, "UP")
        assertFalse(movedEmpty)

        // 2. Create fraction 5 / 3 with cursor in denominator
        val s1 = insertTokensAt(tokens, cursor, chars("5"))
        val s2 = insertFractionAt(s1.first, s1.second)
        val s3 = insertTokensAt(s2.first, s2.second, chars("3"))
        tokens = s3.first
        cursor = s3.second
        assertEquals(listOf(CursorStep(0, SlotType.FRAC_DEN)), cursor.path)

        // First UP moves from denominator to numerator (moved == true -> stays in editor!)
        val (upToNum, movedFromDen) = moveCursorInTokens(tokens, cursor, "UP")
        assertTrue(movedFromDen)
        assertEquals(listOf(CursorStep(0, SlotType.FRAC_NUM)), upToNum.path)

        // Second UP from top-level numerator cannot move further up (moved == false -> escape to history!)
        val (_, movedFromNum) = moveCursorInTokens(tokens, upToNum, "UP")
        assertFalse(movedFromNum)
    }

    @Test
    fun `nested fraction under fraction and fraction over fraction cursor traversal`() {
        var tokens: List<MathToken> = emptyList()
        var cursor = CursorState()

        // Build 5 / (5 / 5)
        val a = insertTokensAt(tokens, cursor, chars("5"))
        val b = insertFractionAt(a.first, a.second)
        val c = insertTokensAt(b.first, b.second, chars("5"))
        val d = insertFractionAt(c.first, c.second)
        val e = insertTokensAt(d.first, d.second, chars("5"))
        tokens = e.first
        cursor = e.second

        assertEquals("5", ExactMathEngine.evaluateTokens(tokens))
        assertEquals(
            listOf(
                CursorStep(0, SlotType.FRAC_DEN),
                CursorStep(0, SlotType.FRAC_DEN),
            ),
            cursor.path,
        )

        // Move UP: inner denominator -> inner numerator (moved == true)
        val (c1, moved1) = moveCursorInTokens(tokens, cursor, "UP")
        assertTrue(moved1)
        assertEquals(
            listOf(
                CursorStep(0, SlotType.FRAC_DEN),
                CursorStep(0, SlotType.FRAC_NUM),
            ),
            c1.path,
        )

        // Move UP again: inner numerator (inside outer denominator) -> outer numerator (moved == true)
        val (c2, moved2) = moveCursorInTokens(tokens, c1, "UP")
        assertTrue(moved2)
        assertEquals(
            listOf(CursorStep(0, SlotType.FRAC_NUM)),
            c2.path,
        )

        // Move UP again: outer numerator -> cannot move up in tokens (moved == false -> escapes to history!)
        val (_, moved3) = moveCursorInTokens(tokens, c2, "UP")
        assertFalse(moved3)
    }

    @Test
    fun `nested power exponent cursor traversal with up down left right`() {
        // Build 2^(3^(4))
        var (tokens, cursor) = insertTokensAt(emptyList(), CursorState(), chars("2"))
        val p1 = insertPowerAt(tokens, cursor)
        val t1 = insertTokensAt(p1.first, p1.second, chars("3"))
        val p2 = insertPowerAt(t1.first, t1.second)
        val t2 = insertTokensAt(p2.first, p2.second, chars("4"))
        tokens = t2.first
        cursor = t2.second

        // 2^(3^4) = 2^81 = 2.4178516392293E+24
        assertEquals("2.4178516392293E+24", ExactMathEngine.evaluateTokens(tokens))

        // DOWN from inner exponent -> outer exponent -> top-level
        val (down1, moved1) = moveCursorInTokens(tokens, cursor, "DOWN")
        assertTrue(moved1)
        assertEquals(listOf(CursorStep(1, SlotType.POW_EXP)), down1.path)

        val (down2, moved2) = moveCursorInTokens(tokens, down1, "DOWN")
        assertTrue(moved2)
        assertTrue(down2.path.isEmpty())
        assertEquals(2, down2.offset)
    }

    @Test
    fun `backspace unwraps empty fraction denominator and restores numerator to parent list`() {
        // Type 125, press ÷ -> FractionToken(num="125", den="")
        val s1 = insertTokensAt(emptyList(), CursorState(), chars("125"))
        val s2 = insertFractionAt(s1.first, s1.second)
        assertEquals(1, s2.first.size)
        assertTrue(s2.first[0] is MathToken.FractionToken)

        // Press Backspace inside empty denominator -> unwraps back to "125" at top level!
        val unfragged = backspaceAt(s2.first, s2.second)
        assertEquals(chars("125"), unfragged.first)
        assertTrue(unfragged.second.path.isEmpty())
        assertEquals(3, unfragged.second.offset)
    }

    @Test
    fun `backspace unwraps empty power token`() {
        val s1 = insertTokensAt(emptyList(), CursorState(), chars("5"))
        val s2 = insertPowerAt(s1.first, s1.second)
        assertEquals(2, s2.first.size)

        val unwrapped = backspaceAt(s2.first, s2.second)
        assertEquals(chars("5"), unwrapped.first)
        assertTrue(unwrapped.second.path.isEmpty())
        assertEquals(1, unwrapped.second.offset)
    }

    @Test
    fun `long expression cursor traversal across 100 tokens never goes out of bounds`() {
        val longExpr = "5·5·5·5·5·5·55·5·5·5·5·5·5·5·5·5·5·5·5·5·5·5·5·5·5"
        val (tokens, endCursor) = insertTokensAt(emptyList(), CursorState(), chars(longExpr))
        assertEquals(longExpr.length, tokens.size)
        assertEquals(longExpr.length, endCursor.offset)

        // Walk all the way left to 0
        var c = endCursor
        repeat(longExpr.length + 5) {
            c = moveCursorInTokens(tokens, c, "LEFT").first
        }
        assertEquals(0, c.offset)

        // Walk all the way right back to end
        repeat(longExpr.length + 5) {
            c = moveCursorInTokens(tokens, c, "RIGHT").first
        }
        assertEquals(longExpr.length, c.offset)
    }

    @Test
    fun `computeWindowedItemMaxWidth never collapses leaf tokens or cursor bar under narrow constraints`() {
        // Regression test for fraction numerator/denominator digit overlapping and invisible cursor:
        // Leaf items (CharToken, FuncToken, sep, CursorBar) must ALWAYS receive Constraints.Infinity
        // even when maxW is smaller than leftEllWidth + rightEllWidth.
        val leafMaxW = computeWindowedItemMaxWidth(
            isComposite = false,
            hasBounded = true,
            maxW = 60,
            totalRowIntrinsicWidth = 60,
            showLeft = true,
            showRight = true,
            leftEllWidth = 45,
            rightEllWidth = 45,
        )
        assertEquals(Constraints.Infinity, leafMaxW)

        // Composite tokens (FractionToken, PowerToken) get full maxW when the row fits without ellipses
        val compositeFittingMaxW = computeWindowedItemMaxWidth(
            isComposite = true,
            hasBounded = true,
            maxW = 500,
            totalRowIntrinsicWidth = 240,
            showLeft = true,
            showRight = true,
            leftEllWidth = 45,
            rightEllWidth = 45,
        )
        assertEquals(500, compositeFittingMaxW)

        // Composite tokens in an overflowing row are bounded by maxW minus active ellipses
        val compositeOverflowingMaxW = computeWindowedItemMaxWidth(
            isComposite = true,
            hasBounded = true,
            maxW = 500,
            totalRowIntrinsicWidth = 900,
            showLeft = true,
            showRight = false,
            leftEllWidth = 45,
            rightEllWidth = 45,
        )
        assertEquals(455, compositeOverflowingMaxW)
    }

    @Test
    fun `clampViewportOffset locks vertical pan on single line calculations and bounds scroll to edges`() {
        val viewportSize = IntSize(width = 400, height = 100)
        val padX = 12f
        val padY = 6f
        // Usable view: W = 400 - 24 = 376, H = 100 - 12 = 88

        // 1. Single-line calculation (1+1+1): content fits vertically and horizontally
        val singleLineSize = IntSize(width = 150, height = 40)
        val clampedSingleLine = clampViewportOffset(
            pan = Offset(x = 50f, y = 200f),
            scale = 1f,
            contentSize = singleLineSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )
        // Horizontal fits -> locked to padX
        assertEquals(padX, clampedSingleLine.x, 0.001f)
        // Vertical fits -> locked to centered position: padY + (88 - 40) / 2 = 6 + 24 = 30f
        assertEquals(30f, clampedSingleLine.y, 0.001f)

        // 2. Long horizontal calculation (W=800, exceeds usable W=376)
        val longCalcSize = IntSize(width = 800, height = 40)
        // Scroll too far right (towards start) -> clamped to padX
        val clampLeft = clampViewportOffset(
            pan = Offset(x = 100f, y = 0f),
            scale = 1f,
            contentSize = longCalcSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )
        assertEquals(padX, clampLeft.x, 0.001f)

        // Scroll too far left (past end of expression) -> clamped to padX - (800 - 376) = 12 - 424 = -412f
        val clampRight = clampViewportOffset(
            pan = Offset(x = -999f, y = 0f),
            scale = 1f,
            contentSize = longCalcSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )
        assertEquals(-412f, clampRight.x, 0.001f)

        // 3. Tall nested fraction (H=300, exceeds usable H=88)
        val tallFracSize = IntSize(width = 200, height = 300)
        // Scroll too far down (past top numerator) -> clamped to padY
        val clampTop = clampViewportOffset(
            pan = Offset(x = padX, y = 150f),
            scale = 1f,
            contentSize = tallFracSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )
        assertEquals(padY, clampTop.y, 0.001f)

        // Scroll too far up (past bottom denominator) -> clamped to padY - (300 - 88) = 6 - 212 = -206f
        val clampBottom = clampViewportOffset(
            pan = Offset(x = padX, y = -500f),
            scale = 1f,
            contentSize = tallFracSize,
            viewportSize = viewportSize,
            padX = padX,
            padY = padY,
        )
        assertEquals(-206f, clampBottom.y, 0.001f)
    }

    @Test
    fun `computeZoomBounds only allows zooming out until calculation fits in viewport`() {
        val viewportSize = IntSize(width = 400, height = 200)
        val padX = 12f
        val padY = 6f
        // Usable view: W = 376, H = 188

        // 1. Calculation already fits within viewport at 1.0x -> minScale is 1.0x (cannot zoom out into void)
        val fitsSize = IntSize(width = 250, height = 100)
        val (minScaleFits, maxScaleFits) = computeZoomBounds(fitsSize, viewportSize, padX, padY)
        assertEquals(1.0f, minScaleFits, 0.001f)
        assertEquals(2.5f, maxScaleFits, 0.001f)

        // 2. Large calculation (width = 752, 2x wider than usable 376)
        val wideSize = IntSize(width = 752, height = 50)
        val (minScaleWide, _) = computeZoomBounds(wideSize, viewportSize, padX, padY)
        // 376 / 752 = 0.5f -> allows zooming out to exactly 50% so the whole expression fits!
        assertEquals(0.5f, minScaleWide, 0.001f)
    }

    @Test
    fun `as calculation shrinks by deleting characters, minScale increases and scale zooms back in`() {
        val viewportSize = IntSize(width = 400, height = 200)
        val padX = 12f
        val padY = 6f
        // Usable view: W = 376, H = 188

        // 1. Initially huge calculation (1504px wide -> 4x wider than viewport)
        var currentContentSize = IntSize(width = 1504, height = 50)
        val (initialMinScale, _) = computeZoomBounds(currentContentSize, viewportSize, padX, padY)
        assertEquals(0.25f, initialMinScale, 0.001f)

        // User zoomed out to maximum allowed (0.25f)
        var scale = initialMinScale

        // 2. User deletes characters one by one, reducing content width to 752px
        currentContentSize = IntSize(width = 752, height = 50)
        val boundsAfterDelete1 = computeZoomBounds(currentContentSize, viewportSize, padX, padY)
        assertEquals(0.5f, boundsAfterDelete1.first, 0.001f)

        // Scale dynamically clamps/zooms in to the new minScale!
        scale = scale.coerceIn(boundsAfterDelete1.first, boundsAfterDelete1.second)
        assertEquals(0.5f, scale, 0.001f)

        // 3. User deletes more characters down to 376px (fits exactly)
        currentContentSize = IntSize(width = 376, height = 50)
        val boundsAfterDelete2 = computeZoomBounds(currentContentSize, viewportSize, padX, padY)
        assertEquals(1.0f, boundsAfterDelete2.first, 0.001f)

        scale = scale.coerceIn(boundsAfterDelete2.first, boundsAfterDelete2.second)
        assertEquals(1.0f, scale, 0.001f)

        // 4. Expression is evaluated (equal pressed) with CopyOverMode.RESULT -> becomes short result (e.g. 80px)
        val shortResultSize = IntSize(width = 80, height = 50)
        val boundsResult = computeZoomBounds(shortResultSize, viewportSize, padX, padY)
        assertEquals(1.0f, boundsResult.first, 0.001f)
        val resultScale = scale.coerceIn(boundsResult.first, boundsResult.second)
        assertEquals(1.0f, resultScale, 0.001f)

        // 5. If CopyOverMode is FORMULA, expression remains unchanged -> preserves user's zoom
        val formulaContentSize = IntSize(width = 752, height = 50)
        val boundsFormula = computeZoomBounds(formulaContentSize, viewportSize, padX, padY)
        assertEquals(0.5f, boundsFormula.first, 0.001f)
        val formulaScale = 0.65f // user was zoomed at 0.65x
        val preservedScale = formulaScale.coerceIn(boundsFormula.first, boundsFormula.second)
        assertEquals(0.65f, preservedScale, 0.001f)
    }

    @Test
    fun `history viewport bounds and clamping maintain consistent start alignment and max zoom out`() {
        val historyViewportSize = IntSize(width = 360, height = 110)
        val padX = 10f
        val padY = 6f
        // Usable view: W = 340, H = 98

        // 1. Long horizontal equation in history (width = 850px)
        val longHistoryFormula = IntSize(width = 850, height = 40)
        val (minScaleLong, _) = computeZoomBounds(longHistoryFormula, historyViewportSize, padX, padY)
        // 340 / 850 = 0.40f -> allows zooming out to exactly 40% so the full equation fits in the history card!
        assertEquals(0.40f, minScaleLong, 0.001f)

        // At default 1.0x zoom, clamping keeps equation aligned to the first digit (padX)
        val initialPan = clampViewportOffset(Offset(padX, padY), 1.0f, longHistoryFormula, historyViewportSize, padX, padY)
        assertEquals(padX, initialPan.x, 0.001f)

        // Cannot scroll past the right edge: minPanX = padX - (850 - 340) = 10 - 510 = -500f
        val clampedFarRight = clampViewportOffset(Offset(-999f, padY), 1.0f, longHistoryFormula, historyViewportSize, padX, padY)
        assertEquals(-500f, clampedFarRight.x, 0.001f)

        // 2. Tall nested fraction in history (height = 300px > 110px max height)
        val tallHistoryFraction = IntSize(width = 180, height = 300)
        val (minScaleTall, _) = computeZoomBounds(tallHistoryFraction, historyViewportSize, padX, padY)
        // 98 / 300 = 0.326f -> allows zooming out to fit all fraction levels in the 110dp window!
        assertEquals(0.326f, minScaleTall, 0.005f)

        // Cannot scroll past the bottom denominator: minPanY = padY - (300 - 98) = 6 - 202 = -196f
        val clampedDeepDenom = clampViewportOffset(Offset(padX, -999f), 1.0f, tallHistoryFraction, historyViewportSize, padX, padY)
        assertEquals(-196f, clampedDeepDenom.y, 0.001f)
    }

    @Test
    fun `history result viewport bounds only zoom out for long result numbers`() {
        val resultPillSize = IntSize(width = 120, height = 36)
        val padX = 12f
        val padY = 6f
        // Usable view: W = 96, H = 24

        // 1. Short result (width = 50px <= 96px) -> cannot zoom out into void (minScale = 1.0f)
        val shortResult = IntSize(width = 50, height = 20)
        val (minScaleShort, _) = computeZoomBounds(shortResult, resultPillSize, padX, padY)
        assertEquals(1.0f, minScaleShort, 0.001f)

        // 2. Huge 40-digit result (width = 384px) -> minScale allows zooming out to 25% (96 / 384 = 0.25f)
        val hugeResult = IntSize(width = 384, height = 20)
        val (minScaleHuge, _) = computeZoomBounds(hugeResult, resultPillSize, padX, padY)
        assertEquals(0.25f, minScaleHuge, 0.001f)
    }

    @Test
    fun `calculateTokensNaturalSize accurately measures single-line and 2D fraction expressions`() {
        val testDensity = Density(density = 2.5f)
        val charW = 20f
        val charH = 30f

        // 1. Single line: 5 · 5 (3 tokens)
        val singleLineTokens = listOf(
            MathToken.CharToken('5'),
            MathToken.CharToken('·'),
            MathToken.CharToken('5'),
        )
        val singleLineSize = calculateTokensNaturalSize(
            tokens = singleLineTokens,
            numberGrouping = NumberGrouping.NONE,
            charWidthPx = charW,
            charHeightPx = charH,
            density = testDensity,
        )
        // 3 characters + 3*1dp padding = 3*20 + 3*2.5 = 60 + 7.5 = 67.5 -> 68px
        assertTrue(singleLineSize.width in 67..70)
        assertEquals(charH.toInt(), singleLineSize.height)

        // 2. Fraction token: 5 / 55
        val fracToken = MathToken.FractionToken(
            num = listOf(MathToken.CharToken('5')),
            den = listOf(MathToken.CharToken('5'), MathToken.CharToken('5')),
        )
        val fracSize = calculateTokensNaturalSize(
            tokens = listOf(fracToken),
            numberGrouping = NumberGrouping.NONE,
            charWidthPx = charW,
            charHeightPx = charH,
            density = testDensity,
        )
        // Height must include both num + den + fraction bar + padding
        assertTrue(fracSize.height > charH.toInt() * 1.5)
    }

    @Test
    fun `findFirstDigitCenterY accurately locates first digit for single line and fraction expressions`() {
        val testDensity = Density(density = 2.5f)
        val charH = 30f

        // 1. Single line: 5 + 5 + 5 -> first digit is in the middle of line height
        val singleLineTokens = listOf(
            MathToken.CharToken('5'),
            MathToken.CharToken('+'),
            MathToken.CharToken('5'),
        )
        val singleLineFirstDigitY = findFirstDigitCenterY(
            tokens = singleLineTokens,
            totalContentHeight = charH,
            charHeightPx = charH,
            density = testDensity,
        )
        assertEquals(15f, singleLineFirstDigitY, 0.001f)

        // 2. Fraction at start: (5+5)/2 -> first digit is in the numerator at the top
        val fracToken = MathToken.FractionToken(
            num = listOf(MathToken.CharToken('5'), MathToken.CharToken('+'), MathToken.CharToken('5')),
            den = listOf(MathToken.CharToken('2')),
        )
        val fracFirstDigitY = findFirstDigitCenterY(
            tokens = listOf(fracToken),
            totalContentHeight = 80f,
            charHeightPx = charH,
            density = testDensity,
        )
        // First digit is in the numerator, so its Y coordinate is well in the top half (< 40f)
        assertTrue(fracFirstDigitY < 40f)
    }

    @Test
    fun `history formula default view centers camera vertically on first digit`() {
        val viewportSize = IntSize(width = 360, height = 110)
        val padX = 10f
        val padY = 6f
        val viewH = (viewportSize.height.toFloat() - 2 * padY).coerceAtLeast(0f) // 98f

        // 1. Expression with first digit centered at y = 20f in 80px content
        val firstDigitY = 20f
        val currentScale = 1.0f
        // Centering on first digit: screenY = panY + firstDigitY = viewportHeight / 2 (55f)
        // targetPanY = 55f - 20f = 35f
        val targetPanY = (viewportSize.height / 2f) - (firstDigitY * currentScale)
        assertEquals(35f, targetPanY, 0.001f)
        assertEquals(55f, targetPanY + firstDigitY, 0.001f)
    }

    @Test
    fun `history row fits calculations inline based on combined width without clipping`() {
        val availableWidthPx = 1000
        val spacingPx = 25

        // 1. Short single line calculation: 5 · 5 = 25 (fits easily)
        val shortFormulaW = 120
        val shortResultW = 90
        val canFitShort = (shortFormulaW + shortResultW + spacingPx <= availableWidthPx)
        assertTrue("Short calculation 5 · 5 = 25 must fit inline on one row", canFitShort)

        // 2. Expression with fraction: (5+5+5+5/2)+5 = 22.5 (width fits alongside result)
        val fracFormulaW = 400
        val fracResultW = 110
        val canFitFrac = (fracFormulaW + fracResultW + spacingPx <= availableWidthPx)
        assertTrue("Fraction calculation with narrow width must fit inline on the same line", canFitFrac)

        // 3. User screenshot case: (5+5+5+ 5/2) + 5 = 22.5 natural size verification
        val userTokens = listOf(
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
        )
        val userTokensNaturalSize = calculateTokensNaturalSize(
            tokens = userTokens,
            numberGrouping = NumberGrouping.NONE,
            charWidthPx = 20f,
            charHeightPx = 30f,
            density = Density(2.5f),
        )
        val userFormulaTotalW = userTokensNaturalSize.width + 50 + 4
        val userResultTotalW = 160
        assertTrue(
            "User calculation (5+5+5+ 5/2)+5 (w=$userFormulaTotalW) + result (w=$userResultTotalW) must easily fit inline within available width ($availableWidthPx)",
            (userFormulaTotalW + userResultTotalW + spacingPx) <= availableWidthPx,
        )

        // 4. Long calculation exceeding screen width -> stacks to new line
        val longFormulaW = 920
        val longResultW = 120
        val canFitLong = (longFormulaW + longResultW + spacingPx <= availableWidthPx)
        assertFalse("Long calculation exceeding width must wrap result to a new stacked line", canFitLong)
    }

    @Test
    fun `computeNextHistorySelection scrolls through formula and result when stacked and skips formula when inline`() {
        // Setup: History has 3 items:
        // Item 0: INLINE
        // Item 1: STACKED (formula on top, result underneath)
        // Item 2: INLINE
        val isStacked: (Int) -> Boolean = { idx -> idx == 1 }
        val size = 3

        // 1. From Item 2 (inline) on RESULT, press UP -> moves to Item 1 (stacked) on RESULT
        val r1 = computeNextHistorySelection("UP", currentIndex = 2, currentSide = HistorySide.RESULT, historySize = size, isEntryStacked = isStacked)
        assertEquals(HistoryNavResult.StayInHistory(1, HistorySide.RESULT), r1)

        // 2. From Item 1 (stacked) on RESULT, press UP -> moves to Item 1 on FORMULA (scrolls through formula!)
        val r2 = computeNextHistorySelection("UP", currentIndex = 1, currentSide = HistorySide.RESULT, historySize = size, isEntryStacked = isStacked)
        assertEquals(HistoryNavResult.StayInHistory(1, HistorySide.FORMULA), r2)

        // 3. From Item 1 (stacked) on FORMULA, press UP -> moves to Item 0 (inline)
        val r3 = computeNextHistorySelection("UP", currentIndex = 1, currentSide = HistorySide.FORMULA, historySize = size, isEntryStacked = isStacked)
        assertEquals(HistoryNavResult.StayInHistory(0, HistorySide.FORMULA), r3)

        // 4. Moving down from Item 0 (inline) on RESULT:
        // Item 0 (inline) -> Item 1 (stacked) hits FORMULA at the top
        val rDown1 = computeNextHistorySelection("DOWN", currentIndex = 0, currentSide = HistorySide.RESULT, historySize = size, isEntryStacked = isStacked)
        assertEquals(HistoryNavResult.StayInHistory(1, HistorySide.FORMULA), rDown1)

        // 5. From Item 1 (stacked) on FORMULA, press DOWN -> moves to Item 1 on RESULT (scrolls through result!)
        val rDown2 = computeNextHistorySelection("DOWN", currentIndex = 1, currentSide = HistorySide.FORMULA, historySize = size, isEntryStacked = isStacked)
        assertEquals(HistoryNavResult.StayInHistory(1, HistorySide.RESULT), rDown2)

        // 6. From Item 1 (stacked) on RESULT, press DOWN -> moves to Item 2 (inline) on RESULT
        val rDown3 = computeNextHistorySelection("DOWN", currentIndex = 1, currentSide = HistorySide.RESULT, historySize = size, isEntryStacked = isStacked)
        assertEquals(HistoryNavResult.StayInHistory(2, HistorySide.RESULT), rDown3)

        // 7. From bottom-most item (Item 2), press DOWN -> exits to editor
        val rExit = computeNextHistorySelection("DOWN", currentIndex = 2, currentSide = HistorySide.RESULT, historySize = size, isEntryStacked = isStacked)
        assertEquals(HistoryNavResult.ExitToEditor, rExit)

        // 8. When items are both inline (e.g. all inline): results under each other skip expression
        val allInline: (Int) -> Boolean = { false }
        val rSkip = computeNextHistorySelection("UP", currentIndex = 2, currentSide = HistorySide.RESULT, historySize = size, isEntryStacked = allInline)
        assertEquals(HistoryNavResult.StayInHistory(1, HistorySide.RESULT), rSkip)

        // 9. LEFT and RIGHT toggle side
        val rLeft = computeNextHistorySelection("LEFT", currentIndex = 1, currentSide = HistorySide.RESULT, historySize = size, isEntryStacked = isStacked)
        assertEquals(HistoryNavResult.StayInHistory(1, HistorySide.FORMULA), rLeft)

        val rRight = computeNextHistorySelection("RIGHT", currentIndex = 1, currentSide = HistorySide.FORMULA, historySize = size, isEntryStacked = isStacked)
        assertEquals(HistoryNavResult.StayInHistory(1, HistorySide.RESULT), rRight)
    }

    @Test
    fun `computeAutoFrameDelta frames cursor horizontally and vertically within bounds`() {
        val viewW = 400f
        val viewH = 150f
        val padX = 12f
        val rightMargin = 24f
        val padY = 6f
        val bottomMargin = 12f

        // 1. Cursor completely inside margins -> zero delta
        val inside = computeAutoFrameDelta(
            cursorX = 100f,
            cursorY = 50f,
            cursorW = 3f,
            cursorH = 20f,
            viewW = viewW,
            viewH = viewH,
            padX = padX,
            rightMargin = rightMargin,
            padY = padY,
            bottomMargin = bottomMargin,
        )
        assertEquals(0f, inside.deltaX, 0.001f)
        assertEquals(0f, inside.deltaY, 0.001f)

        // 2. Cursor overflows right edge (long calculation) -> negative delta shifts left
        val overflowRight = computeAutoFrameDelta(
            cursorX = 500f,
            cursorY = 50f,
            cursorW = 4f,
            cursorH = 20f,
            viewW = viewW,
            viewH = viewH,
            padX = padX,
            rightMargin = rightMargin,
            padY = padY,
            bottomMargin = bottomMargin,
        )
        // (400 - 24) - (500 + 4) = 376 - 504 = -128f
        assertEquals(-128f, overflowRight.deltaX, 0.001f)
        assertEquals(0f, overflowRight.deltaY, 0.001f)

        // 3. Cursor clipped above viewport (tall fraction numerator) -> positive deltaY shifts down
        val overflowTop = computeAutoFrameDelta(
            cursorX = 100f,
            cursorY = -20f,
            cursorW = 3f,
            cursorH = 20f,
            viewW = viewW,
            viewH = viewH,
            padX = padX,
            rightMargin = rightMargin,
            padY = padY,
            bottomMargin = bottomMargin,
        )
        // padY - (-20) = 6 - (-20) = 26f
        assertEquals(26f, overflowTop.deltaY, 0.001f)

        // 4. Cursor clipped below viewport (tall fraction denominator) -> negative deltaY shifts up
        val overflowBottom = computeAutoFrameDelta(
            cursorX = 100f,
            cursorY = 160f,
            cursorW = 3f,
            cursorH = 20f,
            viewW = viewW,
            viewH = viewH,
            padX = padX,
            rightMargin = rightMargin,
            padY = padY,
            bottomMargin = bottomMargin,
        )
        // (viewH - bottomMargin) - (cursorY + cursorH) = (150 - 12) - 180 = 138 - 180 = -42f
        assertEquals(-42f, overflowBottom.deltaY, 0.001f)
    }

    @Test
    fun `10 levels deep nested fractions evaluate accurately and allow full cursor traversal`() {
        var expr: List<MathToken> = listOf(MathToken.CharToken('1'))
        for (i in 0 until 9) {
            expr = listOf(
                MathToken.FractionToken(
                    num = listOf(MathToken.CharToken('2')),
                    den = expr,
                ),
            )
        }
        assertEquals(1, expr.size)
        assertTrue(expr[0] is MathToken.FractionToken)

        // Evaluate with ExactMathEngine (EvalEx arbitrary-precision BigDecimal)
        val result = ExactMathEngine.evaluateTokens(expr)
        assertTrue(!result.isNullOrEmpty())

        // Start cursor at the deepest slot
        var path = emptyList<CursorStep>()
        var cur: List<MathToken> = expr
        while (cur.isNotEmpty() && cur[0] is MathToken.FractionToken) {
            path = path + CursorStep(0, SlotType.FRAC_DEN)
            cur = (cur[0] as MathToken.FractionToken).den
        }
        var cursor = CursorState(path = path, offset = 0)

        // Traverse UP through all 9 levels
        var upCount = 0
        while (true) {
            val (nextCursor, moved) = moveCursorInTokens(expr, cursor, "UP")
            if (!moved) break
            cursor = nextCursor
            upCount++
        }
        assertTrue("Expected to traverse multiple fraction levels upward, traversed $upCount", upCount >= 9)
    }

    @Test
    fun `insertSqrtAt and cursor navigation under radical wing`() {
        var tokens = emptyList<MathToken>()
        var cursor = CursorState()

        // Insert square root
        val (t1, c1) = insertSqrtAt(tokens, cursor)
        tokens = t1
        cursor = c1
        assertEquals(1, tokens.size)
        assertTrue(tokens[0] is MathToken.SqrtToken)
        assertEquals(listOf(CursorStep(0, SlotType.SQRT_RAD)), cursor.path)
        assertEquals(0, cursor.offset)

        // Type '8' and '1' inside the radicand under the wing
        val (t2, c2) = insertTokensAt(tokens, cursor, listOf(MathToken.CharToken('8'), MathToken.CharToken('1')))
        tokens = t2
        cursor = c2
        val sqrt = tokens[0] as MathToken.SqrtToken
        assertEquals(2, sqrt.rad.size)
        assertEquals(2, cursor.offset)

        // Move cursor RIGHT out from under the wing to the main row
        val (c3, movedRight) = moveCursorInTokens(tokens, cursor, "RIGHT")
        assertTrue(movedRight)
        assertEquals(emptyList<CursorStep>(), c3.path)
        assertEquals(1, c3.offset)

        // Move cursor LEFT back under the wing
        val (c4, movedLeft) = moveCursorInTokens(tokens, c3, "LEFT")
        assertTrue(movedLeft)
        assertEquals(listOf(CursorStep(0, SlotType.SQRT_RAD)), c4.path)
        assertEquals(2, c4.offset)

        // Backspace inside empty square root deletes it
        val emptySqrt = listOf<MathToken>(MathToken.SqrtToken(rad = emptyList()))
        val emptySqrtCursor = CursorState(path = listOf(CursorStep(0, SlotType.SQRT_RAD)), offset = 0)
        val (tDeleted, cDeleted) = backspaceAt(emptySqrt, emptySqrtCursor)
        assertTrue(tDeleted.isEmpty())
        assertEquals(CursorState(path = emptyList(), offset = 0), cDeleted)
    }

    @Test
    fun `empty input field operation result copy over prepends latest result tokens`() {
        val lastResult = "42"
        val prependTokens = NumberFormatter.resultStringToTokens(lastResult)
        assertEquals(2, prependTokens.size)

        // 1. Binary operator (+) prepends result and appends operator
        val baseCursor = CursorState(path = emptyList(), offset = prependTokens.size)
        val opTokens = NumberFormatter.resultStringToTokens("+")
        val (tokensWithPlus, cursorWithPlus) = insertTokensAt(prependTokens, baseCursor, opTokens)

        assertEquals(3, tokensWithPlus.size)
        assertEquals(MathToken.CharToken('4'), tokensWithPlus[0])
        assertEquals(MathToken.CharToken('2'), tokensWithPlus[1])
        assertEquals(MathToken.CharToken('+'), tokensWithPlus[2])
        assertEquals(3, cursorWithPlus.offset)
        assertTrue(cursorWithPlus.path.isEmpty())

        // 2. Fraction (÷) wraps latest result into numerator with cursor in denominator
        val (fracTokens, fracCursor) = insertFractionAt(prependTokens, baseCursor)
        assertEquals(1, fracTokens.size)
        val frac = fracTokens[0] as MathToken.FractionToken
        assertEquals(prependTokens, frac.num)
        assertTrue(frac.den.isEmpty())
        assertEquals(listOf(CursorStep(0, SlotType.FRAC_DEN)), fracCursor.path)
        assertEquals(0, fracCursor.offset)

        // 3. Power (xʸ) wraps latest result into base with cursor in exponent
        val (powTokens, powCursor) = insertPowerAt(prependTokens, baseCursor)
        assertEquals(3, powTokens.size)
        assertEquals(prependTokens, powTokens.take(2))
        val pow = powTokens[2] as MathToken.PowerToken
        assertTrue(pow.exp.isEmpty())
        assertEquals(listOf(CursorStep(2, SlotType.POW_EXP)), powCursor.path)
        assertEquals(0, powCursor.offset)
    }

    @Test
    fun `empty input field copy over handles scientific notation results accurately`() {
        val scientificResult = "3.14 · 10²"
        val resultTokens = NumberFormatter.resultStringToTokens(scientificResult)
        assertTrue(resultTokens.any { tok -> tok is MathToken.PowerToken })

        val baseCursor = CursorState(path = emptyList(), offset = resultTokens.size)
        val opTokens = NumberFormatter.resultStringToTokens("−")
        val (tokensWithMinus, cursorWithMinus) = insertTokensAt(resultTokens, baseCursor, opTokens)

        assertEquals(resultTokens.size + 1, tokensWithMinus.size)
        assertEquals(MathToken.CharToken('−'), tokensWithMinus.last())
        assertEquals(resultTokens.size + 1, cursorWithMinus.offset)
    }

    @Test
    fun `holding delete with tokens clears tokens and does NOT clear history, even on repeated callbacks in same press`() {
        var currentTokens = listOf<MathToken>(MathToken.CharToken('5'), MathToken.CharToken('0'))
        val currentHistory = mutableListOf("100", "200")

        var tokensClearedCount = 0
        var historyClearedCount = 0

        val handler = DeleteKeypressHandler(
            getTokens = { currentTokens },
            onClearTokens = {
                tokensClearedCount++
                currentTokens = emptyList()
            },
            onClearHistory = {
                historyClearedCount++
                currentHistory.clear()
            },
        )

        // User touches down
        handler.onPressDown()
        assertTrue(handler.gestureStartedWithTokens)
        assertFalse(handler.actionConsumedInGesture)

        // 1st long-press trigger during this press: clears tokens ONLY
        val firstAction = handler.onLongPress()
        assertEquals(DeleteActionTaken.CLEARED_TOKENS, firstAction)
        assertEquals(1, tokensClearedCount)
        assertEquals(0, historyClearedCount)
        assertTrue(currentTokens.isEmpty())
        assertEquals(2, currentHistory.size)
        assertTrue(handler.actionConsumedInGesture)

        // Finger continues to be held down - gesture detector or recomposition triggers again:
        val secondAction = handler.onLongPress()
        assertEquals(DeleteActionTaken.NONE, secondAction)
        assertEquals(1, tokensClearedCount)
        assertEquals(0, historyClearedCount)
        assertEquals(2, currentHistory.size)

        val thirdAction = handler.onLongPress()
        assertEquals(DeleteActionTaken.NONE, thirdAction)
        assertEquals(0, historyClearedCount)
        assertEquals(2, currentHistory.size)

        // User finally releases finger
        handler.onPressUp()
        assertFalse(handler.actionConsumedInGesture)
        assertFalse(handler.gestureStartedWithTokens)
    }

    @Test
    fun `holding delete on already empty input field clears history`() {
        var currentTokens = emptyList<MathToken>()
        val currentHistory = mutableListOf("100", "200")

        var tokensClearedCount = 0
        var historyClearedCount = 0

        val handler = DeleteKeypressHandler(
            getTokens = { currentTokens },
            onClearTokens = {
                tokensClearedCount++
                currentTokens = emptyList()
            },
            onClearHistory = {
                historyClearedCount++
                currentHistory.clear()
            },
        )

        // User touches down on already empty input field
        handler.onPressDown()
        assertFalse(handler.gestureStartedWithTokens)
        assertFalse(handler.actionConsumedInGesture)

        // Long-press fires: clears history!
        val action = handler.onLongPress()
        assertEquals(DeleteActionTaken.CLEARED_HISTORY, action)
        assertEquals(0, tokensClearedCount)
        assertEquals(1, historyClearedCount)
        assertTrue(currentHistory.isEmpty())
        assertTrue(handler.actionConsumedInGesture)

        // Finger kept held down
        val repeatAction = handler.onLongPress()
        assertEquals(DeleteActionTaken.NONE, repeatAction)
        assertEquals(1, historyClearedCount)

        handler.onPressUp()
        assertFalse(handler.actionConsumedInGesture)
    }

    @Test
    fun `sequential two-stage clearing requires releasing and pressing down again`() {
        var currentTokens = listOf<MathToken>(MathToken.CharToken('9'))
        val currentHistory = mutableListOf("42")

        val handler = DeleteKeypressHandler(
            getTokens = { currentTokens },
            onClearTokens = { currentTokens = emptyList() },
            onClearHistory = { currentHistory.clear() },
        )

        // Step 1: Hold to clear input
        handler.onPressDown()
        val step1 = handler.onLongPress()
        assertEquals(DeleteActionTaken.CLEARED_TOKENS, step1)
        assertTrue(currentTokens.isEmpty())
        assertEquals(1, currentHistory.size) // History MUST still be intact!

        // Release finger
        handler.onPressUp()

        // Step 2: Now press and hold again on empty input
        handler.onPressDown()
        val step2 = handler.onLongPress()
        assertEquals(DeleteActionTaken.CLEARED_HISTORY, step2)
        assertTrue(currentHistory.isEmpty()) // History is now cleared!

        handler.onPressUp()
    }

    @Test
    fun `backspace on root tokens with cursor at offset 0 deletes last token and sets cursor to lastIdx`() {
        val tokens = listOf<MathToken>(
            MathToken.CharToken('1'),
            MathToken.CharToken('2'),
            MathToken.CharToken('·'),
            MathToken.CharToken('5'),
        )
        val cursorAtZero = CursorState(path = emptyList(), offset = 0)

        val (newTokens, newCursor) = backspaceAt(tokens, cursorAtZero)
        assertEquals(3, newTokens.size)
        assertEquals(listOf(MathToken.CharToken('1'), MathToken.CharToken('2'), MathToken.CharToken('·')), newTokens)
        assertEquals(CursorState(path = emptyList(), offset = 3), newCursor)
    }

    @Test
    fun `tapping backspace repeatedly on non-empty tokens deletes all tokens cleanly down to empty`() {
        var currentTokens = listOf<MathToken>(
            MathToken.CharToken('1'),
            MathToken.CharToken('2'),
            MathToken.CharToken('·'),
            MathToken.CharToken('5'),
        )
        var currentCursor = CursorState(path = emptyList(), offset = 4)

        // Delete '5'
        val s1 = backspaceAt(currentTokens, currentCursor)
        currentTokens = s1.first
        currentCursor = s1.second
        assertEquals(3, currentTokens.size)
        assertEquals(listOf(MathToken.CharToken('1'), MathToken.CharToken('2'), MathToken.CharToken('·')), currentTokens)
        assertEquals(3, currentCursor.offset)

        // Delete '·'
        val s2 = backspaceAt(currentTokens, currentCursor)
        currentTokens = s2.first
        currentCursor = s2.second
        assertEquals(2, currentTokens.size)
        assertEquals(listOf(MathToken.CharToken('1'), MathToken.CharToken('2')), currentTokens)
        assertEquals(2, currentCursor.offset)

        // Delete '2'
        val s3 = backspaceAt(currentTokens, currentCursor)
        currentTokens = s3.first
        currentCursor = s3.second
        assertEquals(1, currentTokens.size)
        assertEquals(listOf(MathToken.CharToken('1')), currentTokens)
        assertEquals(1, currentCursor.offset)

        // Delete '1'
        val s4 = backspaceAt(currentTokens, currentCursor)
        currentTokens = s4.first
        currentCursor = s4.second
        assertTrue(currentTokens.isEmpty())
        assertEquals(0, currentCursor.offset)
    }
}
