# Trazabilidad de requisitos — HealthWatch App

Mapea cada requerimiento del documento de levantamiento (RF-01 a RF-21, RNF-01 a RNF-12) a los
archivos que lo implementan. Ver también las decisiones de diseño registradas en el plan de
implementación de esta feature.

## Requerimientos funcionales

### 4.1 Módulo de sensores

| ID | Descripción corta | Archivo(s) | Estado |
|---|---|---|---|
| RF-01 | Lectura continua de FC en foreground | [reloj/.../sensors/HealthSensorManager.kt](../reloj/src/main/java/com/example/reloj/sensors/HealthSensorManager.kt), [reloj/.../presentation/MainActivity.kt](../reloj/src/main/java/com/example/reloj/presentation/MainActivity.kt) | Hecho — ver nota de desviación abajo |
| RF-02 | Lectura de pasos y distancia en tiempo real | `HealthSensorManager.kt` (`Sensor.TYPE_STEP_COUNTER`), [metrics/MetricsCalculator.kt](../reloj/src/main/java/com/example/reloj/metrics/MetricsCalculator.kt) (`distanceMeters`) | Hecho |
| RF-03 | Timestamp por cada lectura | [common/HealthMetrics.kt](../common/src/main/java/com/example/common/HealthMetrics.kt) (`timestamp`, default `System.currentTimeMillis()`) | Hecho |
| RF-04 | Encendido/apagado automático del sensor FC para batería | `HealthSensorManager.kt` (registro/desregistro del listener atado al ciclo de vida de la Activity vía `awaitClose`) | Hecho (best-effort: solo mientras la app está en foreground, no hay modo background separado) |
| RF-05 | Notificar sensor no disponible/error | `HealthSensorManager.kt` (`SensorReading.Error` si no hay sensores), `MainActivity.kt` (`metricsState.sensorError`), [ui/SummaryScreen.kt](../reloj/src/main/java/com/example/reloj/ui/SummaryScreen.kt) | Hecho |

**Nota de desviación (RF-01/RF-02):** el documento nombra explícitamente Health Services API
(`ExerciseClient`/`PassiveMonitoringClient`). La primera implementación usó esa API, pero en
pruebas en hardware real (Galaxy Watch) se confirmó que el AppOp `BODY_SENSORS` queda en modo
`ignore` a nivel de sistema operativo para **todas** las apps del reloj, incluyendo el propio
Health Services de Google — bloqueando silenciosamente la entrega de datos sin ningún error
visible. La entrega clásica vía `SensorManager`/`SensorEventListener` (`Sensor.TYPE_HEART_RATE`,
`Sensor.TYPE_STEP_COUNTER`) sí funciona en ese mismo hardware, así que se migró a esa vía para
tener una demo funcional en dispositivo físico real.

### 4.2 Módulo de procesamiento y alertas

| ID | Descripción corta | Archivo(s) | Estado |
|---|---|---|---|
| RF-06 | FC promedio cada 5 minutos | `MetricsCalculator.kt` (`average5min`) | Hecho |
| RF-07 | Alerta visual + háptica fuera de rango | [alert/AlertManager.kt](../reloj/src/main/java/com/example/reloj/alert/AlertManager.kt), `SummaryScreen.kt` (texto en rojo) | Hecho |
| RF-08 | Clasificar actividad (Sedentario/Moderado/Activo) | `MetricsCalculator.kt` (`activityLevel`) | Hecho |
| RF-09 | Estimar calorías (fórmula MET) | `MetricsCalculator.kt` (`caloriesMet`) | Hecho |
| RF-10 | Zona cardiaca según BPM actual | `MetricsCalculator.kt` (`heartRateZone`), [ui/DetailScreen.kt](../reloj/src/main/java/com/example/reloj/ui/DetailScreen.kt) | Hecho |

### 4.3 Módulo de interfaz de usuario (smartwatch)

| ID | Descripción corta | Archivo(s) | Estado |
|---|---|---|---|
| RF-11 | Pantalla principal: BPM, pasos, nivel de actividad | `SummaryScreen.kt` | Hecho |
| RF-12 | 2 pantallas navegables por swipe (resumen + detalle) | `MainActivity.kt` (`HorizontalPager`), `SummaryScreen.kt`, `DetailScreen.kt` | Hecho — usa `HorizontalPager` de Wear Compose Foundation en vez de `SwipeDismissableNavHost` (equivalente funcional, navegación por swipe nativa) |
| RF-13 | Estado de conexión Bluetooth | `MainActivity.kt` (`isPhoneConnected` vía `NodeClient`/`CapabilityClient`), `SummaryScreen.kt` | Hecho |
| RF-14 | Barra de progreso de meta diaria de pasos | `SummaryScreen.kt`, `MainActivity.kt` (`dailyStepGoal`, recibido por `DataClient`/`MessageClient` desde el teléfono vía `Constants.GOAL_PATH`) | Hecho |
| RF-15 | Brillo adaptativo de pantalla | — | No implementado (prioridad Baja en el documento; fuera de alcance) |

