package com.darksok.canvaslauncher.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Proxy
import org.junit.Test

class DatabaseMigrationsTest {

    @Test
    fun `all migrations are exposed in order`() {
        assertThat(DatabaseMigrations.ALL.map { it.startVersion to it.endVersion }).containsExactly(
            1 to 2,
            2 to 3,
            3 to 4,
        ).inOrder()
    }

    @Test
    fun `all migrations count matches expected versions`() {
        assertThat(DatabaseMigrations.ALL).hasLength(3)
    }

    @Test
    fun `migration one to two creates sticky notes table`() {
        assertContainsSql(runMigration(DatabaseMigrations.ALL[0]), "CREATE TABLE IF NOT EXISTS `canvas_sticky_notes_table`")
    }

    @Test
    fun `migration one to two creates text objects table`() {
        assertContainsSql(runMigration(DatabaseMigrations.ALL[0]), "CREATE TABLE IF NOT EXISTS `canvas_text_objects_table`")
    }

    @Test
    fun `migration one to two creates frame objects table`() {
        assertContainsSql(runMigration(DatabaseMigrations.ALL[0]), "CREATE TABLE IF NOT EXISTS `canvas_frame_objects_table`")
    }

    @Test
    fun `migration one to two creates strokes table`() {
        assertContainsSql(runMigration(DatabaseMigrations.ALL[0]), "CREATE TABLE IF NOT EXISTS `canvas_strokes_table`")
    }

    @Test
    fun `migration one to two creates stroke points table`() {
        assertContainsSql(runMigration(DatabaseMigrations.ALL[0]), "CREATE TABLE IF NOT EXISTS `canvas_stroke_points_table`")
    }

    @Test
    fun `migration one to two creates stroke points index`() {
        assertContainsSql(runMigration(DatabaseMigrations.ALL[0]), "CREATE INDEX IF NOT EXISTS `index_canvas_stroke_points_table_strokeId`")
    }

    @Test
    fun `migration two to three adds text size column with default`() {
        assertThat(runMigration(DatabaseMigrations.ALL[1])).containsExactly(
            "ALTER TABLE `canvas_sticky_notes_table` ADD COLUMN `textSizeWorld` REAL NOT NULL DEFAULT 44.0",
        )
    }

    @Test
    fun `migration three to four creates widgets table`() {
        assertContainsSql(runMigration(DatabaseMigrations.ALL[2]), "CREATE TABLE IF NOT EXISTS `canvas_widgets_table`")
    }

    private fun runMigration(migration: Migration): List<String> {
        val statements = mutableListOf<String>()
        val database = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    statements += (args?.firstOrNull() as String).trimIndent().replace("\r\n", "\n")
                    null
                }
                else -> throw UnsupportedOperationException("Not needed for this test: ${method.name}")
            }
        } as SupportSQLiteDatabase
        migration.migrate(database)
        return statements
    }

    private fun assertContainsSql(statements: List<String>, fragment: String) {
        assertThat(statements.any { it.contains(fragment) }).isTrue()
    }
}
