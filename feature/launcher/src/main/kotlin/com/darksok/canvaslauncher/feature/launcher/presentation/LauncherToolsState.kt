package com.darksok.canvaslauncher.feature.launcher.presentation

import android.graphics.Bitmap

enum class LauncherToolId {
    Search,
    AppsList,
    Edit,
    Widgets,
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

data class WidgetCatalogItemUiState(
    val id: String,
    val title: String,
    val subtitle: String,
    val widgetType: CanvasWidgetType,
)

data class WidgetsUiState(
    val items: List<WidgetCatalogItemUiState> = listOf(
        WidgetCatalogItemUiState(
            id = "clock-digital",
            title = "Clock",
            subtitle = "Digital, no seconds",
            widgetType = CanvasWidgetType.ClockDigital,
        ),
    ),
)

data class ToolsUiState(
    val isExpanded: Boolean = false,
    val activeTool: LauncherToolId? = null,
    val tools: List<LauncherToolModel> = listOf(
        LauncherToolModel(id = LauncherToolId.Search),
        LauncherToolModel(id = LauncherToolId.AppsList),
        LauncherToolModel(id = LauncherToolId.Edit),
        LauncherToolModel(id = LauncherToolId.Widgets),
        LauncherToolModel(id = LauncherToolId.Settings),
    ),
    val search: SearchUiState = SearchUiState(),
    val appsList: AppsListUiState = AppsListUiState(),
    val edit: EditUiState = EditUiState(),
    val widgets: WidgetsUiState = WidgetsUiState(),
) {
    val isSearchActive: Boolean
        get() = activeTool == LauncherToolId.Search

    val isAppsListActive: Boolean
        get() = activeTool == LauncherToolId.AppsList

    val isEditActive: Boolean
        get() = activeTool == LauncherToolId.Edit

    val isWidgetsActive: Boolean
        get() = activeTool == LauncherToolId.Widgets
}
