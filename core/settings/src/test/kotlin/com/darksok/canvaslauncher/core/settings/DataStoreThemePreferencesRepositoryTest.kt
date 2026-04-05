package com.darksok.canvaslauncher.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.darksok.canvaslauncher.core.model.ui.AppLayoutMode
import com.darksok.canvaslauncher.core.model.ui.DarkThemePalette
import com.darksok.canvaslauncher.core.model.ui.LightThemePalette
import com.darksok.canvaslauncher.core.model.ui.ThemeMode
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DataStoreThemePreferencesRepositoryTest {

    @Test
    fun `observe theme mode defaults to system when datastore empty`() = runTest {
        val repository = DataStoreThemePreferencesRepository(FakePreferencesDataStore())

        assertThat(repository.observeThemeMode().first()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `observe light palette defaults to sky breeze when datastore empty`() = runTest {
        val repository = DataStoreThemePreferencesRepository(FakePreferencesDataStore())

        assertThat(repository.observeLightThemePalette().first()).isEqualTo(LightThemePalette.SKY_BREEZE)
    }

    @Test
    fun `observe dark palette defaults to midnight blue when datastore empty`() = runTest {
        val repository = DataStoreThemePreferencesRepository(FakePreferencesDataStore())

        assertThat(repository.observeDarkThemePalette().first()).isEqualTo(DarkThemePalette.MIDNIGHT_BLUE)
    }

    @Test
    fun `observe layout mode defaults to spiral when datastore empty`() = runTest {
        val repository = DataStoreThemePreferencesRepository(FakePreferencesDataStore())

        assertThat(repository.observeLayoutMode().first()).isEqualTo(AppLayoutMode.SPIRAL)
    }

    @Test
    fun `set theme mode stores and emits new value`() = runTest {
        val store = FakePreferencesDataStore()
        val repository = DataStoreThemePreferencesRepository(store)

        repository.setThemeMode(ThemeMode.DARK)

        assertThat(repository.observeThemeMode().first()).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `set theme mode overwrites previous value`() = runTest {
        val store = FakePreferencesDataStore()
        val repository = DataStoreThemePreferencesRepository(store)
        repository.setThemeMode(ThemeMode.LIGHT)

        repository.setThemeMode(ThemeMode.SYSTEM)

        assertThat(repository.observeThemeMode().first()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `set light palette stores and emits new value`() = runTest {
        val store = FakePreferencesDataStore()
        val repository = DataStoreThemePreferencesRepository(store)

        repository.setLightThemePalette(LightThemePalette.ROSE_DAWN)

        assertThat(repository.observeLightThemePalette().first()).isEqualTo(LightThemePalette.ROSE_DAWN)
    }

    @Test
    fun `set dark palette stores and emits new value`() = runTest {
        val store = FakePreferencesDataStore()
        val repository = DataStoreThemePreferencesRepository(store)

        repository.setDarkThemePalette(DarkThemePalette.FOREST_NIGHT)

        assertThat(repository.observeDarkThemePalette().first()).isEqualTo(DarkThemePalette.FOREST_NIGHT)
    }

    @Test
    fun `set layout mode stores and emits new value`() = runTest {
        val store = FakePreferencesDataStore()
        val repository = DataStoreThemePreferencesRepository(store)

        repository.setLayoutMode(AppLayoutMode.SMART_AUTO)

        assertThat(repository.observeLayoutMode().first()).isEqualTo(AppLayoutMode.SMART_AUTO)
    }

    @Test
    fun `repository preserves unrelated keys when theme mode changes`() = runTest {
        val store = FakePreferencesDataStore(
            initial = preferencesOf(LIGHT_THEME_KEY to LightThemePalette.MINT_GARDEN.toStoredValue()),
        )
        val repository = DataStoreThemePreferencesRepository(store)

        repository.setThemeMode(ThemeMode.DARK)

        assertThat(repository.observeLightThemePalette().first()).isEqualTo(LightThemePalette.MINT_GARDEN)
    }

    @Test
    fun `repository preserves unrelated keys when layout mode changes`() = runTest {
        val store = FakePreferencesDataStore(
            initial = preferencesOf(DARK_THEME_KEY to DarkThemePalette.DEEP_OCEAN.toStoredValue()),
        )
        val repository = DataStoreThemePreferencesRepository(store)

        repository.setLayoutMode(AppLayoutMode.CIRCLE)

        assertThat(repository.observeDarkThemePalette().first()).isEqualTo(DarkThemePalette.DEEP_OCEAN)
    }

    @Test
    fun `io exception while observing theme mode emits default`() = runTest {
        val repository = DataStoreThemePreferencesRepository(ThrowingDataStore(IOException("boom")))

        assertThat(repository.observeThemeMode().first()).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `io exception while observing light palette emits default`() = runTest {
        val repository = DataStoreThemePreferencesRepository(ThrowingDataStore(IOException("boom")))

        assertThat(repository.observeLightThemePalette().first()).isEqualTo(LightThemePalette.SKY_BREEZE)
    }

    @Test
    fun `io exception while observing dark palette emits default`() = runTest {
        val repository = DataStoreThemePreferencesRepository(ThrowingDataStore(IOException("boom")))

        assertThat(repository.observeDarkThemePalette().first()).isEqualTo(DarkThemePalette.MIDNIGHT_BLUE)
    }

    @Test
    fun `io exception while observing layout mode emits default`() = runTest {
        val repository = DataStoreThemePreferencesRepository(ThrowingDataStore(IOException("boom")))

        assertThat(repository.observeLayoutMode().first()).isEqualTo(AppLayoutMode.SPIRAL)
    }

    @Test(expected = IllegalStateException::class)
    fun `non io exception while observing theme mode is rethrown`() = runTest {
        val repository = DataStoreThemePreferencesRepository(ThrowingDataStore(IllegalStateException("boom")))

        repository.observeThemeMode().first()
    }

    @Test
    fun `repository emits latest values after multiple updates`() = runTest {
        val store = FakePreferencesDataStore()
        val repository = DataStoreThemePreferencesRepository(store)

        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setLightThemePalette(LightThemePalette.SUNSET_GLOW)
        repository.setDarkThemePalette(DarkThemePalette.CHARCOAL_AMBER)
        repository.setLayoutMode(AppLayoutMode.ICON_COLOR)

        assertThat(repository.observeThemeMode().first()).isEqualTo(ThemeMode.LIGHT)
        assertThat(repository.observeLightThemePalette().first()).isEqualTo(LightThemePalette.SUNSET_GLOW)
        assertThat(repository.observeDarkThemePalette().first()).isEqualTo(DarkThemePalette.CHARCOAL_AMBER)
        assertThat(repository.observeLayoutMode().first()).isEqualTo(AppLayoutMode.ICON_COLOR)
    }

    @Test
    fun `repository updates only targeted key when changing light palette repeatedly`() = runTest {
        val store = FakePreferencesDataStore()
        val repository = DataStoreThemePreferencesRepository(store)
        repository.setThemeMode(ThemeMode.DARK)

        repository.setLightThemePalette(LightThemePalette.MINT_GARDEN)
        repository.setLightThemePalette(LightThemePalette.ROSE_DAWN)

        assertThat(repository.observeThemeMode().first()).isEqualTo(ThemeMode.DARK)
        assertThat(repository.observeLightThemePalette().first()).isEqualTo(LightThemePalette.ROSE_DAWN)
    }

    private class FakePreferencesDataStore(
        initial: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private class ThrowingDataStore(
        private val throwable: Throwable,
    ) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw throwable }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            throw UnsupportedOperationException("Not needed for this test")
        }
    }

    private companion object {
        val LIGHT_THEME_KEY = androidx.datastore.preferences.core.stringPreferencesKey("light_theme_palette")
        val DARK_THEME_KEY = androidx.datastore.preferences.core.stringPreferencesKey("dark_theme_palette")

        fun preferencesOf(vararg pairs: Preferences.Pair<*>): Preferences {
            return mutablePreferencesOf(*pairs)
        }
    }
}
