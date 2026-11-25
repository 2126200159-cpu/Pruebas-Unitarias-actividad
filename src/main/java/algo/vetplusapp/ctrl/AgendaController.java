package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.CitaRepo;
import algo.vetplusapp.repo.ClienteRepo;
import algo.vetplusapp.repo.ServicioRepo;
import algo.vetplusapp.repo.UsuarioRepo;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.RecibeDatos;
import algo.vetplusapp.util.Ui;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DateCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

/** Agenda general (4.6): citas del dia/semana/mes y formulario de nueva cita (o estancia de pension). */
public class AgendaController implements RecibeDatos {

    @FXML
    private ToggleGroup grupoVista;

    @FXML
    private ToggleGroup grupoDias;

    @FXML
    private Button btnNuevaCita;

    @FXML
    private Button btnDiaAnterior;

    @FXML
    private Label lblFechaActual;

    @FXML
    private Button btnDiaSiguiente;

    @FXML
    private ToggleButton tglVistaDia;

    @FXML
    private ToggleButton tglVistaSemana;

    @FXML
    private ToggleButton tglVistaMes;

    @FXML
    private ToggleButton tglDia1;

    @FXML
    private Label lblNumDia1;

    @FXML
    private ToggleButton tglDia2;

    @FXML
    private Label lblNumDia2;

    @FXML
    private ToggleButton tglDia3;

    @FXML
    private Label lblNumDia3;

    @FXML
    private ToggleButton tglDia4;

    @FXML
    private Label lblNumDia4;

    @FXML
    private ToggleButton tglDia5;

    @FXML
    private Label lblNumDia5;

    @FXML
    private ToggleButton tglDia6;

    @FXML
    private Label lblNumDia6;

    @FXML
    private Button btnFiltroProfesional;

    @FXML
    private Button btnFiltroServicio;

    @FXML
    private Label lblTotalCitas;

    @FXML
    private TableView<Row> tblAgenda;

    @FXML
    private TableColumn<Row, String> colHora;

    @FXML
    private TableColumn<Row, String> colPaciente;

    @FXML
    private TableColumn<Row, String> colServicio;

    @FXML
    private TableColumn<Row, String> colProfesional;

    @FXML
    private TableColumn<Row, String> colEstado;

    @FXML
    private Label lblCitasMostradas;

    @FXML
    private Hyperlink lnkMostrarTodas;

    @FXML
    private ComboBox<Row> cmbPropietario;

    @FXML
    private ComboBox<Row> cmbMascota;

    @FXML
    private ComboBox<Row> cmbServicio;

    @FXML
    private ComboBox<Row> cmbProfesional;

    @FXML
    private DatePicker dtpFecha;

    @FXML
    private ComboBox<String> cmbHora;

    @FXML
    private TextArea txtMotivo;

    @FXML
    private HBox cajaAvisoHorario;

    @FXML
    private Label lblAvisoHorario;

    @FXML
    private Button btnGuardarCita;

    @FXML
    private Button btnCancelarCita;

    private static final int LIMITE_FILAS = 8;

    private LocalDate dia = siguienteDiaHabil(LocalDate.now());
    private Row filtroProfesional;
    private Row filtroServicio;
    private boolean mostrarTodas = false;
    private boolean llenando = false;
    private ToggleButton[] botonesDia;
    private Label[] numerosDia;

    private static LocalDate siguienteDiaHabil(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SUNDAY ? d.plusDays(1) : d;
    }

