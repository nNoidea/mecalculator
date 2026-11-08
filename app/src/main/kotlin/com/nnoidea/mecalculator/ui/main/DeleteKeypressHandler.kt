package com.nnoidea.mecalculator.ui.main

/**
 * Action taken when a long-press on the delete/backspace key triggers.
 */
enum class DeleteActionTaken {
    NONE,
    CLEARED_TOKENS,
    CLEARED_HISTORY,
}

/**
 * Encapsulates the two-stage long-press behavior of the delete/backspace key:
 * 1. If tokens are present when the user presses down, holding delete clears ONLY the tokens.
 *    Subsequent callbacks during the SAME continuous press gesture cannot clear history.
 * 2. Only if the input field was already empty when the user pressed down, holding delete
 *    clears the calculation history.
 * 3. Each distinct press gesture (press down -> release/cancel) can execute at most ONE action.
 */
class DeleteKeypressHandler(
    private val getTokens: () -> List<MathToken>,
    private val onClearTokens: () -> Unit,
    private val onClearHistory: () -> Unit,
) {
    /** True if tokens were not empty at the moment press down occurred. */
    var gestureStartedWithTokens: Boolean = false
        private set

    /** True if a long-press action has already been performed during the current press gesture. */
    var actionConsumedInGesture: Boolean = false
        private set

    fun onPressDown() {
        gestureStartedWithTokens = getTokens().isNotEmpty()
        actionConsumedInGesture = false
    }

    fun onPressUp() {
        actionConsumedInGesture = false
        gestureStartedWithTokens = false
    }

    fun onLongPress(): DeleteActionTaken {
        if (actionConsumedInGesture) {
            return DeleteActionTaken.NONE
        }
        actionConsumedInGesture = true
        return if (gestureStartedWithTokens || getTokens().isNotEmpty()) {
            onClearTokens()
            DeleteActionTaken.CLEARED_TOKENS
        } else {
            onClearHistory()
            DeleteActionTaken.CLEARED_HISTORY
        }
    }
}
