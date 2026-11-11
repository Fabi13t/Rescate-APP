const express = require('express');
const path = require('path');
const crypto = require('crypto');
const multer = require('multer');
const { query, tx } = require('../db');
const { HttpError, wrap } = require('../errors');
const { requireAuth, requireMerchant } = require('../auth');
const { PACK_COLS, PACK_JOINS, RESERVATION_SELECT, toPack, toReservation, toPayment } = require('../mappers');
const { notify } = require('../notify');

const router = express.Router();
router.use(requireAuth, requireMerchant);

const upload = multer({
  storage: multer.diskStorage({
    destination: path.join(__dirname, '..', '..', 'uploads'),
    filename: (_req, file, cb) => cb(null, `${Date.now()}-${crypto.randomBytes(4).toString('hex')}${path.extname(file.originalname) || '.jpg'}`)
  }),
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: (_req, file, cb) => cb(null, /^image\//.test(file.mimetype))
});

router.post('/packs', upload.single('photo'), wrap(async (req, res) => {
  if (!req.verified) throw new HttpError(403, 'Tu comercio aún está por verificar. Podrás publicar cuando sea aprobado.');
  if (!req.localId) throw new HttpError(422, 'Registra primero la dirección de tu local.');
  let d;
  try {
    d = typeof req.body.data === 'string' ? JSON.parse(req.body.data) : req.body;
  } catch {
    throw new HttpError(422, 'Datos del pack inválidos.');
  }
  const normal = Number(d.normalPrice);
  const rescue = Number(d.rescuePrice);
  const qty = Number(d.quantity);
  if (!d.title) throw new HttpError(422, 'Ponle un nombre al pack.');
  if (!(rescue > 0) || !(normal > rescue)) throw new HttpError(422, 'El precio rescate debe ser menor al precio real.');
  if (!(qty > 0)) throw new HttpError(422, 'La cantidad debe ser mayor a 0.');
  if (!/^\d{2}:\d{2}$/.test(String(d.pickupUntil || ''))) throw new HttpError(422, 'La hora límite no es válida.');

  const perishable = d.perishable !== false;
  const photo = req.file ? `/uploads/${req.file.filename}` : null;
  const { rows } = await query(
    `INSERT INTO packs (local_id, categoria_id, titulo, descripcion, url_foto, precio_original, precio_rescate,
                        stock_inicial, stock_disponible, peso_aprox_kg, es_perecible, fecha_vencimiento, hora_limite_recojo)
     VALUES ($1,
             COALESCE((SELECT id FROM categorias WHERE alias = $2),
                      (SELECT c.id FROM categorias c JOIN perfiles_comercio pc ON pc.rubro ILIKE c.nombre || '%' WHERE pc.id = $11 LIMIT 1),
                      1),
             $3, $4, $5, $6, $7, $8, $8, $9, $10,
             (now() AT TIME ZONE 'America/Lima')::date + CASE WHEN $10 THEN 0 ELSE 7 END,
             (((now() AT TIME ZONE 'America/Lima')::date + $12::time) AT TIME ZONE 'America/Lima'))
     RETURNING id`,
    [req.localId, d.category || null, d.title, d.description || null, photo, normal, rescue, qty,
      Number(d.kgPerPack) || 1, perishable, req.perfilId, d.pickupUntil]
  );

  const pack = (await query(`SELECT ${PACK_COLS} FROM packs p ${PACK_JOINS} WHERE p.id = $1`, [rows[0].id])).rows[0];
  const vecinos = await query(
    `SELECT u.id FROM usuarios u JOIN preferencias_notificacion pn ON pn.usuario_id = u.id
      JOIN locales_comercio l ON l.id = $1
      WHERE u.rol = 'VECINO' AND pn.packs_nuevos AND u.zona_busqueda_id = l.distrito_id`,
    [req.localId]
  );
  for (const v of vecinos.rows) {
    await notify(null, v.id, 'PACK', '¡Pack disponible cerca!',
      `${pack.pc_nombre} publicó ${pack.p_titulo} a S/ ${Number(pack.p_precio_rescate).toFixed(2)}`, 'PACK', pack.p_id);
  }
  res.status(201).json(toPack(pack));
}));

router.get('/packs', wrap(async (req, res) => {
  const { rows } = await query(
    `SELECT ${PACK_COLS} FROM packs p ${PACK_JOINS} WHERE pc.id = $1 ORDER BY p.creado_en DESC`, [req.perfilId]
  );
  res.json(rows.map((r) => toPack(r)));
}));

router.get('/orders', wrap(async (req, res) => {
  const { rows } = await query(
    `${RESERVATION_SELECT} WHERE pc.id = $1 AND r.estado <> 'RETENIDA' ORDER BY r.reservada_en DESC LIMIT 200`, [req.perfilId]
  );
  res.json(rows.map(toReservation));
}));

const PAYMENT_SELECT = `
  SELECT pg.*, r.codigo, r.usuario_id AS cliente_id, r.pack_id, r.cantidad, u.nombres || ' ' || u.apellidos AS cliente, p.titulo
    FROM pagos pg
    JOIN reservas r ON r.id = pg.reserva_id
    JOIN usuarios u ON u.id = r.usuario_id
    JOIN packs p ON p.id = r.pack_id
    JOIN locales_comercio l ON l.id = p.local_id`;

router.get('/payments', wrap(async (req, res) => {
  const { rows } = await query(
    `${PAYMENT_SELECT} WHERE l.perfil_id = $1 ORDER BY (pg.estado = 'PENDIENTE') DESC, pg.reportado_en DESC LIMIT 200`,
    [req.perfilId]
  );
  res.json(rows.map(toPayment));
}));

async function reviewPayment(req, approve) {
  return tx(async (db) => {
    const { rows } = await db.query(`${PAYMENT_SELECT} WHERE pg.id = $1 AND l.perfil_id = $2 FOR UPDATE OF pg`, [req.params.id, req.perfilId]);
    const pg = rows[0];
    if (!pg) throw new HttpError(404, 'Pago no encontrado.');
    if (pg.estado !== 'PENDIENTE') throw new HttpError(409, `Este pago ya fue ${pg.estado.toLowerCase()}.`);
    if (approve) {
      await db.query(`UPDATE pagos SET estado = 'CONFIRMADO', revisado_en = now() WHERE id = $1`, [pg.id]);
      await db.query(`UPDATE reservas SET estado = 'CONFIRMADA', preparada_en = now() WHERE id = $1`, [pg.reserva_id]);
      await notify(db, pg.cliente_id, 'PAGO', '¡Pago confirmado!', `Ya puedes recoger tu ${pg.titulo}. Código ${pg.codigo}.`, 'RESERVA', pg.reserva_id);
    } else {
      const motivo = (req.body && req.body.reason) || 'La operación no figura en el Yape del comercio.';
      await db.query(`UPDATE pagos SET estado = 'RECHAZADO', motivo_rechazo = $2, revisado_en = now() WHERE id = $1`, [pg.id, motivo]);
      await db.query(`UPDATE reservas SET estado = 'RECHAZADA', cancelada_en = now(), cancelada_por = 'COMERCIO', motivo_cancelacion = $2 WHERE id = $1`, [pg.reserva_id, motivo]);
      await db.query('UPDATE packs SET stock_disponible = stock_disponible + $1 WHERE id = $2', [pg.cantidad, pg.pack_id]);
      await notify(db, pg.cliente_id, 'PAGO', 'Pago rechazado', `${motivo} Reserva ${pg.codigo}.`, 'RESERVA', pg.reserva_id);
    }
    const updated = await db.query(`${PAYMENT_SELECT} WHERE pg.id = $1`, [pg.id]);
    return toPayment(updated.rows[0]);
  });
}

router.patch('/payments/:id/approve', wrap(async (req, res) => res.json(await reviewPayment(req, true))));
router.patch('/payments/:id/reject', wrap(async (req, res) => res.json(await reviewPayment(req, false))));

router.patch('/reservations/:code/pickup', wrap(async (req, res) => {
  const id = await tx(async (db) => {
    const { rows } = await db.query(
      `SELECT r.id, r.estado, r.usuario_id FROM reservas r JOIN packs p ON p.id = r.pack_id
         JOIN locales_comercio l ON l.id = p.local_id
        WHERE r.codigo = $1 AND l.perfil_id = $2 FOR UPDATE OF r`,
      [String(req.params.code).toUpperCase(), req.perfilId]
    );
    const r = rows[0];
    if (!r) throw new HttpError(404, `No se encontró la reserva ${req.params.code} en tu comercio.`);
    if (r.estado !== 'CONFIRMADA') throw new HttpError(409, `La reserva está en estado: ${r.estado}.`);
    await db.query(`UPDATE reservas SET estado = 'RECOGIDA', recogida_en = now() WHERE id = $1`, [r.id]);
    await notify(db, r.usuario_id, 'CALIFICACION', '¿Qué tal tu pack?', 'Califica al comercio y ayuda a otros vecinos.', 'RESERVA', r.id);
    return r.id;
  });
  const { rows } = await query(`${RESERVATION_SELECT} WHERE r.id = $1`, [id]);
  res.json(toReservation(rows[0]));
}));

router.put('/yape', wrap(async (req, res) => {
  const phone = String(req.body.phone || '').replace(/\D/g, '');
  if (phone.length !== 9) throw new HttpError(422, 'Ingresa un celular válido de 9 dígitos.');
  const upd = await query(
    `UPDATE medios_pago_comercio SET celular = $1, actualizado_en = now()
      WHERE id = (SELECT id FROM medios_pago_comercio WHERE perfil_id = $2 ORDER BY id LIMIT 1)`,
    [phone, req.perfilId]
  );
  if (upd.rowCount === 0) {
    await query(
      `INSERT INTO medios_pago_comercio (perfil_id, tipo, celular, titular)
       SELECT $1, 'YAPE', $2, nombre_titular FROM perfiles_comercio WHERE id = $1`,
      [req.perfilId, phone]
    );
  }
  res.json({ message: 'Yape actualizado.' });
}));

module.exports = router;
