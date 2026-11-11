const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const { query } = require('./db');
const { HttpError, wrap } = require('./errors');

const SECRET = () => process.env.JWT_SECRET;
const sha256 = (t) => crypto.createHash('sha256').update(t).digest('hex');

async function createSession(user, device) {
  const token = jwt.sign({ sub: String(user.id), rol: user.rol }, SECRET(), { expiresIn: '30d' });
  await query(
    `INSERT INTO sesiones (usuario_id, token_hash, dispositivo, expira_en)
     VALUES ($1, $2, $3, now() + interval '30 days')`,
    [user.id, sha256(token), device || null]
  );
  return token;
}

const requireAuth = wrap(async (req, _res, next) => {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) throw new HttpError(401, 'Inicia sesión para continuar.');
  let payload;
  try {
    payload = jwt.verify(token, SECRET());
  } catch {
    throw new HttpError(401, 'Tu sesión expiró. Vuelve a iniciar sesión.');
  }
  const { rows } = await query(
    `SELECT s.id FROM sesiones s JOIN usuarios u ON u.id = s.usuario_id
      WHERE s.token_hash = $1 AND s.cerrada_en IS NULL AND s.expira_en > now() AND u.activo`,
    [sha256(token)]
  );
  if (!rows.length) throw new HttpError(401, 'Tu sesión expiró. Vuelve a iniciar sesión.');
  req.user = { id: Number(payload.sub), rol: payload.rol };
  req.tokenHash = sha256(token);
  next();
});

const requireMerchant = wrap(async (req, _res, next) => {
  if (req.user.rol !== 'COMERCIO') throw new HttpError(403, 'Solo disponible para comercios.');
  const { rows } = await query(
    `SELECT pc.id AS perfil_id, pc.estado_verificacion,
            (SELECT l.id FROM locales_comercio l WHERE l.perfil_id = pc.id ORDER BY l.id LIMIT 1) AS local_id
       FROM perfiles_comercio pc WHERE pc.usuario_id = $1`,
    [req.user.id]
  );
  if (!rows.length) throw new HttpError(403, 'Tu cuenta no tiene un comercio registrado.');
  req.perfilId = Number(rows[0].perfil_id);
  req.localId = rows[0].local_id ? Number(rows[0].local_id) : null;
  req.verified = rows[0].estado_verificacion === 'VERIFICADO';
  next();
});

module.exports = { createSession, requireAuth, requireMerchant, sha256 };
