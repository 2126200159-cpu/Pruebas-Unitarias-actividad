package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.AtencionRepo;
import algo.vetplusapp.repo.CitaRepo;
import algo.vetplusapp.repo.ClienteRepo;
import algo.vetplusapp.repo.ServicioRepo;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Nav;
import algo.vetplusapp.util.PuedeSalir;
import algo.vetplusapp.util.RecibeDatos;
import algo.vetplusapp.util.Sesion;
import algo.vetplusapp.util.Ui;
import algo.vetplusapp.util.Valida;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Consulta medica (4.14): nota clinica de la atencion en curso. */
public class ConsultaMedicaController implements RecibeDatos, PuedeSalir {

    @FXML
    private Label lblDatosConsulta;

    @FXML
    private Label lblEstadoAtencion;

    @FXML
    private Label lblPacienteNombre;

    @FXML
    private Label lblPacienteDetalle;

    @FXML
    private Label lblPacientePeso;

    @FXML
    private Label lblPacientePropietario;

    @FXML
    private Label lblPacienteTelefono;

    @FXML
    private Button btnAbrirExpedienteCompleto;

    @FXML
    private HBox cajaAvisoPaciente;

    @FXML
    private Label lblAvisoPaciente;

    @FXML
    private VBox contenedorAntecedentes;

    @FXML
    private Label lblSinAntecedentes;

    @FXML
    private Label lblSinSiguiente;

    @FXML
    private VBox contenedorSiguiente;

    @FXML
    private Label lblSiguienteTitulo;

    @FXML
    private Label lblSiguienteDetalle;

    @FXML
    private Button btnVerMiAgenda;

    @FXML
    private Label lblMedicoNota;

    @FXML
    private TextArea txtMotivoConsulta;

    @FXML
    private TextField txtPesoConsulta;

    @FXML
    private TextField txtTemperatura;

    @FXML
    private TextField txtFrecuenciaCardiaca;

    @FXML
    private TextArea txtExploracionDiagnostico;

    @FXML
    private TextArea txtTratamiento;

    @FXML
    private DatePicker dtpProximaRevision;

    @FXML
    private ComboBox<String> cmbHoraRevision;

    @FXML
    private ComboBox<Row> cmbServicioRegistrado;

    @FXML
    private Label lblAvisoFinalizar;

    @FXML
    private Button btnGuardarBorrador;

    @FXML
    private Button btnFinalizarAtencion;

    @FXML
    private Label lblUltimoGuardado;

    private Row cita;
    private boolean sucio = false;
    private boolean cargando = false;
    private String iniDatos;
    private String iniNombre;
    private String iniDetalle;
    private String iniPeso;
    private String iniPropietario;
    private String iniTelefono;
    private String iniMedico;
    private String iniUltimoGuardado;

