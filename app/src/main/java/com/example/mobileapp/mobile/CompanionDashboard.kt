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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CompanionDashboard(history: List<HealthMetrics>) {
    val scope = rememberCoroutineScope()
    var httpResponse by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Companion Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Resumen de Hoy", fontWeight = FontWeight.Bold)
                val latest = history.lastOrNull() ?: HealthMetrics()
                Text(text = "Total Pasos: ${latest.steps}")
                Text(text = "Distancia: %.2f Km".format(latest.distanceKm))
                Text(text = "Calorías: %.1f kcal".format(latest.calories))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HTTP Buttons Section
        Text(text = "Sincronización Cloud", fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        httpResponse = performHttpRequest("GET")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("GET Data")
            }
            Button(
                onClick = {
                    scope.launch {
                        httpResponse = performHttpRequest("POST")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("POST Data")
            }
        }

        if (httpResponse.isNotEmpty()) {
            Text(
                text = "Respuesta: $httpResponse",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Historial Reciente", fontWeight = FontWeight.SemiBold)
        
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
        headlineContent = { Text("Frecuencia: ${metric.heartRate} BPM") },
        supportingContent = { Text("Pasos: ${metric.steps} | $time") },
        trailingContent = {
            if (metric.heartRate > 100) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alerta",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

suspend fun performHttpRequest(method: String): String = withContext(Dispatchers.IO) {
    try {
        val url = if (method == "GET") {
            URL("https://jsonplaceholder.typicode.com/posts/1")
        } else {
            URL("https://jsonplaceholder.typicode.com/posts")
        }

        with(url.openConnection() as HttpURLConnection) {
            requestMethod = method
            if (method == "POST") {
                doOutput = true
                outputStream.write("{\"title\": \"HealthData\", \"body\": \"Sync\", \"userId\": 1}".toByteArray())
            }
            
            val responseCode = responseCode
            if (responseCode in 200..299) {
                "Éxito ($responseCode)"
            } else {
                "Error ($responseCode)"
            }
        }
    } catch (e: Exception) {
        "Fallo: ${e.message}"
    }
}
