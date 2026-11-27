-- V1: esquema inicial del sistema de gestion del club
-- Fuente: docs/DB-SCHEMA.md (secciones 2 y 3)
-- 25 tablas, en orden de dependencia de claves foraneas.
-- No modela las decisiones pendientes: credenciales de CallMeBot, estado de Pase,
-- nivel torneo ni division deportiva.

-- ---------------------------------------------------------------------------
-- 1. Liga, club y participacion deportiva
-- ---------------------------------------------------------------------------

CREATE TABLE liga (
    id_liga   integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre    varchar(120) NOT NULL,
    provincia varchar(80),
    activa    boolean NOT NULL DEFAULT true
);

CREATE TABLE club (
    id_club          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_liga          integer NOT NULL REFERENCES liga (id_liga) ON DELETE RESTRICT,
    nombre           varchar(150) NOT NULL,
    cuit             varchar(13) NOT NULL,
    domicilio        varchar(200),
    escudo_url       varchar(300),
    color_primario   varchar(20),
    color_secundario varchar(20),
    activo           boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_club_cuit UNIQUE (cuit)
);

CREATE TABLE temporada (
    id_temporada integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_liga      integer NOT NULL REFERENCES liga (id_liga) ON DELETE RESTRICT,
    anio         integer NOT NULL,
    fecha_inicio date NOT NULL,
    fecha_fin    date NOT NULL,
    activa       boolean NOT NULL DEFAULT false,
    fecha_cierre date,
    CONSTRAINT ck_temporada_cierre CHECK (NOT (activa AND fecha_cierre IS NOT NULL))
);

CREATE TABLE categoria (
    id_categoria integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_liga      integer NOT NULL REFERENCES liga (id_liga) ON DELETE RESTRICT,
    anio_desde   integer NOT NULL,
    anio_hasta   integer NOT NULL,
    nombre       varchar(80) NOT NULL,
    CONSTRAINT ck_categoria_rango CHECK (anio_desde <= anio_hasta)
);

CREATE TABLE club_temporada_categoria (
    id_club_temporada_categoria integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_club                     integer NOT NULL REFERENCES club (id_club) ON DELETE RESTRICT,
    id_temporada                integer NOT NULL REFERENCES temporada (id_temporada) ON DELETE RESTRICT,
    id_categoria                integer NOT NULL REFERENCES categoria (id_categoria) ON DELETE RESTRICT,
    activa                      boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_ctc_club_temporada_categoria UNIQUE (id_club, id_temporada, id_categoria)
);

-- ---------------------------------------------------------------------------
-- 2. Padron global y tutores
-- ---------------------------------------------------------------------------

CREATE TABLE jugador (
    id_jugador        integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_liga           integer NOT NULL REFERENCES liga (id_liga) ON DELETE RESTRICT,
    dni               varchar(15) NOT NULL,
    nombre            varchar(100) NOT NULL,
    apellido          varchar(100) NOT NULL,
    fecha_nacimiento  date NOT NULL,
    lugar_nacimiento  varchar(120),
    domicilio         varchar(200),
    foto_url          varchar(300),
    fecha_apto_medico date,
    CONSTRAINT uq_jugador_dni_liga UNIQUE (dni, id_liga)
);

CREATE TABLE tutor (
    id_tutor  integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dni       varchar(15) NOT NULL,
    nombre    varchar(100) NOT NULL,
    apellido  varchar(100) NOT NULL,
    telefono  varchar(30),
    domicilio varchar(200),
    CONSTRAINT uq_tutor_dni UNIQUE (dni)
);

CREATE TABLE jugador_tutor (
    id_jugador  integer NOT NULL REFERENCES jugador (id_jugador) ON DELETE CASCADE,
    id_tutor    integer NOT NULL REFERENCES tutor (id_tutor) ON DELETE RESTRICT,
    vinculo     varchar(10) NOT NULL,
    fecha_desde date NOT NULL,
    fecha_hasta date,
    PRIMARY KEY (id_jugador, id_tutor, fecha_desde),
    CONSTRAINT ck_jugador_tutor_vinculo CHECK (vinculo IN ('Padre', 'Madre', 'Tutor'))
);

