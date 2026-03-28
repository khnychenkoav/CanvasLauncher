package com.darksok.canvaslauncher.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.darksok.canvaslauncher.domain.repository.LayoutPreferencesRepository
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreThemePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferencesRepository, LayoutPreferencesRepository {

    override fun observeThemeMode(): Flow<ThemeMode> {
        return dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { prefs -> prefs[THEME_MODE_KEY].toThemeModeOrDefault() }
    }

    override fun observeLightThemePalette(): Flow<LightThemePalette> {
        return dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { prefs -> prefs[LIGHT_THEME_PALETTE_KEY].toLightThemePaletteOrDefault() }
    }

    override fun observeDarkThemePalette(): Flow<DarkThemePalette> {
        return dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { prefs -> prefs[DARK_THEME_PALETTE_KEY].toDarkThemePaletteOrDefault() }
    }

    override fun observeLayoutMode(): Flow<AppLayoutMode> {
        return dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { prefs -> prefs[LAYOUT_MODE_KEY].toLayoutModeOrDefault() }
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = themeMode.toStoredValue()
        }
    }

    override suspend fun setLightThemePalette(palette: LightThemePalette) {
        dataStore.edit { prefs ->
            prefs[LIGHT_THEME_PALETTE_KEY] = palette.toStoredValue()
        }
    }

    override suspend fun setDarkThemePalette(palette: DarkThemePalette) {
        dataStore.edit { prefs ->
            prefs[DARK_THEME_PALETTE_KEY] = palette.toStoredValue()
        }
    }

    override suspend fun setLayoutMode(layoutMode: AppLayoutMode) {
        dataStore.edit { prefs ->
            prefs[LAYOUT_MODE_KEY] = layoutMode.toStoredValue()
        }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val LIGHT_THEME_PALETTE_KEY = stringPreferencesKey("light_theme_palette")
        val DARK_THEME_PALETTE_KEY = stringPreferencesKey("dark_theme_palette")
        val LAYOUT_MODE_KEY = stringPreferencesKey("layout_mode")
    }
}
