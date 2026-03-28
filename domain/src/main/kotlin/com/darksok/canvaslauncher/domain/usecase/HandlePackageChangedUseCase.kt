package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import com.darksok.canvaslauncher.domain.repository.InstalledAppsSource
import javax.inject.Inject

class HandlePackageChangedUseCase @Inject constructor(
    private val installedAppsSource: InstalledAppsSource,
    private val appsStore: CanvasAppsStore,
    private val iconCacheGateway: IconCacheGateway,
) {
    suspend operator fun invoke(packageName: String) {
        val systemApp = installedAppsSource.getInstalledApp(packageName) ?: return
        val existing = appsStore.getAppsSnapshot().firstOrNull { it.packageName == packageName } ?: return
        appsStore.upsertApp(existing.copy(label = systemApp.label))
        iconCacheGateway.invalidate(packageName)
        iconCacheGateway.preload(listOf(packageName))
    }
}
