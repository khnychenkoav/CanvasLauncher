package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SnapAssistEngineTest {

    @Test
    fun `snaps to nearest horizontal and vertical anchors inside threshold`() {
        val result = SnapAssistEngine.snap(
            candidate = WorldPoint(10f, 14f),
            anchors = listOf(
                WorldPoint(12f, 15f),
                WorldPoint(200f, 300f),
            ),
            cameraScale = 2f,
        )

        assertThat(result.position).isEqualTo(WorldPoint(12f, 15f))
        assertThat(result.guides).containsExactly(
            CanvasSnapGuideUiState(CanvasSnapOrientation.Vertical, 12f),
            CanvasSnapGuideUiState(CanvasSnapOrientation.Horizontal, 15f),
        )
    }

    @Test
    fun `does not snap when anchors are outside threshold`() {
        val result = SnapAssistEngine.snap(
            candidate = WorldPoint(10f, 10f),
            anchors = listOf(WorldPoint(40f, 40f)),
            cameraScale = 1f,
        )

        assertThat(result.position).isEqualTo(WorldPoint(10f, 10f))
        assertThat(result.guides).isEmpty()
    }
}
