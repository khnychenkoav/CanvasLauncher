package com.darksok.canvaslauncher.feature.launcher.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.darksok.canvaslauncher.feature.launcher.R

@Composable
fun AppsListScreen(
    state: AppsListUiState,
    modifier: Modifier = Modifier,
    onQueryChanged: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onShowOnCanvas: (String) -> Unit,
    onRequestUninstall: (String) -> Unit,
    onClose: () -> Unit,
) {
    var actionTarget by remember { mutableStateOf<AppsListItemUiState?>(null) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxSize(),
    ) {
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)),
                    contentAlignment = Alignment.Center,
                ) {
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
                            onLongClick = { actionTarget = item },
                        )
                    }
                }
            }
        }

        actionTarget?.let { item ->
            AppsListActionSheet(
                item = item,
                onDismiss = { actionTarget = null },
                onShowOnCanvas = {
                    actionTarget = null
                    onShowOnCanvas(item.packageName)
                },
                onUninstall = {
                    actionTarget = null
                    onRequestUninstall(item.packageName)
                },
            )
        }
    }
}

@Composable
private fun AppsListSearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
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
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
    ) {
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

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AppListRow(
    item: AppsListItemUiState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val iconImage = remember(item.icon) { item.icon?.asImageBitmap() }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                if (iconImage != null) {
                    Image(
                        bitmap = iconImage,
                        contentDescription = item.label,
                        modifier = Modifier.size(36.dp),
                    )
                } else {
                    Text(
                        text = item.label.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
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

@Composable
private fun AppsListActionSheet(
    item: AppsListItemUiState,
    onDismiss: () -> Unit,
    onShowOnCanvas: () -> Unit,
    onUninstall: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.36f))
                .clickable(onClick = onDismiss),
        )

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ActionSheetButton(
                    text = stringResource(id = R.string.apps_list_menu_show_canvas),
                    onClick = onShowOnCanvas,
                )
                ActionSheetButton(
                    text = stringResource(id = R.string.apps_list_menu_uninstall),
                    onClick = onUninstall,
                    isDestructive = true,
                )
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(id = R.string.apps_list_menu_cancel))
                }
            }
        }
    }
}

@Composable
private fun ActionSheetButton(
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
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
        shape = RoundedCornerShape(14.dp),
        color = background,
        tonalElevation = 2.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}
