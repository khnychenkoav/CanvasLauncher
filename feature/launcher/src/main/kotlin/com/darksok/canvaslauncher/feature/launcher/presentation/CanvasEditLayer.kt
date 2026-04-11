package com.darksok.canvaslauncher.feature.launcher.presentation

import android.Manifest
import android.content.ContentUris
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import android.provider.CalendarContract
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.darksok.canvaslauncher.feature.launcher.R
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.performance.WorldScreenTransformer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun CanvasEditLayer(
    cameraState: CameraState,
    isEditActive: Boolean,
    isWidgetMode: Boolean,
    editState: EditUiState,
    frames: List<CanvasFrameObjectUiState>,
    widgets: List<CanvasWidgetUiState>,
    frameDraft: CanvasFrameDraftUiState?,
    selectedFrameIdForResize: String?,
    selectedWidgetIdForResize: String?,
    selectionDraft: CanvasSelectionDraftUiState?,
    selectionBounds: CanvasSelectionBoundsUiState?,
    hasActiveSelection: Boolean,
    strokes: List<CanvasStrokeUiState>,
    stickyNotes: List<CanvasStickyNoteUiState>,
    textObjects: List<CanvasTextObjectUiState>,
    snapGuides: List<CanvasSnapGuideUiState>,
    modifier: Modifier = Modifier,
    onCanvasTap: (WorldPoint) -> Unit,
    onFrameDragStart: (WorldPoint) -> Unit,
    onFrameDragUpdate: (WorldPoint) -> Unit,
    onFrameDragEnd: () -> Unit,
    onSelectionDragStart: (WorldPoint) -> Unit,
    onSelectionDragUpdate: (WorldPoint) -> Unit,
    onSelectionDragEnd: () -> Unit,
    onSelectionClearTap: () -> Unit,
    onSelectionLongPressAt: (WorldPoint) -> Boolean,
    onSelectionMoveDelta: (ScreenPoint) -> Unit,
    onSelectionMoveEnd: () -> Unit,
    onSelectionResizeStart: (CanvasFrameResizeHandle) -> Unit,
    onSelectionResizeDrag: (CanvasFrameResizeHandle, ScreenPoint) -> Unit,
    onSelectionResizeEnd: () -> Unit,
    onSelectionDeleteTap: () -> Unit,
    onAutoPanDelta: (ScreenPoint) -> Unit,
    onBrushStart: (WorldPoint) -> Unit,
    onBrushPoint: (WorldPoint) -> Unit,
    onBrushEnd: () -> Unit,
    onEraseAt: (WorldPoint) -> Unit,
    onStickyTap: (id: String, centerTap: Boolean) -> Unit,
    onStickyLongPress: (id: String) -> Unit,
    onTextTap: (id: String) -> Unit,
    onWidgetTap: (id: String) -> Unit,
    onWidgetBackgroundTap: () -> Unit,
    onWidgetResizeStart: (CanvasFrameResizeHandle) -> Unit,
    onWidgetResizeDrag: (CanvasFrameResizeHandle, ScreenPoint) -> Unit,
    onWidgetResizeEnd: () -> Unit,
    onWidgetDeleteTap: () -> Unit,
    onFrameTap: (id: String) -> Unit,
    onFrameDeleteTap: (id: String) -> Unit,
    onMoveBackgroundTap: () -> Unit,
    onFrameBorderTap: (id: String) -> Unit,
    onFrameResizeStart: (id: String, handle: CanvasFrameResizeHandle) -> Unit,
    onFrameResizeDrag: (id: String, handle: CanvasFrameResizeHandle, delta: ScreenPoint) -> Unit,
    onFrameResizeEnd: () -> Unit,
    onObjectDragStart: (CanvasObjectDragTarget) -> Unit,
    onObjectDragDelta: (CanvasObjectDragTarget, ScreenPoint) -> Unit,
    onObjectDragEnd: () -> Unit,
    onObjectDragCancel: () -> Unit,
    onCanvasTransform: (panDeltaPx: ScreenPoint, zoomFactor: Float, focusPx: ScreenPoint) -> Unit,
) {
    val density = LocalDensity.current
    val scale = cameraState.scale
    var isMultiTouchGestureActive by remember { mutableStateOf(false) }
    val allowsObjectMove = isEditActive &&
        editState.selectedTool == CanvasEditToolId.Move &&
        !isMultiTouchGestureActive
    val allowsObjectTap = isEditActive && (
        editState.selectedTool == CanvasEditToolId.Move ||
            editState.selectedTool == CanvasEditToolId.Delete
        ) && !isMultiTouchGestureActive
    val allowsFrameBorderSelection = isEditActive && editState.selectedTool == CanvasEditToolId.Move
    val showsSelectionOverlay = isEditActive && editState.selectedTool == CanvasEditToolId.Selection
    val guideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
    val widgetSelectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f)
    val frameCornerRadiusPx = with(density) { FRAME_CORNER_RADIUS_DP.toPx() }
    val frameBorderStrokePx = with(density) { FRAME_BORDER_STROKE_DP.toPx() }
    val frameBorderHitWidthPx = with(density) { FRAME_BORDER_TAP_HIT_DP.toPx() }
    val autoPanZonePx = with(density) { EDGE_AUTO_PAN_ZONE_DP.toPx() }
    val autoPanMaxStepPx = with(density) { EDGE_AUTO_PAN_MAX_STEP_DP.toPx() }
    val frameTitleFallbackText = stringResource(id = R.string.canvas_frame_fallback_title)
    val stickyFallbackText = stringResource(id = R.string.canvas_sticky_fallback_text)
    val textFallbackText = stringResource(id = R.string.canvas_text_fallback_text)
    val frameTitleOffsetPx = with(density) { FRAME_TITLE_OFFSET_DP.toPx() }
    val frameTitleCornerRadiusPx = with(density) { FRAME_TITLE_CORNER_RADIUS_DP.toPx() }
    val frameTitleHorizontalPaddingPx = with(density) { FRAME_TITLE_HORIZONTAL_PADDING_DP.toPx() }
    val frameTitleVerticalPaddingPx = with(density) { FRAME_TITLE_VERTICAL_PADDING_DP.toPx() }
    val frameTitleTextSizePx = with(density) { FRAME_TITLE_TEXT_SIZE_SP.toPx() }
    val frameTitleFillColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
    val frameTitleTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
    val stickyTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    val frameTitlePaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            isLinearText = true
        }
    }
    val stickyTextPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.CENTER
            isLinearText = true
        }
    }
    val canvasTextPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.LEFT
            isLinearText = true
        }
    }
    val typography = MaterialTheme.typography
    SideEffect {
        frameTitlePaint.textSize = frameTitleTextSizePx
        frameTitlePaint.color = frameTitleTextColor.toArgb()
        stickyTextPaint.color = stickyTextColor.toArgb()
        applyPaintTypography(frameTitlePaint, typography.labelLarge)
        applyPaintTypography(stickyTextPaint, typography.bodyMedium)
        applyPaintTypography(canvasTextPaint, typography.bodyMedium)
    }
    val tracksMultiTouch = isEditActive || isWidgetMode

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (tracksMultiTouch) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val pointerCount = event.changes.count { change -> change.pressed }
                                val multiTouchNow = pointerCount > 1
                                if (multiTouchNow != isMultiTouchGestureActive) {
                                    isMultiTouchGestureActive = multiTouchNow
                                }
                                if (pointerCount == 0) {
                                    if (isMultiTouchGestureActive) {
                                        isMultiTouchGestureActive = false
                                    }
                                    break
                                }
                            }
                        }
                    }
                } else {
                    if (isMultiTouchGestureActive) {
                        isMultiTouchGestureActive = false
                    }
                    Modifier
                },
            )
            .canvasMultiTouchTransformModifier(
                enabled = tracksMultiTouch,
                onTransform = onCanvasTransform,
            )
            .canvasInputModifier(
                isEditActive = isEditActive,
                selectedTool = editState.selectedTool,
                cameraState = cameraState,
                onCanvasTap = onCanvasTap,
                onFrameDragStart = onFrameDragStart,
                onFrameDragUpdate = onFrameDragUpdate,
                onFrameDragEnd = onFrameDragEnd,
                hasActiveSelection = hasActiveSelection,
                selectionBounds = selectionBounds,
                onSelectionDragStart = onSelectionDragStart,
                onSelectionDragUpdate = onSelectionDragUpdate,
                onSelectionDragEnd = onSelectionDragEnd,
                onSelectionClearTap = onSelectionClearTap,
                onSelectionLongPressAt = onSelectionLongPressAt,
                onSelectionMoveDelta = onSelectionMoveDelta,
                onSelectionMoveEnd = onSelectionMoveEnd,
                autoPanZonePx = autoPanZonePx,
                autoPanMaxStepPx = autoPanMaxStepPx,
                onAutoPanDelta = onAutoPanDelta,
                onBrushStart = onBrushStart,
                onBrushPoint = onBrushPoint,
                onBrushEnd = onBrushEnd,
                onEraseAt = onEraseAt,
            ),
    ) {
        val frameLayouts = frames.map { frame ->
            val center = WorldScreenTransformer.worldToScreen(frame.center, cameraState)
            val widthPx = (frame.widthWorld * scale).coerceAtLeast(28f)
            val heightPx = (frame.heightWorld * scale).coerceAtLeast(28f)
            val frameLeft = center.x - widthPx / 2f
            val frameTop = center.y - heightPx / 2f
            FrameLayoutState(
                frame = frame,
                widthPx = widthPx,
                heightPx = heightPx,
                leftPx = frameLeft,
                topPx = frameTop,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0.3f),
        ) {
            frameLayouts.forEach { item ->
                drawRoundRect(
                    color = Color(item.frame.colorArgb).copy(alpha = 0.10f),
                    topLeft = Offset(item.leftPx, item.topPx),
                    size = Size(item.widthPx, item.heightPx),
                    cornerRadius = CornerRadius(frameCornerRadiusPx, frameCornerRadiusPx),
                )
                drawRoundRect(
                    color = Color(item.frame.colorArgb).copy(alpha = 0.36f),
                    topLeft = Offset(item.leftPx, item.topPx),
                    size = Size(item.widthPx, item.heightPx),
                    cornerRadius = CornerRadius(frameCornerRadiusPx, frameCornerRadiusPx),
                    style = Stroke(width = frameBorderStrokePx),
                )
            }
        }

        if (isEditActive && editState.selectedTool == CanvasEditToolId.Move &&
            (selectedFrameIdForResize != null || selectedWidgetIdForResize != null)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0.305f)
                    .pointerInput(selectedFrameIdForResize, selectedWidgetIdForResize) {
                        detectTapGestures(onTap = { onMoveBackgroundTap() })
                    },
            )
        }

        val viewportWidthPx = cameraState.viewportWidthPx.toFloat()
        val viewportHeightPx = cameraState.viewportHeightPx.toFloat()
        frameLayouts.forEach { item ->
            val frame = item.frame
            val frameLeft = item.leftPx
            val frameTop = item.topPx
            val frameRight = frameLeft + item.widthPx
            val frameBottom = frameTop + item.heightPx

            if (isEditActive && allowsFrameBorderSelection) {
                val visibleLeft = frameLeft.coerceAtLeast(0f)
                val visibleTop = frameTop.coerceAtLeast(0f)
                val visibleRight = frameRight.coerceAtMost(viewportWidthPx)
                val visibleBottom = frameBottom.coerceAtMost(viewportHeightPx)
                val visibleWidthPx = visibleRight - visibleLeft
                val visibleHeightPx = visibleBottom - visibleTop
                if (visibleWidthPx > 0f && visibleHeightPx > 0f) {
                    val borderHitPx = frameBorderHitWidthPx
                        .coerceAtLeast(1f)
                        .coerceAtMost(min(visibleWidthPx, visibleHeightPx))
                    val topBottomHeightPx = borderHitPx
                    val leftRightWidthPx = borderHitPx
                    val middleHeightPx = (visibleHeightPx - topBottomHeightPx * 2f).coerceAtLeast(0f)

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = visibleLeft.roundToInt(),
                                    y = visibleTop.roundToInt(),
                                )
                            }
                            .requiredSize(
                                width = with(density) { visibleWidthPx.toDp() },
                                height = with(density) { topBottomHeightPx.toDp() },
                            )
                            .pointerInput(frame.id, visibleWidthPx, topBottomHeightPx) {
                                detectTapGestures(onTap = { onFrameBorderTap(frame.id) })
                            }
                            .zIndex(0.31f),
                    )

                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = visibleLeft.roundToInt(),
                                    y = (visibleBottom - topBottomHeightPx).roundToInt(),
                                )
                            }
                            .requiredSize(
                                width = with(density) { visibleWidthPx.toDp() },
                                height = with(density) { topBottomHeightPx.toDp() },
                            )
                            .pointerInput(frame.id, visibleWidthPx, topBottomHeightPx, visibleBottom) {
                                detectTapGestures(onTap = { onFrameBorderTap(frame.id) })
                            }
                            .zIndex(0.31f),
                    )

                    if (middleHeightPx > 0f) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = visibleLeft.roundToInt(),
                                        y = (visibleTop + topBottomHeightPx).roundToInt(),
                                    )
                                }
                                .requiredSize(
                                    width = with(density) { leftRightWidthPx.toDp() },
                                    height = with(density) { middleHeightPx.toDp() },
                                )
                                .pointerInput(frame.id, leftRightWidthPx, middleHeightPx) {
                                    detectTapGestures(onTap = { onFrameBorderTap(frame.id) })
                                }
                                .zIndex(0.31f),
                        )

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (visibleRight - leftRightWidthPx).roundToInt(),
                                        y = (visibleTop + topBottomHeightPx).roundToInt(),
                                    )
                                }
                                .requiredSize(
                                    width = with(density) { leftRightWidthPx.toDp() },
                                    height = with(density) { middleHeightPx.toDp() },
                                )
                                .pointerInput(frame.id, leftRightWidthPx, middleHeightPx, visibleRight) {
                                    detectTapGestures(onTap = { onFrameBorderTap(frame.id) })
                                }
                                .zIndex(0.31f),
                        )
                    }
                }
            }

            val frameTitle = frame.title.ifBlank { frameTitleFallbackText }
            val frameTitleBounds = estimateTextBoundsPx(
                text = frameTitle,
                textSizePx = frameTitleTextSizePx,
            )
            val frameTitleWidthPx = frameTitleBounds.widthPx + frameTitleHorizontalPaddingPx * 2f
            val frameTitleHeightPx = frameTitleBounds.heightPx + frameTitleVerticalPaddingPx * 2f
            val frameTitleLeftPx = item.leftPx + frameTitleOffsetPx
            val frameTitleTopPx = item.topPx + frameTitleOffsetPx
            Canvas(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = frameTitleLeftPx
                        translationY = frameTitleTopPx
                    }
                    .requiredSize(
                        width = with(density) { frameTitleWidthPx.toDp() },
                        height = with(density) { frameTitleHeightPx.toDp() },
                    )
                    .zIndex(0.32f),
            ) {
                drawRoundRect(
                    color = frameTitleFillColor,
                    cornerRadius = CornerRadius(frameTitleCornerRadiusPx, frameTitleCornerRadiusPx),
                )
                drawIntoCanvas { canvas ->
                    val fontMetrics = frameTitlePaint.fontMetrics
                    val baseline = frameTitleVerticalPaddingPx - fontMetrics.ascent
                    canvas.nativeCanvas.drawText(
                        frameTitle,
                        frameTitleHorizontalPaddingPx,
                        baseline,
                        frameTitlePaint,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = frameTitleLeftPx
                        translationY = frameTitleTopPx
                    }
                    .requiredSize(
                        width = with(density) { frameTitleWidthPx.toDp() },
                        height = with(density) { frameTitleHeightPx.toDp() },
                    )
                    .zIndex(0.33f)
                    .objectMoveModifier(
                        enabled = isEditActive && allowsObjectMove,
                        target = CanvasObjectDragTarget.Frame(frame.id),
                        nodeTopLeftScreen = ScreenPoint(
                            x = frameTitleLeftPx,
                            y = frameTitleTopPx,
                        ),
                        canvasWidthPx = viewportWidthPx,
                        canvasHeightPx = viewportHeightPx,
                        autoPanZonePx = autoPanZonePx,
                        autoPanMaxStepPx = autoPanMaxStepPx,
                        onObjectDragStart = onObjectDragStart,
                        onObjectDragDelta = onObjectDragDelta,
                        onObjectDragEnd = onObjectDragEnd,
                        onObjectDragCancel = onObjectDragCancel,
                        onAutoPanDelta = onAutoPanDelta,
                    )
                    .then(
                        if (isEditActive && allowsObjectTap) {
                            Modifier.pointerInput(frame.id, editState.selectedTool) {
                                detectTapGestures(
                                    onTap = { onFrameTap(frame.id) },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
            )

            if (isEditActive && allowsFrameBorderSelection && selectedFrameIdForResize == frame.id) {
                SelectionDeleteButton(
                    onClick = { onFrameDeleteTap(frame.id) },
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = frameLeft + item.widthPx / 2f - with(density) { 18.dp.toPx() }
                            translationY = frameTop - with(density) { DELETE_BUTTON_VERTICAL_OFFSET_DP.toPx() }
                        }
                        .zIndex(1.22f),
                )
                FrameResizeHandles(
                    frameId = frame.id,
                    frameLeftPx = frameLeft,
                    frameTopPx = frameTop,
                    frameWidthPx = item.widthPx,
                    frameHeightPx = item.heightPx,
                    onFrameResizeStart = onFrameResizeStart,
                    onFrameResizeDrag = onFrameResizeDrag,
                    onFrameResizeEnd = onFrameResizeEnd,
                )
            }
        }

        frameDraft?.let { draft ->
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0.35f),
            ) {
                val left = min(draft.startCorner.x, draft.endCorner.x)
                val right = max(draft.startCorner.x, draft.endCorner.x)
                val top = min(draft.startCorner.y, draft.endCorner.y)
                val bottom = max(draft.startCorner.y, draft.endCorner.y)
                val topLeft = WorldScreenTransformer.worldToScreen(
                    point = WorldPoint(left, top),
                    camera = cameraState,
                )
                val bottomRight = WorldScreenTransformer.worldToScreen(
                    point = WorldPoint(right, bottom),
                    camera = cameraState,
                )
                val rectWidth = (bottomRight.x - topLeft.x).coerceAtLeast(1f)
                val rectHeight = (bottomRight.y - topLeft.y).coerceAtLeast(1f)
                drawRoundRect(
                    color = Color(draft.colorArgb).copy(alpha = 0.14f),
                    topLeft = Offset(topLeft.x, topLeft.y),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = CornerRadius(frameCornerRadiusPx, frameCornerRadiusPx),
                )
                drawRoundRect(
                    color = Color(draft.colorArgb).copy(alpha = 0.86f),
                    topLeft = Offset(topLeft.x, topLeft.y),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = CornerRadius(frameCornerRadiusPx, frameCornerRadiusPx),
                    style = Stroke(width = frameBorderStrokePx),
                )
            }
        }

        if (showsSelectionOverlay) {
            val selectionDraftFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f)
            val selectionDraftStrokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            selectionDraft?.let { draft ->
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0.38f),
                ) {
                    val left = min(draft.startCorner.x, draft.endCorner.x)
                    val right = max(draft.startCorner.x, draft.endCorner.x)
                    val top = min(draft.startCorner.y, draft.endCorner.y)
                    val bottom = max(draft.startCorner.y, draft.endCorner.y)
                    val topLeft = WorldScreenTransformer.worldToScreen(
                        point = WorldPoint(left, top),
                        camera = cameraState,
                    )
                    val bottomRight = WorldScreenTransformer.worldToScreen(
                        point = WorldPoint(right, bottom),
                        camera = cameraState,
                    )
                    val rectWidth = (bottomRight.x - topLeft.x).coerceAtLeast(1f)
                    val rectHeight = (bottomRight.y - topLeft.y).coerceAtLeast(1f)
                    drawRoundRect(
                        color = selectionDraftFillColor,
                        topLeft = Offset(topLeft.x, topLeft.y),
                        size = Size(rectWidth, rectHeight),
                        cornerRadius = CornerRadius(frameCornerRadiusPx, frameCornerRadiusPx),
                    )
                    drawRoundRect(
                        color = selectionDraftStrokeColor,
                        topLeft = Offset(topLeft.x, topLeft.y),
                        size = Size(rectWidth, rectHeight),
                        cornerRadius = CornerRadius(frameCornerRadiusPx, frameCornerRadiusPx),
                        style = Stroke(
                            width = frameBorderStrokePx,
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(14f, 10f),
                                phase = 0f,
                            ),
                        ),
                    )
                }
            }

            selectionBounds?.let { bounds ->
                val topLeft = WorldScreenTransformer.worldToScreen(
                    point = WorldPoint(bounds.left, bounds.top),
                    camera = cameraState,
                )
                val bottomRight = WorldScreenTransformer.worldToScreen(
                    point = WorldPoint(bounds.right, bounds.bottom),
                    camera = cameraState,
                )
                val rectWidthPx = (bottomRight.x - topLeft.x).coerceAtLeast(1f)
                val rectHeightPx = (bottomRight.y - topLeft.y).coerceAtLeast(1f)
                val overlayColor = if (bounds.hasIcons) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                }
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = topLeft.x.roundToInt(),
                                y = topLeft.y.roundToInt(),
                            )
                        }
                        .requiredSize(
                            width = with(density) { rectWidthPx.toDp() },
                            height = with(density) { rectHeightPx.toDp() },
                        )
                        .zIndex(1.12f),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = overlayColor.copy(alpha = 0.10f),
                            cornerRadius = CornerRadius(frameCornerRadiusPx, frameCornerRadiusPx),
                        )
                        drawRoundRect(
                            color = overlayColor.copy(alpha = 0.94f),
                            cornerRadius = CornerRadius(frameCornerRadiusPx, frameCornerRadiusPx),
                            style = Stroke(
                                width = frameBorderStrokePx,
                                pathEffect = PathEffect.dashPathEffect(
                                    intervals = floatArrayOf(12f, 8f),
                                    phase = 0f,
                                ),
                            ),
                        )
                    }

                    if (bounds.canDelete) {
                        SelectionDeleteButton(
                            onClick = onSelectionDeleteTap,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = -DELETE_BUTTON_VERTICAL_OFFSET_DP)
                                .zIndex(1.2f),
                        )
                    }
                    if (bounds.canResize) {
                        SelectionResizeHandles(
                            boundsWidthPx = rectWidthPx,
                            boundsHeightPx = rectHeightPx,
                            onSelectionResizeStart = onSelectionResizeStart,
                            onSelectionResizeDrag = onSelectionResizeDrag,
                            onSelectionResizeEnd = onSelectionResizeEnd,
                        )
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0.4f),
        ) {
            strokes.forEach { stroke ->
                if (stroke.points.size < 2) return@forEach
                val path = Path()
                stroke.points.forEachIndexed { index, world ->
                    val screen = WorldScreenTransformer.worldToScreen(world, cameraState)
                    if (index == 0) {
                        path.moveTo(screen.x, screen.y)
                    } else {
                        path.lineTo(screen.x, screen.y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(stroke.colorArgb),
                    style = Stroke(
                        width = (stroke.widthWorld * scale).coerceIn(1.5f, 72f),
                    ),
                )
            }
        }

        val stickyLayouts = stickyNotes.map { note ->
            val center = WorldScreenTransformer.worldToScreen(note.center, cameraState)
            val noteSizePx = (note.sizeWorld * scale).coerceAtLeast(42f)
            val noteLeftPx = center.x - noteSizePx / 2f
            val noteTopPx = center.y - noteSizePx / 2f
            val noteText = note.text.ifBlank { stickyFallbackText }
            val stickyDesiredTextSizePx = (note.textSizeWorld * scale)
                .coerceIn(STICKY_TEXT_MIN_SIZE_PX, noteSizePx * STICKY_TEXT_MAX_SIZE_FRACTION)
            val stickyTextPaddingPerSidePx = (noteSizePx * STICKY_TEXT_INNER_PADDING_SIZE_FRACTION)
                .coerceIn(STICKY_TEXT_INNER_PADDING_MIN_PX, STICKY_TEXT_INNER_PADDING_MAX_PX)
            val stickyTextPaddingPx = stickyTextPaddingPerSidePx * 2f
            val stickyContentWidthPx = (noteSizePx - stickyTextPaddingPx).coerceAtLeast(12f)
            val stickyContentHeightPx = (noteSizePx - stickyTextPaddingPx).coerceAtLeast(12f)
            val stickyTextLayout = fitStickyTextLayout(
                text = noteText,
                desiredTextSizePx = stickyDesiredTextSizePx,
                contentWidthPx = stickyContentWidthPx,
                contentHeightPx = stickyContentHeightPx,
            )
            StickyLayoutState(
                note = note,
                noteText = noteText,
                center = center,
                noteSizePx = noteSizePx,
                leftPx = noteLeftPx,
                topPx = noteTopPx,
                textPaddingPx = stickyTextPaddingPerSidePx,
                textContentWidthPx = stickyContentWidthPx,
                textContentHeightPx = stickyContentHeightPx,
                textLayout = stickyTextLayout,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0.6f),
        ) {
            stickyLayouts.forEach { layout ->
                val noteRectTopLeft = Offset(layout.leftPx, layout.topPx)
                drawRect(
                    color = Color(layout.note.colorArgb).copy(alpha = 0.92f),
                    topLeft = noteRectTopLeft,
                    size = Size(layout.noteSizePx, layout.noteSizePx),
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.14f),
                        ),
                        startY = layout.topPx + layout.noteSizePx * 0.70f,
                        endY = layout.topPx + layout.noteSizePx,
                    ),
                    topLeft = Offset(layout.leftPx, layout.topPx + layout.noteSizePx * 0.70f),
                    size = Size(layout.noteSizePx, layout.noteSizePx * 0.30f),
                )
                drawStickyNoteText(
                    text = layout.noteText,
                    paint = stickyTextPaint,
                    contentLeftPx = layout.leftPx + layout.textPaddingPx,
                    contentTopPx = layout.topPx + layout.textPaddingPx,
                    contentWidthPx = layout.textContentWidthPx,
                    contentHeightPx = layout.textContentHeightPx,
                    textSizePx = layout.textLayout.textSizePx,
                    lineHeightPx = layout.textLayout.lineHeightPx,
                    maxLines = layout.textLayout.maxLines,
                )
            }
        }

        stickyLayouts.forEach { layout ->
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = layout.leftPx
                        translationY = layout.topPx
                    }
                    .size(with(density) { layout.noteSizePx.toDp() })
                    .zIndex(0.61f)
                    .objectMoveModifier(
                        enabled = allowsObjectMove,
                        target = CanvasObjectDragTarget.Sticky(layout.note.id),
                        nodeTopLeftScreen = ScreenPoint(
                            x = layout.leftPx,
                            y = layout.topPx,
                        ),
                        canvasWidthPx = cameraState.viewportWidthPx.toFloat(),
                        canvasHeightPx = cameraState.viewportHeightPx.toFloat(),
                        autoPanZonePx = autoPanZonePx,
                        autoPanMaxStepPx = autoPanMaxStepPx,
                        onObjectDragStart = onObjectDragStart,
                        onObjectDragDelta = onObjectDragDelta,
                        onObjectDragEnd = onObjectDragEnd,
                        onObjectDragCancel = onObjectDragCancel,
                        onAutoPanDelta = onAutoPanDelta,
                    )
                    .then(
                        if (allowsObjectTap) {
                            Modifier.pointerInput(layout.note.id, editState.selectedTool, layout.noteSizePx) {
                                detectTapGestures(
                                    onTap = { tapOffset ->
                                        val centerTap = if (editState.selectedTool == CanvasEditToolId.Delete) {
                                            true
                                        } else {
                                            (tapOffset - Offset(layout.noteSizePx / 2f, layout.noteSizePx / 2f))
                                                .getDistance() <= layout.noteSizePx * 0.22f
                                        }
                                        onStickyTap(layout.note.id, centerTap)
                                    },
                                    onLongPress = {
                                        if (editState.selectedTool == CanvasEditToolId.Move) {
                                            onStickyLongPress(layout.note.id)
                                        }
                                    },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
        }

        val textLayouts = textObjects.map { textObject ->
            val screen = WorldScreenTransformer.worldToScreen(textObject.position, cameraState)
            val textSizePx = (textObject.textSizeWorld * scale).coerceIn(12f, 128f)
            val textContent = textObject.text.ifBlank { textFallbackText }
            val estimatedBounds = estimateTextBoundsPx(
                text = textContent,
                textSizePx = textSizePx,
            )
            val textLeftPx = screen.x - estimatedBounds.widthPx / 2f
            val textTopPx = screen.y - estimatedBounds.heightPx / 2f
            TextObjectLayoutState(
                textObject = textObject,
                text = textContent,
                textSizePx = textSizePx,
                leftPx = textLeftPx,
                topPx = textTopPx,
                widthPx = estimatedBounds.widthPx,
                heightPx = estimatedBounds.heightPx,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0.7f),
        ) {
            textLayouts.forEach { layout ->
                drawFloatingText(
                    text = layout.text,
                    paint = canvasTextPaint,
                    leftPx = layout.leftPx,
                    topPx = layout.topPx,
                    textSizePx = layout.textSizePx,
                    color = Color(layout.textObject.colorArgb),
                )
            }
        }

        textLayouts.forEach { layout ->
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = layout.leftPx
                        translationY = layout.topPx
                    }
                    .requiredSize(
                        width = with(density) { layout.widthPx.toDp() },
                        height = with(density) { layout.heightPx.toDp() },
                    )
                    .zIndex(0.71f)
                    .objectMoveModifier(
                        enabled = allowsObjectMove,
                        target = CanvasObjectDragTarget.Text(layout.textObject.id),
                        nodeTopLeftScreen = ScreenPoint(
                            x = layout.leftPx,
                            y = layout.topPx,
                        ),
                        canvasWidthPx = cameraState.viewportWidthPx.toFloat(),
                        canvasHeightPx = cameraState.viewportHeightPx.toFloat(),
                        autoPanZonePx = autoPanZonePx,
                        autoPanMaxStepPx = autoPanMaxStepPx,
                        onObjectDragStart = onObjectDragStart,
                        onObjectDragDelta = onObjectDragDelta,
                        onObjectDragEnd = onObjectDragEnd,
                        onObjectDragCancel = onObjectDragCancel,
                        onAutoPanDelta = onAutoPanDelta,
                    )
                    .then(
                        if (allowsObjectTap) {
                            Modifier.pointerInput(layout.textObject.id) {
                                detectTapGestures(
                                    onTap = { onTextTap(layout.textObject.id) },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
        }

        CanvasWidgetLayer(
            cameraState = cameraState,
            widgets = widgets,
            isWidgetMode = isWidgetMode,
            selectedWidgetIdForResize = selectedWidgetIdForResize,
            onWidgetTap = onWidgetTap,
            onWidgetBackgroundTap = onWidgetBackgroundTap,
            onWidgetResizeStart = onWidgetResizeStart,
            onWidgetResizeDrag = onWidgetResizeDrag,
            onWidgetResizeEnd = onWidgetResizeEnd,
            onWidgetDeleteTap = onWidgetDeleteTap,
            onObjectDragStart = onObjectDragStart,
            onObjectDragDelta = onObjectDragDelta,
            onObjectDragEnd = onObjectDragEnd,
            onObjectDragCancel = onObjectDragCancel,
            onAutoPanDelta = onAutoPanDelta,
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0.95f),
        ) {
            snapGuides.forEach { guide ->
                when (guide.orientation) {
                    CanvasSnapOrientation.Vertical -> {
                        val x = WorldScreenTransformer.worldToScreen(
                            point = WorldPoint(guide.worldCoordinate, cameraState.worldCenter.y),
                            camera = cameraState,
                        ).x
                        drawLine(
                            color = guideColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.6f,
                        )
                    }

                    CanvasSnapOrientation.Horizontal -> {
                        val y = WorldScreenTransformer.worldToScreen(
                            point = WorldPoint(cameraState.worldCenter.x, guide.worldCoordinate),
                            camera = cameraState,
                        ).y
                        drawLine(
                            color = guideColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.6f,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasWidgetLayer(
    cameraState: CameraState,
    widgets: List<CanvasWidgetUiState>,
    isWidgetMode: Boolean,
    selectedWidgetIdForResize: String?,
    onWidgetTap: (id: String) -> Unit,
    onWidgetBackgroundTap: () -> Unit,
    onWidgetResizeStart: (CanvasFrameResizeHandle) -> Unit,
    onWidgetResizeDrag: (CanvasFrameResizeHandle, ScreenPoint) -> Unit,
    onWidgetResizeEnd: () -> Unit,
    onWidgetDeleteTap: () -> Unit,
    onObjectDragStart: (CanvasObjectDragTarget) -> Unit,
    onObjectDragDelta: (CanvasObjectDragTarget, ScreenPoint) -> Unit,
    onObjectDragEnd: () -> Unit,
    onObjectDragCancel: () -> Unit,
    onAutoPanDelta: (ScreenPoint) -> Unit,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val scale = cameraState.scale
    val hasAnalogWidget = remember(widgets) {
        widgets.any { widget -> widget.type == CanvasWidgetType.ClockAnalog }
    }
    val hasClockWidget = remember(widgets) {
        widgets.any { widget ->
            widget.type == CanvasWidgetType.ClockDigital || widget.type == CanvasWidgetType.ClockAnalog
        }
    }
    val hasWeatherWidget = remember(widgets) {
        widgets.any { widget -> widget.type == CanvasWidgetType.Weather }
    }
    val hasNotificationsWidget = remember(widgets) {
        widgets.any { widget -> widget.type == CanvasWidgetType.Notifications }
    }
    val hasCalendarWidget = remember(widgets) {
        widgets.any { widget -> widget.type == CanvasWidgetType.Calendar }
    }
    val widgetTime = rememberWidgetTime(
        enabled = hasClockWidget,
        updateEverySecond = hasAnalogWidget,
    )
    val digitalClockText = remember(widgetTime) {
        widgetTime.format(WIDGET_DIGITAL_TIME_FORMATTER)
    }
    val weatherSnapshot = rememberWeatherWidgetSnapshot(
        context = context,
        enabled = hasWeatherWidget,
    )
    val notificationsEnabled = rememberNotificationsEnabledFlag(
        context = context,
        enabled = hasNotificationsWidget,
    )
    val notificationAccessEnabled = rememberNotificationListenerAccessFlag(
        context = context,
        enabled = hasNotificationsWidget,
    )
    val liveNotificationEntries = rememberNotificationEntries(enabled = hasNotificationsWidget)
    val calendarSnapshot = rememberCalendarWidgetSnapshot(
        context = context,
        enabled = hasCalendarWidget,
    )

    val weatherTitleText = stringResource(id = R.string.widget_weather_title)
    val weatherHourlyPrefix = stringResource(id = R.string.widget_weather_hourly_prefix)
    val weatherDailyPrefix = stringResource(id = R.string.widget_weather_daily_prefix)
    val notificationsTitleText = stringResource(id = R.string.widget_notifications_title)
    val notificationAccessRequiredText = stringResource(id = R.string.widget_notifications_access_required)
    val notificationPermissionRequiredText = stringResource(id = R.string.widget_notifications_permission_required)
    val notificationEmptyText = stringResource(id = R.string.widget_notifications_empty)
    val calendarTitleText = stringResource(id = R.string.widget_calendar_title)
    val notificationSubtitle = when {
        !notificationAccessEnabled -> stringResource(id = R.string.widget_notifications_access_hint)
        !notificationsEnabled -> stringResource(id = R.string.widget_notifications_permission_hint)
        liveNotificationEntries.isEmpty() -> stringResource(id = R.string.widget_notifications_empty_hint)
        else -> stringResource(
            id = R.string.widget_notifications_count,
            liveNotificationEntries.size,
        )
    }

    val widgetSelectionColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.94f)
    val frameCornerRadiusPx = with(density) { FRAME_CORNER_RADIUS_DP.toPx() }
    val frameBorderStrokePx = with(density) { FRAME_BORDER_STROKE_DP.toPx() }
    val autoPanZonePx = with(density) { EDGE_AUTO_PAN_ZONE_DP.toPx() }
    val autoPanMaxStepPx = with(density) { EDGE_AUTO_PAN_MAX_STEP_DP.toPx() }
    val widgetFillTopColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    val widgetFillBottomColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
    val widgetBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
    val widgetSecondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    val widgetAccentColor = MaterialTheme.colorScheme.primary
    val widgetNotificationsEnabledColor = MaterialTheme.colorScheme.primary
    val widgetNotificationsDisabledColor = MaterialTheme.colorScheme.error
    val widgetFillGradientColors = remember(widgetFillTopColor, widgetFillBottomColor) {
        listOf(widgetFillTopColor, widgetFillBottomColor)
    }

    val widgetClockPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.CENTER
            isLinearText = true
        }
    }
    val widgetTitlePaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.LEFT
            isLinearText = true
        }
    }
    val widgetBodyPaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.LEFT
            isLinearText = true
        }
    }
    val widgetSubtitlePaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            textAlign = AndroidPaint.Align.LEFT
            isLinearText = true
        }
    }
    val typography = MaterialTheme.typography
    SideEffect {
        applyPaintTypography(widgetClockPaint, typography.headlineMedium)
        applyPaintTypography(widgetTitlePaint, typography.labelLarge)
        applyPaintTypography(widgetBodyPaint, typography.titleMedium)
        applyPaintTypography(widgetSubtitlePaint, typography.bodySmall)
    }

    val widgetLayouts = remember(widgets, cameraState) {
        widgets.map { widget ->
            val center = WorldScreenTransformer.worldToScreen(widget.center, cameraState)
            val widthPx = (widget.widthWorld * scale).coerceAtLeast(WIDGET_MIN_WIDTH_RENDER_SIZE_PX)
            val heightPx = (widget.heightWorld * scale).coerceAtLeast(WIDGET_MIN_HEIGHT_RENDER_SIZE_PX)
            val leftPx = center.x - widthPx / 2f
            val topPx = center.y - heightPx / 2f
            val centerZoneMinWidthPx = min(48f, widthPx)
            val centerZoneMinHeightPx = min(36f, heightPx)
            val centerZoneWidthPx = (widthPx * WIDGET_CENTER_DRAG_WIDTH_FACTOR)
                .coerceIn(centerZoneMinWidthPx, widthPx)
            val centerZoneHeightPx = (heightPx * WIDGET_CENTER_DRAG_HEIGHT_FACTOR)
                .coerceIn(centerZoneMinHeightPx, heightPx)
            WidgetLayoutState(
                widget = widget,
                center = center,
                widthPx = widthPx,
                heightPx = heightPx,
                leftPx = leftPx,
                topPx = topPx,
                centerZoneLeftPx = center.x - centerZoneWidthPx / 2f,
                centerZoneTopPx = center.y - centerZoneHeightPx / 2f,
                centerZoneWidthPx = centerZoneWidthPx,
                centerZoneHeightPx = centerZoneHeightPx,
            )
        }
    }
    val hasVisibleNotificationsWidget = remember(
        widgetLayouts,
        cameraState.viewportWidthPx,
        cameraState.viewportHeightPx,
    ) {
        val viewportWidthPx = cameraState.viewportWidthPx.toFloat()
        val viewportHeightPx = cameraState.viewportHeightPx.toFloat()
        widgetLayouts.any { layout ->
            layout.widget.type == CanvasWidgetType.Notifications &&
                layout.leftPx < viewportWidthPx &&
                layout.topPx < viewportHeightPx &&
                (layout.leftPx + layout.widthPx) > 0f &&
                (layout.topPx + layout.heightPx) > 0f
        }
    }
    val tickerEnabled = hasVisibleNotificationsWidget &&
        notificationAccessEnabled &&
        notificationsEnabled &&
        liveNotificationEntries.size > 1
    val tickerClockMs = rememberTickerClock(enabled = tickerEnabled)

    if (isWidgetMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0.75f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onWidgetBackgroundTap() },
                    )
                },
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(0.78f),
    ) {
        widgetLayouts.forEach { layout ->
            val widget = layout.widget
            val cornerRadius = CornerRadius(WIDGET_CORNER_RADIUS_PX, WIDGET_CORNER_RADIUS_PX)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = widgetFillGradientColors,
                    startY = layout.topPx,
                    endY = layout.topPx + layout.heightPx,
                ),
                topLeft = Offset(layout.leftPx, layout.topPx),
                size = Size(layout.widthPx, layout.heightPx),
                cornerRadius = cornerRadius,
            )
            drawRoundRect(
                color = widgetBorderColor,
                topLeft = Offset(layout.leftPx, layout.topPx),
                size = Size(layout.widthPx, layout.heightPx),
                cornerRadius = cornerRadius,
                style = Stroke(width = WIDGET_BORDER_STROKE_PX),
            )

            clipRect(
                left = layout.leftPx,
                top = layout.topPx,
                right = layout.leftPx + layout.widthPx,
                bottom = layout.topPx + layout.heightPx,
            ) {
                when (widget.type) {
                    CanvasWidgetType.ClockDigital -> {
                        val clockTextSizePx = (min(layout.widthPx, layout.heightPx) * WIDGET_CLOCK_TEXT_SIZE_FACTOR)
                            .coerceIn(
                                WIDGET_CLOCK_MIN_TEXT_SIZE_PX,
                                layout.heightPx * WIDGET_CLOCK_MAX_TEXT_HEIGHT_FRACTION,
                            )
                        widgetClockPaint.textSize = clockTextSizePx
                        widgetClockPaint.color = widgetAccentColor.toArgb()
                        val maxClockWidth = (layout.widthPx * 0.86f).coerceAtLeast(18f)
                        val clockLine = ellipsizeToWidth(
                            text = digitalClockText,
                            paint = widgetClockPaint,
                            maxWidthPx = maxClockWidth,
                        )
                        val metrics = widgetClockPaint.fontMetrics
                        val baseline = layout.topPx + (layout.heightPx - metrics.bottom + metrics.top) / 2f - metrics.top
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                clockLine,
                                layout.center.x,
                                baseline,
                                widgetClockPaint,
                            )
                        }
                    }

                    CanvasWidgetType.ClockAnalog -> {
                        val centerOffset = Offset(layout.center.x, layout.center.y)
                        val radius = (min(layout.widthPx, layout.heightPx) * WIDGET_ANALOG_FACE_DIAMETER_FACTOR) / 2f
                        val outerStroke = (radius * WIDGET_ANALOG_OUTER_STROKE_FACTOR)
                            .coerceAtLeast(WIDGET_ANALOG_MIN_STROKE_PX)
                        val markerStroke = (outerStroke * 0.9f).coerceAtLeast(1f)
                        val markerInnerRadius = radius * WIDGET_ANALOG_MARKER_INNER_FACTOR

                        drawCircle(
                            color = widgetAccentColor.copy(alpha = WIDGET_ANALOG_FACE_FILL_ALPHA),
                            radius = radius,
                            center = centerOffset,
                        )
                        drawCircle(
                            color = widgetBorderColor.copy(alpha = 0.68f),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = outerStroke),
                        )
                        for (hourMark in 0 until 12) {
                            val angleRad = ((hourMark / 12f) * (2f * PI.toFloat())) - (PI.toFloat() / 2f)
                            val start = Offset(
                                x = centerOffset.x + cos(angleRad) * markerInnerRadius,
                                y = centerOffset.y + sin(angleRad) * markerInnerRadius,
                            )
                            val end = Offset(
                                x = centerOffset.x + cos(angleRad) * (radius - outerStroke * 0.8f),
                                y = centerOffset.y + sin(angleRad) * (radius - outerStroke * 0.8f),
                            )
                            drawLine(
                                color = widgetBorderColor.copy(alpha = 0.78f),
                                start = start,
                                end = end,
                                strokeWidth = markerStroke,
                            )
                        }

                        val secondProgress = widgetTime.second / 60f
                        val minuteProgress = (widgetTime.minute + secondProgress) / 60f
                        val hourProgress = ((widgetTime.hour % 12) + minuteProgress) / 12f
                        drawWidgetClockHand(
                            center = centerOffset,
                            radius = radius,
                            progress = hourProgress,
                            lengthFactor = WIDGET_ANALOG_HOUR_HAND_LENGTH_FACTOR,
                            strokeWidth = (radius * 0.10f).coerceIn(2.4f, 6.8f),
                            color = widgetBorderColor.copy(alpha = 0.95f),
                        )
                        drawWidgetClockHand(
                            center = centerOffset,
                            radius = radius,
                            progress = minuteProgress,
                            lengthFactor = WIDGET_ANALOG_MINUTE_HAND_LENGTH_FACTOR,
                            strokeWidth = (radius * 0.06f).coerceIn(1.8f, 4.8f),
                            color = widgetAccentColor.copy(alpha = 0.92f),
                        )
                        drawWidgetClockHand(
                            center = centerOffset,
                            radius = radius,
                            progress = secondProgress,
                            lengthFactor = WIDGET_ANALOG_SECOND_HAND_LENGTH_FACTOR,
                            strokeWidth = (radius * 0.028f).coerceIn(1.2f, 2.6f),
                            color = widgetAccentColor.copy(alpha = 0.80f),
                        )
                        drawCircle(
                            color = widgetAccentColor,
                            radius = (radius * WIDGET_ANALOG_CENTER_DOT_FACTOR).coerceAtLeast(2f),
                            center = centerOffset,
                        )
                    }

                    CanvasWidgetType.Weather -> {
                        val horizontalPadding = max(WIDGET_INFO_HORIZONTAL_PADDING_PX, layout.widthPx * 0.08f)
                        val titleSize = (layout.heightPx * 0.12f).coerceAtLeast(11f)
                        val bodySize = (layout.heightPx * 0.24f)
                            .coerceAtLeast(18f)
                            .coerceAtMost(layout.heightPx * WIDGET_INFO_BODY_MAX_HEIGHT_FRACTION)
                        val subtitleSize = (layout.heightPx * 0.11f)
                            .coerceAtLeast(10f)
                            .coerceAtMost(layout.heightPx * WIDGET_INFO_SUBTITLE_MAX_HEIGHT_FRACTION)
                        widgetTitlePaint.textSize = titleSize
                        widgetTitlePaint.color = widgetSecondaryTextColor.toArgb()
                        widgetBodyPaint.textSize = bodySize
                        widgetBodyPaint.color = if (weatherSnapshot.isResolved) {
                            widgetAccentColor.toArgb()
                        } else {
                            widgetSecondaryTextColor.toArgb()
                        }
                        widgetSubtitlePaint.textSize = subtitleSize
                        widgetSubtitlePaint.color = widgetSecondaryTextColor.toArgb()

                        val titleBaseline = layout.topPx + layout.heightPx * 0.20f
                        val bodyBaseline = layout.topPx + layout.heightPx * 0.45f
                        val summaryBaseline = layout.topPx + layout.heightPx * 0.61f
                        val hourlyBaseline = layout.topPx + layout.heightPx * 0.77f
                        val dailyBaseline = layout.topPx + layout.heightPx * 0.91f
                        val left = layout.leftPx + horizontalPadding
                        val availableWidth = (layout.widthPx - horizontalPadding * 2f).coerceAtLeast(24f)
                        val titleLine = ellipsizeToWidth(
                            text = weatherTitleText,
                            paint = widgetTitlePaint,
                            maxWidthPx = availableWidth,
                        )
                        val temperatureLine = ellipsizeToWidth(
                            text = weatherSnapshot.temperatureText,
                            paint = widgetBodyPaint,
                            maxWidthPx = availableWidth,
                        )
                        val summaryLine = ellipsizeToWidth(
                            text = "${weatherSnapshot.condition} | ${weatherSnapshot.locationLabel}",
                            paint = widgetSubtitlePaint,
                            maxWidthPx = availableWidth,
                        )
                        val hourlyLine = ellipsizeToWidth(
                            text = "$weatherHourlyPrefix: ${weatherSnapshot.hourlySummary}",
                            paint = widgetSubtitlePaint,
                            maxWidthPx = availableWidth,
                        )
                        val dailyLine = ellipsizeToWidth(
                            text = "$weatherDailyPrefix: ${weatherSnapshot.dailySummary}",
                            paint = widgetSubtitlePaint,
                            maxWidthPx = availableWidth,
                        )

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                titleLine,
                                left,
                                titleBaseline,
                                widgetTitlePaint,
                            )
                            canvas.nativeCanvas.drawText(
                                temperatureLine,
                                left,
                                bodyBaseline,
                                widgetBodyPaint,
                            )
                            canvas.nativeCanvas.drawText(
                                summaryLine,
                                left,
                                summaryBaseline,
                                widgetSubtitlePaint,
                            )
                            canvas.nativeCanvas.drawText(
                                hourlyLine,
                                left,
                                hourlyBaseline,
                                widgetSubtitlePaint,
                            )
                            canvas.nativeCanvas.drawText(
                                dailyLine,
                                left,
                                dailyBaseline,
                                widgetSubtitlePaint,
                            )
                        }
                    }

                    CanvasWidgetType.Notifications -> {
                        val horizontalPadding = max(WIDGET_INFO_HORIZONTAL_PADDING_PX, layout.widthPx * 0.08f)
                        val titleSize = (layout.heightPx * 0.14f).coerceAtLeast(11f)
                        val bodySize = (layout.heightPx * 0.22f)
                            .coerceAtLeast(13f)
                            .coerceAtMost(layout.heightPx * WIDGET_INFO_BODY_MAX_HEIGHT_FRACTION)
                        val subtitleSize = (layout.heightPx * 0.11f)
                            .coerceAtLeast(10f)
                            .coerceAtMost(layout.heightPx * WIDGET_INFO_SUBTITLE_MAX_HEIGHT_FRACTION)
                        val hasLiveTicker = notificationAccessEnabled &&
                            notificationsEnabled &&
                            liveNotificationEntries.isNotEmpty()
                        val fallbackTickerText = when {
                            !notificationAccessEnabled -> notificationAccessRequiredText
                            !notificationsEnabled -> notificationPermissionRequiredText
                            else -> notificationEmptyText
                        }
                        val statusColor = if (hasLiveTicker) {
                            widgetNotificationsEnabledColor
                        } else if (!notificationAccessEnabled || !notificationsEnabled) {
                            widgetNotificationsDisabledColor
                        } else {
                            widgetSecondaryTextColor
                        }
                        widgetTitlePaint.textSize = titleSize
                        widgetTitlePaint.color = widgetSecondaryTextColor.toArgb()
                        widgetBodyPaint.textSize = bodySize
                        widgetBodyPaint.color = statusColor.toArgb()
                        widgetSubtitlePaint.textSize = subtitleSize
                        widgetSubtitlePaint.color = widgetSecondaryTextColor.toArgb()

                        val titleBaseline = layout.topPx + layout.heightPx * 0.24f
                        val bodyBaseline = layout.topPx + layout.heightPx * 0.58f
                        val subtitleBaseline = layout.topPx + layout.heightPx * 0.82f
                        val left = layout.leftPx + horizontalPadding
                        val availableWidth = (layout.widthPx - horizontalPadding * 2f).coerceAtLeast(24f)
                        val detailLine = ellipsizeToWidth(
                            text = notificationSubtitle,
                            paint = widgetSubtitlePaint,
                            maxWidthPx = availableWidth,
                        )
                        val tickerLineHeight = (
                            widgetBodyPaint.fontMetrics.descent - widgetBodyPaint.fontMetrics.ascent
                            ) * WIDGET_NOTIFICATION_LINE_HEIGHT_MULTIPLIER
                        val tickerRowTop = bodyBaseline + widgetBodyPaint.fontMetrics.ascent
                        val tickerRowBottom = tickerRowTop + tickerLineHeight

                        val tickerCurrentLine: String
                        val tickerNextLine: String
                        val tickerScrollOffsetY: Float
                        if (hasLiveTicker) {
                            val cycleDuration = WIDGET_NOTIFICATION_STATIONARY_MS + WIDGET_NOTIFICATION_SCROLL_MS
                            val cycleTick = (tickerClockMs / cycleDuration).toInt()
                            val phaseMs = tickerClockMs % cycleDuration
                            val currentIndex = cycleTick % liveNotificationEntries.size
                            val nextIndex = (currentIndex + 1) % liveNotificationEntries.size
                            tickerCurrentLine = ellipsizeToWidth(
                                text = liveNotificationEntries[currentIndex].text,
                                paint = widgetBodyPaint,
                                maxWidthPx = availableWidth,
                            )
                            tickerNextLine = ellipsizeToWidth(
                                text = liveNotificationEntries[nextIndex].text,
                                paint = widgetBodyPaint,
                                maxWidthPx = availableWidth,
                            )
                            tickerScrollOffsetY = if (
                                liveNotificationEntries.size <= 1 ||
                                phaseMs < WIDGET_NOTIFICATION_STATIONARY_MS
                            ) {
                                0f
                            } else {
                                val progress = (phaseMs - WIDGET_NOTIFICATION_STATIONARY_MS).toFloat() /
                                    WIDGET_NOTIFICATION_SCROLL_MS.toFloat()
                                tickerLineHeight * progress.coerceIn(0f, 1f)
                            }
                        } else {
                            tickerCurrentLine = ellipsizeToWidth(
                                text = fallbackTickerText,
                                paint = widgetBodyPaint,
                                maxWidthPx = availableWidth,
                            )
                            tickerNextLine = tickerCurrentLine
                            tickerScrollOffsetY = 0f
                        }
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                notificationsTitleText,
                                left,
                                titleBaseline,
                                widgetTitlePaint,
                            )
                            val clipSave = canvas.nativeCanvas.save()
                            canvas.nativeCanvas.clipRect(
                                left,
                                tickerRowTop,
                                left + availableWidth,
                                tickerRowBottom,
                            )
                            if (hasLiveTicker && tickerScrollOffsetY > 0f) {
                                canvas.nativeCanvas.drawText(
                                    tickerCurrentLine,
                                    left,
                                    bodyBaseline - tickerScrollOffsetY,
                                    widgetBodyPaint,
                                )
                                canvas.nativeCanvas.drawText(
                                    tickerNextLine,
                                    left,
                                    bodyBaseline + tickerLineHeight - tickerScrollOffsetY,
                                    widgetBodyPaint,
                                )
                            } else {
                                canvas.nativeCanvas.drawText(
                                    tickerCurrentLine,
                                    left,
                                    bodyBaseline,
                                    widgetBodyPaint,
                                )
                            }
                            canvas.nativeCanvas.restoreToCount(clipSave)
                            canvas.nativeCanvas.drawText(
                                detailLine,
                                left,
                                subtitleBaseline,
                                widgetSubtitlePaint,
                            )
                        }
                    }

                    CanvasWidgetType.Calendar -> {
                        val horizontalPadding = max(WIDGET_INFO_HORIZONTAL_PADDING_PX, layout.widthPx * 0.08f)
                        val titleSize = (layout.heightPx * 0.14f).coerceAtLeast(11f)
                        val bodySize = (layout.heightPx * 0.22f)
                            .coerceAtLeast(13f)
                            .coerceAtMost(layout.heightPx * WIDGET_INFO_BODY_MAX_HEIGHT_FRACTION)
                        val subtitleSize = (layout.heightPx * 0.11f)
                            .coerceAtLeast(10f)
                            .coerceAtMost(layout.heightPx * WIDGET_INFO_SUBTITLE_MAX_HEIGHT_FRACTION)
                        widgetTitlePaint.textSize = titleSize
                        widgetTitlePaint.color = widgetSecondaryTextColor.toArgb()
                        widgetBodyPaint.textSize = bodySize
                        widgetBodyPaint.color = if (calendarSnapshot.isResolved) {
                            widgetAccentColor.toArgb()
                        } else {
                            widgetSecondaryTextColor.toArgb()
                        }
                        widgetSubtitlePaint.textSize = subtitleSize
                        widgetSubtitlePaint.color = widgetSecondaryTextColor.toArgb()

                        val titleBaseline = layout.topPx + layout.heightPx * 0.23f
                        val bodyBaseline = layout.topPx + layout.heightPx * 0.52f
                        val subtitleBaseline = layout.topPx + layout.heightPx * 0.74f
                        val footerBaseline = layout.topPx + layout.heightPx * 0.90f
                        val left = layout.leftPx + horizontalPadding
                        val availableWidth = (layout.widthPx - horizontalPadding * 2f).coerceAtLeast(24f)

                        val titleLine = ellipsizeToWidth(
                            text = calendarTitleText,
                            paint = widgetTitlePaint,
                            maxWidthPx = availableWidth,
                        )
                        val primaryLine = ellipsizeToWidth(
                            text = calendarSnapshot.primaryLine,
                            paint = widgetBodyPaint,
                            maxWidthPx = availableWidth,
                        )
                        val secondaryLine = ellipsizeToWidth(
                            text = calendarSnapshot.secondaryLine,
                            paint = widgetSubtitlePaint,
                            maxWidthPx = availableWidth,
                        )
                        val footerLine = ellipsizeToWidth(
                            text = calendarSnapshot.footerLine,
                            paint = widgetSubtitlePaint,
                            maxWidthPx = availableWidth,
                        )

                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                titleLine,
                                left,
                                titleBaseline,
                                widgetTitlePaint,
                            )
                            canvas.nativeCanvas.drawText(
                                primaryLine,
                                left,
                                bodyBaseline,
                                widgetBodyPaint,
                            )
                            canvas.nativeCanvas.drawText(
                                secondaryLine,
                                left,
                                subtitleBaseline,
                                widgetSubtitlePaint,
                            )
                            canvas.nativeCanvas.drawText(
                                footerLine,
                                left,
                                footerBaseline,
                                widgetSubtitlePaint,
                            )
                        }
                    }
                }
            }
        }
    }

    widgetLayouts.forEach { layout ->
        val widget = layout.widget
        val isSelected = isWidgetMode && selectedWidgetIdForResize == widget.id

        if (isWidgetMode) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = layout.leftPx
                        translationY = layout.topPx
                    }
                    .requiredSize(
                        width = with(density) { layout.widthPx.toDp() },
                        height = with(density) { layout.heightPx.toDp() },
                    )
                    .zIndex(0.79f)
                    .pointerInput(widget.id) {
                        detectTapGestures(
                            onTap = { onWidgetTap(widget.id) },
                        )
                    },
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = layout.centerZoneLeftPx
                        translationY = layout.centerZoneTopPx
                    }
                    .requiredSize(
                        width = with(density) { layout.centerZoneWidthPx.toDp() },
                        height = with(density) { layout.centerZoneHeightPx.toDp() },
                    )
                    .zIndex(0.81f)
                    .objectMoveModifier(
                        enabled = true,
                        target = CanvasObjectDragTarget.Widget(widget.id),
                        nodeTopLeftScreen = ScreenPoint(
                            x = layout.centerZoneLeftPx,
                            y = layout.centerZoneTopPx,
                        ),
                        canvasWidthPx = cameraState.viewportWidthPx.toFloat(),
                        canvasHeightPx = cameraState.viewportHeightPx.toFloat(),
                        autoPanZonePx = autoPanZonePx,
                        autoPanMaxStepPx = autoPanMaxStepPx,
                        onObjectDragStart = onObjectDragStart,
                        onObjectDragDelta = onObjectDragDelta,
                        onObjectDragEnd = onObjectDragEnd,
                        onObjectDragCancel = onObjectDragCancel,
                        onAutoPanDelta = onAutoPanDelta,
                    ),
            )
            Canvas(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = layout.leftPx
                        translationY = layout.topPx
                    }
                    .requiredSize(
                        width = with(density) { layout.widthPx.toDp() },
                        height = with(density) { layout.heightPx.toDp() },
                    )
                    .zIndex(0.82f),
            ) {
                drawRoundRect(
                    color = widgetSelectionColor,
                    cornerRadius = CornerRadius(frameCornerRadiusPx, frameCornerRadiusPx),
                    style = Stroke(
                        width = frameBorderStrokePx,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(12f, 8f),
                            phase = 0f,
                        ),
                    ),
                )
            }
            FrameResizeHandles(
                frameId = widget.id,
                frameLeftPx = layout.leftPx,
                frameTopPx = layout.topPx,
                frameWidthPx = layout.widthPx,
                frameHeightPx = layout.heightPx,
                onFrameResizeStart = { _, handle -> onWidgetResizeStart(handle) },
                onFrameResizeDrag = { _, handle, delta -> onWidgetResizeDrag(handle, delta) },
                onFrameResizeEnd = onWidgetResizeEnd,
            )
            SelectionDeleteButton(
                onClick = onWidgetDeleteTap,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = layout.center.x - with(density) { 18.dp.toPx() }
                        translationY = layout.topPx - with(density) { DELETE_BUTTON_VERTICAL_OFFSET_DP.toPx() }
                    }
                    .zIndex(0.84f),
            )
        }
    }
}

