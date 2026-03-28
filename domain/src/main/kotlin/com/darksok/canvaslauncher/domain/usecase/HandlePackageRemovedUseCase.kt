package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import javax.inject.Inject

class HandlePackageRemovedUseCase @Inject constructor(
    private val appsStore: CanvasAppsStore,
    private val iconCacheGateway: IconCacheGateway,
) {
    suspend operator fun invoke(packageName: String) {
        appsStore.removePackage(packageName)
        iconCacheGateway.remove(packageName)
    }
}
