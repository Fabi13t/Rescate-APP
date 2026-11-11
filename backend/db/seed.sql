CREATE OR REPLACE FUNCTION hoy_lima(h TIME) RETURNS TIMESTAMPTZ AS $$
  SELECT (((now() AT TIME ZONE 'America/Lima')::date + h) AT TIME ZONE 'America/Lima');
$$ LANGUAGE sql STABLE;

INSERT INTO distritos (nombre) VALUES
 ('Los Olivos'), ('San Martín de Porres'), ('Independencia'), ('Comas'), ('Miraflores'),
 ('San Isidro'), ('Santiago de Surco'), ('Jesús María'), ('Lince'), ('Pueblo Libre'), ('San Miguel');

INSERT INTO categorias (nombre, alias, icono) VALUES
 ('Panadería', 'PANADERIA', 'ic_shopping_bag'),
 ('Abarrotes', 'ABARROTES', 'ic_store'),
 ('Preparados', 'PREPARADOS', 'ic_gift');

INSERT INTO tipos_alimento (nombre) VALUES ('Perecible'), ('No perecible'), ('Comida preparada');

INSERT INTO usuarios (rol, nombres, apellidos, correo, telefono, hash_contrasena, distrito_id, zona_busqueda_id, acepta_terminos) VALUES
 ('VECINO',   'Eduardo', 'Palomino', 'vecino@rescate.pe',   '912345678', crypt('123456', gen_salt('bf', 10)), 1, 1, TRUE),
 ('COMERCIO', 'Pedro',   'Quispe',   'comercio@rescate.pe', '987654321', crypt('123456', gen_salt('bf', 10)), 1, 1, TRUE),
 ('COMERCIO', 'Rosa',    'Huamán',   'frugal@rescate.pe',   '986321457', crypt('123456', gen_salt('bf', 10)), 1, 1, TRUE),
 ('COMERCIO', 'Lucía',   'Torres',   'aroma@rescate.pe',    '985111222', crypt('123456', gen_salt('bf', 10)), 1, 1, TRUE),
 ('COMERCIO', 'Jorge',   'Ramos',    'esquina@rescate.pe',  '984222333', crypt('123456', gen_salt('bf', 10)), 2, 2, TRUE),
 ('COMERCIO', 'Carmen',  'Flores',   'elsol@rescate.pe',    '983444555', crypt('123456', gen_salt('bf', 10)), 3, 3, TRUE),
 ('VECINO',   'Julio',   'Mendoza',  'julio@rescate.pe',    '911111111', crypt('123456', gen_salt('bf', 10)), 1, 1, TRUE),
 ('VECINO',   'Diana',   'Gómez',    'diana@rescate.pe',    '922222222', crypt('123456', gen_salt('bf', 10)), 1, 1, TRUE);

INSERT INTO preferencias_notificacion (usuario_id) SELECT id FROM usuarios;

INSERT INTO perfiles_comercio (usuario_id, tipo_documento, numero_documento, razon_social, nombre_titular, nombre_comercial, rubro, estado_verificacion) VALUES
 (2, 'RUC', '20601234567', 'Panificadora Don Pedro S.A.C.', 'Pedro Quispe',  'Panadería Don Pedro', 'Panadería',  'VERIFICADO'),
 (3, 'RUC', '20601234568', 'Comercial Frugal E.I.R.L.',     'Rosa Huamán',   'Mercado Frugal',      'Abarrotes',  'VERIFICADO'),
 (4, 'RUC', '20601234569', 'Aroma Café S.A.C.',             'Lucía Torres',  'Cafetería Aroma',     'Cafetería',  'VERIFICADO'),
 (5, 'DNI', '45678912',    NULL,                            'Jorge Ramos',   'Frutería La Esquina', 'Frutería',   'VERIFICADO'),
 (6, 'RUC', '10456789123', NULL,                            'Carmen Flores', 'Minimarket El Sol',   'Minimarket', 'VERIFICADO');

INSERT INTO medios_pago_comercio (perfil_id, tipo, celular, titular, estado_verificacion) VALUES
 (1, 'YAPE', '987654321', 'Pedro Quispe',  'VERIFICADO'),
 (2, 'YAPE', '986321457', 'Rosa Huamán',   'VERIFICADO'),
 (3, 'YAPE', '985111222', 'Lucía Torres',  'VERIFICADO'),
 (4, 'YAPE', '984222333', 'Jorge Ramos',   'VERIFICADO'),
 (5, 'YAPE', '983444555', 'Carmen Flores', 'VERIFICADO');

