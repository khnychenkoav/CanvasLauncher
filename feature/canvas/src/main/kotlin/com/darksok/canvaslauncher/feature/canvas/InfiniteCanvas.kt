package com.darksok.canvaslauncher.feature.canvas

import android.graphics.Paint
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.CanvasConstants
import com.darksok.canvaslauncher.core.model.canvas.ScreenPoint
import com.darksok.canvaslauncher.core.performance.WorldGridProjector
import com.darksok.canvaslauncher.core.performance.WorldScreenTransformer
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun InfiniteCanvas(
    cameraState: CameraState,
    apps: List<CanvasRenderableApp>,
    draggingPackageName: String?,
    labelsEnabled: Boolean,
    backgroundConfig: CanvasBackgroundConfig,
    modifier: Modifier = Modifier,
    onViewportSizeChanged: (IntSize) -> Unit,
    onTransform: (panDeltaPx: ScreenPoint, zoomFactor: Float, focusPx: ScreenPoint) -> Unit,
    onAppClick: (String) -> Unit,
    onAppDragStart: (String) -> Unit,
    onAppDragDelta: (String, ScreenPoint) -> Unit,
    onAppDragEnd: (String) -> Unit,
    onAppDragCancel: () -> Unit,
) {
    val density = LocalDensity.current
    val edgeGestureGuardPx = with(density) { CanvasUiConstants.EDGE_GESTURE_GUARD_DP.toPx() }
    val touchArbiter = remember { CanvasInteractionArbiter() }
    var isGestureInProgress by remember { mutableStateOf(false) }
    var labelsVisible by remember {
        mutableStateOf(cameraState.scale >= CanvasConstants.Scale.LABEL_VISIBLE_THRESHOLD)
    }
    var labelsActivated by remember { mutableStateOf(false) }
    LaunchedEffect(cameraState.scale) {
        labelsVisible = LabelVisibilityPolicy.nextVisibility(
            previousVisible = labelsVisible,
            scale = cameraState.scale,
        )
    }
    LaunchedEffect(labelsEnabled) {
        if (!labelsEnabled) {
            labelsActivated = false
            return@LaunchedEffect
        }
        delay(CanvasUiConstants.LABEL_REVEAL_DELAY_MS)
        labelsActivated = true
    }
    val hasMatchedApps = apps.any { app -> app.searchVisualState == CanvasSearchVisualState.Matched }
    val pulseAlpha = if (hasMatchedApps) {
        val pulseTransition = rememberInfiniteTransition(label = "search-match-pulse")
        val animatedPulse by pulseTransition.animateFloat(
            initialValue = CanvasUiConstants.MATCH_PULSE_MIN_ALPHA,
            targetValue = CanvasUiConstants.MATCH_PULSE_MAX_ALPHA,
            animationSpec = infiniteRepeatable(
                animation = tween(CanvasUiConstants.MATCH_PULSE_DURATION_MS),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "search-match-alpha",
        )
        animatedPulse
    } else {
        1f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged(onViewportSizeChanged)
            .pointerInput(touchArbiter) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val pointerCount = event.changes.count { it.pressed }
                        val gestureActive = pointerCount > 0
                        if (isGestureInProgress != gestureActive) {
                            isGestureInProgress = gestureActive
                        }
                        val eventUptime = event.changes.maxOfOrNull { it.uptimeMillis } ?: SystemClock.uptimeMillis()
                        touchArbiter.onPointerCountChanged(pointerCount, eventUptime)
                        if (pointerCount == 0) {
                            break
                        }
                    }
                }
            }
            .pointerInput(edgeGestureGuardPx, draggingPackageName, touchArbiter) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (draggingPackageName != null) return@detectTransformGestures
                    val nearHorizontalEdge =
                        centroid.x < edgeGestureGuardPx ||
                            centroid.x > size.width - edgeGestureGuardPx
                    val mostlyHorizontalPan = abs(pan.x) > abs(pan.y)
                    if (nearHorizontalEdge && mostlyHorizontalPan && zoom == 1f) {
                        return@detectTransformGestures
                    }

                    if (pan.x != 0f || pan.y != 0f || zoom != 1f) {
                        touchArbiter.onCanvasTransformDetected(SystemClock.uptimeMillis())
                    }

                    onTransform(
                        ScreenPoint(pan.x, pan.y),
                        zoom,
                        ScreenPoint(centroid.x, centroid.y),
                    )
                }
            },
    ) {
        WorldDotsBackground(
            cameraState = cameraState,
            config = backgroundConfig,
            modifier = Modifier.fillMaxSize(),
        )

        apps.forEach { app ->
            key(app.packageName) {
                val screenPoint = WorldScreenTransformer.worldToScreen(app.worldPosition, cameraState)
                val iconSizePx = (CanvasConstants.Sizes.ICON_WORLD_SIZE * cameraState.scale)
                    .coerceIn(CanvasUiConstants.MIN_ICON_SIZE_PX, CanvasUiConstants.MAX_ICON_SIZE_PX)
                val iconTopLeft = IntOffset(
                    x = (screenPoint.x - iconSizePx / 2f).roundToInt(),
                    y = (screenPoint.y - iconSizePx / 2f).roundToInt(),
                )

                CanvasAppNode(
                    packageName = app.packageName,
                    label = app.label,
                    iconBitmap = app.icon,
                    searchVisualState = app.searchVisualState,
                    matchPulseAlpha = pulseAlpha,
                    iconSizePx = iconSizePx,
                    position = iconTopLeft,
                    showLabel = labelsEnabled && labelsActivated && labelsVisible && !isGestureInProgress,
                    showFullLabel = cameraState.scale >= CanvasUiConstants.FULL_LABEL_ZOOM_THRESHOLD,
                    isDragging = draggingPackageName == app.packageName,
                    onClick = onAppClick,
                    onDragStart = onAppDragStart,
                    onDragDelta = onAppDragDelta,
                    onDragEnd = onAppDragEnd,
                    onDragCancel = onAppDragCancel,
                    isTapSuppressed = touchArbiter::shouldSuppressTap,
                )
            }
        }
    }
}