    @FXML
    private void initialize() {
        iniDatos = lblDatosConsulta.getText();
        iniNombre = lblPacienteNombre.getText();
        iniDetalle = lblPacienteDetalle.getText();
        iniPeso = lblPacientePeso.getText();
        iniPropietario = lblPacientePropietario.getText();
        iniTelefono = lblPacienteTelefono.getText();
        iniMedico = lblMedicoNota.getText();
        iniUltimoGuardado = lblUltimoGuardado.getText();

        cmbServicioRegistrado.setConverter(Dialogos.convertidor("nombre"));
        cmbServicioRegistrado.getItems().setAll(ServicioRepo.activosPorTipo("MEDICO"));
        dtpProximaRevision.valueProperty().addListener((o, a, d) -> {
            cmbHoraRevision.getItems().clear();
            cmbHoraRevision.setValue(null);
            if (d != null && cita != null) {
                cmbHoraRevision.getItems().setAll(CitaRepo.horariosLibres(Sesion.usuarioId, d.toString(), 30));
            }
        });
        // cualquier cambio en los campos marca la nota como "con cambios sin guardar"
        javafx.beans.value.ChangeListener<Object> marcar = (o, a, n) -> {
            if (!cargando) {
                sucio = true;
            }
        };
        txtMotivoConsulta.textProperty().addListener(marcar);
        txtPesoConsulta.textProperty().addListener(marcar);
        txtTemperatura.textProperty().addListener(marcar);
        txtFrecuenciaCardiaca.textProperty().addListener(marcar);
        txtExploracionDiagnostico.textProperty().addListener(marcar);
        txtTratamiento.textProperty().addListener(marcar);
        dtpProximaRevision.valueProperty().addListener(marcar);
        cmbHoraRevision.valueProperty().addListener(marcar);
        cmbServicioRegistrado.valueProperty().addListener(marcar);

        btnGuardarBorrador.setOnAction(e -> Ui.accion(() -> {
            guardarBorrador();
            Ui.info("Borrador guardado.");
        }));
        btnFinalizarAtencion.setOnAction(e -> Ui.accion(this::finalizar));
        btnVerMiAgenda.setOnAction(e -> Nav.ir("agenda-medico"));
        btnAbrirExpedienteCompleto.setOnAction(e -> {
            if (cita != null) {
                Nav.ir("expedientes-medico", cita.lng("mascota_id"));
            }
        });

        vaciar();
    }

    @Override
    public void cargar(Object dato) {
        Row c = null;
        if (dato instanceof Long) {
            c = CitaRepo.cita((Long) dato);
        } else {
            c = CitaRepo.enAtencion(Sesion.usuarioId); // desde el menu: la atencion en curso, si existe
        }
        // un medico solo trabaja con sus propias citas
        if (c == null || c.lng("profesional_id") != Sesion.usuarioId || !"En atención".equals(c.str("estado"))) {
            vaciar();
            return;
        }
        mostrar(c);
    }

    // ------------------------------------------------------------------ estados de la pantalla

    private void bloquear(boolean si) {
        for (javafx.scene.Node n : new javafx.scene.Node[]{txtMotivoConsulta, txtPesoConsulta, txtTemperatura,
            txtFrecuenciaCardiaca, txtExploracionDiagnostico, txtTratamiento, dtpProximaRevision, cmbHoraRevision,
            cmbServicioRegistrado, btnGuardarBorrador, btnFinalizarAtencion}) {
            n.setDisable(si);
        }
    }

    private void vaciar() {
        cargando = true;
        cita = null;
        sucio = false;
        lblDatosConsulta.setText(iniDatos);
        Ui.mostrar(lblEstadoAtencion, false);
        lblPacienteNombre.setText(iniNombre);
        lblPacienteDetalle.setText(iniDetalle);
        lblPacientePeso.setText(iniPeso);
        lblPacientePropietario.setText(iniPropietario);
        lblPacienteTelefono.setText(iniTelefono);
        Ui.mostrar(cajaAvisoPaciente, false);
        Ui.llenarLista(contenedorAntecedentes, lblSinAntecedentes, new ArrayList<Node>());
        Ui.mostrar(contenedorSiguiente, false);
        Ui.mostrar(lblSinSiguiente, true);
        lblMedicoNota.setText(iniMedico);
        lblUltimoGuardado.setText(iniUltimoGuardado);
        txtMotivoConsulta.clear();
        txtPesoConsulta.clear();
        txtTemperatura.clear();
        txtFrecuenciaCardiaca.clear();
        txtExploracionDiagnostico.clear();
        txtTratamiento.clear();
        dtpProximaRevision.setValue(null);
        cmbHoraRevision.getItems().clear();
        cmbServicioRegistrado.setValue(null);
        btnAbrirExpedienteCompleto.setDisable(true);
        bloquear(true);
        cargando = false;
    }

