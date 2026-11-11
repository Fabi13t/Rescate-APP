const express = require('express');
const { query, tx } = require('../db');
const { HttpError, wrap } = require('../errors');
const { requireAuth } = require('../auth');
const { RESERVATION_SELECT, toReservation } = require('../mappers');
const { releaseExpiredHolds } = require('../jobs');
const { notify } = require('../notify');

const router = express.Router();
router.use(requireAuth);

const HOLD_MINUTES = 3;

async function loadReservation(id, db) {
  const runner = db || { query };
  const { rows } = await runner.query(`${RESERVATION_SELECT} WHERE r.id = $1`, [id]);
  return rows[0] ? toReservation(rows[0]) : null;
}

router.post('/', wrap(async (req, res) => {
  const packId = Number(req.body.packId);
  const qty = Number(req.body.quantity) || 1;
  if (qty < 1 || qty > 10) throw new HttpError(422, 'La cantidad no es válida.');
  await releaseExpiredHolds();

  const id = await tx(async (db) => {
    const { rows } = await db.query(
      `SELECT id, stock_disponible, precio_rescate, peso_aprox_kg, estado FROM packs WHERE id = $1 FOR UPDATE`,
      [packId]
    );
    const pack = rows[0];
    if (!pack || pack.estado !== 'EN_PROCESO') throw new HttpError(410, 'Este pack ya no está disponible.');
    if (pack.stock_disponible <= 0) throw new HttpError(409, 'Alguien se adelantó: este pack se agotó.');
    if (pack.stock_disponible < qty) throw new HttpError(409, `Solo quedan ${pack.stock_disponible} packs disponibles.`);

    await db.query('UPDATE packs SET stock_disponible = stock_disponible - $1 WHERE id = $2', [qty, packId]);
    const ins = await db.query(
      `INSERT INTO reservas (codigo, usuario_id, pack_id, cantidad, precio_unitario, total, peso_kg, estado, expira_en)
       VALUES ('RSC-' || lpad(nextval('seq_codigo_reserva')::text, 6, '0'), $1, $2, $3::int, $4::numeric,
               $4::numeric * $3::int, $5::numeric * $3::int,
               'RETENIDA', now() + ($6::text || ' minutes')::interval)
       RETURNING id`,
      [req.user.id, packId, qty, pack.precio_rescate, pack.peso_aprox_kg, String(HOLD_MINUTES)]
    );
    return ins.rows[0].id;
  });
  res.status(201).json(await loadReservation(id));
}));

router.post('/:id/yape', wrap(async (req, res) => {
  const reference = String(req.body.reference || '').trim();
  if (reference.length < 4) throw new HttpError(422, 'Ingresa el número de operación de Yape.');

  await tx(async (db) => {
    const { rows } = await db.query(
      `SELECT r.*, pc.usuario_id AS comercio_usuario_id, u.nombres || ' ' || u.apellidos AS cliente
         FROM reservas r
         JOIN packs p ON p.id = r.pack_id
         JOIN locales_comercio l ON l.id = p.local_id
         JOIN perfiles_comercio pc ON pc.id = l.perfil_id
         JOIN usuarios u ON u.id = r.usuario_id
        WHERE r.id = $1 AND r.usuario_id = $2 FOR UPDATE OF r`,
      [req.params.id, req.user.id]
    );
    const r = rows[0];
    if (!r) throw new HttpError(404, 'Reserva no encontrada.');
    if (r.estado !== 'RETENIDA') throw new HttpError(409, 'Esta reserva ya no está esperando pago.');
    if (new Date(r.expira_en) < new Date()) throw new HttpError(410, 'El tiempo de reserva terminó y el pack se liberó.');

    await db.query(`INSERT INTO pagos (reserva_id, metodo, monto, referencia) VALUES ($1, 'YAPE', $2, $3)`, [r.id, r.total, reference]);
    await db.query(`UPDATE reservas SET estado = 'PAGO_EN_REVISION' WHERE id = $1`, [r.id]);
    await notify(db, r.comercio_usuario_id, 'PAGO', 'Nuevo pago por validar',
      `${r.cliente} reportó un Yape de S/ ${Number(r.total).toFixed(2)} (op. ${reference}).`, 'RESERVA', r.id);
  });
  res.json(await loadReservation(req.params.id));
}));

router.patch('/:id/cancel', wrap(async (req, res) => {
  await tx(async (db) => {
    const { rows } = await db.query(
      'SELECT id, estado, pack_id, cantidad FROM reservas WHERE id = $1 AND usuario_id = $2 FOR UPDATE',
      [req.params.id, req.user.id]
    );
    const r = rows[0];
    if (!r) throw new HttpError(404, 'Reserva no encontrada.');
    if (!['RETENIDA', 'PAGO_EN_REVISION', 'CONFIRMADA'].includes(r.estado)) {
      throw new HttpError(409, 'Esta reserva ya no se puede cancelar.');
    }
    await db.query(
      `UPDATE reservas SET estado = 'CANCELADA', cancelada_en = now(), cancelada_por = 'VECINO',
              motivo_cancelacion = COALESCE($2, 'Cancelada por el vecino') WHERE id = $1`,
      [r.id, req.body && req.body.reason ? req.body.reason : null]
    );
    await db.query('UPDATE packs SET stock_disponible = stock_disponible + $1 WHERE id = $2', [r.cantidad, r.pack_id]);
  });
  res.json(await loadReservation(req.params.id));
}));

router.get('/me', wrap(async (req, res) => {
  const { rows } = await query(
    `${RESERVATION_SELECT} WHERE r.usuario_id = $1 AND r.estado <> 'RETENIDA' ORDER BY r.reservada_en DESC`,
    [req.user.id]
  );
  res.json(rows.map(toReservation));
}));

router.get('/:id', wrap(async (req, res) => {
  const r = await loadReservation(req.params.id);
  if (!r) throw new HttpError(404, 'Reserva no encontrada.');
  res.json(r);
}));

module.exports = router;