@Composable
private fun WorldDotsBackground(
    cameraState: CameraState,
    config: CanvasBackgroundConfig,
    modifier: Modifier = Modifier,
) {
    val dotPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }
    val pointBuffer = remember { DotPointBuffer() }
    SideEffect {
        dotPaint.color = config.dotColor.toArgb()
        dotPaint.strokeWidth = (config.dotRadiusPx * 2f).coerceAtLeast(1f)
    }

    Canvas(modifier = modifier) {
        drawRect(color = config.fillColor)

        val spacingPx = (config.dotSpacingWorld * cameraState.scale)
            .coerceAtLeast(CanvasUiConstants.MIN_DOT_SPACING_PX)
        val origin = WorldGridProjector.anchoredOrigin(
            worldCenter = cameraState.worldCenter,
            scale = cameraState.scale,
            viewportWidthPx = size.width,
            viewportHeightPx = size.height,
            spacingPx = spacingPx,
        )

        val columns = ((size.width + spacingPx * 2f) / spacingPx).toInt().coerceAtLeast(1)
        val rows = ((size.height + spacingPx * 2f) / spacingPx).toInt().coerceAtLeast(1)
        val points = pointBuffer.obtain(columns * rows * 2)
        var pointCount = 0

        var x = origin.x
        while (x <= size.width + spacingPx) {
            var y = origin.y
            while (y <= size.height + spacingPx) {
                points[pointCount++] = x
                points[pointCount++] = y
                y += spacingPx
            }
            x += spacingPx
        }

        if (pointCount > 0) {
            drawIntoCanvas { composeCanvas ->
                composeCanvas.nativeCanvas.drawPoints(points, 0, pointCount, dotPaint)
            }
        }
    }
}

