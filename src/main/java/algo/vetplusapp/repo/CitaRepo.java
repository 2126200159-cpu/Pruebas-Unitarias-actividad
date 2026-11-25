package algo.vetplusapp.repo;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Sesion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Agenda: citas, horarios libres, estancias de pension. */
public final class CitaRepo {

    private static final String BASE = "SELECT c.id, c.fecha, c.hora_inicio, c.duracion_min, c.estado, "
            + "c.consultorio, c.motivo, m.id AS mascota_id, m.nombre AS mascota, m.especie, m.raza, "
            + "p.id AS propietario_id, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, "
            + "p.telefono AS prop_telefono, p.correo AS prop_correo, "
            + "s.id AS servicio_id, s.nombre AS servicio, s.tipo AS tipo, "
            + "u.id AS profesional_id, TRIM(CONCAT(u.nombre, ' ', u.apellidos)) AS profesional "
            + "FROM cita c JOIN mascota m ON m.id = c.mascota_id JOIN propietario p ON p.id = m.propietario_id "
            + "JOIN servicio s ON s.id = c.servicio_id JOIN usuario u ON u.id = c.profesional_id ";

    private CitaRepo() {
    }

    /** Citas entre dos fechas (inclusive); profesional y servicio opcionales. No incluye canceladas. */
    public static List<Row> citas(String desde, String hasta, Long profesionalId, Long servicioId) {
        return Db.consultar(BASE + "WHERE c.fecha BETWEEN ? AND ? AND c.estado <> 'Cancelada' "
                + "AND (? IS NULL OR c.profesional_id = ?) AND (? IS NULL OR c.servicio_id = ?) "
                + "ORDER BY c.fecha, c.hora_inicio", desde, hasta, profesionalId, profesionalId, servicioId,
                servicioId);
    }

    public static Row cita(long id) {
        return Db.uno(BASE + "WHERE c.id = ?", id);
    }

    /** Citas de un profesional de un tipo de servicio (MEDICO o ESTETICA) en una fecha. */
    public static List<Row> citasDeProfesional(long profesionalId, String fecha) {
        return Db.consultar(BASE + "WHERE c.profesional_id = ? AND c.fecha = ? AND c.estado <> 'Cancelada' "
                + "ORDER BY c.hora_inicio", profesionalId, fecha);
    }

    public static List<Row> proximasHoy() {
        return Db.consultar(BASE + "WHERE c.fecha = ? AND c.estado IN ('Confirmada','En atención','En proceso') "
                + "ORDER BY c.hora_inicio", Config.hoy());
    }

    public static Row enAtencion(long profesionalId) {
        return Db.uno(BASE + "WHERE c.profesional_id = ? AND c.estado = 'En atención' ORDER BY c.fecha, c.hora_inicio "
                + "LIMIT 1", profesionalId);
    }

    /** La siguiente cita confirmada del profesional ese dia, despues de la hora dada. */
    public static Row siguiente(long profesionalId, String fecha, String despuesDeHora) {
        return Db.uno(BASE + "WHERE c.profesional_id = ? AND c.fecha = ? AND c.estado = 'Confirmada' "
                + "AND c.hora_inicio > ? ORDER BY c.hora_inicio LIMIT 1", profesionalId, fecha, despuesDeHora);
    }

    public static boolean hayConflicto(long profesionalId, String fecha, String hora, int duracion, Long ignorarId) {
        String fin = Fmt.hora(LocalTime.parse(hora).plusMinutes(duracion));
        return Db.contar("SELECT COUNT(*) AS v FROM cita WHERE profesional_id = ? AND fecha = ? "
                + "AND estado <> 'Cancelada' AND hora_inicio < ? "
                + "AND TIME_FORMAT(ADDTIME(CONCAT(hora_inicio, ':00'), SEC_TO_TIME(duracion_min * 60)), '%H:%i') > ? "
                + "AND (? IS NULL OR id <> ?)", profesionalId, fecha, fin, hora, ignorarId, ignorarId) > 0;
    }

    /** Ventana de horas en la que el profesional atiende ese dia (segun su turno y el horario de la clinica). */
    private static LocalTime[] ventana(long profesionalId, LocalDate dia) {
        Row p = UsuarioRepo.porId(profesionalId);
        int[] v = Config.ventanaTurno(p == null ? "" : p.str("turno"), dia.getDayOfWeek());
        return new LocalTime[]{LocalTime.of(v[0], 0), LocalTime.of(v[1], 0)};
    }

    /** Horarios libres del profesional ese dia (pasos de 30 min, dentro de su turno, sin pasado ni domingos). */
    public static List<String> horariosLibres(long profesionalId, String fecha, int duracion) {
        List<String> libres = new ArrayList<>();
        LocalDate dia = LocalDate.parse(fecha);
        if (dia.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            return libres;
        }
        LocalTime[] v = ventana(profesionalId, dia);
        LocalTime t = v[0];
        while (!t.plusMinutes(duracion).isAfter(v[1])) {
            boolean pasado = LocalDateTime.of(dia, t).isBefore(LocalDateTime.now());
            if (!pasado && !hayConflicto(profesionalId, fecha, Fmt.hora(t), duracion, null)) {
                libres.add(Fmt.hora(t));
            }
            t = t.plusMinutes(Config.PASO_MINUTOS);
        }
        return libres;
    }

