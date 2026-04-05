package com.darksok.canvaslauncher.settings

import android.content.ContextWrapper
import com.darksok.canvaslauncher.core.common.coroutines.DispatchersProvider
import com.darksok.canvaslauncher.core.database.dao.CanvasEditDao
import com.darksok.canvaslauncher.core.database.dao.CanvasStrokeWithPointsEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasFrameObjectEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStickyNoteEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokeEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokePointEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasTextObjectEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasWidgetEntity
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.core.packages.icon.IconBitmapStore
import com.darksok.canvaslauncher.domain.layout.InitialLayoutStrategy
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import com.darksok.canvaslauncher.domain.repository.LayoutPreferencesRepository
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import com.darksok.canvaslauncher.domain.usecase.ObserveDarkThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveLayoutModeUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveLightThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveThemeModeUseCase
import com.darksok.canvaslauncher.domain.usecase.RearrangeAppsUseCase
import com.darksok.canvaslauncher.domain.usecase.SetDarkThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.SetLayoutModeUseCase
import com.darksok.canvaslauncher.domain.usecase.SetLightThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.SetThemeModeUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Test
    fun `ui state reflects stored values`() = runTestWithMain {
        val dependencies = createDependencies(
            initialThemeMode = ThemeMode.DARK,
            initialLightPalette = LightThemePalette.ROSE_DAWN,
            initialDarkPalette = DarkThemePalette.CHARCOAL_AMBER,
            initialLayoutMode = AppLayoutMode.CIRCLE,
        )

        val viewModel = dependencies.createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            SettingsUiState(
                themeMode = ThemeMode.DARK,
                lightPalette = LightThemePalette.ROSE_DAWN,
                darkPalette = DarkThemePalette.CHARCOAL_AMBER,
                layoutMode = AppLayoutMode.CIRCLE,
            ),
        )
        collector.cancel()
        advanceUntilIdle()
    }

    @Test
    fun `theme selection is persisted through use case`() = runTestWithMain {
        val dependencies = createDependencies()
        val viewModel = dependencies.createViewModel()

        viewModel.onThemeModeSelected(ThemeMode.LIGHT)
        advanceUntilIdle()

        assertThat(dependencies.themeRepository.state.value).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `light palette selection is persisted through use case`() = runTestWithMain {
        val dependencies = createDependencies()
        val viewModel = dependencies.createViewModel()

        viewModel.onLightPaletteSelected(LightThemePalette.MINT_GARDEN)
        advanceUntilIdle()

        assertThat(dependencies.themeRepository.lightState.value).isEqualTo(LightThemePalette.MINT_GARDEN)
    }

    @Test
    fun `dark palette selection is persisted through use case`() = runTestWithMain {
        val dependencies = createDependencies()
        val viewModel = dependencies.createViewModel()

        viewModel.onDarkPaletteSelected(DarkThemePalette.FOREST_NIGHT)
        advanceUntilIdle()

        assertThat(dependencies.themeRepository.darkState.value).isEqualTo(DarkThemePalette.FOREST_NIGHT)
    }

    @Test
    fun `layout mode selection is persisted`() = runTestWithMain {
        val dependencies = createDependencies()
        val viewModel = dependencies.createViewModel()

        viewModel.onLayoutModeSelected(AppLayoutMode.CIRCLE)
        advanceUntilIdle()

        assertThat(dependencies.layoutRepository.state.value).isEqualTo(AppLayoutMode.CIRCLE)
    }

    @Test
    fun `non icon layout rearranges apps and clears auto layout frames`() = runTestWithMain {
        val dependencies = createDependencies(
            snapshot = listOf(
                CanvasApp("pkg.alpha", "Alpha", WorldPoint(0f, 0f)),
                CanvasApp("pkg.beta", "Beta", WorldPoint(10f, 20f)),
            ),
            existingFrames = listOf(
                CanvasFrameObjectEntity("auto-layout-frame-old", "Old", 0f, 0f, 100f, 100f, 0),
                CanvasFrameObjectEntity("manual-frame", "Manual", 0f, 0f, 100f, 100f, 0),
            ),
        )
        val viewModel = dependencies.createViewModel()

        viewModel.onLayoutModeSelected(AppLayoutMode.CIRCLE)
        advanceUntilIdle()

        assertThat(dependencies.layoutStrategy.lastMode).isEqualTo(AppLayoutMode.CIRCLE)
        assertThat(dependencies.appsStore.lastUpsertedApps).hasSize(2)
        assertThat(dependencies.canvasEditDao.deletedFrameIds).contains("auto-layout-frame-old")
        assertThat(dependencies.canvasEditDao.deletedFrameIds).doesNotContain("manual-frame")
    }

    @Test
    fun `smart auto with empty snapshot clears existing auto frames`() = runTestWithMain {
        val dependencies = createDependencies(
            snapshot = emptyList(),
            existingFrames = listOf(
                CanvasFrameObjectEntity("auto-layout-frame-smart", "Old", 0f, 0f, 100f, 100f, 0),
            ),
        )
        val viewModel = dependencies.createViewModel()

        viewModel.onLayoutModeSelected(AppLayoutMode.SMART_AUTO)
        advanceUntilIdle()

        assertThat(dependencies.canvasEditDao.deletedFrameIds).containsExactly("auto-layout-frame-smart")
        assertThat(dependencies.canvasEditDao.upsertedFrames).isEmpty()
    }

    @Test
    fun `icon color with empty snapshot does not upsert apps`() = runTestWithMain {
        val dependencies = createDependencies(snapshot = emptyList())
        val viewModel = dependencies.createViewModel()

        viewModel.onLayoutModeSelected(AppLayoutMode.ICON_COLOR)
        advanceUntilIdle()

        assertThat(dependencies.layoutRepository.state.value).isEqualTo(AppLayoutMode.ICON_COLOR)
        assertThat(dependencies.appsStore.lastUpsertedApps).isEmpty()
    }

    @Test
    fun `ui state reacts to repository updates after creation`() = runTestWithMain {
        val dependencies = createDependencies()
        val viewModel = dependencies.createViewModel()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        dependencies.themeRepository.state.value = ThemeMode.DARK
        dependencies.themeRepository.lightState.value = LightThemePalette.SUNSET_GLOW
        dependencies.themeRepository.darkState.value = DarkThemePalette.DEEP_OCEAN
        dependencies.layoutRepository.state.value = AppLayoutMode.CIRCLE
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(
            SettingsUiState(
                themeMode = ThemeMode.DARK,
                lightPalette = LightThemePalette.SUNSET_GLOW,
                darkPalette = DarkThemePalette.DEEP_OCEAN,
                layoutMode = AppLayoutMode.CIRCLE,
            ),
        )
        collector.cancel()
        advanceUntilIdle()
    }

    private fun TestScope.createDependencies(
        initialThemeMode: ThemeMode = ThemeMode.SYSTEM,
        initialLightPalette: LightThemePalette = LightThemePalette.SKY_BREEZE,
        initialDarkPalette: DarkThemePalette = DarkThemePalette.MIDNIGHT_BLUE,
        initialLayoutMode: AppLayoutMode = AppLayoutMode.SPIRAL,
        snapshot: List<CanvasApp> = emptyList(),
        existingFrames: List<CanvasFrameObjectEntity> = emptyList(),
    ): TestDependencies {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val themeRepository = FakeThemePreferencesRepository(
            initialThemeMode = initialThemeMode,
            initialLightPalette = initialLightPalette,
            initialDarkPalette = initialDarkPalette,
        )
        val layoutRepository = FakeLayoutPreferencesRepository(initialLayoutMode)
        val appsStore = FakeCanvasAppsStore(snapshot)
        val canvasEditDao = FakeCanvasEditDao(existingFrames)
        val layoutStrategy = FakeLayoutStrategy()
        return TestDependencies(
            themeRepository = themeRepository,
            layoutRepository = layoutRepository,
            appsStore = appsStore,
            canvasEditDao = canvasEditDao,
            layoutStrategy = layoutStrategy,
            dispatcher = dispatcher,
        )
    }

    private fun runTestWithMain(block: suspend TestScope.() -> Unit) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private data class TestDependencies(
        val themeRepository: FakeThemePreferencesRepository,
        val layoutRepository: FakeLayoutPreferencesRepository,
        val appsStore: FakeCanvasAppsStore,
        val canvasEditDao: FakeCanvasEditDao,
        val layoutStrategy: FakeLayoutStrategy,
        val dispatcher: CoroutineDispatcher,
    ) {
        fun createViewModel(): SettingsViewModel {
            val rearrangeUseCase = RearrangeAppsUseCase(
                appsStore = appsStore,
                layoutStrategy = layoutStrategy,
            )
            return SettingsViewModel(
                observeThemeModeUseCase = ObserveThemeModeUseCase(themeRepository),
                observeLightThemePaletteUseCase = ObserveLightThemePaletteUseCase(themeRepository),
                observeDarkThemePaletteUseCase = ObserveDarkThemePaletteUseCase(themeRepository),
                observeLayoutModeUseCase = ObserveLayoutModeUseCase(layoutRepository),
                setThemeModeUseCase = SetThemeModeUseCase(themeRepository),
                setLightThemePaletteUseCase = SetLightThemePaletteUseCase(themeRepository),
                setDarkThemePaletteUseCase = SetDarkThemePaletteUseCase(themeRepository),
                setLayoutModeUseCase = SetLayoutModeUseCase(layoutRepository),
                rearrangeAppsUseCase = rearrangeUseCase,
                appsStore = appsStore,
                canvasEditDao = canvasEditDao,
                iconCacheGateway = FakeIconCacheGateway(),
                iconBitmapStore = FakeIconBitmapStore(),
                dispatchersProvider = object : DispatchersProvider {
                    override val io: CoroutineDispatcher = dispatcher
                    override val default: CoroutineDispatcher = dispatcher
                    override val main: CoroutineDispatcher = dispatcher
                },
                appContext = ContextWrapper(null),
            )
        }
    }

    private class FakeThemePreferencesRepository(
        initialThemeMode: ThemeMode,
        initialLightPalette: LightThemePalette,
        initialDarkPalette: DarkThemePalette,
    ) : ThemePreferencesRepository {

        val state = MutableStateFlow(initialThemeMode)
        val lightState = MutableStateFlow(initialLightPalette)
        val darkState = MutableStateFlow(initialDarkPalette)

        override fun observeThemeMode(): Flow<ThemeMode> = state
        override fun observeLightThemePalette(): Flow<LightThemePalette> = lightState
        override fun observeDarkThemePalette(): Flow<DarkThemePalette> = darkState

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            state.value = themeMode
        }

        override suspend fun setLightThemePalette(palette: LightThemePalette) {
            lightState.value = palette
        }

        override suspend fun setDarkThemePalette(palette: DarkThemePalette) {
            darkState.value = palette
        }
    }

    private class FakeLayoutPreferencesRepository(
        initial: AppLayoutMode,
    ) : LayoutPreferencesRepository {
        val state = MutableStateFlow(initial)

        override fun observeLayoutMode(): Flow<AppLayoutMode> = state

        override suspend fun setLayoutMode(layoutMode: AppLayoutMode) {
            state.value = layoutMode
        }
    }

    private class FakeCanvasAppsStore(snapshot: List<CanvasApp>) : CanvasAppsStore {
        private val snapshotState = MutableStateFlow(snapshot)
        var lastUpsertedApps: List<CanvasApp> = emptyList()

        override fun observeApps(): Flow<List<CanvasApp>> = flowOf(emptyList())

        override suspend fun getAppsSnapshot(): List<CanvasApp> = snapshotState.value

        override suspend fun upsertApps(apps: List<CanvasApp>) {
            lastUpsertedApps = apps
            snapshotState.value = apps
        }

        override suspend fun upsertApp(app: CanvasApp) = Unit

        override suspend fun removePackages(packages: Set<String>) = Unit

        override suspend fun removePackage(packageName: String) = Unit

        override suspend fun updatePosition(packageName: String, position: WorldPoint) = Unit
    }

    private class FakeLayoutStrategy : InitialLayoutStrategy {
        var lastMode: AppLayoutMode? = null

        override fun layout(
            existingApps: List<CanvasApp>,
            newApps: List<InstalledApp>,
            center: WorldPoint,
            mode: AppLayoutMode,
        ): List<CanvasApp> {
            lastMode = mode
            return newApps.mapIndexed { index, app ->
                CanvasApp(
                    packageName = app.packageName,
                    label = app.label,
                    position = WorldPoint(center.x + index * 50f, center.y + index * 25f),
                )
            }
        }
    }

    private class FakeCanvasEditDao(existingFrames: List<CanvasFrameObjectEntity>) : CanvasEditDao {
        private val frames = existingFrames.toMutableList()
        val deletedFrameIds = mutableListOf<String>()
        val upsertedFrames = mutableListOf<CanvasFrameObjectEntity>()

        override suspend fun getStickyNotes(): List<CanvasStickyNoteEntity> = emptyList()
        override suspend fun getTextObjects(): List<CanvasTextObjectEntity> = emptyList()
        override suspend fun getFrameObjects(): List<CanvasFrameObjectEntity> = frames.toList()
        override suspend fun getWidgets(): List<CanvasWidgetEntity> = emptyList()
        override suspend fun getStrokesWithPoints(): List<CanvasStrokeWithPointsEntity> = emptyList()
        override suspend fun upsertStickyNote(note: CanvasStickyNoteEntity) = Unit
        override suspend fun upsertTextObject(textObject: CanvasTextObjectEntity) = Unit
        override suspend fun upsertFrameObject(frameObject: CanvasFrameObjectEntity) {
            upsertedFrames += frameObject
        }
        override suspend fun upsertWidget(widget: CanvasWidgetEntity) = Unit
        override suspend fun upsertStroke(stroke: CanvasStrokeEntity) = Unit
        override suspend fun insertStrokePoints(points: List<CanvasStrokePointEntity>) = Unit
        override suspend fun deleteStrokePointsByStrokeId(strokeId: String) = Unit
        override suspend fun deleteStrokeById(strokeId: String) = Unit
        override suspend fun deleteStickyNoteById(id: String) = Unit
        override suspend fun deleteTextObjectById(id: String) = Unit
        override suspend fun deleteFrameObjectById(id: String) {
            deletedFrameIds += id
        }
        override suspend fun deleteWidgetById(id: String) = Unit
        override suspend fun deleteAllStrokes() = Unit
        override suspend fun deleteAllStickyNotes() = Unit
        override suspend fun deleteAllTextObjects() = Unit
        override suspend fun deleteAllFrameObjects() = Unit
        override suspend fun deleteAllWidgets() = Unit
    }

    private class FakeIconCacheGateway : IconCacheGateway {
        override suspend fun preload(packageNames: Collection<String>) = Unit
        override suspend fun invalidate(packageName: String) = Unit
        override suspend fun remove(packageName: String) = Unit
    }

    private class FakeIconBitmapStore : IconBitmapStore {
        override val icons = MutableStateFlow<Map<String, android.graphics.Bitmap>>(emptyMap())
        override fun getCached(packageName: String): android.graphics.Bitmap? = null
    }
}
