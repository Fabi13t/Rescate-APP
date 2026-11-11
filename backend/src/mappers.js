const TZ = 'America/Lima';
const num = (v) => (v === null || v === undefined ? 0 : Number(v));
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

function limaParts(date) {
  const d = new Date(date);
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en-GB', {
      timeZone: TZ, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false
    }).formatToParts(d).map((p) => [p.type, p.value])
  );
  return { y: +parts.year, m: +parts.month, d: +parts.day, hh: parts.hour === '24' ? '00' : parts.hour, mm: parts.minute };
}

function relativeLabel(date) {
  if (!date) return '';
  const p = limaParts(date);
  const today = limaParts(new Date());
  const yesterday = limaParts(new Date(Date.now() - 86400000));
  const hm = `${p.hh}:${p.mm}`;
  if (p.y === today.y && p.m === today.m && p.d === today.d) return `Hoy · ${hm}`;
  if (p.y === yesterday.y && p.m === yesterday.m && p.d === yesterday.d) return `Ayer · ${hm}`;
  return `${p.d} ${MESES[p.m - 1]} · ${hm}`;
}

function shortDate(date) {
  if (!date) return '';
  const p = limaParts(date);
  return `${p.d} ${MESES[p.m - 1]}`;
}

function hourLabel(date) {
  if (!date) return '';
  const p = limaParts(date);
  return `${p.hh}:${p.mm}`;
}

const formatPhone = (n) => (n ? String(n).replace(/\D/g, '').replace(/(\d{3})(?=\d)/g, '$1 ').trim() : '');

function photoUrl(path) {
  if (!path) return null;
  if (/^https?:\/\//.test(path)) return path;
  return `${process.env.PUBLIC_URL || ''}${path}`;
}

function distanceMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const rad = (x) => (x * Math.PI) / 180;
  const dLat = rad(lat2 - lat1);
  const dLon = rad(lon2 - lon1);
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLon / 2) ** 2;
  return Math.round(2 * R * Math.asin(Math.sqrt(a)));
}

const PACK_COLS = `
  p.id AS p_id, p.titulo AS p_titulo, p.descripcion AS p_descripcion, p.url_foto AS p_url_foto,
  p.precio_original AS p_precio_original, p.precio_rescate AS p_precio_rescate,
  p.stock_inicial AS p_stock_inicial, p.stock_disponible AS p_stock_disponible,
  p.peso_aprox_kg AS p_peso, p.es_perecible AS p_perecible,
  (p.fecha_vencimiento - (now() AT TIME ZONE 'America/Lima')::date) AS p_dias_vence,
  to_char(p.hora_limite_recojo AT TIME ZONE 'America/Lima', 'HH24:MI') AS p_hora_limite,
  to_char(p.creado_en AT TIME ZONE 'America/Lima', 'DD/MM/YYYY') AS p_publicado,
  p.estado AS p_estado, c.alias AS c_alias,
  l.id AS l_id, l.direccion AS l_direccion, l.latitud AS l_lat, l.longitud AS l_lon,
  l.calificacion AS l_calificacion, to_char(l.hora_cierre, 'HH24:MI') AS l_cierre,
  (SELECT count(*) FROM calificaciones ca WHERE ca.local_id = l.id) AS l_total_calif,
  d.nombre AS d_nombre, pc.id AS pc_id, pc.nombre_comercial AS pc_nombre,
  (SELECT m.celular FROM medios_pago_comercio m WHERE m.perfil_id = pc.id ORDER BY m.id LIMIT 1) AS pc_yape`;

const PACK_JOINS = `
  JOIN locales_comercio l  ON l.id = p.local_id
  JOIN perfiles_comercio pc ON pc.id = l.perfil_id
  JOIN distritos d          ON d.id = l.distrito_id
  JOIN categorias c         ON c.id = p.categoria_id`;

function toStore(r) {
  return {
    id: Number(r.l_id),
    name: r.pc_nombre,
    category: r.c_alias,
    address: r.l_direccion,
    district: r.d_nombre,
    lat: num(r.l_lat),
    lon: num(r.l_lon),
    rating: num(r.l_calificacion),
    ratingCount: num(r.l_total_calif),
    closesAt: r.l_cierre,
    yapeNumber: formatPhone(r.pc_yape)
  };
}

function toPack(r, myLat, myLon) {
  const hasLoc = Number.isFinite(myLat) && Number.isFinite(myLon);
  return {
    id: Number(r.p_id),
    store: toStore(r),
    title: r.p_titulo,
    description: r.p_descripcion || '',
    category: r.c_alias,
    normalPrice: num(r.p_precio_original),
    rescuePrice: num(r.p_precio_rescate),
    stock: num(r.p_stock_disponible),
    initialStock: num(r.p_stock_inicial),
    pickupUntil: r.p_hora_limite,
    expiresInDays: Math.max(0, num(r.p_dias_vence)),
    perishable: r.p_perecible,
    kgPerPack: num(r.p_peso),
    distanceMeters: hasLoc ? distanceMeters(myLat, myLon, num(r.l_lat), num(r.l_lon)) : 0,
    publishedAt: r.p_publicado,
    status: r.p_estado,
    photoUri: photoUrl(r.p_url_foto)
  };
}

