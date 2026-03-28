package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import javax.inject.Inject

class UpdateAppPositionUseCase @Inject constructor(
    private val appsStore: CanvasAppsStore,
) {
    suspend operator fun invoke(
        packageName: String,
        position: WorldPoint,
    ) {
        appsStore.updatePosition(packageName, position)
    }
}
