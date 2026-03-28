package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAppsUseCase @Inject constructor(
    private val appsStore: CanvasAppsStore,
) {
    operator fun invoke(): Flow<List<CanvasApp>> = appsStore.observeApps()
}
