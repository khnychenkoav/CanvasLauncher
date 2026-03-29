package com.darksok.canvaslauncher.feature.launcher.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.darksok.canvaslauncher.feature.launcher.R
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.performance.WorldScreenTransformer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CanvasEditLayer(
    cameraState: CameraState,
    isEditActive: Boolean,
    editState: EditUiState,
    frames: List<CanvasFrameObjectUiState>,
    frameDraft: CanvasFrameDraftUiState?,
    selectedFrameIdForResize: String?,
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
    onFrameTap: (id: String) -> Unit,
    onFrameBorderTap: (id: String) -> Unit,
    onFrameResizeStart: (id: String, handle: CanvasFrameResizeHandle) -> Unit,
    onFrameResizeDrag: (id: String, handle: CanvasFrameResizeHandle, delta: ScreenPoint) -> Unit,
    onFrameResizeEnd: () -> Unit,
    onObjectDragStart: (CanvasObjectDragTarget) -> Unit,
    onObjectDragDelta: (CanvasObjectDragTarget, ScreenPoint) -> Unit,
    onObjectDragEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val scale = cameraState.scale
    val allowsObjectMove = isEditActive && editState.selectedTool == CanvasEditToolId.Move
    val allowsObjectTap = isEditActive && (
        editState.selectedTool == CanvasEditToolId.Move ||
            editState.selectedTool == CanvasEditToolId.Delete
        )
    val allowsFrameBorderSelection = isEditActive && editState.selectedTool == CanvasEditToolId.Move
    val showsSelectionOverlay = isEditActive && editState.selectedTool == CanvasEditToolId.Selection
    val guideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
    val frameCornerRadiusPx = with(density) { FRAME_CORNER_RADIUS_DP.toPx() }
    val frameBorderStrokePx = with(density) { FRAME_BORDER_STROKE_DP.toPx() }
    val frameBorderHitWidthPx = with(density) { FRAME_BORDER_TAP_HIT_DP.toPx() }
    val autoPanZonePx = with(density) { EDGE_AUTO_PAN_ZONE_DP.toPx() }
    val autoPanMaxStepPx = with(density) { EDGE_AUTO_PAN_MAX_STEP_DP.toPx() }
    val stickyTextBaseStyle = MaterialTheme.typography.bodyMedium

    Box(
        modifier = modifier
            .fillMaxSize()
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
                                height = with(density) { visibleHeightPx.toDp() },
                            )
                            .pointerInput(
                                frame.id,
                                frameLeft,
                                frameTop,
                                frameRight,
                                frameBottom,
                                visibleLeft,
                                visibleTop,
                                frameBorderHitWidthPx,
                            ) {
                                detectTapGestures(
                                    onTap = { tapOffset ->
                                        val tapX = visibleLeft + tapOffset.x
                                        val tapY = visibleTop + tapOffset.y
                                        if (
                                            isOnFrameBorderAbsolute(
                                                tapX = tapX,
                                                tapY = tapY,
                                                frameLeft = frameLeft,
                                                frameTop = frameTop,
                                                frameRight = frameRight,
                                                frameBottom = frameBottom,
                                                hitWidthPx = frameBorderHitWidthPx,
                                            )
                                        ) {
                                            onFrameBorderTap(frame.id)
                                        }
                                    },
                                )
                            }
                            .zIndex(0.31f),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (item.leftPx + with(density) { 8.dp.toPx() }).roundToInt(),
                            y = (item.topPx + with(density) { 8.dp.toPx() }).roundToInt(),
                        )
                    }
                    .zIndex(0.32f)
                    .objectMoveModifier(
                        enabled = isEditActive && allowsObjectMove,
                        target = CanvasObjectDragTarget.Frame(frame.id),
                        nodeTopLeftScreen = ScreenPoint(
                            x = item.leftPx + with(density) { 8.dp.toPx() },
                            y = item.topPx + with(density) { 8.dp.toPx() },
                        ),
                        canvasWidthPx = viewportWidthPx,
                        canvasHeightPx = viewportHeightPx,
                        autoPanZonePx = autoPanZonePx,
                        autoPanMaxStepPx = autoPanMaxStepPx,
                        onObjectDragStart = onObjectDragStart,
                        onObjectDragDelta = onObjectDragDelta,
                        onObjectDragEnd = onObjectDragEnd,
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
            ) {
            Text(
                text = frame.title.ifBlank { stringResource(id = R.string.canvas_frame_fallback_title) },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            )
            }

            if (isEditActive && allowsFrameBorderSelection && selectedFrameIdForResize == frame.id) {
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

                    if (editState.selectedTool == CanvasEditToolId.Selection && bounds.canResizeAndDelete) {
                        SelectionDeleteButton(
                            onClick = onSelectionDeleteTap,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-20).dp)
                                .zIndex(1.2f),
                        )
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

        stickyNotes.forEach { note ->
            val center = WorldScreenTransformer.worldToScreen(note.center, cameraState)
            val noteSizePx = (note.sizeWorld * scale).coerceAtLeast(42f)
            val noteLeftPx = center.x - noteSizePx / 2f
            val noteTopPx = center.y - noteSizePx / 2f
            val noteText = note.text.ifBlank { stringResource(id = R.string.canvas_sticky_fallback_text) }
            val lengthFactor = (STICKY_TEXT_BASE_LENGTH / noteText.length.coerceAtLeast(1).toFloat())
                .coerceIn(STICKY_TEXT_MIN_LENGTH_FACTOR, STICKY_TEXT_MAX_LENGTH_FACTOR)
            val stickyAreaFactor = (note.sizeWorld / CanvasEditDefaults.DEFAULT_STICKY_SIZE_WORLD)
                .coerceIn(STICKY_TEXT_MIN_AREA_FACTOR, STICKY_TEXT_MAX_AREA_FACTOR)
            val preferredStickyTextSizePx = (note.textSizeWorld * scale * stickyAreaFactor * lengthFactor)
                .coerceIn(9f, 72f)
            val stickyInnerPaddingPx = with(density) { STICKY_TEXT_INNER_PADDING_DP.toPx() * 2f }
            val stickyTextSizePx = remember(
                note.id,
                note.text,
                noteSizePx,
                note.textSizeWorld,
                scale,
            ) {
                fitStickyTextSizePx(
                    textMeasurer = textMeasurer,
                    baseStyle = stickyTextBaseStyle,
                    density = density,
                    text = noteText,
                    preferredTextSizePx = preferredStickyTextSizePx,
                    contentWidthPx = (noteSizePx - stickyInnerPaddingPx).coerceAtLeast(18f),
                    contentHeightPx = (noteSizePx - stickyInnerPaddingPx).coerceAtLeast(18f),
                )
            }
            Surface(
                shape = RectangleShape,
                color = Color(note.colorArgb).copy(alpha = 0.92f),
                tonalElevation = 3.dp,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (center.x - noteSizePx / 2f).roundToInt(),
                            y = (center.y - noteSizePx / 2f).roundToInt(),
                        )
                    }
                    .size(with(density) { noteSizePx.toDp() })
                    .zIndex(0.6f)
                    .shadow(
                        elevation = 8.dp,
                        shape = RectangleShape,
                        clip = false,
                    )
                    .objectMoveModifier(
                        enabled = allowsObjectMove,
                        target = CanvasObjectDragTarget.Sticky(note.id),
                        nodeTopLeftScreen = ScreenPoint(
                            x = noteLeftPx,
                            y = noteTopPx,
                        ),
                        canvasWidthPx = cameraState.viewportWidthPx.toFloat(),
                        canvasHeightPx = cameraState.viewportHeightPx.toFloat(),
                        autoPanZonePx = autoPanZonePx,
                        autoPanMaxStepPx = autoPanMaxStepPx,
                        onObjectDragStart = onObjectDragStart,
                        onObjectDragDelta = onObjectDragDelta,
                        onObjectDragEnd = onObjectDragEnd,
                        onAutoPanDelta = onAutoPanDelta,
                    )
                    .then(
                        if (allowsObjectTap) {
                            Modifier.pointerInput(note.id, noteSizePx, editState.selectedTool) {
                                detectTapGestures(
                                    onTap = { tapOffset ->
                                        val centerTap = if (editState.selectedTool == CanvasEditToolId.Delete) {
                                            true
                                        } else {
                                            (tapOffset - Offset(noteSizePx / 2f, noteSizePx / 2f))
                                                .getDistance() <= noteSizePx * 0.22f
                                        }
                                        onStickyTap(note.id, centerTap)
                                    },
                                    onLongPress = {
                                        if (editState.selectedTool == CanvasEditToolId.Move) {
                                            onStickyLongPress(note.id)
                                        }
                                    },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.14f),
                                ),
                                startY = size.height * 0.70f,
                                endY = size.height,
                            ),
                            topLeft = Offset(0f, size.height * 0.70f),
                            size = Size(size.width, size.height * 0.30f),
                        )
                    }
                    Text(
                        text = noteText,
                        style = stickyTextBaseStyle.copy(
                            fontSize = with(density) { stickyTextSizePx.toSp() },
                            lineHeight = with(density) { (stickyTextSizePx * STICKY_TEXT_LINE_HEIGHT_MULTIPLIER).toSp() },
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }

        textObjects.forEach { textObject ->
            val screen = WorldScreenTransformer.worldToScreen(textObject.position, cameraState)
            val textSizePx = (textObject.textSizeWorld * scale).coerceIn(12f, 128f)
            val textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = with(density) { textSizePx.toSp() },
            )
            val textContent = textObject.text.ifBlank { stringResource(id = R.string.canvas_text_fallback_text) }
            val measured = textMeasurer.measure(
                text = textContent,
                style = textStyle,
            )
            val textLeftPx = screen.x - measured.size.width / 2f
            val textTopPx = screen.y - measured.size.height / 2f
            Text(
                text = textContent,
                color = Color(textObject.colorArgb),
                style = textStyle,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (screen.x - measured.size.width / 2f).roundToInt(),
                            y = (screen.y - measured.size.height / 2f).roundToInt(),
                        )
                    }
                    .zIndex(0.7f)
                    .objectMoveModifier(
                        enabled = allowsObjectMove,
                        target = CanvasObjectDragTarget.Text(textObject.id),
                        nodeTopLeftScreen = ScreenPoint(
                            x = textLeftPx,
                            y = textTopPx,
                        ),
                        canvasWidthPx = cameraState.viewportWidthPx.toFloat(),
                        canvasHeightPx = cameraState.viewportHeightPx.toFloat(),
                        autoPanZonePx = autoPanZonePx,
                        autoPanMaxStepPx = autoPanMaxStepPx,
                        onObjectDragStart = onObjectDragStart,
                        onObjectDragDelta = onObjectDragDelta,
                        onObjectDragEnd = onObjectDragEnd,
                        onAutoPanDelta = onAutoPanDelta,
                    )
                    .then(
                        if (allowsObjectTap) {
                            Modifier.pointerInput(textObject.id) {
                                detectTapGestures(
                                    onTap = { onTextTap(textObject.id) },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
        }

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
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    val startWorld = WorldScreenTransformer.screenToWorld(
                        point = ScreenPoint(down.position.x, down.position.y),
                        camera = latestCameraState.value,
                    )
                    val currentBounds = latestSelectionBounds.value
                    val deferToResizeHandle = currentBounds?.let { bounds ->
                        bounds.canResizeAndDelete && isNearSelectionResizeHandle(
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
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) {
                                onSelectionDragEnd()
                                dragEnded = true
                                break
                            }
                            val movedDistancePx = (change.position - down.position).getDistance()
                            if (!dragStarted && movedDistancePx > viewConfiguration.touchSlop) {
                                dragStarted = true
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
                                onSelectionDragUpdate(
                                    WorldScreenTransformer.screenToWorld(
                                        point = ScreenPoint(effectivePointer.x, effectivePointer.y),
                                        camera = latestCameraState.value,
                                    ),
                                )
                            }
                        }
                        if (!dragEnded) {
                            onSelectionDragEnd()
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
    onAutoPanDelta: (ScreenPoint) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(target) {
        detectDragGestures(
            onDragStart = {
                onObjectDragStart(target)
            },
            onDragEnd = onObjectDragEnd,
            onDragCancel = onObjectDragEnd,
            onDrag = { change, dragAmount ->
                val pointerScreen = Offset(
                    x = nodeTopLeftScreen.x + change.position.x,
                    y = nodeTopLeftScreen.y + change.position.y,
                )
                val autoPanDelta = edgeAutoPanDelta(
                    pointerScreen = pointerScreen,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    autoPanZonePx = autoPanZonePx,
                    autoPanMaxStepPx = autoPanMaxStepPx,
                )
                autoPanDelta?.let(onAutoPanDelta)
                change.consume()
                onObjectDragDelta(
                    target,
                    ScreenPoint(
                        x = dragAmount.x + autoPanDelta.xOrZero(),
                        y = dragAmount.y + autoPanDelta.yOrZero(),
                    ),
                )
            },
        )
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

private fun isOnFrameBorderAbsolute(
    tapX: Float,
    tapY: Float,
    frameLeft: Float,
    frameTop: Float,
    frameRight: Float,
    frameBottom: Float,
    hitWidthPx: Float,
): Boolean {
    if (tapX < frameLeft || tapX > frameRight || tapY < frameTop || tapY > frameBottom) return false
    return abs(tapX - frameLeft) <= hitWidthPx ||
        abs(frameRight - tapX) <= hitWidthPx ||
        abs(tapY - frameTop) <= hitWidthPx ||
        abs(frameBottom - tapY) <= hitWidthPx
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

private fun fitStickyTextSizePx(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    baseStyle: androidx.compose.ui.text.TextStyle,
    density: Density,
    text: String,
    preferredTextSizePx: Float,
    contentWidthPx: Float,
    contentHeightPx: Float,
): Float {
    if (text.isBlank()) return preferredTextSizePx.coerceAtLeast(STICKY_TEXT_MIN_FIT_SIZE_PX)
    val maxWidth = contentWidthPx.roundToInt().coerceAtLeast(1)
    val maxHeight = contentHeightPx.roundToInt().coerceAtLeast(1)
    var candidate = preferredTextSizePx.coerceAtLeast(STICKY_TEXT_MIN_FIT_SIZE_PX)
    repeat(STICKY_TEXT_FIT_MAX_STEPS) {
        val layout = textMeasurer.measure(
            text = text,
            style = baseStyle.copy(
                fontSize = with(density) { candidate.toSp() },
                lineHeight = with(density) { (candidate * STICKY_TEXT_LINE_HEIGHT_MULTIPLIER).toSp() },
            ),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Clip,
            constraints = Constraints(maxWidth = maxWidth),
        )
        val fits = !layout.hasVisualOverflow && layout.size.height <= maxHeight
        if (fits || candidate <= STICKY_TEXT_MIN_FIT_SIZE_PX + 0.1f) {
            return candidate
        }
        candidate = (candidate * STICKY_TEXT_FIT_SHRINK_FACTOR)
            .coerceAtLeast(STICKY_TEXT_MIN_FIT_SIZE_PX)
    }
    return candidate
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

private const val STICKY_TEXT_BASE_LENGTH = 26f
private const val STICKY_TEXT_MIN_LENGTH_FACTOR = 0.72f
private const val STICKY_TEXT_MAX_LENGTH_FACTOR = 1.46f
private const val STICKY_TEXT_MIN_AREA_FACTOR = 0.75f
private const val STICKY_TEXT_MAX_AREA_FACTOR = 1.65f
private val FRAME_CORNER_RADIUS_DP = 10.dp
private val FRAME_BORDER_STROKE_DP = 1.8.dp
private val FRAME_BORDER_TAP_HIT_DP = 18.dp
private val FRAME_RESIZE_HANDLE_TOUCH_TARGET_DP = 30.dp
private val FRAME_RESIZE_HANDLE_VISUAL_SIZE_DP = 12.dp
private val STICKY_TEXT_INNER_PADDING_DP = 12.dp
private const val STICKY_TEXT_LINE_HEIGHT_MULTIPLIER = 1.08f
private const val STICKY_TEXT_MIN_FIT_SIZE_PX = 6f
private const val STICKY_TEXT_FIT_SHRINK_FACTOR = 0.9f
private const val STICKY_TEXT_FIT_MAX_STEPS = 22
private const val SELECTION_HANDLE_DEFER_RADIUS_PX = 22f
private val EDGE_AUTO_PAN_ZONE_DP = 72.dp
private val EDGE_AUTO_PAN_MAX_STEP_DP = 16.dp
