package com.example.mobileapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobileapp.data.local.HealthMetricEntity
import com.example.mobileapp.data.repository.HealthRepository
import com.example.mobileapp.ui.charts.LineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// RF-19: muestra en tiempo real los datos recibidos del smartwatch.
// RF-21: graficas de evolucion de BPM y pasos (dia actual / ultimos 7 dias).
@Composable
fun HistoryScreen(repository: HealthRepository) {
    var showLast7Days by remember { mutableStateOf(false) }
    val today by repository.observeToday().collectAsState(initial = emptyList())
    val last7 by repository.observeLast7Days().collectAsState(initial = emptyList())
    val data = if (showLast7Days) last7 else today

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Historial de Salud", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showLast7Days, onClick = { showLast7Days = false }, label = { Text("Hoy") })
            FilterChip(selected = showLast7Days, onClick = { showLast7Days = true }, label = { Text("Ultimos 7 dias") })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Frecuencia cardiaca (BPM)", fontWeight = FontWeight.SemiBold)
        LineChart(values = data.map { it.heartRate.toFloat() }, lineColor = Color.Red)

        Spacer(modifier = Modifier.height(16.dp))
        Text("Pasos", fontWeight = FontWeight.SemiBold)
        LineChart(values = data.map { it.steps.toFloat() }, lineColor = Color(0xFF2196F3))

        Spacer(modifier = Modifier.height(16.dp))
        Text("Registros recientes", fontWeight = FontWeight.SemiBold)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(data.reversed()) { MetricRow(it) }
        }
    }
}

@Composable
private fun MetricRow(metric: HealthMetricEntity) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    ListItem(
        headlineContent = { Text("FC: ${metric.heartRate} | Pasos: ${metric.steps}") },
        supportingContent = { Text("${metric.activityLevel} - ${sdf.format(Date(metric.timestamp))}") }
    )
}
