const express = require('express');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const { query, tx } = require('../db');
const { HttpError, wrap } = require('../errors');
const { createSession, requireAuth } = require('../auth');
const { USER_SELECT, toUser } = require('../mappers');

const router = express.Router();
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const DEFAULT_LAT = -11.9905;
const DEFAULT_LON = -77.068;

async function loadUser(id) {
  const { rows } = await query(`${USER_SELECT} WHERE u.id = $1`, [id]);
  return toUser(rows[0]);
}

router.post('/login', wrap(async (req, res) => {
  const email = String(req.body.email || '').trim().toLowerCase();
  const password = String(req.body.password || '');
  if (!email || !password) throw new HttpError(422, 'Ingresa tu correo y contraseña.');
  const { rows } = await query('SELECT id, rol, hash_contrasena, activo FROM usuarios WHERE correo = $1', [email]);
  const u = rows[0];
  if (!u || !u.activo || !u.hash_contrasena || !(await bcrypt.compare(password, u.hash_contrasena))) {
    throw new HttpError(401, 'Correo o contraseña incorrectos.');
  }
  const token = await createSession(u, req.headers['user-agent']);
  res.json({ token, user: await loadUser(u.id) });
}));

router.post('/register', wrap(async (req, res) => {
  const b = req.body;
  const email = String(b.email || '').trim().toLowerCase();
  const role = b.role === 'COMERCIO' ? 'COMERCIO' : 'VECINO';
  if (!b.names || !b.lastNames) throw new HttpError(422, 'Ingresa tus nombres y apellidos.');
  if (!EMAIL_RE.test(email)) throw new HttpError(422, 'El correo no es válido.');
  if (!b.password || String(b.password).length < 8) throw new HttpError(422, 'La contraseña debe tener al menos 8 caracteres.');

  const exists = await query('SELECT 1 FROM usuarios WHERE correo = $1', [email]);
  if (exists.rows.length) throw new HttpError(409, 'Ya existe una cuenta con ese correo.');

  const userId = await tx(async (db) => {
    const dist = await db.query('SELECT id FROM distritos WHERE nombre = $1', [b.district]);
    const distritoId = dist.rows[0] ? dist.rows[0].id : null;
    const hash = await bcrypt.hash(String(b.password), 10);
    const { rows } = await db.query(
      `INSERT INTO usuarios (rol, nombres, apellidos, correo, telefono, hash_contrasena, distrito_id, zona_busqueda_id, acepta_terminos)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $7, TRUE) RETURNING id`,
      [role, b.names, b.lastNames, email, b.phone || null, hash, distritoId]
    );
    const id = rows[0].id;
    await db.query('INSERT INTO preferencias_notificacion (usuario_id) VALUES ($1)', [id]);

    if (role === 'COMERCIO') {
      const doc = String(b.document || '').replace(/\D/g, '');
      if (doc.length !== 8 && doc.length !== 11) throw new HttpError(422, 'El documento debe ser DNI (8) o RUC (11 dígitos).');
      if (!b.storeName) throw new HttpError(422, 'Ingresa el nombre del comercio.');
      const perfil = await db.query(
        `INSERT INTO perfiles_comercio (usuario_id, tipo_documento, numero_documento, nombre_titular, nombre_comercial)
         VALUES ($1, $2, $3, $4, $5) RETURNING id`,
        [id, doc.length === 11 ? 'RUC' : 'DNI', doc, `${b.names} ${b.lastNames}`, b.storeName]
      );
      const perfilId = perfil.rows[0].id;
      if (b.yapeNumber) {
        await db.query(
          'INSERT INTO medios_pago_comercio (perfil_id, tipo, celular, titular) VALUES ($1, $2, $3, $4)',
          [perfilId, 'YAPE', String(b.yapeNumber).replace(/\D/g, ''), `${b.names} ${b.lastNames}`]
        );
      }
      await db.query(
        `INSERT INTO locales_comercio (perfil_id, direccion, distrito_id, latitud, longitud)
         VALUES ($1, 'Dirección por completar', COALESCE($2, 1), $3, $4)`,
        [perfilId, distritoId, DEFAULT_LAT, DEFAULT_LON]
      );
    }
    return id;
  });

  const token = await createSession({ id: userId, rol: role }, req.headers['user-agent']);
  res.status(201).json({ token, user: await loadUser(userId) });
}));

