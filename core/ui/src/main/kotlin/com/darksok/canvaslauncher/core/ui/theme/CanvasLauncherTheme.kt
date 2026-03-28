package com.darksok.canvaslauncher.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette

@Composable
fun CanvasLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    lightPalette: LightThemePalette = LightThemePalette.SKY_BREEZE,
    darkPalette: DarkThemePalette = DarkThemePalette.MIDNIGHT_BLUE,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            darkPalette.toColorScheme()
        } else {
            lightPalette.toColorScheme()
        },
        content = content,
    )
}

fun lightPalettePreviewColors(palette: LightThemePalette): List<Color> {
    val spec = palette.toSpec()
    return listOf(spec.primary, spec.secondary, spec.primaryContainer, spec.surfaceVariant)
}

fun darkPalettePreviewColors(palette: DarkThemePalette): List<Color> {
    val spec = palette.toSpec()
    return listOf(spec.primary, spec.secondary, spec.primaryContainer, spec.surfaceVariant)
}

private fun LightThemePalette.toColorScheme(): ColorScheme {
    val spec = toSpec()
    return lightColorScheme(
        primary = spec.primary,
        onPrimary = spec.onPrimary,
        primaryContainer = spec.primaryContainer,
        onPrimaryContainer = spec.onPrimaryContainer,
        secondary = spec.secondary,
        onSecondary = spec.onSecondary,
        secondaryContainer = spec.secondaryContainer,
        onSecondaryContainer = spec.onSecondaryContainer,
        background = spec.background,
        onBackground = spec.onBackground,
        surface = spec.surface,
        onSurface = spec.onSurface,
        surfaceVariant = spec.surfaceVariant,
    )
}

private fun DarkThemePalette.toColorScheme(): ColorScheme {
    val spec = toSpec()
    return darkColorScheme(
        primary = spec.primary,
        onPrimary = spec.onPrimary,
        primaryContainer = spec.primaryContainer,
        onPrimaryContainer = spec.onPrimaryContainer,
        secondary = spec.secondary,
        onSecondary = spec.onSecondary,
        secondaryContainer = spec.secondaryContainer,
        onSecondaryContainer = spec.onSecondaryContainer,
        background = spec.background,
        onBackground = spec.onBackground,
        surface = spec.surface,
        onSurface = spec.onSurface,
        surfaceVariant = spec.surfaceVariant,
    )
}

private fun LightThemePalette.toSpec(): PaletteSpec {
    return when (this) {
        LightThemePalette.SKY_BREEZE -> PaletteSpec(
            primary = Color(0xFF2F74D0),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD6E6FF),
            onPrimaryContainer = Color(0xFF0B2A52),
            secondary = Color(0xFF2E8FB2),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD5EEF8),
            onSecondaryContainer = Color(0xFF0A2C37),
            surface = Color(0xFFF4F8FF),
            onSurface = Color(0xFF151B24),
            surfaceVariant = Color(0xFFDCE6F5),
            background = Color(0xFFEEF4FF),
            onBackground = Color(0xFF151B24),
        )

        LightThemePalette.MINT_GARDEN -> PaletteSpec(
            primary = Color(0xFF2C8C6F),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFCDEFE2),
            onPrimaryContainer = Color(0xFF0E362A),
            secondary = Color(0xFF4B7D67),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFD3E8DD),
            onSecondaryContainer = Color(0xFF1A3026),
            surface = Color(0xFFF3FAF6),
            onSurface = Color(0xFF16201B),
            surfaceVariant = Color(0xFFD8E6DE),
            background = Color(0xFFEEF7F1),
            onBackground = Color(0xFF16201B),
        )

        LightThemePalette.SUNSET_GLOW -> PaletteSpec(
            primary = Color(0xFFC4673B),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDCCB),
            onPrimaryContainer = Color(0xFF4A1D08),
            secondary = Color(0xFFAF7A3A),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF9E1C7),
            onSecondaryContainer = Color(0xFF3E2710),
            surface = Color(0xFFFFF7F1),
            onSurface = Color(0xFF241A14),
            surfaceVariant = Color(0xFFF1DED1),
            background = Color(0xFFFFF3EA),
            onBackground = Color(0xFF241A14),
        )

        LightThemePalette.ROSE_DAWN -> PaletteSpec(
            primary = Color(0xFFB34F6A),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFD9E2),
            onPrimaryContainer = Color(0xFF4A1023),
            secondary = Color(0xFF8A6175),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF2DCE6),
            onSecondaryContainer = Color(0xFF331927),
            surface = Color(0xFFFFF5F8),
            onSurface = Color(0xFF25181D),
            surfaceVariant = Color(0xFFF1DEE5),
            background = Color(0xFFFFEEF3),
            onBackground = Color(0xFF25181D),
        )
    }
}

