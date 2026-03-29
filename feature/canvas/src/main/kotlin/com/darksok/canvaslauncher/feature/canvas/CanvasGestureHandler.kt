package com.darksok.canvaslauncher.feature.canvas

import com.darksok.canvaslauncher.core.model.canvas.CanvasConstants
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
        val beforeScale = viewportController.cameraState.value.scale
        var scaleChanged = false
        if (zoomFactor != 1f) {
            viewportController.zoomBy(zoomFactor, focusPx)
            val afterScale = viewportController.cameraState.value.scale
            scaleChanged = afterScale != beforeScale
        }
        val blockedByScaleEdge = zoomFactor != 1f && !scaleChanged && (
            (beforeScale >= CanvasConstants.Scale.MAX_SCALE - SCALE_EDGE_EPSILON && zoomFactor > 1f) ||
                (beforeScale <= CanvasConstants.Scale.MIN_SCALE + SCALE_EDGE_EPSILON && zoomFactor < 1f)
            )
        if (blockedByScaleEdge) {
            return
        }
        if (panDeltaPx.x != 0f || panDeltaPx.y != 0f) {
            viewportController.panBy(panDeltaPx)
        }
    }

    private companion object {
        private const val SCALE_EDGE_EPSILON = 0.001f
    }
}
