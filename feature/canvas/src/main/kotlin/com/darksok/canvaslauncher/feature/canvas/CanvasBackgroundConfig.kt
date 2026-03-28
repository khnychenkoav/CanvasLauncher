package com.darksok.canvaslauncher.feature.canvas

import androidx.compose.ui.graphics.Color

data class CanvasBackgroundConfig(
    val fillColor: Color,
    val dotColor: Color,
    val dotSpacingWorld: Float = 120f,
    val dotRadiusPx: Float = 1.5f,
)
