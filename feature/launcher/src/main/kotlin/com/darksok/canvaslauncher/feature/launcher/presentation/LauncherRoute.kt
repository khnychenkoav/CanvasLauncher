package com.darksok.canvaslauncher.feature.launcher.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.darksok.canvaslauncher.core.model.ui.resolveDarkTheme
import com.darksok.canvaslauncher.core.performance.MiniMapProjector
import com.darksok.canvaslauncher.core.ui.theme.CanvasLauncherTheme
import com.darksok.canvaslauncher.feature.canvas.CanvasBackgroundConfig
import com.darksok.canvaslauncher.feature.canvas.InfiniteCanvas
import com.darksok.canvaslauncher.feature.launcher.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LauncherRoute(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    viewModel: LauncherViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = uiState.themeMode.resolveDarkTheme(isSystemDark)

    DisposableEffect(lifecycleOwner, viewModel) {
        viewModel.onHostResumed()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onHostResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = uiState.toolsState.isAppsListActive) {
        viewModel.onAppsListClose()
    }
    BackHandler(enabled = uiState.toolsState.isSearchActive) {
        viewModel.onSearchClose()
    }
    BackHandler(enabled = uiState.toolsState.isEditActive) {
        viewModel.onEditClose()
    }
    BackHandler(enabled = uiState.toolsState.isWidgetsActive) {
        viewModel.onWidgetsClose()
    }
    BackHandler(
        enabled = uiState.toolsState.isExpanded &&
            !uiState.toolsState.isSearchActive &&
            !uiState.toolsState.isAppsListActive &&
            !uiState.toolsState.isEditActive &&
            !uiState.toolsState.isWidgetsActive,
    ) {
        viewModel.collapseToolsPanel()
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collectLatest { messageRes ->
            snackbarHostState.showSnackbar(message = context.getString(messageRes))
        }
    }

    CanvasLauncherTheme(darkTheme = darkTheme, lightPalette = uiState.lightPalette, darkPalette = uiState.darkPalette) {
        SystemBarsContrastEffect(darkTheme = darkTheme)

        val colorScheme = MaterialTheme.colorScheme
        val backgroundConfig = remember(colorScheme) {
            CanvasBackgroundConfig(
                fillColor = colorScheme.surface,
                dotColor = colorScheme.onSurface.copy(alpha = 0.14f),
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) {
            val canvasHostModifier = if (
                uiState.toolsState.isEditActive &&
                uiState.toolsState.edit.inlineEditor.isVisible
            ) {
                Modifier
                    .fillMaxSize()
                    .imePadding()
            } else {
                Modifier.fillMaxSize()
            }
            Box(
                modifier = canvasHostModifier,
            ) {
                if (!uiState.isInitialized) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    InfiniteCanvas(
                        cameraState = uiState.cameraState,
                        apps = uiState.visibleApps,
                        draggingPackageName = uiState.draggingPackageName,
                        appDragEnabled = uiState.toolsState.isEditActive &&
                            uiState.toolsState.edit.selectedTool == CanvasEditToolId.Move,
                        transformEnabled = !uiState.toolsState.isEditActive && !uiState.toolsState.isWidgetsActive,
                        labelsEnabled = true,
                        backgroundConfig = backgroundConfig,
                        onViewportSizeChanged = { size ->
                            viewModel.onViewportSizeChanged(size.width, size.height)
                        },
                        onTransform = { pan, zoom, focus ->
                            viewModel.onTransform(pan, zoom, focus)
                        },
                        onAppClick = viewModel::onAppClick,
                        onAppDragStart = viewModel::onAppDragStart,
                        onAppDragDelta = viewModel::onAppDragDelta,
                        onAppDragEnd = viewModel::onAppDragEnd,
                        onAppDragCancel = viewModel::onAppDragCancel,
                        onAppAutoPanDelta = viewModel::onAppAutoPanDelta,
                    )

                    CanvasEditLayer(
                        cameraState = uiState.cameraState,
                        isEditActive = uiState.toolsState.isEditActive,
                        isWidgetMode = uiState.toolsState.isWidgetsActive,
                        editState = uiState.toolsState.edit,
                        frames = uiState.frames,
                        widgets = uiState.widgets,
                        frameDraft = uiState.frameDraft,
                        selectedFrameIdForResize = uiState.selectedFrameIdForResize,
                        selectedWidgetIdForResize = uiState.selectedWidgetId,
                        selectionDraft = uiState.selectionDraft,
                        selectionBounds = uiState.selectionBounds,
                        hasActiveSelection = uiState.hasActiveSelection,
                        strokes = uiState.strokes,
                        stickyNotes = uiState.stickyNotes,
                        textObjects = uiState.textObjects,
                        snapGuides = uiState.snapGuides,
                        modifier = Modifier.fillMaxSize(),
                        onCanvasTap = viewModel::onEditCanvasTap,
                        onFrameDragStart = viewModel::onEditFrameDragStart,
                        onFrameDragUpdate = viewModel::onEditFrameDragUpdate,
                        onFrameDragEnd = viewModel::onEditFrameDragEnd,
                        onSelectionDragStart = viewModel::onEditSelectionDragStart,
                        onSelectionDragUpdate = viewModel::onEditSelectionDragUpdate,
                        onSelectionDragEnd = viewModel::onEditSelectionDragEnd,
                        onSelectionClearTap = viewModel::onEditSelectionClearTap,
                        onSelectionLongPressAt = viewModel::onEditSelectionLongPressAt,
                        onSelectionMoveDelta = viewModel::onEditSelectionMoveDelta,
                        onSelectionMoveEnd = viewModel::onEditSelectionMoveEnd,
                        onSelectionResizeStart = viewModel::onEditSelectionResizeStart,
                        onSelectionResizeDrag = viewModel::onEditSelectionResizeDrag,
                        onSelectionResizeEnd = viewModel::onEditSelectionResizeEnd,
                        onSelectionDeleteTap = viewModel::onEditSelectionDeleteTap,
                        onAutoPanDelta = viewModel::onEditAutoPanDelta,
                        onBrushStart = viewModel::onEditBrushStart,
                        onBrushPoint = viewModel::onEditBrushPoint,
                        onBrushEnd = viewModel::onEditBrushEnd,
                        onEraseAt = viewModel::onEditEraseAt,
                        onStickyTap = viewModel::onEditStickyTap,
                        onStickyLongPress = viewModel::onEditStickyLongPress,
                        onTextTap = viewModel::onEditTextTap,
                        onWidgetTap = viewModel::onWidgetTap,
                        onWidgetBackgroundTap = viewModel::onWidgetBackgroundTap,
                        onWidgetResizeStart = viewModel::onWidgetResizeStart,
                        onWidgetResizeDrag = viewModel::onWidgetResizeDrag,
                        onWidgetResizeEnd = viewModel::onWidgetResizeEnd,
                        onWidgetDeleteTap = viewModel::onWidgetDeleteSelected,
                        onFrameTap = viewModel::onEditFrameTap,
                        onFrameDeleteTap = viewModel::onEditFrameDeleteTap,
                        onMoveBackgroundTap = viewModel::onEditMoveBackgroundTap,
                        onFrameBorderTap = viewModel::onEditFrameBorderTap,
                        onFrameResizeStart = viewModel::onEditFrameResizeStart,
                        onFrameResizeDrag = viewModel::onEditFrameResizeDrag,
                        onFrameResizeEnd = viewModel::onEditFrameResizeEnd,
                        onObjectDragStart = viewModel::onEditObjectDragStart,
                        onObjectDragDelta = viewModel::onEditObjectDragDelta,
                        onObjectDragEnd = viewModel::onEditObjectDragEnd,
                        onObjectDragCancel = viewModel::onEditObjectDragCancel,
                        onCanvasTransform = { pan, zoom, focus ->
                            viewModel.onTransform(pan, zoom, focus)
                        },
                    )

                    if (!uiState.toolsState.isSearchActive &&
                        !uiState.toolsState.isAppsListActive &&
                        !uiState.toolsState.isEditActive &&
                        !uiState.toolsState.isWidgetsActive &&
                        MiniMapProjector.shouldShow(uiState.cameraState.scale)
                    ) {
                        MiniMapOverlay(
                            appPositions = uiState.allAppPositions,
                            cameraState = uiState.cameraState,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .navigationBarsPadding()
                                .padding(start = 8.dp, bottom = 4.dp),
                        )
                    }

                    AnimatedVisibility(
                        visible = uiState.toolsState.isAppsListActive,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 5 }),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        AppsListScreen(
                            state = uiState.toolsState.appsList,
                            modifier = Modifier.fillMaxSize(),
                            onQueryChanged = viewModel::onAppsListQueryChanged,
                            onAppClick = viewModel::onAppsListAppClick,
                            onShowOnCanvas = viewModel::onAppsListShowOnCanvas,
                            onRequestUninstall = { packageName ->
                                val opened = openUninstallApp(context, packageName)
                                if (!opened) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.error_uninstall_unavailable),
                                        )
                                    }
                                }
                            },
                            onClose = viewModel::onAppsListClose,
                        )
                    }

                    if (!uiState.toolsState.isAppsListActive) {
                        LauncherToolsOverlay(
                            toolsState = uiState.toolsState,
                            modifier = Modifier.align(Alignment.BottomEnd),
                            onToolsToggle = viewModel::onToolsToggle,
                            onToolSelected = { tool ->
                                when (tool) {
                                    LauncherToolId.Search,
                                    LauncherToolId.AppsList,
                                    LauncherToolId.Edit,
                                    LauncherToolId.Widgets -> viewModel.onToolSelected(tool)

                                    LauncherToolId.Settings -> {
                                        viewModel.collapseToolsPanel()
                                        onOpenSettings()
                                    }
                                }
                            },
                            onSearchQueryChanged = viewModel::onSearchQueryChanged,
                            onSearchActionClick = viewModel::onSearchActionClick,
                            onSearchSubmit = viewModel::onSearchSubmit,
                            onSearchOpenInBrowser = { query ->
                                val opened = openQueryInDefaultBrowser(context, query)
                                if (!opened) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(R.string.error_web_search_unavailable),
                                        )
                                    }
                                }
                            },
                            onSearchLaunchTopMatch = viewModel::onSearchLaunchTopMatch,
                            onSearchClose = viewModel::onSearchClose,
                            onSearchOcclusionChanged = viewModel::onSearchOcclusionChanged,
                            onSearchKeyboardVisibilityChanged = viewModel::onSearchKeyboardVisibilityChanged,
                            onEditClose = viewModel::onEditClose,
                            onEditToolSelected = viewModel::onEditToolSelected,
                            onEditColorSelected = viewModel::onEditColorSelected,
                            onEditBrushSizeStep = viewModel::onEditBrushSizeStep,
                            onEditTextSizeStep = viewModel::onEditTextSizeStep,
                            onEditInlineEditorValueChanged = viewModel::onEditInlineEditorValueChanged,
                            onEditInlineEditorConfirm = viewModel::onEditInlineEditorConfirm,
                            onEditInlineEditorCancel = viewModel::onEditInlineEditorCancel,
                            onEditUndo = viewModel::onEditUndo,
                            onEditClearCustomElements = viewModel::onEditClearCustomElements,
                            onWidgetsClose = viewModel::onWidgetsClose,
                            onWidgetCatalogItemSelected = viewModel::onWidgetCatalogItemSelected,
                        )
                    }
                }
            }
        }
    }
}

