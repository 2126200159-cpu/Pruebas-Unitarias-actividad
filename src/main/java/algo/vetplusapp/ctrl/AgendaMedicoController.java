package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.AtencionRepo;
import algo.vetplusapp.repo.CitaRepo;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Nav;
import algo.vetplusapp.util.Sesion;
import algo.vetplusapp.util.Ui;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Mi agenda (medico, 4.13): solo las citas asignadas a la sesion actual. */
public class AgendaMedicoController {

    @FXML
    private ToggleGroup grupoEstadoCitas;

    @FXML
    private Label lblSubtituloAgenda;

    @FXML
    private Label lblTurno;

    @FXML
    private Label lblCitasAsignadas;

    @FXML
    private Label lblDetalleAsignadas;

    @FXML
    private Label lblEnAtencion;

    @FXML
    private Label lblDetalleEnAtencion;

    @FXML
    private Label lblPorAtender;

    @FXML
    private Label lblDetallePorAtender;

    @FXML
    private Label lblFechaAgenda;

    @FXML
    private Button btnDiaAnterior;

    @FXML
    private Button btnHoy;

    @FXML
    private DatePicker dtpFechaAgenda;

    @FXML
    private Button btnDiaSiguiente;

    @FXML
    private ToggleButton tglTodas;

    @FXML
    private ToggleButton tglEnAtencion;

    @FXML
    private ToggleButton tglConfirmadas;

    @FXML
    private ToggleButton tglFinalizadas;

    @FXML
    private TableView<Row> tblAgendaMedico;

    @FXML
    private TableColumn<Row, String> colHorario;

    @FXML
    private TableColumn<Row, String> colPacientePropietario;

    @FXML
    private TableColumn<Row, String> colServicio;

    @FXML
    private TableColumn<Row, String> colConsultorio;

    @FXML
    private TableColumn<Row, String> colEstado;

    @FXML
    private Label lblSinAtencion;

    @FXML
    private VBox contenedorAtencionActual;

    @FXML
    private Label lblAtencionMascota;

    @FXML
    private Label lblAtencionDetalle;

    @FXML
    private Label lblAtencionHorario;

    @FXML
    private HBox cajaAvisoNota;

    @FXML
    private Button btnVerExpedienteActual;

    @FXML
    private Button btnContinuarAtencion;

    @FXML
    private Label lblSinTurno;

    @FXML
    private HBox contenedorTurno;

    @FXML
    private Label lblIntervaloLibre;

    @FXML
    private Label lblIntervaloLibreTexto;

    @FXML
    private Label lblPreparacionTitulo;

    @FXML
    private Label lblPreparacionTexto;

    @FXML
    private Label lblSinProxima;

    @FXML
    private VBox contenedorProxima;

    @FXML
    private Label lblSiguienteHora;

    @FXML
    private Label lblProximaMascota;

    @FXML
    private Label lblProximaPropietaria;

    @FXML
    private Label lblProximaServicio;

    @FXML
    private Label lblProximaHorario;

    @FXML
    private Label lblProximaConsultorio;

    @FXML
    private Label lblProximaMedico;

    @FXML
    private Button btnVerExpedienteProxima;

    @FXML
    private Button btnIniciarAtencion;

    private LocalDate fecha = LocalDate.now();
    private List<Row> citasDelDia = new ArrayList<>();
    private boolean llenando = false;
    private String iniSubtitulo;
    private String iniTextoTodas;

