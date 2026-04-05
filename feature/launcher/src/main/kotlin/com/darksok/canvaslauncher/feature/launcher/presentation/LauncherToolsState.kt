package com.darksok.canvaslauncher.feature.launcher.presentation

import android.graphics.Bitmap
import androidx.annotation.StringRes
import com.darksok.canvaslauncher.feature.launcher.R

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
    val topContactLabel: String? = null,
    val topContactDialNumber: String? = null,
    val showCallContactAction: Boolean = false,
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
    @StringRes val titleResId: Int,
    @StringRes val subtitleResId: Int,
    val widgetType: CanvasWidgetType,
)

data class WidgetsUiState(
    val items: List<WidgetCatalogItemUiState> = listOf(
        WidgetCatalogItemUiState(
            id = "clock-digital",
            titleResId = R.string.widget_catalog_clock_digital_title,
            subtitleResId = R.string.widget_catalog_clock_digital_subtitle,
            widgetType = CanvasWidgetType.ClockDigital,
        ),
        WidgetCatalogItemUiState(
            id = "clock-analog",
            titleResId = R.string.widget_catalog_clock_analog_title,
            subtitleResId = R.string.widget_catalog_clock_analog_subtitle,
            widgetType = CanvasWidgetType.ClockAnalog,
        ),
        WidgetCatalogItemUiState(
            id = "weather",
            titleResId = R.string.widget_catalog_weather_title,
            subtitleResId = R.string.widget_catalog_weather_subtitle,
            widgetType = CanvasWidgetType.Weather,
        ),
        WidgetCatalogItemUiState(
            id = "notifications",
            titleResId = R.string.widget_catalog_notifications_title,
            subtitleResId = R.string.widget_catalog_notifications_subtitle,
            widgetType = CanvasWidgetType.Notifications,
        ),
        WidgetCatalogItemUiState(
            id = "calendar",
            titleResId = R.string.widget_catalog_calendar_title,
            subtitleResId = R.string.widget_catalog_calendar_subtitle,
            widgetType = CanvasWidgetType.Calendar,
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
