package com.darksok.canvaslauncher.core.model.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppLayoutModeTest {

    @Test
    fun `contains spiral mode`() {
        assertThat(AppLayoutMode.values()).asList().contains(AppLayoutMode.SPIRAL)
    }

    @Test
    fun `contains rectangle mode`() {
        assertThat(AppLayoutMode.values()).asList().contains(AppLayoutMode.RECTANGLE)
    }

    @Test
    fun `contains circle mode`() {
        assertThat(AppLayoutMode.values()).asList().contains(AppLayoutMode.CIRCLE)
    }

    @Test
    fun `contains oval mode`() {
        assertThat(AppLayoutMode.values()).asList().contains(AppLayoutMode.OVAL)
    }

    @Test
    fun `contains smart auto mode`() {
        assertThat(AppLayoutMode.values()).asList().contains(AppLayoutMode.SMART_AUTO)
    }

    @Test
    fun `contains icon color mode`() {
        assertThat(AppLayoutMode.values()).asList().contains(AppLayoutMode.ICON_COLOR)
    }

    @Test
    fun `layout mode names are stable`() {
        assertThat(AppLayoutMode.values().map { it.name }).containsExactly(
            "SPIRAL",
            "RECTANGLE",
            "CIRCLE",
            "OVAL",
            "SMART_AUTO",
            "ICON_COLOR",
        ).inOrder()
    }

    @Test
    fun `layout mode entries mirror values order`() {
        assertThat(AppLayoutMode.entries.map { it.name }).containsExactly(
            "SPIRAL",
            "RECTANGLE",
            "CIRCLE",
            "OVAL",
            "SMART_AUTO",
            "ICON_COLOR",
        ).inOrder()
    }
}
