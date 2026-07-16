package com.example.mobileapp.data.remote

import android.util.Log
import com.example.common.HealthMetrics
import com.example.mobileapp.BuildConfig
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Cliente HTTP hacia el backend propio (Node/Express/Postgres, ver /backend).
// Conserva el patron OkHttp de la clase: OkHttpClient() + Request.Builder() + enqueue
// con onFailure/onResponse, con Log.d("FETCH", ...) para depuracion.
class HealthWatchApiClient {
    private val client = OkHttpClient()

    suspend fun postSensorReading(metrics: HealthMetrics): Boolean = suspendCoroutine { cont ->
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
            .url("${BuildConfig.API_BASE_URL}api/Sensores")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("FETCH", "Error: ${e.message}")
                cont.resume(false)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    Log.d("FETCH", "Respuesta: ${it.code}")
                    cont.resume(it.isSuccessful)
                }
            }
        })
    }
}
