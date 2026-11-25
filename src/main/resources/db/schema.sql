-- VETPLUS · esquema de la base de datos (MySQL / MariaDB de XAMPP, motor InnoDB).
-- La aplicacion lo ejecuta en cada arranque; todo es IF NOT EXISTS, no borra datos.

CREATE TABLE IF NOT EXISTS usuario (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL,
  apellidos VARCHAR(100) NOT NULL DEFAULT '',
  correo_usuario VARCHAR(150) NOT NULL,
  contrasena_hash VARCHAR(255) NOT NULL,
  rol VARCHAR(20) NOT NULL,
  especialidad VARCHAR(100) NOT NULL DEFAULT '',
  turno VARCHAR(20) NOT NULL DEFAULT '',
  telefono VARCHAR(20) NOT NULL DEFAULT '',
  curp VARCHAR(18) NOT NULL DEFAULT '',
  direccion VARCHAR(200) NOT NULL DEFAULT '',
  estado VARCHAR(10) NOT NULL DEFAULT 'Activo',
  debe_cambiar_contrasena TINYINT NOT NULL DEFAULT 0,
  contrasena_temporal VARCHAR(40) NULL,
  foto_ruta VARCHAR(255) NOT NULL DEFAULT '',
  UNIQUE KEY uq_usuario_correo (correo_usuario),
  CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMINISTRACION','MEDICO','ESTILISTA')),
  CONSTRAINT ck_usuario_estado CHECK (estado IN ('Activo','Inactivo'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bases creadas con versiones anteriores: agrega la columna si falta.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS contrasena_temporal VARCHAR(40) NULL;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS foto_ruta VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE usuario DROP COLUMN IF EXISTS foto;

CREATE TABLE IF NOT EXISTS propietario (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(80) NOT NULL,
  apellidos VARCHAR(100) NOT NULL DEFAULT '',
  telefono VARCHAR(20) NOT NULL DEFAULT '',
  correo VARCHAR(150) NOT NULL DEFAULT '',
  direccion VARCHAR(200) NOT NULL DEFAULT '',
  curp VARCHAR(18) NOT NULL DEFAULT '',
  fecha_registro DATE NOT NULL,
  canal_registro VARCHAR(30) NOT NULL DEFAULT 'Presencial',
  estado VARCHAR(10) NOT NULL DEFAULT 'Activo',
  contacto_alterno_nombre VARCHAR(120) NOT NULL DEFAULT '',
  contacto_alterno_telefono VARCHAR(20) NOT NULL DEFAULT '',
  CONSTRAINT ck_propietario_estado CHECK (estado IN ('Activo','Inactivo'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS mascota (
  id INT AUTO_INCREMENT PRIMARY KEY,
  propietario_id INT NOT NULL,
  nombre VARCHAR(80) NOT NULL,
  especie VARCHAR(20) NOT NULL DEFAULT 'Perro',
  raza VARCHAR(80) NOT NULL DEFAULT '',
  sexo VARCHAR(10) NOT NULL DEFAULT 'Hembra',
  fecha_nacimiento DATE NULL,
  peso DECIMAL(6,2) NULL,
  esterilizada TINYINT NOT NULL DEFAULT 0,
  microchip VARCHAR(40) NOT NULL DEFAULT '',
  alimentacion VARCHAR(200) NOT NULL DEFAULT '',
  antecedentes TEXT NULL,
  nota_estetica TEXT NULL,
  foto_ruta VARCHAR(500) NOT NULL DEFAULT '',
  estado VARCHAR(10) NOT NULL DEFAULT 'Activa',
  CONSTRAINT fk_mascota_prop FOREIGN KEY (propietario_id) REFERENCES propietario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS servicio (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL,
  descripcion VARCHAR(255) NOT NULL DEFAULT '',
  tipo VARCHAR(10) NOT NULL,
  precio DECIMAL(10,2) NOT NULL DEFAULT 0,
  duracion_min INT NULL,
  activo TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uq_servicio_nombre (nombre),
  CONSTRAINT ck_servicio_tipo CHECK (tipo IN ('MEDICO','ESTETICA','PENSION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cita (
  id INT AUTO_INCREMENT PRIMARY KEY,
  mascota_id INT NOT NULL,
  servicio_id INT NOT NULL,
  profesional_id INT NOT NULL,
  fecha DATE NOT NULL,
  hora_inicio VARCHAR(5) NOT NULL,
  duracion_min INT NOT NULL DEFAULT 30,
  consultorio VARCHAR(40) NOT NULL DEFAULT '',
  estado VARCHAR(20) NOT NULL DEFAULT 'Confirmada',
  motivo VARCHAR(255) NOT NULL DEFAULT '',
  creada_por INT NULL,
  KEY idx_cita_fecha (fecha),
  KEY idx_cita_prof (profesional_id, fecha),
  CONSTRAINT fk_cita_mascota FOREIGN KEY (mascota_id) REFERENCES mascota(id),
  CONSTRAINT fk_cita_servicio FOREIGN KEY (servicio_id) REFERENCES servicio(id),
  CONSTRAINT fk_cita_prof FOREIGN KEY (profesional_id) REFERENCES usuario(id),
  CONSTRAINT fk_cita_creada FOREIGN KEY (creada_por) REFERENCES usuario(id),
  CONSTRAINT ck_cita_estado CHECK (estado IN ('Confirmada','En atención','En proceso','Finalizada','Cancelada'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS atencion_medica (
  id INT AUTO_INCREMENT PRIMARY KEY,
  cita_id INT NOT NULL,
  motivo VARCHAR(255) NOT NULL DEFAULT '',
  peso DECIMAL(6,2) NULL,
  temperatura DECIMAL(4,1) NULL,
  frecuencia_cardiaca INT NULL,
  exploracion_diagnostico TEXT NULL,
  tratamiento TEXT NULL,
  proxima_revision DATE NULL,
  proxima_revision_hora VARCHAR(5) NULL,
  servicio_registrado_id INT NULL,
  estado VARCHAR(10) NOT NULL DEFAULT 'BORRADOR',
  actualizado_en DATETIME NULL,
  UNIQUE KEY uq_atencion_cita (cita_id),
  CONSTRAINT fk_atencion_cita FOREIGN KEY (cita_id) REFERENCES cita(id),
  CONSTRAINT fk_atencion_servicio FOREIGN KEY (servicio_registrado_id) REFERENCES servicio(id),
  CONSTRAINT ck_atencion_estado CHECK (estado IN ('BORRADOR','FINALIZADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS vacuna_cuidado (
  id INT AUTO_INCREMENT PRIMARY KEY,
  mascota_id INT NOT NULL,
  cuidado VARCHAR(80) NOT NULL,
  ultima_aplicacion DATE NULL,
  proxima_fecha DATE NULL,
  aplicado_por VARCHAR(150) NOT NULL DEFAULT '',
  CONSTRAINT fk_vacuna_mascota FOREIGN KEY (mascota_id) REFERENCES mascota(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS servicio_estetica (
  id INT AUTO_INCREMENT PRIMARY KEY,
  cita_id INT NOT NULL,
  preferencias TEXT NULL,
  chk_bano_secado TINYINT NOT NULL DEFAULT 0,
  chk_oidos TINYINT NOT NULL DEFAULT 0,
  chk_corte TINYINT NOT NULL DEFAULT 0,
  chk_revision_unas TINYINT NOT NULL DEFAULT 0,
  observaciones TEXT NULL,
  estado VARCHAR(12) NOT NULL DEFAULT 'EN_PROCESO',
  entrega_estimada VARCHAR(5) NULL,
  propietario_notificado TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uq_estetica_cita (cita_id),
  CONSTRAINT fk_estetica_cita FOREIGN KEY (cita_id) REFERENCES cita(id),
  CONSTRAINT ck_estetica_estado CHECK (estado IN ('EN_PROCESO','FINALIZADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS estancia (
  id INT AUTO_INCREMENT PRIMARY KEY,
  mascota_id INT NOT NULL,
  fecha_entrada DATE NOT NULL,
  fecha_salida_prevista DATE NOT NULL,
  hora_salida VARCHAR(5) NOT NULL DEFAULT '18:00',
  estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVA',
  CONSTRAINT fk_estancia_mascota FOREIGN KEY (mascota_id) REFERENCES mascota(id),
  CONSTRAINT ck_estancia_estado CHECK (estado IN ('ACTIVA','FINALIZADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cuenta (
  id INT AUTO_INCREMENT PRIMARY KEY,
  propietario_id INT NOT NULL,
  fecha DATE NOT NULL,
  estado VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE',
  subtotal DECIMAL(10,2) NOT NULL DEFAULT 0,
  descuento_pct INT NOT NULL DEFAULT 0,
  descuento_monto DECIMAL(10,2) NOT NULL DEFAULT 0,
  total DECIMAL(10,2) NOT NULL DEFAULT 0,
  CONSTRAINT fk_cuenta_prop FOREIGN KEY (propietario_id) REFERENCES propietario(id),
  CONSTRAINT ck_cuenta_estado CHECK (estado IN ('PENDIENTE','PAGADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cuenta_detalle (
  id INT AUTO_INCREMENT PRIMARY KEY,
  cuenta_id INT NOT NULL,
  mascota_id INT NOT NULL,
  servicio_id INT NOT NULL,
  cita_id INT NULL,
  cantidad INT NOT NULL DEFAULT 1,
  precio_unitario DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_detalle_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta(id),
  CONSTRAINT fk_detalle_mascota FOREIGN KEY (mascota_id) REFERENCES mascota(id),
  CONSTRAINT fk_detalle_servicio FOREIGN KEY (servicio_id) REFERENCES servicio(id),
  CONSTRAINT fk_detalle_cita FOREIGN KEY (cita_id) REFERENCES cita(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pago (
  id INT AUTO_INCREMENT PRIMARY KEY,
  cuenta_id INT NOT NULL,
  fecha_hora DATETIME NOT NULL,
  metodo VARCHAR(15) NOT NULL,
  importe DECIMAL(10,2) NOT NULL,
  referencia VARCHAR(100) NOT NULL DEFAULT '',
  correo_comprobante VARCHAR(150) NOT NULL DEFAULT '',
  CONSTRAINT fk_pago_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuenta(id),
  CONSTRAINT ck_pago_metodo CHECK (metodo IN ('TARJETA','EFECTIVO','TRANSFERENCIA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS proveedor (
  id INT AUTO_INCREMENT PRIMARY KEY,
  empresa VARCHAR(120) NOT NULL,
  contacto VARCHAR(120) NOT NULL DEFAULT '',
  telefono VARCHAR(20) NOT NULL DEFAULT '',
  correo VARCHAR(150) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS insumo (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL,
  categoria VARCHAR(80) NOT NULL DEFAULT '',
  existencia DECIMAL(10,2) NOT NULL DEFAULT 0,
  minimo DECIMAL(10,2) NOT NULL DEFAULT 0,
  precio_compra DECIMAL(10,2) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orden_compra (
  id INT AUTO_INCREMENT PRIMARY KEY,
  folio VARCHAR(20) NOT NULL,
  proveedor_id INT NULL,
  creada_por INT NULL,
  fecha DATE NOT NULL,
  fecha_entrega DATE NULL,
  estado VARCHAR(10) NOT NULL DEFAULT 'BORRADOR',
  notas TEXT NULL,
  total DECIMAL(10,2) NOT NULL DEFAULT 0,
  UNIQUE KEY uq_orden_folio (folio),
  CONSTRAINT fk_orden_prov FOREIGN KEY (proveedor_id) REFERENCES proveedor(id),
  CONSTRAINT fk_orden_usuario FOREIGN KEY (creada_por) REFERENCES usuario(id),
  CONSTRAINT ck_orden_estado CHECK (estado IN ('BORRADOR','EMITIDA','RECIBIDA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orden_compra_renglon (
  id INT AUTO_INCREMENT PRIMARY KEY,
  orden_id INT NOT NULL,
  insumo_id INT NOT NULL,
  cantidad DECIMAL(10,2) NOT NULL,
  precio_unitario DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_renglon_orden FOREIGN KEY (orden_id) REFERENCES orden_compra(id) ON DELETE CASCADE,
  CONSTRAINT fk_renglon_insumo FOREIGN KEY (insumo_id) REFERENCES insumo(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS actividad (
  id INT AUTO_INCREMENT PRIMARY KEY,
  fecha_hora DATETIME NOT NULL,
  tipo VARCHAR(20) NOT NULL DEFAULT '',
  titulo VARCHAR(200) NOT NULL,
  detalle VARCHAR(500) NOT NULL DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
