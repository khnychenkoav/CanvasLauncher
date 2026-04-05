package com.darksok.canvaslauncher.core.database.entity

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class RoomMetadataTest {

    @Test
    fun `database source uses version four`() {
        assertThat(databaseSource()).contains("version = 4")
    }

    @Test
    fun `database source exports schema`() {
        assertThat(databaseSource()).contains("exportSchema = true")
    }

    @Test
    fun `database source lists app entity`() {
        assertThat(databaseSource()).contains("AppEntity::class")
    }

    @Test
    fun `database source lists sticky note entity`() {
        assertThat(databaseSource()).contains("CanvasStickyNoteEntity::class")
    }

    @Test
    fun `database source lists text object entity`() {
        assertThat(databaseSource()).contains("CanvasTextObjectEntity::class")
    }

    @Test
    fun `database source lists frame object entity`() {
        assertThat(databaseSource()).contains("CanvasFrameObjectEntity::class")
    }

    @Test
    fun `database source lists widget entity`() {
        assertThat(databaseSource()).contains("CanvasWidgetEntity::class")
    }

    @Test
    fun `database source lists stroke entity`() {
        assertThat(databaseSource()).contains("CanvasStrokeEntity::class")
    }

    @Test
    fun `database source lists stroke point entity`() {
        assertThat(databaseSource()).contains("CanvasStrokePointEntity::class")
    }

    @Test
    fun `app entity uses apps table and primary key`() {
        val source = sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/entity/AppEntity.kt")

        assertThat(source).contains("@Entity(tableName = \"apps_table\")")
        assertThat(source).contains("@PrimaryKey val packageName: String")
    }

    @Test
    fun `sticky note entity uses sticky notes table`() {
        val source = sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/entity/CanvasStickyNoteEntity.kt")

        assertThat(source).contains("@Entity(tableName = \"canvas_sticky_notes_table\")")
        assertThat(source).contains("val textSizeWorld: Float")
    }

    @Test
    fun `text object entity uses text objects table`() {
        assertThat(sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/entity/CanvasTextObjectEntity.kt"))
            .contains("@Entity(tableName = \"canvas_text_objects_table\")")
    }

    @Test
    fun `frame object entity uses frame objects table`() {
        assertThat(sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/entity/CanvasFrameObjectEntity.kt"))
            .contains("@Entity(tableName = \"canvas_frame_objects_table\")")
    }

    @Test
    fun `widget entity uses widgets table`() {
        assertThat(sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/entity/CanvasWidgetEntity.kt"))
            .contains("@Entity(tableName = \"canvas_widgets_table\")")
    }

    @Test
    fun `stroke entity uses strokes table`() {
        assertThat(sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/entity/CanvasStrokeEntity.kt"))
            .contains("@Entity(tableName = \"canvas_strokes_table\")")
    }

    @Test
    fun `stroke points entity defines composite key and cascade`() {
        val source = sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/entity/CanvasStrokePointEntity.kt")

        assertThat(source).contains("primaryKeys = [\"strokeId\", \"pointIndex\"]")
        assertThat(source).contains("onDelete = ForeignKey.CASCADE")
    }

    @Test
    fun `stroke points entity indexes stroke id`() {
        assertThat(sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/entity/CanvasStrokePointEntity.kt"))
            .contains("indices = [Index(\"strokeId\")]")
    }

    @Test
    fun `stroke with points relation maps id to strokeId`() {
        val source = sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/dao/CanvasStrokeWithPointsEntity.kt")

        assertThat(source).contains("@Embedded val stroke: CanvasStrokeEntity")
        assertThat(source).contains("parentColumn = \"id\"")
        assertThat(source).contains("entityColumn = \"strokeId\"")
    }

    private fun databaseSource(): String = sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/CanvasLauncherDatabase.kt")

    private fun sourceOf(relativePath: String): String {
        val moduleFile = File(relativePath)
        val rootFile = File("core/database/$relativePath")
        val file = when {
            moduleFile.exists() -> moduleFile
            rootFile.exists() -> rootFile
            else -> error("Missing source file for test: $relativePath")
        }
        return file.readText()
    }
}
