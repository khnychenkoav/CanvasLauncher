package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.domain.layout.InitialLayoutStrategy
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import java.util.Locale
import javax.inject.Inject

class RearrangeAppsUseCase @Inject constructor(
    private val appsStore: CanvasAppsStore,
    private val layoutStrategy: InitialLayoutStrategy,
) {
    suspend operator fun invoke(
        layoutMode: AppLayoutMode,
        center: WorldPoint = WorldPoint(0f, 0f),
    ): Int {
        val snapshot = appsStore.getAppsSnapshot()
        if (snapshot.isEmpty()) return 0

        val orderedApps = snapshot.sortedBy { app -> app.label.lowercase(Locale.ROOT) }
        val source = orderedApps.map { app ->
            InstalledApp(
                packageName = app.packageName,
                label = app.label,
            )
        }
        val rearranged = layoutStrategy.layout(
            existingApps = emptyList(),
            newApps = source,
            center = center,
            mode = layoutMode,
        )
        appsStore.upsertApps(rearranged)
        return rearranged.size
    }
}
