package com.darksok.canvaslauncher.core.performance

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint

data class GridOriginPx(
    val x: Float,
    val y: Float,
)

object WorldGridProjector {

    fun anchoredOrigin(
        worldCenter: WorldPoint,
        scale: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        spacingPx: Float,
    ): GridOriginPx {
        return GridOriginPx(
            x = positiveModulo(
                value = (-worldCenter.x * scale) + viewportWidthPx / 2f,
                mod = spacingPx,
            ),
            y = positiveModulo(
                value = (-worldCenter.y * scale) + viewportHeightPx / 2f,
                mod = spacingPx,
            ),
        )
    }

    fun positiveModulo(value: Float, mod: Float): Float {
        if (mod <= 0f) return 0f
        val result = value % mod
        return if (result < 0f) result + mod else result
    }
}
