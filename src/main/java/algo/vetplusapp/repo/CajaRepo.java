package algo.vetplusapp.repo;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import java.util.List;

/** Cuentas, descuento familiar, cobros y cierre de caja (3.7). */
public final class CajaRepo {

    private CajaRepo() {
    }

    /** Renglon de una cuenta nueva armada a mano. */
    public static final class Renglon {

        public final long mascotaId;
        public final long servicioId;
        public final int cantidad;

        public Renglon(long mascotaId, long servicioId, int cantidad) {
            this.mascotaId = mascotaId;
            this.servicioId = servicioId;
            this.cantidad = cantidad;
        }
    }

    /** Agrega un servicio a la cuenta pendiente de hoy del propietario (la crea si no existe). */
    public static long agregarRenglon(long propietarioId, long mascotaId, long servicioId, Long citaId, int cantidad) {
        return Db.transaccion(() -> {
            Row cuenta = Db.uno("SELECT id FROM cuenta WHERE propietario_id = ? AND estado = 'PENDIENTE' "
                    + "AND fecha = ? ORDER BY id LIMIT 1", propietarioId, Config.hoy());
            long cuentaId = cuenta != null ? cuenta.id()
                    : Db.insertar("INSERT INTO cuenta (propietario_id, fecha, estado) VALUES (?,?, 'PENDIENTE')",
                            propietarioId, Config.hoy());
            Row servicio = ServicioRepo.porId(servicioId);
            // se copia el precio del momento: si la tarifa cambia, esta cuenta no cambia
            Db.insertar("INSERT INTO cuenta_detalle (cuenta_id, mascota_id, servicio_id, cita_id, cantidad, "
                    + "precio_unitario) VALUES (?,?,?,?,?,?)",
                    cuentaId, mascotaId, servicioId, citaId, cantidad, servicio.dbl("precio"));
            recalcular(cuentaId);
            return cuentaId;
        });
    }

    /** Subtotal, tasa unica segun mascotas distintas, descuento y total (redondeados a 2 decimales). */
    public static void recalcular(long cuentaId) {
        double subtotal = Db.suma("SELECT SUM(cantidad * precio_unitario) AS v FROM cuenta_detalle "
                + "WHERE cuenta_id = ?", cuentaId);
        int mascotas = Db.contar("SELECT COUNT(DISTINCT mascota_id) AS v FROM cuenta_detalle WHERE cuenta_id = ?",
                cuentaId);
        int pct = Config.tasaDescuento(mascotas);
        double descuento = Fmt.redondear(subtotal * pct / 100.0);
        double total = Fmt.redondear(subtotal - descuento);
        Db.ejecutar("UPDATE cuenta SET subtotal = ?, descuento_pct = ?, descuento_monto = ?, total = ? WHERE id = ?",
                Fmt.redondear(subtotal), pct, descuento, total, cuentaId);
    }

    public static long crearCuentaManual(long propietarioId, List<Renglon> renglones) {
        if (renglones.isEmpty()) {
            throw new ReglaException("Agrega al menos un servicio a la cuenta.");
        }
        return Db.transaccion(() -> {
            long id = Db.insertar("INSERT INTO cuenta (propietario_id, fecha, estado) VALUES (?,?, 'PENDIENTE')",
                    propietarioId, Config.hoy());
            for (Renglon r : renglones) {
                Row s = ServicioRepo.porId(r.servicioId);
                Db.insertar("INSERT INTO cuenta_detalle (cuenta_id, mascota_id, servicio_id, cita_id, cantidad, "
                        + "precio_unitario) VALUES (?,?,?,NULL,?,?)",
                        id, r.mascotaId, r.servicioId, r.cantidad, s.dbl("precio"));
            }
            recalcular(id);
            return id;
        });
    }

    public static Row cuenta(long cuentaId) {
        return Db.uno("SELECT c.*, p.id AS prop_id, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, "
                + "p.correo AS prop_correo, "
                + "(SELECT COUNT(DISTINCT d.mascota_id) FROM cuenta_detalle d WHERE d.cuenta_id = c.id) AS n_mascotas, "
                + "(SELECT COUNT(*) FROM cuenta_detalle d WHERE d.cuenta_id = c.id) AS n_servicios "
                + "FROM cuenta c JOIN propietario p ON p.id = c.propietario_id WHERE c.id = ?", cuentaId);
    }

    public static List<Row> detalle(long cuentaId) {
        return Db.consultar("SELECT d.id, m.nombre AS mascota, s.nombre AS servicio, d.cantidad, d.precio_unitario, "
                + "d.cantidad * d.precio_unitario AS importe FROM cuenta_detalle d "
                + "JOIN mascota m ON m.id = d.mascota_id JOIN servicio s ON s.id = d.servicio_id "
                + "WHERE d.cuenta_id = ? ORDER BY d.id", cuentaId);
    }

