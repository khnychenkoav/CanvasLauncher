package com.darksok.canvaslauncher.settings

import com.darksok.canvaslauncher.R
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SettingsLayoutAlgorithmsTest {

    @Test
    fun `classify icon color returns monochrome for empty dimensions`() {
        assertThat(SettingsLayoutAlgorithms.classifyIconColor(width = 0, height = 0) { _, _ -> 0 }).isEqualTo(IconColorGroup.MONOCHROME)
    }

    @Test
    fun `classify icon color returns red for red sample`() {
        assertThat(classifySolidColor(hue = 0f, saturation = 0.9f, value = 0.9f)).isEqualTo(IconColorGroup.RED)
    }

    @Test
    fun `classify icon color returns orange for orange sample`() {
        assertThat(classifySolidColor(hue = 24f, saturation = 0.9f, value = 0.9f)).isEqualTo(IconColorGroup.ORANGE)
    }

    @Test
    fun `classify icon color returns yellow for yellow sample`() {
        assertThat(classifySolidColor(hue = 52f, saturation = 0.9f, value = 0.95f)).isEqualTo(IconColorGroup.YELLOW)
    }

    @Test
    fun `classify icon color returns green for green sample`() {
        assertThat(classifySolidColor(hue = 120f, saturation = 0.9f, value = 0.9f)).isEqualTo(IconColorGroup.GREEN)
    }

    @Test
    fun `classify icon color returns cyan for cyan sample`() {
        assertThat(classifySolidColor(hue = 180f, saturation = 0.9f, value = 0.9f)).isEqualTo(IconColorGroup.CYAN)
    }

    @Test
    fun `classify icon color returns blue for blue sample`() {
        assertThat(classifySolidColor(hue = 220f, saturation = 0.9f, value = 0.9f)).isEqualTo(IconColorGroup.BLUE)
    }

    @Test
    fun `classify icon color returns purple for purple sample`() {
        assertThat(classifySolidColor(hue = 280f, saturation = 0.9f, value = 0.9f)).isEqualTo(IconColorGroup.PURPLE)
    }

    @Test
    fun `classify icon color returns pink for pink sample`() {
        assertThat(classifySolidColor(hue = 320f, saturation = 0.9f, value = 0.95f)).isEqualTo(IconColorGroup.PINK)
    }

    @Test
    fun `classify icon color returns brown for dark orange sample`() {
        assertThat(classifySolidColor(hue = 30f, saturation = 0.8f, value = 0.4f)).isEqualTo(IconColorGroup.BROWN)
    }

    @Test
    fun `classify icon color treats low saturation as monochrome`() {
        assertThat(classifySolidColor(hue = 180f, saturation = 0.05f, value = 0.95f)).isEqualTo(IconColorGroup.MONOCHROME)
    }

    @Test
    fun `classify icon color treats low value as monochrome`() {
        assertThat(classifySolidColor(hue = 180f, saturation = 0.9f, value = 0.1f)).isEqualTo(IconColorGroup.MONOCHROME)
    }

    @Test
    fun `stable slug normalizes separators and case`() {
        assertThat(SettingsLayoutAlgorithms.stableSlug(" Smart Group 42 ")).isEqualTo("smart-group-42")
    }

    @Test
    fun `stable slug falls back to hash for punctuation only`() {
        val slug = SettingsLayoutAlgorithms.stableSlug("!!!")

        assertThat(slug).isNotEmpty()
        assertThat(slug).doesNotContain("-")
    }

    @Test
    fun `compute grouped positions returns empty map for empty groups`() {
        assertThat(SettingsLayoutAlgorithms.computeGroupedPositions(emptyList(), WorldPoint(0f, 0f))).isEmpty()
    }

    @Test
    fun `compute grouped positions preserves every package name`() {
        val positions = SettingsLayoutAlgorithms.computeGroupedPositions(
            groups = listOf(
                listOf(
                    CanvasApp("pkg.one", "One", WorldPoint(0f, 0f)),
                    CanvasApp("pkg.two", "Two", WorldPoint(0f, 0f)),
                ),
                listOf(CanvasApp("pkg.three", "Three", WorldPoint(0f, 0f))),
            ),
            center = WorldPoint(100f, -50f),
        )

        assertThat(positions.keys).containsExactly("pkg.one", "pkg.two", "pkg.three")
    }

    @Test
    fun `compute grouped positions offsets groups around provided center`() {
        val positions = SettingsLayoutAlgorithms.computeGroupedPositions(
            groups = listOf(listOf(CanvasApp("pkg.one", "One", WorldPoint(0f, 0f)))),
            center = WorldPoint(300f, 400f),
        )

        assertThat(positions.getValue("pkg.one").x).isWithin(0.001f).of(232f)
        assertThat(positions.getValue("pkg.one").y).isWithin(0.001f).of(400f)
    }

    @Test
    fun `smart group title resource id resolves known id`() {
        assertThat(SettingsLayoutAlgorithms.smartGroupTitleResId("travel")).isEqualTo(R.string.smart_group_travel)
    }

    @Test
    fun `smart group title resource id returns null for unknown id`() {
        assertThat(SettingsLayoutAlgorithms.smartGroupTitleResId("mystery")).isNull()
    }

    private fun classifySolidColor(
        hue: Float,
        saturation: Float,
        value: Float,
    ): IconColorGroup {
        val argb = hsvToArgb(hue, saturation, value)
        return SettingsLayoutAlgorithms.classifyIconColor(width = 24, height = 24) { _, _ -> argb }
    }

    private fun hsvToArgb(
        hue: Float,
        saturation: Float,
        value: Float,
    ): Int {
        val c = value * saturation
        val x = c * (1 - kotlin.math.abs((hue / 60f) % 2 - 1))
        val m = value - c
        val (r1, g1, b1) = when {
            hue < 60f -> Triple(c, x, 0f)
            hue < 120f -> Triple(x, c, 0f)
            hue < 180f -> Triple(0f, c, x)
            hue < 240f -> Triple(0f, x, c)
            hue < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val r = ((r1 + m) * 255).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)
        return (255 shl 24) or (r shl 16) or (g shl 8) or b
    }
}
