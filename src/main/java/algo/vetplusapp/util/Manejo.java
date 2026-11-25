package algo.vetplusapp.util;

import algo.vetplusapp.db.Db;
import java.util.Optional;

/**
 * Convierte cualquier excepcion en un mensaje claro para el usuario y la registra en el log.
 * La aplicacion sigue funcionando despues de un error recuperable.
 */
public final class Manejo {

    /** Mensaje para el usuario; esError indica si fue un fallo tecnico (y no una regla del negocio). */
    public static final class Resultado {

        public final String mensaje;
        public final boolean esError;

        Resultado(String mensaje, boolean esError) {
            this.mensaje = mensaje;
            this.esError = esError;
        }
    }

    private Manejo() {
    }

    public static Resultado procesar(Throwable t) {
        if (t instanceof Db.ReglaException) {
            AppLog.aviso(t);
            return new Resultado(t.getMessage(), false);
        }
        AppLog.error(t);
        if (t instanceof AlmacenamientoException) {
            return new Resultado(t.getMessage(), true);
        }
        if (t instanceof Db.DbException) {
            return new Resultado("No se pudo completar la operación con la base de datos. Verifica que MySQL "
                    + "(XAMPP) esté encendido e inténtalo de nuevo.", true);
        }
        return new Resultado("Ocurrió un problema inesperado. El detalle quedó registrado en el archivo log de "
                + "la aplicación (Documentos/VetPlus/logs). Puedes seguir trabajando.", true);
    }

    /** Ejecuta la accion; si falla devuelve el mensaje para mostrar. Nunca propaga la excepcion. */
    public static Optional<Resultado> ejecutar(Runnable accion) {
        try {
            accion.run();
            return Optional.empty();
        } catch (RuntimeException e) {
            return Optional.of(procesar(e));
        }
    }
}
