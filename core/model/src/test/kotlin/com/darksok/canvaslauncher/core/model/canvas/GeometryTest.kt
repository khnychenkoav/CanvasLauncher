package com.darksok.canvaslauncher.core.model.canvas

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeometryTest {

    @Test
    fun `world rect contains point strictly inside`() {
        val rect = WorldRect(-10f, -5f, 20f, 15f)
        assertThat(rect.contains(WorldPoint(0f, 0f))).isTrue()
    }

    @Test
    fun `world rect contains point on left boundary`() {
        val rect = WorldRect(-10f, -5f, 20f, 15f)
        assertThat(rect.contains(WorldPoint(-10f, 0f))).isTrue()
    }

    @Test
    fun `world rect contains point on right boundary`() {
        val rect = WorldRect(-10f, -5f, 20f, 15f)
        assertThat(rect.contains(WorldPoint(20f, 0f))).isTrue()
    }

    @Test
    fun `world rect contains point on top boundary`() {
        val rect = WorldRect(-10f, -5f, 20f, 15f)
        assertThat(rect.contains(WorldPoint(0f, -5f))).isTrue()
    }

    @Test
    fun `world rect contains point on bottom boundary`() {
        val rect = WorldRect(-10f, -5f, 20f, 15f)
        assertThat(rect.contains(WorldPoint(0f, 15f))).isTrue()
    }

    @Test
    fun `world rect excludes point left of boundary`() {
        val rect = WorldRect(-10f, -5f, 20f, 15f)
        assertThat(rect.contains(WorldPoint(-10.01f, 0f))).isFalse()
    }

    @Test
    fun `world rect excludes point right of boundary`() {
        val rect = WorldRect(-10f, -5f, 20f, 15f)
        assertThat(rect.contains(WorldPoint(20.01f, 0f))).isFalse()
    }

    @Test
    fun `world rect excludes point above boundary`() {
        val rect = WorldRect(-10f, -5f, 20f, 15f)
        assertThat(rect.contains(WorldPoint(0f, -5.01f))).isFalse()
    }

    @Test
    fun `world rect excludes point below boundary`() {
        val rect = WorldRect(-10f, -5f, 20f, 15f)
        assertThat(rect.contains(WorldPoint(0f, 15.01f))).isFalse()
    }

    @Test
    fun `world rect contains matching point in zero size rect`() {
        val rect = WorldRect(2f, 2f, 2f, 2f)
        assertThat(rect.contains(WorldPoint(2f, 2f))).isTrue()
    }

    @Test
    fun `world rect excludes non matching point in zero size rect`() {
        val rect = WorldRect(2f, 2f, 2f, 2f)
        assertThat(rect.contains(WorldPoint(2f, 3f))).isFalse()
    }

    @Test
    fun `world point equality is coordinate based`() {
        assertThat(WorldPoint(1f, 2f)).isEqualTo(WorldPoint(1f, 2f))
    }

    @Test
    fun `screen point equality is coordinate based`() {
        assertThat(ScreenPoint(3f, 4f)).isEqualTo(ScreenPoint(3f, 4f))
    }

    @Test
    fun `world point copy can replace x only`() {
        val point = WorldPoint(1f, 2f)
        val copy = point.copy(x = 9f)
        assertThat(copy).isEqualTo(WorldPoint(9f, 2f))
    }

    @Test
    fun `screen point copy can replace y only`() {
        val point = ScreenPoint(1f, 2f)
        val copy = point.copy(y = 9f)
        assertThat(copy).isEqualTo(ScreenPoint(1f, 9f))
    }
}