@Composable
private fun rememberWidgetTime(
    enabled: Boolean,
    updateEverySecond: Boolean,
): LocalTime {
    val currentTime by produceState(
        initialValue = LocalTime.now(),
        key1 = enabled,
        key2 = updateEverySecond,
    ) {
        if (!enabled) {
            value = LocalTime.now()
            return@produceState
        }
        while (true) {
            val now = LocalTime.now()
            value = now
            val millisUntilNextTick = if (updateEverySecond) {
                1_000L - (now.nano / 1_000_000L)
            } else {
                ((60 - now.second).coerceAtLeast(1) * 1_000L) - (now.nano / 1_000_000L)
            }
            delay(millisUntilNextTick.coerceAtLeast(120L))
        }
    }
    return currentTime
}

@Composable
private fun rememberNotificationsEnabledFlag(
    context: Context,
    enabled: Boolean,
): Boolean {
    val appContext = remember(context) { context.applicationContext }
    val notificationsEnabled by produceState(
        initialValue = NotificationManagerCompat.from(appContext).areNotificationsEnabled(),
        key1 = appContext,
        key2 = enabled,
    ) {
        if (!enabled) {
            value = false
            return@produceState
        }
        while (true) {
            value = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
            delay(WIDGET_NOTIFICATIONS_POLL_MS)
        }
    }
    return notificationsEnabled
}

