package com.example.reloj.sensors

import android.Manifest
import android.content.Context
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class SensorReading {
    data class Data(val heartRate: Int, val steps: Int) : SensorReading()
    data class Error(val message: String) : SensorReading()
}

// RF-01/RF-02: captura de frecuencia cardiaca y pasos via Health Services API,
// tal como exige el documento de requerimientos (ExerciseClient/PassiveMonitoringClient).
class HealthServicesManager(private val context: Context) {

    private val passiveMonitoringClient =
        HealthServices.getClient(context).passiveMonitoringClient

    // RF-05: si el registro falla (sensor no disponible) o se pierde el permiso,
    // se emite un estado de error en vez de dejar la app sin datos silenciosamente.
    fun readings(): Flow<SensorReading> = callbackFlow {
        val callback = object : PassiveListenerCallback {
            override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
                val heartRate = dataPoints.getData(DataType.HEART_RATE_BPM)
                    .lastOrNull()?.value?.toInt()
                val steps = dataPoints.getData(DataType.STEPS_DAILY)
                    .lastOrNull()?.value?.toInt()
                if (heartRate != null || steps != null) {
                    trySend(SensorReading.Data(heartRate ?: -1, steps ?: 0))
                }
            }

            override fun onPermissionLost() {
                trySend(SensorReading.Error("Permiso de sensores revocado"))
            }

            override fun onRegistrationFailed(throwable: Throwable) {
                trySend(SensorReading.Error(throwable.message ?: "Sensor no disponible"))
            }
        }

        val config = PassiveListenerConfig.builder()
            .setDataTypes(setOf(DataType.HEART_RATE_BPM, DataType.STEPS_DAILY))
            .build()

        try {
            passiveMonitoringClient.setPassiveListenerCallback(config, callback)
        } catch (e: Exception) {
            trySend(SensorReading.Error(e.message ?: "No se pudo iniciar el sensor"))
        }

        awaitClose {
            passiveMonitoringClient.clearPassiveListenerCallbackAsync()
        }
    }

    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION
        )
    }
}
