package com.example.reloj.sensors

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.Calendar

// RF-01/RF-02: captura de frecuencia cardiaca y pasos con SensorManager nativo
// (Sensor.TYPE_HEART_RATE / Sensor.TYPE_STEP_COUNTER), siguiendo el mismo patron que la
// diapositiva de la clase y el proyecto de referencia mi-primer-app: un listener estable que
// se registra sin condicion en cada onResume() (Android ignora registros duplicados del mismo
// listener) en vez de depender de un solo intento gateado por checkSelfPermission.
class HealthSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences("sensor_prefs", Context.MODE_PRIVATE)

    private var heartRate = 0
    private var rawSteps = 0
    private var baselineSteps = prefs.getInt(KEY_BASELINE_STEPS, -1)
    private var lastResetDay = prefs.getInt(KEY_LAST_RESET_DAY, -1)

    var onReading: ((heartRate: Int, steps: Int) -> Unit)? = null

    fun isAvailable(): Boolean = heartRateSensor != null || stepCounterSensor != null

    // Igual que registrarSensores() en la app de referencia: se llama en onCreate y en cada
    // onResume sin comprobar el permiso primero.
    fun start() {
        heartRateSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        stepCounterSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    private fun stepsToday(): Int = if (baselineSteps < 0) 0 else maxOf(0, rawSteps - baselineSteps)

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_HEART_RATE -> {
                heartRate = if (event.accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_LOW &&
                    event.values[0] > 0
                ) {
                    event.values[0].toInt()
                } else {
                    0
                }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                rawSteps = event.values[0].toInt()
                // Reinicia el conteo diario de pasos a medianoche (o en el primer dato recibido).
                val hoy = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                if (baselineSteps < 0 || lastResetDay != hoy) {
                    baselineSteps = rawSteps
                    lastResetDay = hoy
                    prefs.edit()
                        .putInt(KEY_BASELINE_STEPS, baselineSteps)
                        .putInt(KEY_LAST_RESET_DAY, lastResetDay)
                        .apply()
                }
            }
        }
        onReading?.invoke(heartRate, stepsToday())
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    companion object {
        // En Android 16 el sensor android.sensor.heart_rate exige este permiso ademas de
        // BODY_SENSORS (confirmado con dumpsys sensorservice en hardware real).
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION,
            "android.permission.health.READ_HEART_RATE"
        )
        private const val KEY_BASELINE_STEPS = "baseline_steps"
        private const val KEY_LAST_RESET_DAY = "last_reset_day"
    }
}
