const express = require('express');
const { query } = require('../db');
const { HttpError, wrap } = require('../errors');
const { requireAuth } = require('../auth');
const { PACK_COLS, PACK_JOINS, toPack, toStore, toRating } = require('../mappers');
const { releaseExpiredHolds } = require('../jobs');

const router = express.Router();
router.use(requireAuth);

router.get('/packs', wrap(async (req, res) => {
  await releaseExpiredHolds();
  const lat = parseFloat(req.query.lat);
  const lon = parseFloat(req.query.lon);
  const km = parseFloat(req.query.km) || 5;
  const params = [];
  let where = `WHERE p.estado = 'EN_PROCESO' AND l.activo AND pc.estado_verificacion = 'VERIFICADO'`;
  if (req.query.category) {
    params.push(req.query.category);
    where += ` AND c.alias = $${params.length}`;
  }
  if (req.query.q) {
    params.push(`%${req.query.q}%`);
    where += ` AND (p.titulo ILIKE $${params.length} OR pc.nombre_comercial ILIKE $${params.length} OR c.nombre ILIKE $${params.length})`;
  }
  const { rows } = await query(`SELECT ${PACK_COLS} FROM packs p ${PACK_JOINS} ${where} ORDER BY p.creado_en DESC`, params);
  let packs = rows.map((r) => toPack(r, lat, lon));
  if (Number.isFinite(lat) && Number.isFinite(lon)) {
    packs = packs.filter((p) => p.distanceMeters <= km * 1000).sort((a, b) => a.distanceMeters - b.distanceMeters);
  }
  res.json(packs);
}));

router.get('/packs/:id', wrap(async (req, res) => {
  const { rows } = await query(`SELECT ${PACK_COLS} FROM packs p ${PACK_JOINS} WHERE p.id = $1`, [req.params.id]);
  if (!rows[0]) throw new HttpError(404, 'Este pack ya no está disponible.');
  res.json(toPack(rows[0], parseFloat(req.query.lat), parseFloat(req.query.lon)));
}));

router.get('/stores', wrap(async (_req, res) => {
  const { rows } = await query(
    `SELECT DISTINCT ON (l.id) ${PACK_COLS} FROM packs p ${PACK_JOINS}
      WHERE l.activo AND pc.estado_verificacion = 'VERIFICADO' ORDER BY l.id, p.id`
  );
  res.json(rows.map(toStore));
}));

router.get('/stores/:id/ratings', wrap(async (req, res) => {
  const { rows } = await query(
    `SELECT ca.*, pc.nombre_comercial, u.nombres || ' ' || left(u.apellidos, 1) || '.' AS cliente
       FROM calificaciones ca
       JOIN usuarios u ON u.id = ca.usuario_id
       JOIN locales_comercio l ON l.id = ca.local_id
       JOIN perfiles_comercio pc ON pc.id = l.perfil_id
      WHERE ca.local_id = $1 ORDER BY ca.creada_en DESC LIMIT 50`,
    [req.params.id]
  );
  res.json(rows.map(toRating));
}));

router.post('/packs/:id/favorite', wrap(async (req, res) => {
  const del = await query('DELETE FROM favoritos WHERE usuario_id = $1 AND pack_id = $2', [req.user.id, req.params.id]);
  if (del.rowCount === 0) {
    await query('INSERT INTO favoritos (usuario_id, pack_id) VALUES ($1, $2)', [req.user.id, req.params.id]);
  }
  res.json({ favorite: del.rowCount === 0 });
}));

router.get('/favorites', wrap(async (req, res) => {
  const { rows } = await query(
    `SELECT ${PACK_COLS} FROM favoritos f JOIN packs p ON p.id = f.pack_id ${PACK_JOINS}
      WHERE f.usuario_id = $1 ORDER BY f.creado_en DESC`,
    [req.user.id]
  );
  res.json(rows.map((r) => toPack(r)));
}));

router.post('/packs/:id/alert', wrap(async (req, res) => {
  await query(
    `INSERT INTO alertas_stock (usuario_id, pack_id) VALUES ($1, $2)
     ON CONFLICT (usuario_id, pack_id) DO UPDATE SET activa = TRUE, notificada_en = NULL`,
    [req.user.id, req.params.id]
  );
  res.json({ message: 'Te avisaremos si se libera un pack.' });
}));

module.exports = router;
