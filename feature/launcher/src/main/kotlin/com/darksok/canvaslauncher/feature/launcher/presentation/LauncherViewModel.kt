package com.darksok.canvaslauncher.feature.launcher.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darksok.canvaslauncher.core.common.coroutines.DispatchersProvider
import com.darksok.canvaslauncher.core.common.result.AppResult
import com.darksok.canvaslauncher.core.database.dao.CanvasEditDao
import com.darksok.canvaslauncher.core.database.dao.CanvasStrokeWithPointsEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasFrameObjectEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStickyNoteEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokeEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokePointEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasTextObjectEntity
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.core.packages.events.PackageEvent
import com.darksok.canvaslauncher.core.packages.events.PackageEventsBus
import com.darksok.canvaslauncher.core.packages.icon.IconBitmapStore
import com.darksok.canvaslauncher.core.performance.MiniMapProjector
import com.darksok.canvaslauncher.core.performance.ViewportCuller
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
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
import com.darksok.canvaslauncher.feature.canvas.CanvasGestureHandler
import com.darksok.canvaslauncher.feature.canvas.CanvasRenderableApp
import com.darksok.canvaslauncher.feature.canvas.CanvasSearchVisualState
import com.darksok.canvaslauncher.feature.canvas.DragDropController
import com.darksok.canvaslauncher.feature.canvas.DragState
import com.darksok.canvaslauncher.feature.canvas.ViewportController
import com.darksok.canvaslauncher.feature.launcher.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class LauncherViewModel @Inject constructor(
    observeAppsUseCase: ObserveAppsUseCase,
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    observeLightThemePaletteUseCase: ObserveLightThemePaletteUseCase,
    observeDarkThemePaletteUseCase: ObserveDarkThemePaletteUseCase,
    private val syncAppsWithSystemUseCase: SyncAppsWithSystemUseCase,
    private val handlePackageAddedUseCase: HandlePackageAddedUseCase,
    private val handlePackageRemovedUseCase: HandlePackageRemovedUseCase,
    private val handlePackageChangedUseCase: HandlePackageChangedUseCase,
    private val launchAppUseCase: LaunchAppUseCase,
    private val updateAppPositionUseCase: UpdateAppPositionUseCase,
    private val packageEventsBus: PackageEventsBus,
    private val iconBitmapStore: IconBitmapStore,
    private val iconCacheGateway: IconCacheGateway,
    private val canvasEditDao: CanvasEditDao,
    private val viewportController: ViewportController,
    private val gestureHandler: CanvasGestureHandler,
    private val dragDropController: DragDropController,
    private val dispatchersProvider: DispatchersProvider,
) : ViewModel() {

    private val isInitialized = MutableStateFlow(false)
    private val appsState = MutableStateFlow<List<CanvasApp>>(emptyList())
    private val committedDragPositions = MutableStateFlow<Map<String, WorldPoint>>(emptyMap())
    private val isToolsExpanded = MutableStateFlow(false)
    private val activeTool = MutableStateFlow<LauncherToolId?>(null)
    private val searchQuery = MutableStateFlow("")
    private val appsListQuery = MutableStateFlow("")
    private val showSearchLaunchAction = MutableStateFlow(false)
    private val searchOcclusionBottomPx = MutableStateFlow(0)
    private val isSearchKeyboardVisible = MutableStateFlow(false)
    private val editSelectedTool = MutableStateFlow(CanvasEditToolId.Move)
    private val editSelectedColorArgb = MutableStateFlow(CanvasEditDefaults.DEFAULT_COLOR)
    private val editBrushWidthWorld = MutableStateFlow(CanvasEditDefaults.DEFAULT_BRUSH_WIDTH_WORLD)
    private val editTextSizeWorld = MutableStateFlow(CanvasEditDefaults.DEFAULT_TEXT_SIZE_WORLD)
    private val editInlineEditor = MutableStateFlow(CanvasInlineEditorUiState())
    private val frameObjects = MutableStateFlow<List<CanvasFrameObjectUiState>>(emptyList())
    private val stickyNotes = MutableStateFlow<List<CanvasStickyNoteUiState>>(emptyList())
    private val textObjects = MutableStateFlow<List<CanvasTextObjectUiState>>(emptyList())
    private val completedStrokes = MutableStateFlow<List<CanvasStrokeUiState>>(emptyList())
    private val activeStroke = MutableStateFlow<CanvasStrokeUiState?>(null)
    private val snapGuides = MutableStateFlow<List<CanvasSnapGuideUiState>>(emptyList())
    private val spotlightPackageName = MutableStateFlow<String?>(null)
    private val transientMessageRes = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    private var cameraSnapshotBeforeSearch: CameraState? = null
    private var nextCanvasObjectId: Long = 0L
    private var activeObjectDrag: CanvasObjectDragTarget? = null
    private var transientIconSnapDragActive: Boolean = false

    private val themeMode = observeThemeModeUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM,
    )
    private val lightPalette = observeLightThemePaletteUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LightThemePalette.SKY_BREEZE,
    )
    private val darkPalette = observeDarkThemePaletteUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DarkThemePalette.MIDNIGHT_BLUE,
    )

    private val searchMatches = combine(
        appsState,
        searchQuery,
    ) { apps, query ->
        AppSearchEngine.rankByLabel(query = query, apps = apps)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val appsListEntries = combine(
        appsState,
        appsListQuery,
    ) { apps, query ->
        if (query.isBlank()) {
            apps.sortedBy { app -> app.label.lowercase(Locale.ROOT) }
                .map { app -> AppsListEntry(app.packageName, app.label) }
        } else {
            AppSearchEngine.rankByLabel(query = query, apps = apps)
                .map { match -> AppsListEntry(match.packageName, match.label) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val editUiState = combine(
        editSelectedTool,
        editSelectedColorArgb,
        editBrushWidthWorld,
        editTextSizeWorld,
        editInlineEditor,
    ) { selectedTool, selectedColor, brushWidth, textSize, inlineEditor ->
        EditUiState(
            selectedTool = selectedTool,
            selectedColorArgb = selectedColor,
            brushWidthWorld = brushWidth,
            textSizeWorld = textSize,
            inlineEditor = inlineEditor,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EditUiState(),
    )

    private val baseSearchToolsState = combine(
        isToolsExpanded,
        activeTool,
        searchQuery,
        showSearchLaunchAction,
        searchMatches,
    ) { toolsExpanded, currentTool, query, showLaunchAction, rankedMatches ->
        val queryActive = query.isNotBlank()
        val matchedPackages = if (queryActive) {
            rankedMatches.mapTo(LinkedHashSet()) { match -> match.packageName }
        } else {
            emptySet()
        }
        val topMatch = rankedMatches.firstOrNull()
        SearchToolsState(
            toolsState = ToolsUiState(
                isExpanded = toolsExpanded,
                activeTool = currentTool,
                search = SearchUiState(
                    query = query,
                    suggestionLabel = topMatch?.label,
                    topMatchPackageName = topMatch?.packageName,
                    topMatchLabel = topMatch?.label,
                    showLaunchAction = showLaunchAction && topMatch != null,
                ),
            ),
            queryActive = queryActive,
            matchedPackageNames = matchedPackages,
        )
    }

    private val searchToolsState = combine(
        baseSearchToolsState,
        editUiState,
    ) { baseState, editState ->
        baseState.copy(
            toolsState = baseState.toolsState.copy(
                edit = editState,
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchToolsState(),
    )

    private val searchPresentation = combine(
        searchToolsState,
        appsListQuery,
        appsListEntries,
        spotlightPackageName,
    ) { searchTools, listQuery, listEntries, spotlightPackage ->
        SearchPresentationState(
            toolsState = searchTools.toolsState.copy(
                appsList = AppsListUiState(
                    query = listQuery,
                ),
            ),
            queryActive = searchTools.queryActive,
            matchedPackageNames = searchTools.matchedPackageNames,
            appsListEntries = listEntries,
            spotlightPackageName = spotlightPackage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchPresentationState(),
    )

    val messages = transientMessageRes.asSharedFlow()

    private val renderState = combine(
        appsState,
        iconBitmapStore.icons,
        viewportController.cameraState,
        dragDropController.dragState,
        committedDragPositions,
    ) { apps, icons, camera, dragState, committedPositions ->
        RenderState(
            apps = apps,
            icons = icons,
            camera = camera,
            dragState = dragState,
            committedDragPositions = committedPositions,
        )
    }

    private val themedRenderState = combine(
        renderState,
        themeMode,
        lightPalette,
        darkPalette,
        isInitialized,
    ) { render, selectedThemeMode, selectedLightPalette, selectedDarkPalette, initialized ->
        ThemedRenderState(
            render = render,
            themeMode = selectedThemeMode,
            lightPalette = selectedLightPalette,
            darkPalette = selectedDarkPalette,
            isInitialized = initialized,
        )
    }

    private val visibleStrokes = combine(
        completedStrokes,
        activeStroke,
    ) { stored, inProgress ->
        if (inProgress == null) {
            stored
        } else {
            stored + inProgress
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val canvasDecorationsState = combine(
        frameObjects,
        stickyNotes,
        textObjects,
        visibleStrokes,
        snapGuides,
    ) { frames, notes, texts, strokes, guides ->
        CanvasDecorationsState(
            frames = frames,
            notes = notes,
            texts = texts,
            strokes = strokes,
            guides = guides,
        )
    }

    val uiState = combine(
        themedRenderState,
        searchPresentation,
        canvasDecorationsState,
    ) { themed, search, decorations ->
        val render = themed.render
        val withCommittedPositions = DragPositionOverrides.apply(
            apps = render.apps,
            overrides = render.committedDragPositions,
        )
        val effectiveApps = withCommittedPositions.applyDragState(render.dragState)
        val visible = ViewportCuller.cullVisibleApps(
            apps = effectiveApps,
            camera = render.camera,
        )
        val appsListItems = search.appsListEntries.map { entry ->
            AppsListItemUiState(
                packageName = entry.packageName,
                label = entry.label,
                icon = render.icons[entry.packageName],
            )
        }

        LauncherUiState(
            cameraState = render.camera,
            visibleApps = visible.map { app ->
                CanvasRenderableApp(
                    packageName = app.packageName,
                    label = app.label,
                    worldPosition = app.position,
                    icon = render.icons[app.packageName],
                    searchVisualState = when {
                        search.spotlightPackageName != null -> {
                            if (search.spotlightPackageName == app.packageName) {
                                CanvasSearchVisualState.Matched
                            } else {
                                CanvasSearchVisualState.Dimmed
                            }
                        }

                        !search.queryActive -> CanvasSearchVisualState.Normal
                        search.matchedPackageNames.contains(app.packageName) -> CanvasSearchVisualState.Matched
                        else -> CanvasSearchVisualState.Dimmed
                    },
                )
            },
            allAppPositions = if (!search.toolsState.isSearchActive && MiniMapProjector.shouldShow(render.camera.scale)) {
                effectiveApps.map { it.position }
            } else {
                emptyList()
            },
            frames = decorations.frames,
            strokes = decorations.strokes,
            stickyNotes = decorations.notes,
            textObjects = decorations.texts,
            snapGuides = decorations.guides,
            themeMode = themed.themeMode,
            lightPalette = themed.lightPalette,
            darkPalette = themed.darkPalette,
            toolsState = search.toolsState.copy(
                appsList = search.toolsState.appsList.copy(items = appsListItems),
            ),
            draggingPackageName = render.dragState?.packageName,
            isInitialized = themed.isInitialized,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState(),
    )

    init {
        viewModelScope.launch {
            observeAppsUseCase().collect { apps ->
                appsState.value = apps
                if (!isInitialized.value) {
                    isInitialized.value = true
                }
                committedDragPositions.update { overrides ->
                    DragPositionOverrides.pruneCommitted(
                        overrides = overrides,
                        persistedApps = apps,
                    )
                }
            }
        }
        observeSystemPackageEvents()
        initialSync()
    }

    fun onViewportSizeChanged(widthPx: Int, heightPx: Int) {
        viewportController.updateViewportSize(widthPx, heightPx)
    }

    fun onTransform(
        panDeltaPx: ScreenPoint,
        zoomFactor: Float,
        focusPx: ScreenPoint,
    ) {
        clearSpotlight()
        gestureHandler.onTransform(panDeltaPx, zoomFactor, focusPx)
    }

    fun onAppClick(packageName: String) {
        if (activeTool.value == LauncherToolId.Edit) return
        if (dragDropController.dragState.value != null) return
        clearSpotlight()
        viewModelScope.launch {
            when (launchAppUseCase(packageName)) {
                is AppResult.Success -> Unit
                is AppResult.Failure -> transientMessageRes.emit(R.string.error_launch_unavailable)
            }
        }
    }

    fun onAppDragStart(packageName: String) {
        clearSpotlight()
        val app = appsState.value.firstOrNull { it.packageName == packageName } ?: return
        transientIconSnapDragActive = activeTool.value != LauncherToolId.Edit
        snapGuides.value = emptyList()
        dragDropController.startDrag(packageName, app.position)
    }

    fun onAppDragDelta(
        packageName: String,
        delta: ScreenPoint,
    ) {
        val drag = dragDropController.dragState.value ?: return
        if (drag.packageName != packageName) return
        val currentScale = viewportController.cameraState.value.scale
        dragDropController.dragBy(delta, currentScale)
        if (activeTool.value == LauncherToolId.Edit || transientIconSnapDragActive) {
            val updated = dragDropController.dragState.value ?: return
            val anchors = buildSnapAnchors(excludedIconPackage = packageName)
            val snapped = SnapAssistEngine.snap(
                candidate = updated.worldPosition,
                anchors = anchors,
                cameraScale = currentScale,
                previousGuides = snapGuides.value,
                baseThresholdPx = ICON_SNAP_THRESHOLD_SCREEN_PX,
                axisInfluencePx = ICON_SNAP_AXIS_INFLUENCE_SCREEN_PX,
            )
            dragDropController.setDraggedPosition(snapped.position)
            snapGuides.value = snapped.guides
        }
    }

    fun onAppDragEnd(packageName: String) {
        val final = dragDropController.endDrag() ?: return
        if (final.packageName != packageName) {
            dragDropController.finishDrag()
            return
        }
        committedDragPositions.update { current ->
            current + (packageName to final.worldPosition)
        }
        snapGuides.value = emptyList()
        transientIconSnapDragActive = false
        activeObjectDrag = null
        dragDropController.finishDrag()
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching {
                updateAppPositionUseCase(packageName, final.worldPosition)
            }.onFailure { throwable ->
                Log.w(TAG, "Failed to persist drag position for $packageName", throwable)
                committedDragPositions.update { current -> current - packageName }
            }
        }
    }

    fun onAppDragCancel() {
        dragDropController.cancelDrag()
        snapGuides.value = emptyList()
        transientIconSnapDragActive = false
        activeObjectDrag = null
    }

    fun onToolsToggle() {
        clearSpotlight()
        if (activeTool.value != null) return
        isToolsExpanded.update { expanded -> !expanded }
    }

    fun onToolSelected(toolId: LauncherToolId) {
        when (toolId) {
            LauncherToolId.Search -> activateSearchTool()
            LauncherToolId.AppsList -> activateAppsListTool()
            LauncherToolId.Edit -> activateEditTool()
            LauncherToolId.Settings -> isToolsExpanded.value = false
        }
    }

    fun onEditClose() {
        closeEditTool()
    }

    fun onEditToolSelected(tool: CanvasEditToolId) {
        if (activeTool.value != LauncherToolId.Edit) return
        editSelectedTool.value = tool
        if (tool != CanvasEditToolId.Brush) {
            activeStroke.value = null
        }
        if (tool != CanvasEditToolId.Move) {
            activeObjectDrag = null
        }
        if (tool != CanvasEditToolId.StickyNote &&
            tool != CanvasEditToolId.Text &&
            tool != CanvasEditToolId.Frame
        ) {
            onEditInlineEditorCancel()
        }
        snapGuides.value = emptyList()
    }

    fun onEditColorSelected(colorArgb: Int) {
        if (activeTool.value != LauncherToolId.Edit) return
        if (CanvasEditDefaults.PALETTE.contains(colorArgb)) {
            editSelectedColorArgb.value = colorArgb
            when (val target = editInlineEditor.value.target) {
                is CanvasInlineEditorTarget.EditSticky -> {
                    var updated: CanvasStickyNoteUiState? = null
                    stickyNotes.update { notes ->
                        notes.map { note ->
                            if (note.id == target.id) {
                                val next = note.copy(colorArgb = colorArgb)
                                updated = next
                                next
                            } else {
                                note
                            }
                        }
                    }
                    updated?.let(::persistStickyNote)
                }

                is CanvasInlineEditorTarget.EditText -> {
                    var updated: CanvasTextObjectUiState? = null
                    textObjects.update { objects ->
                        objects.map { item ->
                            if (item.id == target.id) {
                                val next = item.copy(colorArgb = colorArgb)
                                updated = next
                                next
                            } else {
                                item
                            }
                        }
                    }
                    updated?.let(::persistTextObject)
                }

                is CanvasInlineEditorTarget.EditFrame -> {
                    var updated: CanvasFrameObjectUiState? = null
                    frameObjects.update { frames ->
                        frames.map { frame ->
                            if (frame.id == target.id) {
                                val next = frame.copy(colorArgb = colorArgb)
                                updated = next
                                next
                            } else {
                                frame
                            }
                        }
                    }
                    updated?.let(::persistFrameObject)
                }

                else -> Unit
            }
        }
    }

    fun onEditBrushSizeStep(deltaWorld: Float) {
        if (activeTool.value != LauncherToolId.Edit) return
        val updated = (editBrushWidthWorld.value + deltaWorld)
            .coerceIn(CanvasEditDefaults.MIN_BRUSH_WIDTH_WORLD, CanvasEditDefaults.MAX_BRUSH_WIDTH_WORLD)
        editBrushWidthWorld.value = updated
    }

    fun onEditTextSizeStep(deltaWorld: Float) {
        if (activeTool.value != LauncherToolId.Edit) return
        val updated = (editTextSizeWorld.value + deltaWorld)
            .coerceIn(CanvasEditDefaults.MIN_TEXT_SIZE_WORLD, CanvasEditDefaults.MAX_TEXT_SIZE_WORLD)
        editTextSizeWorld.value = updated
        when (val target = editInlineEditor.value.target) {
            is CanvasInlineEditorTarget.EditText -> {
                var changed: CanvasTextObjectUiState? = null
                textObjects.update { objects ->
                    objects.map { item ->
                        if (item.id == target.id) {
                            val next = item.copy(textSizeWorld = updated)
                            changed = next
                            next
                        } else {
                            item
                        }
                    }
                }
                changed?.let(::persistTextObject)
            }

            is CanvasInlineEditorTarget.EditSticky -> {
                var changed: CanvasStickyNoteUiState? = null
                stickyNotes.update { notes ->
                    notes.map { note ->
                        if (note.id == target.id) {
                            val next = note.copy(textSizeWorld = updated)
                            changed = next
                            next
                        } else {
                            note
                        }
                    }
                }
                changed?.let(::persistStickyNote)
            }

            else -> Unit
        }
    }

    fun onEditCanvasTap(worldPoint: WorldPoint) {
        if (activeTool.value != LauncherToolId.Edit) return
        when (editSelectedTool.value) {
            CanvasEditToolId.StickyNote -> {
                val noteId = nextCanvasId(prefix = "sticky")
                val note = CanvasStickyNoteUiState(
                    id = noteId,
                    text = "",
                    center = worldPoint,
                    sizeWorld = CanvasEditDefaults.DEFAULT_STICKY_SIZE_WORLD,
                    textSizeWorld = editTextSizeWorld.value,
                    colorArgb = editSelectedColorArgb.value,
                )
                stickyNotes.update { notes -> notes + note }
                persistStickyNote(note)
                openInlineEditor(
                    title = "Sticky note",
                    placeholder = "Type note",
                    value = "",
                    target = CanvasInlineEditorTarget.EditSticky(noteId),
                    isDraft = true,
                )
            }

            CanvasEditToolId.Text -> {
                val textId = nextCanvasId(prefix = "text")
                val text = CanvasTextObjectUiState(
                    id = textId,
                    text = "",
                    position = worldPoint,
                    textSizeWorld = editTextSizeWorld.value,
                    colorArgb = editSelectedColorArgb.value,
                )
                textObjects.update { objects -> objects + text }
                persistTextObject(text)
                openInlineEditor(
                    title = "Text",
                    placeholder = "Type text",
                    value = "",
                    target = CanvasInlineEditorTarget.EditText(textId),
                    isDraft = true,
                )
            }

            CanvasEditToolId.Frame -> {
                val frameId = nextCanvasId(prefix = "frame")
                val frame = CanvasFrameObjectUiState(
                    id = frameId,
                    title = "",
                    center = worldPoint,
                    widthWorld = CanvasEditDefaults.DEFAULT_FRAME_WIDTH_WORLD,
                    heightWorld = CanvasEditDefaults.DEFAULT_FRAME_HEIGHT_WORLD,
                    colorArgb = editSelectedColorArgb.value,
                )
                frameObjects.update { frames -> frames + frame }
                persistFrameObject(frame)
                openInlineEditor(
                    title = "Frame title",
                    placeholder = "Type frame title",
                    value = "",
                    target = CanvasInlineEditorTarget.EditFrame(frameId),
                    isDraft = true,
                )
            }

            else -> Unit
        }
    }

    fun onEditBrushStart(worldPoint: WorldPoint) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Brush) return
        activeStroke.value = CanvasStrokeUiState(
            id = nextCanvasId(prefix = "stroke"),
            points = listOf(worldPoint),
            colorArgb = editSelectedColorArgb.value,
            widthWorld = editBrushWidthWorld.value,
        )
    }

    fun onEditBrushPoint(worldPoint: WorldPoint) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Brush) return
        val stroke = activeStroke.value ?: return
        val previous = stroke.points.lastOrNull() ?: worldPoint
        val dx = worldPoint.x - previous.x
        val dy = worldPoint.y - previous.y
        if (sqrt(dx * dx + dy * dy) < BRUSH_MIN_POINT_DISTANCE_WORLD) return
        activeStroke.value = stroke.copy(points = stroke.points + worldPoint)
    }

    fun onEditBrushEnd() {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Brush) return
        val stroke = activeStroke.value ?: return
        if (stroke.points.size >= 2) {
            completedStrokes.update { it + stroke }
            persistStroke(stroke)
        }
        activeStroke.value = null
    }

    fun onEditEraseAt(worldPoint: WorldPoint) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Eraser) return
        val radiusWorld = ERASER_RADIUS_SCREEN_PX / viewportController.cameraState.value.scale
        val removedStrokeIds = mutableListOf<String>()
        completedStrokes.update { strokes ->
            strokes.filterNot { stroke ->
                val intersects = stroke.points.any { point ->
                    val dx = point.x - worldPoint.x
                    val dy = point.y - worldPoint.y
                    (dx * dx + dy * dy) <= radiusWorld * radiusWorld
                }
                if (intersects) {
                    removedStrokeIds += stroke.id
                }
                intersects
            }
        }
        if (removedStrokeIds.isNotEmpty()) {
            viewModelScope.launch(dispatchersProvider.io) {
                removedStrokeIds.forEach { strokeId ->
                    runCatching { canvasEditDao.deleteStrokeById(strokeId) }
                        .onFailure { throwable ->
                            Log.w(TAG, "Failed to delete stroke $strokeId", throwable)
                        }
                }
            }
        }
    }

    fun onEditStickyTap(
        noteId: String,
        centerTap: Boolean,
    ) {
        if (activeTool.value != LauncherToolId.Edit) return
        if (editSelectedTool.value == CanvasEditToolId.Delete) {
            deleteStickyNote(noteId)
            return
        }
        if (editSelectedTool.value != CanvasEditToolId.Move) return
        val note = stickyNotes.value.firstOrNull { it.id == noteId } ?: return
        if (centerTap) {
            editSelectedColorArgb.value = note.colorArgb
            editTextSizeWorld.value = note.textSizeWorld
            openInlineEditor(
                title = "Edit note",
                placeholder = "Type note",
                value = note.text,
                target = CanvasInlineEditorTarget.EditSticky(noteId),
            )
        } else {
            var resizedNote: CanvasStickyNoteUiState? = null
            stickyNotes.update { notes ->
                notes.map { current ->
                    if (current.id != noteId) {
                        current
                    } else {
                        val resized = current.copy(
                            sizeWorld = nextStickySize(current.sizeWorld),
                        )
                        resizedNote = resized
                        resized
                    }
                }
            }
            resizedNote?.let(::persistStickyNote)
        }
    }

    fun onEditStickyLongPress(noteId: String) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Move) return
        var resizedNote: CanvasStickyNoteUiState? = null
        stickyNotes.update { notes ->
            notes.map { current ->
                if (current.id != noteId) {
                    current
                } else {
                    val resized = current.copy(
                        sizeWorld = nextStickySize(current.sizeWorld),
                    )
                    resizedNote = resized
                    resized
                }
            }
        }
        resizedNote?.let(::persistStickyNote)
    }

    fun onEditTextTap(textId: String) {
        if (activeTool.value != LauncherToolId.Edit) return
        if (editSelectedTool.value == CanvasEditToolId.Delete) {
            deleteTextObject(textId)
            return
        }
        if (editSelectedTool.value != CanvasEditToolId.Move) return
        val text = textObjects.value.firstOrNull { it.id == textId } ?: return
        editSelectedColorArgb.value = text.colorArgb
        editTextSizeWorld.value = text.textSizeWorld
        openInlineEditor(
            title = "Edit text",
            placeholder = "Type text",
            value = text.text,
            target = CanvasInlineEditorTarget.EditText(textId),
        )
    }

    fun onEditFrameTap(frameId: String) {
        if (activeTool.value != LauncherToolId.Edit) return
        if (editSelectedTool.value == CanvasEditToolId.Delete) {
            deleteFrameObject(frameId)
            return
        }
        if (editSelectedTool.value != CanvasEditToolId.Move) return
        val frame = frameObjects.value.firstOrNull { it.id == frameId } ?: return
        editSelectedColorArgb.value = frame.colorArgb
        openInlineEditor(
            title = "Edit frame",
            placeholder = "Type frame title",
            value = frame.title,
            target = CanvasInlineEditorTarget.EditFrame(frameId),
        )
    }

    fun onEditObjectDragStart(target: CanvasObjectDragTarget) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Move) return
        activeObjectDrag = target
        snapGuides.value = emptyList()
    }

    fun onEditObjectDragDelta(
        target: CanvasObjectDragTarget,
        delta: ScreenPoint,
    ) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Move) return
        if (activeObjectDrag != target) return
        val scale = viewportController.cameraState.value.scale
        if (scale == 0f) return
        val worldDeltaX = delta.x / scale
        val worldDeltaY = delta.y / scale

        when (target) {
            is CanvasObjectDragTarget.Frame -> {
                val frame = frameObjects.value.firstOrNull { it.id == target.id } ?: return
                val candidate = WorldPoint(
                    x = frame.center.x + worldDeltaX,
                    y = frame.center.y + worldDeltaY,
                )
                val snap = SnapAssistEngine.snap(
                    candidate = candidate,
                    anchors = buildSnapAnchors(excludedObject = target),
                    cameraScale = scale,
                    previousGuides = snapGuides.value,
                    baseThresholdPx = OBJECT_SNAP_THRESHOLD_SCREEN_PX,
                    axisInfluencePx = OBJECT_SNAP_AXIS_INFLUENCE_SCREEN_PX,
                )
                frameObjects.update { frames ->
                    frames.map { current ->
                        if (current.id == target.id) current.copy(center = snap.position) else current
                    }
                }
                snapGuides.value = snap.guides
            }

            is CanvasObjectDragTarget.Sticky -> {
                val note = stickyNotes.value.firstOrNull { it.id == target.id } ?: return
                val candidate = WorldPoint(
                    x = note.center.x + worldDeltaX,
                    y = note.center.y + worldDeltaY,
                )
                val snap = SnapAssistEngine.snap(
                    candidate = candidate,
                    anchors = buildSnapAnchors(excludedObject = target),
                    cameraScale = scale,
                    previousGuides = snapGuides.value,
                    baseThresholdPx = OBJECT_SNAP_THRESHOLD_SCREEN_PX,
                    axisInfluencePx = OBJECT_SNAP_AXIS_INFLUENCE_SCREEN_PX,
                )
                stickyNotes.update { notes ->
                    notes.map { current ->
                        if (current.id == target.id) current.copy(center = snap.position) else current
                    }
                }
                snapGuides.value = snap.guides
            }

            is CanvasObjectDragTarget.Text -> {
                val text = textObjects.value.firstOrNull { it.id == target.id } ?: return
                val candidate = WorldPoint(
                    x = text.position.x + worldDeltaX,
                    y = text.position.y + worldDeltaY,
                )
                val snap = SnapAssistEngine.snap(
                    candidate = candidate,
                    anchors = buildSnapAnchors(excludedObject = target),
                    cameraScale = scale,
                    previousGuides = snapGuides.value,
                    baseThresholdPx = OBJECT_SNAP_THRESHOLD_SCREEN_PX,
                    axisInfluencePx = OBJECT_SNAP_AXIS_INFLUENCE_SCREEN_PX,
                )
                textObjects.update { texts ->
                    texts.map { current ->
                        if (current.id == target.id) current.copy(position = snap.position) else current
                    }
                }
                snapGuides.value = snap.guides
            }
        }
    }

    fun onEditObjectDragEnd() {
        persistDraggedObject(activeObjectDrag)
        activeObjectDrag = null
        snapGuides.value = emptyList()
    }

    fun onEditInlineEditorValueChanged(value: String) {
        if (activeTool.value != LauncherToolId.Edit) return
        editInlineEditor.update { it.copy(value = value) }
        when (val target = editInlineEditor.value.target) {
            is CanvasInlineEditorTarget.EditSticky -> {
                stickyNotes.update { notes ->
                    notes.map { note ->
                        if (note.id == target.id) note.copy(text = value) else note
                    }
                }
            }

            is CanvasInlineEditorTarget.EditText -> {
                textObjects.update { objects ->
                    objects.map { item ->
                        if (item.id == target.id) item.copy(text = value) else item
                    }
                }
            }

            is CanvasInlineEditorTarget.EditFrame -> {
                frameObjects.update { frames ->
                    frames.map { frame ->
                        if (frame.id == target.id) frame.copy(title = value) else frame
                    }
                }
            }

            else -> Unit
        }
    }

    fun onEditInlineEditorConfirm() {
        if (activeTool.value != LauncherToolId.Edit) return
        val editor = editInlineEditor.value
        val text = editor.value.trim()
        when (val target = editor.target) {
            CanvasInlineEditorTarget.None -> Unit
            is CanvasInlineEditorTarget.NewSticky -> Unit

            is CanvasInlineEditorTarget.EditSticky -> {
                if (text.isBlank()) {
                    deleteStickyNote(target.id)
                } else {
                    var edited: CanvasStickyNoteUiState? = null
                    stickyNotes.update { notes ->
                        notes.map { note ->
                            if (note.id == target.id) {
                                val updated = note.copy(text = text)
                                edited = updated
                                updated
                            } else {
                                note
                            }
                        }
                    }
                    edited?.let(::persistStickyNote)
                }
            }

            is CanvasInlineEditorTarget.NewText -> Unit

            is CanvasInlineEditorTarget.EditText -> {
                if (text.isBlank()) {
                    deleteTextObject(target.id)
                } else {
                    var edited: CanvasTextObjectUiState? = null
                    textObjects.update { objects ->
                        objects.map { item ->
                            if (item.id == target.id) {
                                val updated = item.copy(text = text)
                                edited = updated
                                updated
                            } else {
                                item
                            }
                        }
                    }
                    edited?.let(::persistTextObject)
                }
            }

            is CanvasInlineEditorTarget.NewFrame -> Unit

            is CanvasInlineEditorTarget.EditFrame -> {
                if (text.isBlank()) {
                    deleteFrameObject(target.id)
                } else {
                    var edited: CanvasFrameObjectUiState? = null
                    frameObjects.update { frames ->
                        frames.map { frame ->
                            if (frame.id == target.id) {
                                val updated = frame.copy(title = text)
                                edited = updated
                                updated
                            } else {
                                frame
                            }
                        }
                    }
                    edited?.let(::persistFrameObject)
                }
            }
        }
        editInlineEditor.value = CanvasInlineEditorUiState()
    }

    fun onEditInlineEditorCancel() {
        val editor = editInlineEditor.value
        if (editor.isDraft) {
            when (val target = editor.target) {
                is CanvasInlineEditorTarget.EditSticky -> deleteStickyNote(target.id)
                is CanvasInlineEditorTarget.EditText -> deleteTextObject(target.id)
                is CanvasInlineEditorTarget.EditFrame -> deleteFrameObject(target.id)
                else -> Unit
            }
        } else {
            when (val target = editor.target) {
                is CanvasInlineEditorTarget.EditSticky -> {
                    var restored: CanvasStickyNoteUiState? = null
                    stickyNotes.update { notes ->
                        notes.map { note ->
                            if (note.id == target.id) {
                                val next = note.copy(text = editor.initialValue)
                                restored = next
                                next
                            } else {
                                note
                            }
                        }
                    }
                    restored?.let(::persistStickyNote)
                }

                is CanvasInlineEditorTarget.EditText -> {
                    var restored: CanvasTextObjectUiState? = null
                    textObjects.update { objects ->
                        objects.map { item ->
                            if (item.id == target.id) {
                                val next = item.copy(text = editor.initialValue)
                                restored = next
                                next
                            } else {
                                item
                            }
                        }
                    }
                    restored?.let(::persistTextObject)
                }

                is CanvasInlineEditorTarget.EditFrame -> {
                    var restored: CanvasFrameObjectUiState? = null
                    frameObjects.update { frames ->
                        frames.map { frame ->
                            if (frame.id == target.id) {
                                val next = frame.copy(title = editor.initialValue)
                                restored = next
                                next
                            } else {
                                frame
                            }
                        }
                    }
                    restored?.let(::persistFrameObject)
                }

                else -> Unit
            }
        }
        editInlineEditor.value = CanvasInlineEditorUiState()
    }

    fun onEditClearCustomElements() {
        if (activeTool.value != LauncherToolId.Edit) return
        frameObjects.value = emptyList()
        stickyNotes.value = emptyList()
        textObjects.value = emptyList()
        completedStrokes.value = emptyList()
        activeStroke.value = null
        snapGuides.value = emptyList()
        activeObjectDrag = null
        editInlineEditor.value = CanvasInlineEditorUiState()
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { canvasEditDao.clearAllCustomElements() }
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to clear custom elements", throwable)
                }
        }
    }

    fun collapseToolsPanel() {
        if (activeTool.value == null) {
            isToolsExpanded.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        clearSpotlight()
        searchQuery.value = query
        showSearchLaunchAction.value = false
    }

    fun onSearchActionClick() {
        executeSearch(showLaunchAction = true)
    }

    fun onSearchSubmit() {
        executeSearch(showLaunchAction = true)
    }

    fun onSearchLaunchTopMatch() {
        val packageName = searchMatches.value.firstOrNull()?.packageName ?: return
        onAppClick(packageName)
    }

    fun onSearchClose() {
        closeSearchTool(restoreViewport = true)
    }

    fun onSearchKeyboardVisibilityChanged(isVisible: Boolean) {
        if (isSearchKeyboardVisible.value != isVisible) {
            isSearchKeyboardVisible.value = isVisible
        }
    }

    fun onSearchOcclusionChanged(bottomOcclusionPx: Int) {
        val value = bottomOcclusionPx.coerceAtLeast(0)
        if (searchOcclusionBottomPx.value != value) {
            searchOcclusionBottomPx.value = value
        }
    }

    fun onAppsListQueryChanged(query: String) {
        appsListQuery.value = query
    }

    fun onAppsListAppClick(packageName: String) {
        clearSpotlight()
        onAppClick(packageName)
        closeAppsListTool()
    }

    fun onAppsListShowOnCanvas(packageName: String) {
        closeAppsListTool()
        focusOnPackage(packageName)
        spotlightPackageName.value = packageName
    }

    fun onAppsListClose() {
        closeAppsListTool()
    }

    private fun activateSearchTool() {
        if (activeTool.value == LauncherToolId.Search) return
        if (activeTool.value == LauncherToolId.AppsList) {
            closeAppsListTool()
        } else if (activeTool.value == LauncherToolId.Edit) {
            closeEditTool()
        }
        cameraSnapshotBeforeSearch = viewportController.cameraState.value
        activeTool.value = LauncherToolId.Search
        isToolsExpanded.value = false
        searchQuery.value = ""
        showSearchLaunchAction.value = false
        clearSpotlight()
    }

    private fun activateAppsListTool() {
        if (activeTool.value == LauncherToolId.AppsList) return
        if (activeTool.value == LauncherToolId.Search) {
            closeSearchTool(restoreViewport = true)
        } else if (activeTool.value == LauncherToolId.Edit) {
            closeEditTool()
        }
        activeTool.value = LauncherToolId.AppsList
        isToolsExpanded.value = false
        searchQuery.value = ""
        showSearchLaunchAction.value = false
        appsListQuery.value = ""
    }

    private fun activateEditTool() {
        if (activeTool.value == LauncherToolId.Edit) return
        when (activeTool.value) {
            LauncherToolId.Search -> closeSearchTool(restoreViewport = true)
            LauncherToolId.AppsList -> closeAppsListTool()
            else -> Unit
        }
        activeTool.value = LauncherToolId.Edit
        isToolsExpanded.value = false
        showSearchLaunchAction.value = false
        clearSpotlight()
        snapGuides.value = emptyList()
        activeObjectDrag = null
        transientIconSnapDragActive = false
    }

    private fun closeSearchTool(restoreViewport: Boolean) {
        if (activeTool.value == LauncherToolId.Search && restoreViewport) {
            cameraSnapshotBeforeSearch?.let { snapshot ->
                val currentCamera = viewportController.cameraState.value
                viewportController.setCamera(
                    snapshot.withCurrentViewport(currentCamera),
                )
            }
        }
        cameraSnapshotBeforeSearch = null
        activeTool.value = null
        isToolsExpanded.value = false
        searchQuery.value = ""
        showSearchLaunchAction.value = false
        searchOcclusionBottomPx.value = 0
        isSearchKeyboardVisible.value = false
        clearSpotlight()
    }

    private fun closeAppsListTool() {
        if (activeTool.value == LauncherToolId.AppsList) {
            activeTool.value = null
        }
        isToolsExpanded.value = false
        appsListQuery.value = ""
    }

    private fun closeEditTool() {
        if (activeTool.value == LauncherToolId.Edit) {
            activeTool.value = null
        }
        isToolsExpanded.value = false
        editSelectedTool.value = CanvasEditToolId.Move
        activeStroke.value = null
        editInlineEditor.value = CanvasInlineEditorUiState()
        snapGuides.value = emptyList()
        activeObjectDrag = null
        transientIconSnapDragActive = false
    }

    private fun clearSpotlight() {
        if (spotlightPackageName.value != null) {
            spotlightPackageName.value = null
        }
    }

    private fun executeSearch(showLaunchAction: Boolean) {
        clearSpotlight()
        val topMatch = searchMatches.value.firstOrNull() ?: run {
            showSearchLaunchAction.value = false
            return
        }
        focusOnPackage(topMatch.packageName)
        showSearchLaunchAction.value = showLaunchAction
    }

    private fun focusOnPackage(packageName: String) {
        val app = appsState.value.firstOrNull { item -> item.packageName == packageName } ?: return
        val camera = viewportController.cameraState.value
        val viewportHeight = camera.viewportHeightPx.toFloat()
        val offsetY = if (isSearchKeyboardVisible.value) {
            val targetY = viewportHeight * SEARCH_KEYBOARD_TARGET_SCREEN_Y_RATIO
            targetY - viewportHeight / 2f
        } else {
            val occludedPx = searchOcclusionBottomPx.value.toFloat().coerceAtLeast(SEARCH_BASE_OCCLUSION_PX)
            val desiredLiftPx = occludedPx * SEARCH_FOCUS_OFFSET_MULTIPLIER + SEARCH_FOCUS_EXTRA_GAP_PX
            val maxLiftPx = max(
                SEARCH_MIN_FOCUS_LIFT_PX,
                viewportHeight * SEARCH_MAX_FOCUS_LIFT_RATIO,
            )
            -desiredLiftPx.coerceIn(SEARCH_MIN_FOCUS_LIFT_PX, maxLiftPx)
        }
        val targetScale = max(camera.scale, SEARCH_MIN_FOCUS_SCALE)
        if (targetScale > camera.scale) {
            val focusPoint = ScreenPoint(
                x = camera.viewportWidthPx / 2f,
                y = camera.viewportHeightPx / 2f + offsetY,
            )
            viewportController.zoomBy(targetScale / camera.scale, focusPoint)
        }
        viewportController.centerOn(
            worldPoint = app.position,
            screenOffsetPx = ScreenPoint(0f, offsetY),
        )
    }

    private fun buildSnapAnchors(
        excludedIconPackage: String? = null,
        excludedObject: CanvasObjectDragTarget? = null,
    ): List<WorldPoint> {
        val iconAnchors = DragPositionOverrides.apply(
            apps = appsState.value,
            overrides = committedDragPositions.value,
        ).mapNotNull { app ->
            if (app.packageName == excludedIconPackage) {
                null
            } else {
                app.position
            }
        }

        val excludedStickyId = (excludedObject as? CanvasObjectDragTarget.Sticky)?.id
        val excludedTextId = (excludedObject as? CanvasObjectDragTarget.Text)?.id
        val excludedFrameId = (excludedObject as? CanvasObjectDragTarget.Frame)?.id

        val stickyAnchors = stickyNotes.value
            .asSequence()
            .filterNot { it.id == excludedStickyId }
            .map { it.center }
            .toList()
        val textAnchors = textObjects.value
            .asSequence()
            .filterNot { it.id == excludedTextId }
            .map { it.position }
            .toList()
        val frameAnchors = frameObjects.value
            .asSequence()
            .filterNot { it.id == excludedFrameId }
            .map { it.center }
            .toList()

        return buildList(iconAnchors.size + stickyAnchors.size + textAnchors.size + frameAnchors.size) {
            addAll(iconAnchors)
            addAll(stickyAnchors)
            addAll(textAnchors)
            addAll(frameAnchors)
        }
    }

    private suspend fun loadPersistedCustomElements() {
        val persistedStickyNotes = canvasEditDao.getStickyNotes().map { entity ->
            CanvasStickyNoteUiState(
                id = entity.id,
                text = entity.text,
                center = WorldPoint(entity.centerX, entity.centerY),
                sizeWorld = entity.sizeWorld,
                textSizeWorld = entity.textSizeWorld,
                colorArgb = entity.colorArgb,
            )
        }
        val persistedTextObjects = canvasEditDao.getTextObjects().map { entity ->
            CanvasTextObjectUiState(
                id = entity.id,
                text = entity.text,
                position = WorldPoint(entity.x, entity.y),
                textSizeWorld = entity.textSizeWorld,
                colorArgb = entity.colorArgb,
            )
        }
        val persistedFrameObjects = canvasEditDao.getFrameObjects().map { entity ->
            CanvasFrameObjectUiState(
                id = entity.id,
                title = entity.title,
                center = WorldPoint(entity.centerX, entity.centerY),
                widthWorld = entity.widthWorld,
                heightWorld = entity.heightWorld,
                colorArgb = entity.colorArgb,
            )
        }
        val persistedStrokes = canvasEditDao.getStrokesWithPoints().map { entity ->
            entity.toUiState()
        }

        stickyNotes.value = persistedStickyNotes
        textObjects.value = persistedTextObjects
        frameObjects.value = persistedFrameObjects
        completedStrokes.value = persistedStrokes

        nextCanvasObjectId = max(
            nextCanvasObjectId,
            listOf(
                persistedStickyNotes.map { it.id },
                persistedTextObjects.map { it.id },
                persistedFrameObjects.map { it.id },
                persistedStrokes.map { it.id },
            )
                .flatten()
                .mapNotNull(::extractNumericSuffix)
                .maxOrNull()
                ?.plus(1)
                ?: 0L,
        )
    }

    private fun persistDraggedObject(target: CanvasObjectDragTarget?) {
        when (target) {
            is CanvasObjectDragTarget.Sticky -> {
                stickyNotes.value.firstOrNull { it.id == target.id }?.let(::persistStickyNote)
            }

            is CanvasObjectDragTarget.Text -> {
                textObjects.value.firstOrNull { it.id == target.id }?.let(::persistTextObject)
            }

            is CanvasObjectDragTarget.Frame -> {
                frameObjects.value.firstOrNull { it.id == target.id }?.let(::persistFrameObject)
            }

            null -> Unit
        }
    }

    private fun persistStroke(stroke: CanvasStrokeUiState) {
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching {
                canvasEditDao.upsertStrokeWithPoints(
                    stroke = CanvasStrokeEntity(
                        id = stroke.id,
                        colorArgb = stroke.colorArgb,
                        widthWorld = stroke.widthWorld,
                    ),
                    points = stroke.points.mapIndexed { index, point ->
                        CanvasStrokePointEntity(
                            strokeId = stroke.id,
                            pointIndex = index,
                            x = point.x,
                            y = point.y,
                        )
                    },
                )
            }.onFailure { throwable ->
                Log.w(TAG, "Failed to persist stroke ${stroke.id}", throwable)
            }
        }
    }

    private fun persistStickyNote(note: CanvasStickyNoteUiState) {
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching {
                canvasEditDao.upsertStickyNote(
                    CanvasStickyNoteEntity(
                        id = note.id,
                        text = note.text,
                        centerX = note.center.x,
                        centerY = note.center.y,
                        sizeWorld = note.sizeWorld,
                        textSizeWorld = note.textSizeWorld,
                        colorArgb = note.colorArgb,
                    ),
                )
            }.onFailure { throwable ->
                Log.w(TAG, "Failed to persist sticky note ${note.id}", throwable)
            }
        }
    }

    private fun persistTextObject(textObject: CanvasTextObjectUiState) {
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching {
                canvasEditDao.upsertTextObject(
                    CanvasTextObjectEntity(
                        id = textObject.id,
                        text = textObject.text,
                        x = textObject.position.x,
                        y = textObject.position.y,
                        textSizeWorld = textObject.textSizeWorld,
                        colorArgb = textObject.colorArgb,
                    ),
                )
            }.onFailure { throwable ->
                Log.w(TAG, "Failed to persist text object ${textObject.id}", throwable)
            }
        }
    }

    private fun persistFrameObject(frame: CanvasFrameObjectUiState) {
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching {
                canvasEditDao.upsertFrameObject(
                    CanvasFrameObjectEntity(
                        id = frame.id,
                        title = frame.title,
                        centerX = frame.center.x,
                        centerY = frame.center.y,
                        widthWorld = frame.widthWorld,
                        heightWorld = frame.heightWorld,
                        colorArgb = frame.colorArgb,
                    ),
                )
            }.onFailure { throwable ->
                Log.w(TAG, "Failed to persist frame object ${frame.id}", throwable)
            }
        }
    }

    private fun deleteStickyNote(id: String) {
        stickyNotes.update { notes -> notes.filterNot { note -> note.id == id } }
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { canvasEditDao.deleteStickyNoteById(id) }
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to delete sticky note $id", throwable)
                }
        }
    }

    private fun deleteTextObject(id: String) {
        textObjects.update { objects -> objects.filterNot { item -> item.id == id } }
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { canvasEditDao.deleteTextObjectById(id) }
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to delete text object $id", throwable)
                }
        }
    }

    private fun deleteFrameObject(id: String) {
        frameObjects.update { frames -> frames.filterNot { frame -> frame.id == id } }
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { canvasEditDao.deleteFrameObjectById(id) }
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to delete frame object $id", throwable)
                }
        }
    }

    private fun extractNumericSuffix(id: String): Long? {
        val index = id.lastIndexOf('-')
        if (index < 0 || index == id.lastIndex) return null
        return id.substring(index + 1).toLongOrNull()
    }

    private fun nextStickySize(current: Float): Float {
        val sizes = listOf(
            CanvasEditDefaults.STICKY_MIN_SIZE_WORLD,
            CanvasEditDefaults.DEFAULT_STICKY_SIZE_WORLD,
            CanvasEditDefaults.STICKY_MAX_SIZE_WORLD,
        )
        val currentIndex = sizes.indexOfFirst { value -> value == current }
        if (currentIndex == -1) return CanvasEditDefaults.DEFAULT_STICKY_SIZE_WORLD
        return sizes[(currentIndex + 1) % sizes.size]
    }

    private fun openInlineEditor(
        title: String,
        placeholder: String,
        value: String,
        target: CanvasInlineEditorTarget,
        isDraft: Boolean = false,
    ) {
        editInlineEditor.value = CanvasInlineEditorUiState(
            isVisible = true,
            title = title,
            placeholder = placeholder,
            value = value,
            initialValue = value,
            isDraft = isDraft,
            target = target,
        )
    }

    private fun nextCanvasId(prefix: String): String {
        val next = nextCanvasObjectId++
        return "$prefix-$next"
    }

    private fun initialSync() {
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { loadPersistedCustomElements() }
                .onFailure { throwable ->
                    Log.e(TAG, "Failed to load persisted custom elements", throwable)
                }
            runCatching { syncAppsWithSystemUseCase(preloadIcons = false) }
                .onFailure { throwable ->
                    Log.e(TAG, "Initial sync failed", throwable)
                }
            runCatching {
                val packages = appsState.value.map { app -> app.packageName }
                if (packages.isNotEmpty()) {
                    iconCacheGateway.preload(packages)
                }
            }
                .onFailure { throwable ->
                    Log.w(TAG, "Icon warmup failed", throwable)
                }
        }
    }

    private fun observeSystemPackageEvents() {
        viewModelScope.launch {
            packageEventsBus.events.collect { event ->
                withContext(dispatchersProvider.io) {
                    runCatching {
                        when (event) {
                            is PackageEvent.Added -> {
                                handlePackageAddedUseCase(
                                    packageName = event.packageName,
                                    worldCenter = viewportController.cameraState.value.worldCenter,
                                )
                            }

                            is PackageEvent.Removed -> handlePackageRemovedUseCase(event.packageName)
                            is PackageEvent.Changed -> handlePackageChangedUseCase(event.packageName)
                        }
                    }.onFailure { throwable ->
                        Log.w(TAG, "Failed to process package event: ${event.packageName}", throwable)
                    }
                }
            }
        }
    }

    private fun List<CanvasApp>.applyDragState(
        dragState: DragState?,
    ): List<CanvasApp> {
        if (dragState == null) return this
        return map { app ->
            if (app.packageName == dragState.packageName) {
                app.copy(position = dragState.worldPosition)
            } else {
                app
            }
        }
    }

    private companion object {
        private const val TAG = "LauncherViewModel"
        private const val ICON_SNAP_THRESHOLD_SCREEN_PX = 9f
        private const val ICON_SNAP_AXIS_INFLUENCE_SCREEN_PX = 88f
        private const val OBJECT_SNAP_THRESHOLD_SCREEN_PX = 12f
        private const val OBJECT_SNAP_AXIS_INFLUENCE_SCREEN_PX = 120f
        private const val SEARCH_BASE_OCCLUSION_PX = 64f
        private const val SEARCH_FOCUS_OFFSET_MULTIPLIER = 0.20f
        private const val SEARCH_FOCUS_EXTRA_GAP_PX = 16f
        private const val SEARCH_MIN_FOCUS_LIFT_PX = 32f
        private const val SEARCH_MAX_FOCUS_LIFT_RATIO = 0.22f
        private const val SEARCH_KEYBOARD_TARGET_SCREEN_Y_RATIO = 0.25f
        private const val SEARCH_MIN_FOCUS_SCALE = 1.08f
        private const val BRUSH_MIN_POINT_DISTANCE_WORLD = 2f
        private const val ERASER_RADIUS_SCREEN_PX = 22f
    }
}