private fun openUninstallApp(context: Context, packageName: String): Boolean {
    val packageUri = Uri.fromParts("package", packageName, null)
    val intents = listOf(
        Intent(Intent.ACTION_DELETE, packageUri).putExtra(Intent.EXTRA_RETURN_RESULT, false),
        Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri).putExtra(Intent.EXTRA_RETURN_RESULT, false),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri),
        Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS),
    )
    return intents.any { intent ->
        launchIntentCompat(context, intent)
    }
}

private fun openQueryInDefaultBrowser(
    context: Context,
    query: String,
): Boolean {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) return false
    val encoded = Uri.encode(trimmedQuery)
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.google.com/search?q=$encoded"),
    )
    return launchIntentCompat(context, intent)
}

private fun launchIntentCompat(context: Context, intent: Intent): Boolean {
    val activity = context.findActivity()
    val launchIntent = Intent(intent).apply {
        if (activity == null) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    val canResolve = runCatching {
        context.packageManager.resolveActivity(launchIntent, 0) != null
    }.getOrDefault(false)
    if (!canResolve) {
        return false
    }
    return runCatching {
        if (activity != null) {
            activity.startActivity(launchIntent)
        } else {
            context.startActivity(launchIntent)
        }
    }.onFailure { throwable ->
        Log.w(TAG, "Failed to start intent ${intent.action}", throwable)
    }.isSuccess
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}

private const val TAG = "LauncherRoute"

@Composable
private fun SystemBarsContrastEffect(darkTheme: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}

@Composable
private fun MiniMapOverlay(
    appPositions: List<com.darksok.canvaslauncher.core.model.canvas.WorldPoint>,
    cameraState: com.darksok.canvaslauncher.core.model.canvas.CameraState,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
        tonalElevation = 4.dp,
        modifier = modifier
            .size(152.dp)
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                shape = shape,
            ),
    ) {
        val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        val appColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        val userColor = MaterialTheme.colorScheme.primary
        val viewportColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
        ) {
            val projection = MiniMapProjector.project(
                appPositions = appPositions,
                camera = cameraState,
                mapWidthPx = size.width,
                mapHeightPx = size.height,
            )

            projection.appPoints.forEach { point ->
                drawCircle(
                    color = appColor,
                    radius = 1.8.dp.toPx(),
                    center = Offset(point.x, point.y),
                )
            }

            val user = Offset(projection.userPoint.x, projection.userPoint.y)
            drawLine(lineColor, Offset(user.x, 0f), Offset(user.x, size.height), 1.dp.toPx())
            drawLine(lineColor, Offset(0f, user.y), Offset(size.width, user.y), 1.dp.toPx())

            val viewport = projection.viewportRect
            drawRoundRect(
                color = viewportColor,
                topLeft = Offset(viewport.left, viewport.top),
                size = Size(
                    width = (viewport.right - viewport.left).coerceAtLeast(2.dp.toPx()),
                    height = (viewport.bottom - viewport.top).coerceAtLeast(2.dp.toPx()),
                ),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 1.2.dp.toPx()),
            )

            drawCircle(
                color = userColor,
                radius = 3.2.dp.toPx(),
                center = user,
            )
        }
    }
}