    private void mostrar(Row c) {
        cargando = true;
        cita = c;
        Row m = ClienteRepo.mascota(c.lng("mascota_id"));
        bloquear(false);
        btnAbrirExpedienteCompleto.setDisable(false);

        lblDatosConsulta.setText(Fmt.fechaAbrDM(Fmt.fecha(c.str("fecha"))) + " · "
                + Fmt.rango(c.str("hora_inicio"), c.integer("duracion_min")) + " · "
                + Fmt.nvl(c.str("consultorio"), "—"));
        lblEstadoAtencion.setText("Atención en curso");
        Ui.mostrar(lblEstadoAtencion, true);
        lblPacienteNombre.setText(m.str("nombre"));
        lblPacienteDetalle.setText(Fmt.nvl(m.str("raza"), m.str("especie")) + " · "
                + Fmt.edad(m.str("fecha_nacimiento")) + " · " + m.str("sexo"));
        lblPacientePeso.setText("Peso: " + (m.esNulo("peso") ? "—" : Fmt.importe(m.dbl("peso")) + " kg")
                + " · Esterilizada: " + (m.bool("esterilizada") ? "Sí" : "No"));
        lblPacientePropietario.setText("Propietario: " + m.str("propietario"));
        lblPacienteTelefono.setText("Tel. " + Fmt.nvl(m.str("prop_telefono"), "—"));

        if (ClienteRepo.tieneAvisoClinico(m)) {
            lblAvisoPaciente.setText("Antecedente importante: " + m.str("antecedentes"));
            Ui.mostrar(cajaAvisoPaciente, true);
        } else {
            Ui.mostrar(cajaAvisoPaciente, false);
        }

        List<Node> items = new ArrayList<>();
        for (Row a : AtencionRepo.antecedentes(m.id())) {
            Parent item = Ui.cargarPlantilla("antecedente-item.fxml");
            Ui.texto(item, "lblAntecedenteTitulo", a.str("titulo"));
            Ui.texto(item, "lblAntecedenteTexto", a.str("texto"));
            items.add(item);
        }
        Ui.llenarLista(contenedorAntecedentes, lblSinAntecedentes, items);

        Row sig = CitaRepo.siguiente(Sesion.usuarioId, c.str("fecha"), c.str("hora_inicio"));
        if (sig != null) {
            lblSiguienteTitulo.setText(sig.str("mascota") + " · " + sig.str("servicio"));
            lblSiguienteDetalle.setText(Fmt.rango(sig.str("hora_inicio"), sig.integer("duracion_min")) + " · "
                    + Fmt.nvl(sig.str("consultorio"), "—"));
        }
        Ui.mostrar(contenedorSiguiente, sig != null);
        Ui.mostrar(lblSinSiguiente, sig == null);

        lblMedicoNota.setText(Sesion.nombreCompleto + (Sesion.especialidad.isEmpty() ? "" : " · "
                + Sesion.especialidad));

        // servicio por defecto: el de la cita
        for (Row s : cmbServicioRegistrado.getItems()) {
            if (s.id() == c.lng("servicio_id")) {
                cmbServicioRegistrado.setValue(s);
            }
        }
        Row nota = AtencionRepo.nota(c.id());
        if (nota != null) { // borrador existente: se rellena todo
            txtMotivoConsulta.setText(nota.str("motivo"));
            txtPesoConsulta.setText(nota.esNulo("peso") ? "" : Fmt.importe(nota.dbl("peso")));
            txtTemperatura.setText(nota.esNulo("temperatura") ? "" : String.valueOf(nota.dbl("temperatura")));
            txtFrecuenciaCardiaca.setText(nota.esNulo("frecuencia_cardiaca") ? ""
                    : String.valueOf(nota.integer("frecuencia_cardiaca")));
            txtExploracionDiagnostico.setText(nota.str("exploracion_diagnostico"));
            txtTratamiento.setText(nota.str("tratamiento"));
            dtpProximaRevision.setValue(Fmt.fecha(nota.str("proxima_revision")));
            if (!nota.str("proxima_revision_hora").isEmpty()) {
                cmbHoraRevision.setValue(nota.str("proxima_revision_hora"));
            }
            for (Row s : cmbServicioRegistrado.getItems()) {
                if (!nota.esNulo("servicio_registrado_id") && s.id() == nota.lng("servicio_registrado_id")) {
                    cmbServicioRegistrado.setValue(s);
                }
            }
            lblUltimoGuardado.setText("Último guardado: " + textoGuardado(nota.str("actualizado_en")) + " ·");
        } else {
            txtMotivoConsulta.setText(c.str("motivo"));
            txtPesoConsulta.setText(m.esNulo("peso") ? "" : Fmt.importe(m.dbl("peso")));
        }
        cargando = false;
        sucio = false;
    }

