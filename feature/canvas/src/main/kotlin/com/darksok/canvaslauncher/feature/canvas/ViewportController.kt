package com.darksok.canvaslauncher.feature.canvas

import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.performance.WorldScreenTransformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface ViewportController {
    val cameraState: StateFlow<CameraState>
    fun updateViewportSize(widthPx: Int, heightPx: Int)
    fun panBy(deltaPx: ScreenPoint)
    fun zoomBy(zoomFactor: Float, focusPx: ScreenPoint)
    fun centerOn(worldPoint: WorldPoint, screenOffsetPx: ScreenPoint = ScreenPoint(0f, 0f))
    fun setCamera(cameraState: CameraState)
    fun screenToWorld(screenPoint: ScreenPoint): WorldPoint
    fun worldToScreen(worldPoint: WorldPoint): ScreenPoint
}

class DefaultViewportController @Inject constructor() : ViewportController {

    private val camera = MutableStateFlow(CameraState())
    override val cameraState: StateFlow<CameraState> = camera.asStateFlow()

    override fun updateViewportSize(widthPx: Int, heightPx: Int) {
        val newWidth = if (widthPx <= 0) 1 else widthPx
        val newHeight = if (heightPx <= 0) 1 else heightPx
        camera.value = camera.value.copy(
            viewportWidthPx = newWidth,
            viewportHeightPx = newHeight,
        )
    }

    override fun panBy(deltaPx: ScreenPoint) {
        camera.value = WorldScreenTransformer.applyPan(camera.value, deltaPx)
    }

    override fun zoomBy(zoomFactor: Float, focusPx: ScreenPoint) {
        camera.value = WorldScreenTransformer.applyZoom(camera.value, zoomFactor, focusPx)
    }

    override fun centerOn(worldPoint: WorldPoint, screenOffsetPx: ScreenPoint) {
        val scale = camera.value.scale
        val adjustedCenter = WorldPoint(
            x = worldPoint.x - (screenOffsetPx.x / scale),
            y = worldPoint.y - (screenOffsetPx.y / scale),
        )
        camera.value = camera.value.copy(worldCenter = adjustedCenter)
    }

    override fun setCamera(cameraState: CameraState) {
        camera.value = cameraState
    }

    override fun screenToWorld(screenPoint: ScreenPoint): WorldPoint {
        return WorldScreenTransformer.screenToWorld(screenPoint, camera.value)
    }

    override fun worldToScreen(worldPoint: WorldPoint): ScreenPoint {
        return WorldScreenTransformer.worldToScreen(worldPoint, camera.value)
    }
}
