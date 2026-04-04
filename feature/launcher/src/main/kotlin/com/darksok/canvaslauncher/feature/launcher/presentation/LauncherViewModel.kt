package com.darksok.canvaslauncher.feature.launcher.presentation

import android.util.Log
import androidx.annotation.StringRes
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
import com.darksok.canvaslauncher.core.database.entity.CanvasWidgetEntity
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.CanvasConstants
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
import com.darksok.canvaslauncher.core.performance.WorldScreenTransformer
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
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
    private val frameDraft = MutableStateFlow<CanvasFrameDraftUiState?>(null)
    private val selectedFrameIdForResize = MutableStateFlow<String?>(null)
    private val selectedWidgetIdForResize = MutableStateFlow<String?>(null)
    private val selectionDraft = MutableStateFlow<CanvasSelectionDraftUiState?>(null)
    private val selectedObjects = MutableStateFlow(CanvasSelectionUiState())
    private val selectionBounds = MutableStateFlow<CanvasSelectionBoundsUiState?>(null)
    private val stickyNotes = MutableStateFlow<List<CanvasStickyNoteUiState>>(emptyList())
    private val textObjects = MutableStateFlow<List<CanvasTextObjectUiState>>(emptyList())
    private val widgets = MutableStateFlow<List<CanvasWidgetUiState>>(emptyList())
    private val completedStrokes = MutableStateFlow<List<CanvasStrokeUiState>>(emptyList())
    private val activeStroke = MutableStateFlow<CanvasStrokeUiState?>(null)
    private val snapGuides = MutableStateFlow<List<CanvasSnapGuideUiState>>(emptyList())
    private val spotlightPackageName = MutableStateFlow<String?>(null)
    private val transientMessageRes = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    private var cameraSnapshotBeforeSearch: CameraState? = null
    private var nextCanvasObjectId: Long = 0L
    private var activeObjectDrag: CanvasObjectDragTarget? = null
    private var activeObjectDragSession: ObjectDragSession? = null
    private var activeFrameResizeSession: FrameResizeSession? = null
    private var activeWidgetResizeSession: WidgetResizeSession? = null
    private var activeSelectionResizeSession: SelectionResizeSession? = null
    private var transientIconSnapDragActive: Boolean = false
    private var cameraFlightJob: Job? = null
    private var iconWarmupJob: Job? = null

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

    private val appsSearchIndex = appsState
        .mapLatest { apps ->
            withContext(dispatchersProvider.default) {
                AppSearchEngine.buildIndex(apps)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSearchEngine.buildIndex(emptyList()),
        )
    private val debouncedSearchQuery = searchQuery
        .debounce(SEARCH_MATCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "",
        )
    private val debouncedAppsListQuery = appsListQuery
        .debounce(SEARCH_MATCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "",
        )

    private val searchMatches = combine(
        appsSearchIndex,
        debouncedSearchQuery,
    ) { searchIndex, query ->
        searchIndex to query
    }.mapLatest { (searchIndex, query) ->
        withContext(dispatchersProvider.default) {
            AppSearchEngine.rankByLabel(
                query = query,
                searchIndex = searchIndex,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val appsListEntries = combine(
        appsSearchIndex,
        debouncedAppsListQuery,
    ) { searchIndex, query ->
        searchIndex to query
    }.mapLatest { (searchIndex, query) ->
        if (query.isBlank()) {
            searchIndex.entriesSortedByLabel.map { app ->
                AppsListEntry(
                    packageName = app.packageName,
                    label = app.label,
                )
            }
        } else {
            withContext(dispatchersProvider.default) {
                AppSearchEngine.rankByLabel(
                    query = query,
                    searchIndex = searchIndex,
                ).map { match ->
                    AppsListEntry(
                        packageName = match.packageName,
                        label = match.label,
                    )
                }
            }
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

    private val appsListItemsState = combine(
        appsListEntries,
        iconBitmapStore.icons,
    ) { entries, icons ->
        entries.map { entry ->
            AppsListItemUiState(
                packageName = entry.packageName,
                label = entry.label,
                icon = icons[entry.packageName],
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
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

    private val frameDecorationsState = combine(
        combine(
            frameObjects,
            frameDraft,
            selectedFrameIdForResize,
            selectedWidgetIdForResize,
            selectionDraft,
        ) { frames, currentFrameDraft, selectedFrameId, selectedWidgetId, selectionDraftState ->
            FrameDecorationsState(
                frames = frames,
                frameDraft = currentFrameDraft,
                selectedFrameIdForResize = selectedFrameId,
                selectedWidgetIdForResize = selectedWidgetId,
                selectionDraft = selectionDraftState,
            )
        },
        selectionBounds,
        selectedObjects,
    ) { frameDecorations, selectionBoundsState, selectedItems ->
        frameDecorations.copy(
            selectionBounds = selectionBoundsState,
            hasActiveSelection = !selectedItems.isEmpty,
        )
    }

    private val canvasDecorationsState = combine(
        combine(
            frameDecorationsState,
            stickyNotes,
            textObjects,
            widgets,
            visibleStrokes,
        ) { frameDecorations, notes, texts, canvasWidgets, strokes ->
            CanvasDecorationsState(
                frames = frameDecorations.frames,
                frameDraft = frameDecorations.frameDraft,
                selectedFrameIdForResize = frameDecorations.selectedFrameIdForResize,
                selectedWidgetIdForResize = frameDecorations.selectedWidgetIdForResize,
                selectionDraft = frameDecorations.selectionDraft,
                selectionBounds = frameDecorations.selectionBounds,
                hasActiveSelection = frameDecorations.hasActiveSelection,
                notes = notes,
                texts = texts,
                widgets = canvasWidgets,
                strokes = strokes,
            )
        },
        snapGuides,
    ) { decorations, guides ->
        decorations.copy(
            guides = guides,
        )
    }

    val uiState = combine(
        themedRenderState,
        searchPresentation,
        appsListItemsState,
        canvasDecorationsState,
    ) { themed, search, appsListItems, decorations ->
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
            frameDraft = decorations.frameDraft,
            selectedFrameIdForResize = decorations.selectedFrameIdForResize,
            selectionDraft = decorations.selectionDraft,
            selectionBounds = decorations.selectionBounds,
            hasActiveSelection = decorations.hasActiveSelection,
            widgets = decorations.widgets,
            selectedWidgetId = decorations.selectedWidgetIdForResize,
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
                pruneSelectionToExistingObjects()
                scheduleIconWarmup(apps)
            }
        }
        observeViewportIconWarmup()
        observeSystemPackageEvents()
        initialSync()
    }

    fun onViewportSizeChanged(widthPx: Int, heightPx: Int) {
        viewportController.updateViewportSize(widthPx, heightPx)
    }

    fun onHostResumed() {
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { refreshPersistedDecorations() }
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to refresh persisted decorations on resume", throwable)
                }
        }
    }

    fun onTransform(
        panDeltaPx: ScreenPoint,
        zoomFactor: Float,
        focusPx: ScreenPoint,
    ) {
        clearSpotlight()
        cancelCameraFlightAnimation()
        gestureHandler.onTransform(panDeltaPx, zoomFactor, focusPx)
    }

    fun onAppClick(packageName: String) {
        if (activeTool.value == LauncherToolId.Edit || activeTool.value == LauncherToolId.Widgets) return
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
        if (activeTool.value == LauncherToolId.Widgets) return
        clearSpotlight()
        cancelCameraFlightAnimation()
        val app = appsState.value.firstOrNull { it.packageName == packageName } ?: return
        transientIconSnapDragActive = activeTool.value != LauncherToolId.Edit
        snapGuides.value = emptyList()
        dragDropController.startDrag(packageName, app.position)
    }

    fun onAppDragDelta(
        packageName: String,
        delta: ScreenPoint,
    ) {
        if (activeTool.value == LauncherToolId.Widgets) return
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
        if (activeTool.value == LauncherToolId.Widgets) {
            dragDropController.cancelDrag()
            return
        }
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
        activeObjectDragSession = null
        dragDropController.finishDrag()
        if (!selectedObjects.value.isEmpty) {
            selectionBounds.value = computeSelectionBounds(selectedObjects.value)
        }
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
        if (activeTool.value == LauncherToolId.Widgets) {
            dragDropController.cancelDrag()
            return
        }
        dragDropController.cancelDrag()
        snapGuides.value = emptyList()
        transientIconSnapDragActive = false
        activeObjectDrag = null
        activeObjectDragSession = null
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
            LauncherToolId.Widgets -> activateWidgetsTool()
            LauncherToolId.Settings -> isToolsExpanded.value = false
        }
    }

    fun onEditClose() {
        closeEditTool()
    }

    fun onWidgetsClose() {
        closeWidgetsTool()
    }

    fun onEditToolSelected(tool: CanvasEditToolId) {
        if (activeTool.value != LauncherToolId.Edit) return
        editSelectedTool.value = tool
        cancelCameraFlightAnimation()
        if (tool != CanvasEditToolId.Brush) {
            activeStroke.value = null
        }
        if (tool != CanvasEditToolId.Move) {
            activeObjectDrag = null
            activeObjectDragSession = null
            activeFrameResizeSession = null
            selectedFrameIdForResize.value = null
        }
        if (tool != CanvasEditToolId.Selection) {
            clearSelection()
        }
        if (tool != CanvasEditToolId.Frame) {
            frameDraft.value = null
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
                    titleResId = R.string.edit_inline_title_sticky_note,
                    placeholderResId = R.string.edit_inline_placeholder_note,
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
                    titleResId = R.string.edit_inline_title_text,
                    placeholderResId = R.string.edit_inline_placeholder_text,
                    value = "",
                    target = CanvasInlineEditorTarget.EditText(textId),
                    isDraft = true,
                )
            }

            CanvasEditToolId.Frame -> {
                frameDraft.value = null
                createFrame(
                    center = worldPoint,
                    widthWorld = CanvasEditDefaults.DEFAULT_FRAME_WIDTH_WORLD,
                    heightWorld = CanvasEditDefaults.DEFAULT_FRAME_HEIGHT_WORLD,
                    openEditor = true,
                    isDraft = true,
                )
            }

            else -> Unit
        }
    }

    fun onEditFrameDragStart(worldPoint: WorldPoint) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Frame) return
        frameDraft.value = CanvasFrameDraftUiState(
            startCorner = worldPoint,
            endCorner = worldPoint,
            colorArgb = editSelectedColorArgb.value,
        )
    }

    fun onEditFrameDragUpdate(worldPoint: WorldPoint) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Frame) return
        val draft = frameDraft.value ?: return
        frameDraft.value = draft.copy(endCorner = worldPoint)
    }

    fun onEditFrameDragEnd() {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Frame) return
        val draft = frameDraft.value ?: return
        val left = min(draft.startCorner.x, draft.endCorner.x)
        val right = max(draft.startCorner.x, draft.endCorner.x)
        val top = min(draft.startCorner.y, draft.endCorner.y)
        val bottom = max(draft.startCorner.y, draft.endCorner.y)
        val width = (right - left).coerceAtLeast(FRAME_RESIZE_MIN_WIDTH_WORLD)
        val height = (bottom - top).coerceAtLeast(FRAME_RESIZE_MIN_HEIGHT_WORLD)
        val center = WorldPoint(
            x = (left + right) / 2f,
            y = (top + bottom) / 2f,
        )
        frameDraft.value = null
        createFrame(
            center = center,
            widthWorld = width,
            heightWorld = height,
            openEditor = true,
            isDraft = true,
        )
    }

    fun onEditSelectionDragStart(worldPoint: WorldPoint) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Selection) return
        if (!selectedObjects.value.isEmpty) return
        cancelCameraFlightAnimation()
        activeObjectDrag = null
        activeObjectDragSession = null
        activeFrameResizeSession = null
        activeSelectionResizeSession = null
        selectedFrameIdForResize.value = null
        snapGuides.value = emptyList()
        selectionDraft.value = CanvasSelectionDraftUiState(
            startCorner = worldPoint,
            endCorner = worldPoint,
        )
    }

    fun onEditSelectionDragUpdate(worldPoint: WorldPoint) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Selection) return
        val draft = selectionDraft.value ?: return
        selectionDraft.value = draft.copy(endCorner = worldPoint)
    }

    fun onEditSelectionDragEnd() {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Selection) return
        val draft = selectionDraft.value ?: return
        selectionDraft.value = null
        val rawBounds = worldBoundsOfCorners(draft.startCorner, draft.endCorner)
        val hasArea = rawBounds.width >= SELECTION_MIN_SIZE_WORLD &&
            rawBounds.height >= SELECTION_MIN_SIZE_WORLD
        if (!hasArea) {
            clearSelection()
            return
        }
        val selection = selectObjectsFullyInside(rawBounds)
        selectedObjects.value = selection
        selectionBounds.value = computeSelectionBounds(selection)
        activeSelectionResizeSession = null
        snapGuides.value = emptyList()
    }

    fun onEditSelectionClearTap() {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Selection) return
        clearSelection()
        snapGuides.value = emptyList()
    }

    fun onEditSelectionMoveDelta(delta: ScreenPoint) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Selection) return
        val selection = selectedObjects.value
        if (selection.isEmpty) return
        val scale = viewportController.cameraState.value.scale
        if (scale <= 0f) return
        val worldDeltaX = delta.x / scale
        val worldDeltaY = delta.y / scale
        if (worldDeltaX == 0f && worldDeltaY == 0f) return
        applySelectionTranslation(selection, worldDeltaX, worldDeltaY)
        selectionBounds.value = selectionBounds.value?.offset(worldDeltaX, worldDeltaY)
        snapGuides.value = emptyList()
    }

    fun onEditSelectionMoveEnd() {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Selection) return
        persistSelectionTransform(includeIcons = true)
        selectionBounds.value = computeSelectionBounds(selectedObjects.value)
    }

    fun onEditSelectionResizeStart(handle: CanvasFrameResizeHandle) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Selection) return
        val selection = selectedObjects.value
        val bounds = selectionBounds.value ?: return
        if (selection.isEmpty || !bounds.canResizeAndDelete) return
        activeSelectionResizeSession = SelectionResizeSession(
            handle = handle,
            selection = selection,
            initialBounds = WorldBounds(
                left = bounds.left,
                top = bounds.top,
                right = bounds.right,
                bottom = bounds.bottom,
            ),
            initialFrames = frameObjects.value
                .asSequence()
                .filter { it.id in selection.frameIds }
                .associateBy { it.id },
            initialTexts = textObjects.value
                .asSequence()
                .filter { it.id in selection.textIds }
                .associateBy { it.id },
            initialStrokes = completedStrokes.value
                .asSequence()
                .filter { it.id in selection.strokeIds }
                .associateBy { it.id },
        )
        snapGuides.value = emptyList()
    }

    fun onEditSelectionResizeDrag(
        handle: CanvasFrameResizeHandle,
        delta: ScreenPoint,
    ) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Selection) return
        val session = activeSelectionResizeSession ?: return
        if (session.handle != handle) return
        val scale = viewportController.cameraState.value.scale
        if (scale <= 0f) return

        session.accumulatedDeltaXWorld += delta.x / scale
        session.accumulatedDeltaYWorld += delta.y / scale
        val resizedBounds = session.initialBounds.resizeByHandle(
            handle = handle,
            deltaXWorld = session.accumulatedDeltaXWorld,
            deltaYWorld = session.accumulatedDeltaYWorld,
            minWidthWorld = SELECTION_RESIZE_MIN_WIDTH_WORLD,
            minHeightWorld = SELECTION_RESIZE_MIN_HEIGHT_WORLD,
        )
        val scaleX = (resizedBounds.width / session.initialBounds.width).coerceAtLeast(0.001f)
        val scaleY = (resizedBounds.height / session.initialBounds.height).coerceAtLeast(0.001f)

        frameObjects.update { frames ->
            frames.map { current ->
                val initial = session.initialFrames[current.id] ?: return@map current
                transformFrameByBounds(
                    frame = initial,
                    source = session.initialBounds,
                    target = resizedBounds,
                )
            }
        }
        textObjects.update { texts ->
            texts.map { current ->
                val initial = session.initialTexts[current.id] ?: return@map current
                transformTextByBounds(
                    text = initial,
                    source = session.initialBounds,
                    target = resizedBounds,
                    scaleX = scaleX,
                    scaleY = scaleY,
                )
            }
        }
        completedStrokes.update { strokes ->
            strokes.map { current ->
                val initial = session.initialStrokes[current.id] ?: return@map current
                transformStrokeByBounds(
                    stroke = initial,
                    source = session.initialBounds,
                    target = resizedBounds,
                )
            }
        }
        selectionBounds.value = computeSelectionBounds(selectedObjects.value)
        snapGuides.value = emptyList()
    }

    fun onEditSelectionResizeEnd() {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Selection) {
            activeSelectionResizeSession = null
            return
        }
        persistSelectionTransform(includeIcons = false)
        selectionBounds.value = computeSelectionBounds(selectedObjects.value)
        activeSelectionResizeSession = null
    }

    fun onEditSelectionDeleteTap() {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Selection) return
        deleteSelectionWithoutIcons()
    }

    fun onEditAutoPanDelta(delta: ScreenPoint) {
        if (activeTool.value != LauncherToolId.Edit && activeTool.value != LauncherToolId.Widgets) return
        if (delta.x == 0f && delta.y == 0f) return
        val camera = viewportController.cameraState.value
        gestureHandler.onTransform(
            panDeltaPx = ScreenPoint(
                x = -delta.x,
                y = -delta.y,
            ),
            zoomFactor = 1f,
            focusPx = ScreenPoint(
                x = camera.viewportWidthPx / 2f,
                y = camera.viewportHeightPx / 2f,
            ),
        )
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
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Delete) return
        val currentSelectionBounds = selectionBounds.value
        if (currentSelectionBounds != null &&
            currentSelectionBounds.canResizeAndDelete &&
            currentSelectionBounds.contains(worldPoint)
        ) {
            deleteSelectionWithoutIcons()
            return
        }
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
            pruneSelectionToExistingObjects()
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
        selectedFrameIdForResize.value = null
        activeFrameResizeSession = null
        selectedWidgetIdForResize.value = null
        activeWidgetResizeSession = null
        val note = stickyNotes.value.firstOrNull { it.id == noteId } ?: return
        if (centerTap) {
            editSelectedColorArgb.value = note.colorArgb
            editTextSizeWorld.value = note.textSizeWorld
            openInlineEditor(
                titleResId = R.string.edit_inline_title_edit_note,
                placeholderResId = R.string.edit_inline_placeholder_note,
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
        selectedFrameIdForResize.value = null
        activeFrameResizeSession = null
        selectedWidgetIdForResize.value = null
        activeWidgetResizeSession = null
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
        selectedFrameIdForResize.value = null
        activeFrameResizeSession = null
        selectedWidgetIdForResize.value = null
        activeWidgetResizeSession = null
        val text = textObjects.value.firstOrNull { it.id == textId } ?: return
        editSelectedColorArgb.value = text.colorArgb
        editTextSizeWorld.value = text.textSizeWorld
        openInlineEditor(
            titleResId = R.string.edit_inline_title_edit_text,
            placeholderResId = R.string.edit_inline_placeholder_text,
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
        selectedWidgetIdForResize.value = null
        activeWidgetResizeSession = null
        val frame = frameObjects.value.firstOrNull { it.id == frameId } ?: return
        editSelectedColorArgb.value = frame.colorArgb
        openInlineEditor(
            titleResId = R.string.edit_inline_title_edit_frame,
            placeholderResId = R.string.edit_inline_placeholder_frame_title,
            value = frame.title,
            target = CanvasInlineEditorTarget.EditFrame(frameId),
        )
    }

    fun onEditFrameBorderTap(frameId: String) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Move) return
        if (frameObjects.value.none { it.id == frameId }) return
        selectedFrameIdForResize.value = frameId
    }

    fun onEditFrameResizeStart(
        frameId: String,
        handle: CanvasFrameResizeHandle,
    ) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Move) return
        if (frameObjects.value.none { it.id == frameId }) return
        selectedFrameIdForResize.value = frameId
        activeFrameResizeSession = FrameResizeSession(
            frameId = frameId,
            handle = handle,
        )
    }

    fun onEditFrameResizeDrag(
        frameId: String,
        handle: CanvasFrameResizeHandle,
        delta: ScreenPoint,
    ) {
        if (activeTool.value != LauncherToolId.Edit || editSelectedTool.value != CanvasEditToolId.Move) return
        val session = activeFrameResizeSession ?: return
        if (session.frameId != frameId || session.handle != handle) return
        val frame = frameObjects.value.firstOrNull { it.id == frameId } ?: return
        val scale = viewportController.cameraState.value.scale
        if (scale <= 0f) return
        val resized = frame.resizeByHandleDrag(
            handle = handle,
            deltaXWorld = delta.x / scale,
            deltaYWorld = delta.y / scale,
            minWidthWorld = FRAME_RESIZE_MIN_WIDTH_WORLD,
            minHeightWorld = FRAME_RESIZE_MIN_HEIGHT_WORLD,
        )
        frameObjects.update { frames ->
            frames.map { current ->
                if (current.id == frameId) resized else current
            }
        }
        if (!selectedObjects.value.isEmpty) {
            selectionBounds.value = computeSelectionBounds(selectedObjects.value)
        }
    }

    fun onEditFrameResizeEnd() {
        val session = activeFrameResizeSession ?: return
        val updated = frameObjects.value.firstOrNull { it.id == session.frameId } ?: run {
            activeFrameResizeSession = null
            return
        }
        persistFrameObject(updated)
        activeFrameResizeSession = null
        if (!selectedObjects.value.isEmpty) {
            selectionBounds.value = computeSelectionBounds(selectedObjects.value)
        }
    }

    fun onEditObjectDragStart(target: CanvasObjectDragTarget) {
        val isEditMove = activeTool.value == LauncherToolId.Edit && editSelectedTool.value == CanvasEditToolId.Move
        val isWidgetMove = activeTool.value == LauncherToolId.Widgets && target is CanvasObjectDragTarget.Widget
        if (!isEditMove && !isWidgetMove) return
        cancelCameraFlightAnimation()
        val initialPosition = currentObjectPosition(target) ?: return
        val snapAnchors = buildSnapAnchors(excludedObject = target)
        activeObjectDrag = target
        activeObjectDragSession = ObjectDragSession(
            target = target,
            initialPosition = initialPosition,
            snapAnchors = snapAnchors,
        )
        activeFrameResizeSession = null
        activeWidgetResizeSession = null
        selectedFrameIdForResize.value = (target as? CanvasObjectDragTarget.Frame)?.id
        selectedWidgetIdForResize.value = (target as? CanvasObjectDragTarget.Widget)?.id
        snapGuides.value = emptyList()
    }

    fun onEditObjectDragDelta(
        target: CanvasObjectDragTarget,
        delta: ScreenPoint,
    ) {
        val isEditMove = activeTool.value == LauncherToolId.Edit && editSelectedTool.value == CanvasEditToolId.Move
        val isWidgetMove = activeTool.value == LauncherToolId.Widgets && target is CanvasObjectDragTarget.Widget
        if (!isEditMove && !isWidgetMove) return
        if (activeObjectDrag != target) return
        val session = activeObjectDragSession ?: return
        if (session.target != target) return
        val scale = viewportController.cameraState.value.scale
        if (scale <= 0f) return
        if (currentObjectPosition(target) == null) {
            activeObjectDrag = null
            activeObjectDragSession = null
            snapGuides.value = emptyList()
            return
        }

        session.accumulatedDeltaXWorld += delta.x / scale
        session.accumulatedDeltaYWorld += delta.y / scale
        val candidate = WorldPoint(
            x = session.initialPosition.x + session.accumulatedDeltaXWorld,
            y = session.initialPosition.y + session.accumulatedDeltaYWorld,
        )
        val snap = SnapAssistEngine.snap(
            candidate = candidate,
            anchors = session.snapAnchors,
            cameraScale = scale,
            previousGuides = snapGuides.value,
            baseThresholdPx = OBJECT_SNAP_THRESHOLD_SCREEN_PX,
            axisInfluencePx = OBJECT_SNAP_AXIS_INFLUENCE_SCREEN_PX,
        )
        when (target) {
            is CanvasObjectDragTarget.Frame -> {
                frameObjects.update { frames ->
                    frames.map { current ->
                        if (current.id == target.id) current.copy(center = snap.position) else current
                    }
                }
            }

            is CanvasObjectDragTarget.Sticky -> {
                stickyNotes.update { notes ->
                    notes.map { current ->
                        if (current.id == target.id) current.copy(center = snap.position) else current
                    }
                }
            }

            is CanvasObjectDragTarget.Text -> {
                textObjects.update { texts ->
                    texts.map { current ->
                        if (current.id == target.id) current.copy(position = snap.position) else current
                    }
                }
            }

            is CanvasObjectDragTarget.Widget -> {
                widgets.update { canvasWidgets ->
                    canvasWidgets.map { current ->
                        if (current.id == target.id) current.copy(center = snap.position) else current
                    }
                }
            }
        }
        snapGuides.value = snap.guides
        if (!selectedObjects.value.isEmpty) {
            selectionBounds.value = computeSelectionBounds(selectedObjects.value)
        }
    }

    fun onEditObjectDragEnd() {
        persistDraggedObject(activeObjectDrag)
        activeObjectDrag = null
        activeObjectDragSession = null
        snapGuides.value = emptyList()
        if (!selectedObjects.value.isEmpty) {
            selectionBounds.value = computeSelectionBounds(selectedObjects.value)
        }
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
        frameDraft.value = null
        selectedFrameIdForResize.value = null
        selectedWidgetIdForResize.value = null
        selectionDraft.value = null
        selectedObjects.value = CanvasSelectionUiState()
        selectionBounds.value = null
        stickyNotes.value = emptyList()
        textObjects.value = emptyList()
        widgets.value = emptyList()
        completedStrokes.value = emptyList()
        activeStroke.value = null
        snapGuides.value = emptyList()
        activeObjectDrag = null
        activeObjectDragSession = null
        activeFrameResizeSession = null
        activeWidgetResizeSession = null
        activeSelectionResizeSession = null
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
        if (searchQuery.value == query) return
        clearSpotlight()
        cancelCameraFlightAnimation()
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
        if (appsListQuery.value == query) return
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
        cancelCameraFlightAnimation()
        if (activeTool.value == LauncherToolId.AppsList) {
            closeAppsListTool()
        } else if (activeTool.value == LauncherToolId.Edit) {
            closeEditTool()
        } else if (activeTool.value == LauncherToolId.Widgets) {
            closeWidgetsTool()
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
        cancelCameraFlightAnimation()
        if (activeTool.value == LauncherToolId.Search) {
            closeSearchTool(restoreViewport = true)
        } else if (activeTool.value == LauncherToolId.Edit) {
            closeEditTool()
        } else if (activeTool.value == LauncherToolId.Widgets) {
            closeWidgetsTool()
        }
        activeTool.value = LauncherToolId.AppsList
        isToolsExpanded.value = false
        searchQuery.value = ""
        showSearchLaunchAction.value = false
        appsListQuery.value = ""
    }

    private fun activateEditTool() {
        if (activeTool.value == LauncherToolId.Edit) return
        cancelCameraFlightAnimation()
        when (activeTool.value) {
            LauncherToolId.Search -> closeSearchTool(restoreViewport = true)
            LauncherToolId.AppsList -> closeAppsListTool()
            LauncherToolId.Widgets -> closeWidgetsTool()
            else -> Unit
        }
        activeTool.value = LauncherToolId.Edit
        isToolsExpanded.value = false
        showSearchLaunchAction.value = false
        clearSpotlight()
        snapGuides.value = emptyList()
        activeObjectDrag = null
        activeObjectDragSession = null
        activeFrameResizeSession = null
        activeWidgetResizeSession = null
        activeSelectionResizeSession = null
        selectedFrameIdForResize.value = null
        selectedWidgetIdForResize.value = null
        frameDraft.value = null
        selectionDraft.value = null
        selectedObjects.value = CanvasSelectionUiState()
        selectionBounds.value = null
        transientIconSnapDragActive = false
    }

    private fun activateWidgetsTool() {
        if (activeTool.value == LauncherToolId.Widgets) return
        cancelCameraFlightAnimation()
        when (activeTool.value) {
            LauncherToolId.Search -> closeSearchTool(restoreViewport = true)
            LauncherToolId.AppsList -> closeAppsListTool()
            LauncherToolId.Edit -> closeEditTool()
            else -> Unit
        }
        activeTool.value = LauncherToolId.Widgets
        isToolsExpanded.value = false
        showSearchLaunchAction.value = false
        clearSpotlight()
        snapGuides.value = emptyList()
        activeObjectDrag = null
        activeObjectDragSession = null
        activeFrameResizeSession = null
        activeWidgetResizeSession = null
        activeSelectionResizeSession = null
        selectedFrameIdForResize.value = null
        frameDraft.value = null
        selectionDraft.value = null
        selectedObjects.value = CanvasSelectionUiState()
        selectionBounds.value = null
        transientIconSnapDragActive = false
    }

    private fun closeSearchTool(restoreViewport: Boolean) {
        cancelCameraFlightAnimation()
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
        cancelCameraFlightAnimation()
        isToolsExpanded.value = false
        editSelectedTool.value = CanvasEditToolId.Move
        activeStroke.value = null
        editInlineEditor.value = CanvasInlineEditorUiState()
        snapGuides.value = emptyList()
        activeObjectDrag = null
        activeObjectDragSession = null
        activeFrameResizeSession = null
        activeWidgetResizeSession = null
        activeSelectionResizeSession = null
        selectedFrameIdForResize.value = null
        selectedWidgetIdForResize.value = null
        frameDraft.value = null
        selectionDraft.value = null
        selectedObjects.value = CanvasSelectionUiState()
        selectionBounds.value = null
        transientIconSnapDragActive = false
    }

    private fun closeWidgetsTool() {
        if (activeTool.value == LauncherToolId.Widgets) {
            activeTool.value = null
        }
        cancelCameraFlightAnimation()
        isToolsExpanded.value = false
        snapGuides.value = emptyList()
        activeObjectDrag = null
        activeObjectDragSession = null
        activeWidgetResizeSession = null
        selectedWidgetIdForResize.value = null
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
        val targetCenter = WorldPoint(
            x = app.position.x,
            y = app.position.y - (offsetY / targetScale),
        )
        val targetCamera = camera.copy(
            worldCenter = targetCenter,
            scale = targetScale,
        )
        animateCameraFlight(targetCamera)
    }

    private fun createFrame(
        center: WorldPoint,
        widthWorld: Float,
        heightWorld: Float,
        openEditor: Boolean,
        isDraft: Boolean,
    ) {
        val frameId = nextCanvasId(prefix = "frame")
        val frame = CanvasFrameObjectUiState(
            id = frameId,
            title = "",
            center = center,
            widthWorld = widthWorld.coerceAtLeast(FRAME_RESIZE_MIN_WIDTH_WORLD),
            heightWorld = heightWorld.coerceAtLeast(FRAME_RESIZE_MIN_HEIGHT_WORLD),
            colorArgb = editSelectedColorArgb.value,
        )
        frameObjects.update { frames -> frames + frame }
        persistFrameObject(frame)
        if (openEditor) {
            openInlineEditor(
                titleResId = R.string.edit_inline_title_frame_title,
                placeholderResId = R.string.edit_inline_placeholder_frame_title,
                value = "",
                target = CanvasInlineEditorTarget.EditFrame(frameId),
                isDraft = isDraft,
            )
        }
    }

    private fun findFreeWidgetCenter(
        widthWorld: Float,
        heightWorld: Float,
    ): WorldPoint {
        val start = viewportController.cameraState.value.worldCenter
        val searchStep = (max(widthWorld, heightWorld) * 0.9f).coerceAtLeast(120f)
        val angleSteps = 24
        val maxRings = 140

        fun centerBounds(center: WorldPoint): WorldBounds {
            val halfWidth = widthWorld / 2f
            val halfHeight = heightWorld / 2f
            return WorldBounds(
                left = center.x - halfWidth,
                top = center.y - halfHeight,
                right = center.x + halfWidth,
                bottom = center.y + halfHeight,
            )
        }

        if (isWidgetBoundsFree(centerBounds(start))) {
            return start
        }

        for (ring in 1..maxRings) {
            val radius = ring * searchStep
            val shift = if (ring % 2 == 0) 0.0 else Math.PI / angleSteps
            repeat(angleSteps) { index ->
                val angle = (2.0 * Math.PI * index / angleSteps) + shift
                val candidate = WorldPoint(
                    x = start.x + (cos(angle) * radius).toFloat(),
                    y = start.y + (sin(angle) * radius).toFloat(),
                )
                if (isWidgetBoundsFree(centerBounds(candidate))) {
                    return candidate
                }
            }
        }

        var fallback = WorldPoint(
            x = start.x + searchStep * (maxRings + 2),
            y = start.y + searchStep * (maxRings + 2),
        )
        while (!isWidgetBoundsFree(centerBounds(fallback))) {
            fallback = WorldPoint(
                x = fallback.x + searchStep,
                y = fallback.y + searchStep,
            )
        }
        return fallback
    }

    private fun isWidgetBoundsFree(
        candidate: WorldBounds,
        excludedWidgetId: String? = null,
    ): Boolean {
        val scale = viewportController.cameraState.value.scale
        val iconHalfSizeWorld = iconWorldSizeForScale(scale) / 2f
        val iconIntersects = currentAppPositionsByPackage()
            .values
            .any { position ->
                val iconBounds = WorldBounds(
                    left = position.x - iconHalfSizeWorld,
                    top = position.y - iconHalfSizeWorld,
                    right = position.x + iconHalfSizeWorld,
                    bottom = position.y + iconHalfSizeWorld,
                )
                candidate.intersects(iconBounds)
            }
        if (iconIntersects) return false

        if (frameObjects.value.any { frame -> candidate.intersects(frame.worldBounds()) }) return false
        if (stickyNotes.value.any { note -> candidate.intersects(note.worldBounds()) }) return false
        if (textObjects.value.any { text -> candidate.intersects(text.estimatedWorldBounds()) }) return false
        if (completedStrokes.value.any { stroke ->
                stroke.worldBoundsWithStrokeWidth()?.let { bounds -> candidate.intersects(bounds) } == true
            }
        ) return false
        if (widgets.value.any { widget ->
                widget.id != excludedWidgetId && candidate.intersects(widget.worldBounds())
            }
        ) return false
        return true
    }

    private fun animateCameraFlight(targetCamera: CameraState) {
        cancelCameraFlightAnimation()
        val start = viewportController.cameraState.value
        val target = targetCamera.withCurrentViewport(start)
        val centerDistance = distance(start.worldCenter, target.worldCenter)
        val scaleDistance = abs(start.scale - target.scale)
        if (centerDistance < SEARCH_FLIGHT_MIN_DISTANCE_WORLD && scaleDistance < SEARCH_FLIGHT_MIN_SCALE_DELTA) {
            viewportController.setCamera(target)
            return
        }
        val midCenter = lerp(start.worldCenter, target.worldCenter, SEARCH_FLIGHT_MID_CENTER_RATIO)
        val peakScale = WorldScreenTransformer.clampScale(
            max(start.scale, target.scale) * SEARCH_FLIGHT_ZOOM_MULTIPLIER,
        )
        cameraFlightJob = viewModelScope.launch(dispatchersProvider.main) {
            var flightStartNanos: Long? = null
            while (isActive) {
                val frameNanos = awaitFrame()
                val startNanos = flightStartNanos ?: frameNanos.also { flightStartNanos = it }
                val elapsedMs = ((frameNanos - startNanos).coerceAtLeast(0L)) / 1_000_000f
                val progress = (elapsedMs / SEARCH_FLIGHT_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                val flightCamera = interpolateFlightCamera(
                    start = start,
                    midCenter = midCenter,
                    target = target,
                    peakScale = peakScale,
                    progress = progress,
                )
                val current = viewportController.cameraState.value
                viewportController.setCamera(flightCamera.withCurrentViewport(current))
                if (progress >= 1f) {
                    break
                }
            }
            cameraFlightJob = null
        }
    }

    private fun cancelCameraFlightAnimation() {
        cameraFlightJob?.cancel()
        cameraFlightJob = null
    }

    private fun interpolateFlightCamera(
        start: CameraState,
        midCenter: WorldPoint,
        target: CameraState,
        peakScale: Float,
        progress: Float,
    ): CameraState {
        val center: WorldPoint
        val scale: Float
        if (progress <= SEARCH_FLIGHT_MID_PROGRESS) {
            val local = (progress / SEARCH_FLIGHT_MID_PROGRESS).coerceIn(0f, 1f)
            val eased = easeOutCubic(local)
            center = lerp(start.worldCenter, midCenter, eased)
            scale = lerp(start.scale, peakScale, eased)
        } else {
            val local = ((progress - SEARCH_FLIGHT_MID_PROGRESS) / (1f - SEARCH_FLIGHT_MID_PROGRESS))
                .coerceIn(0f, 1f)
            val eased = easeInOutCubic(local)
            center = lerp(midCenter, target.worldCenter, eased)
            scale = lerp(peakScale, target.scale, eased)
        }
        return target.copy(
            worldCenter = center,
            scale = scale,
        )
    }

    private fun currentObjectPosition(target: CanvasObjectDragTarget): WorldPoint? {
        return when (target) {
            is CanvasObjectDragTarget.Sticky -> stickyNotes.value.firstOrNull { note ->
                note.id == target.id
            }?.center

            is CanvasObjectDragTarget.Text -> textObjects.value.firstOrNull { text ->
                text.id == target.id
            }?.position

            is CanvasObjectDragTarget.Frame -> frameObjects.value.firstOrNull { frame ->
                frame.id == target.id
            }?.center

            is CanvasObjectDragTarget.Widget -> widgets.value.firstOrNull { widget ->
                widget.id == target.id
            }?.center
        }
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
        val excludedWidgetId = (excludedObject as? CanvasObjectDragTarget.Widget)?.id

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
        val widgetAnchors = widgets.value
            .asSequence()
            .filterNot { it.id == excludedWidgetId }
            .map { it.center }
            .toList()

        return buildList(iconAnchors.size + stickyAnchors.size + textAnchors.size + frameAnchors.size + widgetAnchors.size) {
            addAll(iconAnchors)
            addAll(stickyAnchors)
            addAll(textAnchors)
            addAll(frameAnchors)
            addAll(widgetAnchors)
        }
    }

    private fun clearSelection() {
        selectedObjects.value = CanvasSelectionUiState()
        selectionBounds.value = null
        selectionDraft.value = null
        activeSelectionResizeSession = null
    }

    private fun selectObjectsFullyInside(bounds: WorldBounds): CanvasSelectionUiState {
        val scale = viewportController.cameraState.value.scale
        val iconHalfSizeWorld = iconWorldSizeForScale(scale) / 2f
        val appPositions = currentAppPositionsByPackage()
        val selectedPackages = appPositions
            .asSequence()
            .filter { (_, position) ->
                val iconBounds = WorldBounds(
                    left = position.x - iconHalfSizeWorld,
                    top = position.y - iconHalfSizeWorld,
                    right = position.x + iconHalfSizeWorld,
                    bottom = position.y + iconHalfSizeWorld,
                )
                bounds.containsRect(iconBounds)
            }
            .map { (packageName, _) -> packageName }
            .toSet()
        val selectedFrames = frameObjects.value
            .asSequence()
            .filter { frame -> bounds.containsRect(frame.worldBounds()) }
            .map { frame -> frame.id }
            .toSet()
        val selectedTexts = textObjects.value
            .asSequence()
            .filter { text -> bounds.containsRect(text.estimatedWorldBounds()) }
            .map { text -> text.id }
            .toSet()
        val selectedStrokes = completedStrokes.value
            .asSequence()
            .filter { stroke ->
                stroke.points.isNotEmpty() && stroke.points.all { point -> bounds.contains(point) }
            }
            .map { stroke -> stroke.id }
            .toSet()
        return CanvasSelectionUiState(
            packageNames = selectedPackages,
            frameIds = selectedFrames,
            textIds = selectedTexts,
            strokeIds = selectedStrokes,
        )
    }

    private fun computeSelectionBounds(selection: CanvasSelectionUiState): CanvasSelectionBoundsUiState? {
        if (selection.isEmpty) return null
        val appPositions = currentAppPositionsByPackage()
        val scale = viewportController.cameraState.value.scale
        val iconHalfSizeWorld = iconWorldSizeForScale(scale) / 2f
        val bounds = buildList {
            selection.packageNames.forEach { packageName ->
                val position = appPositions[packageName] ?: return@forEach
                add(
                    WorldBounds(
                        left = position.x - iconHalfSizeWorld,
                        top = position.y - iconHalfSizeWorld,
                        right = position.x + iconHalfSizeWorld,
                        bottom = position.y + iconHalfSizeWorld,
                    ),
                )
            }
            frameObjects.value
                .asSequence()
                .filter { it.id in selection.frameIds }
                .forEach { add(it.worldBounds()) }
            textObjects.value
                .asSequence()
                .filter { it.id in selection.textIds }
                .forEach { add(it.estimatedWorldBounds()) }
            completedStrokes.value
                .asSequence()
                .filter { it.id in selection.strokeIds }
                .mapNotNull { it.worldBoundsWithStrokeWidth() }
                .forEach { add(it) }
        }
        if (bounds.isEmpty()) return null
        val left = bounds.minOf { it.left }
        val top = bounds.minOf { it.top }
        val right = bounds.maxOf { it.right }
        val bottom = bounds.maxOf { it.bottom }
        val hasIcons = selection.hasIcons
        return CanvasSelectionBoundsUiState(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            hasIcons = hasIcons,
            canResizeAndDelete = !hasIcons && !selection.isEmpty,
        )
    }

    private fun applySelectionTranslation(
        selection: CanvasSelectionUiState,
        deltaXWorld: Float,
        deltaYWorld: Float,
    ) {
        if (selection.packageNames.isNotEmpty()) {
            val appPositions = currentAppPositionsByPackage()
            committedDragPositions.update { overrides ->
                val next = overrides.toMutableMap()
                selection.packageNames.forEach { packageName ->
                    val current = next[packageName] ?: appPositions[packageName] ?: return@forEach
                    next[packageName] = WorldPoint(
                        x = current.x + deltaXWorld,
                        y = current.y + deltaYWorld,
                    )
                }
                next
            }
        }
        if (selection.frameIds.isNotEmpty()) {
            frameObjects.update { frames ->
                frames.map { current ->
                    if (current.id !in selection.frameIds) {
                        current
                    } else {
                        current.copy(
                            center = WorldPoint(
                                x = current.center.x + deltaXWorld,
                                y = current.center.y + deltaYWorld,
                            ),
                        )
                    }
                }
            }
        }
        if (selection.textIds.isNotEmpty()) {
            textObjects.update { texts ->
                texts.map { current ->
                    if (current.id !in selection.textIds) {
                        current
                    } else {
                        current.copy(
                            position = WorldPoint(
                                x = current.position.x + deltaXWorld,
                                y = current.position.y + deltaYWorld,
                            ),
                        )
                    }
                }
            }
        }
        if (selection.strokeIds.isNotEmpty()) {
            completedStrokes.update { strokes ->
                strokes.map { current ->
                    if (current.id !in selection.strokeIds) {
                        current
                    } else {
                        current.copy(
                            points = current.points.map { point ->
                                WorldPoint(
                                    x = point.x + deltaXWorld,
                                    y = point.y + deltaYWorld,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    fun onWidgetCatalogItemSelected(type: CanvasWidgetType) {
        if (activeTool.value != LauncherToolId.Widgets) return
        val widgetId = nextCanvasId(prefix = "widget")
        val widthWorld = CanvasEditDefaults.DEFAULT_WIDGET_WIDTH_WORLD
        val heightWorld = CanvasEditDefaults.DEFAULT_WIDGET_HEIGHT_WORLD
        val center = findFreeWidgetCenter(widthWorld = widthWorld, heightWorld = heightWorld)
        val widget = CanvasWidgetUiState(
            id = widgetId,
            type = type,
            center = center,
            widthWorld = widthWorld,
            heightWorld = heightWorld,
        )
        widgets.update { current -> current + widget }
        selectedFrameIdForResize.value = null
        activeFrameResizeSession = null
        selectedWidgetIdForResize.value = widgetId
        activeWidgetResizeSession = null
        persistWidget(widget)
    }

    fun onWidgetTap(widgetId: String) {
        if (activeTool.value != LauncherToolId.Widgets) return
        if (widgets.value.none { widget -> widget.id == widgetId }) return
        selectedFrameIdForResize.value = null
        activeFrameResizeSession = null
        selectedWidgetIdForResize.value = widgetId
    }

    fun onWidgetBackgroundTap() {
        if (activeTool.value != LauncherToolId.Widgets) return
        selectedWidgetIdForResize.value = null
        activeWidgetResizeSession = null
    }

    fun onWidgetDeleteSelected() {
        if (activeTool.value != LauncherToolId.Widgets) return
        val widgetId = selectedWidgetIdForResize.value ?: return
        deleteWidget(widgetId)
    }

    fun onWidgetResizeStart(handle: CanvasFrameResizeHandle) {
        if (activeTool.value != LauncherToolId.Widgets) return
        val widgetId = selectedWidgetIdForResize.value ?: return
        if (widgets.value.none { item -> item.id == widgetId }) return
        activeWidgetResizeSession = WidgetResizeSession(
            widgetId = widgetId,
            handle = handle,
        )
        selectedWidgetIdForResize.value = widgetId
        if (activeObjectDrag == CanvasObjectDragTarget.Widget(widgetId)) {
            activeObjectDrag = null
            activeObjectDragSession = null
        }
    }

    fun onWidgetResizeDrag(
        handle: CanvasFrameResizeHandle,
        delta: ScreenPoint,
    ) {
        if (activeTool.value != LauncherToolId.Widgets) return
        val session = activeWidgetResizeSession ?: return
        if (session.handle != handle) return
        val scale = viewportController.cameraState.value.scale
        if (scale <= 0f) return
        val widget = widgets.value.firstOrNull { item -> item.id == session.widgetId } ?: return
        val resized = widget.resizeByHandleDrag(
            handle = handle,
            deltaXWorld = delta.x / scale,
            deltaYWorld = delta.y / scale,
            minWidthWorld = CanvasEditDefaults.WIDGET_MIN_WIDTH_WORLD,
            minHeightWorld = CanvasEditDefaults.WIDGET_MIN_HEIGHT_WORLD,
        )
        widgets.update { items ->
            items.map { current ->
                if (current.id == session.widgetId) {
                    resized
                } else {
                    current
                }
            }
        }
    }

    fun onWidgetResizeEnd() {
        val session = activeWidgetResizeSession ?: return
        val updated = widgets.value.firstOrNull { widget -> widget.id == session.widgetId } ?: run {
            activeWidgetResizeSession = null
            return
        }
        persistWidget(updated)
        activeWidgetResizeSession = null
    }

    private fun persistSelectionTransform(includeIcons: Boolean) {
        val selection = selectedObjects.value
        if (selection.isEmpty) return
        if (selection.frameIds.isNotEmpty()) {
            frameObjects.value
                .asSequence()
                .filter { it.id in selection.frameIds }
                .forEach(::persistFrameObject)
        }
        if (selection.textIds.isNotEmpty()) {
            textObjects.value
                .asSequence()
                .filter { it.id in selection.textIds }
                .forEach(::persistTextObject)
        }
        if (selection.strokeIds.isNotEmpty()) {
            completedStrokes.value
                .asSequence()
                .filter { it.id in selection.strokeIds }
                .forEach(::persistStroke)
        }
        if (includeIcons && selection.packageNames.isNotEmpty()) {
            val appPositions = currentAppPositionsByPackage()
            viewModelScope.launch(dispatchersProvider.io) {
                selection.packageNames.forEach { packageName ->
                    val position = appPositions[packageName] ?: return@forEach
                    runCatching {
                        updateAppPositionUseCase(packageName, position)
                    }.onFailure { throwable ->
                        Log.w(TAG, "Failed to persist selection icon position for $packageName", throwable)
                    }
                }
            }
        }
    }

    private fun deleteSelectionWithoutIcons() {
        val selection = selectedObjects.value
        if (selection.isEmpty || selection.hasIcons) return
        val frameIds = selection.frameIds
        val textIds = selection.textIds
        val strokeIds = selection.strokeIds
        if (frameIds.isEmpty() && textIds.isEmpty() && strokeIds.isEmpty()) {
            clearSelection()
            return
        }

        frameObjects.update { frames -> frames.filterNot { it.id in frameIds } }
        textObjects.update { texts -> texts.filterNot { it.id in textIds } }
        completedStrokes.update { strokes -> strokes.filterNot { it.id in strokeIds } }
        if (selectedFrameIdForResize.value in frameIds) {
            selectedFrameIdForResize.value = null
        }
        if (activeFrameResizeSession?.frameId in frameIds) {
            activeFrameResizeSession = null
        }
        val inlineTarget = editInlineEditor.value.target
        val removedInlineTarget = when (inlineTarget) {
            is CanvasInlineEditorTarget.EditFrame -> inlineTarget.id in frameIds
            is CanvasInlineEditorTarget.EditText -> inlineTarget.id in textIds
            else -> false
        }
        if (removedInlineTarget) {
            editInlineEditor.value = CanvasInlineEditorUiState()
        }
        clearSelection()

        viewModelScope.launch(dispatchersProvider.io) {
            frameIds.forEach { frameId ->
                runCatching { canvasEditDao.deleteFrameObjectById(frameId) }
                    .onFailure { throwable ->
                        Log.w(TAG, "Failed to delete selected frame $frameId", throwable)
                    }
            }
            textIds.forEach { textId ->
                runCatching { canvasEditDao.deleteTextObjectById(textId) }
                    .onFailure { throwable ->
                        Log.w(TAG, "Failed to delete selected text $textId", throwable)
                    }
            }
            strokeIds.forEach { strokeId ->
                runCatching { canvasEditDao.deleteStrokeById(strokeId) }
                    .onFailure { throwable ->
                        Log.w(TAG, "Failed to delete selected stroke $strokeId", throwable)
                    }
            }
        }
    }

    private fun pruneSelectionToExistingObjects() {
        val current = selectedObjects.value
        if (current.isEmpty) {
            selectionBounds.value = null
            activeSelectionResizeSession = null
            return
        }
        val appPackages = appsState.value.mapTo(HashSet()) { it.packageName }
        val frameIds = frameObjects.value.mapTo(HashSet()) { it.id }
        val textIds = textObjects.value.mapTo(HashSet()) { it.id }
        val strokeIds = completedStrokes.value.mapTo(HashSet()) { it.id }
        val pruned = CanvasSelectionUiState(
            packageNames = current.packageNames.filterTo(LinkedHashSet()) { it in appPackages },
            frameIds = current.frameIds.filterTo(LinkedHashSet()) { it in frameIds },
            textIds = current.textIds.filterTo(LinkedHashSet()) { it in textIds },
            strokeIds = current.strokeIds.filterTo(LinkedHashSet()) { it in strokeIds },
        )
        if (pruned != current) {
            selectedObjects.value = pruned
            activeSelectionResizeSession = null
        }
        selectionBounds.value = computeSelectionBounds(pruned)
    }

    private fun transformFrameByBounds(
        frame: CanvasFrameObjectUiState,
        source: WorldBounds,
        target: WorldBounds,
    ): CanvasFrameObjectUiState {
        val original = frame.worldBounds()
        val mappedLeft = source.mapXTo(target, original.left)
        val mappedRight = source.mapXTo(target, original.right)
        val mappedTop = source.mapYTo(target, original.top)
        val mappedBottom = source.mapYTo(target, original.bottom)
        val width = (mappedRight - mappedLeft).coerceAtLeast(FRAME_RESIZE_MIN_WIDTH_WORLD)
        val height = (mappedBottom - mappedTop).coerceAtLeast(FRAME_RESIZE_MIN_HEIGHT_WORLD)
        return frame.copy(
            center = WorldPoint(
                x = (mappedLeft + mappedRight) / 2f,
                y = (mappedTop + mappedBottom) / 2f,
            ),
            widthWorld = width,
            heightWorld = height,
            colorArgb = frame.colorArgb,
            title = frame.title,
        )
    }

    private fun transformTextByBounds(
        text: CanvasTextObjectUiState,
        source: WorldBounds,
        target: WorldBounds,
        scaleX: Float,
        scaleY: Float,
    ): CanvasTextObjectUiState {
        val mappedPosition = source.mapPointTo(target, text.position)
        val scaleFactor = min(scaleX, scaleY)
        val mappedTextSize = (text.textSizeWorld * scaleFactor)
            .coerceIn(TEXT_RESIZE_MIN_SIZE_WORLD, TEXT_RESIZE_MAX_SIZE_WORLD)
        return text.copy(
            position = mappedPosition,
            textSizeWorld = mappedTextSize,
        )
    }

    private fun transformStrokeByBounds(
        stroke: CanvasStrokeUiState,
        source: WorldBounds,
        target: WorldBounds,
    ): CanvasStrokeUiState {
        return stroke.copy(
            points = stroke.points.map { point ->
                source.mapPointTo(target, point)
            },
        )
    }

    private fun currentAppPositionsByPackage(): Map<String, WorldPoint> {
        return DragPositionOverrides.apply(
            apps = appsState.value,
            overrides = committedDragPositions.value,
        ).associate { app -> app.packageName to app.position }
    }

    private fun iconWorldSizeForScale(scale: Float): Float {
        val safeScale = scale.coerceAtLeast(0.0001f)
        val iconPx = (CanvasConstants.Sizes.ICON_WORLD_SIZE * safeScale)
            .coerceIn(ICON_MIN_SIZE_PX, ICON_MAX_SIZE_PX)
        return iconPx / safeScale
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
        val persistedWidgets = canvasEditDao.getWidgets().mapNotNull { entity ->
            val type = entity.type.toCanvasWidgetTypeOrNull() ?: return@mapNotNull null
            CanvasWidgetUiState(
                id = entity.id,
                type = type,
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
        widgets.value = persistedWidgets
        frameDraft.value = null
        selectedFrameIdForResize.value = null
        selectedWidgetIdForResize.value = null
        activeFrameResizeSession = null
        activeWidgetResizeSession = null
        activeSelectionResizeSession = null
        selectionDraft.value = null
        selectedObjects.value = CanvasSelectionUiState()
        selectionBounds.value = null
        completedStrokes.value = persistedStrokes

        nextCanvasObjectId = max(
            nextCanvasObjectId,
            listOf(
                persistedStickyNotes.map { it.id },
                persistedTextObjects.map { it.id },
                persistedFrameObjects.map { it.id },
                persistedWidgets.map { it.id },
                persistedStrokes.map { it.id },
            )
                .flatten()
                .mapNotNull(::extractNumericSuffix)
                .maxOrNull()
                ?.plus(1)
                ?: 0L,
        )
    }

    private suspend fun refreshPersistedDecorations() {
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
        val persistedWidgets = canvasEditDao.getWidgets().mapNotNull { entity ->
            val type = entity.type.toCanvasWidgetTypeOrNull() ?: return@mapNotNull null
            CanvasWidgetUiState(
                id = entity.id,
                type = type,
                center = WorldPoint(entity.centerX, entity.centerY),
                widthWorld = entity.widthWorld,
                heightWorld = entity.heightWorld,
                colorArgb = entity.colorArgb,
            )
        }
        frameObjects.value = persistedFrameObjects
        widgets.value = persistedWidgets
        val frameIds = persistedFrameObjects.mapTo(HashSet()) { frame -> frame.id }
        selectedFrameIdForResize.value = selectedFrameIdForResize.value?.takeIf { frameId ->
            frameId in frameIds
        }
        if (activeFrameResizeSession?.frameId !in frameIds) {
            activeFrameResizeSession = null
        }
        val widgetIds = persistedWidgets.mapTo(HashSet()) { widget -> widget.id }
        selectedWidgetIdForResize.value = selectedWidgetIdForResize.value?.takeIf { widgetId ->
            widgetId in widgetIds
        }
        if (activeWidgetResizeSession?.widgetId !in widgetIds) {
            activeWidgetResizeSession = null
        }
        pruneSelectionToExistingObjects()
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

            is CanvasObjectDragTarget.Widget -> {
                widgets.value.firstOrNull { it.id == target.id }?.let(::persistWidget)
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

    private fun persistWidget(widget: CanvasWidgetUiState) {
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching {
                canvasEditDao.upsertWidget(
                    CanvasWidgetEntity(
                        id = widget.id,
                        type = widget.type.name,
                        centerX = widget.center.x,
                        centerY = widget.center.y,
                        widthWorld = widget.widthWorld,
                        heightWorld = widget.heightWorld,
                        colorArgb = widget.colorArgb,
                    ),
                )
            }.onFailure { throwable ->
                Log.w(TAG, "Failed to persist widget ${widget.id}", throwable)
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
        pruneSelectionToExistingObjects()
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { canvasEditDao.deleteTextObjectById(id) }
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to delete text object $id", throwable)
                }
        }
    }

    private fun deleteFrameObject(id: String) {
        frameObjects.update { frames -> frames.filterNot { frame -> frame.id == id } }
        if (selectedFrameIdForResize.value == id) {
            selectedFrameIdForResize.value = null
        }
        if (activeFrameResizeSession?.frameId == id) {
            activeFrameResizeSession = null
        }
        pruneSelectionToExistingObjects()
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { canvasEditDao.deleteFrameObjectById(id) }
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to delete frame object $id", throwable)
                }
        }
    }

    private fun deleteWidget(id: String) {
        widgets.update { current -> current.filterNot { widget -> widget.id == id } }
        if (selectedWidgetIdForResize.value == id) {
            selectedWidgetIdForResize.value = null
        }
        if (activeWidgetResizeSession?.widgetId == id) {
            activeWidgetResizeSession = null
        }
        viewModelScope.launch(dispatchersProvider.io) {
            runCatching { canvasEditDao.deleteWidgetById(id) }
                .onFailure { throwable ->
                    Log.w(TAG, "Failed to delete widget $id", throwable)
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
        @StringRes titleResId: Int?,
        @StringRes placeholderResId: Int?,
        value: String,
        target: CanvasInlineEditorTarget,
        isDraft: Boolean = false,
    ) {
        editInlineEditor.value = CanvasInlineEditorUiState(
            isVisible = true,
            titleResId = titleResId,
            placeholderResId = placeholderResId,
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
            scheduleIconWarmup(appsState.value)
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeViewportIconWarmup() {
        viewModelScope.launch(dispatchersProvider.io) {
            combine(
                appsState,
                viewportController.cameraState,
            ) { apps, camera ->
                apps to camera.worldCenter
            }
                .debounce(ICON_VIEWPORT_WARMUP_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { (apps, worldCenter) ->
                    if (apps.isEmpty()) return@collect
                    val viewportPackages = prioritizePackagesAroundCenter(
                        apps = apps,
                        center = worldCenter,
                        limit = ICON_VIEWPORT_PRIORITY_COUNT,
                    )
                    runCatching {
                        iconCacheGateway.preload(viewportPackages)
                    }.onFailure { throwable ->
                        Log.w(TAG, "Viewport icon warmup failed", throwable)
                    }
                }
        }
    }

    private fun scheduleIconWarmup(apps: List<CanvasApp>) {
        if (apps.isEmpty()) return
        iconWarmupJob?.cancel()
        val center = viewportController.cameraState.value.worldCenter
        iconWarmupJob = viewModelScope.launch(dispatchersProvider.io) {
            val orderedPackages = prioritizePackagesAroundCenter(
                apps = apps,
                center = center,
            )
            if (orderedPackages.isEmpty()) return@launch
            val priority = orderedPackages.take(ICON_INITIAL_PRIORITY_COUNT)
            val background = orderedPackages.drop(ICON_INITIAL_PRIORITY_COUNT)
            runCatching {
                iconCacheGateway.preload(priority)
            }.onFailure { throwable ->
                Log.w(TAG, "Priority icon warmup failed", throwable)
            }
            background.chunked(ICON_BACKGROUND_WARMUP_BATCH_SIZE).forEach { batch ->
                if (!isActive) return@launch
                runCatching {
                    iconCacheGateway.preload(batch)
                }.onFailure { throwable ->
                    Log.w(TAG, "Background icon warmup batch failed", throwable)
                }
            }
        }
    }

    private fun prioritizePackagesAroundCenter(
        apps: List<CanvasApp>,
        center: WorldPoint,
        limit: Int = Int.MAX_VALUE,
    ): List<String> {
        return apps.asSequence()
            .sortedBy { app ->
                val dx = app.position.x - center.x
                val dy = app.position.y - center.y
                dx * dx + dy * dy
            }
            .take(limit)
            .map { app -> app.packageName }
            .toList()
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

    override fun onCleared() {
        iconWarmupJob?.cancel()
        cancelCameraFlightAnimation()
        super.onCleared()
    }

    private companion object {
        private const val TAG = "LauncherViewModel"
        private const val ICON_SNAP_THRESHOLD_SCREEN_PX = 9f
        private const val ICON_SNAP_AXIS_INFLUENCE_SCREEN_PX = 88f
        private const val OBJECT_SNAP_THRESHOLD_SCREEN_PX = 12f
        private const val OBJECT_SNAP_AXIS_INFLUENCE_SCREEN_PX = 120f
        private const val ICON_INITIAL_PRIORITY_COUNT = 54
        private const val ICON_BACKGROUND_WARMUP_BATCH_SIZE = 32
        private const val ICON_VIEWPORT_PRIORITY_COUNT = 40
        private const val ICON_VIEWPORT_WARMUP_DEBOUNCE_MS = 220L
        private const val SEARCH_BASE_OCCLUSION_PX = 64f
        private const val SEARCH_MATCH_DEBOUNCE_MS = 28L
        private const val SEARCH_FOCUS_OFFSET_MULTIPLIER = 0.20f
        private const val SEARCH_FOCUS_EXTRA_GAP_PX = 16f
        private const val SEARCH_MIN_FOCUS_LIFT_PX = 32f
        private const val SEARCH_MAX_FOCUS_LIFT_RATIO = 0.22f
        private const val SEARCH_KEYBOARD_TARGET_SCREEN_Y_RATIO = 0.25f
        private const val SEARCH_MIN_FOCUS_SCALE = 1.08f
        private const val SEARCH_FLIGHT_DURATION_MS = 620L
        private const val SEARCH_FLIGHT_MID_PROGRESS = 0.56f
        private const val SEARCH_FLIGHT_MID_CENTER_RATIO = 0.56f
        private const val SEARCH_FLIGHT_ZOOM_MULTIPLIER = 1.16f
        private const val SEARCH_FLIGHT_MIN_DISTANCE_WORLD = 1.4f
        private const val SEARCH_FLIGHT_MIN_SCALE_DELTA = 0.012f
        private const val FRAME_RESIZE_MIN_WIDTH_WORLD = 120f
        private const val FRAME_RESIZE_MIN_HEIGHT_WORLD = 96f
        private const val BRUSH_MIN_POINT_DISTANCE_WORLD = 2f
        private const val ERASER_RADIUS_SCREEN_PX = 22f
        private const val ICON_MIN_SIZE_PX = 32f
        private const val ICON_MAX_SIZE_PX = 220f
        private const val SELECTION_MIN_SIZE_WORLD = 14f
        private const val SELECTION_RESIZE_MIN_WIDTH_WORLD = 90f
        private const val SELECTION_RESIZE_MIN_HEIGHT_WORLD = 72f
        private const val TEXT_RESIZE_MIN_SIZE_WORLD = 10f
        private const val TEXT_RESIZE_MAX_SIZE_WORLD = 224f
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
    data class Widget(val id: String) : CanvasObjectDragTarget
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

private data class FrameDecorationsState(
    val frames: List<CanvasFrameObjectUiState> = emptyList(),
    val frameDraft: CanvasFrameDraftUiState? = null,
    val selectedFrameIdForResize: String? = null,
    val selectedWidgetIdForResize: String? = null,
    val selectionDraft: CanvasSelectionDraftUiState? = null,
    val selectionBounds: CanvasSelectionBoundsUiState? = null,
    val hasActiveSelection: Boolean = false,
)

private data class CanvasDecorationsState(
    val frames: List<CanvasFrameObjectUiState> = emptyList(),
    val frameDraft: CanvasFrameDraftUiState? = null,
    val selectedFrameIdForResize: String? = null,
    val selectedWidgetIdForResize: String? = null,
    val selectionDraft: CanvasSelectionDraftUiState? = null,
    val selectionBounds: CanvasSelectionBoundsUiState? = null,
    val hasActiveSelection: Boolean = false,
    val notes: List<CanvasStickyNoteUiState> = emptyList(),
    val texts: List<CanvasTextObjectUiState> = emptyList(),
    val widgets: List<CanvasWidgetUiState> = emptyList(),
    val strokes: List<CanvasStrokeUiState> = emptyList(),
    val guides: List<CanvasSnapGuideUiState> = emptyList(),
)

private data class AppsListEntry(
    val packageName: String,
    val label: String,
)

private data class FrameResizeSession(
    val frameId: String,
    val handle: CanvasFrameResizeHandle,
)

private data class WidgetResizeSession(
    val widgetId: String,
    val handle: CanvasFrameResizeHandle,
)

private data class ObjectDragSession(
    val target: CanvasObjectDragTarget,
    val initialPosition: WorldPoint,
    val snapAnchors: List<WorldPoint>,
    var accumulatedDeltaXWorld: Float = 0f,
    var accumulatedDeltaYWorld: Float = 0f,
)

private data class SelectionResizeSession(
    val handle: CanvasFrameResizeHandle,
    val selection: CanvasSelectionUiState,
    val initialBounds: WorldBounds,
    val initialFrames: Map<String, CanvasFrameObjectUiState>,
    val initialTexts: Map<String, CanvasTextObjectUiState>,
    val initialStrokes: Map<String, CanvasStrokeUiState>,
    var accumulatedDeltaXWorld: Float = 0f,
    var accumulatedDeltaYWorld: Float = 0f,
)

private fun CanvasFrameObjectUiState.resizeByHandleDrag(
    handle: CanvasFrameResizeHandle,
    deltaXWorld: Float,
    deltaYWorld: Float,
    minWidthWorld: Float,
    minHeightWorld: Float,
): CanvasFrameObjectUiState {
    val resized = resizeWorldRectByHandleDrag(
        center = center,
        widthWorld = widthWorld,
        heightWorld = heightWorld,
        handle = handle,
        deltaXWorld = deltaXWorld,
        deltaYWorld = deltaYWorld,
        minWidthWorld = minWidthWorld,
        minHeightWorld = minHeightWorld,
    )
    return copy(
        center = resized.center,
        widthWorld = resized.widthWorld,
        heightWorld = resized.heightWorld,
    )
}

private fun CanvasWidgetUiState.resizeByHandleDrag(
    handle: CanvasFrameResizeHandle,
    deltaXWorld: Float,
    deltaYWorld: Float,
    minWidthWorld: Float,
    minHeightWorld: Float,
): CanvasWidgetUiState {
    val resized = resizeWorldRectByHandleDrag(
        center = center,
        widthWorld = widthWorld,
        heightWorld = heightWorld,
        handle = handle,
        deltaXWorld = deltaXWorld,
        deltaYWorld = deltaYWorld,
        minWidthWorld = minWidthWorld,
        minHeightWorld = minHeightWorld,
    )
    return copy(
        center = resized.center,
        widthWorld = resized.widthWorld,
        heightWorld = resized.heightWorld,
    )
}

private fun resizeWorldRectByHandleDrag(
    center: WorldPoint,
    widthWorld: Float,
    heightWorld: Float,
    handle: CanvasFrameResizeHandle,
    deltaXWorld: Float,
    deltaYWorld: Float,
    minWidthWorld: Float,
    minHeightWorld: Float,
): ResizedWorldRect {
    var nextWidth = widthWorld
    var nextHeight = heightWorld
    var centerShiftX = 0f
    var centerShiftY = 0f

    fun applyLeftEdge(delta: Float) {
        val rawWidth = nextWidth - delta
        val clamped = rawWidth.coerceAtLeast(minWidthWorld)
        val appliedEdgeDelta = nextWidth - clamped
        nextWidth = clamped
        centerShiftX += appliedEdgeDelta / 2f
    }

    fun applyRightEdge(delta: Float) {
        val rawWidth = nextWidth + delta
        val clamped = rawWidth.coerceAtLeast(minWidthWorld)
        val appliedEdgeDelta = clamped - nextWidth
        nextWidth = clamped
        centerShiftX += appliedEdgeDelta / 2f
    }

    fun applyTopEdge(delta: Float) {
        val rawHeight = nextHeight - delta
        val clamped = rawHeight.coerceAtLeast(minHeightWorld)
        val appliedEdgeDelta = nextHeight - clamped
        nextHeight = clamped
        centerShiftY += appliedEdgeDelta / 2f
    }

    fun applyBottomEdge(delta: Float) {
        val rawHeight = nextHeight + delta
        val clamped = rawHeight.coerceAtLeast(minHeightWorld)
        val appliedEdgeDelta = clamped - nextHeight
        nextHeight = clamped
        centerShiftY += appliedEdgeDelta / 2f
    }

    when (handle) {
        CanvasFrameResizeHandle.Left -> applyLeftEdge(deltaXWorld)
        CanvasFrameResizeHandle.TopLeft -> {
            applyLeftEdge(deltaXWorld)
            applyTopEdge(deltaYWorld)
        }

        CanvasFrameResizeHandle.Top -> applyTopEdge(deltaYWorld)
        CanvasFrameResizeHandle.TopRight -> {
            applyRightEdge(deltaXWorld)
            applyTopEdge(deltaYWorld)
        }

        CanvasFrameResizeHandle.Right -> applyRightEdge(deltaXWorld)
        CanvasFrameResizeHandle.BottomRight -> {
            applyRightEdge(deltaXWorld)
            applyBottomEdge(deltaYWorld)
        }

        CanvasFrameResizeHandle.Bottom -> applyBottomEdge(deltaYWorld)
        CanvasFrameResizeHandle.BottomLeft -> {
            applyLeftEdge(deltaXWorld)
            applyBottomEdge(deltaYWorld)
        }
    }

    return ResizedWorldRect(
        center = WorldPoint(
            x = center.x + centerShiftX,
            y = center.y + centerShiftY,
        ),
        widthWorld = nextWidth,
        heightWorld = nextHeight,
    )
}

private data class ResizedWorldRect(
    val center: WorldPoint,
    val widthWorld: Float,
    val heightWorld: Float,
)

private data class WorldBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float
        get() = (right - left).coerceAtLeast(0.0001f)
    val height: Float
        get() = (bottom - top).coerceAtLeast(0.0001f)

    fun contains(point: WorldPoint): Boolean {
        return point.x in left..right && point.y in top..bottom
    }

    fun containsRect(other: WorldBounds): Boolean {
        return other.left >= left &&
            other.right <= right &&
            other.top >= top &&
            other.bottom <= bottom
    }

    fun intersects(other: WorldBounds): Boolean {
        return left < other.right &&
            right > other.left &&
            top < other.bottom &&
            bottom > other.top
    }

    fun offset(deltaX: Float, deltaY: Float): WorldBounds {
        return copy(
            left = left + deltaX,
            top = top + deltaY,
            right = right + deltaX,
            bottom = bottom + deltaY,
        )
    }

    fun resizeByHandle(
        handle: CanvasFrameResizeHandle,
        deltaXWorld: Float,
        deltaYWorld: Float,
        minWidthWorld: Float,
        minHeightWorld: Float,
    ): WorldBounds {
        var nextLeft = left
        var nextTop = top
        var nextRight = right
        var nextBottom = bottom

        when (handle) {
            CanvasFrameResizeHandle.Left -> {
                nextLeft = (left + deltaXWorld).coerceAtMost(right - minWidthWorld)
            }

            CanvasFrameResizeHandle.TopLeft -> {
                nextLeft = (left + deltaXWorld).coerceAtMost(right - minWidthWorld)
                nextTop = (top + deltaYWorld).coerceAtMost(bottom - minHeightWorld)
            }

            CanvasFrameResizeHandle.Top -> {
                nextTop = (top + deltaYWorld).coerceAtMost(bottom - minHeightWorld)
            }

            CanvasFrameResizeHandle.TopRight -> {
                nextTop = (top + deltaYWorld).coerceAtMost(bottom - minHeightWorld)
                nextRight = (right + deltaXWorld).coerceAtLeast(left + minWidthWorld)
            }

            CanvasFrameResizeHandle.Right -> {
                nextRight = (right + deltaXWorld).coerceAtLeast(left + minWidthWorld)
            }

            CanvasFrameResizeHandle.BottomRight -> {
                nextRight = (right + deltaXWorld).coerceAtLeast(left + minWidthWorld)
                nextBottom = (bottom + deltaYWorld).coerceAtLeast(top + minHeightWorld)
            }

            CanvasFrameResizeHandle.Bottom -> {
                nextBottom = (bottom + deltaYWorld).coerceAtLeast(top + minHeightWorld)
            }

            CanvasFrameResizeHandle.BottomLeft -> {
                nextLeft = (left + deltaXWorld).coerceAtMost(right - minWidthWorld)
                nextBottom = (bottom + deltaYWorld).coerceAtLeast(top + minHeightWorld)
            }
        }

        return WorldBounds(
            left = nextLeft,
            top = nextTop,
            right = nextRight,
            bottom = nextBottom,
        )
    }

    fun mapPointTo(target: WorldBounds, point: WorldPoint): WorldPoint {
        val normalizedX = ((point.x - left) / width).coerceIn(-6f, 6f)
        val normalizedY = ((point.y - top) / height).coerceIn(-6f, 6f)
        return WorldPoint(
            x = target.left + normalizedX * target.width,
            y = target.top + normalizedY * target.height,
        )
    }

    fun mapXTo(target: WorldBounds, x: Float): Float {
        val normalized = ((x - left) / width).coerceIn(-6f, 6f)
        return target.left + normalized * target.width
    }

    fun mapYTo(target: WorldBounds, y: Float): Float {
        val normalized = ((y - top) / height).coerceIn(-6f, 6f)
        return target.top + normalized * target.height
    }
}

private fun worldBoundsOfCorners(a: WorldPoint, b: WorldPoint): WorldBounds {
    return WorldBounds(
        left = min(a.x, b.x),
        top = min(a.y, b.y),
        right = max(a.x, b.x),
        bottom = max(a.y, b.y),
    )
}

private fun CanvasFrameObjectUiState.worldBounds(): WorldBounds {
    return WorldBounds(
        left = center.x - widthWorld / 2f,
        top = center.y - heightWorld / 2f,
        right = center.x + widthWorld / 2f,
        bottom = center.y + heightWorld / 2f,
    )
}

private fun CanvasWidgetUiState.worldBounds(): WorldBounds {
    return WorldBounds(
        left = center.x - widthWorld / 2f,
        top = center.y - heightWorld / 2f,
        right = center.x + widthWorld / 2f,
        bottom = center.y + heightWorld / 2f,
    )
}

private fun CanvasStickyNoteUiState.worldBounds(): WorldBounds {
    return WorldBounds(
        left = center.x - sizeWorld / 2f,
        top = center.y - sizeWorld / 2f,
        right = center.x + sizeWorld / 2f,
        bottom = center.y + sizeWorld / 2f,
    )
}

private fun CanvasTextObjectUiState.estimatedWorldBounds(): WorldBounds {
    val safeText = text.ifBlank { "A" }
    val lines = safeText.split('\n').ifEmpty { listOf(safeText) }
    val longestLine = lines.maxOfOrNull { it.length }?.coerceAtLeast(1) ?: 1
    val lineCount = lines.size.coerceAtLeast(1)
    val estimatedWidth = (textSizeWorld * longestLine * TEXT_ESTIMATED_WIDTH_FACTOR)
        .coerceAtLeast(textSizeWorld * TEXT_ESTIMATED_MIN_WIDTH_MULTIPLIER)
    val estimatedHeight = (textSizeWorld * lineCount * TEXT_ESTIMATED_HEIGHT_FACTOR)
        .coerceAtLeast(textSizeWorld * 1.12f)
    return WorldBounds(
        left = position.x - estimatedWidth / 2f,
        top = position.y - estimatedHeight / 2f,
        right = position.x + estimatedWidth / 2f,
        bottom = position.y + estimatedHeight / 2f,
    )
}

private fun CanvasStrokeUiState.worldBoundsWithStrokeWidth(): WorldBounds? {
    if (points.isEmpty()) return null
    val padding = (widthWorld / 2f).coerceAtLeast(1f)
    val left = points.minOf { it.x } - padding
    val right = points.maxOf { it.x } + padding
    val top = points.minOf { it.y } - padding
    val bottom = points.maxOf { it.y } + padding
    return WorldBounds(left = left, top = top, right = right, bottom = bottom)
}

private fun CanvasSelectionBoundsUiState.offset(deltaX: Float, deltaY: Float): CanvasSelectionBoundsUiState {
    return copy(
        left = left + deltaX,
        top = top + deltaY,
        right = right + deltaX,
        bottom = bottom + deltaY,
    )
}

private fun lerp(start: Float, end: Float, amount: Float): Float {
    return start + (end - start) * amount
}

private fun lerp(start: WorldPoint, end: WorldPoint, amount: Float): WorldPoint {
    return WorldPoint(
        x = lerp(start.x, end.x, amount),
        y = lerp(start.y, end.y, amount),
    )
}

private fun distance(a: WorldPoint, b: WorldPoint): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun easeOutCubic(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return 1f - (1f - x) * (1f - x) * (1f - x)
}

private fun easeInOutCubic(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return if (x < 0.5f) {
        4f * x * x * x
    } else {
        1f - ((-2f * x + 2f).let { it * it * it } / 2f)
    }
}

private const val TEXT_ESTIMATED_WIDTH_FACTOR = 0.56f
private const val TEXT_ESTIMATED_MIN_WIDTH_MULTIPLIER = 1.7f
private const val TEXT_ESTIMATED_HEIGHT_FACTOR = 1.22f

private fun String.toCanvasWidgetTypeOrNull(): CanvasWidgetType? {
    return runCatching { CanvasWidgetType.valueOf(this) }.getOrNull()
}