    private static String textoGuardado(String sqlTiempo) {
        if (sqlTiempo.length() < 16) {
            return "—";
        }
        LocalDate dia = Fmt.fecha(sqlTiempo);
        String hora = sqlTiempo.substring(11, 16);
        return (dia.equals(LocalDate.now()) ? "hoy" : Fmt.fechaDMA(dia)) + ", " + hora;
    }

    // ------------------------------------------------------------------ guardar y finalizar

    private AtencionRepo.Nota leerNota() {
        AtencionRepo.Nota n = new AtencionRepo.Nota();
        n.motivo = Valida.t(txtMotivoConsulta.getText());
        double peso = Valida.numero(txtPesoConsulta.getText(), "El peso", false);
        n.peso = Double.isNaN(peso) ? null : peso;
        double temp = Valida.numero(txtTemperatura.getText(), "La temperatura", false);
        n.temperatura = Double.isNaN(temp) ? null : temp;
        n.frecuencia = Valida.entero(txtFrecuenciaCardiaca.getText(), "La frecuencia cardiaca", false);
        n.exploracion = Valida.t(txtExploracionDiagnostico.getText());
        n.tratamiento = Valida.t(txtTratamiento.getText());
        n.proximaFecha = dtpProximaRevision.getValue();
        n.proximaHora = cmbHoraRevision.getValue();
        n.servicioId = cmbServicioRegistrado.getValue() == null ? null : cmbServicioRegistrado.getValue().id();
        if (n.proximaFecha != null && n.proximaFecha.isBefore(LocalDate.now())) {
            throw new ReglaException("La próxima revisión no puede ser una fecha pasada.");
        }
        return n;
    }

    private void guardarBorrador() {
        if (cita == null) {
            return;
        }
        AtencionRepo.guardarBorrador(cita.id(), leerNota());
        lblUltimoGuardado.setText("Último guardado: hoy, " + Fmt.hora(LocalTime.now()) + " ·");
        sucio = false;
    }

    private void finalizar() {
        if (cita == null) {
            return;
        }
        AtencionRepo.Nota nota = leerNota();
        if (!Ui.confirmar("¿Finalizar la atención de " + cita.str("mascota")
                + "? La nota ya no se podrá editar y el servicio se agregará a la cuenta de "
                + cita.str("propietario") + ".")) {
            return;
        }
        boolean revision = AtencionRepo.finalizar(cita.id(), nota);
        boolean pidioRevision = nota.proximaFecha != null && nota.proximaHora != null;
        sucio = false;
        if (pidioRevision && !revision) {
            Ui.aviso("La atención se finalizó, pero no se pudo programar la cita de revisión "
                    + "(horario ocupado). Prográmala desde administración.");
        } else {
            Ui.info(revision ? "Atención finalizada. Se programó la cita de revisión." : "Atención finalizada.");
        }
        Nav.ir("agenda-medico");
    }

    /** El marco la consulta antes de cambiar de pantalla: si hay texto sin guardar, pregunta. */
    @Override
    public boolean puedeSalir() {
        if (cita == null || !sucio) {
            return true;
        }
        ButtonType r = Ui.preguntarGuardar("Tienes cambios sin guardar. ¿Quieres guardar el borrador antes de salir?");
        if (r == ButtonType.YES) {
            try {
                guardarBorrador();
            } catch (ReglaException e) {
                Ui.aviso(e.getMessage());
                return false;
            }
            return true;
        }
        return r == ButtonType.NO;
    }
}