    /** Nombres de las mascotas distintas de la cuenta, en orden. */
    public static List<Row> mascotasDeCuenta(long cuentaId) {
        return Db.consultar("SELECT m.nombre AS nombre FROM cuenta_detalle d "
                + "JOIN mascota m ON m.id = d.mascota_id WHERE d.cuenta_id = ? GROUP BY m.id, m.nombre "
                + "ORDER BY MIN(d.id)", cuentaId);
    }

    public static Row cuentaPendienteDe(long propietarioId) {
        return Db.uno("SELECT id FROM cuenta WHERE propietario_id = ? AND estado = 'PENDIENTE' "
                + "ORDER BY fecha DESC, id DESC LIMIT 1", propietarioId);
    }

    public static List<Row> pendientes() {
        return Db.consultar("SELECT c.id, c.fecha, c.total, "
                + "c.total - COALESCE((SELECT SUM(x.importe) FROM pago x WHERE x.cuenta_id = c.id), 0) AS saldo, "
                + "TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, "
                + "(SELECT GROUP_CONCAT(s.nombre SEPARATOR ', ') FROM cuenta_detalle d JOIN servicio s ON s.id = d.servicio_id "
                + " WHERE d.cuenta_id = c.id) AS concepto "
                + "FROM cuenta c JOIN propietario p ON p.id = c.propietario_id "
                + "WHERE c.estado = 'PENDIENTE' ORDER BY c.fecha DESC, c.id DESC");
    }

    public static List<Row> pagosDelDia(String fecha) {
        return Db.consultar("SELECT g.id, g.fecha_hora, g.metodo, g.importe, "
                + "TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario FROM pago g "
                + "JOIN cuenta c ON c.id = g.cuenta_id JOIN propietario p ON p.id = c.propietario_id "
                + "WHERE substr(g.fecha_hora, 1, 10) = ? ORDER BY g.fecha_hora DESC, g.id DESC", fecha);
    }

    public static double cobradoDelDia(String fecha) {
        return Db.suma("SELECT SUM(importe) AS v FROM pago WHERE substr(fecha_hora, 1, 10) = ?", fecha);
    }

    public static int pacientesConCobro(String fecha) {
        return Db.contar("SELECT COUNT(DISTINCT d.mascota_id) AS v FROM cuenta_detalle d "
                + "WHERE d.cuenta_id IN (SELECT cuenta_id FROM pago WHERE substr(fecha_hora, 1, 10) = ?)", fecha);
    }

    public static int cuentasPagadas(String fecha) {
        return Db.contar("SELECT COUNT(DISTINCT cuenta_id) AS v FROM pago WHERE substr(fecha_hora, 1, 10) = ?",
                fecha);
    }

    /** Confirma el cobro: valida, guarda el pago y pasa la cuenta a Pagada. */
    public static void cobrar(long cuentaId, String metodoTexto, double importe, String referencia, String correo) {
        Row cuenta = cuenta(cuentaId);
        if (cuenta == null || !"PENDIENTE".equals(cuenta.str("estado"))) {
            throw new ReglaException("Esta cuenta ya no está pendiente de pago.");
        }
        if (metodoTexto == null || metodoTexto.isEmpty()) {
            throw new ReglaException("Elige el método de pago.");
        }
        String metodo = metodoTexto.toUpperCase(java.util.Locale.ROOT);
        double total = cuenta.dbl("total");
        if (importe + 0.005 < total) {
            throw new ReglaException("El importe no puede ser menor al total de la cuenta (" + Fmt.dinero(total)
                    + "). Se cobra siempre el total.");
        }
        if (!"EFECTIVO".equals(metodo) && (referencia == null || referencia.trim().isEmpty())) {
            throw new ReglaException("Captura la referencia (autorización de la terminal o folio de transferencia).");
        }
        Db.transaccion(() -> {
            Db.insertar("INSERT INTO pago (cuenta_id, fecha_hora, metodo, importe, referencia, correo_comprobante) "
                    + "VALUES (?,?,?,?,?,?)", cuentaId, Config.ahora(), metodo, total,
                    referencia == null ? "" : referencia.trim(), correo == null ? "" : correo.trim());
            Db.ejecutar("UPDATE cuenta SET estado = 'PAGADA' WHERE id = ?", cuentaId);
            ActividadRepo.registrar("PAGO", "Pago recibido", cuenta.str("propietario") + " · "
                    + Fmt.dinero(total));
        });
    }
}
