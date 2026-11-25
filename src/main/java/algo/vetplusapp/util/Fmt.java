package algo.vetplusapp.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos de dinero, fecha y hora (capitulo 2.7). Se arman a mano para evitar puntos extra en los meses. */
public final class Fmt {

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final String[] DIAS_ABR = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
        "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private static final String[] MESES_ABR = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep",
        "oct", "nov", "dic"};
    public static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DecimalFormat DINERO = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.US));

    private Fmt() {
    }

    public static String dinero(double v) {
        return "$" + DINERO.format(redondear(v));
    }

    /** Descuento con signo menos: −$257.50 */
    public static String descuento(double v) {
        return "\u2212$" + DINERO.format(redondear(Math.abs(v)));
    }

    public static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Importe sin simbolo, para campos de captura: 772.50 */
    public static String importe(double v) {
        return String.format(Locale.US, "%.2f", redondear(v));
    }

    public static String fechaCorta(LocalDate d) {
        return DIAS_ABR[d.getDayOfWeek().getValue() - 1] + ", " + d.getDayOfMonth() + " "
                + MESES_ABR[d.getMonthValue() - 1] + " " + d.getYear();
    }

    public static String fechaLarga(LocalDate d) {
        return DIAS[d.getDayOfWeek().getValue() - 1] + " " + d.getDayOfMonth() + " de "
                + MESES[d.getMonthValue() - 1] + " de " + d.getYear();
    }

    /** 26 octubre 2026 */
    public static String fechaMedia(LocalDate d) {
        return d.getDayOfMonth() + " " + MESES[d.getMonthValue() - 1] + " " + d.getYear();
    }

    /** 12 de febrero de 2024 */
    public static String fechaSinDia(LocalDate d) {
        return d.getDayOfMonth() + " de " + MESES[d.getMonthValue() - 1] + " de " + d.getYear();
    }

    /** 12 feb 2024 */
    public static String fechaDMA(LocalDate d) {
        return d.getDayOfMonth() + " " + MESES_ABR[d.getMonthValue() - 1] + " " + d.getYear();
    }

    /** 28 oct */
    public static String fechaDM(LocalDate d) {
        return d.getDayOfMonth() + " " + MESES_ABR[d.getMonthValue() - 1];
    }

    /** Lunes, 26 oct 2026 */
    public static String fechaDiaCompleto(LocalDate d) {
        return DIAS[d.getDayOfWeek().getValue() - 1] + ", " + fechaDMA(d);
    }

    /** Lunes 26 de octubre */
    public static String fechaDiaMes(LocalDate d) {
        return DIAS[d.getDayOfWeek().getValue() - 1] + " " + d.getDayOfMonth() + " de "
                + MESES[d.getMonthValue() - 1];
    }

    /** Lun 26 oct */
    public static String fechaAbrDM(LocalDate d) {
        return DIAS_ABR[d.getDayOfWeek().getValue() - 1] + " " + d.getDayOfMonth() + " "
                + MESES_ABR[d.getMonthValue() - 1];
    }

    /** 26 OCT 2026 · 10:00 */
    public static String fechaEvento(LocalDate d, String hora) {
        return d.getDayOfMonth() + " " + MESES_ABR[d.getMonthValue() - 1].toUpperCase(Locale.ROOT) + " "
                + d.getYear() + (hora == null || hora.isEmpty() ? "" : " · " + hora);
    }

    public static String diaAbreviado(DayOfWeek d) {
        return DIAS_ABR[d.getValue() - 1];
    }

    public static String hora(LocalTime t) {
        return t.format(HORA);
    }

    public static String rango(String inicio, int minutos) {
        LocalTime t = LocalTime.parse(inicio);
        return Fmt.hora(t) + "\u2013" + Fmt.hora(t.plusMinutes(minutos));
    }

    public static String fechaHoraCorta(LocalDateTime t) {
        return fechaDMA(t.toLocalDate()) + ", " + Fmt.hora(t.toLocalTime());
    }

    public static LocalDate fecha(String iso) {
        return iso == null || iso.isEmpty() ? null : LocalDate.parse(iso.substring(0, 10));
    }

    public static String iniciales(String nombreCompleto) {
        StringBuilder sb = new StringBuilder();
        for (String p : nombreCompleto.trim().split("\\s+")) {
            if (!p.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            }
        }
        return sb.toString();
    }

    /** Edad en texto a partir de la fecha de nacimiento: "4 años", "7 meses". */
    public static String edad(String nacimientoIso) {
        LocalDate n = fecha(nacimientoIso);
        if (n == null) {
            return "—";
        }
        java.time.Period p = java.time.Period.between(n, LocalDate.now());
        if (p.getYears() >= 1) {
            return p.getYears() + (p.getYears() == 1 ? " año" : " años");
        }
        return p.getMonths() + (p.getMonths() == 1 ? " mes" : " meses");
    }

    public static String nvl(String s, String porDefecto) {
        return s == null || s.trim().isEmpty() ? porDefecto : s;
    }

    public static String plural(int n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }
}
