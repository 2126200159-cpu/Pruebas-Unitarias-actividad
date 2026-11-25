package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.AtencionRepo;
import algo.vetplusapp.repo.CitaRepo;
import algo.vetplusapp.repo.ClienteRepo;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Nav;
import algo.vetplusapp.util.RecibeDatos;
import algo.vetplusapp.util.Sesion;
import algo.vetplusapp.util.Ui;
import algo.vetplusapp.util.ImagenStore;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Expedientes (medico, 4.15): busqueda de pacientes y ficha clinica. Muestra todos los pacientes de la clinica. */
public class ExpedientesMedicoController implements RecibeDatos {

    @FXML
    private Button btnRegistrarNota;

    @FXML
    private TextField txtBuscarPaciente;

    @FXML
    private ComboBox<String> cmbEspecie;

    @FXML
    private ComboBox<String> cmbPropietarioFiltro;

    @FXML
    private Label lblTituloPacientes;

    @FXML
    private VBox contenedorPacientes;

    @FXML
    private Label lblSinPacientes;

    @FXML
    private Label lblResumenPacientes;

    @FXML
    private Label lblPropietarioNombre;

    @FXML
    private Label lblPropietarioTel;

    @FXML
    private Label lblPropietarioCorreo;

    @FXML
    private Label lblPropietarioDireccion;

    @FXML
    private Label lblContactoAlterno;

    @FXML
    private Label lblTituloDatosGenerales;

    @FXML
    private ImageView imgRetratoPaciente;

    @FXML
    private Label lblEstadoPaciente;

    @FXML
    private Label lblEspecieRaza;

    @FXML
    private Label lblSexoEdadPeso;

    @FXML
    private Label lblNacimiento;

    @FXML
    private Label lblMicrochip;

    @FXML
    private Button btnAbrirAtencionCurso;

    @FXML
    private HBox cajaAvisoAntecedente;

    @FXML
    private Label lblAvisoAntecedente;

    @FXML
    private VBox contenedorNotas;

    @FXML
    private Label lblSinNotas;

    @FXML
    private VBox contenedorNotaActual;

    @FXML
    private Label lblIndicacionesActual;

    @FXML
    private Label lblProximaRevision;

    @FXML
    private Button btnContinuarAtencionExp;

    @FXML
    private Label lblAlimentacion;

    @FXML
    private TableView<Row> tblVacunasExp;

    @FXML
    private TableColumn<Row, String> colCuidadoExp;

    @FXML
    private TableColumn<Row, String> colUltimaAplicacionExp;

    @FXML
    private TableColumn<Row, String> colProximaFechaExp;

    @FXML
    private TableColumn<Row, String> colEstadoExp;

    @FXML
    private Label lblNotaVacunas;

    private static final String TODAS_ESPECIES = "Todas las especies";
    private static final String TODOS_PROPIETARIOS = "Todos los propietarios";

    private Row seleccionado;
    private boolean llenando = false;
    private String iniTitulo;
    private String iniNacimiento;
    private String iniMicrochip;
    private String iniAlimentacion;
    private String iniNotaVacunas;
    private String iniProximaRevision;
    private String iniIndicaciones;

    @FXML
    private void initialize() {
        iniTitulo = lblTituloDatosGenerales.getText();
        iniNacimiento = lblNacimiento.getText();
        iniMicrochip = lblMicrochip.getText();
        iniAlimentacion = lblAlimentacion.getText();
        iniNotaVacunas = lblNotaVacunas.getText();
        iniProximaRevision = lblProximaRevision.getText();
        iniIndicaciones = lblIndicacionesActual.getText();

        Ui.col(colCuidadoExp, "cuidado");
        Ui.col(colUltimaAplicacionExp, r -> r.str("ultima_aplicacion").isEmpty() ? "—"
                : Fmt.fechaDMA(Fmt.fecha(r.str("ultima_aplicacion"))));
        Ui.col(colProximaFechaExp, r -> r.str("proxima_fecha").isEmpty() ? "—"
                : Fmt.fechaDMA(Fmt.fecha(r.str("proxima_fecha"))));
        Ui.col(colEstadoExp, r -> ClienteRepo.estadoCuidado(r.str("proxima_fecha")));

        cmbEspecie.getItems().setAll(TODAS_ESPECIES, "Perro", "Gato", "Otro");
        cmbEspecie.setValue(TODAS_ESPECIES);
        Set<String> propietarios = new LinkedHashSet<>();
        for (Row p : ClienteRepo.pacientes()) {
            propietarios.add(p.str("propietario"));
        }
        cmbPropietarioFiltro.getItems().add(TODOS_PROPIETARIOS);
        cmbPropietarioFiltro.getItems().addAll(propietarios);
        cmbPropietarioFiltro.setValue(TODOS_PROPIETARIOS);

        txtBuscarPaciente.textProperty().addListener((o, a, n) -> listar());
        cmbEspecie.valueProperty().addListener((o, a, n) -> {
            if (!llenando) {
                listar();
            }
        });
        cmbPropietarioFiltro.valueProperty().addListener((o, a, n) -> {
            if (!llenando) {
                listar();
            }
        });

        btnRegistrarNota.setOnAction(e -> Ui.accion(this::registrarNota));
        btnAbrirAtencionCurso.setOnAction(e -> {
            Row c = CitaRepo.enAtencion(Sesion.usuarioId);
            if (c != null) {
                Nav.ir("consulta-medica", c.id());
            }
        });
        btnContinuarAtencionExp.setOnAction(e -> {
            if (seleccionado != null) {
                Row b = AtencionRepo.borradorDeMascota(seleccionado.id(), Sesion.usuarioId);
                if (b != null) {
                    Nav.ir("consulta-medica", b.lng("cita_id"));
                }
            }
        });

        limpiarFicha();
        listar();
    }

