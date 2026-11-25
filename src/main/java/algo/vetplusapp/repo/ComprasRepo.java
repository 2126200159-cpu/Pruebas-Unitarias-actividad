package algo.vetplusapp.repo;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Sesion;
import algo.vetplusapp.util.Valida;
import java.time.LocalDate;
import java.util.List;

/** Insumos, proveedores y ordenes de compra (3.8). */
public final class ComprasRepo {

    private ComprasRepo() {
    }

    /** Renglon de una orden en edicion. */
    public static final class RenglonOrden {

        public final long insumoId;
        public final String nombre;
        public final double cantidad;
        public final double precio;

        public RenglonOrden(long insumoId, String nombre, double cantidad, double precio) {
            this.insumoId = insumoId;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precio = precio;
        }

        public double importe() {
            return Fmt.redondear(cantidad * precio);
        }
    }

    // ------------------------------------------------------------ insumos y proveedores

    public static List<Row> insumos() {
        return Db.consultar("SELECT * FROM insumo ORDER BY nombre");
    }

    /** Bajo el minimo: existencia estrictamente menor al minimo (criterio unico en todo el sistema). */
    public static List<Row> bajoMinimo() {
        return Db.consultar("SELECT * FROM insumo WHERE existencia < minimo ORDER BY nombre");
    }

    public static long guardarInsumo(Long id, String nombre, String categoria, double existencia, double minimo,
            double precio) {
        Valida.obligatorio(nombre, "nombre del insumo");
        if (existencia < 0 || minimo < 0 || precio < 0) {
            throw new ReglaException("Existencia, mínimo y precio no pueden ser negativos.");
        }
        Valida.costo(precio, "El precio");
        if (id == null) {
            return Db.insertar("INSERT INTO insumo (nombre, categoria, existencia, minimo, precio_compra) "
                    + "VALUES (?,?,?,?,?)", Valida.t(nombre), Valida.t(categoria), existencia, minimo, precio);
        }
        Db.ejecutar("UPDATE insumo SET nombre=?, categoria=?, existencia=?, minimo=?, precio_compra=? WHERE id=?",
                Valida.t(nombre), Valida.t(categoria), existencia, minimo, precio, id);
        return id;
    }

    public static List<Row> proveedores() {
        return Db.consultar("SELECT * FROM proveedor ORDER BY empresa");
    }

    public static Row proveedor(long id) {
        return Db.uno("SELECT * FROM proveedor WHERE id = ?", id);
    }

    public static long guardarProveedor(Long id, String empresa, String contacto, String telefono, String correo) {
        Valida.obligatorio(empresa, "empresa");
        Valida.nombre(contacto, "El contacto", false);
        Valida.telefono(telefono, false);
        Valida.correo(correo, true);
        if (id == null) {
            return Db.insertar("INSERT INTO proveedor (empresa, contacto, telefono, correo) VALUES (?,?,?,?)",
                    Valida.t(empresa), Valida.t(contacto), Valida.t(telefono), Valida.t(correo));
        }
        Db.ejecutar("UPDATE proveedor SET empresa=?, contacto=?, telefono=?, correo=? WHERE id=?",
                Valida.t(empresa), Valida.t(contacto), Valida.t(telefono), Valida.t(correo), id);
        return id;
    }

    // ------------------------------------------------------------ ordenes de compra

    /** Siguiente folio: OC-001, OC-002... (sobre el mayor numero usado). */
    public static String siguienteFolio() {
        int max = 0;
        for (Row r : Db.consultar("SELECT folio FROM orden_compra")) {
            String f = r.str("folio");
            try {
                max = Math.max(max, Integer.parseInt(f.substring(3)));
            } catch (RuntimeException e) {
                // folio con otro formato: se ignora
            }
        }
        return String.format("OC-%03d", max + 1);
    }

    public static List<Row> ordenes() {
        return Db.consultar("SELECT o.id, o.folio, o.estado, o.total, o.fecha, o.fecha_entrega, "
                + "COALESCE(p.empresa, '—') AS proveedor FROM orden_compra o "
                + "LEFT JOIN proveedor p ON p.id = o.proveedor_id ORDER BY o.id DESC");
    }

    /**
     * Guarda la orden. Borrador: no exige que este completa. Emitida: exige proveedor, fecha de entrega y
     * al menos un renglon. Devuelve el id.
     */
    public static long guardarOrden(String folio, Long proveedorId, LocalDate entrega, String notas,
            List<RenglonOrden> renglones, boolean emitir) {
        if (emitir) {
            if (proveedorId == null) {
                throw new ReglaException("Elige el proveedor.");
            }
            if (entrega == null) {
                throw new ReglaException("Captura la fecha de entrega prevista.");
            }
            if (renglones.isEmpty()) {
                throw new ReglaException("Agrega al menos un producto a la orden.");
            }
        }
        Valida.fechaNoPasada(entrega, "La fecha de entrega");
        double total = 0;
        for (RenglonOrden r : renglones) {
            if (r.cantidad <= 0) {
                throw new ReglaException("La cantidad de «" + r.nombre + "» debe ser mayor a 0.");
            }
            Valida.costo(r.precio, "El precio de «" + r.nombre + "»");
            total += r.importe();
        }
        final double totalFinal = Fmt.redondear(total);
        return Db.transaccion(() -> {
            long id = Db.insertar("INSERT INTO orden_compra (folio, proveedor_id, creada_por, fecha, fecha_entrega, "
                    + "estado, notas, total) VALUES (?,?,?,?,?,?,?,?)", folio, proveedorId,
                    Sesion.usuarioId == 0 ? null : Sesion.usuarioId, Config.hoy(),
                    entrega == null ? null : entrega.toString(), emitir ? "EMITIDA" : "BORRADOR",
                    notas == null ? "" : notas.trim(), totalFinal);
            for (RenglonOrden r : renglones) {
                Db.insertar("INSERT INTO orden_compra_renglon (orden_id, insumo_id, cantidad, precio_unitario) "
                        + "VALUES (?,?,?,?)", id, r.insumoId, r.cantidad, r.precio);
            }
            return id;
        });
    }

    /** Registrar la recepcion: pasa a Recibida y SUMA las cantidades a las existencias. */
    public static void recibir(long ordenId) {
        Row o = Db.uno("SELECT estado FROM orden_compra WHERE id = ?", ordenId);
        if (o == null || !"EMITIDA".equals(o.str("estado"))) {
            throw new ReglaException("Solo se puede recibir una orden Emitida.");
        }
        Db.transaccion(() -> {
            for (Row r : Db.consultar("SELECT insumo_id, cantidad FROM orden_compra_renglon WHERE orden_id = ?",
                    ordenId)) {
                Db.ejecutar("UPDATE insumo SET existencia = existencia + ? WHERE id = ?", r.dbl("cantidad"),
                        r.lng("insumo_id"));
            }
            Db.ejecutar("UPDATE orden_compra SET estado = 'RECIBIDA' WHERE id = ?", ordenId);
        });
    }
}
