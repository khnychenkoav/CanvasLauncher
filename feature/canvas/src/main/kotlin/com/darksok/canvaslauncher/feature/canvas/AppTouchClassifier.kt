package com.darksok.canvaslauncher.feature.canvas

object AppTouchClassifier {

    fun shouldTriggerTap(
        maxPointerCount: Int,
        totalMoveDistancePx: Float,
        touchSlopPx: Float,
        isTapSuppressed: Boolean,
    ): Boolean {
        if (isTapSuppressed) return false
        if (maxPointerCount != 1) return false
        return totalMoveDistancePx <= touchSlopPx
    }
}
