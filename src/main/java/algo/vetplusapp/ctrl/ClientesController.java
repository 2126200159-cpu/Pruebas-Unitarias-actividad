package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.CajaRepo;
import algo.vetplusapp.repo.ClienteRepo;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Nav;
import algo.vetplusapp.util.RecibeDatos;
import algo.vetplusapp.util.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/** Clientes y mascotas (4.4): directorio paginado y ficha del propietario con sus mascotas. */
public class ClientesController implements RecibeDatos {

    @FXML
    private Label lblSinMascotas;
    @FXML
    private ToggleGroup grupoFiltroClientes;

    @FXML
    private Button btnRegistrarPropietario;

    @FXML
    private TextField txtBuscar;

    @FXML
    private ToggleButton tglTodos;

    @FXML
    private ToggleButton tglActivos;

    @FXML
    private TableView<Row> tblClientes;

    @FXML
    private TableColumn<Row, String> colPropietario;

    @FXML
    private TableColumn<Row, String> colContacto;

    @FXML
    private TableColumn<Row, String> colMascotas;

    @FXML
    private Label lblPaginacion;

    @FXML
    private Hyperlink lnkAnterior;

    @FXML
    private Hyperlink lnkSiguiente;

    @FXML
    private Label lblNombreCliente;

    @FXML
    private Label lblClienteDesde;

    @FXML
    private Label lblEstadoCliente;

    @FXML
    private Button btnEliminarPropietario;

    @FXML
    private Button btnEditarPropietario;

    @FXML
    private TextField txtTelefono;

    @FXML
    private TextField txtCorreo;

    @FXML
    private TextField txtDireccion;

    @FXML
    private TextField txtCurp;

    @FXML
    private TextField txtRegistro;

    @FXML
    private Label lblTituloMascotas;

    @FXML
    private VBox contenedorMascotas;

    @FXML
    private Button btnRegistrarMascota;

    @FXML
    private Button btnVerCuenta;

    private List<Row> filtrados = new ArrayList<>();
    private int pagina = 0;
    private Row seleccionado;
    private boolean cargandoTabla = false;
    private String iniNombre;
    private String iniDesde;

    @FXML
    private void initialize() {
        iniNombre = lblNombreCliente.getText();
        iniDesde = lblClienteDesde.getText();

        Ui.col(colPropietario, "nombre_completo");
        Ui.col(colContacto, r -> (r.str("telefono") + (r.str("correo").isEmpty() ? "" : " · " + r.str("correo"))));
        Ui.col(colMascotas, "mascotas");

        // las cajas de la ficha son solo de lectura: se editan con el dialogo
        for (javafx.scene.control.TextField t : new javafx.scene.control.TextField[]{txtTelefono, txtCorreo,
            txtDireccion, txtCurp, txtRegistro}) {
            t.setEditable(false);
        }

        Ui.grupoSiempreUno(grupoFiltroClientes, t -> {
            pagina = 0;
            refrescar();
        });
        txtBuscar.textProperty().addListener((o, a, n) -> {
            pagina = 0;
            refrescar();
        });
        lnkAnterior.setOnAction(e -> {
            if (pagina > 0) {
                pagina--;
                mostrarPagina();
            }
        });
        lnkSiguiente.setOnAction(e -> {
            if ((pagina + 1) * Config.CLIENTES_POR_PAGINA < filtrados.size()) {
                pagina++;
                mostrarPagina();
            }
        });
        tblClientes.getSelectionModel().selectedItemProperty().addListener((o, a, fila) -> {
            if (!cargandoTabla && fila != null) {
                mostrarFicha(fila.id());
            }
        });

        btnRegistrarPropietario.setOnAction(e -> Ui.accion(() -> {
            Long id = Dialogos.propietario(null);
            if (id != null) {
                refrescar();
                seleccionar(id);
            }
        }));
        btnEditarPropietario.setOnAction(e -> Ui.accion(() -> {
            if (seleccionado == null) {
                Ui.aviso("Selecciona un cliente para editarlo.");
                return;
            }
            Long id = Dialogos.propietario(seleccionado);
            if (id != null) {
                refrescar();
                seleccionar(id);
            }
        }));
        btnEliminarPropietario.setOnAction(e -> Ui.accion(this::eliminarCliente));
        btnRegistrarMascota.setOnAction(e -> Ui.accion(() -> {
            if (seleccionado == null) {
                Ui.aviso("Selecciona primero al propietario de la mascota.");
                return;
            }
            Long id = Dialogos.mascota(seleccionado.id(), null);
            if (id != null) {
                refrescar();
                seleccionar(seleccionado.id());
            }
        }));
        btnVerCuenta.setOnAction(e -> Ui.accion(() -> {
            if (seleccionado == null) {
                Ui.aviso("Selecciona un cliente.");
                return;
            }
            Row cuenta = CajaRepo.cuentaPendienteDe(seleccionado.id());
            if (cuenta == null) {
                Ui.info("Este cliente no tiene cuentas pendientes.");
            } else {
                Nav.ir("caja", cuenta.id());
            }
        }));
        // el chip de estado cambia entre Activo e Inactivo (un propietario no se borra)
        lblEstadoCliente.setOnMouseClicked(e -> Ui.accion(() -> {
            if (seleccionado == null) {
                return;
            }
            boolean activo = "Activo".equals(seleccionado.str("estado"));
            if (Ui.confirmar(activo ? "¿Pasar a este cliente a Inactivo?" : "¿Reactivar a este cliente?")) {
                ClienteRepo.cambiarEstadoPropietario(seleccionado.id(), !activo);
                long id = seleccionado.id();
                refrescar();
                seleccionar(id);
            }
        }));
        lblEstadoCliente.setCursor(javafx.scene.Cursor.HAND);

        limpiarFicha();
        refrescar();
    }

