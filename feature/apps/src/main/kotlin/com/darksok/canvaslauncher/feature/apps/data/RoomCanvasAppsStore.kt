package com.darksok.canvaslauncher.feature.apps.data

import com.darksok.canvaslauncher.core.common.coroutines.DispatchersProvider
import com.darksok.canvaslauncher.core.database.dao.AppDao
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCanvasAppsStore @Inject constructor(
    private val appDao: AppDao,
    private val dispatchersProvider: DispatchersProvider,
) : CanvasAppsStore {

    override fun observeApps(): Flow<List<CanvasApp>> {
        return appDao.observeApps().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAppsSnapshot(): List<CanvasApp> = withContext(dispatchersProvider.io) {
        appDao.getAppsOnce().map { it.toDomain() }
    }

    override suspend fun upsertApps(apps: List<CanvasApp>) {
        if (apps.isEmpty()) return
        withContext(dispatchersProvider.io) {
            appDao.upsert(apps.map { it.toEntity() })
        }
    }

    override suspend fun upsertApp(app: CanvasApp) {
        withContext(dispatchersProvider.io) {
            appDao.upsert(app.toEntity())
        }
    }

    override suspend fun removePackages(packages: Set<String>) {
        if (packages.isEmpty()) return
        withContext(dispatchersProvider.io) {
            appDao.deleteByPackageNames(packages.toList())
        }
    }

    override suspend fun removePackage(packageName: String) {
        withContext(dispatchersProvider.io) {
            appDao.deleteByPackageName(packageName)
        }
    }

    override suspend fun updatePosition(
        packageName: String,
        position: WorldPoint,
    ) {
        withContext(dispatchersProvider.io) {
            appDao.updatePosition(packageName, position.x, position.y)
        }
    }
}
