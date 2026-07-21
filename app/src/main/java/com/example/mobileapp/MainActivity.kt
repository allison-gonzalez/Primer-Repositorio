package com.example.mobileapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.common.Constants
import com.example.common.HealthMetrics
import com.example.mobileapp.data.local.AppDatabase
import com.example.mobileapp.data.prefs.UserPreferences
import com.example.mobileapp.data.remote.HealthWatchApiClient
import com.example.mobileapp.data.repository.HealthRepository
import com.example.mobileapp.ui.ConsentScreen
import com.example.mobileapp.ui.GoalSettingsActivity
import com.example.mobileapp.ui.HistoryScreen
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// App companion del smartphone: recibe datos del reloj via BLE (RF-19),
// los persiste en Room (RF-20) y sincroniza el excedente al backend propio.
class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var repository: HealthRepository
    private lateinit var userPreferences: UserPreferences
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(applicationContext)
        repository = HealthRepository(db.healthMetricDao(), HealthWatchApiClient())
        userPreferences = UserPreferences(applicationContext)
        requestNotificationPermissionIfNeeded()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val consentGranted by userPreferences.consentGranted.collectAsState(initial = false)
                    if (consentGranted) {
                        MainNavigation(repository)
                    } else {
                        ConsentScreen(onAccept = {
                            lifecycleScope.launch { userPreferences.setConsentGranted(true) }
                        })
                    }
                }
            }
        }
    }

    // Samsung solo reconoce esta app como companion valida del reloj si tiene concedido
    // POST_NOTIFICATIONS (Notification4WatchManager.isValidPhoneApp), aunque la app no
    // muestre notificaciones propias.
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
        }
    }

    // RF-19: recibe HealthMetrics del reloj, los guarda en Room y los reenvia a la API de
    // inmediato (RF-20/backend), en cada lectura, no solo cuando el reloj hace flush de una
    // cola pendiente tras una desconexion.
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("onMessageReceived", "path=${messageEvent.path} de ${messageEvent.sourceNodeId}")
        when (messageEvent.path) {
            Constants.PAYLOAD_PATH -> {
                val metrics = json.decodeFromString<HealthMetrics>(String(messageEvent.data, Charsets.UTF_8))
                lifecycleScope.launch {
                    repository.saveIncoming(metrics)
                    repository.syncPendingToBackend()
                }
            }
            Constants.SYNC_COMPLETE_PATH -> {
                lifecycleScope.launch { repository.syncPendingToBackend() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
        Log.d("onMessageReceived", "Listener registrado")
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }
}

@Composable
fun MainNavigation(repository: HealthRepository) {
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                    label = { Text("Historial") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { context.startActivity(Intent(context, GoalSettingsActivity::class.java)) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Config") },
                    label = { Text("Config") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            HistoryScreen(repository)
        }
    }
}
