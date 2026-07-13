package com.example.reloj.presentation

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.reloj.presentation.theme.RelojTheme
import com.google.android.gms.wearable.*

// Diapositiva 27 - herencias de la clase
class MainActivity : ComponentActivity(),
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener,
    CapabilityClient.OnCapabilityChangedListener {

    private var phoneNodeID: String? = null
    private val mensajes = mutableStateListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RelojTheme {
                AppScaffold {
                    ChatScreenWear(
                        mensajes = mensajes,
                        onEnviar = { texto ->
                            sendMessageToPhone(texto)
                            mensajes.add("Yo: $texto")
                        }
                    )
                }
            }
        }
    }

    // Diapositiva 31 - enviar mensaje
    private fun sendMessageToPhone(texto: String) {
        val nodeId = phoneNodeID ?: run {
            Log.d("Wear", "No se conoce el nodo del celular aún")
            return
        }
        Wearable.getMessageClient(this)
            .sendMessage(nodeId, PAYLOAD_PATH, texto.toByteArray())
            .addOnSuccessListener {
                Log.d("sendMessage", "Mensaje enviado correctamente")
            }
            .addOnFailureListener { e ->
                Log.d("sendMessage", "Error al enviar mensaje ${e.message}")
            }
    }

    // Diapositiva 32 - recibir mensajes
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("onMessageReceived", messageEvent.toString())
        Log.d("onMessageReceived", "ID del nodo ${messageEvent.sourceNodeId}")
        Log.d("onMessageReceived", "Payload: ${messageEvent.path}")
        val message = String(messageEvent.data, Charsets.UTF_8)
        Log.d("onMessageReceived", "Mensaje: $message")

        phoneNodeID = messageEvent.sourceNodeId // guardamos el nodo del celular al recibir
        runOnUiThread {
            mensajes.add("Celular: $message")
        }
    }

    // Diapositiva 30 - listeners en ambos dispositivos
    override fun onResume() {
        super.onResume()
        try {
            Wearable.getDataClient(this).addListener(this)
            Wearable.getMessageClient(this).addListener(this)
            Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"),
                    CapabilityClient.FILTER_REACHABLE)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onPause() {
        super.onPause()
        try {
            Wearable.getDataClient(this).removeListener(this)
            Wearable.getMessageClient(this).removeListener(this)
            Wearable.getCapabilityClient(this).removeListener(this)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {}
    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {}

    companion object {
        // Diapositiva 28 - banderas
        private const val PAYLOAD_PATH = "/chat_message"
    }
}

@Composable
fun ChatScreenWear(
    mensajes: List<String>,
    onEnviar: (String) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = { onEnviar("Hola desde el reloj!") }) {
                Text("Enviar")
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text("Chat")
                }
            }

            // Mostrar mensajes
            items(mensajes.size) { index ->
                Text(
                    text = mensajes[index],
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .transformedHeight(this, transformationSpec),
                )
            }
        }
    }
}
