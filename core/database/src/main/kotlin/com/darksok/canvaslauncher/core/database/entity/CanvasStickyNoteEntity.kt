package com.darksok.canvaslauncher.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "canvas_sticky_notes_table")
data class CanvasStickyNoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val centerX: Float,
    val centerY: Float,
    val sizeWorld: Float,
    val colorArgb: Int,
)

