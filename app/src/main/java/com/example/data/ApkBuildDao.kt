package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApkBuildDao {
    @Query("SELECT * FROM apk_builds ORDER BY createdAt DESC")
    fun getAllBuilds(): Flow<List<ApkBuildEntity>>

    @Query("SELECT COUNT(*) FROM apk_builds")
    suspend fun getBuildCount(): Int

    @Query("SELECT * FROM apk_builds WHERE id = :id LIMIT 1")
    suspend fun getBuildById(id: Long): ApkBuildEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuild(build: ApkBuildEntity): Long

    @Query("DELETE FROM apk_builds WHERE id = :id")
    suspend fun deleteBuildById(id: Long)
}