@Composable
private fun rememberNotificationListenerAccessFlag(
    context: Context,
    enabled: Boolean,
): Boolean {
    val appContext = remember(context) { context.applicationContext }
    val accessEnabled by produceState(
        initialValue = hasNotificationListenerAccess(appContext),
        key1 = appContext,
        key2 = enabled,
    ) {
        if (!enabled) {
            value = false
            return@produceState
        }
        while (true) {
            value = hasNotificationListenerAccess(appContext)
            delay(WIDGET_NOTIFICATIONS_POLL_MS)
        }
    }
    return accessEnabled
}

@Composable
private fun rememberNotificationEntries(enabled: Boolean): List<WidgetNotificationEntry> {
    val entries by produceState(
        initialValue = emptyList<WidgetNotificationEntry>(),
        key1 = enabled,
    ) {
        if (!enabled) {
            value = emptyList()
            return@produceState
        }
        CanvasNotificationFeed.entries.collect { current ->
            value = current
        }
    }
    return entries
}

@Composable
private fun rememberTickerClock(enabled: Boolean): Long {
    val now by produceState(
        initialValue = System.currentTimeMillis(),
        key1 = enabled,
    ) {
        if (!enabled) {
            value = System.currentTimeMillis()
            return@produceState
        }
        while (true) {
            val currentMs = System.currentTimeMillis()
            value = currentMs
            delay(notificationTickerRefreshDelayMs(currentMs))
        }
    }
    return now
}

