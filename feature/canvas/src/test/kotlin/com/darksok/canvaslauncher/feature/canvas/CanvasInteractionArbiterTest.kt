package com.darksok.canvaslauncher.feature.canvas

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CanvasInteractionArbiterTest {

    @Test
    fun `multi touch suppresses tap while pointers active and shortly after`() {
        val arbiter = CanvasInteractionArbiter()

        arbiter.onPointerCountChanged(pressedPointerCount = 2, eventUptimeMs = 1_000L)
        assertThat(arbiter.shouldSuppressTap(nowUptimeMs = 1_050L)).isTrue()

        arbiter.onPointerCountChanged(pressedPointerCount = 0, eventUptimeMs = 1_120L)
        assertThat(arbiter.shouldSuppressTap(nowUptimeMs = 1_180L)).isTrue()
        assertThat(arbiter.shouldSuppressTap(nowUptimeMs = 1_230L)).isFalse()
    }

    @Test
    fun `transform event extends suppression even for single pointer`() {
        val arbiter = CanvasInteractionArbiter()

        arbiter.onPointerCountChanged(pressedPointerCount = 1, eventUptimeMs = 2_000L)
        arbiter.onCanvasTransformDetected(eventUptimeMs = 2_040L)

        assertThat(arbiter.shouldSuppressTap(nowUptimeMs = 2_150L)).isTrue()
        assertThat(arbiter.shouldSuppressTap(nowUptimeMs = 2_300L)).isFalse()
    }
}
