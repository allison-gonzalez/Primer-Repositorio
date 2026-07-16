-- Esquema de la tabla espejo de lecturas de sensores sincronizadas desde la app companion.
-- Room (en el telefono) es la fuente de verdad; esta tabla es un espejo secundario
-- para el requisito de la asignatura de exponer una API HTTP propia.

CREATE TABLE IF NOT EXISTS sensores_lecturas (
    id SERIAL PRIMARY KEY,
    dispositivo_id TEXT NOT NULL,
    heart_rate INTEGER,
    steps INTEGER,
    accel_x REAL,
    accel_y REAL,
    accel_z REAL,
    distancia_m REAL,
    calorias REAL,
    nivel_actividad TEXT,
    zona_cardiaca TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sensores_lecturas_dispositivo_created
    ON sensores_lecturas (dispositivo_id, created_at);
