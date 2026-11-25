package algo.vetplusapp.repo;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Valida;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Atencion medica (3.4) y servicio de estetica canina (3.5). */
public final class AtencionRepo {

    private AtencionRepo() {
    }

    /** Datos capturados en la nota de consulta. */
    public static final class Nota {

        public String motivo = "";
        public Double peso;
        public Double temperatura;
        public Integer frecuencia;
        public String exploracion = "";
        public String tratamiento = "";
        public LocalDate proximaFecha;
        public String proximaHora;
        public Long servicioId;
    }

    // ---------------------------------------------------------------- medica

    public static Row nota(long citaId) {
        return Db.uno("SELECT * FROM atencion_medica WHERE cita_id = ?", citaId);
    }

    public static boolean hayBorrador(long citaId) {
        return Db.contar("SELECT COUNT(*) AS v FROM atencion_medica WHERE cita_id = ? AND estado = 'BORRADOR'",
                citaId) > 0;
    }

    /** Nota en borrador de una mascota con este medico (la mas reciente). */
    public static Row borradorDeMascota(long mascotaId, long medicoId) {
        return Db.uno("SELECT am.*, c.id AS cita_id_c FROM atencion_medica am JOIN cita c ON c.id = am.cita_id "
                + "WHERE c.mascota_id = ? AND c.profesional_id = ? AND am.estado = 'BORRADOR' "
                + "ORDER BY c.fecha DESC, c.hora_inicio DESC LIMIT 1", mascotaId, medicoId);
    }

    private static void upsert(long citaId, Nota n, String estado) {
        Row existe = nota(citaId);
        if (existe != null && "FINALIZADA".equals(existe.str("estado"))) {
            throw new ReglaException("Esta nota ya está finalizada y no se puede editar.");
        }
        Object prox = n.proximaFecha == null ? null : n.proximaFecha.toString();
        if (existe == null) {
            Db.insertar("INSERT INTO atencion_medica (cita_id, motivo, peso, temperatura, frecuencia_cardiaca, "
                    + "exploracion_diagnostico, tratamiento, proxima_revision, proxima_revision_hora, "
                    + "servicio_registrado_id, estado, actualizado_en) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    citaId, n.motivo, n.peso, n.temperatura, n.frecuencia, n.exploracion, n.tratamiento, prox,
                    n.proximaHora, n.servicioId, estado, Config.ahora());
        } else {
            Db.ejecutar("UPDATE atencion_medica SET motivo=?, peso=?, temperatura=?, frecuencia_cardiaca=?, "
                    + "exploracion_diagnostico=?, tratamiento=?, proxima_revision=?, proxima_revision_hora=?, "
                    + "servicio_registrado_id=?, estado=?, actualizado_en=? WHERE cita_id=?",
                    n.motivo, n.peso, n.temperatura, n.frecuencia, n.exploracion, n.tratamiento, prox,
                    n.proximaHora, n.servicioId, estado, Config.ahora(), citaId);
        }
    }

    public static void guardarBorrador(long citaId, Nota n) {
        Db.transaccion(() -> upsert(citaId, n, "BORRADOR"));
    }

