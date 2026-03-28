package com.darksok.canvaslauncher.feature.launcher.di

import com.darksok.canvaslauncher.feature.canvas.CanvasGestureHandler
import com.darksok.canvaslauncher.feature.canvas.DefaultCanvasGestureHandler
import com.darksok.canvaslauncher.feature.canvas.DefaultDragDropController
import com.darksok.canvaslauncher.feature.canvas.DefaultViewportController
import com.darksok.canvaslauncher.feature.canvas.DragDropController
import com.darksok.canvaslauncher.feature.canvas.ViewportController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object LauncherPresentationModule {

    @Provides
    @ViewModelScoped
    fun provideViewportController(): ViewportController = DefaultViewportController()

    @Provides
    @ViewModelScoped
    fun provideDragDropController(): DragDropController = DefaultDragDropController()

    @Provides
    @ViewModelScoped
    fun provideCanvasGestureHandler(
        viewportController: ViewportController,
    ): CanvasGestureHandler = DefaultCanvasGestureHandler(viewportController)
}
