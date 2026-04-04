package com.darksok.canvaslauncher.feature.canvas

import com.darksok.canvaslauncher.core.model.canvas.CanvasConstants
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.performance.WorldScreenTransformer
import kotlin.math.sqrt
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
        val before = viewportController.cameraState.value
        val beforeScale = before.scale
        var updated = before
        var scaleChanged = false
        if (zoomFactor != 1f) {
            updated = WorldScreenTransformer.applyZoom(updated, zoomFactor, focusPx)
            val afterScale = updated.scale
            scaleChanged = afterScale != beforeScale
        }
        val blockedByScaleEdge = zoomFactor != 1f && !scaleChanged && (
            (beforeScale >= CanvasConstants.Scale.MAX_SCALE - SCALE_EDGE_EPSILON && zoomFactor > 1f) ||
                (beforeScale <= CanvasConstants.Scale.MIN_SCALE + SCALE_EDGE_EPSILON && zoomFactor < 1f)
            )
        val panDistancePx = sqrt(panDeltaPx.x * panDeltaPx.x + panDeltaPx.y * panDeltaPx.y)
        val panIsMeaningfulAtEdge = panDistancePx >= SCALE_EDGE_PAN_JITTER_PX
        if ((panDeltaPx.x != 0f || panDeltaPx.y != 0f) && (!blockedByScaleEdge || panIsMeaningfulAtEdge)) {
            updated = WorldScreenTransformer.applyPan(updated, panDeltaPx)
        }
        if (updated != before) {
            viewportController.setCamera(updated)
        }
    }

    private companion object {
        private const val SCALE_EDGE_EPSILON = 0.001f
        private const val SCALE_EDGE_PAN_JITTER_PX = 6f
    }
}
