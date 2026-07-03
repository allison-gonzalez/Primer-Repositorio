package com.example.mobileapp.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobileapp.common.HealthMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CompanionDashboard(history: List<HealthMetrics>) {
    var lastSyncStatus by remember { mutableStateOf("Esperando datos...") }

    // Sincronización automática con la API cada vez que llega un nuevo dato
    val latestMetric = history.lastOrNull()
    LaunchedEffect(latestMetric) {
        latestMetric?.let {
            lastSyncStatus = "Sincronizando..."
            val result = syncWithApi(it)
            lastSyncStatus = result
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Unidad 1 - Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Estado de la Base de Datos (Postgres vía API)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Estado Postgres:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(text = lastSyncStatus, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Lectura Actual (3 Sensores)", fontWeight = FontWeight.Bold)
                latestMetric?.let {
                    Text(text = "1. Ritmo: ${it.heartRate} BPM")
                    Text(text = "2. Pasos: ${it.steps}")
                    Text(text = "3. Acelerómetro: X:%.2f, Y:%.2f, Z:%.2f".format(it.accelX, it.accelY, it.accelZ))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Historial en Memoria", fontWeight = FontWeight.SemiBold)
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(history.reversed()) { metric ->
                MetricItem(metric)
            }
        }
    }
}

@Composable
fun MetricItem(metric: HealthMetrics) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val time = sdf.format(Date(metric.timestamp))
    
    ListItem(
        headlineContent = { Text("FC: ${metric.heartRate} | Pasos: ${metric.steps}") },
        supportingContent = { Text("Acc: [${"%.1f".format(metric.accelX)}, ${"%.1f".format(metric.accelY)}, ${"%.1f".format(metric.accelZ)}] | $time") }
    )
}

suspend fun syncWithApi(metrics: HealthMetrics): String = withContext(Dispatchers.IO) {
    try {
        val url = URL("http://192.168.195.97:5000/api/Sensores") // Ajustar puerto si es necesario (5000 o 5022)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val json = """
            {
                "dispositivoId": "${metrics.deviceId}",
                "heartRate": ${metrics.heartRate},
                "accelX": ${metrics.accelX},
                "accelY": ${metrics.accelY},
                "accelZ": ${metrics.accelZ},
                "steps": ${metrics.steps}
            }
        """.trimIndent()

        OutputStreamWriter(conn.outputStream).use { it.write(json) }

        val code = conn.responseCode
        if (code in 200..299) "Enviado a Postgres (200 OK)" else "Error API ($code)"
    } catch (e: Exception) {
        "Fallo conexión: ${e.message}"
    }
}
