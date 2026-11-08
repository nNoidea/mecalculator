package com.nnoidea.mecalculator.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi

/**
 * Provides crisp, low-latency tactile feedback for calculator buttons and rapid cursor ticks.
 * Uses hardware LRA primitives (EFFECT_CLICK / EFFECT_TICK / PRIMITIVE_CLICK / PRIMITIVE_TICK)
 * instead of legacy waveform durations so vibrations feel sharp and mechanical rather than mushy.
 */
object HapticHelper {

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateEffect(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_TOUCH)
                .build()
            vibrator.vibrate(effect, attrs)
        } else {
            vibrator.vibrate(effect)
        }
    }

    /**
     * Crisp mechanical button press click (triggered immediately on pointer touch down).
     */
    fun performClick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val vibrator = getVibrator(view.context)
            if (vibrator != null && vibrator.hasVibrator()) {
                if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f)
                        .compose()
                    vibrateEffect(vibrator, effect)
                    return
                }
                vibrateEffect(vibrator, VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                return
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = getVibrator(view.context)
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrateEffect(vibrator, VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                return
            }
        }
        view.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
        )
    }

    /**
     * Ultra-short crisp detent tick for rapid arrow-key repeat steps and selection changes.
     */
    fun performTick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val vibrator = getVibrator(view.context)
            if (vibrator != null && vibrator.hasVibrator()) {
                if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)) {
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.75f)
                        .compose()
                    vibrateEffect(vibrator, effect)
                    return
                }
                vibrateEffect(vibrator, VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                return
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = getVibrator(view.context)
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrateEffect(vibrator, VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                return
            }
        }
        view.performHapticFeedback(
            HapticFeedbackConstants.CLOCK_TICK,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
        )
    }

    /**
     * Stronger crisp thud for destructive/major actions (like holding Backspace to clear all).
     */
    fun performHeavyClick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vibrator = getVibrator(view.context)
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrateEffect(vibrator, VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                return
            }
        }
        view.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
        )
    }
}
