package com.darksok.canvaslauncher.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.darksok.canvaslauncher.core.database.dao.AppDao
import com.darksok.canvaslauncher.core.database.entity.AppEntity

@Database(
    entities = [AppEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CanvasLauncherDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
}
