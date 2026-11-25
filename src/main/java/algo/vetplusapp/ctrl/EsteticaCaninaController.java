package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.AtencionRepo;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.RecibeDatos;
import algo.vetplusapp.util.Sesion;
import algo.vetplusapp.util.Ui;
import algo.vetplusapp.util.Valida;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Estetica canina (4.17): jornada de la estilista y formulario del servicio en curso. */
public class EsteticaCaninaController implements RecibeDatos {

    @FXML
    private Label lblSubtituloEstetica;

    @FXML
    private Label lblTurno;

    @FXML
    private Label lblProgramadosHoy;

    @FXML
    private Label lblDetalleProgramados;

    @FXML
    private Label lblEnProceso;

    @FXML
    private Label lblDetalleEnProceso;

    @FXML
    private Label lblFinalizados;

    @FXML
    private Label lblDetalleFinalizados;

    @FXML
    private TableView<Row> tblProgramados;

    @FXML
    private TableColumn<Row, String> colHora;

    @FXML
    private TableColumn<Row, String> colPaciente;

    @FXML
    private TableColumn<Row, String> colCuidado;

    @FXML
    private TableColumn<Row, String> colEstado;

    @FXML
    private Label lblPropietarioEntrega;

    @FXML
    private Label lblContactoEntrega;

    @FXML
    private Label lblEntregaEstimada;

    @FXML
    private Label lblNotaEntrega;

    @FXML
    private Label lblEstadoServicio;

    @FXML
    private Label lblSinServicio;

    @FXML
    private VBox contenedorServicioCurso;

    @FXML
    private Label lblServicioMascota;

    @FXML
    private Label lblServicioDetalle;

    @FXML
    private HBox cajaAvisoMascota;

    @FXML
    private Label lblAvisoMascota;

    @FXML
    private TextField txtServicioEstetica;

    @FXML
    private TextField txtTarifaEstetica;

    @FXML
    private TextArea txtPreferencias;

    @FXML
    private CheckBox chkBanoSecado;

    @FXML
    private CheckBox chkOidos;

    @FXML
    private CheckBox chkCorte;

    @FXML
    private CheckBox chkRevisionUnas;

    @FXML
    private TextArea txtObservaciones;

    @FXML
    private Button btnGuardarAvance;

    @FXML
    private Button btnFinalizarEstetica;

    private List<Row> servicios;
    private Row elegido;
    private boolean llenando = false;
    private String iniSubtitulo;
    private String iniPropietario;
    private String iniContacto;
    private String iniEntrega;
    private String iniNotaEntrega;

    @FXML
    private void initialize() {
        iniSubtitulo = lblSubtituloEstetica.getText();
        iniPropietario = lblPropietarioEntrega.getText();
        iniContacto = lblContactoEntrega.getText();
        iniEntrega = lblEntregaEstimada.getText();
        iniNotaEntrega = lblNotaEntrega.getText();

        lblSubtituloEstetica.setText(iniSubtitulo + " · " + Fmt.fechaDiaMes(LocalDate.now()) + " · "
                + Sesion.nombreCompleto);
        if (!Sesion.turno.isEmpty()) {
            lblTurno.setText(Sesion.nombreCompleto + " · Turno " + Sesion.turno.toLowerCase());
            Ui.mostrar(lblTurno, true);
        } else {
            Ui.mostrar(lblTurno, false);
        }

        Ui.col(colHora, r -> r.str("hora_inicio"));
        Ui.col(colPaciente, r -> r.str("mascota") + " / " + r.str("propietario"));
        Ui.col(colCuidado, "servicio");
        Ui.col(colEstado, r -> estadoTexto(r.str("estado")));

        txtServicioEstetica.setEditable(false);
        txtTarifaEstetica.setEditable(false);

        tblProgramados.getSelectionModel().selectedItemProperty().addListener((o, a, fila) -> {
            if (!llenando) {
                if (fila == null) {
                    limpiarServicio();
                } else {
                    mostrarServicio(fila.id());
                }
            }
        });
        btnGuardarAvance.setOnAction(e -> Ui.accion(this::guardarAvance));
        btnFinalizarEstetica.setOnAction(e -> Ui.accion(this::finalizar));

        limpiarServicio();
        cargarLista(null);
    }

    @Override
    public void cargar(Object dato) {
        if (dato instanceof Long) {
            cargarLista((Long) dato);
        }
    }

    static String estadoTexto(String estadoCita) {
        switch (estadoCita) {
            case "Confirmada":
                return "Programado";
            case "Finalizada":
                return "Finalizado";
            default:
                return estadoCita;
        }
    }

