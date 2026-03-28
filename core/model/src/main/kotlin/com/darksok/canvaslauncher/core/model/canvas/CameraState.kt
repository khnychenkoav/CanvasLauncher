package com.darksok.canvaslauncher.core.model.canvas

data class CameraState(
    val worldCenter: WorldPoint = WorldPoint(0f, 0f),
    val scale: Float = CanvasConstants.Defaults.INITIAL_SCALE,
    val viewportWidthPx: Int = 1,
    val viewportHeightPx: Int = 1,
)

object CanvasConstants {
    object Scale {
        const val MIN_SCALE: Float = 0.3f
        const val MAX_SCALE: Float = 2.0f
        const val LABEL_VISIBLE_THRESHOLD: Float = 0.6f
    }

    object Sizes {
        const val ICON_WORLD_SIZE: Float = 96f
        const val CULLING_BUFFER_WORLD: Float = 240f
    }

    object Icon {
        const val CACHE_BITMAP_SIZE_PX: Int = 144
        const val CACHE_MAX_BYTES: Int = 24 * 1024 * 1024
    }

    object Defaults {
        const val INITIAL_SCALE: Float = 1.0f
    }
}
