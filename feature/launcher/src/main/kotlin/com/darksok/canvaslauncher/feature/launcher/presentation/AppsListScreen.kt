package com.darksok.canvaslauncher.feature.launcher.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.darksok.canvaslauncher.feature.launcher.R
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun AppsListScreen(
    state: AppsListUiState,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    onQueryChanged: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onShowOnCanvas: (String) -> Unit,
    onRequestUninstall: (String) -> Unit,
    onClose: () -> Unit,
) {
    var expandedActionsPackage by remember { mutableStateOf<String?>(null) }
    Surface(
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxSize(),
    ) {
        CompositionLocalProvider(LocalAppsListHazeState provides hazeState) {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                GlassBackdrop(
                    shape = RectangleShape,
                    tintColor = MaterialTheme.colorScheme.surface.copy(alpha = AppsListGlassDefaults.screenTintAlpha),
                    blurRadius = AppsListGlassDefaults.screenBlurRadius,
                    drawHighlight = false,
                    drawBorder = false,
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(id = R.string.apps_list_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(
                                    id = R.string.apps_list_count,
                                    state.items.size,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                        OutlinedButton(onClick = onClose) {
                            Text(text = stringResource(id = R.string.apps_list_close))
                        }
                    }

                    AppsListSearchField(
                        query = state.query,
                        onQueryChanged = onQueryChanged,
                    )

                    if (state.items.isEmpty()) {
                        val emptyShape = RoundedCornerShape(18.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(emptyShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            GlassBackdrop(
                                shape = emptyShape,
                                tintColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                                blurRadius = AppsListGlassDefaults.panelBlurRadius,
                            )
                            Text(
                                text = stringResource(id = R.string.apps_list_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 10.dp),
                        ) {
                            items(
                                items = state.items,
                                key = { item -> item.packageName },
                            ) { item ->
                                AppListRow(
                                    item = item,
                                    onClick = { onAppClick(item.packageName) },
                                    actionsExpanded = expandedActionsPackage == item.packageName,
                                    onToggleActions = {
                                        expandedActionsPackage =
                                            if (expandedActionsPackage == item.packageName) null else item.packageName
                                    },
                                    onCollapseActions = { expandedActionsPackage = null },
                                    onShowOnCanvas = { onShowOnCanvas(item.packageName) },
                                    onRequestUninstall = { onRequestUninstall(item.packageName) },
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
private fun AppsListSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    val searchShape = RoundedCornerShape(22.dp)
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
    val queryText = queryFieldValue.text
    Surface(
        shape = searchShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlassBackdrop(
                shape = searchShape,
                tintColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = AppsListGlassDefaults.panelTintAlpha),
                blurRadius = AppsListGlassDefaults.panelBlurRadius,
            )
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
                keyboardActions = KeyboardActions(onSearch = {}),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (queryText.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.apps_list_search_placeholder),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.52f),
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
private fun AppListRow(
    item: AppsListItemUiState,
    onClick: () -> Unit,
    actionsExpanded: Boolean,
    onToggleActions: () -> Unit,
    onCollapseActions: () -> Unit,
    onShowOnCanvas: () -> Unit,
    onRequestUninstall: () -> Unit,
) {
    val iconImage = remember(item.icon) { item.icon?.asImageBitmap() }
    val rowShape = RoundedCornerShape(16.dp)
    val appIconShape = RoundedCornerShape(10.dp)
    Surface(
        shape = rowShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(item.packageName) {
                detectTapGestures(
                    onTap = {
                        if (actionsExpanded) {
                            onCollapseActions()
                        } else {
                            onClick()
                        }
                    },
                    onLongPress = { onToggleActions() },
                )
            },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GlassBackdrop(
                shape = rowShape,
                tintColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppsListGlassDefaults.itemTintAlpha),
                blurRadius = AppsListGlassDefaults.itemBlurRadius,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .animateContentSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(appIconShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (iconImage != null) {
                        Image(
                            bitmap = iconImage,
                            contentDescription = item.label,
                            contentScale = ContentScale.FillBounds,
                            filterQuality = FilterQuality.Medium,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = item.label.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = actionsExpanded,
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        InlineActionCircleButton(
                            icon = Icons.Rounded.Visibility,
                            contentDescription = stringResource(id = R.string.apps_list_menu_show_canvas),
                            onClick = {
                                onCollapseActions()
                                onShowOnCanvas()
                            },
                        )
                        InlineActionCircleButton(
                            icon = Icons.Rounded.Delete,
                            contentDescription = stringResource(id = R.string.apps_list_menu_uninstall),
                            onClick = {
                                onCollapseActions()
                                onRequestUninstall()
                            },
                            isDestructive = true,
                        )
                    }
                }

                Column(
                    modifier = Modifier.width(0.dp).weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineActionCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    val actionIconShape = RoundedCornerShape(10.dp)
    val background = if (isDestructive) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.34f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
    }
    val contentColor = if (isDestructive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        shape = actionIconShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = onClick,
        modifier = Modifier
            .size(34.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            GlassBackdrop(
                shape = actionIconShape,
                tintColor = background,
                blurRadius = AppsListGlassDefaults.itemBlurRadius,
            )
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun GlassBackdrop(
    shape: Shape,
    tintColor: Color,
    blurRadius: androidx.compose.ui.unit.Dp,
    drawHighlight: Boolean = true,
    drawBorder: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val hazeState = LocalAppsListHazeState.current
    val hazeStyle = remember(tintColor, blurRadius) {
        HazeStyle(
            backgroundColor = Color.Transparent,
            tint = HazeTint(tintColor),
            blurRadius = blurRadius,
            noiseFactor = AppsListGlassDefaults.noiseFactor,
            fallbackTint = HazeTint(tintColor.copy(alpha = AppsListGlassDefaults.fallbackAlpha)),
        )
    }
    val hazeModifier = if (hazeState != null) {
        Modifier.hazeEffect(state = hazeState, style = hazeStyle)
    } else {
        Modifier.background(tintColor.copy(alpha = AppsListGlassDefaults.fallbackAlpha))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .then(hazeModifier),
        )
        if (drawHighlight) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = AppsListGlassDefaults.highlightAlpha),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
        if (drawBorder) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = AppsListGlassDefaults.strokeAlpha),
                        shape = shape,
                    ),
            )
        }
    }
}

private val LocalAppsListHazeState = staticCompositionLocalOf<HazeState?> { null }

private object AppsListGlassDefaults {
    val screenBlurRadius = 30.dp
    val panelBlurRadius = 24.dp
    val itemBlurRadius = 18.dp
    const val screenTintAlpha = 0.20f
    const val panelTintAlpha = 0.24f
    const val itemTintAlpha = 0.18f
    const val noiseFactor = 0.04f
    const val fallbackAlpha = 0.30f
    const val highlightAlpha = 0.18f
    const val strokeAlpha = 0.22f
}
