package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.CajaRepo;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Nav;
import algo.vetplusapp.util.RecibeDatos;
import algo.vetplusapp.util.Ui;
import algo.vetplusapp.util.Valida;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/** Caja y pagos (4.8): cuenta con descuento familiar, pendientes, pagos del dia, cobro y cierre. */
public class CajaController implements RecibeDatos {

    @FXML
    private Button btnNuevaCuenta;

    @FXML
    private Label lblClienteCuenta;

    @FXML
    private Label lblDetalleCuenta;

    @FXML
    private Label lblEstadoCuenta;

    @FXML
    private TableView<Row> tblCuenta;

    @FXML
    private TableColumn<Row, String> colCuentaPaciente;

    @FXML
    private TableColumn<Row, String> colCuentaServicio;

    @FXML
    private TableColumn<Row, String> colCuentaCantidad;

    @FXML
    private TableColumn<Row, String> colCuentaPrecio;

    @FXML
    private TableColumn<Row, String> colCuentaImporte;

    @FXML
    private HBox cajaAvisoDescuento;

    @FXML
    private Label lblAvisoDescuento;

    @FXML
    private Label lblSubtotal;

    @FXML
    private Label lblDescuentoEtiqueta;

    @FXML
    private Label lblDescuentoMonto;

    @FXML
    private Label lblTotalPagar;

    @FXML
    private TableView<Row> tblPendientes;

    @FXML
    private TableColumn<Row, String> colPendCliente;

    @FXML
    private TableColumn<Row, String> colPendConcepto;

    @FXML
    private TableColumn<Row, String> colPendFecha;

    @FXML
    private TableColumn<Row, String> colPendSaldo;

    @FXML
    private TableView<Row> tblPagos;

    @FXML
    private TableColumn<Row, String> colPagoHora;

    @FXML
    private TableColumn<Row, String> colPagoCliente;

    @FXML
    private TableColumn<Row, String> colPagoMetodo;

    @FXML
    private TableColumn<Row, String> colPagoCobrado;

    @FXML
    private Label lblClienteCobro;

    @FXML
    private ComboBox<String> cmbMetodoPago;

    @FXML
    private Button btnMetodoTarjeta;

    @FXML
    private Button btnMetodoEfectivo;

    @FXML
    private Button btnMetodoTransferencia;

    @FXML
    private TextField txtImporteCobrar;

    @FXML
    private TextField txtReferenciaPago;

    @FXML
    private TextField txtCorreoComprobante;

    @FXML
    private HBox cajaAvisoCobro;

    @FXML
    private Label lblAvisoCobro;

    @FXML
    private Button btnConfirmarCobro;

    @FXML
    private Button btnGuardarPendiente;

    @FXML
    private Label lblCobradoHoy;

    @FXML
    private Label lblResumenCierre;

    @FXML
    private Hyperlink lnkVerReporteDia;

    private Row cuenta;
    private boolean cargandoTabla = false;
    private String iniClienteCuenta;
    private String iniDetalleCuenta;
    private String iniClienteCobro;
    private String iniConfirmar;
    private String iniEtiquetaDescuento;
    private String iniDescuentoMonto;

    @FXML
    private void initialize() {
        iniClienteCuenta = lblClienteCuenta.getText();
        iniDetalleCuenta = lblDetalleCuenta.getText();
        iniClienteCobro = lblClienteCobro.getText();
        iniConfirmar = btnConfirmarCobro.getText();
        iniEtiquetaDescuento = lblDescuentoEtiqueta.getText();
        iniDescuentoMonto = lblDescuentoMonto.getText();

        Ui.col(colCuentaPaciente, "mascota");
        Ui.col(colCuentaServicio, "servicio");
        Ui.col(colCuentaCantidad, r -> String.valueOf(r.integer("cantidad")));
        Ui.col(colCuentaPrecio, r -> Fmt.dinero(r.dbl("precio_unitario")));
        Ui.col(colCuentaImporte, r -> Fmt.dinero(r.dbl("importe")));
        Ui.col(colPendCliente, "propietario");
        Ui.col(colPendConcepto, "concepto");
        Ui.col(colPendFecha, r -> Fmt.fechaDMA(Fmt.fecha(r.str("fecha"))));
        Ui.col(colPendSaldo, r -> Fmt.dinero(r.dbl("saldo")));
        Ui.col(colPagoHora, r -> r.str("fecha_hora").length() >= 16 ? r.str("fecha_hora").substring(11, 16) : "");
        Ui.col(colPagoCliente, "propietario");
        Ui.col(colPagoMetodo, r -> metodoTexto(r.str("metodo")));
        Ui.col(colPagoCobrado, r -> Fmt.dinero(r.dbl("importe")));

        cmbMetodoPago.getItems().setAll(Config.METODOS);
        cmbMetodoPago.valueProperty().addListener((o, a, m) -> {
            boolean efectivo = "Efectivo".equals(m);
            txtReferenciaPago.setDisable(efectivo);
            if (efectivo) {
                txtReferenciaPago.clear();
            }
        });
        btnMetodoTarjeta.setOnAction(e -> cmbMetodoPago.setValue("Tarjeta"));
        btnMetodoEfectivo.setOnAction(e -> cmbMetodoPago.setValue("Efectivo"));
        btnMetodoTransferencia.setOnAction(e -> cmbMetodoPago.setValue("Transferencia"));

        tblPendientes.getSelectionModel().selectedItemProperty().addListener((o, a, fila) -> {
            if (!cargandoTabla && fila != null) {
                cargarCuenta(fila.id());
            }
        });

        btnNuevaCuenta.setOnAction(e -> Ui.accion(() -> {
            Long id = Dialogos.cuentaManual();
            if (id != null) {
                refrescarListas();
                cargarCuenta(id);
            }
        }));
        btnConfirmarCobro.setOnAction(e -> Ui.accion(this::cobrar));
        btnGuardarPendiente.setOnAction(e -> {
            if (cuenta == null) {
                Ui.aviso("Selecciona una cuenta.");
                return;
            }
            refrescarListas();
            Ui.info("La cuenta quedó guardada como pendiente de pago.");
        });
        lnkVerReporteDia.setOnAction(e -> Nav.ir("reportes"));

        limpiarCuenta();
        refrescarListas();
    }

