package com.darksok.canvaslauncher.core.packages.icon

import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

interface IconBitmapStore {
    val icons: StateFlow<Map<String, Bitmap>>
    fun getCached(packageName: String): Bitmap?
}
