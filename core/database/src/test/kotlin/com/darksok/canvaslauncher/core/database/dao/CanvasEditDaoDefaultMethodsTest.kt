package com.darksok.canvaslauncher.core.database.dao

import com.darksok.canvaslauncher.core.database.entity.CanvasFrameObjectEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStickyNoteEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokeEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasStrokePointEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasTextObjectEntity
import com.darksok.canvaslauncher.core.database.entity.CanvasWidgetEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CanvasEditDaoDefaultMethodsTest {

    @Test
    fun `upsert stroke with points upserts the stroke`() = runTest {
        val dao = RecordingCanvasEditDao()
        val stroke = stroke(id = "s1")

        dao.upsertStrokeWithPoints(stroke, listOf(point("s1", 0)))

        assertThat(dao.upsertedStrokes).containsExactly(stroke)
    }

    @Test
    fun `upsert stroke with points deletes previous points for stroke id`() = runTest {
        val dao = RecordingCanvasEditDao()

        dao.upsertStrokeWithPoints(stroke(id = "stroke-x"), listOf(point("stroke-x", 0)))

        assertThat(dao.deletedStrokePointIds).containsExactly("stroke-x")
    }

    @Test
    fun `upsert stroke with points inserts all provided points`() = runTest {
        val dao = RecordingCanvasEditDao()
        val points = listOf(point("s1", 0), point("s1", 1))

        dao.upsertStrokeWithPoints(stroke(id = "s1"), points)

        assertThat(dao.insertedPointBatches).containsExactly(points)
    }

    @Test
    fun `upsert stroke with points skips insert when list empty`() = runTest {
        val dao = RecordingCanvasEditDao()

        dao.upsertStrokeWithPoints(stroke(id = "s1"), emptyList())

        assertThat(dao.insertedPointBatches).isEmpty()
    }

    @Test
    fun `upsert stroke with points keeps point order intact`() = runTest {
        val dao = RecordingCanvasEditDao()
        val points = listOf(point("s1", 2), point("s1", 1), point("s1", 0))

        dao.upsertStrokeWithPoints(stroke(id = "s1"), points)

        assertThat(dao.insertedPointBatches.single().map { it.pointIndex }).containsExactly(2, 1, 0).inOrder()
    }

    @Test
    fun `upsert stroke with points deletes using stroke id even when points reference different ids`() = runTest {
        val dao = RecordingCanvasEditDao()

        dao.upsertStrokeWithPoints(
            stroke(id = "expected"),
            listOf(point("other", 0)),
        )

        assertThat(dao.deletedStrokePointIds).containsExactly("expected")
    }

    @Test
    fun `clear all custom elements deletes strokes`() = runTest {
        val dao = RecordingCanvasEditDao()

        dao.clearAllCustomElements()

        assertThat(dao.operations).contains("deleteAllStrokes")
    }

    @Test
    fun `clear all custom elements deletes sticky notes`() = runTest {
        val dao = RecordingCanvasEditDao()

        dao.clearAllCustomElements()

        assertThat(dao.operations).contains("deleteAllStickyNotes")
    }

    @Test
    fun `clear all custom elements deletes text objects`() = runTest {
        val dao = RecordingCanvasEditDao()

        dao.clearAllCustomElements()

        assertThat(dao.operations).contains("deleteAllTextObjects")
    }

    @Test
    fun `clear all custom elements deletes frame objects`() = runTest {
        val dao = RecordingCanvasEditDao()

        dao.clearAllCustomElements()

        assertThat(dao.operations).contains("deleteAllFrameObjects")
    }

    @Test
    fun `clear all custom elements deletes widgets`() = runTest {
        val dao = RecordingCanvasEditDao()

        dao.clearAllCustomElements()

        assertThat(dao.operations).contains("deleteAllWidgets")
    }

    @Test
    fun `clear all custom elements uses stable delete order`() = runTest {
        val dao = RecordingCanvasEditDao()

        dao.clearAllCustomElements()

        assertThat(dao.operations).containsExactly(
            "deleteAllStrokes",
            "deleteAllStickyNotes",
            "deleteAllTextObjects",
            "deleteAllFrameObjects",
            "deleteAllWidgets",
        ).inOrder()
    }

    private fun stroke(id: String) = CanvasStrokeEntity(id = id, colorArgb = 0x123456, widthWorld = 4f)

    private fun point(strokeId: String, index: Int) = CanvasStrokePointEntity(strokeId, index, index.toFloat(), index.toFloat())

    private class RecordingCanvasEditDao : CanvasEditDao {
        val upsertedStrokes = mutableListOf<CanvasStrokeEntity>()
        val deletedStrokePointIds = mutableListOf<String>()
        val insertedPointBatches = mutableListOf<List<CanvasStrokePointEntity>>()
        val operations = mutableListOf<String>()

        override suspend fun getStickyNotes(): List<CanvasStickyNoteEntity> = emptyList()
        override suspend fun getTextObjects(): List<CanvasTextObjectEntity> = emptyList()
        override suspend fun getFrameObjects(): List<CanvasFrameObjectEntity> = emptyList()
        override suspend fun getWidgets(): List<CanvasWidgetEntity> = emptyList()
        override suspend fun getStrokesWithPoints(): List<CanvasStrokeWithPointsEntity> = emptyList()
        override suspend fun upsertStickyNote(note: CanvasStickyNoteEntity) = Unit
        override suspend fun upsertTextObject(textObject: CanvasTextObjectEntity) = Unit
        override suspend fun upsertFrameObject(frameObject: CanvasFrameObjectEntity) = Unit
        override suspend fun upsertWidget(widget: CanvasWidgetEntity) = Unit

        override suspend fun upsertStroke(stroke: CanvasStrokeEntity) {
            upsertedStrokes += stroke
        }

        override suspend fun insertStrokePoints(points: List<CanvasStrokePointEntity>) {
            insertedPointBatches += points
        }

        override suspend fun deleteStrokePointsByStrokeId(strokeId: String) {
            deletedStrokePointIds += strokeId
        }

        override suspend fun deleteStrokeById(strokeId: String) = Unit
        override suspend fun deleteStickyNoteById(id: String) = Unit
        override suspend fun deleteTextObjectById(id: String) = Unit
        override suspend fun deleteFrameObjectById(id: String) = Unit
        override suspend fun deleteWidgetById(id: String) = Unit

        override suspend fun deleteAllStrokes() {
            operations += "deleteAllStrokes"
        }

        override suspend fun deleteAllStickyNotes() {
            operations += "deleteAllStickyNotes"
        }

        override suspend fun deleteAllTextObjects() {
            operations += "deleteAllTextObjects"
        }

        override suspend fun deleteAllFrameObjects() {
            operations += "deleteAllFrameObjects"
        }

        override suspend fun deleteAllWidgets() {
            operations += "deleteAllWidgets"
        }
    }
}
