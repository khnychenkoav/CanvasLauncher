package com.darksok.canvaslauncher.feature.apps.data

import com.darksok.canvaslauncher.core.database.entity.AppEntity
import com.darksok.canvaslauncher.core.model.app.CanvasApp
import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppMappersTest {

    @Test
    fun `entity maps package name to domain`() {
        val entity = AppEntity("pkg.alpha", "Alpha", 1f, 2f)

        assertThat(entity.toDomain().packageName).isEqualTo("pkg.alpha")
    }

    @Test
    fun `entity maps label to domain`() {
        val entity = AppEntity("pkg.alpha", "Alpha", 1f, 2f)

        assertThat(entity.toDomain().label).isEqualTo("Alpha")
    }

    @Test
    fun `entity maps x coordinate to world point`() {
        val entity = AppEntity("pkg.alpha", "Alpha", -12.5f, 2f)

        assertThat(entity.toDomain().position.x).isEqualTo(-12.5f)
    }

    @Test
    fun `entity maps y coordinate to world point`() {
        val entity = AppEntity("pkg.alpha", "Alpha", 1f, 999.25f)

        assertThat(entity.toDomain().position.y).isEqualTo(999.25f)
    }

    @Test
    fun `domain maps package name to entity`() {
        val app = CanvasApp("pkg.beta", "Beta", WorldPoint(3f, 4f))

        assertThat(app.toEntity().packageName).isEqualTo("pkg.beta")
    }

    @Test
    fun `domain maps label to entity`() {
        val app = CanvasApp("pkg.beta", "Beta Label", WorldPoint(3f, 4f))

        assertThat(app.toEntity().label).isEqualTo("Beta Label")
    }

    @Test
    fun `domain maps x coordinate to entity`() {
        val app = CanvasApp("pkg.beta", "Beta", WorldPoint(Float.MIN_VALUE, 4f))

        assertThat(app.toEntity().x).isEqualTo(Float.MIN_VALUE)
    }

    @Test
    fun `domain maps y coordinate to entity`() {
        val app = CanvasApp("pkg.beta", "Beta", WorldPoint(3f, Float.MAX_VALUE))

        assertThat(app.toEntity().y).isEqualTo(Float.MAX_VALUE)
    }

    @Test
    fun `entity round trips to equal domain`() {
        val entity = AppEntity("pkg.gamma", "Gamma", -10.5f, 87.125f)

        assertThat(entity.toDomain().toEntity()).isEqualTo(entity)
    }

    @Test
    fun `domain round trips to equal entity`() {
        val app = CanvasApp("pkg.delta", "Delta", WorldPoint(-88f, 0.5f))

        assertThat(app.toEntity().toDomain()).isEqualTo(app)
    }

    @Test
    fun `mapper preserves empty label`() {
        val entity = AppEntity("pkg.empty", "", 0f, 0f)

        assertThat(entity.toDomain().label).isEmpty()
    }

    @Test
    fun `mapper preserves unicode label`() {
        val entity = AppEntity("pkg.unicode", "Привет", 0f, 0f)

        assertThat(entity.toDomain().label).isEqualTo("Привет")
    }
}
