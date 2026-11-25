package algo.vetplusapp.util;

import algo.vetplusapp.db.Db.ReglaException;
import java.util.regex.Pattern;

/** Validaciones de captura (telefono, CURP, correo, numeros). Lanzan ReglaException con el mensaje. */
public final class Valida {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /** Estructura oficial: 4 letras, fecha AAMMDD valida, sexo H/M, entidad, 3 consonantes, homoclave y digito. */
    private static final Pattern CURP_FORMATO = Pattern.compile(
            "[A-Z]{4}\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])[HM]"
            + "(AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)"
            + "[B-DF-HJ-NP-TV-Z]{3}[A-Z0-9]\\d");
    public static final double PRECIO_MAX = 1_000_000;
    public static final int DURACION_MIN = 5;
    public static final int DURACION_MAX = 480;

    private Valida() {
    }

    /** Costo de un servicio o insumo: numero entre 0 y 1,000,000. */
    public static void costo(double v, String campo) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0) {
            throw new ReglaException(campo + " no puede ser negativo.");
        }
        if (v > PRECIO_MAX) {
            throw new ReglaException(campo + " no puede ser mayor a $1,000,000.");
        }
    }

    /** Tiempo estimado en minutos: entero entre 5 y 480. */
    public static void duracion(Integer minutos) {
        if (minutos == null || minutos < DURACION_MIN || minutos > DURACION_MAX) {
            throw new ReglaException("La duración debe ser de " + DURACION_MIN + " a " + DURACION_MAX + " minutos.");
        }
    }

    public static void fechaNoFutura(java.time.LocalDate d, String campo) {
        if (d != null && d.isAfter(java.time.LocalDate.now())) {
            throw new ReglaException(campo + " no puede ser una fecha futura.");
        }
    }

    public static void fechaNoPasada(java.time.LocalDate d, String campo) {
        if (d != null && d.isBefore(java.time.LocalDate.now())) {
            throw new ReglaException(campo + " no puede ser una fecha pasada.");
        }
    }

    public static String t(String s) {
        return s == null ? "" : s.trim();
    }

    public static void obligatorio(String valor, String campo) {
        if (t(valor).isEmpty()) {
            throw new ReglaException("Falta capturar: " + campo + ".");
        }
    }

    public static void telefono(String v, boolean obligatorio) {
        String s = t(v);
        if (s.isEmpty()) {
            if (obligatorio) {
                throw new ReglaException("Falta capturar: teléfono.");
            }
            return;
        }
        if (!s.matches("\\d+")) {
            throw new ReglaException("El teléfono solo acepta números; no se aceptan letras ni símbolos.");
        }
        if (s.length() != 10) {
            throw new ReglaException("El teléfono debe tener 10 dígitos.");
        }
    }

    /** Nombres de personas o mascotas: solo letras (con acentos, ñ), espacios, punto, apostrofo y guion. */
    public static void nombre(String v, String etiqueta, boolean obligatorio) {
        String s = t(v);
        if (s.isEmpty()) {
            if (obligatorio) {
                throw new ReglaException("Falta capturar: " + etiqueta.toLowerCase() + ".");
            }
            return;
        }
        if (s.matches(".*\\d.*")) {
            throw new ReglaException(etiqueta + " solo acepta letras; no se aceptan números.");
        }
        if (!s.matches("[\\p{L}][\\p{L} .'’-]*")) {
            throw new ReglaException(etiqueta + " solo acepta letras.");
        }
    }

    public static void curp(String v, boolean obligatorio) {
        String s = t(v);
        if (s.isEmpty()) {
            if (obligatorio) {
                throw new ReglaException("Falta capturar: CURP.");
            }
            return;
        }
        if (!s.matches("[A-Za-z0-9]+")) {
            throw new ReglaException("La CURP solo acepta letras y números.");
        }
        if (s.length() != 18) {
            throw new ReglaException("La CURP debe tener 18 caracteres.");
        }
        if (!CURP_FORMATO.matcher(s.toUpperCase(java.util.Locale.ROOT)).matches()) {
            throw new ReglaException("La CURP no tiene un formato válido (ejemplo: GOMA900515HJCRRN09).");
        }
    }

    public static void correo(String v, boolean obligatorio) {
        String s = t(v);
        if (s.isEmpty()) {
            if (obligatorio) {
                throw new ReglaException("Falta capturar: correo.");
            }
            return;
        }
        if (!CORREO.matcher(s).matches()) {
            throw new ReglaException("El correo no tiene un formato válido.");
        }
    }

    public static double numero(String v, String campo, boolean obligatorio) {
        String s = t(v).replace("$", "").replace(",", "");
        if (s.isEmpty()) {
            if (obligatorio) {
                throw new ReglaException("Falta capturar: " + campo + ".");
            }
            return Double.NaN;
        }
        try {
            double d = Double.parseDouble(s);
            if (d < 0) {
                throw new ReglaException(campo + " no puede ser negativo.");
            }
            return d;
        } catch (NumberFormatException e) {
            throw new ReglaException(campo + " debe ser un número válido.");
        }
    }

    public static Integer entero(String v, String campo, boolean obligatorio) {
        double d = numero(v, campo, obligatorio);
        if (Double.isNaN(d)) {
            return null;
        }
        if (d != Math.floor(d)) {
            throw new ReglaException(campo + " debe ser un número entero.");
        }
        return (int) d;
    }
}
