package com.darksok.canvaslauncher.domain.usecase

import com.darksok.canvaslauncher.core.common.result.AppError
import com.darksok.canvaslauncher.core.common.result.AppResult
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.domain.support.FakeAppLaunchService
import com.darksok.canvaslauncher.domain.support.FakeCanvasAppsStore
import com.darksok.canvaslauncher.domain.support.FakeLayoutPreferencesRepository
import com.darksok.canvaslauncher.domain.support.FakeLayoutStrategy
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LayoutAndLaunchUseCasesTest {

    @Test
    fun `launch app use case delegates to service`() = runTest {
        val service = FakeAppLaunchService(AppResult.Success(Unit))
        val useCase = LaunchAppUseCase(service)

        val result = useCase("pkg.launch")

        assertThat(result).isEqualTo(AppResult.Success(Unit))
        assertThat(service.calls).containsExactly("pkg.launch")
    }

    @Test
    fun `launch app use case propagates failure`() = runTest {
        val service = FakeAppLaunchService(AppResult.Failure(AppError.LaunchUnavailable))
        val useCase = LaunchAppUseCase(service)

        val result = useCase("pkg.launch")

        assertThat(result).isEqualTo(AppResult.Failure(AppError.LaunchUnavailable))
    }

    @Test
    fun `observe apps use case exposes repository flow`() = runTest {
        val store = FakeCanvasAppsStore(listOf(CanvasApp("pkg.one", "One", WorldPoint(1f, 2f))))
        val useCase = ObserveAppsUseCase(store)

        val result = useCase().first()

        assertThat(result.map { it.packageName }).containsExactly("pkg.one")
    }

    @Test
    fun `observe layout mode use case exposes repository flow`() = runTest {
        val repository = FakeLayoutPreferencesRepository(AppLayoutMode.OVAL)
        val useCase = ObserveLayoutModeUseCase(repository)

        assertThat(useCase().first()).isEqualTo(AppLayoutMode.OVAL)
    }

    @Test
    fun `set layout mode use case delegates to repository`() = runTest {
        val repository = FakeLayoutPreferencesRepository(AppLayoutMode.SPIRAL)
        val useCase = SetLayoutModeUseCase(repository)

        useCase(AppLayoutMode.CIRCLE)

        assertThat(repository.setCalls).containsExactly(AppLayoutMode.CIRCLE)
        assertThat(repository.observeLayoutMode().first()).isEqualTo(AppLayoutMode.CIRCLE)
    }

    @Test
    fun `update app position use case delegates to store`() = runTest {
        val store = FakeCanvasAppsStore(listOf(CanvasApp("pkg.one", "One", WorldPoint(1f, 2f))))
        val useCase = UpdateAppPositionUseCase(store)

        useCase("pkg.one", WorldPoint(9f, 8f))

        assertThat(store.updatePositionCalls).containsExactly("pkg.one" to WorldPoint(9f, 8f))
        assertThat(store.snapshot().single().position).isEqualTo(WorldPoint(9f, 8f))
    }

    @Test
    fun `rearrange apps use case returns zero for empty store`() = runTest {
        val useCase = RearrangeAppsUseCase(FakeCanvasAppsStore(), FakeLayoutStrategy())

        val count = useCase(AppLayoutMode.RECTANGLE)

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `rearrange apps use case sorts labels before delegating`() = runTest {
        val store = FakeCanvasAppsStore(
            listOf(
                CanvasApp("pkg.z", "Zulu", WorldPoint(0f, 0f)),
                CanvasApp("pkg.a", "Alpha", WorldPoint(1f, 1f)),
            ),
        )
        val strategy = FakeLayoutStrategy()
        val useCase = RearrangeAppsUseCase(store, strategy)

        useCase(AppLayoutMode.RECTANGLE)

        assertThat(strategy.calls.single().newApps.map { it.label }).containsExactly("Alpha", "Zulu").inOrder()
    }

    @Test
    fun `rearrange apps use case passes center and mode`() = runTest {
        val store = FakeCanvasAppsStore(listOf(CanvasApp("pkg.a", "Alpha", WorldPoint(0f, 0f))))
        val strategy = FakeLayoutStrategy()
        val useCase = RearrangeAppsUseCase(store, strategy)

        useCase(AppLayoutMode.CIRCLE, WorldPoint(30f, 40f))

        val call = strategy.calls.single()
        assertThat(call.mode).isEqualTo(AppLayoutMode.CIRCLE)
        assertThat(call.center).isEqualTo(WorldPoint(30f, 40f))
    }

    @Test
    fun `rearrange apps use case writes rearranged apps back`() = runTest {
        val store = FakeCanvasAppsStore(listOf(CanvasApp("pkg.a", "Alpha", WorldPoint(0f, 0f))))
        val strategy = FakeLayoutStrategy { _, newApps, center, _ ->
            newApps.map { app -> CanvasApp(app.packageName, app.label, WorldPoint(center.x + 5f, center.y + 6f)) }
        }
        val useCase = RearrangeAppsUseCase(store, strategy)

        val count = useCase(AppLayoutMode.RECTANGLE, WorldPoint(1f, 2f))

        assertThat(count).isEqualTo(1)
        assertThat(store.snapshot().single().position).isEqualTo(WorldPoint(6f, 8f))
    }
}
