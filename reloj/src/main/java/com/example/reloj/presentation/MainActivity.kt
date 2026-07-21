package com.example.reloj.presentation

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
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
import com.example.reloj.sensors.HealthSensorManager
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
// UI del reloj (RF-11 a RF-15) y transmision BLE al telefono (RF-16/RF-17).
class MainActivity : ComponentActivity(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private var phoneNodeId: String? = null
    private lateinit var healthSensorManager: HealthSensorManager
    private lateinit var accelerometerManager: AccelerometerManager
    private lateinit var pendingSyncQueue: PendingSyncQueue
    private val json = Json { ignoreUnknownKeys = true }
    private val heartRateHistory = mutableListOf<Int>()
    private var lastAccel = AccelReading(0f, 0f, 0f)

    private var metricsState by mutableStateOf(HealthMetrics())
    private var isPhoneConnected by mutableStateOf(false)
    private var dailyStepGoal by mutableStateOf(Constants.DEFAULT_DAILY_STEP_GOAL)

    // RF-16: envia el ultimo dato al telefono cada 5s sin depender de que llegue un evento
    // nuevo de sensor (TYPE_STEP_COUNTER/TYPE_HEART_RATE solo disparan "on-change" y pueden
    // tardar minutos en cambiar), igual que el Handler de la app de referencia.
    private val sendHandler = Handler(Looper.getMainLooper())
    private val sendRunnable = object : Runnable {
        override fun run() {
            sendMetricsToPhone(metricsState)
            sendHandler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        healthSensorManager = HealthSensorManager(applicationContext)
        healthSensorManager.onReading = { heartRate, steps -> onHealthReading(heartRate, steps) }
        accelerometerManager = AccelerometerManager(applicationContext)
        pendingSyncQueue = PendingSyncQueue(applicationContext)
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

    // RF-01/02: funcion dedicada de permisos runtime antes de usar los sensores (patron de la
    // diapositiva de la clase). Si falta algun permiso se pide y se retorna sin registrar el
    // sensor todavia; si ya estan concedidos se registra de inmediato.
    private fun requestHealthPermissionsIfNeeded() {
        val missing = HealthSensorManager.REQUIRED_PERMISSIONS.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            healthSensorManager.start()
        } else {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), SENSOR_PERMISSION_REQUEST_CODE)
        }
    }

    // Igual que la app de referencia: no se revisa grantResults, simplemente se reintenta
    // registrar el sensor (registerListener es idempotente si el listener ya esta registrado).
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SENSOR_PERMISSION_REQUEST_CODE) healthSensorManager.start()
    }

    private fun onHealthReading(heartRate: Int, steps: Int) {
        heartRateHistory.add(heartRate)
        if (heartRateHistory.size > 60) heartRateHistory.removeAt(0)

        val metrics = HealthMetrics(
            heartRate = heartRate,
            steps = steps,
            accelX = lastAccel.x,
            accelY = lastAccel.y,
            accelZ = lastAccel.z,
            distanceMeters = MetricsCalculator.distanceMeters(steps),
            calories = MetricsCalculator.caloriesMet(steps),
            activityLevel = MetricsCalculator.activityLevel(steps),
            heartRateZone = MetricsCalculator.heartRateZone(heartRate),
            avgHeartRate5min = MetricsCalculator.average5min(heartRateHistory)
        )
        metricsState = metrics

        if (AlertManager.isOutOfRange(heartRate)) {
            AlertManager.triggerAlert(this)
        }
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

    // RF-16: sincroniza los datos de sensores con el telefono via Wearable Data Layer API.
    private fun sendMetricsToPhone(metrics: HealthMetrics) {
        val nodeId = phoneNodeId
        if (nodeId == null) {
            Log.d("sendMessage", "Sin nodo de telefono, se encola")
            lifecycleScope.launch { pendingSyncQueue.enqueue(metrics) }
            return
        }
        val payload = json.encodeToString(metrics).toByteArray()
        Wearable.getMessageClient(this)
            .sendMessage(nodeId, Constants.PAYLOAD_PATH, payload)
            .addOnSuccessListener {
                Log.d("sendMessage", "Mensaje enviado correctamente a $nodeId")
                flushPendingQueue(nodeId)
            }
            .addOnFailureListener { e ->
                Log.d("sendMessage", "Error al enviar mensaje a $nodeId: ${e.message}")
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
        // Igual que registrarSensores() en la app de referencia: se reintenta en cada onResume
        // sin condicion (tras volver del dialogo de permisos, al despertar la pantalla, etc.).
        healthSensorManager.start()
        sendHandler.post(sendRunnable)
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
        healthSensorManager.stop()
        sendHandler.removeCallbacks(sendRunnable)
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
        private const val SENSOR_PERMISSION_REQUEST_CODE = 1001
    }
}
