package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.AtencionRepo;
import algo.vetplusapp.repo.ClienteRepo;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Nav;
import algo.vetplusapp.util.Sesion;
import algo.vetplusapp.util.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Mis pacientes (estilista, 4.18). Privacidad: esta vista SOLO lee datos de estetica; nunca consulta
 * diagnosticos, notas medicas ni historial clinico.
 */
public class MisPacientesController {

    @FXML
    private Label lblSubtituloPacientes;

    @FXML
    private Label lblResumenPacientes;

    @FXML
    private TextField txtBuscarPaciente;

    @FXML
    private ComboBox<String> cmbEstadoServicio;

    @FXML
    private ComboBox<String> cmbAsignacion;

    @FXML
    private TableView<Row> tblPacientesAsignados;

    @FXML
    private TableColumn<Row, String> colHoraPac;

    @FXML
    private TableColumn<Row, String> colPacientePropietario;

    @FXML
    private TableColumn<Row, String> colCuidadoPac;

    @FXML
    private TableColumn<Row, String> colEstadoPac;

    @FXML
    private Label lblSinProximaEst;

    @FXML
    private VBox contenedorProximaEst;

    @FXML
    private Label lblProximaTitulo;

    @FXML
    private Label lblProximaDetalle;

    @FXML
    private Label lblProximaNota;

    @FXML
    private Label lblTituloFicha;

    @FXML
    private Label lblSinFicha;

    @FXML
    private VBox contenedorFicha;

    @FXML
    private Label lblFichaMascota;

    @FXML
    private Label lblFichaDetalle;

    @FXML
    private Label lblFichaEstado;

    @FXML
    private HBox cajaAvisoFicha;

    @FXML
    private Label lblAvisoFicha;

    @FXML
    private Label lblPreferenciasCorte;

    @FXML
    private Label lblFichaPropietario;

    @FXML
    private Label lblFichaTelefono;

    @FXML
    private Label lblFichaCorreo;

    @FXML
    private Label lblFichaFecha;

    @FXML
    private Label lblFichaEntrega;

    @FXML
    private Label lblFichaEstilista;

    @FXML
    private Button btnContinuarServicio;

    @FXML
    private VBox contenedorHistorialEst;

    @FXML
    private Label lblSinHistorialEst;

    @FXML
    private VBox contenedorAvance;

    @FXML
    private CheckBox chkAvanceBano;

    @FXML
    private CheckBox chkAvanceOidos;

    @FXML
    private CheckBox chkAvanceCorte;

    @FXML
    private CheckBox chkAvanceRevision;

    @FXML
    private CheckBox chkAvanceUnas;

    @FXML
    private Label lblSinEntrega;

    @FXML
    private VBox contenedorEntrega;

    @FXML
    private Label lblRecomendacionEntrega;

    @FXML
    private Label lblAvisoEntrega;

    private static final String TODOS_ESTADOS = "Todos los estados";

    private List<Row> asignados = new ArrayList<>();
    private boolean llenando = false;

    @FXML
    private void initialize() {
        Ui.col(colHoraPac, r -> r.str("hora_inicio"));
        Ui.col(colPacientePropietario, r -> r.str("mascota") + " / " + r.str("propietario"));
        Ui.col(colCuidadoPac, "servicio");
        Ui.col(colEstadoPac, r -> EsteticaCaninaController.estadoTexto(r.str("estado")));

        cmbEstadoServicio.getItems().setAll(TODOS_ESTADOS, "Programado", "En proceso", "Finalizado");
        cmbEstadoServicio.setValue(TODOS_ESTADOS);
        cmbAsignacion.getItems().setAll("Mis pacientes · Hoy");
        cmbAsignacion.setValue("Mis pacientes · Hoy");

        txtBuscarPaciente.textProperty().addListener((o, a, n) -> filtrar());
        cmbEstadoServicio.valueProperty().addListener((o, a, n) -> filtrar());
        tblPacientesAsignados.getSelectionModel().selectedItemProperty().addListener((o, a, fila) -> {
            if (!llenando && fila != null) {
                mostrarFicha(fila);
            }
        });
        btnContinuarServicio.setOnAction(e -> {
            Row s = tblPacientesAsignados.getSelectionModel().getSelectedItem();
            if (s != null) {
                Nav.ir("estetica-canina", s.id());
            }
        });

        Ui.mostrar(contenedorFicha, false);
        Ui.mostrar(lblSinFicha, true);
        Ui.mostrar(contenedorEntrega, false);
        Ui.mostrar(lblSinEntrega, true);
        Ui.mostrar(contenedorAvance, false);
        Ui.mostrar(cajaAvisoFicha, false);
        Ui.mostrar(btnContinuarServicio, false);
        for (javafx.scene.control.CheckBox c : new javafx.scene.control.CheckBox[]{chkAvanceBano, chkAvanceOidos,
            chkAvanceCorte, chkAvanceRevision, chkAvanceUnas}) {
            c.setMouseTransparent(true); // solo lectura: no responden al clic
            c.setFocusTraversable(false);
        }
        Ui.llenarLista(contenedorHistorialEst, lblSinHistorialEst, new ArrayList<Node>());

        asignados = AtencionRepo.esteticaDelDia(Sesion.usuarioId, Config.hoy());
        filtrar();
        mostrarProxima();
    }