    /** Valida y crea una cita Confirmada. Devuelve su id. */
    public static long crear(long mascotaId, long servicioId, long profesionalId, String fecha, String hora,
            String motivo) {
        Row servicio = ServicioRepo.porId(servicioId);
        if (servicio == null || !servicio.bool("activo")) {
            throw new ReglaException("El servicio elegido no está activo.");
        }
        if ("PENSION".equals(servicio.str("tipo"))) {
            throw new ReglaException("La pensión se registra como estancia, no como cita.");
        }
        Row prof = UsuarioRepo.porId(profesionalId);
        String rolEsperado = "MEDICO".equals(servicio.str("tipo")) ? "MEDICO" : "ESTILISTA";
        if (prof == null || !rolEsperado.equals(prof.str("rol")) || !"Activo".equals(prof.str("estado"))) {
            throw new ReglaException("El profesional no corresponde a este servicio.");
        }
        int duracion = servicio.integer("duracion_min") > 0 ? servicio.integer("duracion_min") : 30;
        if (LocalDateTime.of(LocalDate.parse(fecha), LocalTime.parse(hora)).isBefore(LocalDateTime.now())) {
            throw new ReglaException("Una cita no puede quedar en el pasado.");
        }
        LocalDate dia = LocalDate.parse(fecha);
        LocalTime inicio = LocalTime.parse(hora);
        LocalTime[] v = ventana(profesionalId, dia);
        if (dia.getDayOfWeek() == java.time.DayOfWeek.SUNDAY || inicio.isBefore(v[0])
                || inicio.plusMinutes(duracion).isAfter(v[1])) {
            throw new ReglaException("Ese horario está fuera del turno de " + prof.str("nombre_completo")
                    + " (la clínica no abre domingos; sábados cierra a las 15:00).");
        }
        if (hayConflicto(profesionalId, fecha, hora, duracion, null)) {
            throw new ReglaException("Ese horario ya está ocupado.");
        }
        long id = Db.insertar("INSERT INTO cita (mascota_id, servicio_id, profesional_id, fecha, hora_inicio, "
                + "duracion_min, consultorio, estado, motivo, creada_por) VALUES (?,?,?,?,?,?,?, 'Confirmada', ?, ?)",
                mascotaId, servicioId, profesionalId, fecha, hora, duracion,
                "MEDICO".equals(servicio.str("tipo")) ? Config.CONSULTORIO : "", motivo == null ? "" : motivo.trim(),
                Sesion.usuarioId == 0 ? null : Sesion.usuarioId);
        Row m = ClienteRepo.mascota(mascotaId);
        ActividadRepo.registrar("CITA", "Cita programada", m.str("propietario") + " · " + servicio.str("nombre")
                + " de " + m.str("nombre"));
        return id;
    }

    /** Pension: crea la estancia y agrega dias x tarifa a la cuenta pendiente de hoy del propietario. */
    public static long crearEstancia(long mascotaId, LocalDate entrada, LocalDate salida) {
        if (entrada == null || salida == null) {
            throw new ReglaException("Captura la fecha de entrada y la de salida.");
        }
        if (entrada.isBefore(LocalDate.now())) {
            throw new ReglaException("La fecha de entrada no puede estar en el pasado.");
        }
        if (salida.isBefore(entrada)) {
            throw new ReglaException("La salida no puede ser antes de la entrada.");
        }
        Row pension = ServicioRepo.porNombre("Pensión");
        if (pension == null || !pension.bool("activo")) {
            throw new ReglaException("El servicio de Pensión no está activo.");
        }
        Row m = ClienteRepo.mascota(mascotaId);
        int dias = (int) Math.max(1, ChronoUnit.DAYS.between(entrada, salida));
        return Db.transaccion(() -> {
            long id = Db.insertar("INSERT INTO estancia (mascota_id, fecha_entrada, fecha_salida_prevista, estado) "
                    + "VALUES (?,?,?, 'ACTIVA')", mascotaId, entrada.toString(), salida.toString());
            CajaRepo.agregarRenglon(m.lng("prop_id"), mascotaId, pension.id(), null, dias);
            ActividadRepo.registrar("PENSION", "Ingreso a pensión", m.str("propietario") + " · " + m.str("nombre")
                    + " (" + Fmt.plural(dias, "día", "días") + ")");
            return id;
        });
    }

    public static List<Row> estanciasActivas() {
        return Db.consultar("SELECT e.id, e.fecha_entrada, e.fecha_salida_prevista, e.hora_salida, m.nombre AS mascota, "
                + "m.raza, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, p.telefono AS prop_telefono, "
                + "p.correo AS prop_correo FROM estancia e JOIN mascota m ON m.id = e.mascota_id "
                + "JOIN propietario p ON p.id = m.propietario_id "
                + "WHERE e.estado = 'ACTIVA' AND e.fecha_entrada <= ? ORDER BY e.fecha_salida_prevista, m.nombre",
                Config.hoy());
    }

    /** Pasa una cita Confirmada a En atencion. Solo una atencion en curso por medico. */
    public static void iniciarAtencion(long citaId, long medicoId) {
        Row otra = enAtencion(medicoId);
        if (otra != null && otra.id() != citaId) {
            throw new ReglaException("Ya tienes una atención en curso (" + otra.str("mascota")
                    + "). Primero debes finalizarla.");
        }
        Db.ejecutar("UPDATE cita SET estado = 'En atención' WHERE id = ? AND estado = 'Confirmada'", citaId);
    }
}
