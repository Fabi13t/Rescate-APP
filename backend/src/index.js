require('dotenv').config();

const missing = ['DATABASE_URL', 'JWT_SECRET'].filter((k) => !process.env[k]);
if (missing.length) {
  console.error(`Faltan variables de entorno: ${missing.join(', ')}. Revisa el archivo .env`);
  process.exit(1);
}
const path = require('path');
const express = require('express');
const cors = require('cors');
const { errorHandler } = require('./errors');
const { startJobs } = require('./jobs');
const { query } = require('./db');

const app = express();
app.use(cors());
app.use(express.json({ limit: '1mb' }));
app.use('/uploads', express.static(path.join(__dirname, '..', 'uploads')));

app.get(['/', '/api/v1/health'], async (_req, res) => {
  try {
    const { rows } = await query('SELECT count(*)::int AS tablas FROM information_schema.tables WHERE table_schema = $1', ['public']);
    res.json({ ok: true, api: 'Rescate', db: 'conectada', tablas: rows[0].tablas });
  } catch (e) {
    res.status(500).json({ ok: false, db: 'sin conexión', error: e.message });
  }
});

const api = express.Router();
api.use('/auth', require('./routes/auth'));
api.use('/', require('./routes/packs'));
api.use('/reservations', require('./routes/reservations'));
api.use('/impact', require('./routes/impact'));
api.use('/donations', require('./routes/donations'));
api.use('/notifications', require('./routes/notifications'));
api.use('/merchant', require('./routes/merchant'));
api.use('/', require('./routes/complaints'));
api.use('/', require('./routes/ratings'));
app.use('/api/v1', api);

app.use((req, res) => res.status(404).json({ message: `Ruta no encontrada: ${req.method} ${req.path}` }));
app.use(errorHandler);

if (require.main === module) {
  const PORT = process.env.PORT || 3000;
  app.listen(PORT, () => {
    console.log(`✅ API Rescate escuchando en http://localhost:${PORT}/api/v1`);
    startJobs();
  });
}

module.exports = app;
