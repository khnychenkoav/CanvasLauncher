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

class PackageMutationUseCasesTest {

    @Test
    fun `handle package added ignores missing system app`() = runTest {
        val store = FakeCanvasAppsStore()
        val useCase = HandlePackageAddedUseCase(
            installedAppsSource = FakeInstalledAppsSource(),
            appsStore = store,
            iconCacheGateway = FakeIconCacheGateway(),
            layoutStrategy = FakeLayoutStrategy(),
            layoutPreferencesRepository = FakeLayoutPreferencesRepository(),
        )

        useCase("missing", WorldPoint(0f, 0f))

        assertThat(store.snapshot()).isEmpty()
    }

    @Test
    fun `handle package added reuses existing position`() = runTest {
        val existing = CanvasApp("pkg.one", "Old", WorldPoint(7f, 9f))
        val store = FakeCanvasAppsStore(listOf(existing))
        val icons = FakeIconCacheGateway()
        val strategy = FakeLayoutStrategy()
        val useCase = HandlePackageAddedUseCase(
            installedAppsSource = FakeInstalledAppsSource(listOf(InstalledApp("pkg.one", "New"))),
            appsStore = store,
            iconCacheGateway = icons,
            layoutStrategy = strategy,
            layoutPreferencesRepository = FakeLayoutPreferencesRepository(),
        )

        useCase("pkg.one", WorldPoint(50f, 60f))

        assertThat(store.snapshot().single().position).isEqualTo(WorldPoint(7f, 9f))
        assertThat(store.snapshot().single().label).isEqualTo("New")
        assertThat(strategy.calls).isEmpty()
        assertThat(icons.preloaded.last()).containsExactly("pkg.one")
    }

    @Test
    fun `handle package added asks layout strategy for new app`() = runTest {
        val strategy = FakeLayoutStrategy()
        val useCase = HandlePackageAddedUseCase(
            installedAppsSource = FakeInstalledAppsSource(listOf(InstalledApp("pkg.one", "One"))),
            appsStore = FakeCanvasAppsStore(),
            iconCacheGateway = FakeIconCacheGateway(),
            layoutStrategy = strategy,
            layoutPreferencesRepository = FakeLayoutPreferencesRepository(AppLayoutMode.CIRCLE),
        )

        useCase("pkg.one", WorldPoint(10f, 20f))

        assertThat(strategy.calls).hasSize(1)
        assertThat(strategy.calls.single().mode).isEqualTo(AppLayoutMode.CIRCLE)
        assertThat(strategy.calls.single().center).isEqualTo(WorldPoint(10f, 20f))
    }

    @Test
    fun `handle package added writes positioned app to store`() = runTest {
        val store = FakeCanvasAppsStore()
        val useCase = HandlePackageAddedUseCase(
            installedAppsSource = FakeInstalledAppsSource(listOf(InstalledApp("pkg.one", "One"))),
            appsStore = store,
            iconCacheGateway = FakeIconCacheGateway(),
            layoutStrategy = FakeLayoutStrategy(),
            layoutPreferencesRepository = FakeLayoutPreferencesRepository(),
        )

        useCase("pkg.one", WorldPoint(3f, 4f))

        assertThat(store.snapshot()).hasSize(1)
        assertThat(store.snapshot().single().packageName).isEqualTo("pkg.one")
    }

    @Test
    fun `handle package changed ignores missing system app`() = runTest {
        val store = FakeCanvasAppsStore(listOf(CanvasApp("pkg.one", "One", WorldPoint(0f, 0f))))
        val icons = FakeIconCacheGateway()
        val useCase = HandlePackageChangedUseCase(FakeInstalledAppsSource(), store, icons)

        useCase("pkg.one")

        assertThat(store.snapshot().single().label).isEqualTo("One")
        assertThat(icons.invalidated).isEmpty()
        assertThat(icons.preloaded).isEmpty()
    }

    @Test
    fun `handle package changed ignores package absent in local store`() = runTest {
        val icons = FakeIconCacheGateway()
        val useCase = HandlePackageChangedUseCase(
            FakeInstalledAppsSource(listOf(InstalledApp("pkg.one", "One"))),
            FakeCanvasAppsStore(),
            icons,
        )

        useCase("pkg.one")

        assertThat(icons.invalidated).isEmpty()
        assertThat(icons.preloaded).isEmpty()
    }

    @Test
    fun `handle package changed updates label invalidates and preloads icon`() = runTest {
        val store = FakeCanvasAppsStore(listOf(CanvasApp("pkg.one", "Old", WorldPoint(0f, 0f))))
        val icons = FakeIconCacheGateway()
        val useCase = HandlePackageChangedUseCase(
            FakeInstalledAppsSource(listOf(InstalledApp("pkg.one", "New"))),
            store,
            icons,
        )

        useCase("pkg.one")

        assertThat(store.snapshot().single().label).isEqualTo("New")
        assertThat(icons.invalidated).containsExactly("pkg.one")
        assertThat(icons.preloaded.last()).containsExactly("pkg.one")
    }

    @Test
    fun `handle package removed deletes package and icon`() = runTest {
        val store = FakeCanvasAppsStore(listOf(CanvasApp("pkg.one", "One", WorldPoint(0f, 0f))))
        val icons = FakeIconCacheGateway()
        val useCase = HandlePackageRemovedUseCase(store, icons)

        useCase("pkg.one")

        assertThat(store.snapshot()).isEmpty()
        assertThat(store.removedPackageCalls).containsExactly("pkg.one")
        assertThat(icons.removed).containsExactly("pkg.one")
    }

    @Test
    fun `handle package removed still invalidates icon path for unknown package`() = runTest {
        val store = FakeCanvasAppsStore()
        val icons = FakeIconCacheGateway()
        val useCase = HandlePackageRemovedUseCase(store, icons)

        useCase("unknown")

        assertThat(store.removedPackageCalls).containsExactly("unknown")
        assertThat(icons.removed).containsExactly("unknown")
    }
}
