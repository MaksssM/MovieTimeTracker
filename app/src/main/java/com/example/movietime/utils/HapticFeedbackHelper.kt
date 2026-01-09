package com.example.movietime.utils

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Helper class to provide consistent haptic feedback across the application.
 * Uses standard Android HapticFeedbackConstants to ensure compatibility and premium feel.
 */
object HapticFeedbackHelper {

    /**
     * Subtle tactile feedback for small interactions (e.g., clicking items, small scrolls).
     */
    fun impactLow(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Medium tactile feedback for successful actions (e.g., marking watched, rating).
     */
    fun impactMedium(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Strong tactile feedback for significant events.
     */
    fun impactHigh(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Specific tactile feedback for selection changes (e.g., scrolling wheels/drums).
     */
    fun selection(view: View) {
        // HapticFeedbackConstants.CLOCK_TICK is great for drums/wheels but requires API 21+
        // In many cases KEYBOARD_TAP or VIRTUAL_KEY works well too.
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Tactile feedback for confirmed success (e.g., finishing a task).
     */
    fun success(view: View) {
        // Double tap or specific success effect
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}
