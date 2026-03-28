package com.darksok.canvaslauncher.feature.launcher.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.min
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
                                CloseGlyph(color = MaterialTheme.colorScheme.onSecondaryContainer)
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
                            onClose = onEditClose,
                        )
                    }

                    else -> {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AnimatedVisibility(
                                visible = toolsState.isExpanded,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    toolsState.tools.forEach { tool ->
                                        ToolCircleButton(
                                            onClick = { onToolSelected(tool.id) },
                                            modifier = Modifier.size(ToolPanelUiConstants.BUTTON_SIZE),
                                        ) {
                                            when (tool.id) {
                                                LauncherToolId.Search -> {
                                                    SearchGlyph(color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                }

                                                LauncherToolId.AppsList -> {
                                                    AppsListGlyph(color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                }

                                                LauncherToolId.Edit -> {
                                                    PencilGlyph(color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                }

                                                LauncherToolId.Settings -> {
                                                    SettingsGlyph(color = MaterialTheme.colorScheme.onSecondaryContainer)
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
                                GridNineGlyph(color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                SearchGlyph(color = MaterialTheme.colorScheme.onSecondaryContainer)
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
                CanvasEditToolId.Eraser,
                CanvasEditToolId.StickyNote,
                CanvasEditToolId.Text,
                CanvasEditToolId.Frame,
            )
            editTools.forEachIndexed { index, toolId ->
                ToolCircleButton(
                    onClick = { onToolSelected(toolId) },
                    modifier = Modifier
                        .padding(start = if (index == 0) 0.dp else 6.dp)
                        .size(44.dp),
                    usePrimaryContainer = editState.selectedTool == toolId,
                ) {
                    when (toolId) {
                        CanvasEditToolId.Move -> MoveGlyph(MaterialTheme.colorScheme.onSecondaryContainer)
                        CanvasEditToolId.Brush -> BrushGlyph(MaterialTheme.colorScheme.onSecondaryContainer)
                        CanvasEditToolId.Eraser -> EraserGlyph(MaterialTheme.colorScheme.onSecondaryContainer)
                        CanvasEditToolId.StickyNote -> StickyGlyph(MaterialTheme.colorScheme.onSecondaryContainer)
                        CanvasEditToolId.Text -> TextGlyph(MaterialTheme.colorScheme.onSecondaryContainer)
                        CanvasEditToolId.Frame -> FrameGlyph(MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            ToolCircleButton(
                onClick = onClose,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(ToolPanelUiConstants.BUTTON_SIZE),
            ) {
                CloseGlyph(MaterialTheme.colorScheme.onSecondaryContainer)
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

        if (editState.selectedTool == CanvasEditToolId.Brush || editState.selectedTool == CanvasEditToolId.Text) {
            val valueLabel = if (editState.selectedTool == CanvasEditToolId.Brush) {
                "Brush ${editState.brushWidthWorld.roundToInt()}"
            } else {
                "Text ${editState.textSizeWorld.roundToInt()}"
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
                        if (editState.selectedTool == CanvasEditToolId.Brush) {
                            onBrushSizeStep(-3f)
                        } else {
                            onTextSizeStep(-4f)
                        }
                    },
                    modifier = Modifier.size(36.dp),
                    usePrimaryContainer = false,
                ) {
                    MinusGlyph(MaterialTheme.colorScheme.onSecondaryContainer)
                }
                ToolCircleButton(
                    onClick = {
                        if (editState.selectedTool == CanvasEditToolId.Brush) {
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
                    PlusGlyph(MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        if (editState.inlineEditor.isVisible) {
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
                        if (state.value.isBlank()) {
                            Text(
                                text = state.placeholder,
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
                CheckGlyph(MaterialTheme.colorScheme.onSecondaryContainer)
            }
            ToolCircleButton(
                onClick = onCancel,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(38.dp),
                usePrimaryContainer = false,
            ) {
                CloseGlyph(MaterialTheme.colorScheme.onSecondaryContainer)
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

@Composable
private fun GridNineGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val cell = size.minDimension / 4f
        val radius = min(cell * 0.25f, 2.4.dp.toPx())
        for (x in 1..3) {
            for (y in 1..3) {
                drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(cell * x, cell * y),
                )
            }
        }
    }
}

@Composable
private fun SearchGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val radius = size.minDimension * 0.28f
        val center = Offset(size.minDimension * 0.44f, size.minDimension * 0.44f)
        val strokeWidth = 2.2.dp.toPx()
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = color,
            start = Offset(center.x + radius * 0.68f, center.y + radius * 0.68f),
            end = Offset(size.width * 0.88f, size.height * 0.88f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun CloseGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val strokeWidth = 2.4.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.2f, size.height * 0.2f),
            end = Offset(size.width * 0.8f, size.height * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.8f, size.height * 0.2f),
            end = Offset(size.width * 0.2f, size.height * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun CheckGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val strokeWidth = 2.2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.15f, size.height * 0.55f),
            end = Offset(size.width * 0.42f, size.height * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.42f, size.height * 0.8f),
            end = Offset(size.width * 0.86f, size.height * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun PlusGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.2f, size.height * 0.5f),
            end = Offset(size.width * 0.8f, size.height * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.5f, size.height * 0.2f),
            end = Offset(size.width * 0.5f, size.height * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MinusGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.2f, size.height * 0.5f),
            end = Offset(size.width * 0.8f, size.height * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun PencilGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.2f, size.height * 0.78f),
            end = Offset(size.width * 0.78f, size.height * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.64f, size.height * 0.2f),
            end = Offset(size.width * 0.78f, size.height * 0.36f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.2f, size.height * 0.78f),
            end = Offset(size.width * 0.36f, size.height * 0.64f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MoveGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        drawLine(color, Offset(size.width * 0.5f, size.height * 0.15f), Offset(size.width * 0.5f, size.height * 0.85f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.15f, size.height * 0.5f), Offset(size.width * 0.85f, size.height * 0.5f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun BrushGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.3f, size.height * 0.2f),
            end = Offset(size.width * 0.7f, size.height * 0.6f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.12f,
            center = Offset(size.width * 0.7f, size.height * 0.72f),
        )
    }
}

@Composable
private fun EraserGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.25f, size.height * 0.72f),
            end = Offset(size.width * 0.72f, size.height * 0.25f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.18f, size.height * 0.78f),
            end = Offset(size.width * 0.78f, size.height * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun StickyGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 1.8.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.2f, size.height * 0.18f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.58f, size.height * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.58f, size.height * 0.76f),
            end = Offset(size.width * 0.78f, size.height * 0.56f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun TextGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.2f, size.height * 0.24f),
            end = Offset(size.width * 0.8f, size.height * 0.24f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.5f, size.height * 0.24f),
            end = Offset(size.width * 0.5f, size.height * 0.8f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun FrameGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 1.8.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.16f, size.height * 0.2f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.68f, size.height * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = stroke),
        )
    }
}

@Composable
private fun SettingsGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension * 0.32f
        val innerRadius = size.minDimension * 0.14f
        val stroke = 2.dp.toPx()
        drawCircle(
            color = color,
            radius = outerRadius,
            center = center,
            style = Stroke(width = stroke),
        )
        drawCircle(
            color = color,
            radius = innerRadius,
            center = center,
        )
        for (index in 0 until 8) {
            val angle = (index * 45f).toRadians()
            val cos = kotlin.math.cos(angle).toFloat()
            val sin = kotlin.math.sin(angle).toFloat()
            val start = Offset(
                x = center.x + cos * (outerRadius + 1.dp.toPx()),
                y = center.y + sin * (outerRadius + 1.dp.toPx()),
            )
            val end = Offset(
                x = center.x + cos * (outerRadius + 4.dp.toPx()),
                y = center.y + sin * (outerRadius + 4.dp.toPx()),
            )
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun AppsListGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val stroke = 2.dp.toPx()
        val top = size.height * 0.24f
        val gap = size.height * 0.24f
        val startX = size.width * 0.28f
        val endX = size.width * 0.82f
        for (index in 0..2) {
            val y = top + gap * index
            drawCircle(
                color = color,
                radius = 1.5.dp.toPx(),
                center = Offset(size.width * 0.16f, y),
            )
            drawLine(
                color = color,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun Float.toRadians(): Double {
    return this * (Math.PI / 180.0)
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
