package com.example.reloj.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.example.common.HealthMetrics

// RF-11: pantalla principal con BPM actual, pasos y nivel de actividad.
// RF-13: estado de conexion Bluetooth. RF-14: barra de progreso de meta diaria.
// Muestra los 3 sensores del dispositivo juntos: frecuencia cardiaca (PPG), podometro
// (Health Services) y acelerometro de 3 ejes nativo (SensorManager).
@Composable
fun SummaryScreen(metrics: HealthMetrics, dailyStepGoal: Int, isPhoneConnected: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
            Text("HealthWatch", color = Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)

            val sensorError = metrics.sensorError
            if (sensorError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(sensorError, color = Color.Red, fontSize = 11.sp)
            } else {
                val isAlert = metrics.heartRate < 50 || metrics.heartRate > 160
                Text(
                    "${metrics.heartRate}",
                    color = if (isAlert) Color.Red else Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("BPM", color = Color.Gray, fontSize = 10.sp)
                Text("${metrics.steps} pasos - ${metrics.activityLevel}", color = Color.White, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(8.dp))
                val progress = if (dailyStepGoal > 0) {
                    (metrics.steps.toFloat() / dailyStepGoal.toFloat()).coerceIn(0f, 1f)
                } else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(6.dp)
                        .background(Color.DarkGray, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .background(Color.Cyan, RoundedCornerShape(3.dp))
                    )
                }
                Text("${metrics.steps}/$dailyStepGoal pasos", color = Color.Gray, fontSize = 9.sp)

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Accel X:%.2f Y:%.2f Z:%.2f".format(metrics.accelX, metrics.accelY, metrics.accelZ),
                    color = Color.Cyan,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (isPhoneConnected) "Telefono conectado" else "Telefono desconectado",
                color = if (isPhoneConnected) Color.Green else Color.Red,
                fontSize = 9.sp
            )
        }
    }
}
