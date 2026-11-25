package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.ComprasRepo;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Sesion;
import algo.vetplusapp.util.Ui;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.DateCell;
import javafx.scene.input.KeyCode;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/** Compras e insumos (4.11): inventario, proveedores y la orden de compra que se va armando. */
public class ComprasController {

    @FXML
    private Button btnRegistrarInsumo;

    @FXML
    private TextField txtBuscarInsumo;

    @FXML
    private TableView<Row> tblInsumos;

    @FXML
    private TableColumn<Row, String> colInsumo;

    @FXML
    private TableColumn<Row, String> colExistencia;

    @FXML
    private TableColumn<Row, String> colMinimo;

    @FXML
    private TableColumn<Row, String> colPrecioCompra;

    @FXML
    private HBox cajaAvisoBajoMinimo;

    @FXML
    private Label lblAvisoBajoMinimo;

    @FXML
    private Hyperlink lnkNuevoProveedor;

    @FXML
    private TableView<Row> tblProveedores;

    @FXML
    private TableColumn<Row, String> colEmpresaContacto;

    @FXML
    private TableColumn<Row, String> colTelefonoProveedor;

    @FXML
    private TableColumn<Row, String> colCorreoProveedor;

    @FXML
    private Hyperlink lnkEditarProveedor;

    @FXML
    private Hyperlink lnkHistorialCompras;

    @FXML
    private Label lblTituloOrden;

    @FXML
    private Label lblCreadaPor;

    @FXML
    private Label lblEstadoOrden;

    @FXML
    private ComboBox<Row> cmbProveedor;

    @FXML
    private Label lblTelProveedor;

    @FXML
    private Label lblCorreoProveedor;

    @FXML
    private DatePicker dtpEntrega;

    @FXML
    private TableView<ComprasRepo.RenglonOrden> tblRenglones;

    @FXML
    private TableColumn<ComprasRepo.RenglonOrden, String> colProducto;

    @FXML
    private TableColumn<ComprasRepo.RenglonOrden, String> colCantidad;

    @FXML
    private TableColumn<ComprasRepo.RenglonOrden, String> colPrecioUnitario;

    @FXML
    private TableColumn<ComprasRepo.RenglonOrden, String> colImporteRenglon;

    @FXML
    private Hyperlink lnkAgregarRenglon;

    @FXML
    private Label lblTotalOrden;

    @FXML
    private TextArea txtNotasProveedor;

    @FXML
    private Button btnEmitirOrden;

    @FXML
    private Button btnGuardarBorrador;

    @FXML
    private Label lblNotaExistencias;

    private final List<ComprasRepo.RenglonOrden> renglones = new ArrayList<>();
    private String folio;
    private String iniTituloOrden;

