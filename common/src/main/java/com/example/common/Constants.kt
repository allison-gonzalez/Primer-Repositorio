package com.example.common

object Constants {
    // Wearable Data Layer API message paths (RF-16, RF-18)
    const val PAYLOAD_PATH = "/health_metrics"
    const val SYNC_COMPLETE_PATH = "/sync_complete"
    const val GOAL_PATH = "/daily_step_goal"

    // RF-07: alerta de frecuencia cardiaca fuera de rango en reposo
    const val BPM_ALERT_MIN = 50
    const val BPM_ALERT_MAX = 160

    // Rango valido del sensor PPG (documento de requerimientos, seccion 3.1)
    const val BPM_SENSOR_MIN = 30
    const val BPM_SENSOR_MAX = 220

    // RF-04: muestreo cada 5s en modo activo, cada 30s en modo ahorro de energia
    const val SAMPLING_INTERVAL_ACTIVE_MS = 5_000L
    const val SAMPLING_INTERVAL_SAVER_MS = 30_000L

    // RF-14: meta diaria de pasos por defecto si el usuario no ha configurado una
    const val DEFAULT_DAILY_STEP_GOAL = 8_000
}
