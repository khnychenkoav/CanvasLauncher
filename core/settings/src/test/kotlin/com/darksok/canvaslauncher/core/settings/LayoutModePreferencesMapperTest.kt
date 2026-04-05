package com.darksok.canvaslauncher.core.settings

import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LayoutModePreferencesMapperTest {

    @Test
    fun `stored null value resolves to spiral`() {
        assertThat((null as String?).toLayoutModeOrDefault()).isEqualTo(AppLayoutMode.SPIRAL)
    }

    @Test
    fun `stored invalid value resolves to spiral`() {
        assertThat("UNKNOWN".toLayoutModeOrDefault()).isEqualTo(AppLayoutMode.SPIRAL)
    }

    @Test
    fun `stored lowercase value does not resolve`() {
        assertThat("spiral".toLayoutModeOrDefault()).isEqualTo(AppLayoutMode.SPIRAL)
    }

    @Test
    fun `stored value with whitespace does not resolve`() {
        assertThat(" SPIRAL ".toLayoutModeOrDefault()).isEqualTo(AppLayoutMode.SPIRAL)
    }

    @Test
    fun `spiral value resolves correctly`() {
        assertThat("SPIRAL".toLayoutModeOrDefault()).isEqualTo(AppLayoutMode.SPIRAL)
    }

    @Test
    fun `rectangle value resolves correctly`() {
        assertThat("RECTANGLE".toLayoutModeOrDefault()).isEqualTo(AppLayoutMode.RECTANGLE)
    }

    @Test
    fun `circle value resolves correctly`() {
        assertThat("CIRCLE".toLayoutModeOrDefault()).isEqualTo(AppLayoutMode.CIRCLE)
    }

    @Test
    fun `oval value resolves correctly`() {
        assertThat("OVAL".toLayoutModeOrDefault()).isEqualTo(AppLayoutMode.OVAL)
    }

    @Test
    fun `smart auto value resolves correctly`() {
        assertThat("SMART_AUTO".toLayoutModeOrDefault()).isEqualTo(AppLayoutMode.SMART_AUTO)
    }

    @Test
    fun `icon color value resolves correctly`() {
        assertThat("ICON_COLOR".toLayoutModeOrDefault()).isEqualTo(AppLayoutMode.ICON_COLOR)
    }

    @Test
    fun `spiral serializes to enum name`() {
        assertThat(AppLayoutMode.SPIRAL.toStoredValue()).isEqualTo("SPIRAL")
    }

    @Test
    fun `rectangle serializes to enum name`() {
        assertThat(AppLayoutMode.RECTANGLE.toStoredValue()).isEqualTo("RECTANGLE")
    }

    @Test
    fun `circle serializes to enum name`() {
        assertThat(AppLayoutMode.CIRCLE.toStoredValue()).isEqualTo("CIRCLE")
    }

    @Test
    fun `oval serializes to enum name`() {
        assertThat(AppLayoutMode.OVAL.toStoredValue()).isEqualTo("OVAL")
    }

    @Test
    fun `smart auto serializes to enum name`() {
        assertThat(AppLayoutMode.SMART_AUTO.toStoredValue()).isEqualTo("SMART_AUTO")
    }

    @Test
    fun `icon color serializes to enum name`() {
        assertThat(AppLayoutMode.ICON_COLOR.toStoredValue()).isEqualTo("ICON_COLOR")
    }
}
