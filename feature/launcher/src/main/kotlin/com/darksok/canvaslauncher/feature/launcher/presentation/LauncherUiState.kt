package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.feature.canvas.CanvasRenderableApp

data class LauncherUiState(
    val cameraState: CameraState = CameraState(),
    val visibleApps: List<CanvasRenderableApp> = emptyList(),
    val allAppPositions: List<WorldPoint> = emptyList(),
    val frames: List<CanvasFrameObjectUiState> = emptyList(),
    val frameDraft: CanvasFrameDraftUiState? = null,
    val selectedFrameIdForResize: String? = null,
    val selectionDraft: CanvasSelectionDraftUiState? = null,
    val selectionBounds: CanvasSelectionBoundsUiState? = null,
    val hasActiveSelection: Boolean = false,
    val widgets: List<CanvasWidgetUiState> = emptyList(),
    val selectedWidgetId: String? = null,
    val strokes: List<CanvasStrokeUiState> = emptyList(),
    val stickyNotes: List<CanvasStickyNoteUiState> = emptyList(),
    val textObjects: List<CanvasTextObjectUiState> = emptyList(),
    val snapGuides: List<CanvasSnapGuideUiState> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lightPalette: LightThemePalette = LightThemePalette.SKY_BREEZE,
    val darkPalette: DarkThemePalette = DarkThemePalette.MIDNIGHT_BLUE,
    val toolsState: ToolsUiState = ToolsUiState(),
    val draggingPackageName: String? = null,
    val isInitialized: Boolean = false,
)
