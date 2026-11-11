const express = require('express');
const { query, tx } = require('../db');
const { wrap } = require('../errors');
const { requireAuth } = require('../auth');
const { limaParts, num, MESES } = require('../mappers');

const router = express.Router();
router.use(requireAuth);

const CO2_POR_KG = 0.5;
const DIAS = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

router.get('/me', wrap(async (req, res) => {
  const period = ['semanal', 'mensual', 'total'].includes(req.query.period) ? req.query.period : 'resumen';
  const since = { semanal: "now() - interval '7 days'", mensual: "now() - interval '28 days'", resumen: "now() - interval '6 months'", total: "'-infinity'::timestamptz" }[period];

  const { rows } = await query(
    `SELECT r.recogida_en, r.peso_kg, r.cantidad, p.precio_original, r.precio_unitario, p.local_id
       FROM reservas r JOIN packs p ON p.id = r.pack_id
      WHERE r.usuario_id = $1 AND r.estado = 'RECOGIDA' AND r.recogida_en >= ${since}
      ORDER BY r.recogida_en`,
    [req.user.id]
  );

  const now = limaParts(new Date());
  let buckets;
  let keyOf;
  if (period === 'semanal') {
    buckets = DIAS.map((label) => ({ label, kg: 0 }));
    keyOf = (d) => (new Date(d).toLocaleDateString('en-US', { timeZone: 'America/Lima', weekday: 'short' }));
    const idx = { Mon: 0, Tue: 1, Wed: 2, Thu: 3, Fri: 4, Sat: 5, Sun: 6 };
    rows.forEach((r) => { buckets[idx[keyOf(r.recogida_en)]].kg += num(r.peso_kg); });
  } else if (period === 'mensual') {
    buckets = [1, 2, 3, 4].map((n) => ({ label: `Sem ${n}`, kg: 0 }));
    rows.forEach((r) => {
      const daysAgo = Math.floor((Date.now() - new Date(r.recogida_en).getTime()) / 86400000);
      const i = Math.min(3, Math.max(0, 3 - Math.floor(daysAgo / 7)));
      buckets[i].kg += num(r.peso_kg);
    });
  } else if (period === 'total') {
    const years = [...new Set([now.y - 2, now.y - 1, now.y, ...rows.map((r) => limaParts(r.recogida_en).y)])].sort();
    buckets = years.map((y) => ({ label: String(y), kg: 0 }));
    rows.forEach((r) => { buckets.find((b) => b.label === String(limaParts(r.recogida_en).y)).kg += num(r.peso_kg); });
  } else {
    buckets = [];
    for (let i = 5; i >= 0; i -= 1) {
      const m = ((now.m - 1 - i) % 12 + 12) % 12;
      buckets.push({ label: MESES[m].charAt(0).toUpperCase() + MESES[m].slice(1), kg: 0, m: m + 1 });
    }
    rows.forEach((r) => {
      const b = buckets.find((x) => x.m === limaParts(r.recogida_en).m);
      if (b) b.kg += num(r.peso_kg);
    });
  }

  const kg = rows.reduce((s, r) => s + num(r.peso_kg), 0);
  const saved = rows.reduce((s, r) => s + (num(r.precio_original) - num(r.precio_unitario)) * num(r.cantidad), 0);
  const impact = {
    kg: +kg.toFixed(2),
    co2Kg: +(kg * CO2_POR_KG).toFixed(2),
    savedSoles: +saved.toFixed(2),
    packs: rows.reduce((s, r) => s + num(r.cantidad), 0),
    stores: new Set(rows.map((r) => String(r.local_id))).size,
    series: buckets.map((b) => ({ label: b.label, kg: +b.kg.toFixed(2) }))
  };

  await tx(async (db) => {
    const rep = await db.query(
      `INSERT INTO reportes_impacto (usuario_id, tipo_periodo, kg_rescatados, co2_evitado_kg, monto_ahorrado, cantidad_packs, cantidad_comercios)
       VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id`,
      [req.user.id, period, impact.kg, impact.co2Kg, impact.savedSoles, impact.packs, impact.stores]
    );
    for (const s of impact.series) {
      await db.query('INSERT INTO items_reporte_impacto (reporte_id, etiqueta, kg_rescatados) VALUES ($1, $2, $3)', [rep.rows[0].id, s.label, s.kg]);
    }
  });
  res.json(impact);
}));

module.exports = router;