    /**
     * Finaliza: nota finalizada, cita Finalizada, historial, cuenta del propietario, peso de la mascota,
     * cuidado (vacuna/desparasitacion) y cita de revision si se pidio. Devuelve true si creo la cita de revision.
     */
    public static boolean finalizar(long citaId, Nota n) {
        Valida.obligatorio(n.motivo, "motivo de consulta");
        Valida.obligatorio(n.exploracion, "exploración y diagnóstico");
        if (n.servicioId == null) {
            throw new ReglaException("Elige el servicio registrado que se cobrará.");
        }
        Row cita = CitaRepo.cita(citaId);
        Row servicio = ServicioRepo.porId(n.servicioId);
        return Db.transaccion(() -> {
            upsert(citaId, n, "FINALIZADA");
            Db.ejecutar("UPDATE cita SET estado = 'Finalizada' WHERE id = ?", citaId);
            ClienteRepo.actualizarPesoMascota(cita.lng("mascota_id"), n.peso);
            CajaRepo.agregarRenglon(cita.lng("propietario_id"), cita.lng("mascota_id"), servicio.id(), citaId, 1);
            String nombreServicio = servicio.str("nombre");
            if ("Vacunación".equals(nombreServicio) || "Desparasitación".equals(nombreServicio)) {
                LocalDate hoy = LocalDate.now();
                LocalDate proxima = n.proximaFecha != null ? n.proximaFecha
                        : ("Vacunación".equals(nombreServicio) ? hoy.plusYears(1) : hoy.plusMonths(3));
                ClienteRepo.registrarCuidado(cita.lng("mascota_id"), nombreServicio, hoy, proxima,
                        cita.str("profesional"));
            }
            ActividadRepo.registrar("ATENCION", "Atención finalizada", cita.str("propietario") + " · "
                    + nombreServicio + " de " + cita.str("mascota"));
            boolean creada = false;
            if (n.proximaFecha != null && n.proximaHora != null && !n.proximaHora.isEmpty()) {
                Row consulta = ServicioRepo.porNombre("Consulta");
                try {
                    if (consulta != null) {
                        CitaRepo.crear(cita.lng("mascota_id"), consulta.id(), cita.lng("profesional_id"),
                                n.proximaFecha.toString(), n.proximaHora, "Revisión de seguimiento");
                        creada = true;
                    }
                } catch (ReglaException e) {
                    creada = false;
                }
            }
            return creada;
        });
    }

    /** Antecedentes para la consulta: aviso importante y diagnosticos de atenciones finalizadas. */
    public static List<Row> antecedentes(long mascotaId) {
        List<Row> lista = new ArrayList<>();
        Row m = Db.uno("SELECT antecedentes FROM mascota WHERE id = ?", mascotaId);
        if (m != null && !m.str("antecedentes").isEmpty()) {
            Row r = new Row();
            r.put("titulo", "Antecedente importante");
            r.put("texto", m.str("antecedentes"));
            lista.add(r);
        }
        for (Row a : Db.consultar("SELECT c.fecha, c.hora_inicio, s.nombre AS servicio, "
                + "am.exploracion_diagnostico AS diag FROM atencion_medica am JOIN cita c ON c.id = am.cita_id "
                + "JOIN servicio s ON s.id = c.servicio_id WHERE c.mascota_id = ? AND am.estado = 'FINALIZADA' "
                + "ORDER BY c.fecha DESC, c.hora_inicio DESC LIMIT 5", mascotaId)) {
            Row r = new Row();
            r.put("titulo", Fmt.fechaDMA(Fmt.fecha(a.str("fecha"))) + " · " + a.str("servicio"));
            r.put("texto", a.str("diag"));
            lista.add(r);
        }
        return lista;
    }

    // ---------------------------------------------------------------- estetica

    public static Row servicioEstetica(long citaId) {
        return Db.uno("SELECT * FROM servicio_estetica WHERE cita_id = ?", citaId);
    }

    /** Citas de estetica del profesional en la fecha, con el avance guardado (si existe). */
    public static List<Row> esteticaDelDia(long estilistaId, String fecha) {
        return Db.consultar("SELECT c.id, c.fecha, c.hora_inicio, c.duracion_min, c.estado, c.motivo, "
                + "m.id AS mascota_id, m.nombre AS mascota, m.raza, m.nota_estetica, "
                + "p.id AS propietario_id, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, "
                + "p.telefono AS prop_telefono, p.correo AS prop_correo, "
                + "s.id AS servicio_id, s.nombre AS servicio, s.precio AS tarifa, "
                + "se.preferencias, se.chk_bano_secado, se.chk_oidos, se.chk_corte, se.chk_revision_unas, "
                + "se.observaciones, se.estado AS estado_se, se.entrega_estimada, se.propietario_notificado "
                + "FROM cita c JOIN mascota m ON m.id = c.mascota_id JOIN propietario p ON p.id = m.propietario_id "
                + "JOIN servicio s ON s.id = c.servicio_id LEFT JOIN servicio_estetica se ON se.cita_id = c.id "
                + "WHERE c.profesional_id = ? AND c.fecha = ? AND s.tipo = 'ESTETICA' AND c.estado <> 'Cancelada' "
                + "ORDER BY c.hora_inicio", estilistaId, fecha);
    }

