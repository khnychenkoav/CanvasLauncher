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
        previousGuides: List<CanvasSnapGuideUiState> = emptyList(),
        baseThresholdPx: Float = SNAP_THRESHOLD_SCREEN_PX,
        axisInfluencePx: Float = SNAP_AXIS_INFLUENCE_SCREEN_PX,
        releaseMultiplier: Float = SNAP_RELEASE_MULTIPLIER,
    ): SnapAssistResult {
        if (anchors.isEmpty() || cameraScale <= 0f) {
            return SnapAssistResult(position = candidate, guides = emptyList())
        }
        val thresholdWorld = baseThresholdPx / cameraScale
        val axisInfluenceWorld = axisInfluencePx / cameraScale
        val releaseThresholdWorld = thresholdWorld * releaseMultiplier

        val previousX = previousGuides
            .firstOrNull { guide -> guide.orientation == CanvasSnapOrientation.Vertical }
            ?.worldCoordinate
        val previousY = previousGuides
            .firstOrNull { guide -> guide.orientation == CanvasSnapOrientation.Horizontal }
            ?.worldCoordinate

        val xCandidates = anchors
            .asSequence()
            .filter { anchor -> abs(anchor.y - candidate.y) <= axisInfluenceWorld }
            .map { anchor -> anchor.x }
            .toList()
        val yCandidates = anchors
            .asSequence()
            .filter { anchor -> abs(anchor.x - candidate.x) <= axisInfluenceWorld }
            .map { anchor -> anchor.y }
            .toList()

        val nearestX = xCandidates.minByOrNull { x -> abs(x - candidate.x) }
        val nearestY = yCandidates.minByOrNull { y -> abs(y - candidate.y) }

        var snappedX = candidate.x
        var snappedY = candidate.y
        val guides = mutableListOf<CanvasSnapGuideUiState>()

        val stickyX = if (previousX != null && abs(previousX - candidate.x) <= releaseThresholdWorld) {
            previousX
        } else if (nearestX != null && abs(nearestX - candidate.x) <= thresholdWorld) {
            nearestX
        } else {
            null
        }
        if (stickyX != null) {
            snappedX = stickyX
            guides += CanvasSnapGuideUiState(
                orientation = CanvasSnapOrientation.Vertical,
                worldCoordinate = stickyX,
            )
        }

        val stickyY = if (previousY != null && abs(previousY - candidate.y) <= releaseThresholdWorld) {
            previousY
        } else if (nearestY != null && abs(nearestY - candidate.y) <= thresholdWorld) {
            nearestY
        } else {
            null
        }
        if (stickyY != null) {
            snappedY = stickyY
            guides += CanvasSnapGuideUiState(
                orientation = CanvasSnapOrientation.Horizontal,
                worldCoordinate = stickyY,
            )
        }

        return SnapAssistResult(
            position = WorldPoint(snappedX, snappedY),
            guides = guides,
        )
    }

    private const val SNAP_THRESHOLD_SCREEN_PX = 10f
    private const val SNAP_AXIS_INFLUENCE_SCREEN_PX = 96f
    private const val SNAP_RELEASE_MULTIPLIER = 1.20f
}
