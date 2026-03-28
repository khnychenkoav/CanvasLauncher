package com.darksok.canvaslauncher.core.settings.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.darksok.canvaslauncher.core.settings.DataStoreThemePreferencesRepository
import com.darksok.canvaslauncher.domain.repository.LayoutPreferencesRepository
import com.darksok.canvaslauncher.domain.repository.ThemePreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsProvidersModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("canvas_launcher_settings.preferences_pb") },
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindingsModule {

    @Binds
    @Singleton
    abstract fun bindThemePreferencesRepository(
        impl: DataStoreThemePreferencesRepository,
    ): ThemePreferencesRepository

    @Binds
    abstract fun bindLayoutPreferencesRepository(
        impl: DataStoreThemePreferencesRepository,
    ): LayoutPreferencesRepository
}
