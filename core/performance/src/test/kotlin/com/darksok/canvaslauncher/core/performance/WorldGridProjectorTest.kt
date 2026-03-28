package com.darksok.canvaslauncher.core.performance

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorldGridProjectorTest {

    @Test
    fun `positive modulo handles negative values`() {
        assertThat(WorldGridProjector.positiveModulo(-10f, 6f)).isWithin(0.0001f).of(2f)
        assertThat(WorldGridProjector.positiveModulo(14f, 6f)).isWithin(0.0001f).of(2f)
    }

    @Test
    fun `positive modulo returns zero for non-positive divisor`() {
        assertThat(WorldGridProjector.positiveModulo(10f, 0f)).isEqualTo(0f)
        assertThat(WorldGridProjector.positiveModulo(10f, -5f)).isEqualTo(0f)
    }

    @Test
    fun `anchored origin remains in one cell when camera moves by grid period`() {
        val scale = 1.5f
        val spacingPx = 120f
        val worldPeriod = spacingPx / scale

        val first = WorldGridProjector.anchoredOrigin(
            worldCenter = WorldPoint(0f, 0f),
            scale = scale,
            viewportWidthPx = 1080f,
            viewportHeightPx = 1920f,
            spacingPx = spacingPx,
        )
        val shifted = WorldGridProjector.anchoredOrigin(
            worldCenter = WorldPoint(worldPeriod, worldPeriod),
            scale = scale,
            viewportWidthPx = 1080f,
            viewportHeightPx = 1920f,
            spacingPx = spacingPx,
        )

        assertThat(shifted.x).isWithin(0.0001f).of(first.x)
        assertThat(shifted.y).isWithin(0.0001f).of(first.y)
    }
}
