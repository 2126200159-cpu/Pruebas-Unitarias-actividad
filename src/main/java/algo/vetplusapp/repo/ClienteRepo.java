package algo.vetplusapp.repo;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.ImagenStore;
import algo.vetplusapp.util.Valida;
import java.time.LocalDate;
import java.util.List;

/** Propietarios, mascotas, vacunas e historial. */
public final class ClienteRepo {

    private ClienteRepo() {
    }

    // ---------------------------------------------------------------- propietarios

    public static List<Row> propietarios() {
        return Db.consultar("SELECT p.*, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS nombre_completo, "
                + "(SELECT GROUP_CONCAT(m.nombre SEPARATOR ', ') FROM mascota m WHERE m.propietario_id = p.id) AS mascotas "
                + "FROM propietario p ORDER BY p.nombre, p.apellidos");
    }

    public static List<Row> propietariosActivos() {
        return Db.consultar("SELECT p.id, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS nombre_completo "
                + "FROM propietario p WHERE p.estado = 'Activo' ORDER BY p.nombre, p.apellidos");
    }

    public static Row propietario(long id) {
        return Db.uno("SELECT p.*, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS nombre_completo "
                + "FROM propietario p WHERE p.id = ?", id);
    }

    private static boolean existeOtro(String columna, String valor, Long idExcluir) {
        // la columna viene siempre de constantes de este metodo, nunca del usuario
        return Db.contar("SELECT COUNT(*) AS v FROM propietario WHERE " + columna + " = ? AND (? IS NULL OR id <> ?)",
                valor, idExcluir, idExcluir) > 0;
    }

    /** Alta (id nulo) o edicion. Devuelve el id. No permite clientes duplicados (CURP, correo o nombre+teléfono). */
    public static long guardarPropietario(Long id, String nombre, String apellidos, String telefono, String correo,
            String direccion, String curp, String altNombre, String altTelefono) {
        Valida.nombre(nombre, "El nombre", true);
        Valida.nombre(apellidos, "Los apellidos", true);
        Valida.telefono(telefono, true);
        Valida.correo(correo, true);
        Valida.obligatorio(direccion, "dirección");
        Valida.curp(curp, true);
        Valida.nombre(altNombre, "El contacto alterno", false);
        Valida.telefono(altTelefono, false);
        String curpLimpia = Valida.t(curp).toUpperCase(java.util.Locale.ROOT);
        if (existeOtro("curp", curpLimpia, id)) {
            throw new ReglaException("Ya existe un cliente registrado con esa CURP.");
        }
        if (existeOtro("correo", Valida.t(correo), id)) {
            throw new ReglaException("Ya existe un cliente registrado con ese correo.");
        }
        if (Db.contar("SELECT COUNT(*) AS v FROM propietario WHERE nombre = ? AND apellidos = ? AND telefono = ? "
                + "AND (? IS NULL OR id <> ?)", Valida.t(nombre), Valida.t(apellidos), Valida.t(telefono), id,
                id) > 0) {
            throw new ReglaException("Ya existe un cliente con el mismo nombre y teléfono.");
        }
        if (id == null) {
            return Db.insertar("INSERT INTO propietario (nombre, apellidos, telefono, correo, direccion, curp, "
                    + "fecha_registro, canal_registro, estado, contacto_alterno_nombre, contacto_alterno_telefono) "
                    + "VALUES (?,?,?,?,?,?,?, 'Presencial', 'Activo', ?, ?)",
                    Valida.t(nombre), Valida.t(apellidos), Valida.t(telefono), Valida.t(correo), Valida.t(direccion),
                    curpLimpia, Config.hoy(), Valida.t(altNombre), Valida.t(altTelefono));
        }
        if (propietario(id) == null) {
            throw new ReglaException("El cliente ya no existe.");
        }
        Db.ejecutar("UPDATE propietario SET nombre=?, apellidos=?, telefono=?, correo=?, direccion=?, curp=?, "
                + "contacto_alterno_nombre=?, contacto_alterno_telefono=? WHERE id=?",
                Valida.t(nombre), Valida.t(apellidos), Valida.t(telefono), Valida.t(correo), Valida.t(direccion),
                curpLimpia, Valida.t(altNombre), Valida.t(altTelefono), id);
        return id;
    }

