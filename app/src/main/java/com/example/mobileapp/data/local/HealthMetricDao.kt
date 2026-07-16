package com.example.mobileapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMetricDao {
    @Insert
    suspend fun insert(entity: HealthMetricEntity): Long

    @Query("SELECT * FROM health_metrics WHERE timestamp >= :sinceMillis ORDER BY timestamp ASC")
    fun observeSince(sinceMillis: Long): Flow<List<HealthMetricEntity>>

    @Query("SELECT * FROM health_metrics WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsynced(): List<HealthMetricEntity>

    @Query("UPDATE health_metrics SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("SELECT * FROM health_metrics ORDER BY timestamp DESC LIMIT 20")
    fun observeRecent(): Flow<List<HealthMetricEntity>>
}
