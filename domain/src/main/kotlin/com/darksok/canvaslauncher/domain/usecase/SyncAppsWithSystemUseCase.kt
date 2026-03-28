package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.domain.layout.InitialLayoutStrategy
import com.darksok.canvaslauncher.domain.model.SyncReport
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import com.darksok.canvaslauncher.domain.repository.InstalledAppsSource
import com.darksok.canvaslauncher.domain.repository.LayoutPreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SyncAppsWithSystemUseCase @Inject constructor(
    private val installedAppsSource: InstalledAppsSource,
    private val appsStore: CanvasAppsStore,
    private val layoutStrategy: InitialLayoutStrategy,
    private val iconCacheGateway: IconCacheGateway,
    private val layoutPreferencesRepository: LayoutPreferencesRepository,
) {
    suspend operator fun invoke(
        centerForNewApps: WorldPoint = WorldPoint(0f, 0f),
        preloadIcons: Boolean = true,
    ): SyncReport {
        val layoutMode = layoutPreferencesRepository.observeLayoutMode().first()
        val installed = installedAppsSource.getInstalledApps()
        val local = appsStore.getAppsSnapshot()

        val installedByPackage = installed.associateBy { it.packageName }
        val localByPackage = local.associateBy { it.packageName }

        val toRemove = localByPackage.keys - installedByPackage.keys
        if (toRemove.isNotEmpty()) {
            appsStore.removePackages(toRemove)
            toRemove.forEach { iconCacheGateway.remove(it) }
        }

        val toUpdate = local.mapNotNull { localApp ->
            val system = installedByPackage[localApp.packageName] ?: return@mapNotNull null
            if (system.label != localApp.label) {
                localApp.copy(label = system.label)
            } else {
                null
            }
        }
        if (toUpdate.isNotEmpty()) {
            appsStore.upsertApps(toUpdate)
        }

        val toAddInstalled = installed.filterNot { localByPackage.containsKey(it.packageName) }
        if (toAddInstalled.isNotEmpty()) {
            val effectiveExisting = local
                .filterNot { toRemove.contains(it.packageName) }
                .map { existing ->
                    toUpdate.firstOrNull { it.packageName == existing.packageName } ?: existing
                }
            val placed = layoutStrategy.layout(
                existingApps = effectiveExisting,
                newApps = toAddInstalled,
                center = centerForNewApps,
                mode = layoutMode,
            )
            appsStore.upsertApps(placed)
        }

        if (preloadIcons) {
            iconCacheGateway.preload(installedByPackage.keys)
        }

        return SyncReport(
            added = toAddInstalled.size,
            removed = toRemove.size,
            updated = toUpdate.size,
        )
    }
}
