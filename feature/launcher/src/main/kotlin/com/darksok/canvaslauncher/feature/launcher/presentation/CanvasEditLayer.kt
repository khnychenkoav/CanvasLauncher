package com.darksok.canvaslauncher.feature.launcher.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.performance.WorldScreenTransformer
import kotlin.math.roundToInt

@Composable
fun CanvasEditLayer(
    cameraState: CameraState,
    isEditActive: Boolean,
    editState: EditUiState,
    frames: List<CanvasFrameObjectUiState>,
    strokes: List<CanvasStrokeUiState>,
    stickyNotes: List<CanvasStickyNoteUiState>,
    textObjects: List<CanvasTextObjectUiState>,
    snapGuides: List<CanvasSnapGuideUiState>,
    modifier: Modifier = Modifier,
    onCanvasTap: (WorldPoint) -> Unit,
    onBrushStart: (WorldPoint) -> Unit,
    onBrushPoint: (WorldPoint) -> Unit,
    onBrushEnd: () -> Unit,
    onEraseAt: (WorldPoint) -> Unit,
    onStickyTap: (id: String, centerTap: Boolean) -> Unit,
    onStickyLongPress: (id: String) -> Unit,
    onTextTap: (id: String) -> Unit,
    onFrameTap: (id: String) -> Unit,
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
    val guideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .canvasInputModifier(
                isEditActive = isEditActive,
                selectedTool = editState.selectedTool,
                cameraState = cameraState,
                onCanvasTap = onCanvasTap,
                onBrushStart = onBrushStart,
                onBrushPoint = onBrushPoint,
                onBrushEnd = onBrushEnd,
                onEraseAt = onEraseAt,
            ),
    ) {
        frames.forEach { frame ->
            val center = WorldScreenTransformer.worldToScreen(frame.center, cameraState)
            val widthPx = (frame.widthWorld * scale).coerceAtLeast(28f)
            val heightPx = (frame.heightWorld * scale).coerceAtLeast(28f)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (center.x - widthPx / 2f).roundToInt(),
                            y = (center.y - heightPx / 2f).roundToInt(),
                        )
                    }
                    .size(
                        width = with(density) { widthPx.toDp() },
                        height = with(density) { heightPx.toDp() },
                    )
                    .background(
                        color = Color(frame.colorArgb).copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp),
                    ),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .padding(8.dp)
                        .objectMoveModifier(
                            enabled = allowsObjectMove,
                            target = CanvasObjectDragTarget.Frame(frame.id),
                            onObjectDragStart = onObjectDragStart,
                            onObjectDragDelta = onObjectDragDelta,
                            onObjectDragEnd = onObjectDragEnd,
                        )
                        .pointerInput(allowsObjectTap, frame.id, editState.selectedTool) {
                            if (!allowsObjectTap) return@pointerInput
                            detectTapGestures(
                                onTap = { onFrameTap(frame.id) },
                            )
                        },
                ) {
                    Text(
                        text = frame.title.ifBlank { "Frame" },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    )
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
            val noteText = note.text.ifBlank { "Note" }
            val lengthFactor = (STICKY_TEXT_BASE_LENGTH / noteText.length.coerceAtLeast(1).toFloat())
                .coerceIn(STICKY_TEXT_MIN_LENGTH_FACTOR, STICKY_TEXT_MAX_LENGTH_FACTOR)
            val stickyAreaFactor = (note.sizeWorld / CanvasEditDefaults.DEFAULT_STICKY_SIZE_WORLD)
                .coerceIn(STICKY_TEXT_MIN_AREA_FACTOR, STICKY_TEXT_MAX_AREA_FACTOR)
            val stickyTextSizePx = (note.textSizeWorld * scale * stickyAreaFactor * lengthFactor)
                .coerceIn(9f, 72f)
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
                        onObjectDragStart = onObjectDragStart,
                        onObjectDragDelta = onObjectDragDelta,
                        onObjectDragEnd = onObjectDragEnd,
                    )
                    .pointerInput(allowsObjectTap, note.id, noteSizePx, editState.selectedTool) {
                        if (!allowsObjectTap) return@pointerInput
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
                    },
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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = with(density) { stickyTextSizePx.toSp() },
                            lineHeight = with(density) { (stickyTextSizePx * 1.12f).toSp() },
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        maxLines = 8,
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
            val textContent = textObject.text.ifBlank { "Text" }
            val measured = textMeasurer.measure(
                text = textContent,
                style = textStyle,
            )
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
                        onObjectDragStart = onObjectDragStart,
                        onObjectDragDelta = onObjectDragDelta,
                        onObjectDragEnd = onObjectDragEnd,
                    )
                    .pointerInput(allowsObjectTap, textObject.id) {
                        if (!allowsObjectTap) return@pointerInput
                        detectTapGestures(
                            onTap = { onTextTap(textObject.id) },
                        )
                    },
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

private fun Modifier.canvasInputModifier(
    isEditActive: Boolean,
    selectedTool: CanvasEditToolId,
    cameraState: CameraState,
    onCanvasTap: (WorldPoint) -> Unit,
    onBrushStart: (WorldPoint) -> Unit,
    onBrushPoint: (WorldPoint) -> Unit,
    onBrushEnd: () -> Unit,
    onEraseAt: (WorldPoint) -> Unit,
): Modifier {
    if (!isEditActive) return this
    return when (selectedTool) {
        CanvasEditToolId.Brush -> {
            pointerInput(cameraState, selectedTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onBrushStart(
                            WorldScreenTransformer.screenToWorld(
                                point = ScreenPoint(offset.x, offset.y),
                                camera = cameraState,
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
                                camera = cameraState,
                            ),
                        )
                    },
                )
            }
        }

        CanvasEditToolId.Eraser -> {
            pointerInput(cameraState, selectedTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onEraseAt(
                            WorldScreenTransformer.screenToWorld(
                                point = ScreenPoint(offset.x, offset.y),
                                camera = cameraState,
                            ),
                        )
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onEraseAt(
                            WorldScreenTransformer.screenToWorld(
                                point = ScreenPoint(change.position.x, change.position.y),
                                camera = cameraState,
                            ),
                        )
                    },
                )
            }
        }

        CanvasEditToolId.StickyNote,
        CanvasEditToolId.Text,
        CanvasEditToolId.Frame,
        CanvasEditToolId.Delete,
        -> {
            pointerInput(cameraState, selectedTool) {
                detectTapGestures(
                    onTap = { offset ->
                        onCanvasTap(
                            WorldScreenTransformer.screenToWorld(
                                point = ScreenPoint(offset.x, offset.y),
                                camera = cameraState,
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
    onObjectDragStart: (CanvasObjectDragTarget) -> Unit,
    onObjectDragDelta: (CanvasObjectDragTarget, ScreenPoint) -> Unit,
    onObjectDragEnd: () -> Unit,
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
                change.consume()
                onObjectDragDelta(target, ScreenPoint(dragAmount.x, dragAmount.y))
            },
        )
    }
}

private const val STICKY_TEXT_BASE_LENGTH = 26f
private const val STICKY_TEXT_MIN_LENGTH_FACTOR = 0.72f
private const val STICKY_TEXT_MAX_LENGTH_FACTOR = 1.46f
private const val STICKY_TEXT_MIN_AREA_FACTOR = 0.75f
private const val STICKY_TEXT_MAX_AREA_FACTOR = 1.65f
