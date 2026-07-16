package com.example.reloj.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.common.HealthMetrics
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.pendingSyncDataStore by preferencesDataStore(name = "pending_sync_queue")

// RF-17: encola los datos generados cuando no hay conexion activa con el telefono
// y los transmite en orden al restablecer el vinculo Bluetooth.
class PendingSyncQueue(private val context: Context) {

    private val key = stringPreferencesKey("pending_metrics")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun enqueue(metrics: HealthMetrics) {
        context.pendingSyncDataStore.edit { prefs ->
            val current = decode(prefs[key])
            prefs[key] = json.encodeToString(current + metrics)
        }
    }

    suspend fun peekAll(): List<HealthMetrics> =
        decode(context.pendingSyncDataStore.data.first()[key])

    suspend fun clear() {
        context.pendingSyncDataStore.edit { it.remove(key) }
    }

    private fun decode(raw: String?): List<HealthMetrics> =
        if (raw.isNullOrBlank()) emptyList() else json.decodeFromString(raw)
}
