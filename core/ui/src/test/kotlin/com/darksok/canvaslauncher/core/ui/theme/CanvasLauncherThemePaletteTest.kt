package com.darksok.canvaslauncher.core.ui.theme

import androidx.compose.ui.graphics.Color
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CanvasLauncherThemePaletteTest {

    @Test fun `sky breeze preview primary matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.SKY_BREEZE)[0]).isEqualTo(Color(0xFF2F74D0)) }
    @Test fun `sky breeze preview secondary matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.SKY_BREEZE)[1]).isEqualTo(Color(0xFF2E8FB2)) }
    @Test fun `sky breeze preview primary container matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.SKY_BREEZE)[2]).isEqualTo(Color(0xFFD6E6FF)) }
    @Test fun `sky breeze preview surface variant matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.SKY_BREEZE)[3]).isEqualTo(Color(0xFFDCE6F5)) }
    @Test fun `sky breeze scheme background matches spec`() { assertThat(lightPaletteColorScheme(LightThemePalette.SKY_BREEZE).background).isEqualTo(Color(0xFFEEF4FF)) }

    @Test fun `mint garden preview primary matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.MINT_GARDEN)[0]).isEqualTo(Color(0xFF2C8C6F)) }
    @Test fun `mint garden preview secondary matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.MINT_GARDEN)[1]).isEqualTo(Color(0xFF4B7D67)) }
    @Test fun `mint garden preview primary container matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.MINT_GARDEN)[2]).isEqualTo(Color(0xFFCDEFE2)) }
    @Test fun `mint garden preview surface variant matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.MINT_GARDEN)[3]).isEqualTo(Color(0xFFD8E6DE)) }
    @Test fun `mint garden scheme background matches spec`() { assertThat(lightPaletteColorScheme(LightThemePalette.MINT_GARDEN).background).isEqualTo(Color(0xFFEEF7F1)) }

    @Test fun `sunset glow preview primary matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.SUNSET_GLOW)[0]).isEqualTo(Color(0xFFC4673B)) }
    @Test fun `sunset glow preview secondary matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.SUNSET_GLOW)[1]).isEqualTo(Color(0xFFAF7A3A)) }
    @Test fun `sunset glow preview primary container matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.SUNSET_GLOW)[2]).isEqualTo(Color(0xFFFFDCCB)) }
    @Test fun `sunset glow preview surface variant matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.SUNSET_GLOW)[3]).isEqualTo(Color(0xFFF1DED1)) }
    @Test fun `sunset glow scheme background matches spec`() { assertThat(lightPaletteColorScheme(LightThemePalette.SUNSET_GLOW).background).isEqualTo(Color(0xFFFFF3EA)) }

    @Test fun `rose dawn preview primary matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.ROSE_DAWN)[0]).isEqualTo(Color(0xFFB34F6A)) }
    @Test fun `rose dawn preview secondary matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.ROSE_DAWN)[1]).isEqualTo(Color(0xFF8A6175)) }
    @Test fun `rose dawn preview primary container matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.ROSE_DAWN)[2]).isEqualTo(Color(0xFFFFD9E2)) }
    @Test fun `rose dawn preview surface variant matches spec`() { assertThat(lightPalettePreviewColors(LightThemePalette.ROSE_DAWN)[3]).isEqualTo(Color(0xFFF1DEE5)) }
    @Test fun `rose dawn scheme background matches spec`() { assertThat(lightPaletteColorScheme(LightThemePalette.ROSE_DAWN).background).isEqualTo(Color(0xFFFFEEF3)) }

    @Test fun `midnight blue preview primary matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.MIDNIGHT_BLUE)[0]).isEqualTo(Color(0xFF8AB4FF)) }
    @Test fun `midnight blue preview secondary matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.MIDNIGHT_BLUE)[1]).isEqualTo(Color(0xFF7FC5E0)) }
    @Test fun `midnight blue preview primary container matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.MIDNIGHT_BLUE)[2]).isEqualTo(Color(0xFF1B3F70)) }
    @Test fun `midnight blue preview surface variant matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.MIDNIGHT_BLUE)[3]).isEqualTo(Color(0xFF243040)) }
    @Test fun `midnight blue scheme background matches spec`() { assertThat(darkPaletteColorScheme(DarkThemePalette.MIDNIGHT_BLUE).background).isEqualTo(Color(0xFF0A1019)) }

    @Test fun `deep ocean preview primary matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.DEEP_OCEAN)[0]).isEqualTo(Color(0xFF6CB7D6)) }
    @Test fun `deep ocean preview secondary matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.DEEP_OCEAN)[1]).isEqualTo(Color(0xFF4EC5A8)) }
    @Test fun `deep ocean preview primary container matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.DEEP_OCEAN)[2]).isEqualTo(Color(0xFF0D4D66)) }
    @Test fun `deep ocean preview surface variant matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.DEEP_OCEAN)[3]).isEqualTo(Color(0xFF1D3238)) }
    @Test fun `deep ocean scheme background matches spec`() { assertThat(darkPaletteColorScheme(DarkThemePalette.DEEP_OCEAN).background).isEqualTo(Color(0xFF081216)) }

    @Test fun `forest night preview primary matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.FOREST_NIGHT)[0]).isEqualTo(Color(0xFF7BC58B)) }
    @Test fun `forest night preview secondary matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.FOREST_NIGHT)[1]).isEqualTo(Color(0xFFB3B56C)) }
    @Test fun `forest night preview primary container matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.FOREST_NIGHT)[2]).isEqualTo(Color(0xFF1C5630)) }
    @Test fun `forest night preview surface variant matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.FOREST_NIGHT)[3]).isEqualTo(Color(0xFF2A352C)) }
    @Test fun `forest night scheme background matches spec`() { assertThat(darkPaletteColorScheme(DarkThemePalette.FOREST_NIGHT).background).isEqualTo(Color(0xFF0B130E)) }

    @Test fun `charcoal amber preview primary matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.CHARCOAL_AMBER)[0]).isEqualTo(Color(0xFFE7A65C)) }
    @Test fun `charcoal amber preview secondary matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.CHARCOAL_AMBER)[1]).isEqualTo(Color(0xFFD4B07A)) }
    @Test fun `charcoal amber preview primary container matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.CHARCOAL_AMBER)[2]).isEqualTo(Color(0xFF684218)) }
    @Test fun `charcoal amber preview surface variant matches spec`() { assertThat(darkPalettePreviewColors(DarkThemePalette.CHARCOAL_AMBER)[3]).isEqualTo(Color(0xFF352B25)) }
    @Test fun `charcoal amber scheme background matches spec`() { assertThat(darkPaletteColorScheme(DarkThemePalette.CHARCOAL_AMBER).background).isEqualTo(Color(0xFF100D0B)) }
}
