package com.darksok.canvaslauncher.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps_table")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val x: Float,
    val y: Float,
)
