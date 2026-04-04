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
import com.darksok.canvaslauncher.domain.layout.InitialLayoutStrategy
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import com.darksok.canvaslauncher.domain.repository.LayoutPreferencesRepository
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import com.darksok.canvaslauncher.domain.usecase.ObserveDarkThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveLayoutModeUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveLightThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.RearrangeAppsUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveThemeModeUseCase
import com.darksok.canvaslauncher.domain.usecase.SetDarkThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.SetLayoutModeUseCase
import com.darksok.canvaslauncher.domain.usecase.SetLightThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.SetThemeModeUseCase
import com.darksok.canvaslauncher.core.packages.icon.IconBitmapStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Test
    fun `ui state reflects stored theme mode`() = runTest {
        val themeRepository = FakeThemePreferencesRepository(initial = ThemeMode.SYSTEM)
        val layoutRepository = FakeLayoutPreferencesRepository(initial = AppLayoutMode.SPIRAL)
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = createViewModel(themeRepository, layoutRepository, dispatcher)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.themeMode).isEqualTo(ThemeMode.SYSTEM)
            assertThat(viewModel.uiState.value.lightPalette).isEqualTo(LightThemePalette.SKY_BREEZE)
            assertThat(viewModel.uiState.value.darkPalette).isEqualTo(DarkThemePalette.MIDNIGHT_BLUE)
            assertThat(viewModel.uiState.value.layoutMode).isEqualTo(AppLayoutMode.SPIRAL)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `theme selection is persisted through use case`() = runTest {
        val themeRepository = FakeThemePreferencesRepository(initial = ThemeMode.SYSTEM)
        val layoutRepository = FakeLayoutPreferencesRepository(initial = AppLayoutMode.SPIRAL)
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = createViewModel(themeRepository, layoutRepository, dispatcher)
            viewModel.onThemeModeSelected(ThemeMode.LIGHT)
            advanceUntilIdle()
            assertThat(themeRepository.observeThemeMode().first()).isEqualTo(ThemeMode.LIGHT)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `layout mode selection is persisted`() = runTest {
        val themeRepository = FakeThemePreferencesRepository(initial = ThemeMode.SYSTEM)
        val layoutRepository = FakeLayoutPreferencesRepository(initial = AppLayoutMode.SPIRAL)
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = createViewModel(themeRepository, layoutRepository, dispatcher)
            viewModel.onLayoutModeSelected(AppLayoutMode.CIRCLE)
            advanceUntilIdle()
            assertThat(layoutRepository.observeLayoutMode().first()).isEqualTo(AppLayoutMode.CIRCLE)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `palette selection is persisted`() = runTest {
        val themeRepository = FakeThemePreferencesRepository(initial = ThemeMode.SYSTEM)
        val layoutRepository = FakeLayoutPreferencesRepository(initial = AppLayoutMode.SPIRAL)
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = createViewModel(themeRepository, layoutRepository, dispatcher)
            viewModel.onLightPaletteSelected(LightThemePalette.ROSE_DAWN)
            viewModel.onDarkPaletteSelected(DarkThemePalette.CHARCOAL_AMBER)
            advanceUntilIdle()
            assertThat(themeRepository.observeLightThemePalette().first()).isEqualTo(LightThemePalette.ROSE_DAWN)
            assertThat(themeRepository.observeDarkThemePalette().first()).isEqualTo(DarkThemePalette.CHARCOAL_AMBER)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(
        themeRepository: ThemePreferencesRepository,
        layoutRepository: LayoutPreferencesRepository,
        dispatcher: CoroutineDispatcher,
    ): SettingsViewModel {
        val rearrangeUseCase = RearrangeAppsUseCase(
            appsStore = FakeCanvasAppsStore(),
            layoutStrategy = FakeLayoutStrategy(),
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
            appsStore = FakeCanvasAppsStore(),
            canvasEditDao = FakeCanvasEditDao(),
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

    private class FakeThemePreferencesRepository(
        initial: ThemeMode,
    ) : ThemePreferencesRepository {

        private val state = MutableStateFlow(initial)
        private val lightState = MutableStateFlow(LightThemePalette.SKY_BREEZE)
        private val darkState = MutableStateFlow(DarkThemePalette.MIDNIGHT_BLUE)

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

        private val state = MutableStateFlow(initial)

        override fun observeLayoutMode(): Flow<AppLayoutMode> = state

        override suspend fun setLayoutMode(layoutMode: AppLayoutMode) {
            state.value = layoutMode
        }
    }

    private class FakeCanvasAppsStore : CanvasAppsStore {
        override fun observeApps(): Flow<List<CanvasApp>> = flowOf(emptyList())
        override suspend fun getAppsSnapshot(): List<CanvasApp> = emptyList()
        override suspend fun upsertApps(apps: List<CanvasApp>) = Unit
        override suspend fun upsertApp(app: CanvasApp) = Unit
        override suspend fun removePackages(packages: Set<String>) = Unit
        override suspend fun removePackage(packageName: String) = Unit
        override suspend fun updatePosition(packageName: String, position: WorldPoint) = Unit
    }

    private class FakeLayoutStrategy : InitialLayoutStrategy {
        override fun layout(
            existingApps: List<CanvasApp>,
            newApps: List<InstalledApp>,
            center: WorldPoint,
            mode: AppLayoutMode,
        ): List<CanvasApp> {
            return newApps.map { app ->
                CanvasApp(
                    packageName = app.packageName,
                    label = app.label,
                    position = center,
                )
            }
        }
    }

    private class FakeCanvasEditDao : CanvasEditDao {
        override suspend fun getStickyNotes(): List<CanvasStickyNoteEntity> = emptyList()
        override suspend fun getTextObjects(): List<CanvasTextObjectEntity> = emptyList()
        override suspend fun getFrameObjects(): List<CanvasFrameObjectEntity> = emptyList()
        override suspend fun getWidgets(): List<CanvasWidgetEntity> = emptyList()
        override suspend fun getStrokesWithPoints(): List<CanvasStrokeWithPointsEntity> = emptyList()
        override suspend fun upsertStickyNote(note: CanvasStickyNoteEntity) = Unit
        override suspend fun upsertTextObject(textObject: CanvasTextObjectEntity) = Unit
        override suspend fun upsertFrameObject(frameObject: CanvasFrameObjectEntity) = Unit
        override suspend fun upsertWidget(widget: CanvasWidgetEntity) = Unit
        override suspend fun upsertStroke(stroke: CanvasStrokeEntity) = Unit
        override suspend fun insertStrokePoints(points: List<CanvasStrokePointEntity>) = Unit
        override suspend fun deleteStrokePointsByStrokeId(strokeId: String) = Unit
        override suspend fun deleteStrokeById(strokeId: String) = Unit
        override suspend fun deleteStickyNoteById(id: String) = Unit
        override suspend fun deleteTextObjectById(id: String) = Unit
        override suspend fun deleteFrameObjectById(id: String) = Unit
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
