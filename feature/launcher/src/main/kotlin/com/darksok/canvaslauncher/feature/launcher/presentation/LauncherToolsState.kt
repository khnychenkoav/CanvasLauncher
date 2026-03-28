package com.darksok.canvaslauncher.feature.launcher.presentation

import android.graphics.Bitmap

enum class LauncherToolId {
    Search,
    AppsList,
    Edit,
    Settings,
}

data class LauncherToolModel(
    val id: LauncherToolId,
)

data class SearchUiState(
    val query: String = "",
    val suggestionLabel: String? = null,
    val topMatchPackageName: String? = null,
    val topMatchLabel: String? = null,
    val showLaunchAction: Boolean = false,
)

data class AppsListItemUiState(
    val packageName: String,
    val label: String,
    val icon: Bitmap?,
)

data class AppsListUiState(
    val query: String = "",
    val items: List<AppsListItemUiState> = emptyList(),
)

data class ToolsUiState(
    val isExpanded: Boolean = false,
    val activeTool: LauncherToolId? = null,
    val tools: List<LauncherToolModel> = listOf(
        LauncherToolModel(id = LauncherToolId.Search),
        LauncherToolModel(id = LauncherToolId.AppsList),
        LauncherToolModel(id = LauncherToolId.Edit),
        LauncherToolModel(id = LauncherToolId.Settings),
    ),
    val search: SearchUiState = SearchUiState(),
    val appsList: AppsListUiState = AppsListUiState(),
    val edit: EditUiState = EditUiState(),
) {
    val isSearchActive: Boolean
        get() = activeTool == LauncherToolId.Search

    val isAppsListActive: Boolean
        get() = activeTool == LauncherToolId.AppsList

    val isEditActive: Boolean
        get() = activeTool == LauncherToolId.Edit
}
