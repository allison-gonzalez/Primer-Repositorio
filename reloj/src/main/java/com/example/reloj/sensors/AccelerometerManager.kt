package com.example.reloj.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class AccelReading(val x: Float, val y: Float, val z: Float)

// Tercer sensor del dispositivo (ademas de FC y podometro): acelerometro de 3 ejes nativo
// (Sensor.TYPE_ACCELEROMETER), leido directamente con SensorManager/SensorEventListener
// (patron de la clase), independiente del conteo de pasos por software que entrega Health Services.
class AccelerometerManager(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    fun isAvailable(): Boolean = accelerometer != null

    fun readings(): Flow<AccelReading> = callbackFlow {
        val sensor = accelerometer
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(AccelReading(event.values[0], event.values[1], event.values[2]))
            }
            override fun onAccuracyChanged(changedSensor: Sensor, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
