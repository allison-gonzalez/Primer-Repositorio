package com.example.common

import kotlinx.serialization.Serializable

@Serializable
data class HealthMetrics(
    val heartRate: Int = 0,
    val steps: Int = 0,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val distanceMeters: Float = 0f,
    val calories: Float = 0f,
    val activityLevel: String = ActivityLevel.SEDENTARY,
    val heartRateZone: String = HeartRateZone.RESTING,
    val avgHeartRate5min: Int? = null,
    val sensorError: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "Watch-001"
)

object ActivityLevel {
    const val SEDENTARY = "Sedentario"
    const val MODERATE = "Moderado"
    const val ACTIVE = "Activo"
}

object HeartRateZone {
    const val RESTING = "Descanso"
    const val FAT_BURN = "Quema de grasa"
    const val CARDIO = "Cardio"
    const val PEAK = "Pico"
}