### 4.4 Módulo de transferencia de datos al smartphone

| ID | Descripción corta | Archivo(s) | Estado |
|---|---|---|---|
| RF-16 | Sincronización reloj→teléfono vía BLE (Wearable Data Layer API) | `MainActivity.kt` (`sendMetricsToPhone`, `MessageClient`/`DataClient`/`CapabilityClient`, sin librerías BLE de terceros) | Hecho |
| RF-17 | Encolar datos sin conexión y transmitir al reconectar | [sync/PendingSyncQueue.kt](../reloj/src/main/java/com/example/reloj/sync/PendingSyncQueue.kt), `MainActivity.kt` (`flushPendingQueue`) | Hecho |
| RF-18 | Notificar en el reloj cuando la sync se complete | `MainActivity.kt` (`notifySyncComplete`, `NotificationCompat`) | Hecho |
| RF-19 | Companion muestra datos en tiempo real | [app/MainActivity.kt](../app/src/main/java/com/example/mobileapp/MainActivity.kt) (`onMessageReceived`), [ui/HistoryScreen.kt](../app/src/main/java/com/example/mobileapp/ui/HistoryScreen.kt) | Hecho |
| RF-20 | Historial en base de datos local (Room) | [data/local/AppDatabase.kt](../app/src/main/java/com/example/mobileapp/data/local/AppDatabase.kt), `HealthMetricDao.kt`, `HealthMetricEntity.kt` | Hecho |
| RF-21 | Gráficas de BPM y pasos (hoy / últimos 7 días) | `HistoryScreen.kt`, [ui/charts/LineChart.kt](../app/src/main/java/com/example/mobileapp/ui/charts/LineChart.kt) | Hecho |

## Requerimientos no funcionales

| ID | Descripción corta | Archivo(s) / mecanismo | Estado |
|---|---|---|---|
| RNF-01 | Latencia sensor→pantalla < 2s | Flujo síncrono `SensorEventListener` → `metricsState` (Compose recomposition inmediata) | Cumple por diseño; no medido formalmente con Systrace |
| RNF-02 | Transmisión BLE < 3s | `MessageClient.sendMessage` es asíncrono de baja latencia; verificado manualmente con logs (`Log.d("sendMessage", ...)`) | Cumple en pruebas manuales |
| RNF-03 | < 20% batería extra en 8h | No medido (requiere Battery Historian en dispositivo físico) | Pendiente de medición formal |
| RNF-04 | Disponibilidad 99% / <1 crash por 100h | N/A — no hay suite de pruebas de estrés | Pendiente |
| RNF-05 | Recuperación de sensor en <10s | `HealthSensorManager.kt` reintenta registro en cada `readings()` collect; sin backoff explícito | Parcial |
| RNF-06 | Máx. 3 interacciones para cualquier función | 2 pantallas por swipe + 1 tap (ej. abrir Config en teléfono) | Cumple |
| RNF-07 | Texto ≥14sp, contraste WCAG AA | Tamaños de fuente en `SummaryScreen.kt`/`DetailScreen.kt` (9–32sp: algunos textos secundarios están por debajo de 14sp) | Parcial — revisar tamaños de texto secundario |
| RNF-08 | BLE cifrado (Secure Connections) | Delegado al pareo Bluetooth del sistema operativo (Wearable Data Layer API no expone esta configuración a nivel de app) | Depende del SO, no de la app |
| RNF-09 | Consentimiento explícito antes de recolectar datos | [ui/ConsentScreen.kt](../app/src/main/java/com/example/mobileapp/ui/ConsentScreen.kt), [data/prefs/UserPreferences.kt](../app/src/main/java/com/example/mobileapp/data/prefs/UserPreferences.kt) | Hecho (en el teléfono; el reloj no bloquea captura local por ser el origen del dato) |
| RNF-10 | Base de datos no accesible a otras apps | Room por defecto (almacenamiento interno privado de la app) | Cumple por defecto de la plataforma |
| RNF-11 | Compatible con Wear OS 3.0+ | `reloj/build.gradle.kts` (`minSdk = 30`) | Cumple |
| RNF-12 | Compatible con Android 10+ (API 29) | `app/build.gradle` (`minSdk 29`) | Cumple |

## Backend (requisito adicional del curso, no del documento de requerimientos)

| Componente | Archivo(s) |
|---|---|
| API REST Node.js + Express | [backend/src/server.js](../backend/src/server.js), `routes/sensores.routes.js`, `controllers/sensores.controller.js` |
| Persistencia Postgres (espejo secundario) | `backend/src/db/pool.js`, [backend/sql/schema.sql](../backend/sql/schema.sql) |
| Cliente HTTP en el companion | [data/remote/HealthWatchApiClient.kt](../app/src/main/java/com/example/mobileapp/data/remote/HealthWatchApiClient.kt), `HealthRepository.kt` (`syncPendingToBackend`) |

Room es la fuente de verdad (documento §2.1: "el smartphone cumple el rol de servidor local");
el backend HTTP es un espejo secundario que se sincroniza después de guardar en Room.