private fun CanvasStrokeWithPointsEntity.toUiState(): CanvasStrokeUiState {
    return CanvasStrokeUiState(
        id = stroke.id,
        points = points
            .sortedBy { point -> point.pointIndex }
            .map { point -> WorldPoint(point.x, point.y) },
        colorArgb = stroke.colorArgb,
        widthWorld = stroke.widthWorld,
    )
}

sealed interface CanvasObjectDragTarget {
    data class Sticky(val id: String) : CanvasObjectDragTarget
    data class Text(val id: String) : CanvasObjectDragTarget
    data class Frame(val id: String) : CanvasObjectDragTarget
}

private data class RenderState(
    val apps: List<CanvasApp>,
    val icons: Map<String, android.graphics.Bitmap?>,
    val camera: CameraState,
    val dragState: DragState?,
    val committedDragPositions: Map<String, WorldPoint>,
)

private data class ThemedRenderState(
    val render: RenderState,
    val themeMode: ThemeMode,
    val lightPalette: LightThemePalette,
    val darkPalette: DarkThemePalette,
    val isInitialized: Boolean,
)

private data class SearchPresentationState(
    val toolsState: ToolsUiState = ToolsUiState(),
    val queryActive: Boolean = false,
    val matchedPackageNames: Set<String> = emptySet(),
    val appsListEntries: List<AppsListEntry> = emptyList(),
    val spotlightPackageName: String? = null,
)

private data class SearchToolsState(
    val toolsState: ToolsUiState = ToolsUiState(),
    val queryActive: Boolean = false,
    val matchedPackageNames: Set<String> = emptySet(),
)

private data class CanvasDecorationsState(
    val frames: List<CanvasFrameObjectUiState> = emptyList(),
    val notes: List<CanvasStickyNoteUiState> = emptyList(),
    val texts: List<CanvasTextObjectUiState> = emptyList(),
    val strokes: List<CanvasStrokeUiState> = emptyList(),
    val guides: List<CanvasSnapGuideUiState> = emptyList(),
)

private data class AppsListEntry(
    val packageName: String,
    val label: String,
)
