package algo.vetplusapp.repo;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Row;
import java.util.List;

/** Consultas de reportes e indicadores del Resumen. */
public final class ReporteRepo {

    private static final String LINEAS = "FROM pago g JOIN cuenta c ON c.id = g.cuenta_id "
            + "JOIN cuenta_detalle d ON d.cuenta_id = c.id JOIN mascota m ON m.id = d.mascota_id "
            + "JOIN propietario p ON p.id = m.propietario_id JOIN servicio s ON s.id = d.servicio_id "
            + "WHERE substr(g.fecha_hora, 1, 10) = ? ";
    /** Cobro de un renglon con el descuento familiar ya aplicado (asi suman el total cobrado). */
    private static final String COBRO = "ROUND(d.cantidad * d.precio_unitario * (100 - c.descuento_pct) / 100.0, 2)";

    private ReporteRepo() {
    }

    public static List<Row> pacientesDelDia(String fecha) {
        return Db.consultar("SELECT m.nombre AS paciente, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, "
                + "s.nombre AS servicio, g.metodo AS metodo, " + COBRO + " AS cobro " + LINEAS
                + "ORDER BY g.fecha_hora, d.id", fecha);
    }

    /** Por servicio: nombre, pacientes distintos e importe cobrado. */
    public static List<Row> porServicio(String fecha) {
        return Db.consultar("SELECT s.nombre AS servicio, COUNT(DISTINCT d.mascota_id) AS pacientes, "
                + "SUM(" + COBRO + ") AS importe " + LINEAS + "GROUP BY s.id ORDER BY importe DESC", fecha);
    }

    /** Mascotas cobradas distintas por especie. */
    public static List<Row> porEspecie(String fecha) {
        return Db.consultar("SELECT m.especie AS especie, COUNT(DISTINCT m.id) AS n " + LINEAS
                + "GROUP BY m.especie ORDER BY n DESC", fecha);
    }

    /** Cuenta pendiente con mayor descuento familiar. */
    public static Row mayorDescuentoPendiente() {
        return Db.uno("SELECT c.id, c.descuento_pct, c.descuento_monto, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, "
                + "(SELECT COUNT(DISTINCT d.mascota_id) FROM cuenta_detalle d WHERE d.cuenta_id = c.id) AS n_mascotas "
                + "FROM cuenta c JOIN propietario p ON p.id = c.propietario_id "
                + "WHERE c.estado = 'PENDIENTE' AND c.descuento_pct > 0 ORDER BY c.descuento_monto DESC, c.id LIMIT 1");
    }

    /** Cuenta pendiente de hoy con 2 o mas mascotas distintas (aviso del Resumen). */
    public static Row avisoDescuentoHoy(String hoy) {
        return Db.uno("SELECT c.id, c.descuento_pct, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, "
                + "(SELECT COUNT(DISTINCT d.mascota_id) FROM cuenta_detalle d WHERE d.cuenta_id = c.id) AS n_mascotas "
                + "FROM cuenta c JOIN propietario p ON p.id = c.propietario_id "
                + "WHERE c.estado = 'PENDIENTE' AND c.fecha = ? AND "
                + "(SELECT COUNT(DISTINCT d.mascota_id) FROM cuenta_detalle d WHERE d.cuenta_id = c.id) >= 2 "
                + "ORDER BY c.descuento_pct DESC, c.id LIMIT 1", hoy);
    }

    public static int citasDelDia(String hoy) {
        return Db.contar("SELECT COUNT(*) AS v FROM cita WHERE fecha = ? AND estado <> 'Cancelada'", hoy);
    }

    public static int pacientesDelDiaConCita(String hoy) {
        return Db.contar("SELECT COUNT(DISTINCT mascota_id) AS v FROM cita WHERE fecha = ? AND estado <> 'Cancelada'",
                hoy);
    }

    public static int citasConEstado(String hoy, String... estados) {
        int n = 0;
        for (String e : estados) {
            n += Db.contar("SELECT COUNT(*) AS v FROM cita WHERE fecha = ? AND estado = ?", hoy, e);
        }
        return n;
    }

    public static int pacientesAtendidos(String hoy) {
        return Db.contar("SELECT COUNT(DISTINCT mascota_id) AS v FROM cita WHERE fecha = ? AND estado = 'Finalizada'",
                hoy);
    }

    public static int estanciasActivas(String hoy) {
        return Db.contar("SELECT COUNT(*) AS v FROM estancia WHERE estado = 'ACTIVA' AND fecha_entrada <= ?", hoy);
    }

    public static int salidasHoy(String hoy) {
        return Db.contar("SELECT COUNT(*) AS v FROM estancia WHERE estado = 'ACTIVA' AND fecha_salida_prevista = ?",
                hoy);
    }

    /** Propietarios con movimiento (cita o pago) en la fecha: para el reporte "Contacto del cliente". */
    public static List<Row> contactosDelDia(String fecha) {
        return Db.consultar("SELECT p.id, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, p.telefono AS telefono, "
                + "p.correo AS correo, (SELECT GROUP_CONCAT(m.nombre SEPARATOR ', ') FROM mascota m "
                + " WHERE m.propietario_id = p.id) AS mascotas FROM propietario p WHERE p.id IN ("
                + "SELECT m.propietario_id FROM cita c JOIN mascota m ON m.id = c.mascota_id "
                + "WHERE c.fecha = ? AND c.estado <> 'Cancelada' "
                + "UNION SELECT c.propietario_id FROM pago g JOIN cuenta c ON c.id = g.cuenta_id "
                + "WHERE substr(g.fecha_hora, 1, 10) = ?) ORDER BY propietario", fecha, fecha);
    }

    /** Cuentas de la fecha (pagadas o pendientes) con descuento familiar. */
    public static List<Row> clientesConDescuento(String fecha) {
        return Db.consultar("SELECT TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, c.descuento_pct AS pct, "
                + "c.descuento_monto AS ahorro, c.estado AS estado, "
                + "(SELECT COUNT(DISTINCT d.mascota_id) FROM cuenta_detalle d WHERE d.cuenta_id = c.id) AS n_mascotas "
                + "FROM cuenta c JOIN propietario p ON p.id = c.propietario_id "
                + "WHERE c.descuento_pct > 0 AND c.fecha = ? ORDER BY c.descuento_monto DESC", fecha);
    }

    /** Contacto del propietario de una mascota en pension activa (para el resumen de Reportes). */
    public static Row contactoDeEstancia() {
        return Db.uno("SELECT TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, p.telefono AS telefono, "
                + "p.correo AS correo FROM estancia e JOIN mascota m ON m.id = e.mascota_id "
                + "JOIN propietario p ON p.id = m.propietario_id WHERE e.estado = 'ACTIVA' "
                + "ORDER BY e.fecha_salida_prevista LIMIT 1");
    }
}
