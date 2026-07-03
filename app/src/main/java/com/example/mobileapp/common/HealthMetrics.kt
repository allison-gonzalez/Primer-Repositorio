package com.example.mobileapp.common

data class HealthMetrics(
    val heartRate: Int = 0,
    val steps: Int = 0,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String = "Watch-001"
)
