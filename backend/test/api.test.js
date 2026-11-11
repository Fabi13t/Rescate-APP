require('dotenv').config();
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const app = require('../src/index');

let server;
let base;

before(() => new Promise((resolve) => {
  server = app.listen(0, () => {
    base = `http://127.0.0.1:${server.address().port}/api/v1`;
    resolve();
  });
}));

after(() => new Promise((resolve) => server.close(resolve)));

async function call(method, path, body, token) {
  const res = await fetch(base + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined
  });
  return { status: res.status, body: await res.json() };
}

const login = async (email) => (await call('POST', '/auth/login', { email, password: '123456' })).body.token;

test('la base de datos tiene las 23 tablas', async () => {
  const r = await call('GET', '/health');
  assert.equal(r.body.tablas, 23);
});

test('login con contraseña incorrecta devuelve 401', async () => {
  const r = await call('POST', '/auth/login', { email: 'vecino@rescate.pe', password: 'mala' });
  assert.equal(r.status, 401);
});

test('sin token no se puede ver el feed', async () => {
  const r = await call('GET', '/packs');
  assert.equal(r.status, 401);
});

test('el feed filtra por categoría y búsqueda', async () => {
  const token = await login('vecino@rescate.pe');
  const r = await call('GET', '/packs?category=PREPARADOS&q=postre', null, token);
  assert.equal(r.status, 200);
  assert.equal(r.body.length, 1);
  assert.equal(r.body[0].title, 'Pack de postres');
});

test('flujo completo: retener, pagar con Yape, aprobar, recoger y calificar', async () => {
  const vecino = await login('vecino@rescate.pe');
  const comercio = await login('comercio@rescate.pe');

  const hold = await call('POST', '/reservations', { packId: 5, quantity: 1 }, vecino);
  assert.equal(hold.status, 201);
  assert.equal(hold.body.status, 'RETENIDA');

  const sinStock = await call('POST', '/reservations', { packId: 5, quantity: 1 }, vecino);
  assert.equal(sinStock.status, 409, 'nadie puede comprar la misma última unidad');

  const yape = await call('POST', `/reservations/${hold.body.id}/yape`, { reference: '12345678' }, vecino);
  assert.equal(yape.body.status, 'PAGO_EN_REVISION');

  const pagos = await call('GET', '/merchant/payments', null, comercio);
  const pago = pagos.body.find((p) => p.reservationCode === hold.body.code);
  const aprobado = await call('PATCH', `/merchant/payments/${pago.id}/approve`, {}, comercio);
  assert.equal(aprobado.body.status, 'CONFIRMADO');

  const recojo = await call('PATCH', `/merchant/reservations/${hold.body.code}/pickup`, {}, comercio);
  assert.equal(recojo.body.status, 'RECOGIDA');

  const calif = await call('POST', '/ratings', { reservationId: hold.body.id, stars: 4, comment: 'Muy bueno' }, vecino);
  assert.equal(calif.status, 201);
  const otraVez = await call('POST', '/ratings', { reservationId: hold.body.id, stars: 5 }, vecino);
  assert.equal(otraVez.status, 409, 'solo se califica una vez');
});

test('no se puede calificar con más de 5 estrellas', async () => {
  const vecino = await login('vecino@rescate.pe');
  const r = await call('POST', '/ratings', { reservationId: 3, stars: 7 }, vecino);
  assert.equal(r.status, 422);
});

test('registrar un reclamo genera código y plazo de respuesta', async () => {
  const vecino = await login('vecino@rescate.pe');
  const r = await call('POST', '/complaints', {
    reservationId: 9, type: 'RECLAMO', goodType: 'PRODUCTO',
    detail: 'Una porción de torta estaba aplastada.', request: 'Solicito la reposición.'
  }, vecino);
  assert.equal(r.status, 201);
  assert.match(r.body.code, /^REC-\d{4}-\d{6}$/);
  assert.ok(r.body.dueDate);
});

test('una donación perecible no puede vencer en más de 7 días', async () => {
  const vecino = await login('vecino@rescate.pe');
  const r = await call('POST', '/donations', { title: 'Panes', foodType: 'Perecible', kg: 2, expiresInDays: 10 }, vecino);
  assert.equal(r.status, 422);
});

test('el precio rescate debe ser menor al precio real', async () => {
  const comercio = await login('comercio@rescate.pe');
  const r = await call('POST', '/merchant/packs', {
    data: JSON.stringify({ title: 'X', normalPrice: 5, rescuePrice: 8, quantity: 1, pickupUntil: '20:00' })
  }, comercio);
  assert.equal(r.status, 422);
});

test('un vecino no puede entrar a las rutas del comercio', async () => {
  const vecino = await login('vecino@rescate.pe');
  const r = await call('GET', '/merchant/payments', null, vecino);
  assert.equal(r.status, 403);
});
