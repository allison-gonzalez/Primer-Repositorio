package com.example.mobileapp.wear

import com.example.mobileapp.common.HealthMetrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class HealthSensorManager {

    fun getMetricsFlow(): Flow<HealthMetrics> = flow {
        var steps = 0
        while (true) {
            // Sensor 1: Frecuencia Cardiaca
            val heartRate = Random.nextInt(60, 120)
            
            // Sensor 2: Podómetro (Pasos)
            steps += Random.nextInt(0, 3)
            
            // Sensor 3: Acelerómetro (Ejes X, Y, Z)
            val accelX = Random.nextFloat() * 2 - 1 // Simula entre -1 y 1
            val accelY = 9.8f + (Random.nextFloat() * 2 - 1) // Cerca de la gravedad
            val accelZ = Random.nextFloat() * 2 - 1

            emit(HealthMetrics(
                heartRate = heartRate,
                steps = steps,
                accelX = accelX,
                accelY = accelY,
                accelZ = accelZ
            ))
            delay(3000) // Emitir cada 3 segundos para no saturar la API
        }
    }

    fun checkAlerts(metrics: HealthMetrics): String? {
        return if (metrics.heartRate > 100) "¡Alerta! Ritmo cardiaco alto" else null
    }
}