    /** Citas, cuentas o estancias de las mascotas del cliente: si existen, no se puede borrar sin perder historial. */
    public static boolean tieneHistorialPropietario(long id) {
        return Db.contar("SELECT COUNT(*) AS v FROM cita c JOIN mascota m ON m.id = c.mascota_id "
                + "WHERE m.propietario_id = ?", id)
                + Db.contar("SELECT COUNT(*) AS v FROM cuenta WHERE propietario_id = ?", id)
                + Db.contar("SELECT COUNT(*) AS v FROM estancia e JOIN mascota m ON m.id = e.mascota_id "
                + "WHERE m.propietario_id = ?", id) > 0;
    }

    /**
     * Eliminacion conjunta: borra al cliente junto con sus mascotas (y sus vacunas y fotos), todo en una sola
     * transaccion. Si tiene historial (citas, cuentas o estancias) no se borra nada.
     */
    public static void eliminarPropietario(long id) {
        if (propietario(id) == null) {
            throw new ReglaException("El cliente ya no existe.");
        }
        if (tieneHistorialPropietario(id)) {
            throw new ReglaException("El cliente tiene historial (citas, cuentas o estancias) y no se puede eliminar "
                    + "sin perderlo. Pásalo a Inactivo para conservarlo.");
        }
        List<Row> fotos = Db.consultar("SELECT foto_ruta FROM mascota WHERE propietario_id = ?", id);
        Db.transaccion(() -> {
            Db.ejecutar("DELETE FROM vacuna_cuidado WHERE mascota_id IN (SELECT id FROM mascota "
                    + "WHERE propietario_id = ?)", id);
            Db.ejecutar("DELETE FROM mascota WHERE propietario_id = ?", id);
            Db.ejecutar("DELETE FROM propietario WHERE id = ?", id);
        });
        for (Row f : fotos) {
            ImagenStore.borrar(f.str("foto_ruta"));
        }
    }

    /** Un propietario no se borra: se pasa a Inactivo. */
    public static void cambiarEstadoPropietario(long id, boolean activo) {
        Db.ejecutar("UPDATE propietario SET estado = ? WHERE id = ?", activo ? "Activo" : "Inactivo", id);
    }

    // ---------------------------------------------------------------- mascotas

    public static List<Row> mascotasDe(long propietarioId) {
        return Db.consultar("SELECT * FROM mascota WHERE propietario_id = ? ORDER BY nombre", propietarioId);
    }

    public static List<Row> mascotasActivasDe(long propietarioId) {
        return Db.consultar("SELECT * FROM mascota WHERE propietario_id = ? AND estado = 'Activa' ORDER BY nombre",
                propietarioId);
    }

    /** Mascota con los datos de su propietario. */
    public static Row mascota(long id) {
        return Db.uno("SELECT m.*, p.id AS prop_id, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, "
                + "p.telefono AS prop_telefono, p.correo AS prop_correo, p.direccion AS prop_direccion, "
                + "p.contacto_alterno_nombre AS alt_nombre, p.contacto_alterno_telefono AS alt_telefono "
                + "FROM mascota m JOIN propietario p ON p.id = m.propietario_id WHERE m.id = ?", id);
    }

    public static boolean tieneHistorialMascota(long id) {
        return Db.contar("SELECT COUNT(*) AS v FROM cita WHERE mascota_id = ?", id)
                + Db.contar("SELECT COUNT(*) AS v FROM cuenta_detalle WHERE mascota_id = ?", id)
                + Db.contar("SELECT COUNT(*) AS v FROM estancia WHERE mascota_id = ?", id) > 0;
    }

    public static String fotoRutaMascota(long id) {
        Row r = Db.uno("SELECT foto_ruta FROM mascota WHERE id = ?", id);
        return r == null ? "" : r.str("foto_ruta");
    }

    /** Foto de la mascota (archivo en Documentos/VetPlus/imagenes) o null si no tiene o ya no existe. */
    public static byte[] fotoMascota(long id) {
        return ImagenStore.leer(fotoRutaMascota(id));
    }

