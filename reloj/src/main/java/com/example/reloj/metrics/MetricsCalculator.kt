package com.example.reloj.metrics

import com.example.common.ActivityLevel
import com.example.common.HeartRateZone

object MetricsCalculator {

    // RF-06: frecuencia cardiaca promedio por intervalo de 5 minutos.
    // Con muestreo cada 5s en modo activo, 5 minutos equivalen a ~60 lecturas.
    fun average5min(readings: List<Int>): Int {
        if (readings.isEmpty()) return 0
        val window = readings.takeLast(60)
        return window.sum() / window.size
    }

    // RF-10: zona cardiaca segun el BPM actual.
    fun heartRateZone(bpm: Int): String = when {
        bpm < 100 -> HeartRateZone.RESTING
        bpm < 130 -> HeartRateZone.FAT_BURN
        bpm < 160 -> HeartRateZone.CARDIO
        else -> HeartRateZone.PEAK
    }

    // RF-08: nivel de actividad segun el conteo de pasos por hora.
    fun activityLevel(stepsLastHour: Int): String = when {
        stepsLastHour < 1000 -> ActivityLevel.SEDENTARY
        stepsLastHour < 3000 -> ActivityLevel.MODERATE
        else -> ActivityLevel.ACTIVE
    }

    // RF-09: calorias quemadas con la formula MET estandar: kcal = MET * 3.5 * pesoKg / 200 * minutos.
    fun caloriesMet(steps: Int, weightKg: Double = 70.0, minutes: Double = 1.0): Float {
        val met = when {
            steps < 1000 -> 1.5
            steps < 3000 -> 3.5
            else -> 6.0
        }
        return (met * 3.5 * weightKg / 200.0 * minutes).toFloat()
    }

    // Distancia derivada del conteo de pasos (RF-02), zancada promedio de 0.75 m.
    fun distanceMeters(steps: Int, strideMeters: Double = 0.75): Float =
        (steps * strideMeters).toFloat()
}
