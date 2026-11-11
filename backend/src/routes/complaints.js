const express = require('express');
const { query, tx } = require('../db');
const { HttpError, wrap } = require('../errors');
const { requireAuth, requireMerchant } = require('../auth');
const { toComplaint } = require('../mappers');
const { notify } = require('../notify');

const router = express.Router();
router.use(requireAuth);

const PLAZO_DIAS_HABILES = Number(process.env.RECLAMO_PLAZO_DIAS || 15);

function addBusinessDays(n) {
  const d = new Date();
  let added = 0;
  while (added < n) {
    d.setDate(d.getDate() + 1);
    const day = d.getDay();
    if (day !== 0 && day !== 6) added += 1;
  }
  return d.toISOString().slice(0, 10);
}

const COMPLAINT_SELECT = `
  SELECT rc.*, pc.nombre_comercial, u.nombres || ' ' || u.apellidos AS cliente, r.codigo AS reserva_codigo
    FROM reclamos rc
    JOIN perfiles_comercio pc ON pc.id = rc.perfil_id
    JOIN usuarios u ON u.id = rc.usuario_id
    LEFT JOIN reservas r ON r.id = rc.reserva_id`;

router.post('/complaints', wrap(async (req, res) => {
  const b = req.body;
  const type = b.type === 'QUEJA' ? 'QUEJA' : 'RECLAMO';
  const detail = String(b.detail || '').trim();
  const request = String(b.request || '').trim();
  if (detail.length < 10) throw new HttpError(422, 'Describe lo ocurrido (mínimo 10 caracteres).');
  if (request.length < 5) throw new HttpError(422, 'Indica qué solicitas al comercio.');

  let perfilId = null;
  let reservaId = null;
  if (b.reservationId) {
    const { rows } = await query(
      `SELECT r.id, l.perfil_id FROM reservas r JOIN packs p ON p.id = r.pack_id JOIN locales_comercio l ON l.id = p.local_id
        WHERE r.id = $1 AND r.usuario_id = $2`,
      [b.reservationId, req.user.id]
    );
    if (!rows[0]) throw new HttpError(404, 'No encontramos esa reserva en tu cuenta.');
    reservaId = rows[0].id;
    perfilId = rows[0].perfil_id;
  } else if (b.storeId) {
    const { rows } = await query('SELECT perfil_id FROM locales_comercio WHERE id = $1', [b.storeId]);
    if (!rows[0]) throw new HttpError(404, 'Comercio no encontrado.');
    perfilId = rows[0].perfil_id;
  } else {
    throw new HttpError(422, 'Indica la reserva o el comercio del reclamo.');
  }

  const id = await tx(async (db) => {
    const { rows } = await db.query(
      `INSERT INTO reclamos (codigo, usuario_id, perfil_id, reserva_id, tipo, bien_contratado, monto_reclamado,
                             detalle, pedido, fecha_limite_respuesta)
       VALUES ('REC-' || to_char(now(), 'YYYY') || '-' || lpad(nextval('seq_codigo_reclamo')::text, 6, '0'),
               $1, $2, $3, $4, $5, $6, $7, $8, $9)
       RETURNING id, codigo`,
      [req.user.id, perfilId, reservaId, type, b.goodType === 'SERVICIO' ? 'SERVICIO' : 'PRODUCTO',
        b.amount ? Number(b.amount) : null, detail, request, addBusinessDays(PLAZO_DIAS_HABILES)]
    );
    const owner = await db.query('SELECT usuario_id FROM perfiles_comercio WHERE id = $1', [perfilId]);
    await notify(db, owner.rows[0].usuario_id, 'RECLAMO', `Nueva ${type === 'QUEJA' ? 'queja' : 'reclamación'} recibida`,
      `Tienes el ${rows[0].codigo} por responder.`, 'RECLAMO', rows[0].id);
    return rows[0].id;
  });
  const out = await query(`${COMPLAINT_SELECT} WHERE rc.id = $1`, [id]);
  res.status(201).json(toComplaint(out.rows[0]));
}));

router.get('/complaints/me', wrap(async (req, res) => {
  const { rows } = await query(`${COMPLAINT_SELECT} WHERE rc.usuario_id = $1 ORDER BY rc.creado_en DESC`, [req.user.id]);
  res.json(rows.map(toComplaint));
}));

router.get('/merchant/complaints', requireMerchant, wrap(async (req, res) => {
  const { rows } = await query(
    `${COMPLAINT_SELECT} WHERE rc.perfil_id = $1
      ORDER BY (rc.estado IN ('PENDIENTE','EN_REVISION')) DESC, rc.fecha_limite_respuesta ASC, rc.creado_en DESC`,
    [req.perfilId]
  );
  res.json(rows.map(toComplaint));
}));

router.patch('/merchant/complaints/:id/respond', requireMerchant, wrap(async (req, res) => {
  const response = String(req.body.response || '').trim();
  if (response.length < 10) throw new HttpError(422, 'Escribe una respuesta (mínimo 10 caracteres).');
  await tx(async (db) => {
    const { rows } = await db.query(
      `UPDATE reclamos SET respuesta = $1, estado = 'RESPONDIDO', respondido_en = now()
        WHERE id = $2 AND perfil_id = $3 AND estado IN ('PENDIENTE', 'EN_REVISION')
        RETURNING usuario_id, codigo`,
      [response, req.params.id, req.perfilId]
    );
    if (!rows[0]) throw new HttpError(409, 'Este reclamo ya fue respondido o no existe.');
    await notify(db, rows[0].usuario_id, 'RECLAMO', 'Respondieron tu reclamo', `El comercio respondió el ${rows[0].codigo}.`, 'RECLAMO', Number(req.params.id));
  });
  const out = await query(`${COMPLAINT_SELECT} WHERE rc.id = $1`, [req.params.id]);
  res.json(toComplaint(out.rows[0]));
}));

module.exports = router;
