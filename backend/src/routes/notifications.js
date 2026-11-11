const express = require('express');
const { query } = require('../db');
const { wrap } = require('../errors');
const { requireAuth } = require('../auth');
const { relativeLabel } = require('../mappers');

const router = express.Router();
router.use(requireAuth);

router.get('/me', wrap(async (req, res) => {
  const { rows } = await query(
    'SELECT * FROM notificaciones WHERE usuario_id = $1 ORDER BY creada_en DESC LIMIT 50', [req.user.id]
  );
  res.json(rows.map((n) => ({
    id: Number(n.id), title: n.titulo, body: n.mensaje, time: relativeLabel(n.creada_en),
    type: ['PACK', 'ALERTA', 'RECORDATORIO', 'OFERTA', 'IMPACTO'].includes(n.tipo) ? n.tipo : 'ALERTA',
    read: n.leida
  })));
}));

router.patch('/read-all', wrap(async (req, res) => {
  await query('UPDATE notificaciones SET leida = TRUE WHERE usuario_id = $1', [req.user.id]);
  res.json({ message: 'ok' });
}));

router.get('/preferences', wrap(async (req, res) => {
  const { rows } = await query('SELECT * FROM preferencias_notificacion WHERE usuario_id = $1', [req.user.id]);
  res.json(rows[0] || {});
}));

router.put('/preferences', wrap(async (req, res) => {
  const b = req.body;
  const v = (k) => (typeof b[k] === 'boolean' ? b[k] : true);
  await query(
    `INSERT INTO preferencias_notificacion (usuario_id, packs_nuevos, alertas_stock, recordatorio_recojo, ofertas_dia, resumen_impacto)
     VALUES ($1, $2, $3, $4, $5, $6)
     ON CONFLICT (usuario_id) DO UPDATE SET packs_nuevos = $2, alertas_stock = $3, recordatorio_recojo = $4,
       ofertas_dia = $5, resumen_impacto = $6, actualizado_en = now()`,
    [req.user.id, v('packs_nuevos'), v('alertas_stock'), v('recordatorio_recojo'), v('ofertas_dia'), v('resumen_impacto')]
  );
  res.json({ message: 'Preferencias guardadas.' });
}));

router.post('/support', wrap(async (req, res) => {
  const { rows } = await query('SELECT correo FROM usuarios WHERE id = $1', [req.user.id]);
  await query('INSERT INTO tickets_soporte (usuario_id, correo, asunto, mensaje) VALUES ($1, $2, $3, $4)',
    [req.user.id, rows[0].correo, req.body.subject || 'Consulta', req.body.message || '']);
  res.status(201).json({ message: 'Recibimos tu mensaje. Te responderemos por correo.' });
}));

module.exports = router;
