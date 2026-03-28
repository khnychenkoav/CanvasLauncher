package com.darksok.canvaslauncher.core.performance

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.CanvasConstants
import com.darksok.canvaslauncher.core.model.canvas.WorldRect

object ViewportCuller {

    fun visibleWorldRect(
        camera: CameraState,
        bufferWorld: Float = CanvasConstants.Sizes.CULLING_BUFFER_WORLD,
    ): WorldRect {
        // Convert current screen viewport to world bounds and expand with a buffer
        // to prevent edge pop-in during fast pan/zoom.
        val halfWidthWorld = camera.viewportWidthPx / (2f * camera.scale)
        val halfHeightWorld = camera.viewportHeightPx / (2f * camera.scale)
        return WorldRect(
            left = camera.worldCenter.x - halfWidthWorld - bufferWorld,
            top = camera.worldCenter.y - halfHeightWorld - bufferWorld,
            right = camera.worldCenter.x + halfWidthWorld + bufferWorld,
            bottom = camera.worldCenter.y + halfHeightWorld + bufferWorld,
        )
    }

    fun cullVisibleApps(
        apps: List<CanvasApp>,
        camera: CameraState,
        iconWorldSize: Float = CanvasConstants.Sizes.ICON_WORLD_SIZE,
        bufferWorld: Float = CanvasConstants.Sizes.CULLING_BUFFER_WORLD,
    ): List<CanvasApp> {
        val visible = visibleWorldRect(camera, bufferWorld)
        val half = iconWorldSize / 2f
        return apps.filter { app ->
            val left = app.position.x - half
            val top = app.position.y - half
            val right = app.position.x + half
            val bottom = app.position.y + half

            right >= visible.left &&
                left <= visible.right &&
                bottom >= visible.top &&
                top <= visible.bottom
        }
    }
}