-- ---------------------------------------------------------------------------
-- 3. Seguridad y roles
-- ---------------------------------------------------------------------------

CREATE TABLE rol (
    id_rol integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre varchar(40) NOT NULL
);

CREATE TABLE usuario (
    id_usuario   integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_liga      integer REFERENCES liga (id_liga) ON DELETE RESTRICT,
    id_club      integer REFERENCES club (id_club) ON DELETE RESTRICT,
    keycloak_sub varchar(120) NOT NULL,
    username     varchar(60),
    email        varchar(150),
    nombre       varchar(100),
    apellido     varchar(100),
    telefono     varchar(30),
    dni          varchar(15),
    activo       boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_usuario_keycloak_sub UNIQUE (keycloak_sub),
    CONSTRAINT ck_usuario_alcance CHECK ((id_liga IS NOT NULL) <> (id_club IS NOT NULL))
);

CREATE TABLE usuario_rol (
    id_usuario   integer NOT NULL REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    id_rol       integer NOT NULL REFERENCES rol (id_rol) ON DELETE RESTRICT,
    fecha_inicio date NOT NULL,
    fecha_fin    date,
    PRIMARY KEY (id_usuario, id_rol, fecha_inicio)
);

CREATE TABLE usuario_categoria (
    id_usuario                  integer NOT NULL REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    id_club_temporada_categoria integer NOT NULL REFERENCES club_temporada_categoria (id_club_temporada_categoria) ON DELETE CASCADE,
    rol_en_categoria            varchar(20) NOT NULL,
    PRIMARY KEY (id_usuario, id_club_temporada_categoria)
);

-- ---------------------------------------------------------------------------
-- 4. Fichaje y derivados
-- ---------------------------------------------------------------------------

CREATE TABLE grupo_familiar (
    id_grupo         integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_club          integer NOT NULL REFERENCES club (id_club) ON DELETE RESTRICT,
    apellido_familia varchar(120) NOT NULL,
    observaciones    varchar(300)
);

CREATE TABLE fichaje_inscripcion (
    id_fichaje                  integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_club_temporada_categoria integer NOT NULL REFERENCES club_temporada_categoria (id_club_temporada_categoria) ON DELETE RESTRICT,
    id_jugador                  integer NOT NULL REFERENCES jugador (id_jugador) ON DELETE RESTRICT,
    id_grupo                    integer REFERENCES grupo_familiar (id_grupo) ON DELETE RESTRICT,
    tipo_fichaje                varchar(12) NOT NULL,
    porcentaje_beca             numeric(5, 2) NOT NULL DEFAULT 0,
    consentimiento_imagen       boolean NOT NULL DEFAULT false,
    nro_carnet_liga             varchar(40),
    fecha_alta                  date NOT NULL DEFAULT CURRENT_DATE,
    fecha_baja                  date,
    CONSTRAINT ck_tipo_fichaje CHECK (tipo_fichaje IN ('Propio', 'A_Prestamo', 'Libre'))
);

CREATE TABLE estado_jugador (
    id_estado_jugador integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_fichaje        integer NOT NULL REFERENCES fichaje_inscripcion (id_fichaje) ON DELETE CASCADE,
    id_usuario        integer REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    fecha_hora        timestamptz NOT NULL DEFAULT now(),
    valor             varchar(10) NOT NULL,
    motivo            varchar(300),
    CONSTRAINT ck_estado_jugador_valor CHECK (valor IN ('Activo', 'Inactivo'))
);

CREATE TABLE pase (
    id_pase                integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_fichaje             integer NOT NULL REFERENCES fichaje_inscripcion (id_fichaje) ON DELETE CASCADE,
    id_club_cedente        integer REFERENCES club (id_club) ON DELETE RESTRICT,
    id_club_cesionario     integer REFERENCES club (id_club) ON DELETE RESTRICT,
    club_cedente_nombre    varchar(150),
    club_cesionario_nombre varchar(150),
    fecha_inicio           date NOT NULL,
    fecha_fin              date,
    CONSTRAINT uq_pase_fichaje UNIQUE (id_fichaje),
    CONSTRAINT ck_pase_cedente CHECK (id_club_cedente IS NOT NULL OR club_cedente_nombre IS NOT NULL),
    CONSTRAINT ck_pase_cesionario CHECK (id_club_cesionario IS NOT NULL OR club_cesionario_nombre IS NOT NULL)
);

