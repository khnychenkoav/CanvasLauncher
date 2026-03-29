package com.darksok.canvaslauncher.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `canvas_sticky_notes_table` (
                    `id` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `centerX` REAL NOT NULL,
                    `centerY` REAL NOT NULL,
                    `sizeWorld` REAL NOT NULL,
                    `colorArgb` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `canvas_text_objects_table` (
                    `id` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `x` REAL NOT NULL,
                    `y` REAL NOT NULL,
                    `textSizeWorld` REAL NOT NULL,
                    `colorArgb` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `canvas_frame_objects_table` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `centerX` REAL NOT NULL,
                    `centerY` REAL NOT NULL,
                    `widthWorld` REAL NOT NULL,
                    `heightWorld` REAL NOT NULL,
                    `colorArgb` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `canvas_strokes_table` (
                    `id` TEXT NOT NULL,
                    `colorArgb` INTEGER NOT NULL,
                    `widthWorld` REAL NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `canvas_stroke_points_table` (
                    `strokeId` TEXT NOT NULL,
                    `pointIndex` INTEGER NOT NULL,
                    `x` REAL NOT NULL,
                    `y` REAL NOT NULL,
                    PRIMARY KEY(`strokeId`, `pointIndex`),
                    FOREIGN KEY(`strokeId`) REFERENCES `canvas_strokes_table`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_canvas_stroke_points_table_strokeId` ON `canvas_stroke_points_table` (`strokeId`)",
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE `canvas_sticky_notes_table` ADD COLUMN `textSizeWorld` REAL NOT NULL DEFAULT 44.0",
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `canvas_widgets_table` (
                    `id` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `centerX` REAL NOT NULL,
                    `centerY` REAL NOT NULL,
                    `widthWorld` REAL NOT NULL,
                    `heightWorld` REAL NOT NULL,
                    `colorArgb` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
