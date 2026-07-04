package com.example.mobileapp.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobileapp.common.HealthMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CompanionDashboard(history: List<HealthMetrics>) {
    var lastSyncStatus by remember { mutableStateOf("Esperando datos...") }
    val latestMetric = history.lastOrNull()

    LaunchedEffect(latestMetric) {
        latestMetric?.let {
            lastSyncStatus = "Sincronizando..."
            lastSyncStatus = syncWithOkHttp(it)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Unidad 1 - Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Estado Postgres (via OkHttp):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(lastSyncStatus, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sensores Activos", fontWeight = FontWeight.Bold)
                latestMetric?.let {
                    Text("FC: ${it.heartRate} BPM | Pasos: ${it.steps}")
                    Text("Acc: [${"%.1f".format(it.accelX)}, ${"%.1f".format(it.accelY)}, ${"%.1f".format(it.accelZ)}]")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sección de Botones HTTP GET/POST (Requerimiento)
        val scope = rememberCoroutineScope()
        Text("Pruebas HTTP API", fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { 
                scope.launch { 
                    lastSyncStatus = "Probando GET..."
                    val client = OkHttpClient()
                    val request = Request.Builder().url("https://jsonplaceholder.typicode.com/posts/1").build()
                    withContext(Dispatchers.IO) {
                        try { client.newCall(request).execute().use { lastSyncStatus = "GET: ${it.code} OK" } }
                        catch (e: Exception) { lastSyncStatus = "GET Falló: ${e.message}" }
                    }
                }
            }, modifier = Modifier.weight(1f)) { Text("GET") }
            
            Button(onClick = { 
                scope.launch { 
                    lastSyncStatus = "Probando POST..."
                    val client = OkHttpClient()
                    val body = "{}".toRequestBody("application/json".toMediaType())
                    val request = Request.Builder().url("https://jsonplaceholder.typicode.com/posts").post(body).build()
                    withContext(Dispatchers.IO) {
                        try { client.newCall(request).execute().use { lastSyncStatus = "POST: ${it.code} OK" } }
                        catch (e: Exception) { lastSyncStatus = "POST Falló: ${e.message}" }
                    }
                }
            }, modifier = Modifier.weight(1f)) { Text("POST") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(history.reversed()) { MetricItem(it) }
        }
    }
}

@Composable
fun MetricItem(metric: HealthMetrics) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    ListItem(
        headlineContent = { Text("FC: ${metric.heartRate} | Pasos: ${metric.steps}") },
        supportingContent = { Text("Acel: [${"%.1f".format(metric.accelX)}, ${"%.1f".format(metric.accelY)}] | ${sdf.format(Date(metric.timestamp))}") }
    )
}

suspend fun syncWithOkHttp(metrics: HealthMetrics): String = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
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

    val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder()
        .url("http://192.168.195.97:5000/api/Sensores")
        .post(body)
        .build()

    try {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) "Postgres OK (200)" else "Error: ${response.code}"
        }
    } catch (e: Exception) {
        "Fallo: ${e.message}"
    }
}
