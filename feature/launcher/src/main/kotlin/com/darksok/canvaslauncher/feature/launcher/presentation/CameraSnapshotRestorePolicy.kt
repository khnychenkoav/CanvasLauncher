package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.canvas.CameraState

internal fun CameraState.withCurrentViewport(current: CameraState): CameraState {
    return copy(
        viewportWidthPx = current.viewportWidthPx,
        viewportHeightPx = current.viewportHeightPx,
    )
}