private fun notificationTickerRefreshDelayMs(nowMs: Long): Long {
    val cycleDurationMs = WIDGET_NOTIFICATION_STATIONARY_MS + WIDGET_NOTIFICATION_SCROLL_MS
    val phaseMs = nowMs % cycleDurationMs
    return if (phaseMs < WIDGET_NOTIFICATION_STATIONARY_MS) {
        (WIDGET_NOTIFICATION_STATIONARY_MS - phaseMs).coerceAtLeast(1L)
    } else {
        min(WIDGET_NOTIFICATION_TICKER_FRAME_MS, (cycleDurationMs - phaseMs).coerceAtLeast(1L))
    }
}

private data class CalendarWidgetSnapshot(
    val primaryLine: String,
    val secondaryLine: String,
    val footerLine: String,
    val isResolved: Boolean,
)

private data class CalendarEventEntry(
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val allDay: Boolean,
)

@Composable
private fun rememberCalendarWidgetSnapshot(
    context: Context,
    enabled: Boolean,
): CalendarWidgetSnapshot {
    val appContext = remember(context) { context.applicationContext }
    val loadingText = stringResource(id = R.string.widget_calendar_loading)
    val calendarUnavailableText = stringResource(id = R.string.widget_calendar_unavailable)
    val grantAccessText = stringResource(id = R.string.widget_calendar_permission_hint)
    val noMoreText = stringResource(id = R.string.widget_calendar_no_more_today)
    val snapshot by produceState(
        initialValue = CalendarWidgetSnapshot(
            primaryLine = loadingText,
            secondaryLine = grantAccessText,
            footerLine = noMoreText,
            isResolved = false,
        ),
        key1 = appContext,
        key2 = enabled,
    ) {
        if (!enabled) {
            value = CalendarWidgetSnapshot(
                primaryLine = loadingText,
                secondaryLine = grantAccessText,
                footerLine = noMoreText,
                isResolved = false,
            )
            return@produceState
        }
        while (true) {
            value = runCatching {
                loadCalendarSnapshot(appContext)
            }.getOrElse {
                CalendarWidgetSnapshot(
                    primaryLine = calendarUnavailableText,
                    secondaryLine = grantAccessText,
                    footerLine = noMoreText,
                    isResolved = false,
                )
            }
            delay(WIDGET_CALENDAR_REFRESH_MS)
        }
    }
    return snapshot
}

