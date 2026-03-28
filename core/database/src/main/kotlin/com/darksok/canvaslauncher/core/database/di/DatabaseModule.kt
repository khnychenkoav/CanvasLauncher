package com.darksok.canvaslauncher.core.database.di

import android.content.Context
import androidx.room.Room
import com.darksok.canvaslauncher.core.database.CanvasLauncherDatabase
import com.darksok.canvaslauncher.core.database.DatabaseMigrations
import com.darksok.canvaslauncher.core.database.dao.AppDao
import com.darksok.canvaslauncher.core.database.dao.CanvasEditDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "canvas_launcher.db"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): CanvasLauncherDatabase {
        return Room.databaseBuilder(
            context,
            CanvasLauncherDatabase::class.java,
            DATABASE_NAME,
        )
            .addMigrations(*DatabaseMigrations.ALL)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideAppDao(database: CanvasLauncherDatabase): AppDao = database.appDao()

    @Provides
    fun provideCanvasEditDao(database: CanvasLauncherDatabase): CanvasEditDao = database.canvasEditDao()
}
