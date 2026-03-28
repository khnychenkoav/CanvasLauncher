package com.darksok.canvaslauncher.feature.launcher.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
    val scale = cameraState.scale
    val allowsObjectMove = isEditActive && editState.selectedTool == CanvasEditToolId.Move
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
                        color = Color(frame.colorArgb).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .objectMoveModifier(
                        enabled = allowsObjectMove,
                        target = CanvasObjectDragTarget.Frame(frame.id),
                        onObjectDragStart = onObjectDragStart,
                        onObjectDragDelta = onObjectDragDelta,
                        onObjectDragEnd = onObjectDragEnd,
                    )
                    .pointerInput(allowsObjectMove, frame.id) {
                        if (!allowsObjectMove) return@pointerInput
                        detectTapGestures(
                            onTap = { onFrameTap(frame.id) },
                        )
                    },
            ) {
                Text(
                    text = frame.title.ifBlank { "Frame" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    modifier = Modifier.offset(x = 12.dp, y = 10.dp),
                )
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
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(note.colorArgb).copy(alpha = 0.28f),
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
                    .objectMoveModifier(
                        enabled = allowsObjectMove,
                        target = CanvasObjectDragTarget.Sticky(note.id),
                        onObjectDragStart = onObjectDragStart,
                        onObjectDragDelta = onObjectDragDelta,
                        onObjectDragEnd = onObjectDragEnd,
                    )
                    .pointerInput(allowsObjectMove, note.id, noteSizePx) {
                        if (!allowsObjectMove) return@pointerInput
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val centerTap = (tapOffset - Offset(noteSizePx / 2f, noteSizePx / 2f))
                                    .getDistance() <= noteSizePx * 0.22f
                                onStickyTap(note.id, centerTap)
                            },
                            onLongPress = {
                                onStickyLongPress(note.id)
                            },
                        )
                    },
            ) {
                Text(
                    text = note.text.ifBlank { "Note" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.offset(x = 10.dp, y = 10.dp),
                    maxLines = 4,
                )
            }
        }

        textObjects.forEach { textObject ->
            val screen = WorldScreenTransformer.worldToScreen(textObject.position, cameraState)
            val textSizePx = (textObject.textSizeWorld * scale).coerceIn(12f, 128f)
            Text(
                text = textObject.text.ifBlank { "Text" },
                color = Color(textObject.colorArgb),
                fontSize = with(density) { textSizePx.toSp() },
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = screen.x.roundToInt(),
                            y = screen.y.roundToInt(),
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
                    .pointerInput(allowsObjectMove, textObject.id) {
                        if (!allowsObjectMove) return@pointerInput
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
