package com.example.mobileapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// RNF-09: consentimiento explicito del usuario antes de iniciar la recoleccion
// de datos biometricos provenientes del smartwatch.
@Composable
fun ConsentScreen(onAccept: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Consentimiento de datos de salud", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "HealthWatch recolecta tu frecuencia cardiaca y conteo de pasos desde tu " +
                "smartwatch para mostrarte tu historial de salud. Estos datos se " +
                "almacenan localmente en tu telefono y no se comparten con terceros " +
                "sin tu consentimiento explicito."
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAccept) {
            Text("Acepto y deseo continuar")
        }
    }
}
