package com.darksok.canvaslauncher.feature.canvas

import com.darksok.canvaslauncher.core.model.canvas.CanvasConstants

internal object LabelVisibilityPolicy {
    private const val HYSTERESIS = 0.04f
    private val showThreshold = CanvasConstants.Scale.LABEL_VISIBLE_THRESHOLD + HYSTERESIS
    private val hideThreshold = CanvasConstants.Scale.LABEL_VISIBLE_THRESHOLD - HYSTERESIS

    fun nextVisibility(
        previousVisible: Boolean,
        scale: Float,
    ): Boolean {
        return when {
            previousVisible && scale < hideThreshold -> false
            !previousVisible && scale > showThreshold -> true
            else -> previousVisible
        }
    }
}
