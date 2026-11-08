package com.nnoidea.mecalculator.ui.main

data class HistoryEntry(
    val tokens: List<MathToken>,
    val result: String,
)

enum class HistorySide {
    FORMULA,
    RESULT,
}

internal sealed interface HistoryNavResult {
    data class StayInHistory(val index: Int, val side: HistorySide) : HistoryNavResult
    data object ExitToEditor : HistoryNavResult
    data object Unhandled : HistoryNavResult
}

/**
 * Computes arrow-key navigation in history based on visual layout hierarchy:
 * - When an expression and result are stacked vertically under each other:
 *   - Formula is on TOP, Result is on the line UNDER it.
 *   - Moving UP from Result focuses the Formula directly above it.
 *   - Moving DOWN from Formula focuses the Result directly below it.
 * - When two results are under each other (inline rows):
 *   - Moving UP/DOWN moves directly between the results, skipping the expressions on the left.
 * - Moving LEFT/RIGHT toggles between Formula (left) and Result (right).
 */
internal fun computeNextHistorySelection(
    dir: String,
    currentIndex: Int,
    currentSide: HistorySide,
    historySize: Int,
    isEntryStacked: (Int) -> Boolean,
): HistoryNavResult {
    if (historySize <= 0 || currentIndex !in 0 until historySize) {
        return HistoryNavResult.Unhandled
    }

    val isCurrentStacked = isEntryStacked(currentIndex)

    return when (dir) {
        "UP" -> {
            if (isCurrentStacked && currentSide == HistorySide.RESULT) {
                // Stacked: Formula is directly above Result -> move up to Formula of this row
                HistoryNavResult.StayInHistory(currentIndex, HistorySide.FORMULA)
            } else if (currentIndex > 0) {
                val prevIndex = currentIndex - 1
                val isPrevStacked = isEntryStacked(prevIndex)
                if (isPrevStacked) {
                    // Moving up into a stacked item: Result is at the bottom of that item
                    HistoryNavResult.StayInHistory(prevIndex, HistorySide.RESULT)
                } else {
                    // Inline items under each other: maintain current side (skips formula if moving up results)
                    HistoryNavResult.StayInHistory(prevIndex, currentSide)
                }
            } else {
                HistoryNavResult.Unhandled
            }
        }
        "DOWN" -> {
            if (isCurrentStacked && currentSide == HistorySide.FORMULA) {
                // Stacked: Result is directly under Formula -> move down to Result of this row
                HistoryNavResult.StayInHistory(currentIndex, HistorySide.RESULT)
            } else if (currentIndex < historySize - 1) {
                val nextIndex = currentIndex + 1
                val isNextStacked = isEntryStacked(nextIndex)
                if (isNextStacked) {
                    // Moving down into a stacked item: Formula is at the top of that item
                    HistoryNavResult.StayInHistory(nextIndex, HistorySide.FORMULA)
                } else {
                    // Inline items under each other: maintain current side
                    HistoryNavResult.StayInHistory(nextIndex, currentSide)
                }
            } else {
                // At the bottom of history -> exit into calculator editor
                HistoryNavResult.ExitToEditor
            }
        }
        "LEFT" -> {
            if (currentSide != HistorySide.FORMULA) {
                HistoryNavResult.StayInHistory(currentIndex, HistorySide.FORMULA)
            } else {
                HistoryNavResult.Unhandled
            }
        }
        "RIGHT" -> {
            if (currentSide != HistorySide.RESULT) {
                HistoryNavResult.StayInHistory(currentIndex, HistorySide.RESULT)
            } else {
                HistoryNavResult.Unhandled
            }
        }
        else -> HistoryNavResult.Unhandled
    }
}