    /**
     * Alta o edicion de mascota ligada a un propietario. La foto (opcional) se guarda como archivo: primero se
     * escribe el archivo y despues la base de datos; si la base falla se borra el archivo nuevo y todo queda como estaba.
     */
    public static long guardarMascota(Long id, long propietarioId, String nombre, String especie, String raza,
            String sexo, LocalDate nacimiento, Double peso, boolean esterilizada, String microchip,
            String alimentacion, String antecedentes, String notaEstetica, byte[] fotoNueva, boolean quitarFoto) {
        Valida.nombre(nombre, "El nombre de la mascota", true);
        Valida.obligatorio(especie, "especie");
        Valida.obligatorio(sexo, "sexo");
        Valida.fechaNoFutura(nacimiento, "La fecha de nacimiento");
        if (peso != null && (peso < 0 || peso > 500)) {
            throw new ReglaException("El peso debe estar entre 0 y 500 kg.");
        }
        if (propietario(propietarioId) == null) {
            throw new ReglaException("El propietario de la mascota no existe.");
        }
        if (id != null && mascota(id) == null) {
            throw new ReglaException("La mascota ya no existe.");
        }
        if (Db.contar("SELECT COUNT(*) AS v FROM mascota WHERE propietario_id = ? AND nombre = ? AND especie = ? "
                + "AND (? IS NULL OR id <> ?)", propietarioId, Valida.t(nombre), especie, id, id) > 0) {
            throw new ReglaException("Este cliente ya tiene una mascota con ese nombre y especie.");
        }
        String nac = nacimiento == null ? null : nacimiento.toString();
        String anterior = id == null ? "" : fotoRutaMascota(id);
        String archivoNuevo = fotoNueva == null ? null : ImagenStore.guardar(fotoNueva, "mascota");
        String rutaFinal = archivoNuevo != null ? archivoNuevo : (quitarFoto ? "" : anterior);
        try {
            long resultado = Db.transaccion(() -> {
                if (id == null) {
                    return Db.insertar("INSERT INTO mascota (propietario_id, nombre, especie, raza, sexo, "
                            + "fecha_nacimiento, peso, esterilizada, microchip, alimentacion, antecedentes, "
                            + "nota_estetica, foto_ruta, estado) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?, 'Activa')",
                            propietarioId, Valida.t(nombre), especie, Valida.t(raza), sexo, nac, peso, esterilizada,
                            Valida.t(microchip), Valida.t(alimentacion), Valida.t(antecedentes),
                            Valida.t(notaEstetica), rutaFinal);
                }
                Db.ejecutar("UPDATE mascota SET nombre=?, especie=?, raza=?, sexo=?, fecha_nacimiento=?, peso=?, "
                        + "esterilizada=?, microchip=?, alimentacion=?, antecedentes=?, nota_estetica=?, foto_ruta=? "
                        + "WHERE id=?",
                        Valida.t(nombre), especie, Valida.t(raza), sexo, nac, peso, esterilizada,
                        Valida.t(microchip), Valida.t(alimentacion), Valida.t(antecedentes),
                        Valida.t(notaEstetica), rutaFinal, id);
                return id;
            });
            if (!anterior.isEmpty() && !anterior.equals(rutaFinal)) {
                ImagenStore.borrar(anterior);
            }
            return resultado;
        } catch (RuntimeException e) {
            ImagenStore.borrar(archivoNuevo);
            throw e;
        }
    }

    /** Pasa la mascota a Activa o Inactiva (conserva su historial). */
    public static void cambiarEstadoMascota(long id, boolean activa) {
        Db.ejecutar("UPDATE mascota SET estado = ? WHERE id = ?", activa ? "Activa" : "Inactiva", id);
    }

    /** Elimina la mascota (y sus vacunas y su foto) solo si no tiene citas, cuentas ni estancias. */
    public static void eliminarMascota(long id) {
        if (mascota(id) == null) {
            throw new ReglaException("La mascota ya no existe.");
        }
        if (tieneHistorialMascota(id)) {
            throw new ReglaException("La mascota tiene citas, cuentas o estancias registradas y no se puede eliminar "
                    + "sin perder ese historial. Puedes pasarla a Inactiva.");
        }
        String foto = fotoRutaMascota(id);
        Db.transaccion(() -> {
            Db.ejecutar("DELETE FROM vacuna_cuidado WHERE mascota_id = ?", id);
            Db.ejecutar("DELETE FROM mascota WHERE id = ?", id);
        });
        ImagenStore.borrar(foto);
    }

