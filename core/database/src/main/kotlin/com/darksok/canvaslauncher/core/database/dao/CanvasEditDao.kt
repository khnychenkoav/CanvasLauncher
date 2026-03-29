package com.darksok.canvaslauncher.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.darksok.canvaslauncher.core.database.entity.CanvasFrameObjectEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStickyNoteEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokeEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokePointEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasTextObjectEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasWidgetEntity

@Dao
interface CanvasEditDao {

    @Query("SELECT * FROM canvas_sticky_notes_table")
    suspend fun getStickyNotes(): List<CanvasStickyNoteEntity>

    @Query("SELECT * FROM canvas_text_objects_table")
    suspend fun getTextObjects(): List<CanvasTextObjectEntity>

    @Query("SELECT * FROM canvas_frame_objects_table")
    suspend fun getFrameObjects(): List<CanvasFrameObjectEntity>

    @Query("SELECT * FROM canvas_widgets_table")
    suspend fun getWidgets(): List<CanvasWidgetEntity>

    @Transaction
    @Query("SELECT * FROM canvas_strokes_table")
    suspend fun getStrokesWithPoints(): List<CanvasStrokeWithPointsEntity>

    @Upsert
    suspend fun upsertStickyNote(note: CanvasStickyNoteEntity)

    @Upsert
    suspend fun upsertTextObject(textObject: CanvasTextObjectEntity)

    @Upsert
    suspend fun upsertFrameObject(frameObject: CanvasFrameObjectEntity)

    @Upsert
    suspend fun upsertWidget(widget: CanvasWidgetEntity)

    @Upsert
    suspend fun upsertStroke(stroke: CanvasStrokeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrokePoints(points: List<CanvasStrokePointEntity>)

    @Query("DELETE FROM canvas_stroke_points_table WHERE strokeId = :strokeId")
    suspend fun deleteStrokePointsByStrokeId(strokeId: String)

    @Query("DELETE FROM canvas_strokes_table WHERE id = :strokeId")
    suspend fun deleteStrokeById(strokeId: String)

    @Query("DELETE FROM canvas_sticky_notes_table WHERE id = :id")
    suspend fun deleteStickyNoteById(id: String)

    @Query("DELETE FROM canvas_text_objects_table WHERE id = :id")
    suspend fun deleteTextObjectById(id: String)

    @Query("DELETE FROM canvas_frame_objects_table WHERE id = :id")
    suspend fun deleteFrameObjectById(id: String)

    @Query("DELETE FROM canvas_widgets_table WHERE id = :id")
    suspend fun deleteWidgetById(id: String)

    @Query("DELETE FROM canvas_strokes_table")
    suspend fun deleteAllStrokes()

    @Query("DELETE FROM canvas_sticky_notes_table")
    suspend fun deleteAllStickyNotes()

    @Query("DELETE FROM canvas_text_objects_table")
    suspend fun deleteAllTextObjects()

    @Query("DELETE FROM canvas_frame_objects_table")
    suspend fun deleteAllFrameObjects()

    @Query("DELETE FROM canvas_widgets_table")
    suspend fun deleteAllWidgets()

    @Transaction
    suspend fun upsertStrokeWithPoints(
        stroke: CanvasStrokeEntity,
        points: List<CanvasStrokePointEntity>,
    ) {
        upsertStroke(stroke)
        deleteStrokePointsByStrokeId(stroke.id)
        if (points.isNotEmpty()) {
            insertStrokePoints(points)
        }
    }

    @Transaction
    suspend fun clearAllCustomElements() {
        deleteAllStrokes()
        deleteAllStickyNotes()
        deleteAllTextObjects()
        deleteAllFrameObjects()
        deleteAllWidgets()
    }
}
