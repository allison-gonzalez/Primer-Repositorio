const express = require('express');
const { crearLectura, listarLecturas } = require('../controllers/sensores.controller');

const router = express.Router();

// Compatible con el JSON que ya arma el cliente OkHttp del companion Android.
router.post('/Sensores', crearLectura);
router.get('/Sensores', listarLecturas);

module.exports = router;