    public static void actualizarPesoMascota(long id, Double peso) {
        if (peso != null && peso > 0) {
            Db.ejecutar("UPDATE mascota SET peso = ? WHERE id = ?", peso, id);
        }
    }

    // ---------------------------------------------------------------- vacunas y cuidados

    public static List<Row> cuidados(long mascotaId) {
        return Db.consultar("SELECT * FROM vacuna_cuidado WHERE mascota_id = ? ORDER BY cuidado", mascotaId);
    }

    /** Al dia / Proxima (30 dias o menos) / Vencida. */
    public static String estadoCuidado(String proximaFecha) {
        LocalDate p = Fmt.fecha(proximaFecha);
        if (p == null) {
            return "Al día";
        }
        LocalDate hoy = LocalDate.now();
        if (p.isBefore(hoy)) {
            return "Vencida";
        }
        if (!p.isAfter(hoy.plusDays(30))) {
            return "Próxima";
        }
        return "Al día";
    }

    /** Crea o actualiza el registro de un cuidado (vacuna, desparasitacion) de una mascota. */
    public static void registrarCuidado(long mascotaId, String cuidado, LocalDate aplicada, LocalDate proxima,
            String aplicadoPor) {
        Row existente = Db.uno("SELECT id FROM vacuna_cuidado WHERE mascota_id = ? AND cuidado = ?", mascotaId,
                cuidado);
        String prox = proxima == null ? null : proxima.toString();
        if (existente == null) {
            Db.insertar("INSERT INTO vacuna_cuidado (mascota_id, cuidado, ultima_aplicacion, proxima_fecha, "
                    + "aplicado_por) VALUES (?,?,?,?,?)", mascotaId, cuidado, aplicada.toString(), prox, aplicadoPor);
        } else {
            Db.ejecutar("UPDATE vacuna_cuidado SET ultima_aplicacion=?, proxima_fecha=?, aplicado_por=? WHERE id=?",
                    aplicada.toString(), prox, aplicadoPor, existente.id());
        }
    }

    // ---------------------------------------------------------------- historial

    /** Historial clinico completo (administracion y medicos). Mas reciente primero. */
    public static List<Row> historialClinico(long mascotaId) {
        return Db.consultar("SELECT c.id, c.fecha, c.hora_inicio, c.estado, s.nombre AS servicio, s.tipo, "
                + "TRIM(CONCAT(u.nombre, ' ', u.apellidos)) AS profesional, "
                + "am.exploracion_diagnostico AS diagnostico, am.tratamiento AS tratamiento, "
                + "am.estado AS estado_nota, se.observaciones AS observaciones "
                + "FROM cita c JOIN servicio s ON s.id = c.servicio_id JOIN usuario u ON u.id = c.profesional_id "
                + "LEFT JOIN atencion_medica am ON am.cita_id = c.id "
                + "LEFT JOIN servicio_estetica se ON se.cita_id = c.id "
                + "WHERE c.mascota_id = ? AND c.estado <> 'Cancelada' ORDER BY c.fecha DESC, c.hora_inicio DESC",
                mascotaId);
    }

    /** Historial de estetica para la estilista. Nunca toca tablas ni columnas medicas. */
    public static List<Row> historialEstetica(long mascotaId) {
        return Db.consultar("SELECT c.id, c.fecha, c.hora_inicio, c.estado, s.nombre AS servicio, "
                + "TRIM(CONCAT(u.nombre, ' ', u.apellidos)) AS profesional, se.observaciones AS observaciones "
                + "FROM cita c JOIN servicio s ON s.id = c.servicio_id JOIN usuario u ON u.id = c.profesional_id "
                + "LEFT JOIN servicio_estetica se ON se.cita_id = c.id "
                + "WHERE c.mascota_id = ? AND s.tipo = 'ESTETICA' AND c.estado <> 'Cancelada' "
                + "ORDER BY c.fecha DESC, c.hora_inicio DESC", mascotaId);
    }

    // ---------------------------------------------------------------- pacientes (vista medica)

    public static List<Row> pacientes() {
        return Db.consultar("SELECT m.*, p.id AS prop_id, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, "
                + "p.telefono AS prop_telefono FROM mascota m JOIN propietario p ON p.id = m.propietario_id "
                + "ORDER BY m.nombre");
    }

    public static boolean tieneAvisoClinico(Row mascota) {
        return !mascota.str("antecedentes").isEmpty();
    }
}
