-- VETPLUS · script para phpMyAdmin (opcional).
-- La aplicacion crea todo esto sola al abrirse; este archivo sirve para revisar o recrear la base a mano.
-- Precios en MXN, de referencia del mercado mexicano; se ajustan despues en Servicios y tarifas.

CREATE DATABASE IF NOT EXISTS vetplus CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE vetplus;

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

-- ============ Datos iniciales ============

-- Servicios (tarifa de referencia y duracion en minutos)
INSERT IGNORE INTO servicio (nombre, descripcion, tipo, precio, duracion_min, activo) VALUES
  ('Consulta', 'Consulta médica general', 'MEDICO', 350.00, 30, 1),
  ('Vacunación', 'Aplicación de vacunas', 'MEDICO', 300.00, 30, 1),
  ('Desparasitación', 'Control de parásitos internos y externos', 'MEDICO', 150.00, 30, 1),
  ('Cirugía', 'Procedimientos quirúrgicos (esterilización y otros)', 'MEDICO', 2800.00, 90, 1),
  ('Estética', 'Baño, corte y cuidado estético', 'ESTETICA', 350.00, 60, 1),
  ('Pensión', 'Hospedaje por noche', 'PENSION', 300.00, NULL, 1);

-- Administrador: usuario admin@vetplus.com / contraseña admin123 (pide cambiarla al entrar)
INSERT INTO usuario (nombre, apellidos, correo_usuario, contrasena_hash, rol, especialidad, turno, estado, debe_cambiar_contrasena)
SELECT 'Administrador', 'VETPLUS', 'admin@vetplus.com', 'pbkdf2$120000$PnQ+cVH/JT+rnZh6ClsDjQ==$+BsFYl2QFRIf9BA3FHpr87f4VsfpA4U4oy9t1cHYOic=', 'ADMINISTRACION', 'Administración general', 'Matutino', 'Activo', 1
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE correo_usuario = 'admin@vetplus.com');

-- Personal de ejemplo: contraseña inicial VetPlus2026 (pide cambiarla al entrar)
INSERT IGNORE INTO usuario (nombre, apellidos, correo_usuario, contrasena_hash, rol, especialidad, turno, telefono, estado, debe_cambiar_contrasena) VALUES
  ('Lucía', 'Torres Navarro', 'lucia.torres@vetplus.com', 'pbkdf2$120000$89Ndk6uE6j0VKQ4VqLOj3Q==$HbkvxnKx2U2dVXKinTnn++bQV1L+KkUJjkGnGfQyHvA=', 'MEDICO', 'Medicina general', 'Matutino', '3331234511', 'Activo', 1),
  ('Ricardo', 'Mendoza Ibarra', 'ricardo.mendoza@vetplus.com', 'pbkdf2$120000$MVR3lYND7zOD2sd8abIpWg==$HiKPtHeDXS/m97mNPnX5n+zz6ZY9438p4zJln62Vrz0=', 'MEDICO', 'Cirugía y medicina interna', 'Vespertino', '3331234512', 'Activo', 1),
  ('Fernanda', 'Castillo Ruiz', 'fernanda.castillo@vetplus.com', 'pbkdf2$120000$Jg1VQjIBOlt7ISntOY9gsw==$gkGJ1THkQROrtLeG45vaVfDxgxOKHFg2dnXHlAKhb+M=', 'ESTILISTA', 'Estética canina', 'Matutino', '3331234513', 'Activo', 1),
  ('Diego', 'Salazar Pérez', 'diego.salazar@vetplus.com', 'pbkdf2$120000$zOpxcOs9vEPR9SA4bSr+eQ==$7SlaMGHH6hDqTiF3cBVhMJ8+8huSI80EDrx8sPXffxE=', 'ESTILISTA', 'Corte y estilizado', 'Vespertino', '3331234514', 'Activo', 1);

-- Proveedores
INSERT INTO proveedor (empresa, contacto, telefono, correo) VALUES
  ('Distribuidora Veterinaria del Occidente', 'Mónica Reyes', '3331234501', 'ventas@distvetoccidente.example.com'),
  ('Farma Animal Jalisco', 'Héctor Ramírez', '3331234502', 'pedidos@farmaanimal.example.com'),
  ('PetCare Suministros', 'Laura Gómez', '3331234503', 'contacto@petcare.example.com');

-- Insumos (existencia, minimo y precio de compra de referencia)
INSERT INTO insumo (nombre, categoria, existencia, minimo, precio_compra) VALUES
  ('Vacuna múltiple (polivalente)', 'Vacunas', 40, 15, 95),
  ('Vacuna antirrábica', 'Vacunas', 40, 15, 55),
  ('Desparasitante interno (tableta)', 'Desparasitación', 80, 30, 15),
  ('Antipulgas y garrapatas (pipeta)', 'Desparasitación', 30, 12, 90),
  ('Jeringa 3 ml (pieza)', 'Material de curación', 150, 50, 2.5),
  ('Gasas estériles (paquete)', 'Material de curación', 60, 20, 18),
  ('Guantes de nitrilo (caja)', 'Material de curación', 8, 3, 150),
  ('Suero fisiológico 500 ml', 'Material de curación', 20, 8, 38),
  ('Champú hipoalergénico (litro)', 'Estética', 10, 4, 170),
  ('Acondicionador de pelo (litro)', 'Estética', 8, 3, 190),
  ('Limpiador de oídos (frasco)', 'Estética', 12, 5, 85),
  ('Toallas desechables (paquete)', 'Estética', 15, 5, 60),
  ('Alimento de recuperación (lata)', 'Alimentos', 25, 10, 42);

-- Contrasenas temporales visibles para administracion hasta que cada empleado la cambie
UPDATE usuario SET contrasena_temporal = 'admin123' WHERE correo_usuario = 'admin@vetplus.com' AND debe_cambiar_contrasena = 1 AND contrasena_temporal IS NULL;
UPDATE usuario SET contrasena_temporal = 'VetPlus2026' WHERE correo_usuario IN ('lucia.torres@vetplus.com','ricardo.mendoza@vetplus.com','fernanda.castillo@vetplus.com','diego.salazar@vetplus.com') AND debe_cambiar_contrasena = 1 AND contrasena_temporal IS NULL;