    @Override
    public void cargar(Object dato) {
        if (dato instanceof Long) {
            llenando = true;
            txtBuscarPaciente.clear();
            cmbEspecie.setValue(TODAS_ESPECIES);
            cmbPropietarioFiltro.setValue(TODOS_PROPIETARIOS);
            llenando = false;
            listar();
            seleccionar((Long) dato);
        }
    }

    // ------------------------------------------------------------------ lista

    private void listar() {
        String q = txtBuscarPaciente.getText() == null ? "" : txtBuscarPaciente.getText().trim()
                .toLowerCase(Locale.ROOT);
        String especie = cmbEspecie.getValue();
        String propietario = cmbPropietarioFiltro.getValue();
        List<Node> filas = new ArrayList<>();
        int total = 0;
        for (Row m : ClienteRepo.pacientes()) {
            if (especie != null && !TODAS_ESPECIES.equals(especie) && !especie.equals(m.str("especie"))) {
                continue;
            }
            if (propietario != null && !TODOS_PROPIETARIOS.equals(propietario)
                    && !propietario.equals(m.str("propietario"))) {
                continue;
            }
            String texto = (m.str("nombre") + " " + m.str("microchip") + " " + m.str("prop_telefono"))
                    .toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !texto.contains(q)) {
                continue;
            }
            total++;
            Parent item = Ui.cargarPlantilla("paciente-item.fxml");
            Ui.texto(item, "lblPacienteNombre", m.str("nombre"));
            Ui.texto(item, "lblPacienteDetalle", m.str("especie") + " · " + Fmt.nvl(m.str("raza"), "Sin raza")
                    + " · " + m.str("propietario"));
            boolean gato = "Gato".equals(m.str("especie"));
            Ui.mostrar(item.lookup("#iconoPerro"), !gato);
            Ui.mostrar(item.lookup("#iconoGato"), gato);
            long id = m.id();
            item.setOnMouseClicked(e -> seleccionar(id));
            item.setCursor(javafx.scene.Cursor.HAND);
            filas.add(item);
        }
        lblTituloPacientes.setText("Pacientes · " + total);
        lblResumenPacientes.setText(Fmt.plural(total, "paciente", "pacientes") + " en este listado");
        Ui.llenarLista(contenedorPacientes, lblSinPacientes, filas);
    }

    private void registrarNota() {
        if (seleccionado == null) {
            Ui.aviso("Elige un paciente de la lista.");
            return;
        }
        Row enCurso = CitaRepo.enAtencion(Sesion.usuarioId);
        if (enCurso != null && enCurso.lng("mascota_id") == seleccionado.id()) {
            Nav.ir("consulta-medica", enCurso.id());
            return;
        }
        if (enCurso != null) {
            Ui.aviso("Ya tienes una atención en curso con " + enCurso.str("mascota")
                    + ". Primero debes finalizarla.");
            return;
        }
        for (Row c : CitaRepo.citasDeProfesional(Sesion.usuarioId, algo.vetplusapp.util.Config.hoy())) {
            if ("Confirmada".equals(c.str("estado")) && c.lng("mascota_id") == seleccionado.id()) {
                if (Ui.confirmar("¿Iniciar la atención de " + seleccionado.str("nombre") + " (cita de "
                        + c.str("hora_inicio") + ")?")) {
                    CitaRepo.iniciarAtencion(c.id(), Sesion.usuarioId);
                    Nav.ir("consulta-medica", c.id());
                }
                return;
            }
        }
        Ui.info("Este paciente no tiene una cita tuya hoy. Las notas se escriben dentro de una atención: "
                + "pide a administración que programe la cita.");
    }

    // ------------------------------------------------------------------ ficha

    private void limpiarFicha() {
        seleccionado = null;
        lblTituloDatosGenerales.setText(iniTitulo);
        Ui.mostrar(lblEstadoPaciente, false);
        lblNacimiento.setText(iniNacimiento);
        lblMicrochip.setText(iniMicrochip);
        lblAlimentacion.setText(iniAlimentacion);
        lblNotaVacunas.setText(iniNotaVacunas);
        imgRetratoPaciente.setImage(null);
        Ui.mostrar(btnAbrirAtencionCurso, false);
        Ui.mostrar(cajaAvisoAntecedente, false);
        Ui.mostrar(contenedorNotaActual, false);
        Ui.llenarLista(contenedorNotas, lblSinNotas, new ArrayList<Node>());
        tblVacunasExp.getItems().clear();
    }

    private void seleccionar(long id) {
        Row m = ClienteRepo.mascota(id);
        if (m == null) {
            limpiarFicha();
            return;
        }
        seleccionado = m;
        lblTituloDatosGenerales.setText(m.str("nombre") + " · Datos generales");
        lblEstadoPaciente.setText("Hembra".equals(m.str("sexo")) ? "Paciente activa" : "Paciente activo");
        Ui.mostrar(lblEstadoPaciente, true);
        lblEspecieRaza.setText(m.str("especie") + " · " + Fmt.nvl(m.str("raza"), "Sin raza"));
        lblSexoEdadPeso.setText(m.str("sexo") + " · " + Fmt.edad(m.str("fecha_nacimiento")) + " · "
                + (m.esNulo("peso") ? "—" : Fmt.importe(m.dbl("peso")) + " kg"));
        lblNacimiento.setText("Nacimiento: " + (m.str("fecha_nacimiento").isEmpty() ? "—"
                : Fmt.fechaDMA(Fmt.fecha(m.str("fecha_nacimiento")))));
        lblMicrochip.setText("Microchip: " + Fmt.nvl(m.str("microchip"), "—"));
        lblAlimentacion.setText("Alimentación: " + Fmt.nvl(m.str("alimentacion"), "—"));

        byte[] foto = ImagenStore.leer(m.str("foto_ruta")); // null si no tiene o el archivo ya no existe
        imgRetratoPaciente.setImage(foto == null ? null : new javafx.scene.image.Image(new java.io.ByteArrayInputStream(foto)));

        lblPropietarioNombre.setText(m.str("propietario"));
        lblPropietarioTel.setText(Fmt.nvl(m.str("prop_telefono"), "—"));
        lblPropietarioCorreo.setText(Fmt.nvl(m.str("prop_correo"), "—"));
        lblPropietarioDireccion.setText(Fmt.nvl(m.str("prop_direccion"), "—"));
        lblContactoAlterno.setText(m.str("alt_nombre").isEmpty() ? "—"
                : m.str("alt_nombre") + " · " + Fmt.nvl(m.str("alt_telefono"), "—"));

        if (ClienteRepo.tieneAvisoClinico(m)) {
            lblAvisoAntecedente.setText("Antecedente importante: " + m.str("antecedentes"));
            Ui.mostrar(cajaAvisoAntecedente, true);
        } else {
            Ui.mostrar(cajaAvisoAntecedente, false);
        }

        Row enCurso = CitaRepo.enAtencion(Sesion.usuarioId);
        Ui.mostrar(btnAbrirAtencionCurso, enCurso != null && enCurso.lng("mascota_id") == m.id());

        // notas finalizadas (solo clinicas, no estetica)
        List<Node> notas = new ArrayList<>();
        for (Row h : ClienteRepo.historialClinico(m.id())) {
            if ("ESTETICA".equals(h.str("tipo"))) {
                continue;
            }
            Parent item = Ui.cargarPlantilla("evento-item.fxml");
            Ui.texto(item, "lblEventoFecha", Fmt.fechaEvento(Fmt.fecha(h.str("fecha")), h.str("hora_inicio")));
            Ui.texto(item, "lblEventoTitulo", h.str("servicio") + " · " + h.str("estado"));
            Ui.texto(item, "lblEventoProfesional", h.str("profesional"));
            String nota = (h.str("diagnostico") + (h.str("tratamiento").isEmpty() ? ""
                    : "\nTratamiento: " + h.str("tratamiento"))).trim();
            Ui.texto(item, "lblEventoNota", Fmt.nvl(nota, "Sin nota registrada."));
            notas.add(item);
        }
        Ui.llenarLista(contenedorNotas, lblSinNotas, notas);

        Row borrador = AtencionRepo.borradorDeMascota(m.id(), Sesion.usuarioId);
        if (borrador != null) {
            lblIndicacionesActual.setText(Fmt.nvl(borrador.str("tratamiento"), iniIndicaciones));
            lblProximaRevision.setText(borrador.str("proxima_revision").isEmpty() ? iniProximaRevision
                    : "Próxima revisión: " + Fmt.fechaDMA(Fmt.fecha(borrador.str("proxima_revision"))));
        }
        Ui.mostrar(contenedorNotaActual, borrador != null);

        List<Row> cuidados = ClienteRepo.cuidados(m.id());
        tblVacunasExp.getItems().setAll(cuidados);
        Row ultimo = null;
        for (Row c : cuidados) {
            if (!c.str("ultima_aplicacion").isEmpty()
                    && (ultimo == null || c.str("ultima_aplicacion").compareTo(ultimo.str("ultima_aplicacion")) > 0)) {
                ultimo = c;
            }
        }
        lblNotaVacunas.setText(ultimo == null ? iniNotaVacunas : ultimo.str("cuidado") + " aplicada por "
                + Fmt.nvl(ultimo.str("aplicado_por"), "—") + " · "
                + Fmt.fechaDMA(Fmt.fecha(ultimo.str("ultima_aplicacion"))));
    }
}
