package algo.vetplusapp.repo;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.util.Valida;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Catalogo de servicios: 6 servicios base (no se borran) y los que agregue administracion. */
public final class ServicioRepo {

    public static final Set<String> BASE = new HashSet<>(Arrays.asList("Consulta", "Vacunación",
            "Desparasitación", "Cirugía", "Estética", "Pensión"));

    private ServicioRepo() {
    }

    public static List<Row> todos() {
        return Db.consultar("SELECT * FROM servicio ORDER BY id");
    }

    public static List<Row> activos() {
        return Db.consultar("SELECT * FROM servicio WHERE activo = 1 ORDER BY id");
    }

    public static List<Row> activosPorTipo(String tipo) {
        return Db.consultar("SELECT * FROM servicio WHERE activo = 1 AND tipo = ? ORDER BY id", tipo);
    }

    public static Row porId(long id) {
        return Db.uno("SELECT * FROM servicio WHERE id = ?", id);
    }

    public static Row porNombre(String nombre) {
        return Db.uno("SELECT * FROM servicio WHERE nombre = ?", nombre);
    }

    public static boolean esBase(String nombre) {
        return BASE.contains(nombre);
    }

    /** Alta de un servicio nuevo (medico o de estetica). Precio y tiempo estimado son obligatorios. */
    public static long crear(String nombre, String descripcion, String tipo, double precio, Integer duracion) {
        String n = Valida.t(nombre);
        Valida.obligatorio(n, "nombre del servicio");
        if (n.length() > 60) {
            throw new ReglaException("El nombre del servicio no puede pasar de 60 caracteres.");
        }
        if (!"MEDICO".equals(tipo) && !"ESTETICA".equals(tipo)) {
            throw new ReglaException("El tipo del servicio debe ser Médico o Estética.");
        }
        Valida.costo(precio, "El precio");
        Valida.duracion(duracion);
        if (Db.contar("SELECT COUNT(*) AS v FROM servicio WHERE nombre = ?", n) > 0) {
            throw new ReglaException("Ya existe un servicio con ese nombre.");
        }
        return Db.insertar("INSERT INTO servicio (nombre, descripcion, tipo, precio, duracion_min, activo) "
                + "VALUES (?, ?, ?, ?, ?, 1)", n, Valida.t(descripcion), tipo, precio, duracion);
    }

    /** Cambia tarifa y duracion. Las cuentas ya creadas conservan su precio_unitario (3.2). */
    public static void actualizarTarifa(long id, double precio, Integer duracion) {
        Row s = porId(id);
        if (s == null) {
            throw new ReglaException("El servicio ya no existe.");
        }
        Valida.costo(precio, "El precio");
        boolean pension = "PENSION".equals(s.str("tipo"));
        if (!pension) {
            Valida.duracion(duracion);
        }
        Db.ejecutar("UPDATE servicio SET precio = ?, duracion_min = ? WHERE id = ?", precio,
                pension ? null : duracion, id);
    }

    /** Cambia nombre y descripcion. Los servicios base conservan su nombre (el sistema los usa por nombre). */
    public static void actualizarDatos(long id, String nombre, String descripcion) {
        Row s = porId(id);
        if (s == null) {
            throw new ReglaException("El servicio ya no existe.");
        }
        String n = Valida.t(nombre);
        Valida.obligatorio(n, "nombre del servicio");
        if (!n.equals(s.str("nombre")) && esBase(s.str("nombre"))) {
            throw new ReglaException("Los servicios base no se pueden renombrar; puedes cambiar su precio y duración.");
        }
        Row otro = Db.uno("SELECT id FROM servicio WHERE nombre = ?", n);
        if (otro != null && otro.id() != id) {
            throw new ReglaException("Ya existe un servicio con ese nombre.");
        }
        Db.ejecutar("UPDATE servicio SET nombre = ?, descripcion = ? WHERE id = ?", n, Valida.t(descripcion), id);
    }

    public static void cambiarActivo(long id, boolean activo) {
        Db.ejecutar("UPDATE servicio SET activo = ? WHERE id = ?", activo, id);
    }

    /** Citas, cuentas o notas que usan el servicio. */
    public static boolean tieneRelaciones(long id) {
        return Db.contar("SELECT COUNT(*) AS v FROM cita WHERE servicio_id = ?", id)
                + Db.contar("SELECT COUNT(*) AS v FROM cuenta_detalle WHERE servicio_id = ?", id)
                + Db.contar("SELECT COUNT(*) AS v FROM atencion_medica WHERE servicio_registrado_id = ?", id) > 0;
    }

    public static void eliminar(long id) {
        Row s = porId(id);
        if (s == null) {
            throw new ReglaException("El servicio ya no existe.");
        }
        if (esBase(s.str("nombre"))) {
            throw new ReglaException("«" + s.str("nombre") + "» es un servicio base del sistema y no se puede "
                    + "eliminar; si ya no lo ofreces, desactívalo.");
        }
        if (tieneRelaciones(id)) {
            throw new ReglaException("El servicio tiene citas o cuentas registradas y no se puede eliminar sin perder "
                    + "ese historial. Puedes desactivarlo.");
        }
        Db.ejecutar("DELETE FROM servicio WHERE id = ?", id);
    }
}