    private void cargarLista(Long seleccionar) {
        servicios = AtencionRepo.esteticaDelDia(Sesion.usuarioId, Config.hoy());
        int programados = 0;
        int enProceso = 0;
        int finalizados = 0;
        String nombreEnProceso = null;
        Row autoSeleccion = null;
        Set<Long> notificados = new HashSet<>();
        for (Row s : servicios) {
            switch (s.str("estado")) {
                case "En proceso":
                    enProceso++;
                    nombreEnProceso = s.str("mascota");
                    if (autoSeleccion == null) {
                        autoSeleccion = s;
                    }
                    break;
                case "Finalizada":
                    finalizados++;
                    if (s.bool("propietario_notificado")) {
                        notificados.add(s.lng("propietario_id"));
                    }
                    break;
                default:
                    programados++;
                    break;
            }
        }
        lblProgramadosHoy.setText(String.valueOf(servicios.size()));
        lblDetalleProgramados.setText(servicios.isEmpty() ? "Sin servicios programados"
                : programados + (programados == 1 ? " por iniciar" : " por iniciar"));
        lblEnProceso.setText(String.valueOf(enProceso));
        lblDetalleEnProceso.setText(nombreEnProceso == null ? "Sin servicio en proceso" : nombreEnProceso);
        lblFinalizados.setText(String.valueOf(finalizados));
        lblDetalleFinalizados.setText(finalizados == 0 ? "Sin servicios finalizados"
                : Fmt.plural(notificados.size(), "propietario notificado", "propietarios notificados"));

        llenando = true;
        tblProgramados.getItems().setAll(servicios);
        llenando = false;

        Long idElegir = seleccionar != null ? seleccionar : (autoSeleccion == null ? null : autoSeleccion.id());
        if (idElegir != null) {
            for (Row s : servicios) {
                if (s.id() == idElegir) {
                    llenando = true;
                    tblProgramados.getSelectionModel().select(s);
                    llenando = false;
                    mostrarServicio(s.id());
                    return;
                }
            }
        }
        limpiarServicio();
    }

    private void limpiarServicio() {
        elegido = null;
        lblPropietarioEntrega.setText(iniPropietario);
        lblContactoEntrega.setText(iniContacto);
        lblEntregaEstimada.setText(iniEntrega);
        lblNotaEntrega.setText(iniNotaEntrega);
        Ui.mostrar(lblEstadoServicio, false);
        Ui.mostrar(contenedorServicioCurso, false);
        Ui.mostrar(lblSinServicio, true);
    }

    private void mostrarServicio(long citaId) {
        Row s = AtencionRepo.esteticaDeCita(citaId);
        if (s == null || s.lng("profesional_id") != Sesion.usuarioId) {
            limpiarServicio();
            return;
        }
        elegido = s;
        boolean cerrado = "Finalizada".equals(s.str("estado"));
        lblPropietarioEntrega.setText(s.str("propietario"));
        lblContactoEntrega.setText("Tel. " + Fmt.nvl(s.str("prop_telefono"), "—"));
        lblEntregaEstimada.setText("Entrega estimada: " + AtencionRepo.entregaEstimada(s));
        lblNotaEntrega.setText(cerrado ? "Servicio finalizado: el propietario ya puede recoger a la mascota."
                : iniNotaEntrega);

        lblEstadoServicio.setText(estadoTexto(s.str("estado")));
        Ui.mostrar(lblEstadoServicio, true);
        Ui.mostrar(lblSinServicio, false);
        Ui.mostrar(contenedorServicioCurso, true);

        lblServicioMascota.setText(s.str("mascota"));
        lblServicioDetalle.setText(Fmt.nvl(s.str("raza"), "Mascota") + " · Propietario: " + s.str("propietario"));
        if (!s.str("nota_estetica").isEmpty()) {
            lblAvisoMascota.setText(s.str("nota_estetica"));
            Ui.mostrar(cajaAvisoMascota, true);
        } else {
            Ui.mostrar(cajaAvisoMascota, false);
        }
        txtServicioEstetica.setText(s.str("servicio"));
        txtTarifaEstetica.setText(Fmt.dinero(s.dbl("tarifa")));
        txtPreferencias.setText(s.str("preferencias"));
        chkBanoSecado.setSelected(s.bool("chk_bano_secado"));
        chkOidos.setSelected(s.bool("chk_oidos"));
        chkCorte.setSelected(s.bool("chk_corte"));
        chkRevisionUnas.setSelected(s.bool("chk_revision_unas"));
        txtObservaciones.setText(s.str("observaciones"));

        for (javafx.scene.Node n : new javafx.scene.Node[]{txtPreferencias, chkBanoSecado, chkOidos, chkCorte,
            chkRevisionUnas, txtObservaciones, btnGuardarAvance, btnFinalizarEstetica}) {
            n.setDisable(cerrado);
        }
    }

    private void guardarAvance() {
        if (elegido == null) {
            Ui.aviso("Elige un servicio de la lista.");
            return;
        }
        AtencionRepo.guardarAvance(elegido.id(), Valida.t(txtPreferencias.getText()), chkBanoSecado.isSelected(),
                chkOidos.isSelected(), chkCorte.isSelected(), chkRevisionUnas.isSelected(),
                Valida.t(txtObservaciones.getText()));
        cargarLista(elegido.id());
        Ui.info("Avance guardado.");
    }

    private void finalizar() {
        if (elegido == null) {
            Ui.aviso("Elige un servicio de la lista.");
            return;
        }
        if (!Ui.confirmar("¿Finalizar el servicio de " + elegido.str("mascota")
                + "? Se agregará a la cuenta de " + elegido.str("propietario") + ".")) {
            return;
        }
        AtencionRepo.finalizarEstetica(elegido.id(), Valida.t(txtPreferencias.getText()),
                chkBanoSecado.isSelected(), chkOidos.isSelected(), chkCorte.isSelected(),
                chkRevisionUnas.isSelected(), Valida.t(txtObservaciones.getText()));
        cargarLista(null);
        limpiarServicio();
        llenando = true;
        tblProgramados.getSelectionModel().clearSelection();
        llenando = false;
        Ui.info("Servicio finalizado. Avisa al propietario para la entrega.");
    }
}