private fun DarkThemePalette.toSpec(): PaletteSpec {
    return when (this) {
        DarkThemePalette.MIDNIGHT_BLUE -> PaletteSpec(
            primary = Color(0xFF8AB4FF),
            onPrimary = Color(0xFF0E2E5D),
            primaryContainer = Color(0xFF1B3F70),
            onPrimaryContainer = Color(0xFFD6E6FF),
            secondary = Color(0xFF7FC5E0),
            onSecondary = Color(0xFF093446),
            secondaryContainer = Color(0xFF1A4C63),
            onSecondaryContainer = Color(0xFFCCEFFF),
            surface = Color(0xFF0D131D),
            onSurface = Color(0xFFE1E8F5),
            surfaceVariant = Color(0xFF243040),
            background = Color(0xFF0A1019),
            onBackground = Color(0xFFE1E8F5),
        )

        DarkThemePalette.DEEP_OCEAN -> PaletteSpec(
            primary = Color(0xFF6CB7D6),
            onPrimary = Color(0xFF003447),
            primaryContainer = Color(0xFF0D4D66),
            onPrimaryContainer = Color(0xFFC7EEFF),
            secondary = Color(0xFF4EC5A8),
            onSecondary = Color(0xFF00382C),
            secondaryContainer = Color(0xFF0B5745),
            onSecondaryContainer = Color(0xFFB6F4E2),
            surface = Color(0xFF0A161A),
            onSurface = Color(0xFFDCECEF),
            surfaceVariant = Color(0xFF1D3238),
            background = Color(0xFF081216),
            onBackground = Color(0xFFDCECEF),
        )

        DarkThemePalette.FOREST_NIGHT -> PaletteSpec(
            primary = Color(0xFF7BC58B),
            onPrimary = Color(0xFF0D3A1B),
            primaryContainer = Color(0xFF1C5630),
            onPrimaryContainer = Color(0xFFCCF2D4),
            secondary = Color(0xFFB3B56C),
            onSecondary = Color(0xFF2E3000),
            secondaryContainer = Color(0xFF474A1A),
            onSecondaryContainer = Color(0xFFF1F3B6),
            surface = Color(0xFF101812),
            onSurface = Color(0xFFE0EADF),
            surfaceVariant = Color(0xFF2A352C),
            background = Color(0xFF0B130E),
            onBackground = Color(0xFFE0EADF),
        )

        DarkThemePalette.CHARCOAL_AMBER -> PaletteSpec(
            primary = Color(0xFFE7A65C),
            onPrimary = Color(0xFF3D2300),
            primaryContainer = Color(0xFF684218),
            onPrimaryContainer = Color(0xFFFFE2BD),
            secondary = Color(0xFFD4B07A),
            onSecondary = Color(0xFF3A2B10),
            secondaryContainer = Color(0xFF5A4320),
            onSecondaryContainer = Color(0xFFFFE8C9),
            surface = Color(0xFF171311),
            onSurface = Color(0xFFF0E5DD),
            surfaceVariant = Color(0xFF352B25),
            background = Color(0xFF100D0B),
            onBackground = Color(0xFFF0E5DD),
        )
    }
}

private data class PaletteSpec(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val background: Color,
    val onBackground: Color,
)
