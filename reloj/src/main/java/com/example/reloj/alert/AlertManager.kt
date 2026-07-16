package com.example.reloj.alert

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.common.Constants

object AlertManager {

    // RF-07: BPM fuera de rango en reposo (por debajo de 50 o por encima de 160).
    fun isOutOfRange(bpm: Int): Boolean =
        bpm < Constants.BPM_ALERT_MIN || bpm > Constants.BPM_ALERT_MAX

    fun triggerAlert(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }
}
