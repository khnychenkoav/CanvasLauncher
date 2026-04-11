package com.darksok.canvaslauncher.feature.launcher.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.darksok.canvaslauncher.feature.launcher.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.roundToInt

@Composable
fun LauncherToolsOverlay(
    toolsState: ToolsUiState,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    onToolsToggle: () -> Unit,
    onToolSelected: (LauncherToolId) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchActionClick: () -> Unit,
    onSearchSubmit: () -> Unit,
    onSearchOpenInBrowser: (String) -> Unit,
    onSearchCallContact: (String) -> Unit,
    onSearchLaunchTopMatch: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchOcclusionChanged: (Int) -> Unit,
    onSearchKeyboardVisibilityChanged: (Boolean) -> Unit,
    onEditClose: () -> Unit,
    onEditToolSelected: (CanvasEditToolId) -> Unit,
    onEditColorSelected: (Int) -> Unit,
    onEditBrushSizeStep: (Float) -> Unit,
    onEditTextSizeStep: (Float) -> Unit,
    onEditInlineEditorValueChanged: (String) -> Unit,
    onEditInlineEditorConfirm: () -> Unit,
    onEditInlineEditorCancel: () -> Unit,
    onEditUndo: () -> Unit,
    onEditClearCustomElements: () -> Unit,
    onWidgetsClose: () -> Unit,
    onWidgetCatalogItemSelected: (CanvasWidgetType) -> Unit,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val rootWindowInsets = ViewCompat.getRootWindowInsets(view)
    val imeBottomPx = rootWindowInsets
        ?.getInsets(WindowInsetsCompat.Type.ime())
        ?.bottom
        ?: 0
    val navigationBottomPx = rootWindowInsets
        ?.getInsets(WindowInsetsCompat.Type.navigationBars())
        ?.bottom
        ?: 0
    val rootHeightPx = view.rootView.height
    val currentHeightPx = view.height
    val resizedByImePx = (rootHeightPx - currentHeightPx).coerceAtLeast(0)
    val keyboardOverlapPx = (imeBottomPx - resizedByImePx).coerceAtLeast(0)
    val bottomLiftPx = if (imeBottomPx > 0) keyboardOverlapPx else navigationBottomPx
    val effectiveBottomLiftPx = if (toolsState.isSearchActive) bottomLiftPx else navigationBottomPx
    val bottomLiftDp = with(density) { effectiveBottomLiftPx.toDp() }
    val widgetsPanelHeightDp = with(density) { (rootHeightPx / 3f).toDp() }.coerceAtLeast(220.dp)
    val isKeyboardVisible = imeBottomPx > navigationBottomPx
    val keyboardController = LocalSoftwareKeyboardController.current
    var toolsContentHeightPx by remember { mutableStateOf(0) }
    LaunchedEffect(toolsState.isSearchActive, toolsContentHeightPx, effectiveBottomLiftPx) {
        val occlusion = if (toolsState.isSearchActive) {
            toolsContentHeightPx + effectiveBottomLiftPx
        } else {
            0
        }
        onSearchOcclusionChanged(occlusion)
    }
    LaunchedEffect(toolsState.isSearchActive, isKeyboardVisible) {
        onSearchKeyboardVisibilityChanged(toolsState.isSearchActive && isKeyboardVisible)
    }

    CompositionLocalProvider(LocalToolsHazeState provides hazeState) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = bottomLiftDp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val closeButtonSize = ToolPanelUiConstants.BUTTON_SIZE
            val panelMaxWidth = maxWidth
            val panelMaxHeight = maxHeight
            val searchFieldMaxWidth =
                (maxWidth - closeButtonSize - ToolPanelUiConstants.SEARCH_ROW_GAP - 2.dp).coerceAtLeast(180.dp)

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> toolsContentHeightPx = size.height },
        ) {
            val canLaunchTopApp = toolsState.search.showLaunchAction && toolsState.search.topMatchLabel != null
            val canCallTopContact = toolsState.search.showCallContactAction &&
                toolsState.search.topContactLabel != null &&
                toolsState.search.topContactDialNumber != null
            AnimatedVisibility(
                visible = toolsState.isSearchActive && (canLaunchTopApp || canCallTopContact),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (canLaunchTopApp) {
                        SearchActionButton(
                            text = stringResource(
                                id = R.string.search_launch_top_match,
                                toolsState.search.topMatchLabel.orEmpty(),
                            ),
                            onClick = onSearchLaunchTopMatch,
                        )
                    }
                    if (canCallTopContact) {
                        val callActionLabel = stringResource(id = R.string.search_call_button_label)
                        SearchActionButton(
                            text = stringResource(
                                id = R.string.search_call_contact,
                                callActionLabel,
                                toolsState.search.topContactLabel.orEmpty(),
                            ),
                            onClick = {
                                val number = toolsState.search.topContactDialNumber
                                if (!number.isNullOrBlank()) {
                                    onSearchCallContact(number)
                                }
                            },
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = toolsState.activeTool,
                label = "tools-content",
            ) { activeTool ->
                when (activeTool) {
                    LauncherToolId.Search -> {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            SearchInputPill(
                                query = toolsState.search.query,
                                suggestion = toolsState.search.suggestionLabel,
                                maxWidth = searchFieldMaxWidth,
                                onQueryChanged = onSearchQueryChanged,
                                onSearchClick = onSearchActionClick,
                                onBrowserSearchClick = onSearchOpenInBrowser,
                                onSubmit = onSearchSubmit,
                            )
                            ToolCircleButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onSearchClose()
                                },
                                modifier = Modifier
                                    .padding(start = ToolPanelUiConstants.SEARCH_ROW_GAP)
                                    .size(ToolPanelUiConstants.BUTTON_SIZE),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }

                    LauncherToolId.Edit -> {
                        EditPanelContent(
                            editState = toolsState.edit,
                            maxWidth = panelMaxWidth,
                            onToolSelected = onEditToolSelected,
                            onColorSelected = onEditColorSelected,
                            onBrushSizeStep = onEditBrushSizeStep,
                            onTextSizeStep = onEditTextSizeStep,
                            onInlineEditorValueChanged = onEditInlineEditorValueChanged,
                            onInlineEditorConfirm = onEditInlineEditorConfirm,
                            onInlineEditorCancel = onEditInlineEditorCancel,
                            onUndo = onEditUndo,
                            onClearCustomElements = onEditClearCustomElements,
                            onClose = onEditClose,
                        )
                    }

                    LauncherToolId.Widgets -> {
                        WidgetsPanelContent(
                            widgetsState = toolsState.widgets,
                            panelHeight = widgetsPanelHeightDp,
                            onCatalogItemSelected = onWidgetCatalogItemSelected,
                            onClose = onWidgetsClose,
                        )
                    }

                    else -> {
                        val quickTools = toolsState.tools.filterNot { it.id == LauncherToolId.Search }
                        val quickToolsRows = quickTools.chunked(2)
                        val singleLayerQuickToolsHeight =
                            (ToolPanelUiConstants.BUTTON_SIZE * quickTools.size) +
                                (ToolPanelUiConstants.ITEM_GAP * (quickTools.size - 1).coerceAtLeast(0))
                        val twoLayerQuickToolsHeight =
                            (ToolPanelUiConstants.BUTTON_SIZE * quickToolsRows.size) +
                                (ToolPanelUiConstants.ITEM_GAP * (quickToolsRows.size - 1).coerceAtLeast(0))
                        val panelControlsBaseHeight = (ToolPanelUiConstants.BUTTON_SIZE * 2) +
                            (ToolPanelUiConstants.ITEM_GAP * 2)
                        val useTwoLayerQuickTools = toolsState.isExpanded &&
                            quickTools.size > 2 &&
                            panelControlsBaseHeight + singleLayerQuickToolsHeight > panelMaxHeight &&
                            panelControlsBaseHeight + twoLayerQuickToolsHeight <= panelMaxHeight
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(ToolPanelUiConstants.ITEM_GAP),
                        ) {
                            ToolCircleButton(
                                onClick = { onToolSelected(LauncherToolId.Search) },
                                modifier = Modifier.size(ToolPanelUiConstants.BUTTON_SIZE),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }

                            AnimatedVisibility(
                                visible = toolsState.isExpanded,
                                enter = fadeIn() +
                                    slideInVertically(initialOffsetY = { it / 2 }) +
                                    expandVertically(expandFrom = Alignment.Bottom),
                                exit = fadeOut() +
                                    slideOutVertically(targetOffsetY = { it / 2 }) +
                                    shrinkVertically(shrinkTowards = Alignment.Bottom),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(ToolPanelUiConstants.ITEM_GAP),
                                ) {
                                    if (useTwoLayerQuickTools) {
                                        quickToolsRows.forEach { toolsRow ->
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                toolsRow.forEach { tool ->
                                                    ToolCircleButton(
                                                        onClick = { onToolSelected(tool.id) },
                                                        modifier = Modifier.size(ToolPanelUiConstants.BUTTON_SIZE),
                                                    ) {
                                                        when (tool.id) {
                                                            LauncherToolId.AppsList -> {
                                                                Icon(
                                                                    imageVector = Icons.AutoMirrored.Rounded.ViewList,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                )
                                                            }

                                                            LauncherToolId.Edit -> {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Edit,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                )
                                                            }

                                                            LauncherToolId.Settings -> {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Settings,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                )
                                                            }

                                                            LauncherToolId.Widgets -> {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Widgets,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                )
                                                            }

                                                            LauncherToolId.Search -> Unit
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        quickTools.forEach { tool ->
                                            ToolCircleButton(
                                                onClick = { onToolSelected(tool.id) },
                                                modifier = Modifier.size(ToolPanelUiConstants.BUTTON_SIZE),
                                            ) {
                                                when (tool.id) {
                                                    LauncherToolId.AppsList -> {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Rounded.ViewList,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        )
                                                    }

                                                    LauncherToolId.Edit -> {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Edit,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        )
                                                    }

                                                    LauncherToolId.Settings -> {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Settings,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        )
                                                    }

                                                    LauncherToolId.Widgets -> {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Widgets,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        )
                                                    }

                                                    LauncherToolId.Search -> Unit
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            ToolCircleButton(
                                onClick = onToolsToggle,
                                modifier = Modifier.size(ToolPanelUiConstants.BUTTON_SIZE),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Apps,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun WidgetsPanelContent(
    widgetsState: WidgetsUiState,
    panelHeight: androidx.compose.ui.unit.Dp,
    onCatalogItemSelected: (CanvasWidgetType) -> Unit,
    onClose: () -> Unit,
) {
    val panelShape = RoundedCornerShape(22.dp)
    Surface(
        shape = panelShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = panelHeight),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlassButtonBackground(
                shape = panelShape,
                tintColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                    alpha = ToolPanelUiConstants.GLASS_PANEL_TINT_ALPHA,
                ),
                blurRadius = ToolPanelUiConstants.GLASS_PANEL_BLUR_RADIUS,
            )
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(id = R.string.widgets_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                    ToolCircleButton(
                        onClick = onClose,
                        modifier = Modifier.size(42.dp),
                        usePrimaryContainer = false,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    itemsIndexed(
                        items = widgetsState.items,
                        key = { _, item -> item.id },
                    ) { _, item ->
                        val itemShape = RoundedCornerShape(16.dp)
                        Surface(
                            onClick = { onCatalogItemSelected(item.widgetType) },
                            shape = itemShape,
                            color = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("widget_catalog_item_${item.widgetType.name}"),
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                GlassButtonBackground(
                                    shape = itemShape,
                                    tintColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = ToolPanelUiConstants.GLASS_LIST_ITEM_TINT_ALPHA,
                                    ),
                                    blurRadius = ToolPanelUiConstants.GLASS_LIST_ITEM_BLUR_RADIUS,
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(
                                                alpha = ToolPanelUiConstants.WIDGET_ITEM_BORDER_ALPHA,
                                            ),
                                            shape = itemShape,
                                        ),
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    Text(
                                        text = stringResource(id = item.titleResId),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = stringResource(id = item.subtitleResId),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchActionButton(
    text: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart,
        ) {
            GlassButtonBackground(
                shape = shape,
                tintColor = MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = ToolPanelUiConstants.GLASS_PRIMARY_TINT_ALPHA,
                ),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SearchInputPill(
    query: String,
    suggestion: String?,
    maxWidth: androidx.compose.ui.unit.Dp,
    onQueryChanged: (String) -> Unit,
    onSearchClick: () -> Unit,
    onBrowserSearchClick: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val searchShape = RoundedCornerShape(28.dp)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var queryFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = query,
                selection = TextRange(query.length),
            ),
        )
    }
    LaunchedEffect(query) {
        if (queryFieldValue.text != query) {
            queryFieldValue = queryFieldValue.copy(
                text = query,
                selection = TextRange(query.length),
            )
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val queryText = queryFieldValue.text
    val highlightColor = MaterialTheme.colorScheme.primary
    val suggestionTextWithTheme = remember(queryText, suggestion, highlightColor) {
        SearchSuggestionTextFormatter.build(
            query = queryText,
            suggestion = suggestion,
            highlightColor = highlightColor,
        )
    }

    Surface(
        shape = searchShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .height(ToolPanelUiConstants.BUTTON_SIZE)
            .size(width = maxWidth, height = ToolPanelUiConstants.BUTTON_SIZE),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            GlassButtonBackground(
                shape = searchShape,
                tintColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                    alpha = ToolPanelUiConstants.GLASS_PANEL_TINT_ALPHA,
                ),
                blurRadius = ToolPanelUiConstants.GLASS_PANEL_BLUR_RADIUS,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
            ) {
                ToolCircleButton(
                    onClick = onSearchClick,
                    modifier = Modifier.size(44.dp),
                    usePrimaryContainer = false,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }

                BasicTextField(
                    value = queryFieldValue,
                    onValueChange = { updated ->
                        val sanitizedText = updated.text.replace('\n', ' ')
                        val pinned = updated.copy(
                            text = sanitizedText,
                            selection = TextRange(sanitizedText.length),
                        )
                        queryFieldValue = pinned
                        if (pinned.text != query) {
                            onQueryChanged(pinned.text)
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                        autoCorrect = false,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSubmit() },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .padding(start = 8.dp, end = 12.dp),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (queryText.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.search_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.48f),
                                    maxLines = 1,
                                )
                            } else if (suggestionTextWithTheme != null) {
                                Text(
                                    text = suggestionTextWithTheme,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.46f),
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                ToolCircleButton(
                    onClick = {
                        val trimmed = queryText.trim()
                        if (trimmed.isNotEmpty()) {
                            onBrowserSearchClick(trimmed)
                        }
                    },
                    modifier = Modifier.size(40.dp),
                    usePrimaryContainer = false,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                            alpha = if (queryText.isBlank()) 0.42f else 1f,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditPanelContent(
    editState: EditUiState,
    maxWidth: androidx.compose.ui.unit.Dp,
    onToolSelected: (CanvasEditToolId) -> Unit,
    onColorSelected: (Int) -> Unit,
    onBrushSizeStep: (Float) -> Unit,
    onTextSizeStep: (Float) -> Unit,
    onInlineEditorValueChanged: (String) -> Unit,
    onInlineEditorConfirm: () -> Unit,
    onInlineEditorCancel: () -> Unit,
    onUndo: () -> Unit,
    onClearCustomElements: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val editTools = listOf(
            CanvasEditToolId.Move,
            CanvasEditToolId.Brush,
            CanvasEditToolId.Selection,
            CanvasEditToolId.StickyNote,
            CanvasEditToolId.Text,
            CanvasEditToolId.Frame,
            CanvasEditToolId.Delete,
        )
        val editToolButtonSize = 38.dp
        val editToolGap = 4.dp
        val closeButtonSize = 44.dp
        val editToolsSingleRowWidth = (editToolButtonSize * editTools.size) +
            (editToolGap * (editTools.size - 1).coerceAtLeast(0))
        val requiredSingleRowWidth = editToolsSingleRowWidth + 6.dp + closeButtonSize
        val useTwoLayerEditTools = requiredSingleRowWidth > maxWidth

        if (useTwoLayerEditTools) {
            val editToolRows = editTools.chunked((editTools.size + 1) / 2)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                editToolRows.forEachIndexed { rowIndex, toolsRow ->
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        toolsRow.forEachIndexed { index, toolId ->
                            ToolCircleButton(
                                onClick = { onToolSelected(toolId) },
                                modifier = Modifier
                                    .padding(start = if (index == 0) 0.dp else editToolGap)
                                    .size(editToolButtonSize),
                                usePrimaryContainer = editState.selectedTool == toolId,
                            ) {
                                when (toolId) {
                                    CanvasEditToolId.Move -> Icon(
                                        imageVector = Icons.Rounded.OpenWith,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    CanvasEditToolId.Brush -> Icon(
                                        imageVector = Icons.Rounded.Brush,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    CanvasEditToolId.Selection -> Icon(
                                        imageVector = Icons.Rounded.SelectAll,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    CanvasEditToolId.StickyNote -> Icon(
                                        imageVector = Icons.Rounded.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    CanvasEditToolId.Text -> Icon(
                                        imageVector = Icons.Rounded.TextFields,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    CanvasEditToolId.Frame -> Icon(
                                        imageVector = Icons.Rounded.CropSquare,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                    CanvasEditToolId.Delete -> Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                        if (rowIndex == 0) {
                            ToolCircleButton(
                                onClick = onUndo,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(closeButtonSize),
                                usePrimaryContainer = editState.canUndo,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                                        alpha = if (editState.canUndo) 1f else 0.42f,
                                    ),
                                )
                            }
                            ToolCircleButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(closeButtonSize),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                editTools.forEachIndexed { index, toolId ->
                    ToolCircleButton(
                        onClick = { onToolSelected(toolId) },
                        modifier = Modifier
                            .padding(start = if (index == 0) 0.dp else editToolGap)
                            .size(editToolButtonSize),
                        usePrimaryContainer = editState.selectedTool == toolId,
                    ) {
                        when (toolId) {
                            CanvasEditToolId.Move -> Icon(
                                imageVector = Icons.Rounded.OpenWith,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            CanvasEditToolId.Brush -> Icon(
                                imageVector = Icons.Rounded.Brush,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            CanvasEditToolId.Selection -> Icon(
                                imageVector = Icons.Rounded.SelectAll,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            CanvasEditToolId.StickyNote -> Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            CanvasEditToolId.Text -> Icon(
                                imageVector = Icons.Rounded.TextFields,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            CanvasEditToolId.Frame -> Icon(
                                imageVector = Icons.Rounded.CropSquare,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            CanvasEditToolId.Delete -> Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                ToolCircleButton(
                    onClick = onUndo,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(closeButtonSize),
                    usePrimaryContainer = editState.canUndo,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                            alpha = if (editState.canUndo) 1f else 0.42f,
                        ),
                    )
                }

                ToolCircleButton(
                    onClick = onClose,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(closeButtonSize),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            CanvasEditDefaults.PALETTE.forEachIndexed { index, color ->
                PaletteChip(
                    colorArgb = color,
                    selected = editState.selectedColorArgb == color,
                    modifier = Modifier.padding(start = if (index == 0) 0.dp else 6.dp),
                    onClick = { onColorSelected(color) },
                )
            }
        }

        val textSizeEditableTarget = editState.inlineEditor.target is CanvasInlineEditorTarget.EditText ||
            editState.inlineEditor.target is CanvasInlineEditorTarget.EditSticky
        val showBrushOrTextSizeControls = editState.selectedTool == CanvasEditToolId.Brush ||
            editState.selectedTool == CanvasEditToolId.Text ||
            textSizeEditableTarget

        if (showBrushOrTextSizeControls) {
            val isBrushSize = editState.selectedTool == CanvasEditToolId.Brush
            val valueLabel = if (isBrushSize) {
                stringResource(
                    id = R.string.edit_brush_size_value,
                    editState.brushWidthWorld.roundToInt(),
                )
            } else if (editState.inlineEditor.target is CanvasInlineEditorTarget.EditSticky) {
                stringResource(
                    id = R.string.edit_sticky_text_size_value,
                    editState.textSizeWorld.roundToInt(),
                )
            } else {
                stringResource(
                    id = R.string.edit_text_size_value,
                    editState.textSizeWorld.roundToInt(),
                )
            }
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.padding(end = 8.dp),
                )
                ToolCircleButton(
                    onClick = {
                        if (isBrushSize) {
                            onBrushSizeStep(-3f)
                        } else {
                            onTextSizeStep(-4f)
                        }
                    },
                    modifier = Modifier.size(36.dp),
                    usePrimaryContainer = false,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                ToolCircleButton(
                    onClick = {
                        if (isBrushSize) {
                            onBrushSizeStep(3f)
                        } else {
                            onTextSizeStep(4f)
                        }
                    },
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(36.dp),
                    usePrimaryContainer = false,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        Surface(
            onClick = onClearCustomElements,
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.height(40.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                GlassButtonBackground(
                    shape = RoundedCornerShape(14.dp),
                    tintColor = MaterialTheme.colorScheme.errorContainer.copy(
                        alpha = ToolPanelUiConstants.GLASS_ERROR_TINT_ALPHA,
                    ),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = stringResource(R.string.edit_clear_custom_elements),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        maxLines = 1,
                    )
                }
            }
        }

        if (editState.inlineEditor.isVisible) {
            editState.inlineEditor.titleResId?.let { titleResId ->
                Text(
                    text = stringResource(id = titleResId),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
            EditInlineEditorPill(
                state = editState.inlineEditor,
                maxWidth = maxWidth,
                onValueChanged = onInlineEditorValueChanged,
                onConfirm = onInlineEditorConfirm,
                onCancel = onInlineEditorCancel,
            )
        }
    }
}

@Composable
private fun EditInlineEditorPill(
    state: CanvasInlineEditorUiState,
    maxWidth: androidx.compose.ui.unit.Dp,
    onValueChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val inlineEditorShape = RoundedCornerShape(20.dp)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isMultilineInput = when (state.target) {
        is CanvasInlineEditorTarget.EditSticky,
        is CanvasInlineEditorTarget.NewSticky,
        is CanvasInlineEditorTarget.EditText,
        is CanvasInlineEditorTarget.NewText,
        -> true

        else -> false
    }
    var editorValue by remember(state.target, state.isVisible) {
        mutableStateOf(
            TextFieldValue(
                text = state.value,
                selection = TextRange(state.value.length),
            ),
        )
    }
    LaunchedEffect(state.value, state.target) {
        if (state.value != editorValue.text) {
            val maxIndex = state.value.length
            editorValue = editorValue.copy(
                text = state.value,
                selection = TextRange(
                    start = editorValue.selection.start.coerceIn(0, maxIndex),
                    end = editorValue.selection.end.coerceIn(0, maxIndex),
                ),
            )
        }
    }
    val placeholderText = state.placeholderResId?.let { placeholderResId ->
        stringResource(id = placeholderResId)
    }.orEmpty()
    LaunchedEffect(state.isVisible) {
        if (state.isVisible) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }
    Surface(
        shape = inlineEditorShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .width(maxWidth)
            .heightIn(min = INLINE_EDITOR_MULTILINE_MIN_HEIGHT, max = 196.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlassButtonBackground(
                shape = inlineEditorShape,
                tintColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                    alpha = ToolPanelUiConstants.GLASS_INLINE_EDITOR_TINT_ALPHA,
                ),
                blurRadius = ToolPanelUiConstants.GLASS_PANEL_BLUR_RADIUS,
            )
            Row(
                verticalAlignment = if (isMultilineInput) Alignment.Bottom else Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                TextField(
                    value = editorValue,
                    onValueChange = { updated ->
                        val sanitized = if (isMultilineInput) {
                            updated
                        } else {
                            val sanitizedText = updated.text.replace('\n', ' ')
                            val cursor = updated.selection.end.coerceIn(0, sanitizedText.length)
                            updated.copy(
                                text = sanitizedText,
                                selection = TextRange(cursor),
                                composition = null,
                            )
                        }
                        editorValue = sanitized
                        if (sanitized.text != state.value) {
                            onValueChanged(sanitized.text)
                        }
                    },
                    singleLine = !isMultilineInput,
                    maxLines = if (isMultilineInput) INLINE_EDITOR_MAX_LINES else 1,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    placeholder = {
                        if (placeholderText.isNotBlank()) {
                            Text(
                                text = placeholderText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.52f),
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (isMultilineInput) ImeAction.Default else ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isMultilineInput) {
                                onConfirm()
                            }
                        },
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        disabledTextColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.56f),
                        cursorColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        errorCursorColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .padding(vertical = 2.dp),
                )
                ToolCircleButton(
                    onClick = onConfirm,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(38.dp),
                    usePrimaryContainer = false,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                ToolCircleButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(38.dp),
                    usePrimaryContainer = false,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteChip(
    colorArgb: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val selectedDotColor = MaterialTheme.colorScheme.surface
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color(colorArgb),
        tonalElevation = if (selected) 5.dp else 1.dp,
        modifier = modifier.size(if (selected) 26.dp else 22.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(color = selectedDotColor, radius = size.minDimension / 3f)
                }
            }
        }
    }
}

@Composable
private fun ToolCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    usePrimaryContainer: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tintColor = if (usePrimaryContainer) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = ToolPanelUiConstants.GLASS_PRIMARY_TINT_ALPHA)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = ToolPanelUiConstants.GLASS_SECONDARY_TINT_ALPHA)
    }
    val blurTint = tintColor.copy(alpha = ToolPanelUiConstants.CIRCLE_GLASS_BLUR_TINT_ALPHA)
    val hazeState = LocalToolsHazeState.current
    val hazeStyle = remember(blurTint) {
        HazeStyle(
            backgroundColor = Color.Transparent,
            tint = HazeTint(blurTint),
            blurRadius = ToolPanelUiConstants.CIRCLE_GLASS_BLUR_RADIUS,
            noiseFactor = ToolPanelUiConstants.CIRCLE_GLASS_NOISE_FACTOR,
            fallbackTint = HazeTint(blurTint),
        )
    }
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            val hazeBackdropModifier = if (hazeState != null) {
                Modifier.hazeEffect(state = hazeState, style = hazeStyle)
            } else {
                Modifier.background(blurTint.copy(alpha = 0.35f))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .then(hazeBackdropModifier),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(tintColor.copy(alpha = ToolPanelUiConstants.CIRCLE_GLASS_BASE_ALPHA))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ToolPanelUiConstants.CIRCLE_STROKE_ALPHA),
                        shape = CircleShape,
                    ),
            )
            content()
        }
    }
}

@Composable
private fun GlassButtonBackground(
    shape: Shape,
    tintColor: Color,
    blurRadius: androidx.compose.ui.unit.Dp = ToolPanelUiConstants.GLASS_BLUR_RADIUS,
    modifier: Modifier = Modifier,
) {
    val hazeState = LocalToolsHazeState.current
    val hazeStyle = remember(tintColor, blurRadius) {
        HazeStyle(
            backgroundColor = Color.Transparent,
            tint = HazeTint(tintColor),
            blurRadius = blurRadius,
            noiseFactor = ToolPanelUiConstants.GLASS_NOISE_FACTOR,
            fallbackTint = HazeTint(tintColor.copy(alpha = ToolPanelUiConstants.GLASS_FALLBACK_ALPHA)),
        )
    }
    val hazeBackdropModifier = if (hazeState != null) {
        Modifier.hazeEffect(state = hazeState, style = hazeStyle)
    } else {
        Modifier.background(tintColor.copy(alpha = ToolPanelUiConstants.GLASS_FALLBACK_ALPHA))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .then(hazeBackdropModifier),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = ToolPanelUiConstants.GLASS_HIGHLIGHT_ALPHA),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = ToolPanelUiConstants.GLASS_STROKE_ALPHA),
                    shape = shape,
                ),
        )
    }
}

private val LocalToolsHazeState = staticCompositionLocalOf<HazeState?> { null }

internal object SearchSuggestionTextFormatter {
    fun build(
        query: String,
        suggestion: String?,
        highlightColor: Color,
    ): AnnotatedString? {
        if (query.isBlank() || suggestion.isNullOrBlank()) return null
        if (!suggestion.startsWith(query, ignoreCase = true)) return null
        if (suggestion.length == query.length) return null
        val suffix = suggestion.drop(query.length)
        return buildAnnotatedString {
            append(query)
            withStyle(
                style = SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.SemiBold,
                ),
            ) {
                append(suffix)
            }
        }
    }
}

private const val INLINE_EDITOR_MAX_LINES = 10
private val INLINE_EDITOR_MULTILINE_MIN_HEIGHT = 132.dp

private object ToolPanelUiConstants {
    val BUTTON_SIZE = 56.dp
    val SEARCH_ROW_GAP = 10.dp
    val ITEM_GAP = 10.dp
    val GLASS_BLUR_RADIUS = 22.dp
    val GLASS_PANEL_BLUR_RADIUS = 26.dp
    val GLASS_LIST_ITEM_BLUR_RADIUS = 18.dp
    const val GLASS_NOISE_FACTOR = 0.04f
    const val GLASS_FALLBACK_ALPHA = 0.30f
    const val GLASS_HIGHLIGHT_ALPHA = 0.18f
    const val GLASS_STROKE_ALPHA = 0.22f
    const val GLASS_PRIMARY_TINT_ALPHA = 0.30f
    const val GLASS_SECONDARY_TINT_ALPHA = 0.28f
    const val GLASS_ERROR_TINT_ALPHA = 0.30f
    const val GLASS_PANEL_TINT_ALPHA = 0.24f
    const val GLASS_LIST_ITEM_TINT_ALPHA = 0.18f
    const val GLASS_INLINE_EDITOR_TINT_ALPHA = 0.22f
    const val WIDGET_ITEM_BORDER_ALPHA = 0.34f
    const val CIRCLE_STROKE_ALPHA = 0.18f
    val CIRCLE_GLASS_BLUR_RADIUS = 24.dp
    const val CIRCLE_GLASS_NOISE_FACTOR = 0.04f
    const val CIRCLE_GLASS_BLUR_TINT_ALPHA = 0.38f
    const val CIRCLE_GLASS_BASE_ALPHA = 0.12f
}