const RESERVATION_SELECT = `
  SELECT r.id AS r_id, r.codigo AS r_codigo, r.cantidad AS r_cantidad, r.estado AS r_estado,
         r.reservada_en AS r_reservada_en, r.expira_en AS r_expira_en, r.usuario_id AS r_usuario_id,
         u.nombres || ' ' || u.apellidos AS r_cliente,
         (SELECT pg.referencia FROM pagos pg WHERE pg.reserva_id = r.id ORDER BY pg.id DESC LIMIT 1) AS r_referencia,
         (SELECT ca.puntuacion FROM calificaciones ca WHERE ca.reserva_id = r.id) AS r_mi_puntuacion,
         ${PACK_COLS}
    FROM reservas r
    JOIN usuarios u ON u.id = r.usuario_id
    JOIN packs p ON p.id = r.pack_id
    ${PACK_JOINS}`;

function toReservation(r) {
  return {
    id: Number(r.r_id),
    code: r.r_codigo,
    pack: toPack(r),
    quantity: num(r.r_cantidad),
    status: r.r_estado,
    createdAt: relativeLabel(r.r_reservada_en),
    yapeReference: r.r_referencia,
    customerName: r.r_cliente,
    myRating: r.r_mi_puntuacion === null || r.r_mi_puntuacion === undefined ? null : Number(r.r_mi_puntuacion)
  };
}

const USER_SELECT = `
  SELECT u.*, d.nombre AS distrito, pc.nombre_comercial, pc.estado_verificacion,
         (SELECT m.celular FROM medios_pago_comercio m WHERE m.perfil_id = pc.id ORDER BY m.id LIMIT 1) AS yape
    FROM usuarios u
    LEFT JOIN distritos d ON d.id = u.distrito_id
    LEFT JOIN perfiles_comercio pc ON pc.usuario_id = u.id`;

function toUser(r) {
  return {
    id: Number(r.id),
    names: r.nombres,
    lastNames: r.apellidos,
    email: r.correo,
    phone: r.telefono || '',
    district: r.distrito || '',
    role: r.rol === 'COMERCIO' ? 'COMERCIO' : 'VECINO',
    storeName: r.nombre_comercial || null,
    yapeNumber: r.yape ? formatPhone(r.yape) : null,
    verified: r.rol !== 'COMERCIO' || r.estado_verificacion === 'VERIFICADO'
  };
}

function toPayment(r) {
  return {
    id: Number(r.id),
    reservationCode: r.codigo,
    customerName: r.cliente,
    amount: num(r.monto),
    product: r.titulo,
    reference: r.referencia,
    status: r.estado,
    time: relativeLabel(r.reportado_en)
  };
}

const DONATION_STATUS = { PUBLICADA: 'Publicada', ENTREGADA: 'Entregada', RETIRADA: 'Retirada' };

function toDonation(r) {
  const [title, ...rest] = String(r.observaciones || 'Donación').split(' · ');
  return {
    id: Number(r.id),
    title,
    foodType: r.tipo,
    kg: num(r.cantidad_kg),
    expiresInDays: Math.max(0, num(r.dias)),
    district: r.distrito || '',
    notes: rest.join(' · '),
    status: DONATION_STATUS[r.estado] || r.estado,
    createdAt: shortDate(r.creada_en)
  };
}

function toComplaint(r) {
  return {
    id: Number(r.id),
    code: r.codigo,
    type: r.tipo,
    goodType: r.bien_contratado,
    amount: r.monto_reclamado === null ? null : num(r.monto_reclamado),
    storeName: r.nombre_comercial,
    customerName: r.cliente,
    reservationCode: r.reserva_codigo || null,
    detail: r.detalle,
    request: r.pedido,
    status: r.estado,
    response: r.respuesta,
    createdAt: shortDate(r.creado_en),
    respondedAt: r.respondido_en ? shortDate(r.respondido_en) : null,
    dueDate: shortDate(r.fecha_limite_respuesta)
  };
}

function toRating(r) {
  return {
    id: Number(r.id),
    reservationId: Number(r.reserva_id),
    storeName: r.nombre_comercial,
    customerName: r.cliente,
    stars: num(r.puntuacion),
    comment: r.comentario || '',
    merchantReply: r.respuesta_comercio,
    createdAt: shortDate(r.creada_en)
  };
}

module.exports = {
  PACK_COLS, PACK_JOINS, RESERVATION_SELECT, USER_SELECT,
  toPack, toStore, toReservation, toUser, toPayment, toDonation, toComplaint, toRating,
  relativeLabel, shortDate, hourLabel, limaParts, distanceMeters, num, MESES
};
