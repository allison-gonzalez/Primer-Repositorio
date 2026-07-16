# HealthWatch API

API REST (Node.js + Express + Postgres) para el proyecto HealthWatch App. Actua como
espejo secundario de las lecturas de sensores: la fuente de verdad es la base de
datos Room de la app companion en el telefono (documento de requerimientos, seccion
2.1); esta API existe para cumplir el requisito de la asignatura de exponer una
API HTTP propia.

## Requisitos

- Node.js 18+
- PostgreSQL 14+

## Instalacion

```bash
cd backend
npm install
cp .env.example .env
# Edita .env con las credenciales de tu Postgres local
```

## Crear la base de datos

```bash
createdb healthwatch
psql -d healthwatch -f sql/schema.sql
```

## Levantar el servidor

```bash
npm run dev    # con recarga automatica (nodemon)
# o
npm start
```

El servidor escucha por defecto en `http://localhost:5000`.

## Endpoints

| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | `/api/health` | Liveness check |
| POST | `/api/Sensores` | Registra una lectura de sensores |
| GET | `/api/Sensores?dispositivoId=&desde=&hasta=` | Lista lecturas (filtros opcionales) |

### Ejemplo: registrar una lectura

```bash
curl -X POST http://localhost:5000/api/Sensores \
  -H "Content-Type: application/json" \
  -d '{
    "dispositivoId": "Watch-001",
    "heartRate": 78,
    "steps": 1200,
    "accelX": 0.1,
    "accelY": 9.8,
    "accelZ": 0.0,
    "distanciaM": 900,
    "calorias": 45.2,
    "nivelActividad": "Moderado",
    "zonaCardiaca": "Cardio"
  }'
```

### Ejemplo: consultar el historial

```bash
curl "http://localhost:5000/api/Sensores?dispositivoId=Watch-001"
```

## Nota sobre la app Android

El cliente OkHttp de la app companion (`HealthWatchApiClient.kt`) apunta a la URL
definida en `gradle.properties` (`API_BASE_URL`). Por defecto usa `10.0.2.2`, la IP
de loopback al host desde el emulador de Android. En un dispositivo fisico, cambia
esa propiedad a la IP LAN real de la maquina donde corre este backend.
