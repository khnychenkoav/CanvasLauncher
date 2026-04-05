package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.domain.support.FakeCanvasAppsStore
import com.darksok.canvaslauncher.domain.support.FakeIconCacheGateway
import com.darksok.canvaslauncher.domain.support.FakeInstalledAppsSource
import com.darksok.canvaslauncher.domain.support.FakeLayoutPreferencesRepository
import com.darksok.canvaslauncher.domain.support.FakeLayoutStrategy
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncAppsWithSystemUseCaseTest {

    @Test
    fun `sync removes stale updates labels and adds missing apps`() = runTest {
        val installedSource = FakeInstalledAppsSource(
            listOf(
                InstalledApp(packageName = "new.app", label = "New App"),
                InstalledApp(packageName = "keep.app", label = "Keep Updated"),
            ),
        )
        val localStore = FakeCanvasAppsStore(
            listOf(
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
        assertThat(localStore.snapshot().map { it.packageName }).containsExactly("keep.app", "new.app")
        assertThat(localStore.snapshot().first { it.packageName == "keep.app" }.label).isEqualTo("Keep Updated")
        assertThat(iconCache.preloaded.last()).containsExactly("new.app", "keep.app")
        assertThat(iconCache.removed).contains("old.app")
    }

    @Test
    fun `sync skips preload when disabled`() = runTest {
        val iconCache = FakeIconCacheGateway()
        val useCase = SyncAppsWithSystemUseCase(
            installedAppsSource = FakeInstalledAppsSource(listOf(InstalledApp("pkg.one", "One"))),
            appsStore = FakeCanvasAppsStore(),
            layoutStrategy = FakeLayoutStrategy(),
            iconCacheGateway = iconCache,
            layoutPreferencesRepository = FakeLayoutPreferencesRepository(),
        )

        useCase(preloadIcons = false)

        assertThat(iconCache.preloaded).isEmpty()
    }

    @Test
    fun `sync does not call remove when nothing stale exists`() = runTest {
        val store = FakeCanvasAppsStore(listOf(CanvasApp("pkg.one", "One", WorldPoint(0f, 0f))))
        val useCase = SyncAppsWithSystemUseCase(
            installedAppsSource = FakeInstalledAppsSource(listOf(InstalledApp("pkg.one", "One"))),
            appsStore = store,
            layoutStrategy = FakeLayoutStrategy(),
            iconCacheGateway = FakeIconCacheGateway(),
            layoutPreferencesRepository = FakeLayoutPreferencesRepository(),
        )

        val report = useCase()

        assertThat(report.removed).isEqualTo(0)
        assertThat(store.removedPackagesCalls).isEmpty()
    }

    @Test
    fun `sync does not call update when labels already match`() = runTest {
        val store = FakeCanvasAppsStore(listOf(CanvasApp("pkg.one", "One", WorldPoint(0f, 0f))))
        val useCase = SyncAppsWithSystemUseCase(
            installedAppsSource = FakeInstalledAppsSource(listOf(InstalledApp("pkg.one", "One"))),
            appsStore = store,
            layoutStrategy = FakeLayoutStrategy(),
            iconCacheGateway = FakeIconCacheGateway(),
            layoutPreferencesRepository = FakeLayoutPreferencesRepository(),
        )

        val report = useCase()

        assertThat(report.updated).isEqualTo(0)
        assertThat(store.upsertAppsCalls).isEmpty()
    }

    @Test
    fun `sync passes effective existing apps to layout after removals and updates`() = runTest {
        val store = FakeCanvasAppsStore(
            listOf(
                CanvasApp("remove.me", "Remove", WorldPoint(0f, 0f)),
                CanvasApp("keep.me", "Old Label", WorldPoint(5f, 5f)),
            ),
        )
        val strategy = FakeLayoutStrategy()
        val useCase = SyncAppsWithSystemUseCase(
            installedAppsSource = FakeInstalledAppsSource(
                listOf(
                    InstalledApp("keep.me", "New Label"),
                    InstalledApp("new.app", "New App"),
                ),
            ),
            appsStore = store,
            layoutStrategy = strategy,
            iconCacheGateway = FakeIconCacheGateway(),
            layoutPreferencesRepository = FakeLayoutPreferencesRepository(AppLayoutMode.OVAL),
        )

        useCase(centerForNewApps = WorldPoint(9f, 9f))

        val call = strategy.calls.single()
        assertThat(call.existingApps.map { it.packageName }).containsExactly("keep.me")
        assertThat(call.existingApps.single().label).isEqualTo("New Label")
        assertThat(call.newApps.map { it.packageName }).containsExactly("new.app")
        assertThat(call.center).isEqualTo(WorldPoint(9f, 9f))
        assertThat(call.mode).isEqualTo(AppLayoutMode.OVAL)
    }

    @Test
    fun `sync reports zeroes when data already aligned`() = runTest {
        val useCase = SyncAppsWithSystemUseCase(
            installedAppsSource = FakeInstalledAppsSource(listOf(InstalledApp("pkg.one", "One"))),
            appsStore = FakeCanvasAppsStore(listOf(CanvasApp("pkg.one", "One", WorldPoint(0f, 0f)))),
            layoutStrategy = FakeLayoutStrategy(),
            iconCacheGateway = FakeIconCacheGateway(),
            layoutPreferencesRepository = FakeLayoutPreferencesRepository(),
        )

        val report = useCase()

        assertThat(report.added).isEqualTo(0)
        assertThat(report.removed).isEqualTo(0)
        assertThat(report.updated).isEqualTo(0)
    }
}
