package com.darksok.canvaslauncher.core.performance

import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.CanvasConstants
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import kotlin.math.max
import kotlin.math.min

object WorldScreenTransformer {

    fun worldToScreen(point: WorldPoint, camera: CameraState): ScreenPoint {
        val halfW = camera.viewportWidthPx / 2f
        val halfH = camera.viewportHeightPx / 2f
        return ScreenPoint(
            x = (point.x - camera.worldCenter.x) * camera.scale + halfW,
            y = (point.y - camera.worldCenter.y) * camera.scale + halfH,
        )
    }

    fun screenToWorld(point: ScreenPoint, camera: CameraState): WorldPoint {
        val halfW = camera.viewportWidthPx / 2f
        val halfH = camera.viewportHeightPx / 2f
        return WorldPoint(
            x = (point.x - halfW) / camera.scale + camera.worldCenter.x,
            y = (point.y - halfH) / camera.scale + camera.worldCenter.y,
        )
    }

    fun applyPan(camera: CameraState, panDeltaPx: ScreenPoint): CameraState {
        return camera.copy(
            worldCenter = WorldPoint(
                x = camera.worldCenter.x - panDeltaPx.x / camera.scale,
                y = camera.worldCenter.y - panDeltaPx.y / camera.scale,
            ),
        )
    }

    fun applyZoom(
        camera: CameraState,
        zoomFactor: Float,
        focus: ScreenPoint,
    ): CameraState {
        val newScale = clampScale(camera.scale * zoomFactor)
        if (newScale == camera.scale) return camera

        // Keep the world coordinate under the pinch centroid stable while scaling.
        val anchoredWorld = screenToWorld(focus, camera)
        val halfW = camera.viewportWidthPx / 2f
        val halfH = camera.viewportHeightPx / 2f

        val newCenter = WorldPoint(
            x = anchoredWorld.x - (focus.x - halfW) / newScale,
            y = anchoredWorld.y - (focus.y - halfH) / newScale,
        )

        return camera.copy(
            worldCenter = newCenter,
            scale = newScale,
        )
    }

    fun clampScale(scale: Float): Float {
        val clamped = min(CanvasConstants.Scale.MAX_SCALE, max(CanvasConstants.Scale.MIN_SCALE, scale))
        return when {
            clamped >= CanvasConstants.Scale.MAX_SCALE - SCALE_EDGE_SNAP_EPSILON -> CanvasConstants.Scale.MAX_SCALE
            clamped <= CanvasConstants.Scale.MIN_SCALE + SCALE_EDGE_SNAP_EPSILON -> CanvasConstants.Scale.MIN_SCALE
            else -> clamped
        }
    }

    private const val SCALE_EDGE_SNAP_EPSILON = 0.01f
}