    @Override
    public void cargar(Object dato) {
        if (dato instanceof Long) {
            cargarCuenta((Long) dato);
        }
    }

    private static String metodoTexto(String m) {
        if (m.isEmpty()) {
            return "";
        }
        return m.substring(0, 1) + m.substring(1).toLowerCase(java.util.Locale.ROOT);
    }

    private void refrescarListas() {
        cargandoTabla = true;
        tblPendientes.getItems().setAll(CajaRepo.pendientes());
        cargandoTabla = false;
        String hoy = Config.hoy();
        tblPagos.getItems().setAll(CajaRepo.pagosDelDia(hoy));
        lblCobradoHoy.setText("Cobrado hoy: " + Fmt.dinero(CajaRepo.cobradoDelDia(hoy)));
        lblResumenCierre.setText(CajaRepo.pacientesConCobro(hoy) + " pacientes · " + CajaRepo.cuentasPagadas(hoy)
                + " cuentas pagadas");
    }

    private void limpiarCuenta() {
        cuenta = null;
        lblClienteCuenta.setText(iniClienteCuenta);
        lblDetalleCuenta.setText(iniDetalleCuenta);
        Ui.mostrar(lblEstadoCuenta, false);
        tblCuenta.getItems().clear();
        Ui.mostrar(cajaAvisoDescuento, false);
        lblSubtotal.setText(Fmt.dinero(0));
        lblDescuentoEtiqueta.setText(iniEtiquetaDescuento);
        lblDescuentoMonto.setText(iniDescuentoMonto);
        lblTotalPagar.setText(Fmt.dinero(0));
        lblClienteCobro.setText(iniClienteCobro);
        cmbMetodoPago.setValue(null);
        txtImporteCobrar.clear();
        txtReferenciaPago.clear();
        txtCorreoComprobante.clear();
        btnConfirmarCobro.setText(iniConfirmar);
        btnConfirmarCobro.setDisable(false);
        Ui.mostrar(cajaAvisoCobro, false);
    }

    private void cargarCuenta(long id) {
        Row c = CajaRepo.cuenta(id);
        if (c == null) {
            limpiarCuenta();
            return;
        }
        cuenta = c;
        boolean pagada = "PAGADA".equals(c.str("estado"));
        List<Row> renglones = CajaRepo.detalle(id);
        lblClienteCuenta.setText(c.str("propietario"));
        lblDetalleCuenta.setText("Cuenta del " + Fmt.fechaDMA(Fmt.fecha(c.str("fecha"))) + " · "
                + Fmt.plural(c.integer("n_mascotas"), "mascota distinta", "mascotas distintas"));
        lblEstadoCuenta.setText(pagada ? "Pagada" : "Pendiente de pago");
        Ui.mostrar(lblEstadoCuenta, true);
        tblCuenta.getItems().setAll(renglones);

        lblSubtotal.setText(Fmt.dinero(c.dbl("subtotal")));
        lblDescuentoEtiqueta.setText("Descuento " + c.integer("descuento_pct") + "%");
        lblDescuentoMonto.setText(Fmt.descuento(c.dbl("descuento_monto")));
        lblTotalPagar.setText(Fmt.dinero(c.dbl("total")));

        if (c.integer("n_mascotas") >= 2) {
            lblAvisoDescuento.setText("Descuento familiar del " + c.integer("descuento_pct") + "%: "
                    + nombresMascotas(id) + " son " + c.integer("n_mascotas")
                    + " mascotas distintas del mismo cliente en esta cuenta.");
            Ui.mostrar(cajaAvisoDescuento, true);
        } else {
            Ui.mostrar(cajaAvisoDescuento, false);
        }

        lblClienteCobro.setText("Cobrar a " + c.str("propietario"));
        txtImporteCobrar.setText(Fmt.importe(c.dbl("total")));
        txtCorreoComprobante.setText(c.str("prop_correo"));
        btnConfirmarCobro.setText("Confirmar cobro · " + Fmt.dinero(c.dbl("total")));
        btnConfirmarCobro.setDisable(pagada);
        btnGuardarPendiente.setDisable(pagada);
        lblAvisoCobro.setText(pagada ? "Esta cuenta ya fue cobrada."
                : "El cobro liquidará los " + c.integer("n_servicios")
                + " servicios de esta cuenta y quedará vinculado a sus citas.");
        Ui.mostrar(cajaAvisoCobro, true);
    }

    private String nombresMascotas(long cuentaId) {
        List<Row> m = CajaRepo.mascotasDeCuenta(cuentaId);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m.size(); i++) {
            if (i > 0) {
                sb.append(i == m.size() - 1 ? " y " : ", ");
            }
            sb.append(m.get(i).str("nombre"));
        }
        return sb.toString();
    }

    private void cobrar() {
        if (cuenta == null) {
            Ui.aviso("Selecciona una cuenta para cobrar.");
            return;
        }
        double importe = Valida.numero(txtImporteCobrar.getText(), "El importe", true);
        CajaRepo.cobrar(cuenta.id(), cmbMetodoPago.getValue(), importe, txtReferenciaPago.getText(),
                txtCorreoComprobante.getText());
        limpiarCuenta();
        refrescarListas();
        Ui.info("Cobro registrado. La cuenta quedó pagada.");
    }
}
