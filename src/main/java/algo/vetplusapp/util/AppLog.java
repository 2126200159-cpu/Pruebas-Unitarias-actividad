package algo.vetplusapp.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Archivo log: Documentos/VetPlus/logs/vetplus.log. Una linea por evento:
 * fecha hora | NIVEL | modulo | operacion | tipo de excepcion | mensaje (ya sin datos sensibles).
 * Nunca se escriben contrasenas ni el rastro completo de la excepcion (que puede traer datos).
 */
public final class AppLog {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    private AppLog() {
    }

    public static void info(String modulo, String operacion, String mensaje) {
        escribir("INFO", modulo, operacion, "-", mensaje);
    }

    /** Regla del negocio o validacion que impidio una operacion (error esperado del usuario). */
    public static void aviso(Throwable t) {
        escribir("AVISO", moduloDe(t), operacionDe(t), t.getClass().getSimpleName(), t.getMessage());
    }

    /** Error tecnico: base de datos, disco o fallo inesperado. */
    public static void error(Throwable t) {
        Throwable raiz = t;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        String tipo = t.getClass().getSimpleName() + (raiz == t ? "" : " <- " + raiz.getClass().getSimpleName());
        String detalle = t.getMessage() + (raiz == t || raiz.getMessage() == null ? "" : " | causa: "
                + raiz.getMessage());
        escribir("ERROR", moduloDe(t), operacionDe(t), tipo, detalle);
    }

    public static void error(String modulo, String operacion, Throwable t) {
        escribir("ERROR", modulo, operacion, t.getClass().getSimpleName(), t.getMessage());
    }

    private static StackTraceElement origen(Throwable t) {
        StackTraceElement primero = null;
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            if (!c.startsWith("algo.vetplusapp.") || c.endsWith(".AppLog") || c.endsWith(".Manejo")) {
                continue;
            }
            if (primero == null) {
                primero = e;
            }
            if (c.contains(".repo.") || c.contains(".ctrl.") || c.endsWith(".ImagenStore")) {
                return e;
            }
        }
        return primero;
    }

    private static String moduloDe(Throwable t) {
        StackTraceElement e = origen(t);
        if (e == null) {
            return "Aplicacion";
        }
        String c = e.getClassName();
        String simple = c.substring(c.lastIndexOf('.') + 1);
        int dolar = simple.indexOf('$');
        return dolar > 0 ? simple.substring(0, dolar) : simple;
    }

    private static String operacionDe(Throwable t) {
        StackTraceElement e = origen(t);
        if (e == null) {
            return "desconocida";
        }
        String m = e.getMethodName();
        if (m.startsWith("lambda$")) {
            String[] partes = m.split("\\$");
            return partes.length > 1 ? partes[1] : m;
        }
        return m;
    }

    private static synchronized void escribir(String nivel, String modulo, String operacion, String tipo,
            String mensaje) {
        String linea = FECHA.format(LocalDateTime.now()) + " | " + nivel + " | " + limpio(modulo) + " | "
                + limpio(operacion) + " | " + limpio(tipo) + " | " + Sensible.limpiar(mensaje) + System.lineSeparator();
        try {
            Path archivo = Rutas.archivoLog();
            if (Files.exists(archivo) && Files.size(archivo) > MAX_BYTES) {
                Files.move(archivo, archivo.resolveSibling("vetplus.log.1"), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.write(archivo, linea.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException e) {
            // el log nunca debe tumbar la aplicacion
            System.err.println("No se pudo escribir el log: " + Sensible.limpiar(e.getMessage()));
        }
    }

    private static String limpio(String s) {
        return s == null || s.isEmpty() ? "-" : Sensible.limpiar(s).replace('|', '/');
    }
}
