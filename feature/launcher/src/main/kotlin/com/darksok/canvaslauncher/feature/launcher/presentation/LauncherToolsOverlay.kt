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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.darksok.canvaslauncher.feature.launcher.R
import kotlin.math.roundToInt

@Composable
fun LauncherToolsOverlay(
    toolsState: ToolsUiState,
    modifier: Modifier = Modifier,
    onToolsToggle: () -> Unit,
    onToolSelected: (LauncherToolId) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchActionClick: () -> Unit,
    onSearchSubmit: () -> Unit,
    onSearchOpenInBrowser: (String) -> Unit,
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
    onEditClearCustomElements: () -> Unit,
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomLiftDp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        val closeButtonSize = ToolPanelUiConstants.BUTTON_SIZE
        val panelMaxWidth = maxWidth
        val searchFieldMaxWidth =
            (maxWidth - closeButtonSize - ToolPanelUiConstants.SEARCH_ROW_GAP - 2.dp).coerceAtLeast(180.dp)

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> toolsContentHeightPx = size.height },
        ) {
            AnimatedVisibility(
                visible = toolsState.isSearchActive && toolsState.search.showLaunchAction && toolsState.search.topMatchLabel != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            ) {
                SearchLaunchButton(
                    appLabel = toolsState.search.topMatchLabel.orEmpty(),
                    onClick = onSearchLaunchTopMatch,
                )
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
                            onClearCustomElements = onEditClearCustomElements,
                            onClose = onEditClose,
                        )
                    }

                    else -> {
                        val quickTools = toolsState.tools.filterNot { it.id == LauncherToolId.Search }
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
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

                                                LauncherToolId.Search -> Unit
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

@Composable
private fun SearchLaunchButton(
    appLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(id = R.string.search_launch_top_match, appLabel),
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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val highlightColor = MaterialTheme.colorScheme.primary
    val suggestionTextWithTheme = remember(query, suggestion, highlightColor) {
        SearchSuggestionTextFormatter.build(
            query = query,
            suggestion = suggestion,
            highlightColor = highlightColor,
        )
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
        tonalElevation = 6.dp,
        modifier = Modifier
            .height(ToolPanelUiConstants.BUTTON_SIZE)
            .size(width = maxWidth, height = ToolPanelUiConstants.BUTTON_SIZE),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
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
                value = query,
                onValueChange = onQueryChanged,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onSubmit() },
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .padding(start = 8.dp, end = 12.dp),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
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
                    val trimmed = query.trim()
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
                        alpha = if (query.isBlank()) 0.42f else 1f,
                    ),
                )
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
    onClearCustomElements: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
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
            editTools.forEachIndexed { index, toolId ->
                ToolCircleButton(
                    onClick = { onToolSelected(toolId) },
                    modifier = Modifier
                        .padding(start = if (index == 0) 0.dp else 4.dp)
                        .size(38.dp),
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
                onClick = onClose,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
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
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
            tonalElevation = 3.dp,
            modifier = Modifier.height(40.dp),
        ) {
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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
        modifier = Modifier
            .height(50.dp)
            .size(width = maxWidth, height = 50.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            BasicTextField(
                value = state.value,
                onValueChange = onValueChanged,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (state.value.isBlank() && placeholderText.isNotBlank()) {
                            Text(
                                text = placeholderText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.52f),
                            )
                        }
                        inner()
                    }
                },
            )
            ToolCircleButton(
                onClick = onConfirm,
                modifier = Modifier.size(38.dp),
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
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (usePrimaryContainer) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.96f)
        },
        tonalElevation = 6.dp,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

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

private object ToolPanelUiConstants {
    val BUTTON_SIZE = 56.dp
    val SEARCH_ROW_GAP = 10.dp
}
