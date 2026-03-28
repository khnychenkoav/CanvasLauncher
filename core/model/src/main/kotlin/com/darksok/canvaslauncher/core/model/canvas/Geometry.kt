package com.darksok.canvaslauncher.core.model.canvas

data class WorldPoint(
    val x: Float,
    val y: Float,
)

data class ScreenPoint(
    val x: Float,
    val y: Float,
)

data class WorldRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun contains(point: WorldPoint): Boolean {
        return point.x in left..right && point.y in top..bottom
    }
}
