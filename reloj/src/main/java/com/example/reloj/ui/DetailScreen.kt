package com.example.reloj.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.example.common.HealthMetrics

// RF-12: segunda pantalla (detalle de sensores) navegable por swipe desde el resumen.
@Composable
fun DetailScreen(metrics: HealthMetrics) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
            Text("Detalle de sensores", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Zona cardiaca: ${metrics.heartRateZone}", color = Color.White, fontSize = 11.sp)
            Text("Promedio 5min: ${metrics.avgHeartRate5min ?: "--"} BPM", color = Color.White, fontSize = 11.sp)
            Text("Distancia: %.0f m".format(metrics.distanceMeters), color = Color.White, fontSize = 11.sp)
            Text("Calorias: %.1f kcal".format(metrics.calories), color = Color.White, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (metrics.sensorError != null) "Sensor: ERROR" else "Sensor: OK",
                color = if (metrics.sensorError != null) Color.Red else Color.Green,
                fontSize = 10.sp
            )
        }
    }
}
