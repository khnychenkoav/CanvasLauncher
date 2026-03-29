package com.darksok.canvaslauncher.settings

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darksok.canvaslauncher.R
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.core.model.ui.resolveDarkTheme
import com.darksok.canvaslauncher.core.ui.theme.CanvasLauncherTheme
import com.darksok.canvaslauncher.core.ui.theme.darkPalettePreviewColors
import com.darksok.canvaslauncher.core.ui.theme.lightPalettePreviewColors
import com.darksok.canvaslauncher.i18n.AppLanguage
import com.darksok.canvaslauncher.i18n.AppLocaleManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRoute(
    onClose: () -> Unit,
    onOpenLauncherChooser: () -> Unit,
    onOpenPhoneSettings: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var layoutDescriptionMode by remember { mutableStateOf<AppLayoutMode?>(null) }
    var selectedLanguage by remember { mutableStateOf(AppLocaleManager.readPreferredLanguage(context)) }
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = uiState.themeMode.resolveDarkTheme(isSystemDark)

    CanvasLauncherTheme(darkTheme = darkTheme, lightPalette = uiState.lightPalette, darkPalette = uiState.darkPalette) {
        SystemBarsContrastEffect(darkTheme = darkTheme)
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .systemBarsPadding(),
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(id = R.string.settings_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        )
                    }
                    OutlinedButton(
                        onClick = onClose,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_close_button),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_language_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(id = R.string.settings_language_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AppLanguage.entries.forEach { language ->
                                FilterChip(
                                    selected = selectedLanguage == language,
                                    onClick = {
                                        if (selectedLanguage != language) {
                                            selectedLanguage = language
                                            onLanguageSelected(language)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = languageLabel(language),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    modifier = Modifier.heightIn(min = 34.dp),
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_layout_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(id = R.string.settings_layout_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        )
                        AppLayoutMode.entries.forEach { mode ->
                            LayoutModeOptionCard(
                                mode = mode,
                                selected = uiState.layoutMode == mode,
                                onSelect = {
                                    layoutDescriptionMode = null
                                    viewModel.onLayoutModeSelected(mode)
                                },
                                onLongPress = {
                                    layoutDescriptionMode = mode
                                },
                            )
                        }
                        layoutDescriptionMode?.let { mode ->
                            Text(
                                text = layoutModeDescription(mode),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_theme_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(id = R.string.settings_theme_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = uiState.themeMode == mode,
                                    onClick = { viewModel.onThemeModeSelected(mode) },
                                    label = {
                                        Text(
                                            text = themeModeLabel(mode),
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    modifier = Modifier.heightIn(min = 34.dp),
                                )
                            }
                        }
                        Text(
                            text = stringResource(id = R.string.settings_palette_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                        )
                        Text(
                            text = stringResource(id = R.string.settings_light_palette_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        LightThemePalette.entries.forEach { palette ->
                            PaletteOptionCard(
                                label = lightPaletteLabel(palette),
                                swatches = lightPalettePreviewColors(palette),
                                selected = uiState.lightPalette == palette,
                                onSelect = { viewModel.onLightPaletteSelected(palette) },
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.settings_dark_palette_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        DarkThemePalette.entries.forEach { palette ->
                            PaletteOptionCard(
                                label = darkPaletteLabel(palette),
                                swatches = darkPalettePreviewColors(palette),
                                selected = uiState.darkPalette == palette,
                                onSelect = { viewModel.onDarkPaletteSelected(palette) },
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.settings_reopen_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_system_controls_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        OutlinedButton(
                            onClick = onOpenLauncherChooser,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_launcher_picker_button),
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenPhoneSettings,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_phone_settings_button),
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteOptionCard(
    label: String,
    swatches: List<Color>,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
        },
        tonalElevation = if (selected) 4.dp else 1.dp,
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PaletteSwatches(swatches = swatches)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PaletteSwatches(swatches: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        swatches.forEach { color ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = color, shape = CircleShape),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LayoutModeOptionCard(
    mode: AppLayoutMode,
    selected: Boolean,
    onSelect: () -> Unit,
    onLongPress: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
        },
        tonalElevation = if (selected) 4.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onSelect,
                onLongClick = onLongPress,
            ),
    ) {
        Text(
            text = layoutModeLabel(mode),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun SystemBarsContrastEffect(darkTheme: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String {
    return when (mode) {
        ThemeMode.SYSTEM -> stringResource(id = R.string.theme_mode_system)
        ThemeMode.LIGHT -> stringResource(id = R.string.theme_mode_light)
        ThemeMode.DARK -> stringResource(id = R.string.theme_mode_dark)
    }
}

@Composable
private fun lightPaletteLabel(palette: LightThemePalette): String {
    return when (palette) {
        LightThemePalette.SKY_BREEZE -> stringResource(id = R.string.light_palette_sky_breeze)
        LightThemePalette.MINT_GARDEN -> stringResource(id = R.string.light_palette_mint_garden)
        LightThemePalette.SUNSET_GLOW -> stringResource(id = R.string.light_palette_sunset_glow)
        LightThemePalette.ROSE_DAWN -> stringResource(id = R.string.light_palette_rose_dawn)
    }
}

@Composable
private fun darkPaletteLabel(palette: DarkThemePalette): String {
    return when (palette) {
        DarkThemePalette.MIDNIGHT_BLUE -> stringResource(id = R.string.dark_palette_midnight_blue)
        DarkThemePalette.DEEP_OCEAN -> stringResource(id = R.string.dark_palette_deep_ocean)
        DarkThemePalette.FOREST_NIGHT -> stringResource(id = R.string.dark_palette_forest_night)
        DarkThemePalette.CHARCOAL_AMBER -> stringResource(id = R.string.dark_palette_charcoal_amber)
    }
}

@Composable
private fun layoutModeLabel(mode: AppLayoutMode): String {
    return when (mode) {
        AppLayoutMode.SPIRAL -> stringResource(id = R.string.layout_mode_spiral)
        AppLayoutMode.RECTANGLE -> stringResource(id = R.string.layout_mode_rectangle)
        AppLayoutMode.CIRCLE -> stringResource(id = R.string.layout_mode_circle)
        AppLayoutMode.OVAL -> stringResource(id = R.string.layout_mode_oval)
        AppLayoutMode.SMART_AUTO -> stringResource(id = R.string.layout_mode_smart_auto)
        AppLayoutMode.ICON_COLOR -> stringResource(id = R.string.layout_mode_icon_color)
    }
}

@Composable
private fun layoutModeDescription(mode: AppLayoutMode): String {
    return when (mode) {
        AppLayoutMode.SPIRAL -> stringResource(id = R.string.layout_mode_spiral_description)
        AppLayoutMode.RECTANGLE -> stringResource(id = R.string.layout_mode_rectangle_description)
        AppLayoutMode.CIRCLE -> stringResource(id = R.string.layout_mode_circle_description)
        AppLayoutMode.OVAL -> stringResource(id = R.string.layout_mode_oval_description)
        AppLayoutMode.SMART_AUTO -> stringResource(id = R.string.layout_mode_smart_auto_description)
        AppLayoutMode.ICON_COLOR -> stringResource(id = R.string.layout_mode_icon_color_description)
    }
}

@Composable
private fun languageLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SYSTEM -> stringResource(id = R.string.language_system)
        AppLanguage.ENGLISH -> stringResource(id = R.string.language_english)
        AppLanguage.RUSSIAN -> stringResource(id = R.string.language_russian)
        AppLanguage.SPANISH -> stringResource(id = R.string.language_spanish)
        AppLanguage.GERMAN -> stringResource(id = R.string.language_german)
        AppLanguage.FRENCH -> stringResource(id = R.string.language_french)
        AppLanguage.PORTUGUESE_BRAZIL -> stringResource(id = R.string.language_portuguese_brazil)
    }
}