private suspend fun loadCalendarSnapshot(context: Context): CalendarWidgetSnapshot {
    if (!hasCalendarPermission(context)) {
        return CalendarWidgetSnapshot(
            primaryLine = context.getString(R.string.widget_calendar_permission_required),
            secondaryLine = context.getString(R.string.widget_calendar_permission_hint),
            footerLine = context.getString(R.string.widget_calendar_no_more_today),
            isResolved = false,
        )
    }
    val events = withContext(Dispatchers.IO) {
        queryTodayCalendarEvents(context)
    }
    if (events.isEmpty()) {
        return CalendarWidgetSnapshot(
            primaryLine = context.getString(R.string.widget_calendar_empty),
            secondaryLine = context.getString(R.string.widget_calendar_empty_hint),
            footerLine = context.getString(R.string.widget_calendar_no_more_today),
            isResolved = true,
        )
    }
    val untitledEvent = context.getString(R.string.widget_calendar_no_title)
    val firstEvent = events.first()
    val firstTitle = firstEvent.title.ifBlank { untitledEvent }
    val firstTime = formatCalendarEventTime(context, firstEvent)
    val footerLine = buildString {
        val secondEvent = events.getOrNull(1)
        if (secondEvent != null) {
            append(
                context.getString(
                    R.string.widget_calendar_event_compact,
                    formatCalendarEventTime(context, secondEvent),
                    secondEvent.title.ifBlank { untitledEvent },
                ),
            )
        } else {
            append(context.getString(R.string.widget_calendar_no_more_today))
        }
        val hiddenEventsCount = (events.size - 2).coerceAtLeast(0)
        if (hiddenEventsCount > 0) {
            append(" | ")
            append(context.getString(R.string.widget_calendar_more_count, hiddenEventsCount))
        }
    }
    return CalendarWidgetSnapshot(
        primaryLine = firstTitle,
        secondaryLine = firstTime,
        footerLine = footerLine,
        isResolved = true,
    )
}

@Suppress("MissingPermission")
private fun queryTodayCalendarEvents(context: Context): List<CalendarEventEntry> {
    val zoneId = java.time.ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(uriBuilder, startOfDay)
    ContentUris.appendId(uriBuilder, endOfDay)
    val projection = arrayOf(
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.ALL_DAY,
    )
    val events = mutableListOf<CalendarEventEntry>()
    context.contentResolver.query(
        uriBuilder.build(),
        projection,
        "${CalendarContract.Instances.VISIBLE} = 1",
        null,
        "${CalendarContract.Instances.BEGIN} ASC",
    )?.use { cursor ->
        while (cursor.moveToNext() && events.size < WIDGET_CALENDAR_MAX_EVENTS) {
            val startTimeMs = cursor.getLong(0)
            val endTimeMs = cursor.getLong(1)
            val title = cursor.getString(2).orEmpty()
            val allDay = cursor.getInt(3) == 1
            events += CalendarEventEntry(
                title = title,
                startTimeMs = startTimeMs,
                endTimeMs = endTimeMs,
                allDay = allDay,
            )
        }
    }
    return events
}

private fun formatCalendarEventTime(
    context: Context,
    event: CalendarEventEntry,
): String {
    if (event.allDay) return context.getString(R.string.widget_calendar_all_day)
    val formatter = AndroidDateFormat.getTimeFormat(context)
    val start = formatter.format(java.util.Date(event.startTimeMs))
    val end = formatter.format(java.util.Date(event.endTimeMs))
    return if (event.endTimeMs > event.startTimeMs) {
        context.getString(R.string.widget_calendar_time_range, start, end)
    } else {
        start
    }
}

private data class WeatherWidgetSnapshot(
    val temperatureText: String,
    val condition: String,
    val locationLabel: String,
    val hourlySummary: String,
    val dailySummary: String,
    val isResolved: Boolean,
)

@Composable
private fun rememberWeatherWidgetSnapshot(
    context: Context,
    enabled: Boolean,
): WeatherWidgetSnapshot {
    val appContext = remember(context) { context.applicationContext }
    val waitingCondition = stringResource(id = R.string.widget_weather_waiting)
    val localAreaLabel = stringResource(id = R.string.widget_weather_local_area)
    val weatherUnavailable = stringResource(id = R.string.widget_weather_unavailable)
    val connectionRequired = stringResource(id = R.string.widget_weather_connection_required)
    val hourlyUnavailable = stringResource(id = R.string.widget_weather_hourly_unavailable)
    val dailyUnavailable = stringResource(id = R.string.widget_weather_daily_unavailable)
    val snapshot by produceState(
        initialValue = WeatherWidgetSnapshot(
            temperatureText = WIDGET_UNAVAILABLE_VALUE,
            condition = waitingCondition,
            locationLabel = localAreaLabel,
            hourlySummary = hourlyUnavailable,
            dailySummary = dailyUnavailable,
            isResolved = false,
        ),
        key1 = appContext,
        key2 = enabled,
    ) {
        if (!enabled) {
            value = WeatherWidgetSnapshot(
                temperatureText = WIDGET_UNAVAILABLE_VALUE,
                condition = waitingCondition,
                locationLabel = localAreaLabel,
                hourlySummary = hourlyUnavailable,
                dailySummary = dailyUnavailable,
                isResolved = false,
            )
            return@produceState
        }
        while (true) {
            value = runCatching {
                loadWeatherSnapshot(appContext)
            }.getOrElse {
                WeatherWidgetSnapshot(
                    temperatureText = WIDGET_UNAVAILABLE_VALUE,
                    condition = weatherUnavailable,
                    locationLabel = connectionRequired,
                    hourlySummary = hourlyUnavailable,
                    dailySummary = dailyUnavailable,
                    isResolved = false,
                )
            }
            delay(WIDGET_WEATHER_REFRESH_MS)
        }
    }
    return snapshot
}

private suspend fun loadWeatherSnapshot(context: Context): WeatherWidgetSnapshot {
    if (!hasLocationPermission(context)) {
        return WeatherWidgetSnapshot(
            temperatureText = WIDGET_UNAVAILABLE_VALUE,
            condition = context.getString(R.string.widget_weather_location_required),
            locationLabel = context.getString(R.string.widget_weather_grant_location_access),
            hourlySummary = context.getString(R.string.widget_weather_hourly_unavailable),
            dailySummary = context.getString(R.string.widget_weather_daily_unavailable),
            isResolved = false,
        )
    }
    val location = withContext(Dispatchers.IO) {
        resolveBestLastKnownLocation(context)
    } ?: return WeatherWidgetSnapshot(
        temperatureText = WIDGET_UNAVAILABLE_VALUE,
        condition = context.getString(R.string.widget_weather_locating_device),
        locationLabel = context.getString(R.string.widget_weather_location_not_ready),
        hourlySummary = context.getString(R.string.widget_weather_hourly_unavailable),
        dailySummary = context.getString(R.string.widget_weather_daily_unavailable),
        isResolved = false,
    )
    val locationLabel = resolveLocationLabel(
        context = context,
        latitude = location.latitude,
        longitude = location.longitude,
    )
    val weather = fetchWeatherAtLocation(
        context = context,
        latitude = location.latitude,
        longitude = location.longitude,
    ) ?: return WeatherWidgetSnapshot(
        temperatureText = WIDGET_UNAVAILABLE_VALUE,
        condition = context.getString(R.string.widget_weather_unavailable),
        locationLabel = locationLabel,
        hourlySummary = context.getString(R.string.widget_weather_hourly_unavailable),
        dailySummary = context.getString(R.string.widget_weather_daily_unavailable),
        isResolved = false,
    )
    return WeatherWidgetSnapshot(
        temperatureText = context.getString(R.string.widget_weather_temperature_c, weather.temperatureC),
        condition = weather.condition,
        locationLabel = locationLabel,
        hourlySummary = formatHourlyForecastSummary(context, weather.hourlyForecast),
        dailySummary = formatDailyForecastSummary(context, weather.dailyForecast),
        isResolved = true,
    )
}

private data class LiveWeatherData(
    val temperatureC: Int,
    val condition: String,
    val hourlyForecast: List<LiveHourlyForecastEntry>,
    val dailyForecast: List<LiveDailyForecastEntry>,
)

private data class LiveHourlyForecastEntry(
    val hourLabel: String,
    val temperatureC: Int,
)

private data class LiveDailyForecastEntry(
    val dayLabel: String,
    val maxTempC: Int,
    val minTempC: Int,
)

private suspend fun fetchWeatherAtLocation(
    context: Context,
    latitude: Double,
    longitude: Double,
): LiveWeatherData? {
    return withContext(Dispatchers.IO) {
        val lat = String.format(Locale.US, "%.4f", latitude)
        val lon = String.format(Locale.US, "%.4f", longitude)
        val endpoint = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code" +
                "&hourly=temperature_2m" +
                "&daily=temperature_2m_max,temperature_2m_min" +
                "&forecast_days=4&timezone=auto",
        )
        val connection = (endpoint.openConnection() as? HttpURLConnection) ?: return@withContext null
        connection.connectTimeout = WIDGET_WEATHER_NETWORK_TIMEOUT_MS
        connection.readTimeout = WIDGET_WEATHER_NETWORK_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.doInput = true
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) return@withContext null
            val payload = connection.inputStream.bufferedReader().use { reader -> reader.readText() }
            val json = JSONObject(payload)
            val current = json.optJSONObject("current") ?: return@withContext null
            val temperature = current.optDouble("temperature_2m", Double.NaN)
            if (temperature.isNaN()) return@withContext null
            val code = current.optInt("weather_code", Int.MIN_VALUE)
            val now = current.optString("time", "")
            val hourly = parseHourlyForecast(
                times = json.optJSONObject("hourly")?.optJSONArray("time"),
                temperatures = json.optJSONObject("hourly")?.optJSONArray("temperature_2m"),
                fromIsoTime = now,
            )
            val daily = parseDailyForecast(
                times = json.optJSONObject("daily")?.optJSONArray("time"),
                maxTemperatures = json.optJSONObject("daily")?.optJSONArray("temperature_2m_max"),
                minTemperatures = json.optJSONObject("daily")?.optJSONArray("temperature_2m_min"),
            )
            LiveWeatherData(
                temperatureC = temperature.roundToInt(),
                condition = weatherCodeToLabel(context, code),
                hourlyForecast = hourly,
                dailyForecast = daily,
            )
        } finally {
            connection.disconnect()
        }
    }
}

private fun parseHourlyForecast(
    times: JSONArray?,
    temperatures: JSONArray?,
    fromIsoTime: String,
): List<LiveHourlyForecastEntry> {
    if (times == null || temperatures == null || times.length() != temperatures.length()) return emptyList()
    val currentTime = runCatching { LocalDateTime.parse(fromIsoTime) }.getOrNull()
    val entries = mutableListOf<LiveHourlyForecastEntry>()
    for (index in 0 until times.length()) {
        val timeIso = times.optString(index, "")
        val parsed = runCatching { LocalDateTime.parse(timeIso) }.getOrNull() ?: continue
        if (currentTime != null && parsed.isBefore(currentTime)) continue
        val temp = temperatures.optDouble(index, Double.NaN)
        if (temp.isNaN()) continue
        entries += LiveHourlyForecastEntry(
            hourLabel = parsed.toLocalTime().format(WIDGET_HOURLY_TIME_FORMATTER),
            temperatureC = temp.roundToInt(),
        )
        if (entries.size >= WIDGET_HOURLY_FORECAST_ITEMS) break
    }
    return entries
}