CREATE TABLE seguro (
    id_seguro         integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_fichaje        integer NOT NULL REFERENCES fichaje_inscripcion (id_fichaje) ON DELETE CASCADE,
    fecha_alta        date NOT NULL,
    fecha_vencimiento date NOT NULL,
    nro_poliza        varchar(40) NOT NULL
);

-- ---------------------------------------------------------------------------
-- 5. Partidos
-- ---------------------------------------------------------------------------

CREATE TABLE partido (
    id_partido                  integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_club_temporada_categoria integer NOT NULL REFERENCES club_temporada_categoria (id_club_temporada_categoria) ON DELETE RESTRICT,
    fecha_hora                  timestamptz NOT NULL,
    rival                       varchar(150) NOT NULL,
    condicion                   varchar(10) NOT NULL,
    cancha                      varchar(120),
    estado                      varchar(12) NOT NULL DEFAULT 'Programado',
    gol_local                   integer,
    gol_visitante               integer,
    CONSTRAINT ck_partido_condicion CHECK (condicion IN ('Local', 'Visitante')),
    CONSTRAINT ck_partido_estado CHECK (estado IN ('Programado', 'Jugado', 'Suspendido'))
);

CREATE TABLE convocatoria_partido (
    id_partido         integer NOT NULL REFERENCES partido (id_partido) ON DELETE CASCADE,
    id_jugador         integer NOT NULL REFERENCES jugador (id_jugador) ON DELETE RESTRICT,
    dorsal             integer NOT NULL,
    goles              integer NOT NULL DEFAULT 0,
    tarjetas_amarillas integer NOT NULL DEFAULT 0,
    tarjeta_roja       boolean NOT NULL DEFAULT false,
    PRIMARY KEY (id_partido, id_jugador),
    CONSTRAINT uq_convocatoria_dorsal UNIQUE (id_partido, dorsal)
);

CREATE TABLE cambio_partido (
    id_cambio       integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_partido      integer NOT NULL REFERENCES partido (id_partido) ON DELETE CASCADE,
    id_jugador_sale  integer NOT NULL REFERENCES jugador (id_jugador) ON DELETE RESTRICT,
    id_jugador_entra integer NOT NULL REFERENCES jugador (id_jugador) ON DELETE RESTRICT,
    minuto          integer NOT NULL
);

-- ---------------------------------------------------------------------------
-- 6. Finanzas
-- ---------------------------------------------------------------------------

CREATE TABLE valor_cuota (
    id_valor_cuota integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_club        integer NOT NULL REFERENCES club (id_club) ON DELETE RESTRICT,
    precio         numeric(12, 2) NOT NULL,
    fecha_vigencia date NOT NULL,
    CONSTRAINT ck_valor_cuota_precio CHECK (precio >= 0)
);

CREATE TABLE pago (
    id_pago          integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_club          integer NOT NULL REFERENCES club (id_club) ON DELETE RESTRICT,
    id_usuario_cobro integer REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    nro_recibo       varchar(40) NOT NULL,
    fecha_pago       timestamptz NOT NULL DEFAULT now(),
    medio_pago       varchar(15) NOT NULL,
    monto_total      numeric(12, 2) NOT NULL,
    estado           varchar(12) NOT NULL DEFAULT 'Registrado',
    motivo_anulacion varchar(300),
    CONSTRAINT uq_pago_recibo UNIQUE (id_club, nro_recibo),
    CONSTRAINT ck_pago_medio CHECK (medio_pago IN ('Efectivo', 'Transferencia')),
    CONSTRAINT ck_pago_estado CHECK (estado IN ('Registrado', 'Anulado')),
    CONSTRAINT ck_pago_monto CHECK (monto_total >= 0)
);

