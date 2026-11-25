package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.ClienteRepo;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Nav;
import algo.vetplusapp.util.RecibeDatos;
import algo.vetplusapp.util.Ui;
import algo.vetplusapp.util.ImagenStore;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Expediente de mascota (4.5): datos, propietario, historial de atenciones y vacunas. */
public class ExpedienteMascotaController implements RecibeDatos {

    @FXML
    private ToggleGroup grupoPestanas;

    @FXML
    private Label lblTituloExpediente;

    @FXML
    private Label lblRutaExpediente;

    @FXML
    private Button btnEliminarMascota;

    @FXML
    private Button btnEditarMascota;

    @FXML
    private ImageView imgRetrato;

    @FXML
    private Label lblNombreMascota;

    @FXML
    private Label lblEstadoMascota;

    @FXML
    private Label lblRazaEspecieSexo;

    @FXML
    private TextField txtEdad;

    @FXML
    private TextField txtPeso;

    @FXML
    private Label lblNacimiento;

    @FXML
    private Label lblEsterilizada;

    @FXML
    private Label lblMicrochip;

    @FXML
    private Label lblPropietariaNombre;

    @FXML
    private Label lblPropietariaTel;

    @FXML
    private Hyperlink lnkLlamar;

    @FXML
    private Label lblPropietariaCorreo;

    @FXML
    private Label lblPropietariaDireccion;

    @FXML
    private Label lblContactoAlterno;

    @FXML
    private Button btnVerMascotasPropietaria;

    @FXML
    private HBox cajaAntecedente;

    @FXML
    private Label lblAntecedente;

    @FXML
    private ToggleButton tglHistorial;

    @FXML
    private ToggleButton tglVacunas;

    @FXML
    private ToggleButton tglDocumentos;

    @FXML
    private Label lblTotalAtenciones;

    @FXML
    private VBox contenedorHistorial;

    @FXML
    private Label lblSinHistorial;

    @FXML
    private TableView<Row> tblVacunas;

    @FXML
    private TableColumn<Row, String> colCuidado;

    @FXML
    private TableColumn<Row, String> colUltimaAplicacion;

    @FXML
    private TableColumn<Row, String> colProximaFecha;

    @FXML
    private TableColumn<Row, String> colEstadoCuidado;

    private Row mascota;
    private String iniTitulo;
    private String iniRuta;
    private String iniNacimiento;
    private String iniEsterilizada;
    private String iniMicrochip;

    @FXML
    private void initialize() {
        iniTitulo = lblTituloExpediente.getText();
        iniRuta = lblRutaExpediente.getText();
        iniNacimiento = lblNacimiento.getText();
        iniEsterilizada = lblEsterilizada.getText();
        iniMicrochip = lblMicrochip.getText();

        Ui.col(colCuidado, "cuidado");
        Ui.col(colUltimaAplicacion, r -> fechaODash(r.str("ultima_aplicacion")));
        Ui.col(colProximaFecha, r -> fechaODash(r.str("proxima_fecha")));
        Ui.col(colEstadoCuidado, r -> ClienteRepo.estadoCuidado(r.str("proxima_fecha")));

        txtEdad.setEditable(false);
        txtPeso.setEditable(false);
        tglDocumentos.setDisable(true); // sin contenido diseñado (capitulo 8)
        Ui.grupoSiempreUno(grupoPestanas, t -> {
            if (t == tglHistorial) {
                Ui.desplazarA(contenedorHistorial);
            } else if (t == tglVacunas) {
                Ui.desplazarA(tblVacunas);
            }
        });

        btnEditarMascota.setOnAction(e -> Ui.accion(() -> {
            if (mascota == null) {
                return;
            }
            Long id = Dialogos.mascota(mascota.lng("prop_id"), mascota);
            if (id != null) {
                mostrar(id);
            }
        }));
        btnEliminarMascota.setOnAction(e -> Ui.accion(this::eliminarMascota));
        lnkLlamar.setOnAction(e -> {
            if (mascota != null) {
                Ui.contacto(mascota.str("propietario"), mascota.str("prop_telefono"), mascota.str("prop_correo"));
            }
        });
        btnVerMascotasPropietaria.setOnAction(e -> {
            if (mascota != null) {
                Nav.ir("clientes", mascota.lng("prop_id"));
            }
        });
        limpiar();
    }

    @Override
    public void cargar(Object dato) {
        if (dato instanceof Long) {
            mostrar((Long) dato);
        }
    }

    private static String fechaODash(String iso) {
        return iso.isEmpty() ? "—" : Fmt.fechaDMA(Fmt.fecha(iso));
    }

    private void limpiar() {
        mascota = null;
        lblTituloExpediente.setText(iniTitulo);
        lblRutaExpediente.setText(iniRuta);
        Ui.mostrar(lblEstadoMascota, false);
        Ui.mostrar(cajaAntecedente, false);
        lblNacimiento.setText(iniNacimiento);
        lblEsterilizada.setText(iniEsterilizada);
        lblMicrochip.setText(iniMicrochip);
        imgRetrato.setImage(null);
        tblVacunas.getItems().clear();
        Ui.llenarLista(contenedorHistorial, lblSinHistorial, new ArrayList<Node>());
    }

