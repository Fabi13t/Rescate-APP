const { query } = require('./db');

let messaging = null;
try {
  if (process.env.FIREBASE_SERVICE_ACCOUNT) {
    const admin = require('firebase-admin');
    admin.initializeApp({ credential: admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)) });
    messaging = admin.messaging();
  }
} catch (e) {
  console.warn('[push] Firebase no configurado:', e.message);
}

async function notify(db, usuarioId, tipo, titulo, mensaje, entidadTipo = null, entidadId = null) {
  const runner = db || { query };
  await runner.query(
    `INSERT INTO notificaciones (usuario_id, tipo, titulo, mensaje, entidad_tipo, entidad_id)
     VALUES ($1, $2, $3, $4, $5, $6)`,
    [usuarioId, tipo, titulo, mensaje, entidadTipo, entidadId]
  );
  if (messaging) {
    const { rows } = await query(
      `SELECT token_fcm FROM sesiones WHERE usuario_id = $1 AND cerrada_en IS NULL AND token_fcm IS NOT NULL`,
      [usuarioId]
    );
    await Promise.all(rows.map((r) =>
      messaging.send({ token: r.token_fcm, notification: { title: titulo, body: mensaje }, data: { tipo } }).catch(() => null)
    ));
  }
}

module.exports = { notify };
