const express = require('express');
const { query, tx } = require('../db');
const { HttpError, wrap } = require('../errors');
const { requireAuth, requireMerchant } = require('../auth');
const { toRating } = require('../mappers');
const { notify } = require('../notify');

const router = express.Router();
router.use(requireAuth);

const RATING_SELECT = `
  SELECT ca.*, pc.nombre_comercial, u.nombres || ' ' || left(u.apellidos, 1) || '.' AS cliente
    FROM calificaciones ca
    JOIN usuarios u ON u.id = ca.usuario_id
    JOIN locales_comercio l ON l.id = ca.local_id
    JOIN perfiles_comercio pc ON pc.id = l.perfil_id`;

router.post('/ratings', wrap(async (req, res) => {
  const stars = Number(req.body.stars);
  if (!Number.isInteger(stars) || stars < 1 || stars > 5) throw new HttpError(422, 'La puntuación debe ser de 1 a 5 estrellas.');
  const comment = req.body.comment ? String(req.body.comment).trim().slice(0, 300) : null;

  const id = await tx(async (db) => {
    const { rows } = await db.query(
      `SELECT r.id, r.estado, p.local_id, pc.usuario_id AS comercio_usuario_id
         FROM reservas r JOIN packs p ON p.id = r.pack_id
         JOIN locales_comercio l ON l.id = p.local_id JOIN perfiles_comercio pc ON pc.id = l.perfil_id
        WHERE r.id = $1 AND r.usuario_id = $2`,
      [req.body.reservationId, req.user.id]
    );
    const r = rows[0];
    if (!r) throw new HttpError(404, 'No encontramos esa reserva en tu cuenta.');
    if (r.estado !== 'RECOGIDA') throw new HttpError(409, 'Solo puedes calificar packs que ya recogiste.');
    const exists = await db.query('SELECT 1 FROM calificaciones WHERE reserva_id = $1', [r.id]);
    if (exists.rows.length) throw new HttpError(409, 'Ya calificaste esta reserva.');

    const ins = await db.query(
      `INSERT INTO calificaciones (reserva_id, usuario_id, local_id, puntuacion, comentario)
       VALUES ($1, $2, $3, $4, $5) RETURNING id`,
      [r.id, req.user.id, r.local_id, stars, comment]
    );
    await notify(db, r.comercio_usuario_id, 'CALIFICACION', `Nueva calificación: ${'★'.repeat(stars)}`,
      comment || 'Un cliente calificó tu comercio.', 'CALIFICACION', ins.rows[0].id);
    return ins.rows[0].id;
  });
  const out = await query(`${RATING_SELECT} WHERE ca.id = $1`, [id]);
  res.status(201).json(toRating(out.rows[0]));
}));

router.get('/merchant/ratings', requireMerchant, wrap(async (req, res) => {
  const { rows } = await query(`${RATING_SELECT} WHERE l.perfil_id = $1 ORDER BY ca.creada_en DESC`, [req.perfilId]);
  res.json(rows.map(toRating));
}));

router.patch('/merchant/ratings/:id/reply', requireMerchant, wrap(async (req, res) => {
  const reply = String(req.body.reply || '').trim().slice(0, 300);
  if (!reply) throw new HttpError(422, 'Escribe una respuesta.');
  const upd = await query(
    `UPDATE calificaciones ca SET respuesta_comercio = $1
       FROM locales_comercio l
      WHERE l.id = ca.local_id AND ca.id = $2 AND l.perfil_id = $3
      RETURNING ca.id`,
    [reply, req.params.id, req.perfilId]
  );
  if (!upd.rowCount) throw new HttpError(404, 'Calificación no encontrada.');
  const out = await query(`${RATING_SELECT} WHERE ca.id = $1`, [req.params.id]);
  res.json(toRating(out.rows[0]));
}));

module.exports = router;
