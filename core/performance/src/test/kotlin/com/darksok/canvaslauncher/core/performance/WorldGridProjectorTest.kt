package com.darksok.canvaslauncher.core.performance

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorldGridProjectorTest {

    @Test
    fun `positive modulo handles negative values`() {
        assertThat(WorldGridProjector.positiveModulo(-10f, 6f)).isWithin(0.0001f).of(2f)
    }

    @Test
    fun `positive modulo handles positive overflow`() {
        assertThat(WorldGridProjector.positiveModulo(14f, 6f)).isWithin(0.0001f).of(2f)
    }

    @Test
    fun `positive modulo returns zero for zero divisor`() {
        assertThat(WorldGridProjector.positiveModulo(10f, 0f)).isEqualTo(0f)
    }

    @Test
    fun `positive modulo returns zero for negative divisor`() {
        assertThat(WorldGridProjector.positiveModulo(10f, -5f)).isEqualTo(0f)
    }

    @Test
    fun `positive modulo preserves exact multiple as zero`() {
        assertThat(WorldGridProjector.positiveModulo(18f, 6f)).isEqualTo(0f)
    }

    @Test
    fun `positive modulo preserves value already in range`() {
        assertThat(WorldGridProjector.positiveModulo(3f, 6f)).isEqualTo(3f)
    }

    @Test
    fun `positive modulo handles fractional inputs`() {
        assertThat(WorldGridProjector.positiveModulo(-2.5f, 4f)).isWithin(0.0001f).of(1.5f)
    }

    @Test
    fun `anchored origin remains in one cell when camera moves by grid period`() {
        val scale = 1.5f
        val spacingPx = 120f
        val worldPeriod = spacingPx / scale
        val first = WorldGridProjector.anchoredOrigin(WorldPoint(0f, 0f), scale, 1080f, 1920f, spacingPx)
        val shifted = WorldGridProjector.anchoredOrigin(WorldPoint(worldPeriod, worldPeriod), scale, 1080f, 1920f, spacingPx)
        assertThat(shifted.x).isWithin(0.0001f).of(first.x)
        assertThat(shifted.y).isWithin(0.0001f).of(first.y)
    }

    @Test
    fun `anchored origin x stays inside one spacing cell`() {
        val origin = WorldGridProjector.anchoredOrigin(WorldPoint(123f, -456f), 2f, 1080f, 1920f, 120f)
        assertThat(origin.x).isAtLeast(0f)
        assertThat(origin.x).isLessThan(120f)
    }

    @Test
    fun `anchored origin y stays inside one spacing cell`() {
        val origin = WorldGridProjector.anchoredOrigin(WorldPoint(123f, -456f), 2f, 1080f, 1920f, 120f)
        assertThat(origin.y).isAtLeast(0f)
        assertThat(origin.y).isLessThan(120f)
    }

    @Test
    fun `anchored origin responds independently per axis`() {
        val first = WorldGridProjector.anchoredOrigin(WorldPoint(0f, 0f), 1f, 100f, 100f, 20f)
        val movedX = WorldGridProjector.anchoredOrigin(WorldPoint(5f, 0f), 1f, 100f, 100f, 20f)
        assertThat(movedX.x).isNotEqualTo(first.x)
        assertThat(movedX.y).isEqualTo(first.y)
    }
}
