package com.darksok.canvaslauncher.core.database

import androidx.room.migration.Migration

object DatabaseMigrations {
    /**
     * Keep explicit migrations here when schema version is incremented.
     * MVP starts at v1, so migration array is intentionally empty.
     */
    val ALL: Array<Migration> = emptyArray()
}
