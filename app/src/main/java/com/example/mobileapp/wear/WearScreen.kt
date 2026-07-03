package com.example.mobileapp.wear

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobileapp.common.HealthMetrics

@Composable
fun WearScreen(metrics: HealthMetrics, alert: String?) {
    // Animación de pulso para el icono de sincronización
    val infiniteTransition = rememberInfiniteTransition(label = "syncPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            // Icono de Sincronización con animación
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Sincronizando",
                tint = Color.Cyan,
                modifier = Modifier
                    .size(20.dp)
                    .alpha(alpha)
            )

            Text(
                text = "HealthWatch",
                color = Color.Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Heart Rate
            Text(
                text = "${metrics.heartRate}",
                color = if (alert != null) Color.Red else Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(text = "BPM", color = Color.Gray, fontSize = 10.sp)

            Spacer(modifier = Modifier.height(8.dp))

            // Steps & Accel Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${metrics.steps}", color = Color.White, fontSize = 14.sp)
                    Text(text = "Pasos", color = Color.Gray, fontSize = 8.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "X:%.1f Y:%.1f".format(metrics.accelX, metrics.accelY), 
                        color = Color.White, 
                        fontSize = 10.sp
                    )
                    Text(text = "Acel.", color = Color.Gray, fontSize = 8.sp)
                }
            }

            if (alert != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ALERTA",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.Red, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