    private void mostrar(long id) {
        Row m = ClienteRepo.mascota(id);
        if (m == null) {
            limpiar();
            return;
        }
        mascota = m;
        lblTituloExpediente.setText("Expediente de " + m.str("nombre"));
        lblRutaExpediente.setText("Clientes y mascotas / " + m.str("propietario") + " / " + m.str("nombre"));
        lblNombreMascota.setText(m.str("nombre"));
        lblEstadoMascota.setText(m.str("estado"));
        Ui.mostrar(lblEstadoMascota, true);
        lblRazaEspecieSexo.setText(Fmt.nvl(m.str("raza"), "Sin raza") + " · " + m.str("especie") + " · "
                + m.str("sexo"));
        txtEdad.setText(Fmt.edad(m.str("fecha_nacimiento")));
        txtPeso.setText(m.esNulo("peso") ? "" : Fmt.importe(m.dbl("peso")) + " kg");
        lblNacimiento.setText("Nacimiento: " + fechaODash(m.str("fecha_nacimiento")));
        lblEsterilizada.setText("Esterilizada: " + (m.bool("esterilizada") ? "Sí" : "No"));
        lblMicrochip.setText("Microchip: " + Fmt.nvl(m.str("microchip"), "—"));

        byte[] foto = ImagenStore.leer(m.str("foto_ruta")); // null si no tiene o el archivo ya no existe
        imgRetrato.setImage(foto == null ? null : new javafx.scene.image.Image(new java.io.ByteArrayInputStream(foto)));

        lblPropietariaNombre.setText(m.str("propietario"));
        lblPropietariaTel.setText(Fmt.nvl(m.str("prop_telefono"), "—"));
        lblPropietariaCorreo.setText(Fmt.nvl(m.str("prop_correo"), "—"));
        lblPropietariaDireccion.setText(Fmt.nvl(m.str("prop_direccion"), "—"));
        lblContactoAlterno.setText(m.str("alt_nombre").isEmpty() ? "—"
                : m.str("alt_nombre") + " · " + Fmt.nvl(m.str("alt_telefono"), "—"));

        if (ClienteRepo.tieneAvisoClinico(m)) {
            lblAntecedente.setText("Antecedente importante: " + m.str("antecedentes"));
            Ui.mostrar(cajaAntecedente, true);
        } else {
            Ui.mostrar(cajaAntecedente, false);
        }

        List<Row> historial = ClienteRepo.historialClinico(id);
        lblTotalAtenciones.setText("Registro cronológico · " + historial.size()
                + (historial.size() == 1 ? " atención en total" : " atenciones en total"));
        List<Node> eventos = new ArrayList<>();
        for (Row h : historial) {
            Parent item = Ui.cargarPlantilla("evento-item.fxml");
            Ui.texto(item, "lblEventoFecha", Fmt.fechaEvento(Fmt.fecha(h.str("fecha")), h.str("hora_inicio")));
            Ui.texto(item, "lblEventoTitulo", h.str("servicio") + " · " + h.str("estado"));
            Ui.texto(item, "lblEventoProfesional", h.str("profesional"));
            String nota;
            if ("ESTETICA".equals(h.str("tipo"))) {
                nota = h.str("observaciones");
            } else {
                nota = (h.str("diagnostico") + (h.str("tratamiento").isEmpty() ? ""
                        : "\nTratamiento: " + h.str("tratamiento"))).trim();
            }
            Ui.texto(item, "lblEventoNota", Fmt.nvl(nota, "Sin nota registrada."));
            eventos.add(item);
        }
        Ui.llenarLista(contenedorHistorial, lblSinHistorial, eventos);

        tblVacunas.getItems().setAll(ClienteRepo.cuidados(id));
    }

    private void eliminarMascota() {
        if (mascota == null) {
            return;
        }
        long id = mascota.id();
        long propietario = mascota.lng("prop_id");
        String nombre = mascota.str("nombre");
        if (ClienteRepo.tieneHistorialMascota(id)) {
            if (Ui.confirmar(nombre + " tiene citas, cuentas o estancias registradas, por eso no se puede eliminar "
                    + "sin perder el historial.\n\n¿Quieres pasarla a Inactiva?")) {
                ClienteRepo.cambiarEstadoMascota(id, false);
                mostrar(id);
                Ui.info(nombre + " quedó como Inactiva.");
            }
            return;
        }
        if (!Ui.confirmar("¿Eliminar a " + nombre + "? Esta acción no se puede deshacer.")) {
            return;
        }
        ClienteRepo.eliminarMascota(id);
        Ui.info("Mascota eliminada.");
        Nav.ir("clientes", propietario);
    }
}
