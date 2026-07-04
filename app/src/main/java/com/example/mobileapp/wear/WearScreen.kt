package com.example.mobileapp.wear

import android.content.Context
import android.content.Intent
import android.os.Vibrator
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobileapp.common.HealthMetrics
import com.example.mobileapp.mobile.DetailsActivity

@Composable
fun WearScreen(metrics: HealthMetrics, alert: String?) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // Vibración al detectar alerta (Punto 4 del PDF)
    LaunchedEffect(alert) {
        if (alert != null && vibrator.hasVibrator()) {
            vibrator.vibrate(500)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "syncPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Default.Sync, null, tint = Color.Cyan, modifier = Modifier.size(16.dp).alpha(alpha))
            Text("HealthWatch", color = Color.Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            
            Text("${metrics.heartRate}", color = if (alert != null) Color.Red else Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("BPM", color = Color.Gray, fontSize = 10.sp)

            Row {
                Text("${metrics.steps} Pasos", color = Color.White, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón para abrir segunda ventana (Punto 5 del PDF)
            Button(
                onClick = { 
                    val intent = Intent(context, DetailsActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Detalles", fontSize = 10.sp)
            }

            if (alert != null) {
                Text("ALERTA", color = Color.White, fontSize = 8.sp, modifier = Modifier.background(Color.Red, CircleShape).padding(4.dp))
            }
        }
    }
}