    @FXML
    private void initialize() {
        botonesDia = new ToggleButton[]{tglDia1, tglDia2, tglDia3, tglDia4, tglDia5, tglDia6};
        numerosDia = new Label[]{lblNumDia1, lblNumDia2, lblNumDia3, lblNumDia4, lblNumDia5, lblNumDia6};

        Ui.col(colHora, r -> (esRango() ? Fmt.fechaAbrDM(Fmt.fecha(r.str("fecha"))) + " · " : "")
                + Fmt.rango(r.str("hora_inicio"), r.integer("duracion_min")));
        Ui.col(colPaciente, r -> r.str("mascota") + " / " + r.str("propietario"));
        Ui.col(colServicio, "servicio");
        Ui.col(colProfesional, "profesional");
        Ui.col(colEstado, "estado");

        cmbPropietario.setConverter(Dialogos.convertidor("nombre_completo"));
        cmbMascota.setConverter(Dialogos.convertidor("nombre"));
        cmbProfesional.setConverter(Dialogos.convertidor("nombre_completo"));
        cmbServicio.setConverter(new javafx.util.StringConverter<Row>() {
            @Override
            public String toString(Row r) {
                return r == null ? "" : r.str("nombre") + " · " + Fmt.dinero(r.dbl("precio"));
            }

            @Override
            public Row fromString(String s) {
                return null;
            }
        });
        cmbPropietario.getItems().setAll(ClienteRepo.propietariosActivos());
        cmbServicio.getItems().setAll(ServicioRepo.activos());

        // no se pueden elegir fechas pasadas ni domingos
        dtpFecha.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && (item.isBefore(LocalDate.now()) || item.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                    setDisable(true);
                }
            }
        });

        Ui.mostrar(cajaAvisoHorario, false);

        btnNuevaCita.setOnAction(e -> cmbPropietario.requestFocus());
        btnDiaAnterior.setOnAction(e -> moverDia(-1));
        btnDiaSiguiente.setOnAction(e -> moverDia(1));
        Ui.grupoSiempreUno(grupoVista, t -> cargarTabla());
        Ui.grupoSiempreUno(grupoDias, t -> {
            int i = java.util.Arrays.asList(botonesDia).indexOf(t);
            if (i >= 0 && !llenando) {
                dia = lunesDe(dia).plusDays(i);
                tglVistaDia.setSelected(true);
                cargarTabla();
            }
        });

        btnFiltroProfesional.setOnAction(e -> menuFiltro(btnFiltroProfesional, "Todos los profesionales",
                UsuarioRepo.todos(), "nombre_completo", r -> filtroProfesional = r));
        btnFiltroServicio.setOnAction(e -> menuFiltro(btnFiltroServicio, "Todos los servicios",
                ServicioRepo.todos(), "nombre", r -> filtroServicio = r));
        lnkMostrarTodas.setOnAction(e -> {
            mostrarTodas = !mostrarTodas;
            lnkMostrarTodas.setText(mostrarTodas ? "Mostrar menos" : "Mostrar todas");
            cargarTabla();
        });

        cmbPropietario.valueProperty().addListener((o, a, p) -> {
            cmbMascota.getItems().setAll(p == null ? new ArrayList<Row>() : ClienteRepo.mascotasActivasDe(p.id()));
            cmbMascota.setValue(null);
        });
        cmbServicio.valueProperty().addListener((o, a, s) -> alElegirServicio(s));
        cmbProfesional.valueProperty().addListener((o, a, p) -> actualizarHoras());
        dtpFecha.valueProperty().addListener((o, a, d) -> actualizarHoras());
        cmbHora.valueProperty().addListener((o, a, h) -> actualizarAviso());

        btnGuardarCita.setOnAction(e -> Ui.accion(this::guardarCita));
        btnCancelarCita.setOnAction(e -> limpiarFormulario());

        tglVistaDia.setSelected(true);
        pintarDias();
        cargarTabla();
    }

    @Override
    public void cargar(Object dato) {
        if ("nuevaCita".equals(dato)) {
            cmbPropietario.requestFocus();
        }
    }

    // ------------------------------------------------------------------ dias y tabla

    private boolean esRango() {
        return !tglVistaDia.isSelected();
    }

    private static LocalDate lunesDe(LocalDate d) {
        return d.minusDays(d.getDayOfWeek().getValue() - 1L);
    }

    private void moverDia(int pasos) {
        dia = dia.plusDays(pasos);
        if (dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dia = dia.plusDays(pasos);
        }
        tglVistaDia.setSelected(true);
        pintarDias();
        cargarTabla();
    }

    private void pintarDias() {
        LocalDate lunes = lunesDe(dia);
        llenando = true;
        for (int i = 0; i < 6; i++) {
            numerosDia[i].setText(String.valueOf(lunes.plusDays(i).getDayOfMonth()));
        }
        int idx = dia.getDayOfWeek().getValue() - 1;
        botonesDia[idx].setSelected(true);
        llenando = false;
    }

    private void cargarTabla() {
        LocalDate desde = dia;
        LocalDate hasta = dia;
        if (tglVistaSemana.isSelected()) {
            desde = lunesDe(dia);
            hasta = desde.plusDays(5);
            lblFechaActual.setText("Semana del " + Fmt.fechaMedia(desde) + " al " + Fmt.fechaMedia(hasta));
        } else if (tglVistaMes.isSelected()) {
            desde = dia.withDayOfMonth(1);
            hasta = dia.withDayOfMonth(dia.lengthOfMonth());
            lblFechaActual.setText(Fmt.fechaMedia(desde).substring(Fmt.fechaMedia(desde).indexOf(' ') + 1));
        } else {
            lblFechaActual.setText(Fmt.fechaMedia(dia));
        }
        pintarDias();
        List<Row> citas = CitaRepo.citas(desde.toString(), hasta.toString(),
                filtroProfesional == null ? null : filtroProfesional.id(),
                filtroServicio == null ? null : filtroServicio.id());
        int total = citas.size();
        List<Row> visibles = mostrarTodas || total <= LIMITE_FILAS ? citas : citas.subList(0, LIMITE_FILAS);
        tblAgenda.getItems().setAll(visibles);
        tblAgenda.refresh();
        lblTotalCitas.setText(Fmt.plural(total, "cita", "citas"));
        lblCitasMostradas.setText(visibles.size() + " de " + total + " citas ·");
        Ui.mostrar(lnkMostrarTodas, total > LIMITE_FILAS);
    }

    private void menuFiltro(javafx.scene.control.Button boton, String textoTodos, List<Row> opciones, String columna,
            java.util.function.Consumer<Row> alElegir) {
        ContextMenu cm = new ContextMenu();
        MenuItem todos = new MenuItem(textoTodos);
        todos.setOnAction(e -> {
            alElegir.accept(null);
            boton.setText(textoTodos);
            cargarTabla();
        });
        cm.getItems().add(todos);
        for (Row r : opciones) {
            MenuItem mi = new MenuItem(r.str(columna));
            mi.setOnAction(e -> {
                alElegir.accept(r);
                boton.setText(r.str(columna));
                cargarTabla();
            });
            cm.getItems().add(mi);
        }
        cm.show(boton, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    // ------------------------------------------------------------------ formulario

    private boolean esPension() {
        return cmbServicio.getValue() != null && "PENSION".equals(cmbServicio.getValue().str("tipo"));
    }

    private void alElegirServicio(Row s) {
        cmbProfesional.setValue(null);
        cmbHora.getItems().clear();
        cmbHora.setValue(null);
        if (s == null) {
            cmbProfesional.getItems().clear();
            actualizarAviso();
            return;
        }
        boolean pension = "PENSION".equals(s.str("tipo"));
        cmbProfesional.setDisable(pension);
        cmbHora.setDisable(pension);
        if (pension) {
            cmbProfesional.getItems().clear();
        } else {
            String rol = "MEDICO".equals(s.str("tipo")) ? "MEDICO" : "ESTILISTA";
            cmbProfesional.getItems().setAll(UsuarioRepo.profesionalesActivos(rol));
        }
        actualizarAviso();
    }

    private void actualizarHoras() {
        cmbHora.getItems().clear();
        cmbHora.setValue(null);
        Row s = cmbServicio.getValue();
        Row p = cmbProfesional.getValue();
        if (s != null && p != null && dtpFecha.getValue() != null && !esPension()) {
            int dur = s.integer("duracion_min") > 0 ? s.integer("duracion_min") : 30;
            cmbHora.getItems().setAll(CitaRepo.horariosLibres(p.id(), dtpFecha.getValue().toString(), dur));
        }
        actualizarAviso();
    }

    private void actualizarAviso() {
        Row s = cmbServicio.getValue();
        Row p = cmbProfesional.getValue();
        if (s == null || p == null || dtpFecha.getValue() == null || cmbHora.getValue() == null || esPension()) {
            Ui.mostrar(cajaAvisoHorario, false);
            return;
        }
        int dur = s.integer("duracion_min") > 0 ? s.integer("duracion_min") : 30;
        boolean ocupado = CitaRepo.hayConflicto(p.id(), dtpFecha.getValue().toString(), cmbHora.getValue(), dur, null);
        lblAvisoHorario.setText(ocupado ? "Ese horario ya está ocupado."
                : "Horario disponible · Duración estimada: " + dur + " min.");
        Ui.mostrar(cajaAvisoHorario, true);
        btnGuardarCita.setDisable(ocupado);
    }

    private void guardarCita() {
        if (cmbPropietario.getValue() == null || cmbMascota.getValue() == null || cmbServicio.getValue() == null
                || dtpFecha.getValue() == null) {
            Ui.aviso("Completa propietario, mascota, servicio y fecha.");
            return;
        }
        if (esPension()) {
            LocalDate salida = Dialogos.fecha("Fecha de salida de la pensión", dtpFecha.getValue().plusDays(1));
            if (salida == null) {
                return;
            }
            CitaRepo.crearEstancia(cmbMascota.getValue().id(), dtpFecha.getValue(), salida);
            Ui.info("Estancia registrada. Los días de pensión se agregaron a la cuenta del propietario.");
        } else {
            if (cmbProfesional.getValue() == null || cmbHora.getValue() == null) {
                Ui.aviso("Elige el profesional y la hora.");
                return;
            }
            CitaRepo.crear(cmbMascota.getValue().id(), cmbServicio.getValue().id(), cmbProfesional.getValue().id(),
                    dtpFecha.getValue().toString(), cmbHora.getValue(), txtMotivo.getText());
            Ui.info("Cita guardada.");
        }
        dia = siguienteDiaHabil(dtpFecha.getValue());
        limpiarFormulario();
        tglVistaDia.setSelected(true);
        cargarTabla();
    }

    private void limpiarFormulario() {
        cmbPropietario.setValue(null);
        cmbMascota.getItems().clear();
        cmbServicio.setValue(null);
        cmbProfesional.getItems().clear();
        cmbProfesional.setDisable(false);
        cmbHora.setDisable(false);
        cmbHora.getItems().clear();
        cmbHora.setValue(null);
        dtpFecha.setValue(null);
        txtMotivo.clear();
        btnGuardarCita.setDisable(false);
        Ui.mostrar(cajaAvisoHorario, false);
    }
}
