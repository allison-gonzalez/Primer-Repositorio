package com.example.mobileapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.common.Constants
import com.example.mobileapp.data.prefs.UserPreferences
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch

// RF-14: configuracion de la meta diaria de pasos, enviada al reloj via DataClient.
// Segunda Activity de la app companion (patron multi-Activity de la clase).
class GoalSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userPreferences = UserPreferences(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var goalText by remember { mutableStateOf("") }
                    val currentGoal by userPreferences.dailyStepGoal
                        .collectAsState(initial = Constants.DEFAULT_DAILY_STEP_GOAL)

                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Meta diaria de pasos", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Actual: $currentGoal pasos")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = goalText,
                            onValueChange = { input -> goalText = input.filter { it.isDigit() } },
                            label = { Text("Nueva meta") }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            val goal = goalText.toIntOrNull() ?: return@Button
                            lifecycleScope.launch {
                                userPreferences.setDailyStepGoal(goal)
                                sendGoalToWatch(goal)
                            }
                        }) {
                            Text("Guardar")
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { finish() }) {
                            Text("Regresar")
                        }
                    }
                }
            }
        }
    }

    private fun sendGoalToWatch(goal: Int) {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(this)
                    .sendMessage(node.id, Constants.GOAL_PATH, goal.toString().toByteArray())
            }
        }
    }
}
