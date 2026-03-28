package com.darksok.canvaslauncher.core.packages.di

import android.content.Context
import android.content.pm.PackageManager
import com.darksok.canvaslauncher.core.common.coroutines.DefaultDispatchersProvider
import com.darksok.canvaslauncher.core.common.coroutines.DispatchersProvider
import com.darksok.canvaslauncher.core.packages.events.InMemoryPackageEventsBus
import com.darksok.canvaslauncher.core.packages.events.PackageEventsBus
import com.darksok.canvaslauncher.core.packages.icon.IconBitmapStore
import com.darksok.canvaslauncher.core.packages.icon.LruIconCacheGateway
import com.darksok.canvaslauncher.core.packages.launch.PackageManagerAppLaunchService
import com.darksok.canvaslauncher.core.packages.source.PackageManagerInstalledAppsSource
import com.darksok.canvaslauncher.domain.repository.AppLaunchService
import com.darksok.canvaslauncher.domain.repository.IconCacheGateway
import com.darksok.canvaslauncher.domain.repository.InstalledAppsSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PackageProvidersModule {

    @Provides
    @Singleton
    fun providePackageManager(
        @ApplicationContext context: Context,
    ): PackageManager = context.packageManager

    @Provides
    @Singleton
    fun provideDispatchersProvider(): DispatchersProvider = DefaultDispatchersProvider()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PackageBindingsModule {

    @Binds
    @Singleton
    abstract fun bindInstalledAppsSource(
        impl: PackageManagerInstalledAppsSource,
    ): InstalledAppsSource

    @Binds
    @Singleton
    abstract fun bindAppLaunchService(
        impl: PackageManagerAppLaunchService,
    ): AppLaunchService

    @Binds
    @Singleton
    abstract fun bindIconCacheGateway(
        impl: LruIconCacheGateway,
    ): IconCacheGateway

    @Binds
    @Singleton
    abstract fun bindIconBitmapStore(
        impl: LruIconCacheGateway,
    ): IconBitmapStore

    @Binds
    @Singleton
    abstract fun bindPackageEventsBus(
        impl: InMemoryPackageEventsBus,
    ): PackageEventsBus
}
