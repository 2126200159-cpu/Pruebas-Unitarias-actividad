package algo.vetplusapp.db;

import java.time.LocalDate;

/**
 * Datos iniciales del negocio: servicios con tarifas de referencia, personal de ejemplo, proveedores e insumos.
 * Todo se inserta solo si no existe (no pisa lo que administracion ya haya capturado).
 * Precios en MXN, tomados como referencia del mercado mexicano (estéticas y clínicas privadas); se ajustan
 * en Servicios y tarifas.
 */
final class Semilla {

    static final String ADMIN_USUARIO = "admin@vetplus.com";
    static final String ADMIN_CONTRASENA = "admin123";
    /** Contrasena inicial del personal de ejemplo; cada quien debe cambiarla en su primer acceso. */
    static final String PERSONAL_CONTRASENA = "VetPlus2026";

    private static final Object[][] SERVICIOS = {
        {"Consulta", "Consulta médica general", "MEDICO", 350.00, 30},
        {"Vacunación", "Aplicación de vacunas", "MEDICO", 300.00, 30},
        {"Desparasitación", "Control de parásitos internos y externos", "MEDICO", 150.00, 30},
        {"Cirugía", "Procedimientos quirúrgicos (esterilización y otros)", "MEDICO", 2800.00, 90},
        {"Estética", "Baño, corte y cuidado estético", "ESTETICA", 350.00, 60},
        {"Pensión", "Hospedaje por noche", "PENSION", 300.00, null}
    };
    private static final String[][] PERSONAL = {
        {"Lucía", "Torres Navarro", "lucia.torres@vetplus.com", "MEDICO", "Medicina general", "Matutino", "3331234511"},
        {"Ricardo", "Mendoza Ibarra", "ricardo.mendoza@vetplus.com", "MEDICO", "Cirugía y medicina interna", "Vespertino", "3331234512"},
        {"Fernanda", "Castillo Ruiz", "fernanda.castillo@vetplus.com", "ESTILISTA", "Estética canina", "Matutino", "3331234513"},
        {"Diego", "Salazar Pérez", "diego.salazar@vetplus.com", "ESTILISTA", "Corte y estilizado", "Vespertino", "3331234514"}
    };
    private static final String[][] PROVEEDORES = {
        {"Distribuidora Veterinaria del Occidente", "Mónica Reyes", "3331234501", "ventas@distvetoccidente.example.com"},
        {"Farma Animal Jalisco", "Héctor Ramírez", "3331234502", "pedidos@farmaanimal.example.com"},
        {"PetCare Suministros", "Laura Gómez", "3331234503", "contacto@petcare.example.com"}
    };
    private static final Object[][] INSUMOS = {
        {"Vacuna múltiple (polivalente)", "Vacunas", 40.00, 15.00, 95.00},
        {"Vacuna antirrábica", "Vacunas", 40.00, 15.00, 55.00},
        {"Desparasitante interno (tableta)", "Desparasitación", 80.00, 30.00, 15.00},
        {"Antipulgas y garrapatas (pipeta)", "Desparasitación", 30.00, 12.00, 90.00},
        {"Jeringa 3 ml (pieza)", "Material de curación", 150.00, 50.00, 2.50},
        {"Gasas estériles (paquete)", "Material de curación", 60.00, 20.00, 18.00},
        {"Guantes de nitrilo (caja)", "Material de curación", 8.00, 3.00, 150.00},
        {"Suero fisiológico 500 ml", "Material de curación", 20.00, 8.00, 38.00},
        {"Champú hipoalergénico (litro)", "Estética", 10.00, 4.00, 170.00},
        {"Acondicionador de pelo (litro)", "Estética", 8.00, 3.00, 190.00},
        {"Limpiador de oídos (frasco)", "Estética", 12.00, 5.00, 85.00},
        {"Toallas desechables (paquete)", "Estética", 15.00, 5.00, 60.00},
        {"Alimento de recuperación (lata)", "Alimentos", 25.00, 10.00, 42.00}
    };

    private Semilla() {
    }

