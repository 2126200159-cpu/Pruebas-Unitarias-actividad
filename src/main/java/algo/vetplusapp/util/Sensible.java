package algo.vetplusapp.util;

import java.util.regex.Pattern;

/** Quita de un texto la informacion sensible antes de escribirlo en el log. */
public final class Sensible {

    private static final Pattern HASH = Pattern.compile("pbkdf2\\$\\d+\\$[A-Za-z0-9+/=]+\\$[A-Za-z0-9+/=]+");
    private static final Pattern CLAVE = Pattern.compile(
            "(?i)\\b(contrase[ñn]a(?:_?hash|_?temporal)?|password|passwd|pwd|clave|token|secret)\\b\\s*[=:]\\s*"
            + "(\"[^\"]*\"|'[^']*'|\\S+)");
    private static final Pattern CORREO = Pattern.compile(
            "([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final Pattern CURP = Pattern.compile("\\b[A-Z]{4}\\d{6}[HM][A-Z]{2}[A-Z]{3}[A-Z0-9]\\d\\b");
    private static final Pattern TELEFONO = Pattern.compile("(?<!\\d)\\d{10}(?!\\d)");

    private Sensible() {
    }

    public static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        String t = texto.replace('\r', ' ').replace('\n', ' ');
        t = HASH.matcher(t).replaceAll("[HASH]");
        t = CLAVE.matcher(t).replaceAll("$1=***");
        t = CORREO.matcher(t).replaceAll("$1***@$2");
        t = CURP.matcher(t).replaceAll("[CURP]");
        t = TELEFONO.matcher(t).replaceAll("[TEL]");
        return t.length() > 600 ? t.substring(0, 600) + "…" : t;
    }
}
