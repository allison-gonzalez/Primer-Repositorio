package com.example.mobileapp.data.repository

import com.example.common.HealthMetrics
import com.example.mobileapp.data.local.HealthMetricDao
import com.example.mobileapp.data.local.HealthMetricEntity
import com.example.mobileapp.data.remote.HealthWatchApiClient
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.concurrent.TimeUnit

// Room es la fuente de verdad (documento 2.1: "el smartphone cumple el rol de servidor
// local"); el backend HTTP es un espejo secundario. La sincronizacion siempre va
// Room -> backend, nunca al reves, y la UI nunca depende de que el backend este arriba.
class HealthRepository(
    private val dao: HealthMetricDao,
    private val apiClient: HealthWatchApiClient
) {
    // RF-20: guarda en Room cada lectura recibida del reloj.
    suspend fun saveIncoming(metrics: HealthMetrics) {
        dao.insert(metrics.toEntity())
    }

    // RF-21: historial del dia actual.
    fun observeToday(): Flow<List<HealthMetricEntity>> = dao.observeSince(startOfDayMillis())

    // RF-21: historial de los ultimos 7 dias.
    fun observeLast7Days(): Flow<List<HealthMetricEntity>> =
        dao.observeSince(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))

    fun observeRecent(): Flow<List<HealthMetricEntity>> = dao.observeRecent()

    suspend fun syncPendingToBackend() {
        dao.getUnsynced().forEach { entity ->
            val ok = apiClient.postSensorReading(entity.toDomain())
            if (ok) dao.markSynced(entity.id)
        }
    }

    private fun startOfDayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

private fun HealthMetrics.toEntity() = HealthMetricEntity(
    heartRate = heartRate,
    steps = steps,
    accelX = accelX,
    accelY = accelY,
    accelZ = accelZ,
    distanceMeters = distanceMeters,
    calories = calories,
    activityLevel = activityLevel,
    heartRateZone = heartRateZone,
    avgHeartRate5min = avgHeartRate5min,
    timestamp = timestamp,
    deviceId = deviceId
)

private fun HealthMetricEntity.toDomain() = HealthMetrics(
    heartRate = heartRate,
    steps = steps,
    accelX = accelX,
    accelY = accelY,
    accelZ = accelZ,
    distanceMeters = distanceMeters,
    calories = calories,
    activityLevel = activityLevel,
    heartRateZone = heartRateZone,
    avgHeartRate5min = avgHeartRate5min,
    timestamp = timestamp,
    deviceId = deviceId
)
