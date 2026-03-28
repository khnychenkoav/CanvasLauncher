package com.darksok.canvaslauncher.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "canvas_text_objects_table")
data class CanvasTextObjectEntity(
    @PrimaryKey val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val textSizeWorld: Float,
    val colorArgb: Int,
)

