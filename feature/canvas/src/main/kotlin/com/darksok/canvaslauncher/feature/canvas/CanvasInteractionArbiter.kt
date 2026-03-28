package com.darksok.canvaslauncher.feature.canvas

internal class CanvasInteractionArbiter(
    private val tapSuppressionWindowMs: Long = DEFAULT_TAP_SUPPRESSION_WINDOW_MS,
) {

    private var activePointerCount: Int = 0
    private var suppressTapUntilUptimeMs: Long = 0L

    fun onPointerCountChanged(
        pressedPointerCount: Int,
        eventUptimeMs: Long,
    ) {
        activePointerCount = pressedPointerCount
        if (pressedPointerCount > 1) {
            suppressTapUntilUptimeMs = maxOf(
                suppressTapUntilUptimeMs,
                eventUptimeMs + tapSuppressionWindowMs,
            )
        }
    }

    fun onCanvasTransformDetected(eventUptimeMs: Long) {
        suppressTapUntilUptimeMs = maxOf(
            suppressTapUntilUptimeMs,
            eventUptimeMs + tapSuppressionWindowMs,
        )
    }

    fun shouldSuppressTap(nowUptimeMs: Long): Boolean {
        return activePointerCount > 1 || nowUptimeMs <= suppressTapUntilUptimeMs
    }

    private companion object {
        const val DEFAULT_TAP_SUPPRESSION_WINDOW_MS = 220L
    }
}