INSERT INTO locales_comercio (perfil_id, direccion, distrito_id, latitud, longitud, hora_apertura, hora_cierre) VALUES
 (1, 'Av. Los Olivos 234',       1, -11.99150000, -77.07050000, '06:00', '21:00'),
 (2, 'Jr. Las Palmeras 512',     1, -11.98620000, -77.06510000, '07:00', '20:00'),
 (3, 'Av. Carlos Izaguirre 980', 1, -11.99580000, -77.06120000, '08:00', '22:00'),
 (4, 'Calle Los Pinos 145',      2, -12.00030000, -77.07420000, '07:00', '21:00'),
 (5, 'Av. Naranjal 1201',        3, -11.98110000, -77.05900000, '07:00', '22:00');

INSERT INTO packs (local_id, categoria_id, titulo, descripcion, url_foto, precio_original, precio_rescate, stock_inicial, stock_disponible,
                   peso_aprox_kg, es_perecible, fecha_vencimiento, hora_limite_recojo, estado) VALUES
 (1, 1, 'Pack sorpresa',          'Selección de panes y bollería del día, próximos a vencer y en perfectas condiciones.', NULL, 15, 6.0, 6, 4, 1.2, TRUE,  CURRENT_DATE,     hoy_lima('20:30'), 'EN_PROCESO'),
 (2, 2, 'Verduras variadas',      'Verduras frescas de estación: tomate, zanahoria, zapallo y hojas verdes.',             NULL, 20, 8.0, 4, 2, 2.5, TRUE,  CURRENT_DATE + 2, hoy_lima('20:00'), 'EN_PROCESO'),
 (3, 3, 'Pack de postres',        'Postres y tortas del día: porciones de torta, alfajores y keke.',                      NULL, 18, 7.0, 5, 3, 0.8, TRUE,  CURRENT_DATE,     hoy_lima('21:30'), 'EN_PROCESO'),
 (4, 2, 'Frutas de temporada',    'Mix de frutas maduras ideales para jugos o postres.',                                  NULL, 16, 6.5, 6, 5, 3.0, TRUE,  CURRENT_DATE + 1, hoy_lima('20:30'), 'EN_PROCESO'),
 (1, 1, 'Pack panes dulces',      'Panes dulces, empanadas y croissants del día.',                                        NULL, 12, 5.0, 3, 1, 0.9, TRUE,  CURRENT_DATE,     hoy_lima('20:30'), 'EN_PROCESO'),
 (3, 3, 'Almuerzo del día',       'Menú del día listo para calentar: entrada y segundo.',                                 NULL, 22, 9.0, 4, 0, 1.0, TRUE,  CURRENT_DATE,     hoy_lima('15:00'), 'EN_PROCESO'),
 (5, 2, 'Abarrotes no perecibles','Arroz, menestras y fideos con empaque dañado pero producto intacto.',                  NULL, 25, 10.0, 8, 6, 2.0, FALSE, CURRENT_DATE + 5, hoy_lima('21:00'), 'EN_PROCESO'),
 (1, 1, 'Pack panes',             'Pan francés, ciabatta e integral del día.',                                            NULL, 10, 4.0, 2, 0, 1.0, TRUE,  CURRENT_DATE - 7, hoy_lima('20:30') - interval '7 days', 'FINALIZADO');

INSERT INTO reservas (codigo, usuario_id, pack_id, cantidad, precio_unitario, total, peso_kg, estado, reservada_en, expira_en, recogida_en) VALUES
 ('RSC-000101', 1, 1, 1,  6.0,  6.0, 1.2, 'CONFIRMADA',      now() - interval '30 min', now() - interval '27 min', NULL),
 ('RSC-000102', 1, 4, 1,  6.5,  6.5, 3.0, 'CONFIRMADA',      now() - interval '2 hour', now() - interval '117 min', NULL),
 ('RSC-000103', 1, 7, 2, 10.0, 20.0, 4.0, 'RECOGIDA',        now() - interval '10 day', now() - interval '10 day' + interval '3 min', now() - interval '10 day' + interval '1 hour'),
 ('RSC-000104', 1, 8, 1,  4.0,  4.0, 1.0, 'RECOGIDA',        now() - interval '17 day', now() - interval '17 day' + interval '3 min', now() - interval '17 day' + interval '1 hour'),
 ('RSC-000105', 1, 2, 1,  8.0,  8.0, 2.5, 'NO_RECOGIDA',     now() - interval '23 day', now() - interval '23 day' + interval '3 min', NULL),
 ('RSC-000106', 7, 1, 1,  6.0,  6.0, 1.2, 'PAGO_EN_REVISION', now() - interval '10 min', now() - interval '7 min', NULL),
 ('RSC-000107', 8, 5, 1,  5.0,  5.0, 0.9, 'CONFIRMADA',      now() - interval '40 min', now() - interval '37 min', NULL),
 ('RSC-000108', 8, 1, 1,  6.0,  6.0, 1.2, 'RECOGIDA',        now() - interval '1 day', now() - interval '1 day' + interval '3 min', now() - interval '1 day' + interval '2 hour'),
 ('RSC-000109', 1, 3, 1,  7.0,  7.0, 0.8, 'RECOGIDA',        now() - interval '3 day', now() - interval '3 day' + interval '3 min', now() - interval '3 day' + interval '1 hour');