    @FXML
    private void initialize() {
        iniSubtitulo = lblSubtituloAgenda.getText();
        lblSubtituloAgenda.setText(iniSubtitulo + " · Agenda asignada a " + Sesion.nombreCompleto);
        if (!Sesion.turno.isEmpty()) {
            lblTurno.setText("Turno " + Sesion.turno.toLowerCase());
            Ui.mostrar(lblTurno, true);
        } else {
            Ui.mostrar(lblTurno, false);
        }

        Ui.col(colHorario, r -> Fmt.rango(r.str("hora_inicio"), r.integer("duracion_min")));
        Ui.col(colPacientePropietario, r -> r.str("mascota") + " / " + r.str("propietario"));
        Ui.col(colServicio, "servicio");
        Ui.col(colConsultorio, "consultorio");
        Ui.col(colEstado, "estado");

        btnDiaAnterior.setOnAction(e -> irA(fecha.minusDays(1)));
        btnDiaSiguiente.setOnAction(e -> irA(fecha.plusDays(1)));
        btnHoy.setOnAction(e -> irA(LocalDate.now()));
        dtpFechaAgenda.valueProperty().addListener((o, a, d) -> {
            if (!llenando && d != null) {
                irA(d);
            }
        });
        Ui.grupoSiempreUno(grupoEstadoCitas, t -> filtrarTabla());

        btnVerExpedienteActual.setOnAction(e -> {
            Row c = CitaRepo.enAtencion(Sesion.usuarioId);
            if (c != null) {
                Nav.ir("expedientes-medico", c.lng("mascota_id"));
            }
        });
        btnContinuarAtencion.setOnAction(e -> {
            Row c = CitaRepo.enAtencion(Sesion.usuarioId);
            if (c != null) {
                Nav.ir("consulta-medica", c.id());
            }
        });
        btnVerExpedienteProxima.setOnAction(e -> {
            Row c = proxima();
            if (c != null) {
                Nav.ir("expedientes-medico", c.lng("mascota_id"));
            }
        });
        btnIniciarAtencion.setOnAction(e -> Ui.accion(() -> {
            Row c = proxima();
            if (c == null) {
                return;
            }
            if (!Config.hoy().equals(c.str("fecha"))) {
                throw new ReglaException("Solo puedes iniciar atenciones de las citas de hoy.");
            }
            CitaRepo.iniciarAtencion(c.id(), Sesion.usuarioId);
            Nav.ir("consulta-medica", c.id());
        }));

        cargar();
    }

    private void irA(LocalDate d) {
        fecha = d;
        cargar();
    }

    private Row proxima() {
        for (Row c : citasDelDia) {
            if ("Confirmada".equals(c.str("estado"))) {
                return c;
            }
        }
        return null;
    }

    private void cargar() {
        llenando = true;
        dtpFechaAgenda.setValue(fecha);
        llenando = false;
        lblFechaAgenda.setText(Fmt.fechaDiaCompleto(fecha));
        citasDelDia = CitaRepo.citasDeProfesional(Sesion.usuarioId, fecha.toString());

        int enAtencion = 0;
        int porAtender = 0;
        String nombreEnAtencion = null;
        for (Row c : citasDelDia) {
            if ("En atención".equals(c.str("estado"))) {
                enAtencion++;
                nombreEnAtencion = c.str("mascota");
            } else if ("Confirmada".equals(c.str("estado"))) {
                porAtender++;
            }
        }
        lblCitasAsignadas.setText(String.valueOf(citasDelDia.size()));
        lblDetalleAsignadas.setText(citasDelDia.isEmpty() ? "Sin citas asignadas" : Fmt.fechaDiaMes(fecha));
        lblEnAtencion.setText(String.valueOf(enAtencion));
        lblDetalleEnAtencion.setText(nombreEnAtencion == null ? "Sin paciente en atención" : nombreEnAtencion);
        lblPorAtender.setText(String.valueOf(porAtender));
        Row prox = proxima();
        lblDetallePorAtender.setText(prox == null ? "Sin siguiente cita" : "Siguiente: " + prox.str("hora_inicio"));

        tglTodas.setText("Todos · " + citasDelDia.size());
        filtrarTabla();
        mostrarAtencionActual();
        mostrarTurnoYProxima(prox);
    }

