package com.darksok.canvaslauncher.core.performance

import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import kotlin.math.max
import kotlin.math.min

data class MiniMapPoint(
    val x: Float,
    val y: Float,
)

data class MiniMapRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class MiniMapProjection(
    val appPoints: List<MiniMapPoint>,
    val userPoint: MiniMapPoint,
    val viewportRect: MiniMapRect,
)

object MiniMapProjector {
    const val SHOW_FROM_SCALE: Float = 1.35f
    private const val MIN_WORLD_SPAN: Float = 1_000f

    fun shouldShow(scale: Float): Boolean = scale >= SHOW_FROM_SCALE

    fun project(
        appPositions: List<WorldPoint>,
        camera: CameraState,
        mapWidthPx: Float,
        mapHeightPx: Float,
        worldPadding: Float = 220f,
    ): MiniMapProjection {
        val fallback = camera.worldCenter
        val source = if (appPositions.isEmpty()) listOf(fallback) else appPositions + fallback

        var minX = source.minOf { it.x } - worldPadding
        var maxX = source.maxOf { it.x } + worldPadding
        var minY = source.minOf { it.y } - worldPadding
        var maxY = source.maxOf { it.y } + worldPadding

        if (maxX - minX < MIN_WORLD_SPAN) {
            val cx = (maxX + minX) / 2f
            minX = cx - MIN_WORLD_SPAN / 2f
            maxX = cx + MIN_WORLD_SPAN / 2f
        }
        if (maxY - minY < MIN_WORLD_SPAN) {
            val cy = (maxY + minY) / 2f
            minY = cy - MIN_WORLD_SPAN / 2f
            maxY = cy + MIN_WORLD_SPAN / 2f
        }

        val worldWidth = maxX - minX
        val worldHeight = maxY - minY

        fun toMap(point: WorldPoint): MiniMapPoint {
            val nx = ((point.x - minX) / worldWidth).coerceIn(0f, 1f)
            val ny = ((point.y - minY) / worldHeight).coerceIn(0f, 1f)
            return MiniMapPoint(
                x = nx * mapWidthPx,
                y = ny * mapHeightPx,
            )
        }

        val userPoint = toMap(camera.worldCenter)
        val appPoints = appPositions.map(::toMap)

        val visibleWorldWidth = camera.viewportWidthPx / camera.scale
        val visibleWorldHeight = camera.viewportHeightPx / camera.scale
        val halfRectW = visibleWorldWidth / worldWidth * mapWidthPx / 2f
        val halfRectH = visibleWorldHeight / worldHeight * mapHeightPx / 2f

        val viewportRect = MiniMapRect(
            left = max(0f, userPoint.x - halfRectW),
            top = max(0f, userPoint.y - halfRectH),
            right = min(mapWidthPx, userPoint.x + halfRectW),
            bottom = min(mapHeightPx, userPoint.y + halfRectH),
        )

        return MiniMapProjection(
            appPoints = appPoints,
            userPoint = userPoint,
            viewportRect = viewportRect,
        )
    }
}
