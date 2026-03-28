package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.domain.layout.InitialLayoutStrategy
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import com.darksok.canvaslauncher.domain.repository.InstalledAppsSource
import com.darksok.canvaslauncher.domain.repository.LayoutPreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncAppsWithSystemUseCaseTest {

    @Test
    fun `sync removes stale updates labels and adds missing apps`() = runTest {
        val installedApps = listOf(
            InstalledApp(packageName = "new.app", label = "New App"),
            InstalledApp(packageName = "keep.app", label = "Keep Updated"),
        )
        val installedSource = FakeInstalledAppsSource(installedApps)
        val localStore = FakeCanvasAppsStore(
            initial = listOf(
                CanvasApp("old.app", "Old", WorldPoint(1f, 1f)),
                CanvasApp("keep.app", "Keep", WorldPoint(2f, 2f)),
            ),
        )
        val iconCache = FakeIconCacheGateway()
        val useCase = SyncAppsWithSystemUseCase(
            installedAppsSource = installedSource,
            appsStore = localStore,
            layoutStrategy = FakeLayoutStrategy(),
            iconCacheGateway = iconCache,
            layoutPreferencesRepository = FakeLayoutPreferencesRepository(),
        )

        val report = useCase(centerForNewApps = WorldPoint(10f, 20f))

        assertThat(report.added).isEqualTo(1)
        assertThat(report.removed).isEqualTo(1)
        assertThat(report.updated).isEqualTo(1)

        val packages = localStore.snapshot().map { it.packageName }
        assertThat(packages).containsExactly("keep.app", "new.app")
        assertThat(localStore.snapshot().first { it.packageName == "keep.app" }.label).isEqualTo("Keep Updated")
        assertThat(iconCache.preloaded.last()).containsExactly("new.app", "keep.app")
        assertThat(iconCache.removed).contains("old.app")
    }

    private class FakeInstalledAppsSource(
        private val items: List<InstalledApp>,
    ) : InstalledAppsSource {
        override suspend fun getInstalledApps(): List<InstalledApp> = items

        override suspend fun getInstalledApp(packageName: String): InstalledApp? {
            return items.firstOrNull { it.packageName == packageName }
        }
    }

    private class FakeCanvasAppsStore(
        initial: List<CanvasApp>,
    ) : CanvasAppsStore {
        private val state = MutableStateFlow(initial)

        override fun observeApps(): Flow<List<CanvasApp>> = state.asStateFlow()

        override suspend fun getAppsSnapshot(): List<CanvasApp> = state.value

        override suspend fun upsertApps(apps: List<CanvasApp>) {
            val current = state.value.associateBy { it.packageName }.toMutableMap()
            apps.forEach { current[it.packageName] = it }
            state.value = current.values.toList()
        }

        override suspend fun upsertApp(app: CanvasApp) {
            upsertApps(listOf(app))
        }

        override suspend fun removePackages(packages: Set<String>) {
            state.value = state.value.filterNot { it.packageName in packages }
        }

        override suspend fun removePackage(packageName: String) {
            state.value = state.value.filterNot { it.packageName == packageName }
        }

        override suspend fun updatePosition(packageName: String, position: WorldPoint) {
            state.value = state.value.map { app ->
                if (app.packageName == packageName) app.copy(position = position) else app
            }
        }

        fun snapshot(): List<CanvasApp> = state.value
    }

    private class FakeLayoutStrategy : InitialLayoutStrategy {
        override fun layout(
            existingApps: List<CanvasApp>,
            newApps: List<InstalledApp>,
            center: WorldPoint,
            mode: AppLayoutMode,
        ): List<CanvasApp> {
            return newApps.mapIndexed { index, app ->
                CanvasApp(
                    packageName = app.packageName,
                    label = app.label,
                    position = WorldPoint(center.x + index, center.y + index),
                )
            }
        }
    }

    private class FakeIconCacheGateway : IconCacheGateway {
        val preloaded = mutableListOf<Set<String>>()
        val removed = mutableListOf<String>()

        override suspend fun preload(packageNames: Collection<String>) {
            preloaded += packageNames.toSet()
        }

        override suspend fun invalidate(packageName: String) = Unit

        override suspend fun remove(packageName: String) {
            removed += packageName
        }
    }

    private class FakeLayoutPreferencesRepository : LayoutPreferencesRepository {
        override fun observeLayoutMode(): Flow<AppLayoutMode> = flowOf(AppLayoutMode.SPIRAL)
        override suspend fun setLayoutMode(layoutMode: AppLayoutMode) = Unit
    }
}
