package com.darksok.canvaslauncher.feature.canvas

import android.graphics.Bitmap
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint

data class CanvasRenderableApp(
    val packageName: String,
    val label: String,
    val worldPosition: WorldPoint,
    val icon: Bitmap?,
    val searchVisualState: CanvasSearchVisualState = CanvasSearchVisualState.Normal,
)

enum class CanvasSearchVisualState {
    Normal,
    Dimmed,
    Matched,
}

data class DragState(
    val packageName: String,
    val worldPosition: WorldPoint,
)