    @FXML
    private void initialize() {
        iniTituloOrden = lblTituloOrden.getText();

        Ui.col(colInsumo, r -> r.str("nombre") + (r.str("categoria").isEmpty() ? "" : " · " + r.str("categoria")));
        Ui.col(colExistencia, r -> cantidad(r.dbl("existencia")));
        Ui.col(colMinimo, r -> cantidad(r.dbl("minimo")));
        Ui.col(colPrecioCompra, r -> Fmt.dinero(r.dbl("precio_compra")));
        Ui.col(colEmpresaContacto, r -> r.str("empresa") + (r.str("contacto").isEmpty() ? "" : " · " + r.str("contacto")));
        Ui.col(colTelefonoProveedor, "telefono");
        Ui.col(colCorreoProveedor, "correo");

        colProducto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombre));
        colCantidad.setCellValueFactory(d -> new SimpleStringProperty(cantidad(d.getValue().cantidad)));
        colPrecioUnitario.setCellValueFactory(d -> new SimpleStringProperty(Fmt.dinero(d.getValue().precio)));
        colImporteRenglon.setCellValueFactory(d -> new SimpleStringProperty(Fmt.dinero(d.getValue().importe())));

        cmbProveedor.setConverter(Dialogos.convertidor("empresa"));
        cmbProveedor.valueProperty().addListener((o, a, p) -> {
            lblTelProveedor.setText(p == null ? "" : Fmt.nvl(p.str("telefono"), "—"));
            lblCorreoProveedor.setText(p == null ? "" : Fmt.nvl(p.str("correo"), "—"));
        });
        dtpEntrega.setDayCellFactory(p -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item.isBefore(LocalDate.now())) {
                    setDisable(true);
                }
            }
        });

        txtBuscarInsumo.textProperty().addListener((o, a, n) -> cargarInsumos());
        btnRegistrarInsumo.setOnAction(e -> Ui.accion(() -> {
            if (Dialogos.insumo(null)) {
                cargarInsumos();
            }
        }));
        // para editar un insumo: doble clic en la fila
        tblInsumos.setOnMouseClicked(e -> {
            Row fila = tblInsumos.getSelectionModel().getSelectedItem();
            if (e.getClickCount() == 2 && fila != null) {
                Ui.accion(() -> {
                    if (Dialogos.insumo(fila)) {
                        cargarInsumos();
                    }
                });
            }
        });
        lnkNuevoProveedor.setOnAction(e -> Ui.accion(() -> {
            if (Dialogos.proveedor(null) != null) {
                cargarProveedores();
            }
        }));
        lnkEditarProveedor.setOnAction(e -> Ui.accion(() -> {
            Row sel = tblProveedores.getSelectionModel().getSelectedItem();
            if (sel == null) {
                Ui.aviso("Selecciona un proveedor de la tabla para editarlo.");
                return;
            }
            if (Dialogos.proveedor(sel) != null) {
                cargarProveedores();
            }
        }));
        lnkHistorialCompras.setOnAction(e -> Ui.accion(() -> {
            Dialogos.historialCompras();
            cargarInsumos(); // una recepcion suma existencias
        }));

        lnkAgregarRenglon.setOnAction(e -> Ui.accion(() -> {
            ComprasRepo.RenglonOrden r = Dialogos.renglonOrden();
            if (r != null) {
                renglones.add(r);
                refrescarRenglones();
            }
        }));
        // Suprimir quita el renglon seleccionado
        tblRenglones.setOnKeyPressed(e -> {
            ComprasRepo.RenglonOrden sel = tblRenglones.getSelectionModel().getSelectedItem();
            if (e.getCode() == KeyCode.DELETE && sel != null) {
                renglones.remove(sel);
                refrescarRenglones();
            }
        });

        btnGuardarBorrador.setOnAction(e -> Ui.accion(() -> guardarOrden(false)));
        btnEmitirOrden.setOnAction(e -> Ui.accion(() -> guardarOrden(true)));

        cargarInsumos();
        cargarProveedores();
        nuevaOrden();
    }

    private static String cantidad(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.2f", v);
    }

    private void cargarInsumos() {
        String q = txtBuscarInsumo.getText() == null ? "" : txtBuscarInsumo.getText().trim().toLowerCase(Locale.ROOT);
        List<Row> visibles = new ArrayList<>();
        for (Row r : ComprasRepo.insumos()) {
            if (q.isEmpty() || (r.str("nombre") + " " + r.str("categoria")).toLowerCase(Locale.ROOT).contains(q)) {
                visibles.add(r);
            }
        }
        tblInsumos.getItems().setAll(visibles);

        List<Row> bajos = ComprasRepo.bajoMinimo();
        if (bajos.isEmpty()) {
            Ui.mostrar(cajaAvisoBajoMinimo, false);
            return;
        }
        StringBuilder nombres = new StringBuilder();
        for (int i = 0; i < bajos.size(); i++) {
            if (i > 0) {
                nombres.append(i == bajos.size() - 1 ? " y " : ", ");
            }
            nombres.append(bajos.get(i).str("nombre").toLowerCase(Locale.ROOT));
        }
        lblAvisoBajoMinimo.setText(bajos.size() + (bajos.size() == 1 ? " insumo bajo el mínimo: "
                : " insumos bajo el mínimo: ") + nombres + ".");
        Ui.mostrar(cajaAvisoBajoMinimo, true);
    }

    private void cargarProveedores() {
        List<Row> prov = ComprasRepo.proveedores();
        tblProveedores.getItems().setAll(prov);
        Row actual = cmbProveedor.getValue();
        cmbProveedor.getItems().setAll(prov);
        if (actual != null) {
            for (Row r : prov) {
                if (r.id() == actual.id()) {
                    cmbProveedor.setValue(r);
                }
            }
        }
    }

    private void refrescarRenglones() {
        tblRenglones.getItems().setAll(renglones);
        double total = 0;
        for (ComprasRepo.RenglonOrden r : renglones) {
            total += r.importe();
        }
        lblTotalOrden.setText(Fmt.dinero(total)); // se recalcula cada vez que cambia un renglon
    }

    /** Prepara una orden nueva en Borrador con el siguiente folio. */
    private void nuevaOrden() {
        folio = ComprasRepo.siguienteFolio();
        renglones.clear();
        lblTituloOrden.setText(iniTituloOrden + " · " + folio);
        lblCreadaPor.setText("Creada por " + Sesion.nombreCompleto + " · " + Fmt.fechaDMA(LocalDate.now()));
        lblEstadoOrden.setText("Borrador");
        cmbProveedor.setValue(null);
        dtpEntrega.setValue(null);
        txtNotasProveedor.clear();
        refrescarRenglones();
    }

    private void guardarOrden(boolean emitir) {
        ComprasRepo.guardarOrden(folio, cmbProveedor.getValue() == null ? null : cmbProveedor.getValue().id(),
                dtpEntrega.getValue(), txtNotasProveedor.getText(), new ArrayList<>(renglones), emitir);
        String guardada = folio;
        if (emitir) {
            lblEstadoOrden.setText("Emitida");
            Ui.info("Orden " + guardada + " emitida. Las existencias se actualizan al registrar la recepción "
                    + "(Historial de compras).");
        } else {
            Ui.info("Orden " + guardada + " guardada como borrador.");
        }
        nuevaOrden();
    }
}
