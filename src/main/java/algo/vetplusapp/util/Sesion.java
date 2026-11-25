package algo.vetplusapp.util;

/** Quien inicio sesion. Todas las pantallas leen de aqui. */
public final class Sesion {

    public static long usuarioId;
    public static String nombreCompleto = "";
    public static String rol = "";            // ADMINISTRACION, MEDICO o ESTILISTA
    public static String turno = "";
    public static String especialidad = "";

    private Sesion() {
    }

    public static void limpiar() {
        usuarioId = 0;
        nombreCompleto = "";
        rol = "";
        turno = "";
        especialidad = "";
    }
}
