package com.darksok.canvaslauncher.core.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow

fun primaryTextGlowShadow(
    color: Color,
    alpha: Float = PRIMARY_TEXT_GLOW_ALPHA,
    blurRadius: Float = PRIMARY_TEXT_GLOW_RADIUS_PX,
): Shadow {
    return Shadow(
        color = color.copy(alpha = alpha),
        offset = Offset.Zero,
        blurRadius = blurRadius,
    )
}

private const val PRIMARY_TEXT_GLOW_ALPHA = 0.62f
private const val PRIMARY_TEXT_GLOW_RADIUS_PX = 10f