    public static Row esteticaDeCita(long citaId) {
        List<Row> r = Db.consultar("SELECT c.id, c.fecha, c.hora_inicio, c.duracion_min, c.estado, c.motivo, "
                + "c.profesional_id, m.id AS mascota_id, m.nombre AS mascota, m.raza, m.nota_estetica, "
                + "p.id AS propietario_id, TRIM(CONCAT(p.nombre, ' ', p.apellidos)) AS propietario, "
                + "p.telefono AS prop_telefono, p.correo AS prop_correo, "
                + "s.id AS servicio_id, s.nombre AS servicio, s.precio AS tarifa, "
                + "TRIM(CONCAT(u.nombre, ' ', u.apellidos)) AS estilista, "
                + "se.preferencias, se.chk_bano_secado, se.chk_oidos, se.chk_corte, se.chk_revision_unas, "
                + "se.observaciones, se.estado AS estado_se, se.entrega_estimada, se.propietario_notificado "
                + "FROM cita c JOIN mascota m ON m.id = c.mascota_id JOIN propietario p ON p.id = m.propietario_id "
                + "JOIN servicio s ON s.id = c.servicio_id JOIN usuario u ON u.id = c.profesional_id "
                + "LEFT JOIN servicio_estetica se ON se.cita_id = c.id WHERE c.id = ? AND s.tipo = 'ESTETICA'", citaId);
        return r.isEmpty() ? null : r.get(0);
    }

    public static String entregaEstimada(Row cita) {
        return Fmt.hora(java.time.LocalTime.parse(cita.str("hora_inicio")).plusMinutes(cita.integer("duracion_min")));
    }

    private static void guardarSe(long citaId, Row cita, String preferencias, boolean bano, boolean oidos,
            boolean corte, boolean unas, String observaciones, String estado) {
        Row existe = servicioEstetica(citaId);
        if (existe != null && "FINALIZADO".equals(existe.str("estado"))) {
            throw new ReglaException("Este servicio ya está finalizado.");
        }
        if (existe == null) {
            Db.insertar("INSERT INTO servicio_estetica (cita_id, preferencias, chk_bano_secado, chk_oidos, chk_corte, "
                    + "chk_revision_unas, observaciones, estado, entrega_estimada) VALUES (?,?,?,?,?,?,?,?,?)",
                    citaId, preferencias, bano, oidos, corte, unas, observaciones, estado, entregaEstimada(cita));
        } else {
            Db.ejecutar("UPDATE servicio_estetica SET preferencias=?, chk_bano_secado=?, chk_oidos=?, chk_corte=?, "
                    + "chk_revision_unas=?, observaciones=?, estado=? WHERE cita_id=?",
                    preferencias, bano, oidos, corte, unas, observaciones, estado, citaId);
        }
    }

    /** Guarda casillas y observaciones. El primer guardado pasa la cita de Confirmada a En proceso. */
    public static void guardarAvance(long citaId, String preferencias, boolean bano, boolean oidos, boolean corte,
            boolean unas, String observaciones) {
        Row cita = esteticaDeCita(citaId);
        Db.transaccion(() -> {
            guardarSe(citaId, cita, preferencias, bano, oidos, corte, unas, observaciones, "EN_PROCESO");
            Db.ejecutar("UPDATE cita SET estado = 'En proceso' WHERE id = ? AND estado = 'Confirmada'", citaId);
        });
    }

    /** Finaliza: historial de estetica, propietario notificado, servicio a la cuenta. */
    public static void finalizarEstetica(long citaId, String preferencias, boolean bano, boolean oidos,
            boolean corte, boolean unas, String observaciones) {
        Row cita = esteticaDeCita(citaId);
        Db.transaccion(() -> {
            guardarSe(citaId, cita, preferencias, bano, oidos, corte, unas, observaciones, "FINALIZADO");
            Db.ejecutar("UPDATE servicio_estetica SET propietario_notificado = 1 WHERE cita_id = ?", citaId);
            Db.ejecutar("UPDATE cita SET estado = 'Finalizada' WHERE id = ?", citaId);
            CajaRepo.agregarRenglon(cita.lng("propietario_id"), cita.lng("mascota_id"), cita.lng("servicio_id"),
                    citaId, 1);
            ActividadRepo.registrar("ESTETICA", "Estética finalizada", cita.str("propietario") + " · "
                    + cita.str("servicio") + " de " + cita.str("mascota") + " · listo para entrega");
        });
    }
}
