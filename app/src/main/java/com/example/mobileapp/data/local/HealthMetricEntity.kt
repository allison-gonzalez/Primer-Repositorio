package com.example.mobileapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// RF-20: historial de datos persistido localmente (Room Database).
@Entity(tableName = "health_metrics")
data class HealthMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val heartRate: Int,
    val steps: Int,
    val distanceMeters: Float,
    val calories: Float,
    val activityLevel: String,
    val heartRateZone: String,
    val avgHeartRate5min: Int?,
    val timestamp: Long,
    val deviceId: String,
    val synced: Boolean = false
)
