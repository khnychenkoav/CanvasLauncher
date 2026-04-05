package com.darksok.canvaslauncher.core.model.app

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppModelsTest {

    @Test
    fun `installed app stores package name`() {
        val app = InstalledApp(packageName = "pkg.one", label = "One")
        assertThat(app.packageName).isEqualTo("pkg.one")
    }

    @Test
    fun `installed app stores label`() {
        val app = InstalledApp(packageName = "pkg.one", label = "One")
        assertThat(app.label).isEqualTo("One")
    }

    @Test
    fun `installed app equality is based on fields`() {
        assertThat(InstalledApp("pkg", "Label")).isEqualTo(InstalledApp("pkg", "Label"))
    }

    @Test
    fun `installed app copy can change label independently`() {
        val original = InstalledApp("pkg", "Old")
        val copy = original.copy(label = "New")
        assertThat(copy.label).isEqualTo("New")
        assertThat(copy.packageName).isEqualTo("pkg")
    }

    @Test
    fun `canvas app stores package name`() {
        val app = CanvasApp("pkg.one", "One", WorldPoint(1f, 2f))
        assertThat(app.packageName).isEqualTo("pkg.one")
    }

    @Test
    fun `canvas app stores label`() {
        val app = CanvasApp("pkg.one", "One", WorldPoint(1f, 2f))
        assertThat(app.label).isEqualTo("One")
    }

    @Test
    fun `canvas app stores position`() {
        val position = WorldPoint(1f, 2f)
        val app = CanvasApp("pkg.one", "One", position)
        assertThat(app.position).isEqualTo(position)
    }

    @Test
    fun `canvas app equality is based on all fields`() {
        assertThat(CanvasApp("pkg", "Label", WorldPoint(1f, 2f)))
            .isEqualTo(CanvasApp("pkg", "Label", WorldPoint(1f, 2f)))
    }

    @Test
    fun `canvas app copy can change position without affecting package`() {
        val original = CanvasApp("pkg", "Label", WorldPoint(1f, 2f))
        val copy = original.copy(position = WorldPoint(5f, 8f))
        assertThat(copy.packageName).isEqualTo("pkg")
        assertThat(copy.position).isEqualTo(WorldPoint(5f, 8f))
    }
}
