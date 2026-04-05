package com.darksok.canvaslauncher.feature.launcher.presentation

import com.darksok.canvaslauncher.core.model.canvas.WorldPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CanvasEditStateTest {

    @Test
    fun `selection is empty by default`() {
        assertThat(CanvasSelectionUiState().isEmpty).isTrue()
    }

    @Test
    fun `selection has icons when package names present`() {
        assertThat(CanvasSelectionUiState(packageNames = setOf("pkg")).hasIcons).isTrue()
    }

    @Test
    fun `selection is not empty when frame ids present`() {
        assertThat(CanvasSelectionUiState(frameIds = setOf("frame")).isEmpty).isFalse()
    }

    @Test
    fun `selection is not empty when sticky ids present`() {
        assertThat(CanvasSelectionUiState(stickyIds = setOf("sticky")).isEmpty).isFalse()
    }

    @Test
    fun `selection is not empty when text ids present`() {
        assertThat(CanvasSelectionUiState(textIds = setOf("text")).isEmpty).isFalse()
    }

    @Test
    fun `selection is not empty when stroke ids present`() {
        assertThat(CanvasSelectionUiState(strokeIds = setOf("stroke")).isEmpty).isFalse()
    }

    @Test
    fun `selection bounds contains point inside rectangle`() {
        val bounds = CanvasSelectionBoundsUiState(0f, 0f, 10f, 20f, hasIcons = true, canResize = true, canDelete = true)

        assertThat(bounds.contains(WorldPoint(5f, 5f))).isTrue()
    }

    @Test
    fun `selection bounds contains left top edge`() {
        val bounds = CanvasSelectionBoundsUiState(0f, 0f, 10f, 20f, hasIcons = true, canResize = true, canDelete = true)

        assertThat(bounds.contains(WorldPoint(0f, 0f))).isTrue()
    }

    @Test
    fun `selection bounds contains right bottom edge`() {
        val bounds = CanvasSelectionBoundsUiState(0f, 0f, 10f, 20f, hasIcons = true, canResize = true, canDelete = true)

        assertThat(bounds.contains(WorldPoint(10f, 20f))).isTrue()
    }

    @Test
    fun `selection bounds rejects point outside rectangle`() {
        val bounds = CanvasSelectionBoundsUiState(0f, 0f, 10f, 20f, hasIcons = true, canResize = true, canDelete = true)

        assertThat(bounds.contains(WorldPoint(10.1f, 20f))).isFalse()
    }

    @Test
    fun `edit ui state defaults to move tool`() {
        assertThat(EditUiState().selectedTool).isEqualTo(CanvasEditToolId.Move)
    }

    @Test
    fun `inline editor is hidden by default`() {
        assertThat(CanvasInlineEditorUiState().isVisible).isFalse()
    }

    @Test
    fun `inline editor target defaults to none`() {
        assertThat(CanvasInlineEditorUiState().target).isEqualTo(CanvasInlineEditorTarget.None)
    }

    @Test
    fun `default widget ui state color matches default green`() {
        val widget = CanvasWidgetUiState(
            id = "widget",
            type = CanvasWidgetType.ClockDigital,
            center = WorldPoint(0f, 0f),
            widthWorld = 100f,
            heightWorld = 50f,
        )

        assertThat(widget.colorArgb).isEqualTo(0xFF72E38C.toInt())
    }

    @Test
    fun `widget catalog defaults expose four items in expected order`() {
        val items = WidgetsUiState().items.map { it.widgetType }

        assertThat(items).containsExactly(
            CanvasWidgetType.ClockDigital,
            CanvasWidgetType.ClockAnalog,
            CanvasWidgetType.Weather,
            CanvasWidgetType.Notifications,
        ).inOrder()
    }

    @Test
    fun `widget catalog item ids are unique`() {
        val ids = WidgetsUiState().items.map { it.id }

        assertThat(ids.distinct()).hasSize(ids.size)
    }

    @Test
    fun `tools state search flag follows active tool`() {
        assertThat(ToolsUiState(activeTool = LauncherToolId.Search).isSearchActive).isTrue()
    }

    @Test
    fun `tools state apps list flag follows active tool`() {
        assertThat(ToolsUiState(activeTool = LauncherToolId.AppsList).isAppsListActive).isTrue()
    }

    @Test
    fun `tools state edit flag follows active tool`() {
        assertThat(ToolsUiState(activeTool = LauncherToolId.Edit).isEditActive).isTrue()
    }

    @Test
    fun `tools state widgets flag follows active tool`() {
        assertThat(ToolsUiState(activeTool = LauncherToolId.Widgets).isWidgetsActive).isTrue()
    }
}
