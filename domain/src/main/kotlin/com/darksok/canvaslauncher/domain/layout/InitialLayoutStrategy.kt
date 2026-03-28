package com.darksok.canvaslauncher.domain.layout

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode

interface InitialLayoutStrategy {
    fun layout(
        existingApps: List<CanvasApp>,
        newApps: List<InstalledApp>,
        center: WorldPoint = WorldPoint(0f, 0f),
        mode: AppLayoutMode = AppLayoutMode.SPIRAL,
    ): List<CanvasApp>
}
