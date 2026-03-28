package com.darksok.canvaslauncher.core.model.app

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint

data class InstalledApp(
    val packageName: String,
    val label: String,
)

data class CanvasApp(
    val packageName: String,
    val label: String,
    val position: WorldPoint,
)
