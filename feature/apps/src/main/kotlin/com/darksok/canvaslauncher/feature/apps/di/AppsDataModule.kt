package com.darksok.canvaslauncher.feature.apps.di

import com.darksok.canvaslauncher.domain.layout.InitialLayoutStrategy
import com.darksok.canvaslauncher.domain.layout.MultiPatternLayoutStrategy
import com.darksok.canvaslauncher.domain.repository.CanvasAppsStore
import com.darksok.canvaslauncher.feature.apps.data.RoomCanvasAppsStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppsDataBindingsModule {

    @Binds
    @Singleton
    abstract fun bindCanvasAppsStore(
        impl: RoomCanvasAppsStore,
    ): CanvasAppsStore
}

@Module
@InstallIn(SingletonComponent::class)
object AppsDataProvidersModule {

    @Provides
    @Singleton
    fun provideInitialLayoutStrategy(): InitialLayoutStrategy = MultiPatternLayoutStrategy()
}
