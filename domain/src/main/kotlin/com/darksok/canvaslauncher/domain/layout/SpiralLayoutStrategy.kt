package com.darksok.canvaslauncher.domain.layout

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import kotlin.math.cos
import kotlin.math.sin

class SpiralLayoutStrategy(
    private val radialStep: Float = 38f,
    private val angularStepRad: Float = 0.52f,
) : InitialLayoutStrategy {

    override fun layout(
        existingApps: List<CanvasApp>,
        newApps: List<InstalledApp>,
        center: WorldPoint,
        mode: AppLayoutMode,
    ): List<CanvasApp> {
        if (newApps.isEmpty()) return emptyList()
        val startIndex = existingApps.size
        return newApps.mapIndexed { index, app ->
            val n = startIndex + index
            val angle = n * angularStepRad
            val radius = radialStep * angle
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            CanvasApp(
                packageName = app.packageName,
                label = app.label,
                position = WorldPoint(x, y),
            )
        }
    }
}