    private void filtrarTabla() {
        String estado = tglEnAtencion.isSelected() ? "En atención" : tglConfirmadas.isSelected() ? "Confirmada"
                : tglFinalizadas.isSelected() ? "Finalizada" : "";
        List<Row> visibles = new ArrayList<>();
        for (Row c : citasDelDia) {
            if (estado.isEmpty() || estado.equals(c.str("estado"))) {
                visibles.add(c);
            }
        }
        tblAgendaMedico.getItems().setAll(visibles);
    }

    private void mostrarAtencionActual() {
        Row c = CitaRepo.enAtencion(Sesion.usuarioId);
        boolean hay = c != null;
        Ui.mostrar(contenedorAtencionActual, hay);
        Ui.mostrar(lblSinAtencion, !hay);
        if (!hay) {
            return;
        }
        lblAtencionMascota.setText(c.str("mascota"));
        lblAtencionDetalle.setText(Fmt.nvl(c.str("raza"), c.str("especie")) + " · Propietario: "
                + c.str("propietario"));
        lblAtencionHorario.setText(Fmt.rango(c.str("hora_inicio"), c.integer("duracion_min")) + " · "
                + Fmt.nvl(c.str("consultorio"), "—") + (c.str("motivo").isEmpty() ? "" : " · " + c.str("motivo")));
        Ui.mostrar(cajaAvisoNota, AtencionRepo.hayBorrador(c.id()));
    }

    private void mostrarTurnoYProxima(Row prox) {
        boolean hayCitas = !citasDelDia.isEmpty();
        Ui.mostrar(contenedorTurno, hayCitas);
        Ui.mostrar(lblSinTurno, !hayCitas);
        if (hayCitas) {
            // primer hueco libre entre dos citas consecutivas
            String hueco = null;
            long minutos = 0;
            for (int i = 0; i + 1 < citasDelDia.size(); i++) {
                Row a = citasDelDia.get(i);
                Row b = citasDelDia.get(i + 1);
                LocalTime finA = LocalTime.parse(a.str("hora_inicio")).plusMinutes(a.integer("duracion_min"));
                LocalTime iniB = LocalTime.parse(b.str("hora_inicio"));
                if (finA.isBefore(iniB)) {
                    hueco = Fmt.hora(finA) + "\u2013" + Fmt.hora(iniB);
                    minutos = java.time.Duration.between(finA, iniB).toMinutes();
                    break;
                }
            }
            if (hueco != null) {
                lblIntervaloLibre.setText(hueco);
                lblIntervaloLibreTexto.setText("Intervalo libre de " + minutos + " min: úsalo para notas pendientes "
                        + "o descanso.");
            } else {
                lblIntervaloLibre.setText("Sin huecos");
                lblIntervaloLibreTexto.setText("No hay intervalos libres entre tus citas de este día.");
            }
            if (prox != null) {
                lblPreparacionTitulo.setText(Fmt.rango(prox.str("hora_inicio"), prox.integer("duracion_min"))
                        + " · " + prox.str("mascota"));
                lblPreparacionTexto.setText("Revisa el expediente de " + prox.str("mascota")
                        + " y prepara el consultorio antes de la cita.");
            } else {
                lblPreparacionTitulo.setText("Sin citas por atender");
                lblPreparacionTexto.setText("Ya atendiste o iniciaste todas las citas de este día.");
            }
        }

        boolean hayProx = prox != null;
        Ui.mostrar(contenedorProxima, hayProx);
        Ui.mostrar(lblSinProxima, !hayProx);
        if (hayProx) {
            lblSiguienteHora.setText("Siguiente · " + prox.str("hora_inicio"));
            lblProximaMascota.setText(prox.str("mascota"));
            lblProximaPropietaria.setText(prox.str("propietario"));
            lblProximaServicio.setText(prox.str("servicio"));
            lblProximaHorario.setText(Fmt.rango(prox.str("hora_inicio"), prox.integer("duracion_min")));
            lblProximaConsultorio.setText(Fmt.nvl(prox.str("consultorio"), "—"));
            lblProximaMedico.setText(Sesion.nombreCompleto);
        }
    }
}
