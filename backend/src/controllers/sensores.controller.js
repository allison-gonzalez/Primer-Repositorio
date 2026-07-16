const pool = require('../db/pool');

async function crearLectura(req, res, next) {
  try {
    const {
      dispositivoId,
      heartRate,
      steps,
      accelX,
      accelY,
      accelZ,
      distanciaM,
      calorias,
      nivelActividad,
      zonaCardiaca,
    } = req.body;

    if (!dispositivoId) {
      return res.status(400).json({ error: 'dispositivoId es requerido' });
    }

    const result = await pool.query(
      `INSERT INTO sensores_lecturas
        (dispositivo_id, heart_rate, steps, accel_x, accel_y, accel_z, distancia_m, calorias, nivel_actividad, zona_cardiaca)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
       RETURNING *`,
      [
        dispositivoId,
        heartRate ?? null,
        steps ?? null,
        accelX ?? null,
        accelY ?? null,
        accelZ ?? null,
        distanciaM ?? null,
        calorias ?? null,
        nivelActividad ?? null,
        zonaCardiaca ?? null,
      ]
    );

    res.status(201).json(result.rows[0]);
  } catch (err) {
    next(err);
  }
}

async function listarLecturas(req, res, next) {
  try {
    const { dispositivoId, desde, hasta } = req.query;
    const conditions = [];
    const params = [];

    if (dispositivoId) {
      params.push(dispositivoId);
      conditions.push(`dispositivo_id = $${params.length}`);
    }
    if (desde) {
      params.push(new Date(desde));
      conditions.push(`created_at >= $${params.length}`);
    }
    if (hasta) {
      params.push(new Date(hasta));
      conditions.push(`created_at <= $${params.length}`);
    }

    const whereClause = conditions.length ? `WHERE ${conditions.join(' AND ')}` : '';
    const result = await pool.query(
      `SELECT * FROM sensores_lecturas ${whereClause} ORDER BY created_at DESC LIMIT 500`,
      params
    );

    res.json(result.rows);
  } catch (err) {
    next(err);
  }
}

module.exports = { crearLectura, listarLecturas };
