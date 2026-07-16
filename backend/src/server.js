require('dotenv').config();
const express = require('express');
const cors = require('cors');
const sensoresRoutes = require('./routes/sensores.routes');
const errorHandler = require('./middleware/errorHandler');

const app = express();
app.use(cors());
app.use(express.json());

app.get('/api/health', (req, res) => res.json({ status: 'ok' }));
app.use('/api', sensoresRoutes);

app.use(errorHandler);

const port = process.env.PORT || 5000;
app.listen(port, () => {
  console.log(`HealthWatch API escuchando en el puerto ${port}`);
});
