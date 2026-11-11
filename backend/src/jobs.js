const { query, tx } = require('./db');
const { notify } = require('./notify');

async function releaseExpiredHolds() {
  return tx(async (db) => {
    const { rows } = await db.query(
      `WITH vencidas AS (
         UPDATE reservas
            SET estado = 'CANCELADA', cancelada_en = now(), cancelada_por = 'SISTEMA',
                motivo_cancelacion = 'Tiempo de retención agotado'
          WHERE estado = 'RETENIDA' AND expira_en < now()
        RETURNING pack_id, cantidad
       )
       UPDATE packs p SET stock_disponible = p.stock_disponible + v.total
         FROM (SELECT pack_id, SUM(cantidad) AS total FROM vencidas GROUP BY pack_id) v
        WHERE p.id = v.pack_id
        RETURNING p.id, p.titulo`
    );
    for (const pack of rows) {
      const alerts = await db.query(
        `UPDATE alertas_stock SET activa = FALSE, notificada_en = now()
          WHERE pack_id = $1 AND activa RETURNING usuario_id`,
        [pack.id]
      );
      for (const a of alerts.rows) {
        await notify(db, a.usuario_id, 'ALERTA', 'Un pack se liberó', `${pack.titulo} vuelve a estar disponible.`, 'PACK', pack.id);
      }
    }
    return rows.length;
  });
}

async function closeExpiredPacks() {
  await query(
    `UPDATE packs SET estado = 'FINALIZADO'
      WHERE estado = 'EN_PROCESO' AND (hora_limite_recojo < now() OR stock_disponible = 0 AND NOT EXISTS
            (SELECT 1 FROM reservas r WHERE r.pack_id = packs.id AND r.estado IN ('RETENIDA','PAGO_EN_REVISION','CONFIRMADA')))`
  );
  await query(
    `UPDATE reservas r SET estado = 'NO_RECOGIDA'
       FROM packs p
      WHERE p.id = r.pack_id AND r.estado = 'CONFIRMADA' AND p.hora_limite_recojo < now() - interval '1 hour'`
  );
}

function startJobs() {
  const run = async () => {
    try {
      await releaseExpiredHolds();
      await closeExpiredPacks();
    } catch (e) {
      console.error('[jobs]', e.message);
    }
  };
  run();
  return setInterval(run, 30 * 1000);
}

module.exports = { startJobs, releaseExpiredHolds };
