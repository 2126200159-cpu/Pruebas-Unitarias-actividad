package algo.vetplusapp.db;

import java.util.LinkedHashMap;
import java.util.Map;

/** Una fila de resultado: columna -> valor. Evita una clase modelo por cada tabla. */
public class Row {

    private final Map<String, Object> datos = new LinkedHashMap<>();

    public void put(String columna, Object valor) {
        datos.put(columna.toLowerCase(), valor);
    }

    public Object obj(String k) {
        return datos.get(k.toLowerCase());
    }

    /** Texto; si es nulo devuelve cadena vacia. */
    public String str(String k) {
        Object o = obj(k);
        return o == null ? "" : o.toString();
    }

    public boolean esNulo(String k) {
        return obj(k) == null;
    }

    private static Number num(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof Boolean) {
            return ((Boolean) o) ? 1 : 0; // TINYINT(1) puede llegar como Boolean
        }
        if (o instanceof Number) {
            return (Number) o;
        }
        return Double.parseDouble(o.toString());
    }

    public int integer(String k) {
        return num(obj(k)).intValue();
    }

    public long lng(String k) {
        return num(obj(k)).longValue();
    }

    public double dbl(String k) {
        return num(obj(k)).doubleValue();
    }

    public boolean bool(String k) {
        return integer(k) != 0;
    }

    public long id() {
        return lng("id");
    }

    @Override
    public String toString() {
        return datos.toString();
    }
}