    @Override
    public void cargar(Object dato) {
        if (dato instanceof Long) {
            txtBuscar.clear();
            tglTodos.setSelected(true);
            refrescar();
            seleccionar((Long) dato);
        }
    }

    private void refrescar() {
        List<Row> todos = ClienteRepo.propietarios();
        int activos = 0;
        for (Row r : todos) {
            if ("Activo".equals(r.str("estado"))) {
                activos++;
            }
        }
        tglTodos.setText("Todos · " + todos.size());
        tglActivos.setText("Activos · " + activos);

        String q = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase(Locale.ROOT);
        boolean soloActivos = tglActivos.isSelected();
        filtrados = new ArrayList<>();
        for (Row r : todos) {
            if (soloActivos && !"Activo".equals(r.str("estado"))) {
                continue;
            }
            String texto = (r.str("nombre_completo") + " " + r.str("telefono") + " " + r.str("correo") + " "
                    + r.str("mascotas")).toLowerCase(Locale.ROOT);
            if (q.isEmpty() || texto.contains(q)) {
                filtrados.add(r);
            }
        }
        mostrarPagina();
    }

    private void mostrarPagina() {
        int n = filtrados.size();
        int por = Config.CLIENTES_POR_PAGINA;
        if (pagina * por >= n) {
            pagina = Math.max(0, (n - 1) / por);
        }
        int desde = pagina * por;
        int hasta = Math.min(n, desde + por);
        cargandoTabla = true;
        tblClientes.getItems().setAll(filtrados.subList(desde, hasta));
        cargandoTabla = false;
        lblPaginacion.setText(n == 0 ? "0 clientes" : (desde + 1) + "–" + hasta + " de " + n + " clientes");
        lnkAnterior.setDisable(pagina == 0);
        lnkSiguiente.setDisable(hasta >= n);
        if (seleccionado != null) {
            for (Row r : tblClientes.getItems()) {
                if (r.id() == seleccionado.id()) {
                    cargandoTabla = true;
                    tblClientes.getSelectionModel().select(r);
                    cargandoTabla = false;
                }
            }
        }
    }

    /** Localiza al propietario en la lista (cambiando de pagina si hace falta) y muestra su ficha. */
    private void seleccionar(long id) {
        for (int i = 0; i < filtrados.size(); i++) {
            if (filtrados.get(i).id() == id) {
                pagina = i / Config.CLIENTES_POR_PAGINA;
                break;
            }
        }
        mostrarFicha(id);
        mostrarPagina();
    }

