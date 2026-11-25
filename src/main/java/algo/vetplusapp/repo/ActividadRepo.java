package algo.vetplusapp.repo;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.util.Config;
import java.time.LocalTime;
import java.util.List;

public final class ActividadRepo {

    private ActividadRepo() {
    }

    /** Guarda un movimiento. El titulo queda como "09:45 · Pago recibido". */
    public static void registrar(String tipo, String titulo, String detalle) {
        String hora = LocalTime.now().format(algo.vetplusapp.util.Fmt.HORA);
        Db.ejecutar("INSERT INTO actividad (fecha_hora, tipo, titulo, detalle) VALUES (?,?,?,?)",
                Config.ahora(), tipo, hora + " · " + titulo, detalle);
    }

    public static List<Row> deHoy(int limite) {
        return Db.consultar("SELECT titulo, detalle FROM actividad WHERE substr(fecha_hora,1,10) = ? "
                + "ORDER BY id DESC LIMIT ?", Config.hoy(), limite);
    }
}