INSERT INTO pagos (reserva_id, monto, referencia, estado, reportado_en, revisado_en) VALUES
 (1,  6.0, '10005321', 'CONFIRMADO', now() - interval '29 min', now() - interval '28 min'),
 (2,  6.5, '10004410', 'CONFIRMADO', now() - interval '2 hour', now() - interval '115 min'),
 (3, 20.0, '10001984', 'CONFIRMADO', now() - interval '10 day', now() - interval '10 day'),
 (4,  4.0, '10007731', 'CONFIRMADO', now() - interval '17 day', now() - interval '17 day'),
 (5,  8.0, '10002290', 'CONFIRMADO', now() - interval '23 day', now() - interval '23 day'),
 (6,  6.0, '20000001', 'PENDIENTE',  now() - interval '8 min',  NULL),
 (7,  5.0, '20000002', 'CONFIRMADO', now() - interval '39 min', now() - interval '38 min'),
 (8,  6.0, '20000003', 'CONFIRMADO', now() - interval '1 day',  now() - interval '1 day'),
 (9,  7.0, '10009001', 'CONFIRMADO', now() - interval '3 day',  now() - interval '3 day');

INSERT INTO calificaciones (reserva_id, usuario_id, local_id, puntuacion, comentario, respuesta_comercio, creada_en) VALUES
 (3, 1, 5, 4, 'Buen pack, los productos estaban completos.', NULL, now() - interval '10 day'),
 (4, 1, 1, 5, 'El pan llegó fresquito, ¡excelente!', '¡Gracias Eduardo, te esperamos pronto!', now() - interval '17 day'),
 (8, 8, 1, 4, 'Muy buena atención, rápido el recojo.', NULL, now() - interval '1 day');

INSERT INTO reclamos (codigo, usuario_id, perfil_id, reserva_id, tipo, bien_contratado, monto_reclamado, detalle, pedido, estado, fecha_limite_respuesta, creado_en) VALUES
 ('REC-2026-000001', 1, 3, 9, 'RECLAMO', 'PRODUCTO', 7.0,
  'Una de las porciones de torta del pack estaba aplastada.', 'Solicito la reposición de la porción o la devolución parcial.',
  'PENDIENTE', CURRENT_DATE + 15, now() - interval '2 day');

INSERT INTO donaciones (usuario_id, tipo_alimento_id, cantidad_kg, es_perecible, fecha_caducidad, observaciones, estado, creada_en) VALUES
 (1, 1, 3,  TRUE,  CURRENT_DATE + 2, 'Panes del día · Listo para recoger después de las 7 p. m.', 'PUBLICADA', now() - interval '5 day'),
 (1, 2, 10, FALSE, CURRENT_DATE + 7, 'Arroz y menestras · Bolsas cerradas.',                      'ENTREGADA', now() - interval '18 day');

INSERT INTO notificaciones (usuario_id, tipo, titulo, mensaje, leida, entidad_tipo, entidad_id) VALUES
 (1, 'PACK',         '¡Pack disponible cerca!', 'Panadería Don Pedro publicó un Pack sorpresa a S/ 6.00', FALSE, 'PACK', 1),
 (1, 'ALERTA',       'Un pack se liberó',       'Se liberó 1 unidad de Verduras variadas en Mercado Frugal', FALSE, 'PACK', 2),
 (1, 'RECORDATORIO', 'Recuerda tu recojo',      'Tu reserva RSC-000101 se recoge hasta las 20:30', TRUE, 'RESERVA', 1),
 (2, 'PAGO',         'Nuevo pago por validar',  'Julio Mendoza reportó un Yape de S/ 6.00', FALSE, 'RESERVA', 6),
 (4, 'RECLAMO',      'Nuevo reclamo recibido',  'Tienes un reclamo REC-2026-000001 por responder', FALSE, 'RECLAMO', 1);

SELECT setval('seq_codigo_reclamo', 1);