@Composable
private fun CanvasAppNode(
    packageName: String,
    label: String,
    iconBitmap: Bitmap?,
    searchVisualState: CanvasSearchVisualState,
    matchPulseAlpha: Float,
    iconSizePx: Float,
    position: IntOffset,
    showLabel: Boolean,
    showFullLabel: Boolean,
    isDragging: Boolean,
    onClick: (String) -> Unit,
    onDragStart: (String) -> Unit,
    onDragDelta: (String, ScreenPoint) -> Unit,
    onDragEnd: (String) -> Unit,
    onDragCancel: () -> Unit,
    isTapSuppressed: (Long) -> Boolean,
) {
    val bitmap: ImageBitmap? = remember(iconBitmap) { iconBitmap?.asImageBitmap() }
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0f) },
        )
    }
    val iconSizeDp = with(LocalDensity.current) { iconSizePx.toDp() }
    val touchSlopPx = LocalViewConfiguration.current.touchSlop
    val isInteractive = searchVisualState != CanvasSearchVisualState.Dimmed
    val interactionModifier = if (isInteractive) {
        Modifier
            .pointerInput(packageName) {
                var dragAccepted = false
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragAccepted = !isTapSuppressed(SystemClock.uptimeMillis())
                        if (dragAccepted) {
                            onDragStart(packageName)
                        }
                    },
                    onDragEnd = {
                        if (dragAccepted) {
                            onDragEnd(packageName)
                        }
                        dragAccepted = false
                    },
                    onDragCancel = {
                        if (dragAccepted) {
                            onDragCancel()
                        }
                        dragAccepted = false
                    },
                    onDrag = { change, dragAmount ->
                        if (!dragAccepted) {
                            return@detectDragGesturesAfterLongPress
                        }
                        change.consume()
                        onDragDelta(packageName, ScreenPoint(dragAmount.x, dragAmount.y))
                    },
                )
            }
            .pointerInput(packageName, touchSlopPx) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    var trackedId = firstDown.id
                    var maxPointerCount = 1
                    var totalMove = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        if (pressedCount > maxPointerCount) {
                            maxPointerCount = pressedCount
                        }

                        val tracked = event.changes.firstOrNull { it.id == trackedId }
                        if (tracked == null) {
                            if (event.changes.none { it.pressed }) break
                            trackedId = event.changes.first().id
                            continue
                        }

                        totalMove += tracked.positionChange().getDistance()
                        if (!tracked.pressed) {
                            val eventUptime = event.changes.maxOfOrNull { it.uptimeMillis } ?: SystemClock.uptimeMillis()
                            if (AppTouchClassifier.shouldTriggerTap(
                                    maxPointerCount = maxPointerCount,
                                    totalMoveDistancePx = totalMove,
                                    touchSlopPx = touchSlopPx,
                                    isTapSuppressed = isTapSuppressed(eventUptime),
                                )
                            ) {
                                onClick(packageName)
                            }
                            break
                        }
                    }
                }
            }
    } else {
        Modifier
    }
    val labelPulseColor = MaterialTheme.colorScheme.onSurface
    val dimmedLabelColor = labelPulseColor.copy(alpha = CanvasUiConstants.DIMMED_LABEL_ALPHA)
    val labelColor = when (searchVisualState) {
        CanvasSearchVisualState.Normal -> labelPulseColor
        CanvasSearchVisualState.Dimmed -> dimmedLabelColor
        CanvasSearchVisualState.Matched -> lerp(
            start = dimmedLabelColor,
            stop = labelPulseColor,
            fraction = matchPulseAlpha,
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset { position }
            .width(iconSizeDp)
            .zIndex(if (isDragging) 2f else 1f)
            .then(interactionModifier),
    ) {
        Box(
            modifier = Modifier
                .size(iconSizeDp)
                .clip(CircleShape)
                .background(
                    if (isDragging) {
                        MaterialTheme.colorScheme.primary.copy(alpha = CanvasUiConstants.DRAG_HIGHLIGHT_ALPHA)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
        ) {
            if (bitmap != null) {
                when (searchVisualState) {
                    CanvasSearchVisualState.Normal -> {
                        Image(
                            bitmap = bitmap,
                            contentDescription = label,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    CanvasSearchVisualState.Dimmed -> {
                        Image(
                            bitmap = bitmap,
                            contentDescription = label,
                            colorFilter = grayscaleFilter,
                            alpha = CanvasUiConstants.DIMMED_ICON_ALPHA,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    CanvasSearchVisualState.Matched -> {
                        Image(
                            bitmap = bitmap,
                            contentDescription = label,
                            colorFilter = grayscaleFilter,
                            alpha = CanvasUiConstants.DIMMED_ICON_ALPHA,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Image(
                            bitmap = bitmap,
                            contentDescription = label,
                            alpha = matchPulseAlpha,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        if (showLabel) {
            val labelContainerModifier = if (showFullLabel) {
                Modifier
                    .padding(top = CanvasUiConstants.LABEL_TOP_MARGIN_DP)
                    .wrapContentWidth(unbounded = true)
                    .clip(RoundedCornerShape(CanvasUiConstants.LABEL_CORNER_DP))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = CanvasUiConstants.LABEL_BACKGROUND_ALPHA))
            } else {
                Modifier
                    .padding(top = CanvasUiConstants.LABEL_TOP_MARGIN_DP)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CanvasUiConstants.LABEL_CORNER_DP))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = CanvasUiConstants.LABEL_BACKGROUND_ALPHA))
            }
            Box(
                modifier = labelContainerModifier,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = if (showFullLabel) TextOverflow.Clip else TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

private object CanvasUiConstants {
    val EDGE_GESTURE_GUARD_DP = 20.dp
    const val MIN_ICON_SIZE_PX = 32f
    const val MAX_ICON_SIZE_PX = 220f
    const val DRAG_HIGHLIGHT_ALPHA = 0.20f
    const val LABEL_BACKGROUND_ALPHA = 0.82f
    val LABEL_TOP_MARGIN_DP = 4.dp
    val LABEL_CORNER_DP = 8.dp
    const val MIN_DOT_SPACING_PX = 20f
    const val LABEL_REVEAL_DELAY_MS = 180L
    const val DIMMED_ICON_ALPHA = 0.92f
    const val DIMMED_LABEL_ALPHA = 0.46f
    const val MATCH_PULSE_MIN_ALPHA = 0.24f
    const val MATCH_PULSE_MAX_ALPHA = 1f
    const val MATCH_PULSE_DURATION_MS = 900
    const val FULL_LABEL_ZOOM_THRESHOLD = CanvasConstants.Scale.MAX_SCALE - 0.01f
}

private class DotPointBuffer {
    private var data: FloatArray = FloatArray(0)

    fun obtain(requiredSize: Int): FloatArray {
        if (requiredSize <= data.size) return data
        var nextSize = data.size.coerceAtLeast(256)
        while (nextSize < requiredSize) {
            nextSize *= 2
        }
        data = FloatArray(nextSize)
        return data
    }
}
