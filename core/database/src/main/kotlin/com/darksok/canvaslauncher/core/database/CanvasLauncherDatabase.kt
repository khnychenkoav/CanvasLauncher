package com.darksok.canvaslauncher.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.darksok.canvaslauncher.core.database.dao.AppDao
import com.darksok.canvaslauncher.core.database.dao.CanvasEditDao
import com.darksok.canvaslauncher.core.database.entity.AppEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasFrameObjectEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStickyNoteEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokeEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokePointEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasTextObjectEntity

@Database(
    entities = [
        AppEntity::class,
        CanvasStickyNoteEntity::class,
        CanvasTextObjectEntity::class,
        CanvasFrameObjectEntity::class,
        CanvasStrokeEntity::class,
        CanvasStrokePointEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class CanvasLauncherDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun canvasEditDao(): CanvasEditDao
}