    private void limpiarFicha() {
        seleccionado = null;
        lblNombreCliente.setText(iniNombre);
        lblClienteDesde.setText(iniDesde);
        Ui.mostrar(lblEstadoCliente, false);
        for (javafx.scene.control.TextField t : new javafx.scene.control.TextField[]{txtTelefono, txtCorreo,
            txtDireccion, txtCurp, txtRegistro}) {
            t.clear();
        }
        lblTituloMascotas.setText("Sus mascotas");
        Ui.llenarLista(contenedorMascotas, lblSinMascotas, new ArrayList<Node>());
    }

    private void mostrarFicha(long id) {
        Row p = ClienteRepo.propietario(id);
        if (p == null) {
            limpiarFicha();
            return;
        }
        seleccionado = p;
        lblNombreCliente.setText(p.str("nombre_completo"));
        lblClienteDesde.setText("Cliente desde el " + Fmt.fechaSinDia(Fmt.fecha(p.str("fecha_registro"))));
        lblEstadoCliente.setText(p.str("estado"));
        Ui.mostrar(lblEstadoCliente, true);
        txtTelefono.setText(p.str("telefono"));
        txtCorreo.setText(p.str("correo"));
        txtDireccion.setText(p.str("direccion"));
        txtCurp.setText(p.str("curp"));
        txtRegistro.setText(Fmt.fechaDMA(Fmt.fecha(p.str("fecha_registro"))) + " · " + p.str("canal_registro"));

        List<Row> mascotas = ClienteRepo.mascotasDe(id);
        lblTituloMascotas.setText("Sus mascotas · " + mascotas.size());
        List<Node> items = new ArrayList<>();
        for (Row m : mascotas) {
            Parent item = Ui.cargarPlantilla("mascota-item.fxml");
            Ui.texto(item, "lblMascotaNombre", m.str("nombre"));
            String edad = Fmt.edad(m.str("fecha_nacimiento"));
            Ui.texto(item, "lblMascotaDetalle", m.str("especie") + " · " + Fmt.nvl(m.str("raza"), "Sin raza")
                    + " · " + edad + " · " + m.str("sexo"));
            boolean gato = "Gato".equals(m.str("especie"));
            Ui.mostrar(item.lookup("#iconoPerro"), !gato);
            Ui.mostrar(item.lookup("#iconoGato"), gato);
            long mascotaId = m.id();
            item.setOnMouseClicked(e -> Nav.ir("expediente-mascota", mascotaId));
            item.setCursor(javafx.scene.Cursor.HAND);
            items.add(item);
        }
        Ui.llenarLista(contenedorMascotas, lblSinMascotas, items);
    }

    /** Eliminacion conjunta: cliente y sus mascotas; si tiene historial ofrece pasarlo a Inactivo. */
    private void eliminarCliente() {
        if (seleccionado == null) {
            Ui.aviso("Selecciona un cliente para eliminarlo.");
            return;
        }
        long id = seleccionado.id();
        String nombre = seleccionado.str("nombre_completo");
        if (ClienteRepo.tieneHistorialPropietario(id)) {
            if (Ui.confirmar(nombre + " tiene historial (citas, cuentas o estancias), así que no se puede eliminar "
                    + "sin perderlo.\n\n¿Quieres pasarlo a Inactivo?")) {
                ClienteRepo.cambiarEstadoPropietario(id, false);
                refrescar();
                seleccionar(id);
            }
            return;
        }
        int mascotas = ClienteRepo.mascotasDe(id).size();
        if (!Ui.confirmar("¿Eliminar a " + nombre + (mascotas > 0 ? " junto con " + (mascotas == 1 ? "su mascota"
                : "sus " + mascotas + " mascotas") : "") + "? Esta acción no se puede deshacer.")) {
            return;
        }
        ClienteRepo.eliminarPropietario(id);
        limpiarFicha();
        refrescar();
        Ui.info("Cliente eliminado.");
    }
}
