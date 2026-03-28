package com.darksok.canvaslauncher.domain.repository

import com.darksok.canvaslauncher.core.common.result.AppResult
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import kotlinx.coroutines.flow.Flow

interface CanvasAppsStore {
    fun observeApps(): Flow<List<CanvasApp>>
    suspend fun getAppsSnapshot(): List<CanvasApp>
    suspend fun upsertApps(apps: List<CanvasApp>)
    suspend fun upsertApp(app: CanvasApp)
    suspend fun removePackages(packages: Set<String>)
    suspend fun removePackage(packageName: String)
    suspend fun updatePosition(packageName: String, position: WorldPoint)
}

interface InstalledAppsSource {
    suspend fun getInstalledApps(): List<InstalledApp>
    suspend fun getInstalledApp(packageName: String): InstalledApp?
}

interface AppLaunchService {
    suspend fun launch(packageName: String): AppResult<Unit>
}

interface IconCacheGateway {
    suspend fun preload(packageNames: Collection<String>)
    suspend fun invalidate(packageName: String)
    suspend fun remove(packageName: String)
}
