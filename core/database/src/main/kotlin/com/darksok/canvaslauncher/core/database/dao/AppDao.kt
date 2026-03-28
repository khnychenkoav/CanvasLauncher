package com.darksok.canvaslauncher.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.darksok.canvaslauncher.core.database.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM apps_table")
    fun observeApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps_table")
    suspend fun getAppsOnce(): List<AppEntity>

    @Upsert
    suspend fun upsert(apps: List<AppEntity>)

    @Upsert
    suspend fun upsert(app: AppEntity)

    @Query("DELETE FROM apps_table WHERE packageName IN (:packageNames)")
    suspend fun deleteByPackageNames(packageNames: List<String>)

    @Query("DELETE FROM apps_table WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("UPDATE apps_table SET x = :x, y = :y WHERE packageName = :packageName")
    suspend fun updatePosition(
        packageName: String,
        x: Float,
        y: Float,
    )
}
