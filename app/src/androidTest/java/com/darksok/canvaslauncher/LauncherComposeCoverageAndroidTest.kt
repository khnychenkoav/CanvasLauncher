package com.darksok.canvaslauncher

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.ui.theme.CanvasLauncherTheme
import com.darksok.canvaslauncher.feature.launcher.R
import com.darksok.canvaslauncher.feature.launcher.presentation.AppsListItemUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.AppsListScreen
import com.darksok.canvaslauncher.feature.launcher.presentation.AppsListUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasEditLayer
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasEditToolId
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasFrameResizeHandle
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasInlineEditorTarget
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasInlineEditorUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasSelectionBoundsUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasSelectionDraftUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasFrameObjectUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasStickyNoteUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasStrokeUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasTextObjectUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasWidgetUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.CanvasWidgetType
import com.darksok.canvaslauncher.feature.launcher.presentation.EditUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.LauncherToolId
import com.darksok.canvaslauncher.feature.launcher.presentation.LauncherToolsOverlay
import com.darksok.canvaslauncher.feature.launcher.presentation.SearchUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.ToolsUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.WidgetCatalogItemUiState
import com.darksok.canvaslauncher.feature.launcher.presentation.WidgetsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherComposeCoverageAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun appsListScreenShowsEmptyState() {
        val emptyText = composeRule.activity.getString(R.string.apps_list_empty)

        composeRule.setContent {
            CanvasLauncherTheme {
                AppsListScreen(
                    state = AppsListUiState(items = emptyList()),
                    onQueryChanged = { _ -> },
                    onAppClick = { _ -> },
                    onShowOnCanvas = { _ -> },
                    onRequestUninstall = { _ -> },
                    onClose = {},
                )
            }
        }

        composeRule.onNodeWithText(emptyText).assertExists()
    }

    @Test
    fun appsListScreenHandlesClickAndContextActions() {
        val closeText = composeRule.activity.getString(R.string.apps_list_close)
        val showOnCanvasText = composeRule.activity.getString(R.string.apps_list_menu_show_canvas)
        val uninstallText = composeRule.activity.getString(R.string.apps_list_menu_uninstall)

        val appClickPackages = mutableListOf<String>()
        val showOnCanvasPackages = mutableListOf<String>()
        val uninstallPackages = mutableListOf<String>()
        var closeClicks = 0

        composeRule.setContent {
            CanvasLauncherTheme {
                AppsListScreen(
                    state = AppsListUiState(
                        items = listOf(
                            AppsListItemUiState(
                                packageName = "pkg.maps",
                                label = "Maps",
                                icon = null,
                            ),
                        ),
                    ),
                    onQueryChanged = { _ -> },
                    onAppClick = { packageName -> appClickPackages += packageName },
                    onShowOnCanvas = { packageName -> showOnCanvasPackages += packageName },
                    onRequestUninstall = { packageName -> uninstallPackages += packageName },
                    onClose = { closeClicks++ },
                )
            }
        }

        composeRule.onNodeWithText("Maps").performClick()
        composeRule.onNodeWithText(closeText).performClick()
        composeRule.onNodeWithText("pkg.maps", useUnmergedTree = true).performTouchInput { longClick() }
        composeRule.onNodeWithText(showOnCanvasText).assertExists().performClick()
        composeRule.onNodeWithText("pkg.maps", useUnmergedTree = true).performTouchInput { longClick() }
        composeRule.onNodeWithText(uninstallText).assertExists().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("pkg.maps"), appClickPackages)
            assertEquals(listOf("pkg.maps"), showOnCanvasPackages)
            assertEquals(listOf("pkg.maps"), uninstallPackages)
            assertEquals(1, closeClicks)
        }
    }

    @Test
    fun launcherToolsOverlaySearchActionsInvokeCallbacks() {
        val launchText = composeRule.activity.getString(R.string.search_launch_top_match, "Maps")
        val callLabel = composeRule.activity.getString(R.string.search_call_button_label)
        val callText = composeRule.activity.getString(R.string.search_call_contact, callLabel, "Alice")
        var launchClicks = 0
        var callClicks = 0

        composeRule.setContent {
            CanvasLauncherTheme {
                LauncherToolsOverlay(
                    toolsState = ToolsUiState(
                        isExpanded = true,
                        activeTool = LauncherToolId.Search,
                        search = SearchUiState(
                            topMatchPackageName = "pkg.maps",
                            topMatchLabel = "Maps",
                            showLaunchAction = true,
                            topContactLabel = "Alice",
                            topContactDialNumber = "+79990000000",
                            showCallContactAction = true,
                        ),
                    ),
                    onToolsToggle = {},
                    onToolSelected = { _ -> },
                    onSearchQueryChanged = { _ -> },
                    onSearchActionClick = {},
                    onSearchSubmit = {},
                    onSearchOpenInBrowser = { _ -> },
                    onSearchCallContact = { callClicks++ },
                    onSearchLaunchTopMatch = { launchClicks++ },
                    onSearchClose = {},
                    onSearchOcclusionChanged = { _ -> },
                    onSearchKeyboardVisibilityChanged = { _ -> },
                    onEditClose = {},
                    onEditToolSelected = { _ -> },
                    onEditColorSelected = { _ -> },
                    onEditBrushSizeStep = { _ -> },
                    onEditTextSizeStep = { _ -> },
                    onEditInlineEditorValueChanged = { _ -> },
                    onEditInlineEditorConfirm = {},
                    onEditInlineEditorCancel = {},
                    onEditUndo = {},
                    onEditClearCustomElements = {},
                    onWidgetsClose = {},
                    onWidgetCatalogItemSelected = { _ -> },
                )
            }
        }

        composeRule.onNodeWithText(launchText).assertExists().performClick()
        composeRule.onNodeWithText(callText).assertExists().performClick()
        composeRule.runOnIdle {
            assertEquals(1, launchClicks)
            assertEquals(1, callClicks)
        }
    }

    @Test
    fun launcherToolsOverlayWidgetsSelectsCatalogItem() {
        var selectedWidget: CanvasWidgetType? = null
        val weatherTag = "widget_catalog_item_${CanvasWidgetType.Weather.name}"
        val calendarTag = "widget_catalog_item_${CanvasWidgetType.Calendar.name}"

        composeRule.setContent {
            CanvasLauncherTheme {
                LauncherToolsOverlay(
                    toolsState = ToolsUiState(
                        isExpanded = true,
                        activeTool = LauncherToolId.Widgets,
                        widgets = WidgetsUiState(
                            items = listOf(
                                WidgetCatalogItemUiState(
                                    id = "weather",
                                    titleResId = R.string.widget_catalog_weather_title,
                                    subtitleResId = R.string.widget_catalog_weather_subtitle,
                                    widgetType = CanvasWidgetType.Weather,
                                ),
                                WidgetCatalogItemUiState(
                                    id = "calendar",
                                    titleResId = R.string.widget_catalog_calendar_title,
                                    subtitleResId = R.string.widget_catalog_calendar_subtitle,
                                    widgetType = CanvasWidgetType.Calendar,
                                ),
                            ),
                        ),
                    ),
                    onToolsToggle = {},
                    onToolSelected = { _ -> },
                    onSearchQueryChanged = { _ -> },
                    onSearchActionClick = {},
                    onSearchSubmit = {},
                    onSearchOpenInBrowser = { _ -> },
                    onSearchCallContact = {},
                    onSearchLaunchTopMatch = {},
                    onSearchClose = {},
                    onSearchOcclusionChanged = { _ -> },
                    onSearchKeyboardVisibilityChanged = { _ -> },
                    onEditClose = {},
                    onEditToolSelected = { _ -> },
                    onEditColorSelected = { _ -> },
                    onEditBrushSizeStep = { _ -> },
                    onEditTextSizeStep = { _ -> },
                    onEditInlineEditorValueChanged = { _ -> },
                    onEditInlineEditorConfirm = {},
                    onEditInlineEditorCancel = {},
                    onEditUndo = {},
                    onEditClearCustomElements = {},
                    onWidgetsClose = {},
                    onWidgetCatalogItemSelected = { widgetType -> selectedWidget = widgetType },
                )
            }
        }

        composeRule.onNodeWithTag(weatherTag).assertExists().performClick()
        composeRule.runOnIdle {
            assertEquals(CanvasWidgetType.Weather, selectedWidget)
        }

        composeRule.onNodeWithTag(calendarTag).assertExists().performClick()
        composeRule.runOnIdle {
            assertEquals(CanvasWidgetType.Calendar, selectedWidget)
        }
    }

    @Test
    fun launcherToolsOverlayEditModeShowsBrushControlsAndClearAction() {
        val clearText = composeRule.activity.getString(R.string.edit_clear_custom_elements)
        val brushValueText = composeRule.activity.getString(R.string.edit_brush_size_value, 18)
        var clearClicks = 0

        composeRule.setContent {
            CanvasLauncherTheme {
                LauncherToolsOverlay(
                    toolsState = ToolsUiState(
                        isExpanded = true,
                        activeTool = LauncherToolId.Edit,
                        edit = EditUiState(
                            selectedTool = CanvasEditToolId.Brush,
                            brushWidthWorld = 18f,
                            inlineEditor = CanvasInlineEditorUiState(
                                isVisible = true,
                                titleResId = R.string.edit_inline_title_text,
                                placeholderResId = R.string.edit_inline_placeholder_text,
                                target = CanvasInlineEditorTarget.NewText(WorldPoint(0f, 0f)),
                            ),
                            canUndo = true,
                        ),
                    ),
                    onToolsToggle = {},
                    onToolSelected = { _ -> },
                    onSearchQueryChanged = { _ -> },
                    onSearchActionClick = {},
                    onSearchSubmit = {},
                    onSearchOpenInBrowser = { _ -> },
                    onSearchCallContact = {},
                    onSearchLaunchTopMatch = {},
                    onSearchClose = {},
                    onSearchOcclusionChanged = { _ -> },
                    onSearchKeyboardVisibilityChanged = { _ -> },
                    onEditClose = {},
                    onEditToolSelected = { _ -> },
                    onEditColorSelected = { _ -> },
                    onEditBrushSizeStep = { _ -> },
                    onEditTextSizeStep = { _ -> },
                    onEditInlineEditorValueChanged = { _ -> },
                    onEditInlineEditorConfirm = {},
                    onEditInlineEditorCancel = {},
                    onEditUndo = {},
                    onEditClearCustomElements = { clearClicks++ },
                    onWidgetsClose = {},
                    onWidgetCatalogItemSelected = { _ -> },
                )
            }
        }

        composeRule.onNodeWithText(brushValueText).assertExists()
        composeRule.onNodeWithText(clearText).assertExists().performClick()
        composeRule.runOnIdle {
            assertEquals(1, clearClicks)
        }
    }

    @Test
    fun canvasEditLayerSelectionDeleteButtonInvokesCallback() {
        val deleteSelectionDescription =
            composeRule.activity.getString(R.string.canvas_selection_delete_content_description)
        var selectionDeleteClicks = 0

        composeRule.setContent {
            CanvasLauncherTheme {
                CanvasEditLayer(
                    cameraState = CameraState(
                        worldCenter = WorldPoint(0f, 0f),
                        scale = 1f,
                        viewportWidthPx = 1080,
                        viewportHeightPx = 1920,
                    ),
                    isEditActive = true,
                    isWidgetMode = false,
                    editState = EditUiState(selectedTool = CanvasEditToolId.Selection),
                    frames = emptyList(),
                    widgets = emptyList(),
                    frameDraft = null,
                    selectedFrameIdForResize = null,
                    selectedWidgetIdForResize = null,
                    selectionDraft = CanvasSelectionDraftUiState(
                        startCorner = WorldPoint(-120f, -120f),
                        endCorner = WorldPoint(120f, 120f),
                    ),
                    selectionBounds = CanvasSelectionBoundsUiState(
                        left = -140f,
                        top = -140f,
                        right = 140f,
                        bottom = 140f,
                        hasIcons = false,
                        canResize = true,
                        canDelete = true,
                    ),
                    hasActiveSelection = true,
                    strokes = listOf(
                        CanvasStrokeUiState(
                            id = "s1",
                            points = listOf(WorldPoint(-40f, -40f), WorldPoint(40f, 40f)),
                            colorArgb = 0xFF1565C0.toInt(),
                            widthWorld = 14f,
                        ),
                    ),
                    stickyNotes = emptyList(),
                    textObjects = emptyList(),
                    snapGuides = emptyList(),
                    onCanvasTap = { _: WorldPoint -> },
                    onFrameDragStart = { _: WorldPoint -> },
                    onFrameDragUpdate = { _: WorldPoint -> },
                    onFrameDragEnd = {},
                    onSelectionDragStart = { _: WorldPoint -> },
                    onSelectionDragUpdate = { _: WorldPoint -> },
                    onSelectionDragEnd = {},
                    onSelectionClearTap = {},
                    onSelectionLongPressAt = { _: WorldPoint -> false },
                    onSelectionMoveDelta = { _: ScreenPoint -> },
                    onSelectionMoveEnd = {},
                    onSelectionResizeStart = { _: CanvasFrameResizeHandle -> },
                    onSelectionResizeDrag = { _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onSelectionResizeEnd = {},
                    onSelectionDeleteTap = { selectionDeleteClicks++ },
                    onAutoPanDelta = { _: ScreenPoint -> },
                    onBrushStart = { _: WorldPoint -> },
                    onBrushPoint = { _: WorldPoint -> },
                    onBrushEnd = {},
                    onEraseAt = { _: WorldPoint -> },
                    onStickyTap = { _: String, _: Boolean -> },
                    onStickyLongPress = { _: String -> },
                    onTextTap = { _: String -> },
                    onWidgetTap = { _: String -> },
                    onWidgetBackgroundTap = {},
                    onWidgetResizeStart = { _: CanvasFrameResizeHandle -> },
                    onWidgetResizeDrag = { _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onWidgetResizeEnd = {},
                    onWidgetDeleteTap = {},
                    onFrameTap = { _: String -> },
                    onFrameDeleteTap = { _: String -> },
                    onMoveBackgroundTap = {},
                    onFrameBorderTap = { _: String -> },
                    onFrameResizeStart = { _: String, _: CanvasFrameResizeHandle -> },
                    onFrameResizeDrag = { _: String, _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onFrameResizeEnd = {},
                    onObjectDragStart = { _ -> },
                    onObjectDragDelta = { _, _ -> },
                    onObjectDragEnd = {},
                    onObjectDragCancel = {},
                    onCanvasTransform = { _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription(deleteSelectionDescription).assertExists().performClick()
        composeRule.runOnIdle {
            assertEquals(1, selectionDeleteClicks)
        }
    }

    @Test
    fun canvasEditLayerFrameDeleteButtonInvokesCallback() {
        val deleteSelectionDescription =
            composeRule.activity.getString(R.string.canvas_selection_delete_content_description)
        var frameDeleteClicks = 0

        composeRule.setContent {
            CanvasLauncherTheme {
                CanvasEditLayer(
                    cameraState = CameraState(
                        worldCenter = WorldPoint(0f, 0f),
                        scale = 1f,
                        viewportWidthPx = 1080,
                        viewportHeightPx = 1920,
                    ),
                    isEditActive = true,
                    isWidgetMode = false,
                    editState = EditUiState(selectedTool = CanvasEditToolId.Move),
                    frames = listOf(
                        CanvasFrameObjectUiState(
                            id = "frame-1",
                            title = "Frame 1",
                            center = WorldPoint(0f, 0f),
                            widthWorld = 380f,
                            heightWorld = 240f,
                            colorArgb = 0xFF2E7D32.toInt(),
                        ),
                    ),
                    widgets = emptyList(),
                    frameDraft = null,
                    selectedFrameIdForResize = "frame-1",
                    selectedWidgetIdForResize = null,
                    selectionDraft = null,
                    selectionBounds = null,
                    hasActiveSelection = false,
                    strokes = emptyList(),
                    stickyNotes = emptyList(),
                    textObjects = emptyList(),
                    snapGuides = emptyList(),
                    onCanvasTap = { _: WorldPoint -> },
                    onFrameDragStart = { _: WorldPoint -> },
                    onFrameDragUpdate = { _: WorldPoint -> },
                    onFrameDragEnd = {},
                    onSelectionDragStart = { _: WorldPoint -> },
                    onSelectionDragUpdate = { _: WorldPoint -> },
                    onSelectionDragEnd = {},
                    onSelectionClearTap = {},
                    onSelectionLongPressAt = { _: WorldPoint -> false },
                    onSelectionMoveDelta = { _: ScreenPoint -> },
                    onSelectionMoveEnd = {},
                    onSelectionResizeStart = { _: CanvasFrameResizeHandle -> },
                    onSelectionResizeDrag = { _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onSelectionResizeEnd = {},
                    onSelectionDeleteTap = {},
                    onAutoPanDelta = { _: ScreenPoint -> },
                    onBrushStart = { _: WorldPoint -> },
                    onBrushPoint = { _: WorldPoint -> },
                    onBrushEnd = {},
                    onEraseAt = { _: WorldPoint -> },
                    onStickyTap = { _: String, _: Boolean -> },
                    onStickyLongPress = { _: String -> },
                    onTextTap = { _: String -> },
                    onWidgetTap = { _: String -> },
                    onWidgetBackgroundTap = {},
                    onWidgetResizeStart = { _: CanvasFrameResizeHandle -> },
                    onWidgetResizeDrag = { _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onWidgetResizeEnd = {},
                    onWidgetDeleteTap = {},
                    onFrameTap = { _: String -> },
                    onFrameDeleteTap = { _: String -> frameDeleteClicks++ },
                    onMoveBackgroundTap = {},
                    onFrameBorderTap = { _: String -> },
                    onFrameResizeStart = { _: String, _: CanvasFrameResizeHandle -> },
                    onFrameResizeDrag = { _: String, _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onFrameResizeEnd = {},
                    onObjectDragStart = { _ -> },
                    onObjectDragDelta = { _, _ -> },
                    onObjectDragEnd = {},
                    onObjectDragCancel = {},
                    onCanvasTransform = { _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription(deleteSelectionDescription).assertExists().performClick()
        composeRule.runOnIdle {
            assertEquals(1, frameDeleteClicks)
        }
    }

    @Test
    fun canvasEditLayerWidgetDeleteButtonInvokesCallback() {
        val deleteSelectionDescription =
            composeRule.activity.getString(R.string.canvas_selection_delete_content_description)
        var widgetDeleteClicks = 0

        composeRule.setContent {
            CanvasLauncherTheme {
                CanvasEditLayer(
                    cameraState = CameraState(
                        worldCenter = WorldPoint(0f, 0f),
                        scale = 1f,
                        viewportWidthPx = 1080,
                        viewportHeightPx = 1920,
                    ),
                    isEditActive = false,
                    isWidgetMode = true,
                    editState = EditUiState(selectedTool = CanvasEditToolId.Move),
                    frames = emptyList(),
                    widgets = listOf(
                        CanvasWidgetUiState(
                            id = "widget-1",
                            type = CanvasWidgetType.ClockDigital,
                            center = WorldPoint(0f, 0f),
                            widthWorld = 340f,
                            heightWorld = 140f,
                        ),
                    ),
                    frameDraft = null,
                    selectedFrameIdForResize = null,
                    selectedWidgetIdForResize = "widget-1",
                    selectionDraft = null,
                    selectionBounds = null,
                    hasActiveSelection = false,
                    strokes = emptyList(),
                    stickyNotes = emptyList(),
                    textObjects = emptyList(),
                    snapGuides = emptyList(),
                    onCanvasTap = { _: WorldPoint -> },
                    onFrameDragStart = { _: WorldPoint -> },
                    onFrameDragUpdate = { _: WorldPoint -> },
                    onFrameDragEnd = {},
                    onSelectionDragStart = { _: WorldPoint -> },
                    onSelectionDragUpdate = { _: WorldPoint -> },
                    onSelectionDragEnd = {},
                    onSelectionClearTap = {},
                    onSelectionLongPressAt = { _: WorldPoint -> false },
                    onSelectionMoveDelta = { _: ScreenPoint -> },
                    onSelectionMoveEnd = {},
                    onSelectionResizeStart = { _: CanvasFrameResizeHandle -> },
                    onSelectionResizeDrag = { _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onSelectionResizeEnd = {},
                    onSelectionDeleteTap = {},
                    onAutoPanDelta = { _: ScreenPoint -> },
                    onBrushStart = { _: WorldPoint -> },
                    onBrushPoint = { _: WorldPoint -> },
                    onBrushEnd = {},
                    onEraseAt = { _: WorldPoint -> },
                    onStickyTap = { _: String, _: Boolean -> },
                    onStickyLongPress = { _: String -> },
                    onTextTap = { _: String -> },
                    onWidgetTap = { _: String -> },
                    onWidgetBackgroundTap = {},
                    onWidgetResizeStart = { _: CanvasFrameResizeHandle -> },
                    onWidgetResizeDrag = { _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onWidgetResizeEnd = {},
                    onWidgetDeleteTap = { widgetDeleteClicks++ },
                    onFrameTap = { _: String -> },
                    onFrameDeleteTap = { _: String -> },
                    onMoveBackgroundTap = {},
                    onFrameBorderTap = { _: String -> },
                    onFrameResizeStart = { _: String, _: CanvasFrameResizeHandle -> },
                    onFrameResizeDrag = { _: String, _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onFrameResizeEnd = {},
                    onObjectDragStart = { _ -> },
                    onObjectDragDelta = { _, _ -> },
                    onObjectDragEnd = {},
                    onObjectDragCancel = {},
                    onCanvasTransform = { _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription(deleteSelectionDescription).assertExists().performClick()
        composeRule.runOnIdle {
            assertEquals(1, widgetDeleteClicks)
        }
    }

    @Test
    fun canvasEditLayerComposesStickyTextFrameAndAllWidgetTypes() {
        composeRule.setContent {
            CanvasLauncherTheme {
                CanvasEditLayer(
                    cameraState = CameraState(
                        worldCenter = WorldPoint(0f, 0f),
                        scale = 1f,
                        viewportWidthPx = 1080,
                        viewportHeightPx = 1920,
                    ),
                    isEditActive = true,
                    isWidgetMode = true,
                    editState = EditUiState(selectedTool = CanvasEditToolId.Move),
                    frames = listOf(
                        CanvasFrameObjectUiState(
                            id = "frame-1",
                            title = "Frame 1",
                            center = WorldPoint(-120f, -120f),
                            widthWorld = 360f,
                            heightWorld = 220f,
                            colorArgb = 0xFF2E7D32.toInt(),
                        ),
                    ),
                    widgets = listOf(
                        CanvasWidgetUiState("w1", CanvasWidgetType.ClockDigital, WorldPoint(-80f, 20f), 320f, 140f),
                        CanvasWidgetUiState("w2", CanvasWidgetType.ClockAnalog, WorldPoint(220f, -40f), 240f, 240f),
                        CanvasWidgetUiState("w3", CanvasWidgetType.Weather, WorldPoint(120f, 180f), 360f, 164f),
                        CanvasWidgetUiState("w4", CanvasWidgetType.Notifications, WorldPoint(-240f, 220f), 380f, 164f),
                        CanvasWidgetUiState("w5", CanvasWidgetType.Calendar, WorldPoint(260f, 260f), 400f, 172f),
                    ),
                    frameDraft = null,
                    selectedFrameIdForResize = null,
                    selectedWidgetIdForResize = "w2",
                    selectionDraft = null,
                    selectionBounds = null,
                    hasActiveSelection = false,
                    strokes = listOf(
                        CanvasStrokeUiState(
                            id = "stroke-1",
                            points = listOf(WorldPoint(-30f, -30f), WorldPoint(30f, 30f)),
                            colorArgb = 0xFF1565C0.toInt(),
                            widthWorld = 12f,
                        ),
                    ),
                    stickyNotes = listOf(
                        CanvasStickyNoteUiState(
                            id = "sticky-1",
                            text = "Reminder",
                            center = WorldPoint(40f, -220f),
                            sizeWorld = 220f,
                            textSizeWorld = 44f,
                            colorArgb = 0xFFE65100.toInt(),
                        ),
                    ),
                    textObjects = listOf(
                        CanvasTextObjectUiState(
                            id = "text-1",
                            text = "Hello",
                            position = WorldPoint(-220f, 60f),
                            textSizeWorld = 44f,
                            colorArgb = 0xFF6A1B9A.toInt(),
                        ),
                    ),
                    snapGuides = emptyList(),
                    onCanvasTap = { _: WorldPoint -> },
                    onFrameDragStart = { _: WorldPoint -> },
                    onFrameDragUpdate = { _: WorldPoint -> },
                    onFrameDragEnd = {},
                    onSelectionDragStart = { _: WorldPoint -> },
                    onSelectionDragUpdate = { _: WorldPoint -> },
                    onSelectionDragEnd = {},
                    onSelectionClearTap = {},
                    onSelectionLongPressAt = { _: WorldPoint -> false },
                    onSelectionMoveDelta = { _: ScreenPoint -> },
                    onSelectionMoveEnd = {},
                    onSelectionResizeStart = { _: CanvasFrameResizeHandle -> },
                    onSelectionResizeDrag = { _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onSelectionResizeEnd = {},
                    onSelectionDeleteTap = {},
                    onAutoPanDelta = { _: ScreenPoint -> },
                    onBrushStart = { _: WorldPoint -> },
                    onBrushPoint = { _: WorldPoint -> },
                    onBrushEnd = {},
                    onEraseAt = { _: WorldPoint -> },
                    onStickyTap = { _: String, _: Boolean -> },
                    onStickyLongPress = { _: String -> },
                    onTextTap = { _: String -> },
                    onWidgetTap = { _: String -> },
                    onWidgetBackgroundTap = {},
                    onWidgetResizeStart = { _: CanvasFrameResizeHandle -> },
                    onWidgetResizeDrag = { _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onWidgetResizeEnd = {},
                    onWidgetDeleteTap = {},
                    onFrameTap = { _: String -> },
                    onFrameDeleteTap = { _: String -> },
                    onMoveBackgroundTap = {},
                    onFrameBorderTap = { _: String -> },
                    onFrameResizeStart = { _: String, _: CanvasFrameResizeHandle -> },
                    onFrameResizeDrag = { _: String, _: CanvasFrameResizeHandle, _: ScreenPoint -> },
                    onFrameResizeEnd = {},
                    onObjectDragStart = { _ -> },
                    onObjectDragDelta = { _, _ -> },
                    onObjectDragEnd = {},
                    onObjectDragCancel = {},
                    onCanvasTransform = { _, _, _ -> },
                )
            }
        }

        composeRule.onRoot().assertExists()
    }
}