CREATE TABLE cuota (
    id_cuota           integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_club            integer NOT NULL REFERENCES club (id_club) ON DELETE RESTRICT,
    id_jugador         integer NOT NULL REFERENCES jugador (id_jugador) ON DELETE RESTRICT,
    id_fichaje         integer NOT NULL REFERENCES fichaje_inscripcion (id_fichaje) ON DELETE RESTRICT,
    id_valor_cuota     integer NOT NULL REFERENCES valor_cuota (id_valor_cuota) ON DELETE RESTRICT,
    id_pago            integer REFERENCES pago (id_pago) ON DELETE RESTRICT,
    periodo            varchar(7) NOT NULL,
    fecha_vencimiento  date NOT NULL,
    monto_base         numeric(12, 2) NOT NULL,
    descuento_aplicado numeric(12, 2) NOT NULL DEFAULT 0,
    estado             varchar(10) NOT NULL DEFAULT 'Pendiente',
    CONSTRAINT uq_cuota_fichaje_periodo UNIQUE (id_fichaje, periodo),
    CONSTRAINT ck_cuota_estado CHECK (estado IN ('Pendiente', 'Pagada', 'Adeudada', 'Anulada')),
    CONSTRAINT ck_cuota_montos CHECK (monto_base >= 0 AND descuento_aplicado >= 0)
);

CREATE TABLE estado_cuota (
    id_estado_cuota integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_cuota        integer NOT NULL REFERENCES cuota (id_cuota) ON DELETE CASCADE,
    fecha_hora      timestamptz NOT NULL DEFAULT now(),
    valor_estado    varchar(10) NOT NULL,
    CONSTRAINT ck_estado_cuota_valor CHECK (valor_estado IN ('Pendiente', 'Pagada', 'Adeudada', 'Anulada'))
);

-- ---------------------------------------------------------------------------
-- 7. Notificaciones
-- ---------------------------------------------------------------------------

CREATE TABLE notificacion (
    id_notificacion   integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_club           integer NOT NULL REFERENCES club (id_club) ON DELETE RESTRICT,
    id_usuario        integer NOT NULL REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    id_jugador        integer REFERENCES jugador (id_jugador) ON DELETE CASCADE,
    tipo_notificacion varchar(15) NOT NULL,
    titulo            varchar(150) NOT NULL,
    mensaje           text,
    fecha_creacion    timestamptz NOT NULL DEFAULT now(),
    leida             boolean NOT NULL DEFAULT false,
    CONSTRAINT ck_tipo_notificacion CHECK (tipo_notificacion IN ('Cumpleanios', 'Sistema'))
);

-- ---------------------------------------------------------------------------
-- 8. Indices unicos parciales (DB-SCHEMA 3.1 y 3.2)
-- ---------------------------------------------------------------------------

-- Una sola temporada activa por liga.
CREATE UNIQUE INDEX uq_temporada_activa_liga ON temporada (id_liga) WHERE activa;

-- Un jugador no puede estar fichado en dos clubes a la vez.
CREATE UNIQUE INDEX uq_fichaje_vigente ON fichaje_inscripcion (id_jugador) WHERE fecha_baja IS NULL;

-- Un usuario no puede tener dos cargos vigentes a la vez.
CREATE UNIQUE INDEX uq_usuario_rol_vigente ON usuario_rol (id_usuario) WHERE fecha_fin IS NULL;

-- ---------------------------------------------------------------------------
-- 9. Indices por camino de lectura (DB-SCHEMA 3.7)
-- ---------------------------------------------------------------------------

CREATE INDEX idx_partido_ctc_fecha      ON partido (id_club_temporada_categoria, fecha_hora);
CREATE INDEX idx_fichaje_ctc            ON fichaje_inscripcion (id_club_temporada_categoria);
CREATE INDEX idx_cuota_fichaje_estado   ON cuota (id_fichaje, estado);
CREATE INDEX idx_cuota_club_periodo     ON cuota (id_club, periodo);
CREATE INDEX idx_estado_jugador_fichaje ON estado_jugador (id_fichaje, fecha_hora DESC);
CREATE INDEX idx_estado_cuota_cuota     ON estado_cuota (id_cuota, fecha_hora DESC);
CREATE INDEX idx_notificacion_usuario   ON notificacion (id_usuario, leida);
CREATE INDEX idx_jugador_fecha_nac      ON jugador (fecha_nacimiento);
