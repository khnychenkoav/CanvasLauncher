package com.darksok.canvaslauncher.domain.layout

import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.app.InstalledApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InitialLayoutStrategyDefaultsTest {

    @Test
    fun `default center and mode are applied when omitted`() {
        val strategy: InitialLayoutStrategy = RecordingStrategy()

        strategy.layout(
            existingApps = emptyList(),
            newApps = emptyList(),
        )

        val recording = strategy as RecordingStrategy
        assertThat(recording.recordedCenter).isEqualTo(WorldPoint(0f, 0f))
        assertThat(recording.recordedMode).isEqualTo(AppLayoutMode.SMART_AUTO)
    }

    private class RecordingStrategy : InitialLayoutStrategy {
        var recordedCenter: WorldPoint? = null
        var recordedMode: AppLayoutMode? = null

        override fun layout(
            existingApps: List<CanvasApp>,
            newApps: List<InstalledApp>,
            center: WorldPoint,
            mode: AppLayoutMode,
        ): List<CanvasApp> {
            recordedCenter = center
            recordedMode = mode
            return emptyList()
        }
    }
}