private fun parseDailyForecast(
    times: JSONArray?,
    maxTemperatures: JSONArray?,
    minTemperatures: JSONArray?,
): List<LiveDailyForecastEntry> {
    if (
        times == null ||
        maxTemperatures == null ||
        minTemperatures == null ||
        times.length() != maxTemperatures.length() ||
        times.length() != minTemperatures.length()
    ) {
        return emptyList()
    }
    val entries = mutableListOf<LiveDailyForecastEntry>()
    for (index in 0 until times.length()) {
        val dateIso = times.optString(index, "")
        val date = runCatching { LocalDate.parse(dateIso) }.getOrNull() ?: continue
        val max = maxTemperatures.optDouble(index, Double.NaN)
        val min = minTemperatures.optDouble(index, Double.NaN)
        if (max.isNaN() || min.isNaN()) continue
        entries += LiveDailyForecastEntry(
            dayLabel = date.format(WIDGET_DAILY_DAY_FORMATTER),
            maxTempC = max.roundToInt(),
            minTempC = min.roundToInt(),
        )
        if (entries.size >= WIDGET_DAILY_FORECAST_ITEMS) break
    }
    return entries
}

private fun formatHourlyForecastSummary(
    context: Context,
    hourly: List<LiveHourlyForecastEntry>,
): String {
    if (hourly.isEmpty()) return context.getString(R.string.widget_weather_hourly_unavailable)
    return hourly.joinToString(separator = " • ") { item ->
        context.getString(
            R.string.widget_weather_hourly_item,
            item.hourLabel,
            item.temperatureC,
        )
    }
}

private fun formatDailyForecastSummary(
    context: Context,
    daily: List<LiveDailyForecastEntry>,
): String {
    if (daily.isEmpty()) return context.getString(R.string.widget_weather_daily_unavailable)
    return daily.joinToString(separator = " • ") { item ->
        context.getString(
            R.string.widget_weather_daily_item,
            item.dayLabel,
            item.maxTempC,
            item.minTempC,
        )
    }
}

private suspend fun resolveLocationLabel(
    context: Context,
    latitude: Double,
    longitude: Double,
): String {
    val locationFromGeocoder = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)
        }.getOrNull()
            ?.firstOrNull()
            ?.let { address ->
                address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: address.countryName
            }
    }
    return locationFromGeocoder ?: context.getString(
        R.string.widget_weather_location_coordinates,
        "%.2f".format(Locale.US, latitude),
        "%.2f".format(Locale.US, longitude),
    )
}

private fun weatherCodeToLabel(context: Context, code: Int): String {
    val stringRes = when (code) {
        0 -> R.string.widget_weather_code_clear_sky
        1, 2 -> R.string.widget_weather_code_partly_cloudy
        3 -> R.string.widget_weather_code_overcast
        45, 48 -> R.string.widget_weather_code_fog
        51, 53, 55 -> R.string.widget_weather_code_light_drizzle
        56, 57 -> R.string.widget_weather_code_freezing_drizzle
        61, 63, 65 -> R.string.widget_weather_code_rain
        66, 67 -> R.string.widget_weather_code_freezing_rain
        71, 73, 75, 77 -> R.string.widget_weather_code_snow
        80, 81, 82 -> R.string.widget_weather_code_rain_showers
        85, 86 -> R.string.widget_weather_code_snow_showers
        95 -> R.string.widget_weather_code_thunderstorm
        96, 99 -> R.string.widget_weather_code_thunderstorm_hail
        else -> R.string.widget_weather_code_current
    }
    return context.getString(stringRes)
}

private fun hasNotificationListenerAccess(context: Context): Boolean {
    val listeners = Settings.Secure.getString(
        context.contentResolver,
        NOTIFICATION_LISTENERS_SETTING_KEY,
    ) ?: return false
    val componentName = ComponentName(context, CanvasNotificationListenerService::class.java)
    val full = componentName.flattenToString()
    val short = componentName.flattenToShortString()
    return listeners.split(':').any { listener ->
        listener.equals(full, ignoreCase = true) || listener.equals(short, ignoreCase = true)
    }
}

private fun applyPaintTypography(
    paint: AndroidPaint,
    style: TextStyle,
) {
    val baseTypeface = style.fontFamily.toAndroidTypeface()
    val isBold = (style.fontWeight ?: FontWeight.Normal).weight >= FontWeight.SemiBold.weight
    paint.typeface = Typeface.create(baseTypeface, if (isBold) Typeface.BOLD else Typeface.NORMAL)
}

private fun FontFamily?.toAndroidTypeface(): Typeface {
    return when (this) {
        FontFamily.Monospace -> Typeface.MONOSPACE
        FontFamily.Serif -> Typeface.SERIF
        FontFamily.SansSerif -> Typeface.SANS_SERIF
        FontFamily.Cursive -> Typeface.create("cursive", Typeface.NORMAL)
        else -> Typeface.DEFAULT
    }
}

private fun hasCalendarPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CALENDAR,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun hasLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

@Suppress("MissingPermission")
private fun resolveBestLastKnownLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(
        LocationManager.FUSED_PROVIDER,
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )
    return providers.asSequence()
        .filter { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) }
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .minByOrNull { location -> location.accuracy.takeIf { it > 0f } ?: Float.MAX_VALUE }
}

@Composable
private fun FrameResizeHandles(
    frameId: String,
    frameLeftPx: Float,
    frameTopPx: Float,
    frameWidthPx: Float,
    frameHeightPx: Float,
    onFrameResizeStart: (id: String, handle: CanvasFrameResizeHandle) -> Unit,
    onFrameResizeDrag: (id: String, handle: CanvasFrameResizeHandle, delta: ScreenPoint) -> Unit,
    onFrameResizeEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val touchSizePx = with(density) { FRAME_RESIZE_HANDLE_TOUCH_TARGET_DP.toPx() }
    val handleSizePx = with(density) { FRAME_RESIZE_HANDLE_VISUAL_SIZE_DP.toPx() }
    val placements = frameHandlePlacements(frameWidthPx, frameHeightPx)

    placements.forEach { placement ->
        val centerX = frameLeftPx + placement.centerX
        val centerY = frameTopPx + placement.centerY
        val handleBoxLeft = centerX - touchSizePx / 2f
        val handleBoxTop = centerY - touchSizePx / 2f
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = handleBoxLeft.roundToInt(),
                        y = handleBoxTop.roundToInt(),
                    )
                }
                .size(with(density) { touchSizePx.toDp() })
                .zIndex(1.15f)
                .singlePointerResizeDrag(
                    key1 = frameId,
                    key2 = placement.handle,
                    onDragStart = { onFrameResizeStart(frameId, placement.handle) },
                    onDrag = { delta ->
                        onFrameResizeDrag(
                            frameId,
                            placement.handle,
                            delta,
                        )
                    },
                    onDragEnd = onFrameResizeEnd,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.size(with(density) { handleSizePx.toDp() }),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(999.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun SelectionResizeHandles(
    boundsWidthPx: Float,
    boundsHeightPx: Float,
    onSelectionResizeStart: (handle: CanvasFrameResizeHandle) -> Unit,
    onSelectionResizeDrag: (handle: CanvasFrameResizeHandle, delta: ScreenPoint) -> Unit,
    onSelectionResizeEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val touchSizePx = with(density) { FRAME_RESIZE_HANDLE_TOUCH_TARGET_DP.toPx() }
    val handleSizePx = with(density) { FRAME_RESIZE_HANDLE_VISUAL_SIZE_DP.toPx() }
    val placements = frameHandlePlacements(boundsWidthPx, boundsHeightPx)

    placements.forEach { placement ->
        val handleBoxLeft = placement.centerX - touchSizePx / 2f
        val handleBoxTop = placement.centerY - touchSizePx / 2f
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = handleBoxLeft.roundToInt(),
                        y = handleBoxTop.roundToInt(),
                    )
                }
                .size(with(density) { touchSizePx.toDp() })
                .zIndex(1.18f)
                .singlePointerResizeDrag(
                    key1 = placement.handle,
                    onDragStart = { onSelectionResizeStart(placement.handle) },
                    onDrag = { delta ->
                        onSelectionResizeDrag(
                            placement.handle,
                            delta,
                        )
                    },
                    onDragEnd = onSelectionResizeEnd,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.size(with(density) { handleSizePx.toDp() }),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(999.dp),
                        ),
                )
            }
        }
    }
}

@Composable
private fun SelectionDeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = stringResource(id = R.string.canvas_selection_delete_content_description),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun Modifier.canvasInputModifier(
    isEditActive: Boolean,
    selectedTool: CanvasEditToolId,
    cameraState: CameraState,
    onCanvasTap: (WorldPoint) -> Unit,
    onFrameDragStart: (WorldPoint) -> Unit,
    onFrameDragUpdate: (WorldPoint) -> Unit,
    onFrameDragEnd: () -> Unit,
    hasActiveSelection: Boolean,
    selectionBounds: CanvasSelectionBoundsUiState?,
    onSelectionDragStart: (WorldPoint) -> Unit,
    onSelectionDragUpdate: (WorldPoint) -> Unit,
    onSelectionDragEnd: () -> Unit,
    onSelectionClearTap: () -> Unit,
    onSelectionLongPressAt: (WorldPoint) -> Boolean,
    onSelectionMoveDelta: (ScreenPoint) -> Unit,
    onSelectionMoveEnd: () -> Unit,
    autoPanZonePx: Float,
    autoPanMaxStepPx: Float,
    onAutoPanDelta: (ScreenPoint) -> Unit,
    onBrushStart: (WorldPoint) -> Unit,
    onBrushPoint: (WorldPoint) -> Unit,
    onBrushEnd: () -> Unit,
    onEraseAt: (WorldPoint) -> Unit,
): Modifier {
    if (!isEditActive) return this
    val latestCameraState = rememberUpdatedState(cameraState)
    val latestHasActiveSelection = rememberUpdatedState(hasActiveSelection)
    val latestSelectionBounds = rememberUpdatedState(selectionBounds)
    return when (selectedTool) {
        CanvasEditToolId.Brush -> {
            pointerInput(selectedTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onBrushStart(
                            WorldScreenTransformer.screenToWorld(
                                point = ScreenPoint(offset.x, offset.y),
                                camera = latestCameraState.value,
                            ),
                        )
                    },
                    onDragEnd = onBrushEnd,
                    onDragCancel = onBrushEnd,
                    onDrag = { change, _ ->
                        change.consume()
                        onBrushPoint(
                            WorldScreenTransformer.screenToWorld(
                                point = ScreenPoint(change.position.x, change.position.y),
                                camera = latestCameraState.value,
                            ),
                        )
                    },
                )
            }
        }

        CanvasEditToolId.Selection -> {
            pointerInput(selectedTool) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val pointerId = down.id
                    val startWorld = WorldScreenTransformer.screenToWorld(
                        point = ScreenPoint(down.position.x, down.position.y),
                        camera = latestCameraState.value,
                    )
                    val currentBounds = latestSelectionBounds.value
                    val deferToResizeHandle = currentBounds?.let { bounds ->
                        bounds.canResize && isNearSelectionResizeHandle(
                            screenPoint = down.position,
                            selectionBounds = bounds,
                            cameraState = latestCameraState.value,
                            handleRadiusPx = SELECTION_HANDLE_DEFER_RADIUS_PX,
                        )
                    } ?: false
                    if (deferToResizeHandle) {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.none { it.pressed }) {
                                break
                            }
                        }
                        return@awaitEachGesture
                    }
                    val hasSelection = latestHasActiveSelection.value
                    val moveExistingSelection = currentBounds?.contains(startWorld) == true
                    if (hasSelection && !moveExistingSelection) {
                        onSelectionClearTap()
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.none { it.pressed }) {
                                break
                            }
                        }
                        return@awaitEachGesture
                    }

                    if (moveExistingSelection) {
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                if (moved) {
                                    onSelectionMoveEnd()
                                }
                                break
                            }
                            val autoPanDelta = edgeAutoPanDelta(
                                pointerScreen = change.position,
                                canvasWidthPx = size.width.toFloat(),
                                canvasHeightPx = size.height.toFloat(),
                                autoPanZonePx = autoPanZonePx,
                                autoPanMaxStepPx = autoPanMaxStepPx,
                            )
                            autoPanDelta?.let(onAutoPanDelta)
                            val delta = (change.position - change.previousPosition) + autoPanDelta.toOffset()
                            if (delta != Offset.Zero) {
                                moved = true
                                change.consume()
                                onSelectionMoveDelta(ScreenPoint(delta.x, delta.y))
                            }
                        }
                    } else {
                        onSelectionDragStart(startWorld)
                        var dragStarted = false
                        var dragEnded = false
                        var longPressMoveMode = false
                        var movedLongPressSelection = false
                        var longPressSelectionAttempted = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                if (longPressMoveMode) {
                                    if (movedLongPressSelection) {
                                        onSelectionMoveEnd()
                                    }
                                } else {
                                    onSelectionDragEnd()
                                }
                                dragEnded = true
                                break
                            }
                            val movedDistancePx = (change.position - down.position).getDistance()
                            if (!dragStarted && !longPressMoveMode &&
                                !longPressSelectionAttempted &&
                                movedDistancePx <= viewConfiguration.touchSlop &&
                                change.uptimeMillis - down.uptimeMillis >= viewConfiguration.longPressTimeoutMillis
                            ) {
                                longPressSelectionAttempted = true
                                onSelectionDragEnd()
                                val longPressWorld = WorldScreenTransformer.screenToWorld(
                                    point = ScreenPoint(change.position.x, change.position.y),
                                    camera = latestCameraState.value,
                                )
                                longPressMoveMode = onSelectionLongPressAt(longPressWorld)
                                if (longPressMoveMode) {
                                    change.consume()
                                    continue
                                }
                                onSelectionDragStart(startWorld)
                            }
                            if (!dragStarted && !longPressMoveMode && movedDistancePx > viewConfiguration.touchSlop) {
                                dragStarted = true
                            }
                            if (longPressMoveMode) {
                                val autoPanDelta = edgeAutoPanDelta(
                                    pointerScreen = change.position,
                                    canvasWidthPx = size.width.toFloat(),
                                    canvasHeightPx = size.height.toFloat(),
                                    autoPanZonePx = autoPanZonePx,
                                    autoPanMaxStepPx = autoPanMaxStepPx,
                                )
                                autoPanDelta?.let(onAutoPanDelta)
                                val delta = (change.position - change.previousPosition) + autoPanDelta.toOffset()
                                if (delta != Offset.Zero) {
                                    movedLongPressSelection = true
                                    change.consume()
                                    onSelectionMoveDelta(ScreenPoint(delta.x, delta.y))
                                }
                            } else if (dragStarted) {
                                val autoPanDelta = edgeAutoPanDelta(
                                    pointerScreen = change.position,
                                    canvasWidthPx = size.width.toFloat(),
                                    canvasHeightPx = size.height.toFloat(),
                                    autoPanZonePx = autoPanZonePx,
                                    autoPanMaxStepPx = autoPanMaxStepPx,
                                )
                                autoPanDelta?.let(onAutoPanDelta)
                                val effectivePointer = change.position + autoPanDelta.toOffset()
                                change.consume()
                                onSelectionDragUpdate(
                                    WorldScreenTransformer.screenToWorld(
                                        point = ScreenPoint(effectivePointer.x, effectivePointer.y),
                                        camera = latestCameraState.value,
                                    ),
                                )
                            }
                        }
                        if (!dragEnded) {
                            if (longPressMoveMode) {
                                if (movedLongPressSelection) {
                                    onSelectionMoveEnd()
                                }
                            } else {
                                onSelectionDragEnd()
                            }
                        }
                    }
                }
            }
        }

        CanvasEditToolId.Frame -> {
            pointerInput(selectedTool) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startWorld = WorldScreenTransformer.screenToWorld(
                        point = ScreenPoint(down.position.x, down.position.y),
                        camera = latestCameraState.value,
                    )
                    val pointerId = down.id
                    var dragStarted = false
                    var dragEnded = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                        if (!change.pressed) {
                            if (dragStarted) {
                                onFrameDragEnd()
                                dragEnded = true
                            } else {
                                onCanvasTap(startWorld)
                            }
                            break
                        }
                        val movedDistancePx = (change.position - down.position).getDistance()
                        if (!dragStarted && movedDistancePx > viewConfiguration.touchSlop) {
                            dragStarted = true
                            onFrameDragStart(startWorld)
                        }
                        if (dragStarted) {
                            val autoPanDelta = edgeAutoPanDelta(
                                pointerScreen = change.position,
                                canvasWidthPx = size.width.toFloat(),
                                canvasHeightPx = size.height.toFloat(),
                                autoPanZonePx = autoPanZonePx,
                                autoPanMaxStepPx = autoPanMaxStepPx,
                            )
                            autoPanDelta?.let(onAutoPanDelta)
                            val effectivePointer = change.position + autoPanDelta.toOffset()
                            change.consume()
                            val currentWorld = WorldScreenTransformer.screenToWorld(
                                point = ScreenPoint(effectivePointer.x, effectivePointer.y),
                                camera = latestCameraState.value,
                            )
                            onFrameDragUpdate(currentWorld)
                        }
                    }
                    if (dragStarted && !dragEnded) {
                        onFrameDragEnd()
                    }
                }
            }
        }

        CanvasEditToolId.StickyNote,
        CanvasEditToolId.Text,
        -> {
            pointerInput(selectedTool) {
                detectTapGestures(
                    onTap = { offset ->
                        onCanvasTap(
                            WorldScreenTransformer.screenToWorld(
                                point = ScreenPoint(offset.x, offset.y),
                                camera = latestCameraState.value,
                            ),
                        )
                    },
                )
            }
        }

        CanvasEditToolId.Delete -> {
            pointerInput(selectedTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onEraseAt(
                            WorldScreenTransformer.screenToWorld(
                                point = ScreenPoint(offset.x, offset.y),
                                camera = latestCameraState.value,
                            ),
                        )
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onEraseAt(
                            WorldScreenTransformer.screenToWorld(
                                point = ScreenPoint(change.position.x, change.position.y),
                                camera = latestCameraState.value,
                            ),
                        )
                    },
                )
            }
        }

        CanvasEditToolId.Move -> this
    }
}

