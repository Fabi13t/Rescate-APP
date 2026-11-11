CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TABLE IF EXISTS calificaciones, reclamos, items_reporte_impacto, reportes_impacto,
  solicitudes_donacion, donaciones, tipos_alimento, pagos, reservas, alertas_stock, favoritos,
  packs, locales_comercio, categorias, medios_pago_comercio, perfiles_comercio, tickets_soporte,
  notificaciones, preferencias_notificacion, codigos_verificacion, sesiones, usuarios, distritos CASCADE;
DROP SEQUENCE IF EXISTS seq_codigo_reserva, seq_codigo_reclamo;

CREATE SEQUENCE seq_codigo_reserva START 1000;
CREATE SEQUENCE seq_codigo_reclamo START 1;

CREATE TABLE distritos (
  id         BIGSERIAL PRIMARY KEY,
  nombre     VARCHAR(100) NOT NULL UNIQUE,
  provincia  VARCHAR(100) NOT NULL DEFAULT 'Lima',
  activo     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE usuarios (
  id                BIGSERIAL PRIMARY KEY,
  rol               VARCHAR(50)  NOT NULL CHECK (rol IN ('VECINO', 'COMERCIO', 'ADMIN')),
  nombres           VARCHAR(80)  NOT NULL,
  apellidos         VARCHAR(80)  NOT NULL,
  correo            VARCHAR(150) NOT NULL UNIQUE,
  telefono          VARCHAR(15),
  hash_contrasena   VARCHAR(255),
  proveedor_acceso  VARCHAR(20)  NOT NULL DEFAULT 'CORREO' CHECK (proveedor_acceso IN ('CORREO', 'GOOGLE')),
  distrito_id       BIGINT REFERENCES distritos(id),
  zona_busqueda_id  BIGINT REFERENCES distritos(id),
  url_avatar        VARCHAR(255),
  acepta_terminos   BOOLEAN NOT NULL DEFAULT FALSE,
  activo            BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en         TIMESTAMPTZ NOT NULL DEFAULT now(),
  actualizado_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sesiones (
  id          BIGSERIAL PRIMARY KEY,
  usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  token_hash  VARCHAR(255) NOT NULL UNIQUE,
  token_fcm   VARCHAR(255),
  dispositivo VARCHAR(100),
  creada_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
  expira_en   TIMESTAMPTZ NOT NULL,
  cerrada_en  TIMESTAMPTZ
);

CREATE TABLE codigos_verificacion (
  id          BIGSERIAL PRIMARY KEY,
  usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  hash_codigo VARCHAR(255) NOT NULL,
  proposito   VARCHAR(50)  NOT NULL CHECK (proposito IN ('RECUPERAR_CONTRASENA', 'VERIFICAR_CORREO')),
  expira_en   TIMESTAMPTZ NOT NULL,
  usado       BOOLEAN NOT NULL DEFAULT FALSE,
  creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE preferencias_notificacion (
  id                  BIGSERIAL PRIMARY KEY,
  usuario_id          BIGINT NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
  packs_nuevos        BOOLEAN NOT NULL DEFAULT TRUE,
  alertas_stock       BOOLEAN NOT NULL DEFAULT TRUE,
  recordatorio_recojo BOOLEAN NOT NULL DEFAULT TRUE,
  ofertas_dia         BOOLEAN NOT NULL DEFAULT TRUE,
  resumen_impacto     BOOLEAN NOT NULL DEFAULT TRUE,
  actualizado_en      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notificaciones (
  id           BIGSERIAL PRIMARY KEY,
  usuario_id   BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  tipo         VARCHAR(50)  NOT NULL CHECK (tipo IN ('PACK', 'ALERTA', 'RECORDATORIO', 'OFERTA', 'IMPACTO', 'RECLAMO', 'CALIFICACION', 'PAGO')),
  titulo       VARCHAR(150) NOT NULL,
  mensaje      TEXT NOT NULL,
  leida        BOOLEAN NOT NULL DEFAULT FALSE,
  entidad_tipo VARCHAR(50),
  entidad_id   BIGINT,
  creada_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tickets_soporte (
  id          BIGSERIAL PRIMARY KEY,
  usuario_id  BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
  correo      VARCHAR(150) NOT NULL,
  asunto      VARCHAR(200) NOT NULL,
  mensaje     TEXT NOT NULL,
  estado      VARCHAR(50) NOT NULL DEFAULT 'ABIERTO' CHECK (estado IN ('ABIERTO', 'EN_PROCESO', 'CERRADO')),
  creado_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE perfiles_comercio (
  id                  BIGSERIAL PRIMARY KEY,
  usuario_id          BIGINT NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
  tipo_documento      VARCHAR(10)  NOT NULL CHECK (tipo_documento IN ('RUC', 'DNI')),
  numero_documento    VARCHAR(11)  NOT NULL UNIQUE,
  razon_social        VARCHAR(150),
  nombre_titular      VARCHAR(150) NOT NULL,
  nombre_comercial    VARCHAR(150) NOT NULL,
  rubro               VARCHAR(80),
  estado_verificacion VARCHAR(50)  NOT NULL DEFAULT 'POR_VERIFICAR' CHECK (estado_verificacion IN ('POR_VERIFICAR', 'VERIFICADO', 'RECHAZADO')),
  creado_en           TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK ((tipo_documento = 'RUC' AND length(numero_documento) = 11) OR (tipo_documento = 'DNI' AND length(numero_documento) = 8))
);

CREATE TABLE medios_pago_comercio (
  id                  BIGSERIAL PRIMARY KEY,
  perfil_id           BIGINT NOT NULL REFERENCES perfiles_comercio(id) ON DELETE CASCADE,
  tipo                VARCHAR(20)  NOT NULL DEFAULT 'YAPE' CHECK (tipo IN ('YAPE', 'PLIN')),
  celular             VARCHAR(15)  NOT NULL,
  titular             VARCHAR(150) NOT NULL,
  qr_url              VARCHAR(255),
  estado_verificacion VARCHAR(50)  NOT NULL DEFAULT 'POR_VERIFICAR' CHECK (estado_verificacion IN ('POR_VERIFICAR', 'VERIFICADO', 'RECHAZADO')),
  actualizado_en      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE categorias (
  id     BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL UNIQUE,
  alias  VARCHAR(100) NOT NULL UNIQUE,
  icono  VARCHAR(100)
);

CREATE TABLE locales_comercio (
  id            BIGSERIAL PRIMARY KEY,
  perfil_id     BIGINT NOT NULL REFERENCES perfiles_comercio(id) ON DELETE CASCADE,
  direccion     VARCHAR(255) NOT NULL,
  distrito_id   BIGINT NOT NULL REFERENCES distritos(id),
  latitud       DECIMAL(10,8) NOT NULL,
  longitud      DECIMAL(11,8) NOT NULL,
  hora_apertura TIME NOT NULL DEFAULT '08:00',
  hora_cierre   TIME NOT NULL DEFAULT '21:00',
  calificacion  DECIMAL(2,1) NOT NULL DEFAULT 0 CHECK (calificacion BETWEEN 0 AND 5),
  activo        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE packs (
  id                 BIGSERIAL PRIMARY KEY,
  local_id           BIGINT NOT NULL REFERENCES locales_comercio(id) ON DELETE CASCADE,
  categoria_id       BIGINT NOT NULL REFERENCES categorias(id),
  titulo             VARCHAR(150) NOT NULL,
  descripcion        TEXT,
  url_foto           VARCHAR(255),
  precio_original    DECIMAL(10,2) NOT NULL CHECK (precio_original > 0),
  precio_rescate     DECIMAL(10,2) NOT NULL CHECK (precio_rescate > 0),
  stock_inicial      INTEGER NOT NULL CHECK (stock_inicial > 0),
  stock_disponible   INTEGER NOT NULL CHECK (stock_disponible >= 0),
  peso_aprox_kg      DECIMAL(6,2) NOT NULL DEFAULT 1,
  es_perecible       BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_vencimiento  DATE NOT NULL,
  hora_limite_recojo TIMESTAMPTZ NOT NULL,
  estado             VARCHAR(50) NOT NULL DEFAULT 'EN_PROCESO' CHECK (estado IN ('EN_PROCESO', 'FINALIZADO')),
  creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
  CHECK (precio_rescate < precio_original),
  CHECK (stock_disponible <= stock_inicial)
);

CREATE TABLE favoritos (
  id         BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  pack_id    BIGINT NOT NULL REFERENCES packs(id) ON DELETE CASCADE,
  creado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (usuario_id, pack_id)
);

CREATE TABLE alertas_stock (
  id            BIGSERIAL PRIMARY KEY,
  usuario_id    BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  pack_id       BIGINT NOT NULL REFERENCES packs(id) ON DELETE CASCADE,
  activa        BOOLEAN NOT NULL DEFAULT TRUE,
  notificada_en TIMESTAMPTZ,
  creada_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (usuario_id, pack_id)
);

CREATE TABLE reservas (
  id                 BIGSERIAL PRIMARY KEY,
  codigo             VARCHAR(20) NOT NULL UNIQUE,
  usuario_id         BIGINT NOT NULL REFERENCES usuarios(id),
  pack_id            BIGINT NOT NULL REFERENCES packs(id),
  cantidad           INTEGER NOT NULL CHECK (cantidad > 0),
  precio_unitario    DECIMAL(10,2) NOT NULL,
  total              DECIMAL(10,2) NOT NULL,
  peso_kg            DECIMAL(8,2) NOT NULL DEFAULT 0,
  estado             VARCHAR(50) NOT NULL DEFAULT 'RETENIDA' CHECK (estado IN
                       ('RETENIDA', 'PAGO_EN_REVISION', 'CONFIRMADA', 'RECOGIDA', 'CANCELADA', 'NO_RECOGIDA', 'RECHAZADA')),
  reservada_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
  expira_en          TIMESTAMPTZ NOT NULL,
  preparada_en       TIMESTAMPTZ,
  recogida_en        TIMESTAMPTZ,
  cancelada_en       TIMESTAMPTZ,
  cancelada_por      VARCHAR(20) CHECK (cancelada_por IN ('VECINO', 'COMERCIO', 'SISTEMA')),
  motivo_cancelacion TEXT
);

CREATE TABLE pagos (
  id             BIGSERIAL PRIMARY KEY,
  reserva_id     BIGINT NOT NULL REFERENCES reservas(id) ON DELETE CASCADE,
  metodo         VARCHAR(50) NOT NULL DEFAULT 'YAPE',
  monto          DECIMAL(10,2) NOT NULL,
  referencia     VARCHAR(30) NOT NULL UNIQUE,
  captura_url    VARCHAR(255),
  estado         VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'CONFIRMADO', 'RECHAZADO')),
  motivo_rechazo TEXT,
  reportado_en   TIMESTAMPTZ NOT NULL DEFAULT now(),
  revisado_en    TIMESTAMPTZ
);

CREATE TABLE tipos_alimento (
  id     BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE donaciones (
  id               BIGSERIAL PRIMARY KEY,
  usuario_id       BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  tipo_alimento_id BIGINT NOT NULL REFERENCES tipos_alimento(id),
  cantidad_kg      DECIMAL(8,2) NOT NULL CHECK (cantidad_kg > 0),
  es_perecible     BOOLEAN NOT NULL DEFAULT TRUE,
  fecha_caducidad  DATE NOT NULL,
  observaciones    TEXT,
  estado_recojo    VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado_recojo IN ('PENDIENTE', 'RECOGIDO')),
  estado           VARCHAR(50) NOT NULL DEFAULT 'PUBLICADA' CHECK (estado IN ('PUBLICADA', 'ENTREGADA', 'RETIRADA')),
  creada_en        TIMESTAMPTZ NOT NULL DEFAULT now(),
  publicada_en     TIMESTAMPTZ DEFAULT now(),

  CHECK (NOT es_perecible OR fecha_caducidad <= (creada_en::date + 7))
);

CREATE TABLE solicitudes_donacion (
  id          BIGSERIAL PRIMARY KEY,
  donacion_id BIGINT NOT NULL REFERENCES donaciones(id) ON DELETE CASCADE,
  usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  mensaje     VARCHAR(255),
  estado      VARCHAR(50) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA')),
  creada_en   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE reportes_impacto (
  id                 BIGSERIAL PRIMARY KEY,
  usuario_id         BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  tipo_periodo       VARCHAR(50) NOT NULL CHECK (tipo_periodo IN ('resumen', 'semanal', 'mensual', 'total')),
  kg_rescatados      DECIMAL(10,2) NOT NULL DEFAULT 0,
  co2_evitado_kg     DECIMAL(10,2) NOT NULL DEFAULT 0,
  monto_ahorrado     DECIMAL(10,2) NOT NULL DEFAULT 0,
  cantidad_packs     INTEGER NOT NULL DEFAULT 0,
  cantidad_comercios INTEGER NOT NULL DEFAULT 0,
  generado_en        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE items_reporte_impacto (
  id            BIGSERIAL PRIMARY KEY,
  reporte_id    BIGINT NOT NULL REFERENCES reportes_impacto(id) ON DELETE CASCADE,
  etiqueta      VARCHAR(100) NOT NULL,
  kg_rescatados DECIMAL(10,2) NOT NULL DEFAULT 0
);

CREATE TABLE reclamos (
  id                     BIGSERIAL PRIMARY KEY,
  codigo                 VARCHAR(20) NOT NULL UNIQUE,
  usuario_id             BIGINT NOT NULL REFERENCES usuarios(id),
  perfil_id              BIGINT NOT NULL REFERENCES perfiles_comercio(id),
  reserva_id             BIGINT REFERENCES reservas(id),
  tipo                   VARCHAR(10) NOT NULL CHECK (tipo IN ('RECLAMO', 'QUEJA')),
  bien_contratado        VARCHAR(10) NOT NULL DEFAULT 'PRODUCTO' CHECK (bien_contratado IN ('PRODUCTO', 'SERVICIO')),
  monto_reclamado        DECIMAL(10,2),
  detalle                TEXT NOT NULL,
  pedido                 TEXT NOT NULL,
  estado                 VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'EN_REVISION', 'RESPONDIDO', 'CERRADO')),
  respuesta              TEXT,
  respondido_en          TIMESTAMPTZ,
  fecha_limite_respuesta DATE NOT NULL,
  creado_en              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE calificaciones (
  id                 BIGSERIAL PRIMARY KEY,
  reserva_id         BIGINT NOT NULL UNIQUE REFERENCES reservas(id) ON DELETE CASCADE,
  usuario_id         BIGINT NOT NULL REFERENCES usuarios(id),
  local_id           BIGINT NOT NULL REFERENCES locales_comercio(id) ON DELETE CASCADE,
  puntuacion         SMALLINT NOT NULL CHECK (puntuacion BETWEEN 1 AND 5),
  comentario         VARCHAR(300),
  respuesta_comercio VARCHAR(300),
  creada_en          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION fn_actualizar_calificacion_local() RETURNS TRIGGER AS $$
DECLARE
  v_local BIGINT := COALESCE(NEW.local_id, OLD.local_id);
BEGIN
  UPDATE locales_comercio
     SET calificacion = COALESCE((SELECT ROUND(AVG(puntuacion)::numeric, 1)
                                    FROM calificaciones WHERE local_id = v_local), 0)
   WHERE id = v_local;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_calificacion_promedio
AFTER INSERT OR UPDATE OR DELETE ON calificaciones
FOR EACH ROW EXECUTE FUNCTION fn_actualizar_calificacion_local();

CREATE INDEX idx_packs_local          ON packs(local_id);
CREATE INDEX idx_packs_estado         ON packs(estado, stock_disponible);
CREATE INDEX idx_reservas_usuario     ON reservas(usuario_id);
CREATE INDEX idx_reservas_estado_exp  ON reservas(estado, expira_en);
CREATE INDEX idx_pagos_reserva        ON pagos(reserva_id);
CREATE INDEX idx_notificaciones_user  ON notificaciones(usuario_id, leida);
CREATE INDEX idx_reclamos_perfil      ON reclamos(perfil_id, estado);
CREATE INDEX idx_reclamos_usuario     ON reclamos(usuario_id);
CREATE INDEX idx_calificaciones_local ON calificaciones(local_id);
