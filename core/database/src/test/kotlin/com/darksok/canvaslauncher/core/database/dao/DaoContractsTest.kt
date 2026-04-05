package com.darksok.canvaslauncher.core.database.dao

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class DaoContractsTest {

    @Test
    fun `app dao observe apps query selects from apps table`() {
        assertThat(appDaoSource()).contains("@Query(\"SELECT * FROM apps_table\")\n    fun observeApps()")
    }

    @Test
    fun `app dao get apps once query selects from apps table`() {
        assertThat(appDaoSource()).contains("@Query(\"SELECT * FROM apps_table\")\n    suspend fun getAppsOnce()")
    }

    @Test
    fun `app dao batch upsert uses upsert annotation`() {
        assertThat(appDaoSource()).contains("@Upsert\n    suspend fun upsert(apps: List<AppEntity>)")
    }

    @Test
    fun `app dao single upsert uses upsert annotation`() {
        assertThat(appDaoSource()).contains("@Upsert\n    suspend fun upsert(app: AppEntity)")
    }

    @Test
    fun `app dao delete by package names uses in clause`() {
        assertThat(appDaoSource()).contains("DELETE FROM apps_table WHERE packageName IN (:packageNames)")
    }

    @Test
    fun `app dao delete by package name uses equality clause`() {
        assertThat(appDaoSource()).contains("DELETE FROM apps_table WHERE packageName = :packageName")
    }

    @Test
    fun `app dao update position updates both coordinates`() {
        assertThat(appDaoSource()).contains("UPDATE apps_table SET x = :x, y = :y WHERE packageName = :packageName")
    }

    @Test
    fun `canvas edit dao get strokes with points is transactional`() {
        assertThat(canvasEditDaoSource()).contains("@Transaction\n    @Query(\"SELECT * FROM canvas_strokes_table\")\n    suspend fun getStrokesWithPoints()")
    }

    @Test
    fun `canvas edit dao insert stroke points uses replace strategy`() {
        assertThat(canvasEditDaoSource()).contains("@Insert(onConflict = OnConflictStrategy.REPLACE)")
    }

    @Test
    fun `canvas edit dao upsert stroke with points is transactional`() {
        assertThat(canvasEditDaoSource()).contains("@Transaction\n    suspend fun upsertStrokeWithPoints(")
    }

    @Test
    fun `canvas edit dao clear all custom elements is transactional`() {
        assertThat(canvasEditDaoSource()).contains("@Transaction\n    suspend fun clearAllCustomElements()")
    }

    @Test
    fun `canvas edit dao clear all custom elements deletes widgets last`() {
        assertThat(canvasEditDaoSource()).contains("deleteAllFrameObjects()\n        deleteAllWidgets()")
    }

    private fun appDaoSource(): String = sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/dao/AppDao.kt")

    private fun canvasEditDaoSource(): String = sourceOf("src/main/kotlin/com/darksok/canvaslauncher/core/database/dao/CanvasEditDao.kt")

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