@Composable
private fun Modifier.objectMoveModifier(
    enabled: Boolean,
    target: CanvasObjectDragTarget,
    nodeTopLeftScreen: ScreenPoint,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    autoPanZonePx: Float,
    autoPanMaxStepPx: Float,
    onObjectDragStart: (CanvasObjectDragTarget) -> Unit,
    onObjectDragDelta: (CanvasObjectDragTarget, ScreenPoint) -> Unit,
    onObjectDragEnd: () -> Unit,
    onObjectDragCancel: () -> Unit,
    onAutoPanDelta: (ScreenPoint) -> Unit,
): Modifier {
    if (!enabled) return this
    val latestNodeTopLeftScreen = rememberUpdatedState(nodeTopLeftScreen)
    val latestCanvasWidthPx = rememberUpdatedState(canvasWidthPx)
    val latestCanvasHeightPx = rememberUpdatedState(canvasHeightPx)
    val latestAutoPanZonePx = rememberUpdatedState(autoPanZonePx)
    val latestAutoPanMaxStepPx = rememberUpdatedState(autoPanMaxStepPx)
    val latestOnObjectDragStart = rememberUpdatedState(onObjectDragStart)
    val latestOnObjectDragDelta = rememberUpdatedState(onObjectDragDelta)
    val latestOnObjectDragEnd = rememberUpdatedState(onObjectDragEnd)
    val latestOnObjectDragCancel = rememberUpdatedState(onObjectDragCancel)
    val latestOnAutoPanDelta = rememberUpdatedState(onAutoPanDelta)
    return pointerInput(target) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val pointerId = down.id
            var dragStarted = false

            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { change -> change.pressed }
                val pointerChange = event.changes.firstOrNull { change -> change.id == pointerId }

                if (pointerChange == null || !pointerChange.pressed) {
                    if (dragStarted) {
                        latestOnObjectDragEnd.value()
                    }
                    break
                }

                // Multi-touch should be owned by canvas pinch/zoom, never by object drag.
                if (pressed.size > 1) {
                    if (dragStarted) {
                        latestOnObjectDragCancel.value()
                    }
                    while (true) {
                        val releaseEvent = awaitPointerEvent()
                        if (releaseEvent.changes.none { change -> change.pressed }) {
                            break
                        }
                    }
                    break
                }

                val movedDistancePx = (pointerChange.position - down.position).getDistance()
                if (!dragStarted && movedDistancePx > (viewConfiguration.touchSlop * OBJECT_DRAG_START_SLOP_MULTIPLIER)) {
                    dragStarted = true
                    latestOnObjectDragStart.value(target)
                }
                if (dragStarted) {
                    val delta = pointerChange.position - pointerChange.previousPosition
                    if (delta != Offset.Zero) {
                        val nodeTopLeft = latestNodeTopLeftScreen.value
                        val pointerScreen = Offset(
                            x = nodeTopLeft.x + pointerChange.position.x,
                            y = nodeTopLeft.y + pointerChange.position.y,
                        )
                        val autoPanDelta = edgeAutoPanDelta(
                            pointerScreen = pointerScreen,
                            canvasWidthPx = latestCanvasWidthPx.value,
                            canvasHeightPx = latestCanvasHeightPx.value,
                            autoPanZonePx = latestAutoPanZonePx.value,
                            autoPanMaxStepPx = latestAutoPanMaxStepPx.value,
                        )
                        autoPanDelta?.let(latestOnAutoPanDelta.value)
                        pointerChange.consume()
                        latestOnObjectDragDelta.value(
                            target,
                            ScreenPoint(
                                x = delta.x + autoPanDelta.xOrZero(),
                                y = delta.y + autoPanDelta.yOrZero(),
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.canvasMultiTouchTransformModifier(
    enabled: Boolean,
    onTransform: (panDeltaPx: ScreenPoint, zoomFactor: Float, focusPx: ScreenPoint) -> Unit,
): Modifier {
    if (!enabled) return this
    val latestOnTransform = rememberUpdatedState(onTransform)
    return pointerInput(Unit) {
        awaitEachGesture {
            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val pressed = event.changes.filter { change -> change.pressed }
                if (pressed.isEmpty()) {
                    break
                }
                if (pressed.size < 2) {
                    continue
                }

                val zoom = event.calculateZoom()
                val pan = event.calculatePan()
                val centroid = event.calculateCentroid(useCurrent = true)
                val isPanMeaningful = (pan.x * pan.x + pan.y * pan.y) >= MIN_TRANSFORM_PAN_DELTA_SQ_PX
                val isZoomMeaningful = abs(zoom - 1f) >= MIN_TRANSFORM_ZOOM_DELTA
                if (
                    centroid.x.isNaN() ||
                    centroid.y.isNaN() ||
                    (!isPanMeaningful && !isZoomMeaningful)
                ) {
                    continue
                }

                latestOnTransform.value(
                    ScreenPoint(pan.x, pan.y),
                    zoom,
                    ScreenPoint(centroid.x, centroid.y),
                )
                event.changes.forEach { change ->
                    if (change.positionChanged()) {
                        change.consume()
                    }
                }
            }
        }
    }
}

private fun Modifier.singlePointerResizeDrag(
    key1: Any,
    key2: Any? = null,
    onDragStart: () -> Unit,
    onDrag: (ScreenPoint) -> Unit,
    onDragEnd: () -> Unit,
): Modifier {
    return pointerInput(key1, key2) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val pointerId = down.id
            var dragStarted = false

            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { change -> change.pressed }
                val pointerChange = event.changes.firstOrNull { change -> change.id == pointerId } ?: break

                if (!pointerChange.pressed) {
                    if (dragStarted) {
                        onDragEnd()
                    }
                    break
                }

                // Pinch/2-finger gesture should never resize frame handles.
                if (pressed.size > 1) {
                    if (dragStarted) {
                        onDragEnd()
                    }
                    while (true) {
                        val releaseEvent = awaitPointerEvent()
                        if (releaseEvent.changes.none { change -> change.pressed }) {
                            break
                        }
                    }
                    break
                }

                val movedDistancePx = (pointerChange.position - down.position).getDistance()
                if (!dragStarted && movedDistancePx > viewConfiguration.touchSlop) {
                    dragStarted = true
                    onDragStart()
                }
                if (dragStarted) {
                    val delta = pointerChange.position - pointerChange.previousPosition
                    if (delta != Offset.Zero) {
                        pointerChange.consume()
                        onDrag(ScreenPoint(delta.x, delta.y))
                    }
                }
            }
        }
    }
}

private fun frameHandlePlacements(
    frameWidthPx: Float,
    frameHeightPx: Float,
): List<FrameResizeHandlePlacement> {
    val centerX = frameWidthPx / 2f
    val centerY = frameHeightPx / 2f
    return listOf(
        FrameResizeHandlePlacement(CanvasFrameResizeHandle.TopLeft, 0f, 0f),
        FrameResizeHandlePlacement(CanvasFrameResizeHandle.Top, centerX, 0f),
        FrameResizeHandlePlacement(CanvasFrameResizeHandle.TopRight, frameWidthPx, 0f),
        FrameResizeHandlePlacement(CanvasFrameResizeHandle.Right, frameWidthPx, centerY),
        FrameResizeHandlePlacement(CanvasFrameResizeHandle.BottomRight, frameWidthPx, frameHeightPx),
        FrameResizeHandlePlacement(CanvasFrameResizeHandle.Bottom, centerX, frameHeightPx),
        FrameResizeHandlePlacement(CanvasFrameResizeHandle.BottomLeft, 0f, frameHeightPx),
        FrameResizeHandlePlacement(CanvasFrameResizeHandle.Left, 0f, centerY),
    )
}

private fun isNearSelectionResizeHandle(
    screenPoint: Offset,
    selectionBounds: CanvasSelectionBoundsUiState,
    cameraState: CameraState,
    handleRadiusPx: Float,
): Boolean {
    val topLeft = WorldScreenTransformer.worldToScreen(
        point = WorldPoint(selectionBounds.left, selectionBounds.top),
        camera = cameraState,
    )
    val bottomRight = WorldScreenTransformer.worldToScreen(
        point = WorldPoint(selectionBounds.right, selectionBounds.bottom),
        camera = cameraState,
    )
    val width = (bottomRight.x - topLeft.x).coerceAtLeast(1f)
    val height = (bottomRight.y - topLeft.y).coerceAtLeast(1f)
    val localPoint = Offset(
        x = screenPoint.x - topLeft.x,
        y = screenPoint.y - topLeft.y,
    )
    return frameHandlePlacements(width, height).any { placement ->
        (localPoint - Offset(placement.centerX, placement.centerY)).getDistance() <= handleRadiusPx
    }
}

private fun edgeAutoPanDelta(
    pointerScreen: Offset,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    autoPanZonePx: Float,
    autoPanMaxStepPx: Float,
): ScreenPoint? {
    if (canvasWidthPx <= 1f || canvasHeightPx <= 1f || autoPanZonePx <= 0f || autoPanMaxStepPx <= 0f) {
        return null
    }
    fun axisDelta(
        value: Float,
        maxValue: Float,
    ): Float {
        return when {
            value < autoPanZonePx -> {
                val intensity = ((autoPanZonePx - value) / autoPanZonePx).coerceIn(0f, 1f)
                -autoPanMaxStepPx * intensity
            }

            value > maxValue - autoPanZonePx -> {
                val intensity = ((value - (maxValue - autoPanZonePx)) / autoPanZonePx).coerceIn(0f, 1f)
                autoPanMaxStepPx * intensity
            }

            else -> 0f
        }
    }

    val deltaX = axisDelta(pointerScreen.x, canvasWidthPx)
    val deltaY = axisDelta(pointerScreen.y, canvasHeightPx)
    if (deltaX == 0f && deltaY == 0f) return null
    return ScreenPoint(deltaX, deltaY)
}

private fun ScreenPoint?.toOffset(): Offset {
    return Offset(
        x = this?.x ?: 0f,
        y = this?.y ?: 0f,
    )
}

private fun ScreenPoint?.xOrZero(): Float = this?.x ?: 0f
private fun ScreenPoint?.yOrZero(): Float = this?.y ?: 0f

private data class StickyTextLayout(
    val textSizePx: Float,
    val lineHeightPx: Float,
    val maxLines: Int,
)

private fun fitStickyTextLayout(
    text: String,
    desiredTextSizePx: Float,
    contentWidthPx: Float,
    contentHeightPx: Float,
): StickyTextLayout {
    val paragraphs = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .split('\n')
        .ifEmpty { listOf(text) }
    val normalizedParagraphs = paragraphs.map { it.length.coerceAtLeast(1) }
    val totalChars = normalizedParagraphs.sum().coerceAtLeast(1)
    val safeWidthPx = contentWidthPx.coerceAtLeast(4f)
    val safeHeightPx = contentHeightPx.coerceAtLeast(4f)
    val maxCharsPerLineCandidate = max(
        STICKY_TEXT_MIN_CHARS_PER_LINE,
        min(STICKY_TEXT_MAX_CHARS_PER_LINE, totalChars),
    )

    var bestSizePx = STICKY_TEXT_MIN_SIZE_PX
    var bestLineCount = normalizedParagraphs.size.coerceAtLeast(1)
    for (charsPerLine in STICKY_TEXT_MIN_CHARS_PER_LINE..maxCharsPerLineCandidate) {
        val wrappedLineCount = normalizedParagraphs.sumOf { paragraphChars ->
            ((paragraphChars + charsPerLine - 1) / charsPerLine).coerceAtLeast(1)
        }.coerceAtLeast(1)
        val maxByWidthPx = safeWidthPx / (charsPerLine * STICKY_TEXT_ESTIMATED_CHAR_WIDTH_FACTOR)
        val maxByHeightPx = safeHeightPx / (wrappedLineCount * STICKY_TEXT_LINE_HEIGHT_MULTIPLIER)
        val candidatePx = min(desiredTextSizePx, min(maxByWidthPx, maxByHeightPx))
            .coerceAtLeast(STICKY_TEXT_MIN_SIZE_PX)
        if (candidatePx > bestSizePx) {
            bestSizePx = candidatePx
            bestLineCount = wrappedLineCount
        }
    }
    val textSizePx = bestSizePx
    val lineHeightPx = (textSizePx * STICKY_TEXT_LINE_HEIGHT_MULTIPLIER).coerceAtLeast(1f)
    val maxLines = bestLineCount.coerceAtLeast(1)
    return StickyTextLayout(
        textSizePx = textSizePx,
        lineHeightPx = lineHeightPx,
        maxLines = maxLines,
    )
}

private data class EstimatedTextBoundsPx(
    val widthPx: Float,
    val heightPx: Float,
)

private fun estimateTextBoundsPx(
    text: String,
    textSizePx: Float,
): EstimatedTextBoundsPx {
    val lines = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .split('\n')
        .ifEmpty { listOf(text) }
    val longestLine = lines.maxOfOrNull { it.length }?.coerceAtLeast(1) ?: 1
    val lineCount = lines.size.coerceAtLeast(1)
    val widthPx = (longestLine * textSizePx * TEXT_ESTIMATED_CHAR_WIDTH_FACTOR)
        .coerceAtLeast(textSizePx * TEXT_ESTIMATED_MIN_WIDTH_MULTIPLIER)
    val heightPx = (lineCount * textSizePx * TEXT_ESTIMATED_LINE_HEIGHT_MULTIPLIER)
        .coerceAtLeast(textSizePx * TEXT_ESTIMATED_LINE_HEIGHT_MULTIPLIER)
    return EstimatedTextBoundsPx(widthPx = widthPx, heightPx = heightPx)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStickyNoteText(
    text: String,
    paint: AndroidPaint,
    contentLeftPx: Float,
    contentTopPx: Float,
    contentWidthPx: Float,
    contentHeightPx: Float,
    textSizePx: Float,
    lineHeightPx: Float,
    maxLines: Int,
) {
    if (contentWidthPx <= 0f || contentHeightPx <= 0f || textSizePx <= 0f || maxLines <= 0) return
    paint.textAlign = AndroidPaint.Align.CENTER
    paint.textSize = textSizePx
    val lines = wrapTextForWidth(
        text = text,
        paint = paint,
        maxWidthPx = contentWidthPx,
        maxLines = maxLines,
    )
    if (lines.isEmpty()) return

    val actualLineHeight = lineHeightPx.coerceAtLeast(1f)
    val maxVisibleLines = (contentHeightPx / actualLineHeight).toInt().coerceAtLeast(1)
    val visibleLines = lines.take(maxVisibleLines)
    val totalHeight = visibleLines.size * actualLineHeight
    val fontMetrics = paint.fontMetrics
    val firstBaseline =
        contentTopPx + ((contentHeightPx - totalHeight) / 2f).coerceAtLeast(0f) - fontMetrics.ascent
    val centerX = contentLeftPx + contentWidthPx / 2f

    drawIntoCanvas { canvas ->
        visibleLines.forEachIndexed { index, line ->
            canvas.nativeCanvas.drawText(
                line,
                centerX,
                firstBaseline + index * actualLineHeight,
                paint,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFloatingText(
    text: String,
    paint: AndroidPaint,
    leftPx: Float,
    topPx: Float,
    textSizePx: Float,
    color: Color,
) {
    if (textSizePx <= 0f) return
    paint.textAlign = AndroidPaint.Align.LEFT
    paint.textSize = textSizePx
    paint.color = color.toArgb()
    val lines = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .split('\n')
        .ifEmpty { listOf(text) }
    val lineHeightPx = (textSizePx * TEXT_ESTIMATED_LINE_HEIGHT_MULTIPLIER).coerceAtLeast(1f)
    val firstBaseline = topPx - paint.fontMetrics.ascent
    drawIntoCanvas { canvas ->
        lines.forEachIndexed { index, line ->
            canvas.nativeCanvas.drawText(
                line,
                leftPx,
                firstBaseline + index * lineHeightPx,
                paint,
            )
        }
    }
}

private fun wrapTextForWidth(
    text: String,
    paint: AndroidPaint,
    maxWidthPx: Float,
    maxLines: Int,
): List<String> {
    if (maxWidthPx <= 0f || maxLines <= 0) return emptyList()
    val source = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
    val paragraphs = source.split('\n')
    val lines = mutableListOf<String>()
    paragraphs.forEachIndexed { paragraphIndex, paragraph ->
        var remaining = paragraph
        if (remaining.isEmpty()) {
            lines += ""
        }
        while (remaining.isNotEmpty()) {
            if (paint.measureText(remaining) <= maxWidthPx) {
                lines += remaining
                break
            }
            var splitIndex = remaining.length
            while (splitIndex > 1 && paint.measureText(remaining, 0, splitIndex) > maxWidthPx) {
                splitIndex--
            }
            val nearestSpace = remaining.lastIndexOf(' ', startIndex = splitIndex - 1)
            if (nearestSpace > 0) {
                splitIndex = nearestSpace
            }
            if (splitIndex <= 0) {
                splitIndex = 1
            }
            val line = remaining.substring(0, splitIndex).trimEnd()
            lines += if (line.isEmpty()) remaining.substring(0, splitIndex) else line
            remaining = remaining.substring(splitIndex).trimStart()
        }
        if (paragraphIndex < paragraphs.lastIndex && lines.size < maxLines) {
            // Keep explicit paragraph separation predictable.
            if (lines.isNotEmpty() && lines.last().isNotEmpty()) {
                lines += ""
            }
        }
    }
    if (lines.size <= maxLines) {
        return lines
    }
    val truncated = lines.take(maxLines).toMutableList()
    truncated[truncated.lastIndex] = ellipsizeToWidth(
        text = truncated.last(),
        paint = paint,
        maxWidthPx = maxWidthPx,
    )
    return truncated
}

private fun ellipsizeToWidth(
    text: String,
    paint: AndroidPaint,
    maxWidthPx: Float,
): String {
    if (text.isEmpty() || maxWidthPx <= 0f) return ""
    val normalizedWidthPx = maxWidthPx.roundToInt().coerceAtLeast(1)
    val key = EllipsizeCacheKey(
        text = text,
        maxWidthPx = normalizedWidthPx,
        textSizeBits = paint.textSize.toBits(),
        textScaleXBits = paint.textScaleX.toBits(),
        letterSpacingBits = paint.letterSpacing.toBits(),
        isFakeBold = paint.isFakeBoldText,
        typefaceHash = paint.typeface?.hashCode() ?: 0,
    )
    return WidgetTextEllipsizeCache.resolve(
        key = key,
        text = text,
        paint = paint,
        maxWidthPx = normalizedWidthPx.toFloat(),
    )
}

private fun computeEllipsizedText(
    text: String,
    paint: AndroidPaint,
    maxWidthPx: Float,
): String {
    val ellipsis = "..."
    if (paint.measureText(text) <= maxWidthPx) return text
    val ellipsisWidth = paint.measureText(ellipsis)
    if (ellipsisWidth > maxWidthPx) return ""
    var low = 0
    var high = text.length
    while (low < high) {
        val middle = (low + high + 1) ushr 1
        val width = paint.measureText(text, 0, middle) + ellipsisWidth
        if (width <= maxWidthPx) {
            low = middle
        } else {
            high = middle - 1
        }
    }
    return text.substring(0, low).trimEnd() + ellipsis
}

private data class EllipsizeCacheKey(
    val text: String,
    val maxWidthPx: Int,
    val textSizeBits: Int,
    val textScaleXBits: Int,
    val letterSpacingBits: Int,
    val isFakeBold: Boolean,
    val typefaceHash: Int,
)

private object WidgetTextEllipsizeCache {
    private val cache = object : LinkedHashMap<EllipsizeCacheKey, String>(
        WIDGET_ELLIPSIZE_CACHE_SIZE + 1,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<EllipsizeCacheKey, String>?,
        ): Boolean {
            return size > WIDGET_ELLIPSIZE_CACHE_SIZE
        }
    }

    @Synchronized
    fun resolve(
        key: EllipsizeCacheKey,
        text: String,
        paint: AndroidPaint,
        maxWidthPx: Float,
    ): String {
        cache[key]?.let { cached ->
            return cached
        }
        return computeEllipsizedText(
            text = text,
            paint = paint,
            maxWidthPx = maxWidthPx,
        ).also { computed ->
            cache[key] = computed
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWidgetClockHand(
    center: Offset,
    radius: Float,
    progress: Float,
    lengthFactor: Float,
    strokeWidth: Float,
    color: Color,
) {
    val angleRad = (progress * (2f * PI.toFloat())) - (PI.toFloat() / 2f)
    val handLength = radius * lengthFactor
    val end = Offset(
        x = center.x + cos(angleRad) * handLength,
        y = center.y + sin(angleRad) * handLength,
    )
    drawLine(
        color = color,
        start = center,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private data class FrameResizeHandlePlacement(
    val handle: CanvasFrameResizeHandle,
    val centerX: Float,
    val centerY: Float,
)

private data class FrameLayoutState(
    val frame: CanvasFrameObjectUiState,
    val widthPx: Float,
    val heightPx: Float,
    val leftPx: Float,
    val topPx: Float,
)

private data class StickyLayoutState(
    val note: CanvasStickyNoteUiState,
    val noteText: String,
    val center: ScreenPoint,
    val noteSizePx: Float,
    val leftPx: Float,
    val topPx: Float,
    val textPaddingPx: Float,
    val textContentWidthPx: Float,
    val textContentHeightPx: Float,
    val textLayout: StickyTextLayout,
)

private data class TextObjectLayoutState(
    val textObject: CanvasTextObjectUiState,
    val text: String,
    val textSizePx: Float,
    val leftPx: Float,
    val topPx: Float,
    val widthPx: Float,
    val heightPx: Float,
)

private data class WidgetLayoutState(
    val widget: CanvasWidgetUiState,
    val center: ScreenPoint,
    val widthPx: Float,
    val heightPx: Float,
    val leftPx: Float,
    val topPx: Float,
    val centerZoneLeftPx: Float,
    val centerZoneTopPx: Float,
    val centerZoneWidthPx: Float,
    val centerZoneHeightPx: Float,
)

private val FRAME_CORNER_RADIUS_DP = 10.dp
private val FRAME_BORDER_STROKE_DP = 1.8.dp
private val FRAME_BORDER_TAP_HIT_DP = 18.dp
private val FRAME_RESIZE_HANDLE_TOUCH_TARGET_DP = 30.dp
private val FRAME_RESIZE_HANDLE_VISUAL_SIZE_DP = 12.dp
private val FRAME_TITLE_OFFSET_DP = 8.dp
private val DELETE_BUTTON_VERTICAL_OFFSET_DP = 42.dp
private val FRAME_TITLE_CORNER_RADIUS_DP = 8.dp
private val FRAME_TITLE_HORIZONTAL_PADDING_DP = 8.dp
private val FRAME_TITLE_VERTICAL_PADDING_DP = 5.dp
private val FRAME_TITLE_TEXT_SIZE_SP = 13.sp
private const val STICKY_TEXT_INNER_PADDING_SIZE_FRACTION = 0.11f
private const val STICKY_TEXT_INNER_PADDING_MIN_PX = 2f
private const val STICKY_TEXT_INNER_PADDING_MAX_PX = 22f
private const val STICKY_TEXT_LINE_HEIGHT_MULTIPLIER = 1.08f
private const val STICKY_TEXT_MIN_SIZE_PX = 6f
private const val STICKY_TEXT_MAX_SIZE_FRACTION = 0.32f
private const val STICKY_TEXT_ESTIMATED_CHAR_WIDTH_FACTOR = 0.68f
private const val STICKY_TEXT_MIN_CHARS_PER_LINE = 6
private const val STICKY_TEXT_MAX_CHARS_PER_LINE = 30
private const val SELECTION_HANDLE_DEFER_RADIUS_PX = 22f
private val EDGE_AUTO_PAN_ZONE_DP = 72.dp
private val EDGE_AUTO_PAN_MAX_STEP_DP = 16.dp
private const val MIN_TRANSFORM_PAN_DELTA_SQ_PX = 0.16f
private const val MIN_TRANSFORM_ZOOM_DELTA = 0.0012f
private const val WIDGET_MIN_WIDTH_RENDER_SIZE_PX = 56f
private const val WIDGET_MIN_HEIGHT_RENDER_SIZE_PX = 42f
private const val WIDGET_CENTER_DRAG_WIDTH_FACTOR = 0.46f
private const val WIDGET_CENTER_DRAG_HEIGHT_FACTOR = 0.58f
private const val WIDGET_CLOCK_TEXT_SIZE_FACTOR = 0.34f
private const val WIDGET_CLOCK_MIN_TEXT_SIZE_PX = 10f
private const val WIDGET_CLOCK_MAX_TEXT_HEIGHT_FRACTION = 0.70f
private const val WIDGET_CORNER_RADIUS_PX = 16f
private const val WIDGET_BORDER_STROKE_PX = 1.4f
private const val WIDGET_INFO_HORIZONTAL_PADDING_PX = 16f
private const val WIDGET_INFO_BODY_MAX_HEIGHT_FRACTION = 0.68f
private const val WIDGET_INFO_SUBTITLE_MAX_HEIGHT_FRACTION = 0.34f
private const val WIDGET_ANALOG_FACE_DIAMETER_FACTOR = 0.80f
private const val WIDGET_ANALOG_OUTER_STROKE_FACTOR = 0.036f
private const val WIDGET_ANALOG_MIN_STROKE_PX = 1.4f
private const val WIDGET_ANALOG_MARKER_INNER_FACTOR = 0.78f
private const val WIDGET_ANALOG_HOUR_HAND_LENGTH_FACTOR = 0.52f
private const val WIDGET_ANALOG_MINUTE_HAND_LENGTH_FACTOR = 0.74f
private const val WIDGET_ANALOG_SECOND_HAND_LENGTH_FACTOR = 0.84f
private const val WIDGET_ANALOG_CENTER_DOT_FACTOR = 0.065f
private const val WIDGET_ANALOG_FACE_FILL_ALPHA = 0.14f
private const val WIDGET_WEATHER_REFRESH_MS = 15 * 60 * 1_000L
private const val WIDGET_WEATHER_NETWORK_TIMEOUT_MS = 4_500
private const val WIDGET_CALENDAR_REFRESH_MS = 5 * 60 * 1_000L
private const val WIDGET_CALENDAR_MAX_EVENTS = 8
private const val WIDGET_NOTIFICATIONS_POLL_MS = 3_000L
private const val WIDGET_NOTIFICATION_STATIONARY_MS = 5_000L
private const val WIDGET_NOTIFICATION_SCROLL_MS = 850L
private const val WIDGET_NOTIFICATION_TICKER_FRAME_MS = 48L
private const val WIDGET_NOTIFICATION_LINE_HEIGHT_MULTIPLIER = 1.18f
private const val WIDGET_ELLIPSIZE_CACHE_SIZE = 512
private const val WIDGET_HOURLY_FORECAST_ITEMS = 3
private const val WIDGET_DAILY_FORECAST_ITEMS = 3
private const val WIDGET_UNAVAILABLE_VALUE = "--"
private const val NOTIFICATION_LISTENERS_SETTING_KEY = "enabled_notification_listeners"
private const val TEXT_ESTIMATED_CHAR_WIDTH_FACTOR = 0.58f
private const val TEXT_ESTIMATED_LINE_HEIGHT_MULTIPLIER = 1.12f
private const val TEXT_ESTIMATED_MIN_WIDTH_MULTIPLIER = 0.9f
private const val OBJECT_DRAG_START_SLOP_MULTIPLIER = 1.0f
private val WIDGET_DIGITAL_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val WIDGET_HOURLY_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
private val WIDGET_DAILY_DAY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