router.post('/recover', wrap(async (req, res) => {
  const email = String(req.body.email || '').trim().toLowerCase();
  const { rows } = await query('SELECT id FROM usuarios WHERE correo = $1', [email]);
  if (rows[0]) {
    const code = String(crypto.randomInt(0, 1000000)).padStart(6, '0');
    await query(
      `INSERT INTO codigos_verificacion (usuario_id, hash_codigo, proposito, expira_en)
       VALUES ($1, $2, 'RECUPERAR_CONTRASENA', now() + interval '15 minutes')`,
      [rows[0].id, await bcrypt.hash(code, 8)]
    );

    console.log(`[recover] Código para ${email}: ${code}`);
  }

  res.json({ message: 'Si el correo está registrado, te enviamos un código.' });
}));

router.post('/reset', wrap(async (req, res) => {
  const email = String(req.body.email || '').trim().toLowerCase();
  const { code, password } = req.body;
  if (!password || String(password).length < 8) throw new HttpError(422, 'La contraseña debe tener al menos 8 caracteres.');
  const { rows } = await query(
    `SELECT c.id, c.hash_codigo, u.id AS usuario_id FROM codigos_verificacion c JOIN usuarios u ON u.id = c.usuario_id
      WHERE u.correo = $1 AND c.proposito = 'RECUPERAR_CONTRASENA' AND NOT c.usado AND c.expira_en > now()
      ORDER BY c.id DESC LIMIT 1`,
    [email]
  );
  if (!rows[0] || !(await bcrypt.compare(String(code || ''), rows[0].hash_codigo))) {
    throw new HttpError(422, 'El código no es válido o ya expiró.');
  }
  await tx(async (db) => {
    await db.query('UPDATE codigos_verificacion SET usado = TRUE WHERE id = $1', [rows[0].id]);
    await db.query('UPDATE usuarios SET hash_contrasena = $1, actualizado_en = now() WHERE id = $2',
      [await bcrypt.hash(String(password), 10), rows[0].usuario_id]);
    await db.query('UPDATE sesiones SET cerrada_en = now() WHERE usuario_id = $1 AND cerrada_en IS NULL', [rows[0].usuario_id]);
  });
  res.json({ message: 'Contraseña actualizada.' });
}));

router.get('/me', requireAuth, wrap(async (req, res) => res.json(await loadUser(req.user.id))));

router.put('/me', requireAuth, wrap(async (req, res) => {
  const b = req.body;
  const email = String(b.email || '').trim().toLowerCase();
  if (!EMAIL_RE.test(email)) throw new HttpError(422, 'Ingresa un correo válido.');
  await query(
    `UPDATE usuarios SET nombres = $1, apellidos = $2, correo = $3, telefono = $4,
            distrito_id = COALESCE((SELECT id FROM distritos WHERE nombre = $5), distrito_id), actualizado_en = now()
      WHERE id = $6`,
    [b.names, b.lastNames, email, b.phone || null, b.district, req.user.id]
  );
  res.json(await loadUser(req.user.id));
}));

router.post('/fcm', requireAuth, wrap(async (req, res) => {
  await query('UPDATE sesiones SET token_fcm = $1 WHERE token_hash = $2', [req.body.token || null, req.tokenHash]);
  res.json({ message: 'ok' });
}));

router.post('/logout', requireAuth, wrap(async (req, res) => {
  await query('UPDATE sesiones SET cerrada_en = now() WHERE token_hash = $1', [req.tokenHash]);
  res.json({ message: 'Sesión cerrada.' });
}));

module.exports = router;