    static void cargar() {
        for (Object[] s : SERVICIOS) {
            Db.ejecutar("INSERT IGNORE INTO servicio (nombre, descripcion, tipo, precio, duracion_min, activo) "
                    + "VALUES (?, ?, ?, ?, ?, 1)", s[0], s[1], s[2], s[3], s[4]);
            // si la tarifa sigue en 0.00 (nunca capturada) se pone la de referencia; una tarifa ya capturada no se toca
            Db.ejecutar("UPDATE servicio SET precio = ? WHERE nombre = ? AND precio = 0", s[3], s[0]);
        }

        // versiones anteriores usaban un usuario sin @: se pasa al correo sin perder la cuenta ni su contrasena
        String[] anteriores = {"admin", "lucia.torres", "ricardo.mendoza", "fernanda.castillo", "diego.salazar"};
        for (String viejo : anteriores) {
            String nuevo = viejo + "@vetplus.com";
            if (Db.contar("SELECT COUNT(*) AS v FROM usuario WHERE correo_usuario = ?", nuevo) == 0) {
                Db.ejecutar("UPDATE usuario SET correo_usuario = ? WHERE correo_usuario = ?", nuevo, viejo);
            }
        }

        if (Db.contar("SELECT COUNT(*) AS v FROM usuario WHERE rol = 'ADMINISTRACION'") == 0) {
            Db.ejecutar("INSERT INTO usuario (nombre, apellidos, correo_usuario, contrasena_hash, rol, "
                    + "especialidad, turno, estado, debe_cambiar_contrasena, contrasena_temporal) "
                    + "VALUES (?, ?, ?, ?, 'ADMINISTRACION', 'Administración general', 'Matutino', 'Activo', 1, ?)",
                    "Administrador", "VETPLUS", ADMIN_USUARIO, Hash.crear(ADMIN_CONTRASENA), ADMIN_CONTRASENA);
        }
        for (String[] p : PERSONAL) {
            if (Db.contar("SELECT COUNT(*) AS v FROM usuario WHERE correo_usuario = ?", p[2]) == 0) {
                Db.ejecutar("INSERT INTO usuario (nombre, apellidos, correo_usuario, contrasena_hash, rol, "
                        + "especialidad, turno, telefono, estado, debe_cambiar_contrasena, contrasena_temporal) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Activo', 1, ?)",
                        p[0], p[1], p[2], Hash.crear(PERSONAL_CONTRASENA), p[3], p[4], p[5], p[6],
                        PERSONAL_CONTRASENA);
            }
        }
        if (Db.contar("SELECT COUNT(*) AS v FROM proveedor") == 0) {
            for (String[] p : PROVEEDORES) {
                Db.ejecutar("INSERT INTO proveedor (empresa, contacto, telefono, correo) VALUES (?, ?, ?, ?)",
                        p[0], p[1], p[2], p[3]);
            }
        }
        if (Db.contar("SELECT COUNT(*) AS v FROM insumo") == 0) {
            for (Object[] i : INSUMOS) {
                Db.ejecutar("INSERT INTO insumo (nombre, categoria, existencia, minimo, precio_compra) "
                        + "VALUES (?, ?, ?, ?, ?)", i[0], i[1], i[2], i[3], i[4]);
            }
        }
        // cuentas de versiones anteriores que aun no han cambiado su contrasena inicial
        Db.ejecutar("UPDATE usuario SET contrasena_temporal = ? WHERE correo_usuario = ? "
                + "AND debe_cambiar_contrasena = 1 AND contrasena_temporal IS NULL", ADMIN_CONTRASENA, ADMIN_USUARIO);
        for (String[] p : PERSONAL) {
            Db.ejecutar("UPDATE usuario SET contrasena_temporal = ? WHERE correo_usuario = ? "
                    + "AND debe_cambiar_contrasena = 1 AND contrasena_temporal IS NULL", PERSONAL_CONTRASENA, p[2]);
        }
        Db.ejecutar("UPDATE estancia SET estado = 'FINALIZADA' WHERE estado = 'ACTIVA' AND fecha_salida_prevista < ?",
                LocalDate.now().toString());
    }
}
