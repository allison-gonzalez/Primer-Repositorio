package com.example.reloj.presentation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import com.example.common.Constants
import com.example.common.HealthMetrics
import com.example.reloj.alert.AlertManager
import com.example.reloj.metrics.MetricsCalculator
import com.example.reloj.presentation.theme.RelojTheme
import com.example.reloj.sensors.AccelReading
import com.example.reloj.sensors.AccelerometerManager
import com.example.reloj.sensors.HealthServicesManager
import com.example.reloj.sensors.SensorReading
import com.example.reloj.sync.PendingSyncQueue
import com.example.reloj.ui.DetailScreen
import com.example.reloj.ui.SummaryScreen
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Captura de sensores (RF-01/02/04/05), procesamiento y alertas (RF-06 a RF-10),
// UI del reloj (RF-11 a RF-15) y transmision BLE al telefono (RF-16 a RF-18).
class MainActivity : ComponentActivity(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private var phoneNodeId: String? = null
    private lateinit var healthServicesManager: HealthServicesManager
    private lateinit var accelerometerManager: AccelerometerManager
    private lateinit var pendingSyncQueue: PendingSyncQueue
    private val json = Json { ignoreUnknownKeys = true }
    private val heartRateHistory = mutableListOf<Int>()
    private var lastAccel = AccelReading(0f, 0f, 0f)

    private var metricsState by mutableStateOf(HealthMetrics())
    private var isPhoneConnected by mutableStateOf(false)
    private var dailyStepGoal by mutableStateOf(Constants.DEFAULT_DAILY_STEP_GOAL)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            startCollectingMetrics()
        } else {
            metricsState = metricsState.copy(sensorError = "Permisos de sensores denegados")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthServicesManager = HealthServicesManager(applicationContext)
        accelerometerManager = AccelerometerManager(applicationContext)
        pendingSyncQueue = PendingSyncQueue(applicationContext)
        createNotificationChannel()
        startCollectingAccelerometer()

        setContent {
            RelojTheme {
                AppScaffold {
                    val pagerState = rememberPagerState(pageCount = { 2 })
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        if (page == 0) {
                            SummaryScreen(
                                metrics = metricsState,
                                dailyStepGoal = dailyStepGoal,
                                isPhoneConnected = isPhoneConnected
                            )
                        } else {
                            DetailScreen(metrics = metricsState)
                        }
                    }
                }
            }
        }

        requestHealthPermissionsIfNeeded()
    }

    // RF-01/02: funcion dedicada de permisos runtime antes de usar los sensores (patron de la clase).
    private fun requestHealthPermissionsIfNeeded() {
        val missing = HealthServicesManager.REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) startCollectingMetrics() else permissionLauncher.launch(missing.toTypedArray())
    }

    // Tercer sensor (acelerometro nativo): sin permiso runtime, se colecciona de inmediato
    // y se muestra junto a FC/pasos en la misma pantalla (SummaryScreen).
    private fun startCollectingAccelerometer() {
        if (!accelerometerManager.isAvailable()) return
        lifecycleScope.launch {
            accelerometerManager.readings().collect { accel ->
                lastAccel = accel
                metricsState = metricsState.copy(accelX = accel.x, accelY = accel.y, accelZ = accel.z)
            }
        }
    }

    private fun startCollectingMetrics() {
        lifecycleScope.launch {
            healthServicesManager.readings().collect { reading ->
                when (reading) {
                    is SensorReading.Error -> {
                        // RF-05: notificar cuando un sensor no este disponible o falle.
                        metricsState = metricsState.copy(sensorError = reading.message)
                    }
                    is SensorReading.Data -> {
                        heartRateHistory.add(reading.heartRate)
                        if (heartRateHistory.size > 60) heartRateHistory.removeAt(0)

                        val metrics = HealthMetrics(
                            heartRate = reading.heartRate,
                            steps = reading.steps,
                            accelX = lastAccel.x,
                            accelY = lastAccel.y,
                            accelZ = lastAccel.z,
                            distanceMeters = MetricsCalculator.distanceMeters(reading.steps),
                            calories = MetricsCalculator.caloriesMet(reading.steps),
                            activityLevel = MetricsCalculator.activityLevel(reading.steps),
                            heartRateZone = MetricsCalculator.heartRateZone(reading.heartRate),
                            avgHeartRate5min = MetricsCalculator.average5min(heartRateHistory),
                            sensorError = null
                        )
                        metricsState = metrics

                        if (AlertManager.isOutOfRange(reading.heartRate)) {
                            AlertManager.triggerAlert(this@MainActivity)
                        }
                        sendMetricsToPhone(metrics)
                    }
                }
            }
        }
    }

    // RF-16: sincroniza los datos de sensores con el telefono via Wearable Data Layer API.
    private fun sendMetricsToPhone(metrics: HealthMetrics) {
        val nodeId = phoneNodeId
        if (nodeId == null) {
            lifecycleScope.launch { pendingSyncQueue.enqueue(metrics) }
            return
        }
        val payload = json.encodeToString(metrics).toByteArray()
        Wearable.getMessageClient(this)
            .sendMessage(nodeId, Constants.PAYLOAD_PATH, payload)
            .addOnSuccessListener { flushPendingQueue(nodeId) }
            .addOnFailureListener {
                lifecycleScope.launch { pendingSyncQueue.enqueue(metrics) }
            }
    }

    // RF-17: encola datos sin conexion y los transmite al restablecer el vinculo Bluetooth.
    private fun flushPendingQueue(nodeId: String) {
        lifecycleScope.launch {
            val pending = pendingSyncQueue.peekAll()
            if (pending.isEmpty()) return@launch
            pending.forEach { metrics ->
                Wearable.getMessageClient(this@MainActivity)
                    .sendMessage(nodeId, Constants.PAYLOAD_PATH, json.encodeToString(metrics).toByteArray())
            }
            pendingSyncQueue.clear()
            Wearable.getMessageClient(this@MainActivity)
                .sendMessage(nodeId, Constants.SYNC_COMPLETE_PATH, ByteArray(0))
            // RF-18: notificar al usuario en el reloj cuando la sincronizacion se complete.
            notifySyncComplete()
        }
    }

    private fun notifySyncComplete() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("HealthWatch")
            .setContentText("Sincronizacion completada")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(this).notify(SYNC_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Sincronizacion", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    // Recibe la meta diaria de pasos configurada desde el telefono (RF-14).
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == Constants.GOAL_PATH) {
            val goal = String(messageEvent.data, Charsets.UTF_8).toIntOrNull()
            if (goal != null) dailyStepGoal = goal
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(this).addListener(this)
            Wearable.getMessageClient(this).addListener(this)
            Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE)
            Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
                isPhoneConnected = nodes.isNotEmpty()
                phoneNodeId = nodes.firstOrNull()?.id
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(this).removeListener(this)
            Wearable.getMessageClient(this).removeListener(this)
            Wearable.getCapabilityClient(this).removeListener(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {}

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        isPhoneConnected = capabilityInfo.nodes.isNotEmpty()
        phoneNodeId = capabilityInfo.nodes.firstOrNull()?.id
    }

    companion object {
        private const val CHANNEL_ID = "sync_channel"
        private const val SYNC_NOTIFICATION_ID = 1
    }
}
