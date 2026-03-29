package com.darksok.canvaslauncher.feature.launcher.presentation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LauncherToolsStateTest {

    @Test
    fun `search is active only when active tool is search`() {
        val inactive = ToolsUiState(activeTool = null)
        val active = ToolsUiState(activeTool = LauncherToolId.Search)

        assertThat(inactive.isSearchActive).isFalse()
        assertThat(active.isSearchActive).isTrue()
    }

    @Test
    fun `default tools list exposes search tool`() {
        val state = ToolsUiState()

        assertThat(state.tools.map { it.id }).containsExactly(
            LauncherToolId.Search,
            LauncherToolId.AppsList,
            LauncherToolId.Edit,
            LauncherToolId.Widgets,
            LauncherToolId.Settings,
        ).inOrder()
    }
}
