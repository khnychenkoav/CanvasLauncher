package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.domain.layout.InitialLayoutStrategy
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import com.darksok.canvaslauncher.domain.repository.InstalledAppsSource
import com.darksok.canvaslauncher.domain.repository.LayoutPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class HandlePackageAddedUseCase @Inject constructor(
    private val installedAppsSource: InstalledAppsSource,
    private val appsStore: CanvasAppsStore,
    private val iconCacheGateway: IconCacheGateway,
    private val layoutStrategy: InitialLayoutStrategy,
    private val layoutPreferencesRepository: LayoutPreferencesRepository,
) {
    suspend operator fun invoke(
        packageName: String,
        worldCenter: WorldPoint,
    ) {
        val app = installedAppsSource.getInstalledApp(packageName) ?: return
        val snapshot = appsStore.getAppsSnapshot()
        val existing = snapshot.firstOrNull { it.packageName == packageName }
        val entry = existing ?: run {
            val mode = layoutPreferencesRepository.observeLayoutMode().first()
            layoutStrategy.layout(
                existingApps = snapshot,
                newApps = listOf(app),
                center = worldCenter,
                mode = mode,
            ).first()
        }

        val updatedEntry = entry.copy(label = app.label)

        appsStore.upsertApp(updatedEntry)
        iconCacheGateway.preload(listOf(packageName))
    }
}