    private void filtrar() {
        String q = txtBuscarPaciente.getText() == null ? "" : txtBuscarPaciente.getText().trim()
                .toLowerCase(Locale.ROOT);
        String estado = cmbEstadoServicio.getValue();
        List<Row> visibles = new ArrayList<>();
        for (Row s : asignados) {
            if (estado != null && !TODOS_ESTADOS.equals(estado)
                    && !estado.equals(EsteticaCaninaController.estadoTexto(s.str("estado")))) {
                continue;
            }
            String texto = (s.str("mascota") + " " + s.str("propietario") + " " + s.str("prop_telefono"))
                    .toLowerCase(Locale.ROOT);
            if (q.isEmpty() || texto.contains(q)) {
                visibles.add(s);
            }
        }
        llenando = true;
        tblPacientesAsignados.getItems().setAll(visibles);
        llenando = false;
        lblResumenPacientes.setText(visibles.size() + (visibles.size() == 1 ? " paciente · Hoy"
                : " pacientes · Hoy"));
    }

    private void mostrarProxima() {
        Row prox = null;
        for (Row s : asignados) {
            if ("Confirmada".equals(s.str("estado"))) {
                prox = s;
                break;
            }
        }
        Ui.mostrar(contenedorProximaEst, prox != null);
        Ui.mostrar(lblSinProximaEst, prox == null);
        if (prox != null) {
            lblProximaTitulo.setText(prox.str("mascota") + " · " + prox.str("servicio"));
            lblProximaDetalle.setText(Fmt.rango(prox.str("hora_inicio"), prox.integer("duracion_min")) + " · "
                    + prox.str("propietario"));
            lblProximaNota.setText(Fmt.nvl(prox.str("nota_estetica"), "Sin notas de estética."));
        }
    }

    private void mostrarFicha(Row s) {
        boolean abierto = !"Finalizada".equals(s.str("estado"));
        lblTituloFicha.setText(s.str("mascota") + " · Ficha de estética");
        Ui.mostrar(contenedorFicha, true);
        Ui.mostrar(lblSinFicha, false);
        lblFichaMascota.setText(s.str("mascota"));
        lblFichaDetalle.setText(Fmt.nvl(s.str("raza"), "Mascota") + " · " + s.str("servicio"));
        lblFichaEstado.setText(EsteticaCaninaController.estadoTexto(s.str("estado")));
        if (!s.str("nota_estetica").isEmpty()) {
            lblAvisoFicha.setText(s.str("nota_estetica"));
            Ui.mostrar(cajaAvisoFicha, true);
        } else {
            Ui.mostrar(cajaAvisoFicha, false);
        }
        lblPreferenciasCorte.setText(Fmt.nvl(s.str("preferencias"), "Sin preferencias registradas."));
        lblFichaPropietario.setText(s.str("propietario"));
        lblFichaTelefono.setText(Fmt.nvl(s.str("prop_telefono"), "—"));
        lblFichaCorreo.setText(Fmt.nvl(s.str("prop_correo"), "—"));
        lblFichaFecha.setText(Fmt.fechaDMA(Fmt.fecha(s.str("fecha"))));
        lblFichaEntrega.setText(AtencionRepo.entregaEstimada(s));
        lblFichaEstilista.setText(Sesion.nombreCompleto);
        Ui.mostrar(btnContinuarServicio, abierto); // solo si tiene un servicio abierto hoy

        // historial: solo servicios de estetica de esta mascota
        List<Node> eventos = new ArrayList<>();
        for (Row h : ClienteRepo.historialEstetica(s.lng("mascota_id"))) {
            Parent item = Ui.cargarPlantilla("evento-item.fxml");
            Ui.texto(item, "lblEventoFecha", Fmt.fechaEvento(Fmt.fecha(h.str("fecha")), h.str("hora_inicio")));
            Ui.texto(item, "lblEventoTitulo", h.str("servicio") + " · "
                    + EsteticaCaninaController.estadoTexto(h.str("estado")));
            Ui.texto(item, "lblEventoProfesional", h.str("profesional"));
            Ui.texto(item, "lblEventoNota", Fmt.nvl(h.str("observaciones"), "Sin observaciones."));
            eventos.add(item);
        }
        Ui.llenarLista(contenedorHistorialEst, lblSinHistorialEst, eventos);

        // avance del servicio actual (solo lectura)
        Ui.mostrar(contenedorAvance, abierto);
        chkAvanceBano.setSelected(s.bool("chk_bano_secado"));
        chkAvanceOidos.setSelected(s.bool("chk_oidos"));
        chkAvanceCorte.setSelected(s.bool("chk_corte"));
        chkAvanceRevision.setSelected(s.bool("chk_revision_unas"));
        chkAvanceUnas.setSelected(s.bool("chk_revision_unas"));

        // recomendaciones de entrega
        Ui.mostrar(contenedorEntrega, true);
        Ui.mostrar(lblSinEntrega, false);
        lblRecomendacionEntrega.setText("Entrega estimada: " + AtencionRepo.entregaEstimada(s)
                + (s.str("nota_estetica").isEmpty() ? "" : ". Ten en cuenta: " + s.str("nota_estetica")));
        lblAvisoEntrega.setText("Avisa a " + s.str("propietario") + " (Tel. " + Fmt.nvl(s.str("prop_telefono"), "—")
                + ") cuando el servicio esté finalizado.");
    }
}
