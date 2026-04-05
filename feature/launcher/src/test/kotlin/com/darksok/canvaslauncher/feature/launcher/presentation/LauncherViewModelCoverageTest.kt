package com.darksok.canvaslauncher.feature.launcher.presentation

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.darksok.canvaslauncher.core.common.coroutines.DispatchersProvider
import com.darksok.canvaslauncher.core.common.result.AppError
import com.darksok.canvaslauncher.core.common.result.AppResult
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
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.core.packages.events.InMemoryPackageEventsBus
import com.darksok.canvaslauncher.core.packages.events.PackageEvent
import com.darksok.canvaslauncher.core.packages.icon.IconBitmapStore
import com.darksok.canvaslauncher.domain.layout.InitialLayoutStrategy
import com.darksok.canvaslauncher.domain.repository.AppLaunchService
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import com.darksok.canvaslauncher.domain.repository.InstalledAppsSource
import com.darksok.canvaslauncher.domain.repository.LayoutPreferencesRepository
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import com.darksok.canvaslauncher.domain.usecase.HandlePackageAddedUseCase
import com.darksok.canvaslauncher.domain.usecase.HandlePackageChangedUseCase
import com.darksok.canvaslauncher.domain.usecase.HandlePackageRemovedUseCase
import com.darksok.canvaslauncher.domain.usecase.LaunchAppUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveAppsUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveDarkThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveLightThemePaletteUseCase
import com.darksok.canvaslauncher.domain.usecase.ObserveThemeModeUseCase
import com.darksok.canvaslauncher.domain.usecase.SyncAppsWithSystemUseCase
import com.darksok.canvaslauncher.domain.usecase.UpdateAppPositionUseCase
import com.darksok.canvaslauncher.feature.canvas.DefaultCanvasGestureHandler
import com.darksok.canvaslauncher.feature.canvas.DefaultDragDropController
import com.darksok.canvaslauncher.feature.canvas.DefaultViewportController
import com.darksok.canvaslauncher.feature.launcher.R
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LauncherViewModelCoverageTest {

    @Test
    fun `edit flow creates objects, deletes selection and restores by undo`() = runTestWithMain {
        val harness = createHarness()
        val viewModel = harness.viewModel
        val uiCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.onViewportSizeChanged(widthPx = 1080, heightPx = 1920)
        viewModel.onToolSelected(LauncherToolId.Edit)
        advanceUntilIdle()

        viewModel.onEditToolSelected(CanvasEditToolId.StickyNote)
        viewModel.onEditCanvasTap(WorldPoint(20f, 30f))
        advanceUntilIdle()
        viewModel.onEditInlineEditorValueChanged("Sticky A")
        viewModel.onEditInlineEditorConfirm()
        advanceUntilIdle()

        viewModel.onEditToolSelected(CanvasEditToolId.Text)
        viewModel.onEditCanvasTap(WorldPoint(-40f, 16f))
        advanceUntilIdle()
        viewModel.onEditInlineEditorValueChanged("Text A")
        viewModel.onEditInlineEditorConfirm()
        advanceUntilIdle()

        viewModel.onEditToolSelected(CanvasEditToolId.Brush)
        viewModel.onEditBrushStart(WorldPoint(-18f, -18f))
        viewModel.onEditBrushPoint(WorldPoint(22f, 20f))
        viewModel.onEditBrushEnd()
        advanceUntilIdle()

        val beforeDelete = viewModel.uiState.value
        assertThat(beforeDelete.stickyNotes).hasSize(1)
        assertThat(beforeDelete.textObjects).hasSize(1)
        assertThat(beforeDelete.strokes).hasSize(1)

        viewModel.onEditToolSelected(CanvasEditToolId.Selection)
        viewModel.onEditSelectionDragStart(WorldPoint(-150f, -150f))
        viewModel.onEditSelectionDragUpdate(WorldPoint(150f, 150f))
        viewModel.onEditSelectionDragEnd()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.hasActiveSelection).isTrue()

        viewModel.onEditSelectionDeleteTap()
        advanceUntilIdle()
        val afterDelete = viewModel.uiState.value
        assertThat(afterDelete.stickyNotes).isEmpty()
        assertThat(afterDelete.textObjects).isEmpty()
        assertThat(afterDelete.strokes).isEmpty()

        viewModel.onEditUndo()
        advanceUntilIdle()
        val afterUndo = viewModel.uiState.value
        assertThat(afterUndo.stickyNotes).hasSize(1)
        assertThat(afterUndo.textObjects).hasSize(1)
        assertThat(afterUndo.strokes).hasSize(1)
        uiCollector.cancel()
    }

    @Test
    fun `widgets flow supports add resize drag and delete`() = runTestWithMain {
        val harness = createHarness()
        val viewModel = harness.viewModel
        val uiCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.onViewportSizeChanged(widthPx = 1080, heightPx = 1920)
        viewModel.onToolSelected(LauncherToolId.Widgets)
        advanceUntilIdle()

        viewModel.onWidgetCatalogItemSelected(CanvasWidgetType.ClockDigital)
        viewModel.onWidgetCatalogItemSelected(CanvasWidgetType.ClockAnalog)
        viewModel.onWidgetCatalogItemSelected(CanvasWidgetType.Weather)
        viewModel.onWidgetCatalogItemSelected(CanvasWidgetType.Notifications)
        viewModel.onWidgetCatalogItemSelected(CanvasWidgetType.Calendar)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.widgets).hasSize(5)
        val firstWidgetId = viewModel.uiState.value.widgets.first().id
        viewModel.onWidgetTap(firstWidgetId)
        viewModel.onWidgetResizeStart(CanvasFrameResizeHandle.BottomRight)
        viewModel.onWidgetResizeDrag(CanvasFrameResizeHandle.BottomRight, ScreenPoint(80f, 32f))
        viewModel.onWidgetResizeEnd()
        advanceUntilIdle()

        val target = CanvasObjectDragTarget.Widget(firstWidgetId)
        viewModel.onEditObjectDragStart(target)
        viewModel.onEditObjectDragDelta(target, ScreenPoint(36f, -20f))
        viewModel.onEditObjectDragCancel()
        advanceUntilIdle()

        viewModel.onEditObjectDragStart(target)
        viewModel.onEditObjectDragDelta(target, ScreenPoint(-28f, 18f))
        viewModel.onEditObjectDragEnd()
        advanceUntilIdle()

        viewModel.onWidgetTap(firstWidgetId)
        viewModel.onWidgetDeleteSelected()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.widgets).hasSize(4)
        uiCollector.cancel()
    }

    @Test
    fun `app drag, search launch, apps list and package events are handled`() = runTestWithMain {
        val harness = createHarness(
            initialApps = listOf(
                CanvasApp("pkg.alpha", "Alpha", WorldPoint(0f, 0f)),
                CanvasApp("pkg.beta", "Beta", WorldPoint(220f, 120f)),
            ),
            installedApps = mutableListOf(
                InstalledApp("pkg.alpha", "Alpha Updated"),
                InstalledApp("pkg.beta", "Beta"),
                InstalledApp("pkg.new", "New App"),
            ),
        )
        val viewModel = harness.viewModel
        val uiCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.onViewportSizeChanged(widthPx = 1080, heightPx = 1920)
        viewModel.onToolSelected(LauncherToolId.Edit)
        advanceUntilIdle()
        viewModel.onAppDragStart("pkg.alpha")
        viewModel.onAppDragDelta("pkg.alpha", ScreenPoint(100f, 50f))
        viewModel.onAppDragEnd("pkg.alpha")
        advanceUntilIdle()
        assertThat(harness.appsStore.updatePositionCalls).isNotEmpty()

        viewModel.onToolSelected(LauncherToolId.Search)
        viewModel.onSearchQueryChanged("alph")
        advanceTimeBy(100)
        advanceUntilIdle()
        viewModel.onSearchLaunchTopMatch()
        advanceUntilIdle()
        assertThat(harness.appLaunchService.calls).contains("pkg.alpha")

        viewModel.onToolSelected(LauncherToolId.Search)
        viewModel.onSearchQueryChanged("no-match")
        advanceTimeBy(100)
        viewModel.onSearchActionClick()
        advanceUntilIdle()

        viewModel.onToolSelected(LauncherToolId.AppsList)
        viewModel.onAppsListQueryChanged("bet")
        advanceTimeBy(100)
        advanceUntilIdle()
        viewModel.onAppsListAppClick("pkg.beta")
        advanceUntilIdle()
        assertThat(harness.appLaunchService.calls).contains("pkg.beta")

        viewModel.onToolSelected(LauncherToolId.AppsList)
        viewModel.onAppsListShowOnCanvas("pkg.unknown")
        advanceUntilIdle()

        harness.packageEventsBus.publish(PackageEvent.Changed("pkg.alpha"))
        harness.packageEventsBus.publish(PackageEvent.Removed("pkg.beta"))
        harness.packageEventsBus.publish(PackageEvent.Added("pkg.new"))
        advanceUntilIdle()

        val packages = harness.appsStore.snapshot().map { it.packageName }
        assertThat(packages).contains("pkg.alpha")
        assertThat(packages).contains("pkg.new")
        assertThat(packages).doesNotContain("pkg.beta")
        uiCollector.cancel()
    }

    @Test
    fun `launch failure emits transient error message`() = runTestWithMain {
        val harness = createHarness()
        val viewModel = harness.viewModel
        val uiCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        harness.appLaunchService.nextResult = AppResult.Failure(AppError.LaunchUnavailable)
        advanceUntilIdle()

        val messages = mutableListOf<Int>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            messages += viewModel.messages.first()
        }

        viewModel.onAppClick("pkg.alpha")
        advanceUntilIdle()

        assertThat(messages).contains(R.string.error_launch_unavailable)
        collector.cancel()
        uiCollector.cancel()
    }

    @Test
    fun `existing canvas objects support frame edit delete erase and clear`() = runTestWithMain {
        val harness = createHarness(
            initialFrames = listOf(
                CanvasFrameObjectEntity(
                    id = "frame-1",
                    title = "Frame One",
                    centerX = 0f,
                    centerY = 0f,
                    widthWorld = 260f,
                    heightWorld = 200f,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
            initialStickyNotes = listOf(
                CanvasStickyNoteEntity(
                    id = "sticky-1",
                    text = "Note",
                    centerX = 16f,
                    centerY = 18f,
                    sizeWorld = CanvasEditDefaults.DEFAULT_STICKY_SIZE_WORLD,
                    textSizeWorld = CanvasEditDefaults.DEFAULT_TEXT_SIZE_WORLD,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
            initialTextObjects = listOf(
                CanvasTextObjectEntity(
                    id = "text-1",
                    text = "Body",
                    x = -24f,
                    y = 14f,
                    textSizeWorld = CanvasEditDefaults.DEFAULT_TEXT_SIZE_WORLD,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
            initialWidgets = listOf(
                CanvasWidgetEntity(
                    id = "widget-1",
                    type = CanvasWidgetType.ClockDigital.name,
                    centerX = 80f,
                    centerY = 80f,
                    widthWorld = CanvasEditDefaults.DEFAULT_WIDGET_WIDTH_WORLD,
                    heightWorld = CanvasEditDefaults.DEFAULT_WIDGET_HEIGHT_WORLD,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
            initialStrokes = listOf(
                CanvasStrokeWithPointsEntity(
                    stroke = CanvasStrokeEntity(
                        id = "stroke-1",
                        colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                        widthWorld = 14f,
                    ),
                    points = listOf(
                        CanvasStrokePointEntity("stroke-1", 0, -12f, -12f),
                        CanvasStrokePointEntity("stroke-1", 1, 12f, 12f),
                    ),
                ),
            ),
        )
        val viewModel = harness.viewModel
        val uiCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.onViewportSizeChanged(widthPx = 1080, heightPx = 1920)
        viewModel.onToolSelected(LauncherToolId.Edit)
        advanceUntilIdle()

        viewModel.onEditFrameBorderTap("frame-1")
        viewModel.onEditFrameResizeStart("frame-1", CanvasFrameResizeHandle.BottomRight)
        viewModel.onEditFrameResizeDrag("frame-1", CanvasFrameResizeHandle.BottomRight, ScreenPoint(60f, 30f))
        viewModel.onEditFrameResizeEnd()
        advanceUntilIdle()

        viewModel.onEditFrameTap("frame-1")
        viewModel.onEditInlineEditorValueChanged("Edited Frame")
        viewModel.onEditInlineEditorCancel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.frames.single().title).isEqualTo("Frame One")

        viewModel.onEditFrameTap("frame-1")
        viewModel.onEditInlineEditorValueChanged("")
        viewModel.onEditInlineEditorConfirm()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.frames).isEmpty()

        viewModel.onEditStickyTap("sticky-1", centerTap = false)
        viewModel.onEditStickyLongPress("sticky-1")
        viewModel.onEditStickyTap("sticky-1", centerTap = true)
        viewModel.onEditInlineEditorValueChanged("")
        viewModel.onEditInlineEditorConfirm()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.stickyNotes).isEmpty()

        viewModel.onEditTextTap("text-1")
        viewModel.onEditInlineEditorValueChanged("")
        viewModel.onEditInlineEditorConfirm()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.textObjects).isEmpty()

        viewModel.onEditToolSelected(CanvasEditToolId.Delete)
        viewModel.onEditEraseAt(WorldPoint(0f, 0f))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.strokes).isEmpty()

        viewModel.onEditToolSelected(CanvasEditToolId.Move)
        viewModel.onEditFrameDeleteTap("missing-frame")
        viewModel.onEditClearCustomElements()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.widgets).isEmpty()
        uiCollector.cancel()
    }

    @Test
    fun `selection move and resize sessions update bounds and persist transforms`() = runTestWithMain {
        val harness = createHarness(
            initialApps = listOf(
                CanvasApp("pkg.far", "Far App", WorldPoint(1000f, 1000f)),
            ),
            installedApps = mutableListOf(
                InstalledApp("pkg.far", "Far App"),
            ),
            initialFrames = listOf(
                CanvasFrameObjectEntity(
                    id = "frame-7",
                    title = "Frame",
                    centerX = 0f,
                    centerY = 0f,
                    widthWorld = 280f,
                    heightWorld = 180f,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
            initialTextObjects = listOf(
                CanvasTextObjectEntity(
                    id = "text-7",
                    text = "Text",
                    x = -20f,
                    y = -30f,
                    textSizeWorld = 42f,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
            initialStrokes = listOf(
                CanvasStrokeWithPointsEntity(
                    stroke = CanvasStrokeEntity("stroke-7", CanvasEditDefaults.DEFAULT_COLOR, 10f),
                    points = listOf(
                        CanvasStrokePointEntity("stroke-7", 0, -60f, -60f),
                        CanvasStrokePointEntity("stroke-7", 1, 60f, 60f),
                    ),
                ),
            ),
        )
        val viewModel = harness.viewModel
        val uiCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.onViewportSizeChanged(widthPx = 1080, heightPx = 1920)
        advanceUntilIdle()

        viewModel.onToolSelected(LauncherToolId.Edit)
        viewModel.onEditToolSelected(CanvasEditToolId.Selection)
        viewModel.onEditSelectionDragStart(WorldPoint(-220f, -220f))
        viewModel.onEditSelectionDragUpdate(WorldPoint(220f, 220f))
        viewModel.onEditSelectionDragEnd()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.hasActiveSelection).isTrue()

        viewModel.onEditSelectionMoveDelta(ScreenPoint(32f, 18f))
        viewModel.onEditSelectionMoveEnd()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectionBounds).isNotNull()

        viewModel.onEditSelectionResizeStart(CanvasFrameResizeHandle.BottomRight)
        viewModel.onEditSelectionResizeDrag(CanvasFrameResizeHandle.BottomRight, ScreenPoint(24f, 24f))
        viewModel.onEditSelectionResizeEnd()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectionBounds).isNotNull()

        viewModel.onEditSelectionClearTap()
        val selectedIcon = viewModel.onEditSelectionLongPressAt(WorldPoint(1000f, 1000f))
        assertThat(selectedIcon).isTrue()
        viewModel.onEditSelectionMoveDelta(ScreenPoint(14f, 9f))
        viewModel.onEditSelectionMoveEnd()
        advanceUntilIdle()

        assertThat(harness.appsStore.updatePositionCalls).isNotEmpty()
        uiCollector.cancel()
    }

    @Test
    fun `host resume refreshes decorations and contacts permission toggles do not regress state`() = runTestWithMain {
        val harness = createHarness(
            initialFrames = listOf(
                CanvasFrameObjectEntity(
                    id = "frame-1",
                    title = "Before",
                    centerX = 0f,
                    centerY = 0f,
                    widthWorld = 220f,
                    heightWorld = 140f,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
            initialWidgets = listOf(
                CanvasWidgetEntity(
                    id = "widget-1",
                    type = CanvasWidgetType.ClockDigital.name,
                    centerX = 120f,
                    centerY = 120f,
                    widthWorld = CanvasEditDefaults.DEFAULT_WIDGET_WIDTH_WORLD,
                    heightWorld = CanvasEditDefaults.DEFAULT_WIDGET_HEIGHT_WORLD,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
        )
        val viewModel = harness.viewModel
        val uiCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        harness.canvasEditDao.upsertFrameObject(
            CanvasFrameObjectEntity(
                id = "frame-2",
                title = "After",
                centerX = 320f,
                centerY = 96f,
                widthWorld = 260f,
                heightWorld = 150f,
                colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
            ),
        )

        viewModel.onHostResumed()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.frames.map { it.id }).contains("frame-2")

        viewModel.onContactsPermissionChanged(true)
        advanceUntilIdle()
        viewModel.onContactsPermissionChanged(true)
        advanceUntilIdle()
        viewModel.onContactsPermissionChanged(false)
        advanceUntilIdle()
        viewModel.onContactsPermissionChanged(false)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.frames).isNotEmpty()
        uiCollector.cancel()
    }

    @Test
    fun `deleting selected frame removes contained decorations and clears inline editor`() = runTestWithMain {
        val harness = createHarness(
            initialApps = emptyList(),
            installedApps = mutableListOf(),
            initialFrames = listOf(
                CanvasFrameObjectEntity(
                    id = "frame-1",
                    title = "Container",
                    centerX = 0f,
                    centerY = 0f,
                    widthWorld = 520f,
                    heightWorld = 420f,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
            initialStickyNotes = listOf(
                CanvasStickyNoteEntity(
                    id = "sticky-1",
                    text = "Inside",
                    centerX = 40f,
                    centerY = 40f,
                    sizeWorld = CanvasEditDefaults.DEFAULT_STICKY_SIZE_WORLD,
                    textSizeWorld = CanvasEditDefaults.DEFAULT_TEXT_SIZE_WORLD,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
            initialTextObjects = listOf(
                CanvasTextObjectEntity(
                    id = "text-1",
                    text = "Inside text",
                    x = -30f,
                    y = 24f,
                    textSizeWorld = CanvasEditDefaults.DEFAULT_TEXT_SIZE_WORLD,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
            initialWidgets = listOf(
                CanvasWidgetEntity(
                    id = "widget-1",
                    type = CanvasWidgetType.Notifications.name,
                    centerX = 24f,
                    centerY = -30f,
                    widthWorld = 260f,
                    heightWorld = 140f,
                    colorArgb = CanvasEditDefaults.DEFAULT_COLOR,
                ),
            ),
            initialStrokes = listOf(
                CanvasStrokeWithPointsEntity(
                    stroke = CanvasStrokeEntity("stroke-1", CanvasEditDefaults.DEFAULT_COLOR, 12f),
                    points = listOf(
                        CanvasStrokePointEntity("stroke-1", 0, -40f, -40f),
                        CanvasStrokePointEntity("stroke-1", 1, 40f, 40f),
                    ),
                ),
            ),
        )
        val viewModel = harness.viewModel
        val uiCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
        advanceUntilIdle()

        viewModel.onViewportSizeChanged(widthPx = 1080, heightPx = 1920)
        viewModel.onToolSelected(LauncherToolId.Edit)
        advanceUntilIdle()

        viewModel.onEditFrameTap("frame-1")
        viewModel.onEditInlineEditorValueChanged("Container edited")
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.toolsState.edit.inlineEditor.isVisible).isTrue()

        viewModel.onEditToolSelected(CanvasEditToolId.Selection)
        viewModel.onEditSelectionDragStart(WorldPoint(-400f, -300f))
        viewModel.onEditSelectionDragUpdate(WorldPoint(400f, 300f))
        viewModel.onEditSelectionDragEnd()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.hasActiveSelection).isTrue()

        viewModel.onEditSelectionDeleteTap()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.frames).isEmpty()
        assertThat(state.stickyNotes).isEmpty()
        assertThat(state.textObjects).isEmpty()
        assertThat(state.widgets).isEmpty()
        assertThat(state.strokes).isEmpty()
        assertThat(state.hasActiveSelection).isFalse()
        assertThat(state.toolsState.edit.inlineEditor.isVisible).isFalse()
        uiCollector.cancel()
    }

    private fun TestScope.createHarness(
        initialApps: List<CanvasApp> = listOf(
            CanvasApp("pkg.alpha", "Alpha", WorldPoint(0f, 0f)),
            CanvasApp("pkg.beta", "Beta", WorldPoint(180f, 100f)),
        ),
        installedApps: MutableList<InstalledApp> = mutableListOf(
            InstalledApp("pkg.alpha", "Alpha"),
            InstalledApp("pkg.beta", "Beta"),
        ),
        initialFrames: List<CanvasFrameObjectEntity> = emptyList(),
        initialStickyNotes: List<CanvasStickyNoteEntity> = emptyList(),
        initialTextObjects: List<CanvasTextObjectEntity> = emptyList(),
        initialWidgets: List<CanvasWidgetEntity> = emptyList(),
        initialStrokes: List<CanvasStrokeWithPointsEntity> = emptyList(),
    ): Harness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val appsStore = FakeCanvasAppsStore(initialApps)
        val installedSource = FakeInstalledAppsSource(installedApps)
        val iconCacheGateway = RecordingIconCacheGateway()
        val launchService = FakeAppLaunchService()
        val layoutRepository = FakeLayoutPreferencesRepository()
        val themeRepository = FakeThemePreferencesRepository()
        val layoutStrategy = FakeLayoutStrategy()
        val canvasEditDao = FakeCanvasEditDao(
            initialFrames = initialFrames,
            initialStickyNotes = initialStickyNotes,
            initialTextObjects = initialTextObjects,
            initialWidgets = initialWidgets,
            initialStrokes = initialStrokes,
        )
        val iconBitmapStore = FakeIconBitmapStore()
        val packageEventsBus = InMemoryPackageEventsBus()

        val viewportController = DefaultViewportController()
        val gestureHandler = DefaultCanvasGestureHandler(viewportController)
        val dragDropController = DefaultDragDropController()

        val dispatchersProvider = object : DispatchersProvider {
            override val io: CoroutineDispatcher = dispatcher
            override val default: CoroutineDispatcher = dispatcher
            override val main: CoroutineDispatcher = dispatcher
        }

        val viewModel = LauncherViewModel(
            appContext = ApplicationProvider.getApplicationContext<Application>(),
            observeAppsUseCase = ObserveAppsUseCase(appsStore),
            observeThemeModeUseCase = ObserveThemeModeUseCase(themeRepository),
            observeLightThemePaletteUseCase = ObserveLightThemePaletteUseCase(themeRepository),
            observeDarkThemePaletteUseCase = ObserveDarkThemePaletteUseCase(themeRepository),
            syncAppsWithSystemUseCase = SyncAppsWithSystemUseCase(
                installedAppsSource = installedSource,
                appsStore = appsStore,
                layoutStrategy = layoutStrategy,
                iconCacheGateway = iconCacheGateway,
                layoutPreferencesRepository = layoutRepository,
            ),
            handlePackageAddedUseCase = HandlePackageAddedUseCase(
                installedAppsSource = installedSource,
                appsStore = appsStore,
                iconCacheGateway = iconCacheGateway,
                layoutStrategy = layoutStrategy,
                layoutPreferencesRepository = layoutRepository,
            ),
            handlePackageRemovedUseCase = HandlePackageRemovedUseCase(
                appsStore = appsStore,
                iconCacheGateway = iconCacheGateway,
            ),
            handlePackageChangedUseCase = HandlePackageChangedUseCase(
                installedAppsSource = installedSource,
                appsStore = appsStore,
                iconCacheGateway = iconCacheGateway,
            ),
            launchAppUseCase = LaunchAppUseCase(launchService),
            updateAppPositionUseCase = UpdateAppPositionUseCase(appsStore),
            packageEventsBus = packageEventsBus,
            iconBitmapStore = iconBitmapStore,
            iconCacheGateway = iconCacheGateway,
            canvasEditDao = canvasEditDao,
            viewportController = viewportController,
            gestureHandler = gestureHandler,
            dragDropController = dragDropController,
            dispatchersProvider = dispatchersProvider,
        )

        return Harness(
            viewModel = viewModel,
            appsStore = appsStore,
            appLaunchService = launchService,
            packageEventsBus = packageEventsBus,
            canvasEditDao = canvasEditDao,
        )
    }

    private fun runTestWithMain(block: suspend TestScope.() -> Unit) {
        val dispatcher = StandardTestDispatcher()
        Dispatchers.setMain(dispatcher)
        try {
            runTest(dispatcher) {
                block()
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private data class Harness(
        val viewModel: LauncherViewModel,
        val appsStore: FakeCanvasAppsStore,
        val appLaunchService: FakeAppLaunchService,
        val packageEventsBus: InMemoryPackageEventsBus,
        val canvasEditDao: FakeCanvasEditDao,
    )

    private class FakeCanvasAppsStore(
        initial: List<CanvasApp>,
    ) : CanvasAppsStore {
        private val state = MutableStateFlow(initial)
        val updatePositionCalls = mutableListOf<Pair<String, WorldPoint>>()

        override fun observeApps(): Flow<List<CanvasApp>> = state.asStateFlow()

        override suspend fun getAppsSnapshot(): List<CanvasApp> = state.value

        override suspend fun upsertApps(apps: List<CanvasApp>) {
            val current = state.value.associateBy { it.packageName }.toMutableMap()
            apps.forEach { app -> current[app.packageName] = app }
            state.value = current.values.toList()
        }

        override suspend fun upsertApp(app: CanvasApp) {
            upsertApps(listOf(app))
        }

        override suspend fun removePackages(packages: Set<String>) {
            state.value = state.value.filterNot { app -> app.packageName in packages }
        }

        override suspend fun removePackage(packageName: String) {
            state.value = state.value.filterNot { app -> app.packageName == packageName }
        }

        override suspend fun updatePosition(packageName: String, position: WorldPoint) {
            updatePositionCalls += packageName to position
            state.value = state.value.map { app ->
                if (app.packageName == packageName) {
                    app.copy(position = position)
                } else {
                    app
                }
            }
        }

        fun snapshot(): List<CanvasApp> = state.value
    }

    private class FakeInstalledAppsSource(
        private val items: MutableList<InstalledApp>,
    ) : InstalledAppsSource {
        override suspend fun getInstalledApps(): List<InstalledApp> = items.toList()

        override suspend fun getInstalledApp(packageName: String): InstalledApp? {
            return items.firstOrNull { app -> app.packageName == packageName }
        }
    }

    private class FakeLayoutStrategy : InitialLayoutStrategy {
        override fun layout(
            existingApps: List<CanvasApp>,
            newApps: List<InstalledApp>,
            center: WorldPoint,
            mode: AppLayoutMode,
        ): List<CanvasApp> {
            val start = existingApps.size
            return newApps.mapIndexed { index, app ->
                CanvasApp(
                    packageName = app.packageName,
                    label = app.label,
                    position = WorldPoint(
                        x = center.x + (start + index) * 42f,
                        y = center.y + (start + index) * 26f,
                    ),
                )
            }
        }
    }

    private class FakeLayoutPreferencesRepository : LayoutPreferencesRepository {
        private val state = MutableStateFlow(AppLayoutMode.SMART_AUTO)

        override fun observeLayoutMode(): Flow<AppLayoutMode> = state.asStateFlow()

        override suspend fun setLayoutMode(layoutMode: AppLayoutMode) {
            state.value = layoutMode
        }
    }

    private class FakeThemePreferencesRepository : ThemePreferencesRepository {
        private val modeState = MutableStateFlow(ThemeMode.SYSTEM)
        private val lightState = MutableStateFlow(LightThemePalette.SKY_BREEZE)
        private val darkState = MutableStateFlow(DarkThemePalette.MIDNIGHT_BLUE)

        override fun observeThemeMode(): Flow<ThemeMode> = modeState.asStateFlow()

        override fun observeLightThemePalette(): Flow<LightThemePalette> = lightState.asStateFlow()

        override fun observeDarkThemePalette(): Flow<DarkThemePalette> = darkState.asStateFlow()

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            modeState.value = themeMode
        }

        override suspend fun setLightThemePalette(palette: LightThemePalette) {
            lightState.value = palette
        }

        override suspend fun setDarkThemePalette(palette: DarkThemePalette) {
            darkState.value = palette
        }
    }

    private class RecordingIconCacheGateway : IconCacheGateway {
        override suspend fun preload(packageNames: Collection<String>) = Unit
        override suspend fun invalidate(packageName: String) = Unit
        override suspend fun remove(packageName: String) = Unit
    }

    private class FakeAppLaunchService : AppLaunchService {
        var nextResult: AppResult<Unit> = AppResult.Success(Unit)
        val calls = mutableListOf<String>()

        override suspend fun launch(packageName: String): AppResult<Unit> {
            calls += packageName
            return nextResult
        }
    }

    private class FakeIconBitmapStore : IconBitmapStore {
        override val icons: MutableStateFlow<Map<String, Bitmap>> = MutableStateFlow(emptyMap())
        override fun getCached(packageName: String): Bitmap? = icons.value[packageName]
    }

    private class FakeCanvasEditDao : CanvasEditDao {
        constructor(
            initialFrames: List<CanvasFrameObjectEntity> = emptyList(),
            initialStickyNotes: List<CanvasStickyNoteEntity> = emptyList(),
            initialTextObjects: List<CanvasTextObjectEntity> = emptyList(),
            initialWidgets: List<CanvasWidgetEntity> = emptyList(),
            initialStrokes: List<CanvasStrokeWithPointsEntity> = emptyList(),
        ) {
            initialFrames.forEach { frame -> frameObjects[frame.id] = frame }
            initialStickyNotes.forEach { note -> stickyNotes[note.id] = note }
            initialTextObjects.forEach { text -> textObjects[text.id] = text }
            initialWidgets.forEach { widget -> widgets[widget.id] = widget }
            initialStrokes.forEach { strokeWithPoints ->
                strokes[strokeWithPoints.stroke.id] = strokeWithPoints.stroke
                strokePoints += strokeWithPoints.points
            }
        }

        private val stickyNotes = LinkedHashMap<String, CanvasStickyNoteEntity>()
        private val textObjects = LinkedHashMap<String, CanvasTextObjectEntity>()
        private val frameObjects = LinkedHashMap<String, CanvasFrameObjectEntity>()
        private val widgets = LinkedHashMap<String, CanvasWidgetEntity>()
        private val strokes = LinkedHashMap<String, CanvasStrokeEntity>()
        private val strokePoints = mutableListOf<CanvasStrokePointEntity>()

        override suspend fun getStickyNotes(): List<CanvasStickyNoteEntity> = stickyNotes.values.toList()

        override suspend fun getTextObjects(): List<CanvasTextObjectEntity> = textObjects.values.toList()

        override suspend fun getFrameObjects(): List<CanvasFrameObjectEntity> = frameObjects.values.toList()

        override suspend fun getWidgets(): List<CanvasWidgetEntity> = widgets.values.toList()

        override suspend fun getStrokesWithPoints(): List<CanvasStrokeWithPointsEntity> {
            return strokes.values.map { stroke ->
                CanvasStrokeWithPointsEntity(
                    stroke = stroke,
                    points = strokePoints
                        .filter { point -> point.strokeId == stroke.id }
                        .sortedBy { point -> point.pointIndex },
                )
            }
        }

        override suspend fun upsertStickyNote(note: CanvasStickyNoteEntity) {
            stickyNotes[note.id] = note
        }

        override suspend fun upsertTextObject(textObject: CanvasTextObjectEntity) {
            textObjects[textObject.id] = textObject
        }

        override suspend fun upsertFrameObject(frameObject: CanvasFrameObjectEntity) {
            frameObjects[frameObject.id] = frameObject
        }

        override suspend fun upsertWidget(widget: CanvasWidgetEntity) {
            widgets[widget.id] = widget
        }

        override suspend fun upsertStroke(stroke: CanvasStrokeEntity) {
            strokes[stroke.id] = stroke
        }

        override suspend fun insertStrokePoints(points: List<CanvasStrokePointEntity>) {
            strokePoints += points
        }

        override suspend fun deleteStrokePointsByStrokeId(strokeId: String) {
            strokePoints.removeAll { point -> point.strokeId == strokeId }
        }

        override suspend fun deleteStrokeById(strokeId: String) {
            strokes.remove(strokeId)
            strokePoints.removeAll { point -> point.strokeId == strokeId }
        }

        override suspend fun deleteStickyNoteById(id: String) {
            stickyNotes.remove(id)
        }

        override suspend fun deleteTextObjectById(id: String) {
            textObjects.remove(id)
        }

        override suspend fun deleteFrameObjectById(id: String) {
            frameObjects.remove(id)
        }

        override suspend fun deleteWidgetById(id: String) {
            widgets.remove(id)
        }

        override suspend fun deleteAllStrokes() {
            strokes.clear()
            strokePoints.clear()
        }

        override suspend fun deleteAllStickyNotes() {
            stickyNotes.clear()
        }

        override suspend fun deleteAllTextObjects() {
            textObjects.clear()
        }

        override suspend fun deleteAllFrameObjects() {
            frameObjects.clear()
        }

        override suspend fun deleteAllWidgets() {
            widgets.clear()
        }

        override suspend fun upsertStrokeWithPoints(
            stroke: CanvasStrokeEntity,
            points: List<CanvasStrokePointEntity>,
        ) {
            upsertStroke(stroke)
            deleteStrokePointsByStrokeId(stroke.id)
            if (points.isNotEmpty()) {
                insertStrokePoints(points)
            }
        }

        override suspend fun clearAllCustomElements() {
            deleteAllStrokes()
            deleteAllStickyNotes()
            deleteAllTextObjects()
            deleteAllFrameObjects()
            deleteAllWidgets()
        }
    }
}
