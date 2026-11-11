const express = require('express');
const { query } = require('../db');
const { HttpError, wrap } = require('../errors');
const { requireAuth } = require('../auth');
const { toDonation } = require('../mappers');

const router = express.Router();
router.use(requireAuth);

const DONATION_SELECT = `
  SELECT dn.*, ta.nombre AS tipo, d.nombre AS distrito,
         (dn.fecha_caducidad - (now() AT TIME ZONE 'America/Lima')::date) AS dias
    FROM donaciones dn
    JOIN tipos_alimento ta ON ta.id = dn.tipo_alimento_id
    JOIN usuarios u ON u.id = dn.usuario_id
    LEFT JOIN distritos d ON d.id = u.distrito_id`;

router.post('/', wrap(async (req, res) => {
  const b = req.body;
  const days = Number(b.expiresInDays);
  const kg = Number(b.kg);
  if (!b.title) throw new HttpError(422, 'Cuéntanos qué vas a donar.');
  if (!(kg > 0)) throw new HttpError(422, 'La cantidad debe ser mayor a 0.');
  const perishable = b.foodType !== 'No perecible';
  if (perishable && days > 7) throw new HttpError(422, 'La caducidad máxima permitida es de 7 días.');

  const { rows } = await query(
    `INSERT INTO donaciones (usuario_id, tipo_alimento_id, cantidad_kg, es_perecible, fecha_caducidad, observaciones)
     VALUES ($1, COALESCE((SELECT id FROM tipos_alimento WHERE nombre = $2), 1), $3, $4,
             (now() AT TIME ZONE 'America/Lima')::date + $5::int, $6)
     RETURNING id`,
    [req.user.id, b.foodType, kg, perishable, Math.max(0, days || 0), b.notes ? `${b.title} · ${b.notes}` : b.title]
  );
  const out = await query(`${DONATION_SELECT} WHERE dn.id = $1`, [rows[0].id]);
  res.status(201).json(toDonation(out.rows[0]));
}));

router.get('/me', wrap(async (req, res) => {
  const { rows } = await query(`${DONATION_SELECT} WHERE dn.usuario_id = $1 ORDER BY dn.creada_en DESC`, [req.user.id]);
  res.json(rows.map(toDonation));
}));

router.patch('/:id/withdraw', wrap(async (req, res) => {
  const upd = await query(
    `UPDATE donaciones SET estado = 'RETIRADA' WHERE id = $1 AND usuario_id = $2 AND estado = 'PUBLICADA'`,
    [req.params.id, req.user.id]
  );
  if (!upd.rowCount) throw new HttpError(409, 'Esta donación ya no se puede retirar.');
  res.json({ message: 'Publicación retirada.' });
}));

module.exports = router;
