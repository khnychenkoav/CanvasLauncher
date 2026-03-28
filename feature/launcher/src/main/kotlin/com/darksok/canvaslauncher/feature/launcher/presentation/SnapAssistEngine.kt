package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import kotlin.math.abs

internal data class SnapAssistResult(
    val position: WorldPoint,
    val guides: List<CanvasSnapGuideUiState>,
)

internal object SnapAssistEngine {

    fun snap(
        candidate: WorldPoint,
        anchors: List<WorldPoint>,
        cameraScale: Float,
        baseThresholdPx: Float = SNAP_THRESHOLD_SCREEN_PX,
    ): SnapAssistResult {
        if (anchors.isEmpty() || cameraScale <= 0f) {
            return SnapAssistResult(position = candidate, guides = emptyList())
        }
        val thresholdWorld = baseThresholdPx / cameraScale

        val nearestX = anchors
            .map { anchor -> anchor.x }
            .minByOrNull { x -> abs(x - candidate.x) }
        val nearestY = anchors
            .map { anchor -> anchor.y }
            .minByOrNull { y -> abs(y - candidate.y) }

        var snappedX = candidate.x
        var snappedY = candidate.y
        val guides = mutableListOf<CanvasSnapGuideUiState>()

        if (nearestX != null && abs(nearestX - candidate.x) <= thresholdWorld) {
            snappedX = nearestX
            guides += CanvasSnapGuideUiState(
                orientation = CanvasSnapOrientation.Vertical,
                worldCoordinate = nearestX,
            )
        }

        if (nearestY != null && abs(nearestY - candidate.y) <= thresholdWorld) {
            snappedY = nearestY
            guides += CanvasSnapGuideUiState(
                orientation = CanvasSnapOrientation.Horizontal,
                worldCoordinate = nearestY,
            )
        }

        return SnapAssistResult(
            position = WorldPoint(snappedX, snappedY),
            guides = guides,
        )
    }

    private const val SNAP_THRESHOLD_SCREEN_PX = 18f
}
