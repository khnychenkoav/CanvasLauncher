package com.darksok.canvaslauncher.feature.canvas

import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import javax.inject.Inject

interface CanvasGestureHandler {
    fun onTransform(
        panDeltaPx: ScreenPoint,
        zoomFactor: Float,
        focusPx: ScreenPoint,
    )
}

class DefaultCanvasGestureHandler @Inject constructor(
    private val viewportController: ViewportController,
) : CanvasGestureHandler {
    override fun onTransform(
        panDeltaPx: ScreenPoint,
        zoomFactor: Float,
        focusPx: ScreenPoint,
    ) {
        if (zoomFactor != 1f) {
            viewportController.zoomBy(zoomFactor, focusPx)
        }
        if (panDeltaPx.x != 0f || panDeltaPx.y != 0f) {
            viewportController.panBy(panDeltaPx)
        }
    }
}
