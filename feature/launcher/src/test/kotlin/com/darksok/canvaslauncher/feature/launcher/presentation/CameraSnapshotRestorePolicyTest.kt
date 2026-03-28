package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.canvas.CameraState
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CameraSnapshotRestorePolicyTest {

    @Test
    fun `restores world state from snapshot and keeps current viewport`() {
        val snapshot = CameraState(
            worldCenter = WorldPoint(120f, 250f),
            scale = 1.4f,
            viewportWidthPx = 2400,
            viewportHeightPx = 1080,
        )
        val current = CameraState(
            worldCenter = WorldPoint(-5f, -10f),
            scale = 0.8f,
            viewportWidthPx = 1080,
            viewportHeightPx = 2340,
        )

        val restored = snapshot.withCurrentViewport(current)

        assertThat(restored.worldCenter).isEqualTo(snapshot.worldCenter)
        assertThat(restored.scale).isEqualTo(snapshot.scale)
        assertThat(restored.viewportWidthPx).isEqualTo(current.viewportWidthPx)
        assertThat(restored.viewportHeightPx).isEqualTo(current.viewportHeightPx)
    }
}
