package algo.vetplusapp.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Constantes del negocio y utilidades de tiempo. */
public final class Config {

    /** Horario de la clinica: lunes a viernes 9:00-19:00, sabado 9:00-15:00, domingo cerrado. */
    public static final int HORA_APERTURA = 9;
    public static final int HORA_CIERRE = 19;
    public static final int HORA_CIERRE_SABADO = 15;
    public static final int HORA_CORTE_TURNO = 14;
    public static final int PASO_MINUTOS = 30;
    public static final String CONSULTORIO = "Consultorio 1";
    public static final int CLIENTES_POR_PAGINA = 7;
    public static final String[] TURNOS = {"Matutino", "Vespertino", "Mixto"};
    public static final String[] METODOS = {"Tarjeta", "Efectivo", "Transferencia"};
    private static final DateTimeFormatter SQL_TIEMPO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Config() {
    }

    public static String hoy() {
        return LocalDate.now().toString();
    }

    public static String ahora() {
        return LocalDateTime.now().format(SQL_TIEMPO);
    }

    public static String rolTexto(String rol) {
        switch (rol) {
            case "ADMINISTRACION":
                return "Administración";
            case "MEDICO":
                return "Médico veterinario";
            case "ESTILISTA":
                return "Estilista canino";
            default:
                return rol;
        }
    }

    public static String rolCodigo(String texto) {
        if ("Administración".equals(texto)) {
            return "ADMINISTRACION";
        }
        if ("Médico veterinario".equals(texto)) {
            return "MEDICO";
        }
        return "ESTILISTA";
    }

    /** Tasa de descuento familiar segun mascotas distintas (3.7). */
    public static int tasaDescuento(int mascotasDistintas) {
        if (mascotasDistintas >= 3) {
            return 25;
        }
        if (mascotasDistintas == 2) {
            return 15;
        }
        return 0;
    }

    public static int cierreDelDia(java.time.DayOfWeek d) {
        return d == java.time.DayOfWeek.SATURDAY ? HORA_CIERRE_SABADO : HORA_CIERRE;
    }

    /** Ventana de atencion {inicio, fin} en horas para un turno: Matutino 9-14, Vespertino 14-cierre, Mixto todo el dia. */
    public static int[] ventanaTurno(String turno, java.time.DayOfWeek d) {
        int cierre = cierreDelDia(d);
        if ("Matutino".equals(turno)) {
            return new int[]{HORA_APERTURA, Math.min(HORA_CORTE_TURNO, cierre)};
        }
        if ("Vespertino".equals(turno)) {
            return new int[]{Math.min(HORA_CORTE_TURNO, cierre), cierre};
        }
        return new int[]{HORA_APERTURA, cierre};
    }
}
