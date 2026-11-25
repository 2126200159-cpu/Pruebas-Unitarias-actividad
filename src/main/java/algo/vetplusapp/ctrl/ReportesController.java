package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.CajaRepo;
import algo.vetplusapp.repo.ReporteRepo;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Ui;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.FileChooser;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

/** Reportes (4.9): indicadores, reporte seleccionado (tabla grande) y tres resumenes. */
public class ReportesController {

    @FXML
    private ToggleGroup grupoReportes;

    @FXML
    private Label lblDatosAl;

    @FXML
    private Button btnExportarReporte;

    @FXML
    private ToggleButton tglPacientesDia;

    @FXML
    private ToggleButton tglPacientesServicio;

    @FXML
    private ToggleButton tglContactoCliente;

    @FXML
    private ToggleButton tglMascotasEspecie;

    @FXML
    private ToggleButton tglClientesDescuento;

    @FXML
    private Label lblPacientesCobro;

    @FXML
    private Label lblDetallePacientesCobro;

    @FXML
    private Label lblTotalDia;

    @FXML
    private Label lblDetalleTotalDia;

    @FXML
    private Label lblTicketPaciente;

    @FXML
    private Label lblDetalleTicket;

    @FXML
    private Label lblFechaReporte;

    @FXML
    private Hyperlink lnkCambiarFecha;

    @FXML
    private TableView<Row> tblPacientesDia;

    @FXML
    private TableColumn<Row, String> colRepPaciente;

    @FXML
    private TableColumn<Row, String> colRepPropietario;

    @FXML
    private TableColumn<Row, String> colRepServicio;

    @FXML
    private TableColumn<Row, String> colRepMetodo;

    @FXML
    private TableColumn<Row, String> colRepCobro;

    @FXML
    private Label lblTotalCobradoTitulo;

    @FXML
    private Label lblTotalCobrado;

    @FXML
    private Hyperlink lnkVerReporteServicios;

    @FXML
    private VBox contenedorServicios;

    @FXML
    private Label lblSinServicios;

    @FXML
    private Hyperlink lnkVerReporteEspecies;

    @FXML
    private Label lblEspecies;

    @FXML
    private ProgressBar pbEspecies;

    @FXML
    private Label lblPorcentajesEspecie;

    @FXML
    private Hyperlink lnkClientesDescuento;

    @FXML
    private Label lblClienteDescuento;

    @FXML
    private Label lblAhorroDescuento;

    @FXML
    private Hyperlink lnkContactoCliente;

    @FXML
    private Label lblContactoNombre;

    @FXML
    private Label lblContactoTel;

    @FXML
    private Label lblContactoCorreo;

    private LocalDate fecha = LocalDate.now();
    private String iniContactoNombre;
    private String iniContactoTel;
    private String iniContactoCorreo;
    private String iniEspecies;
    private String iniPorcentajes;
    private String iniClienteDescuento;
    private String iniAhorro;

    @FXML
    private void initialize() {
        iniContactoNombre = lblContactoNombre.getText();
        iniContactoTel = lblContactoTel.getText();
        iniContactoCorreo = lblContactoCorreo.getText();
        iniEspecies = lblEspecies.getText();
        iniPorcentajes = lblPorcentajesEspecie.getText();
        iniClienteDescuento = lblClienteDescuento.getText();
        iniAhorro = lblAhorroDescuento.getText();

        Ui.grupoSiempreUno(grupoReportes, t -> mostrarReporte());
        lnkCambiarFecha.setOnAction(e -> {
            LocalDate nueva = Dialogos.fecha("Cambiar fecha del reporte", fecha);
            if (nueva != null) {
                fecha = nueva;
                cargarTodo();
            }
        });
        lnkVerReporteServicios.setOnAction(e -> tglPacientesServicio.setSelected(true));
        lnkVerReporteEspecies.setOnAction(e -> tglMascotasEspecie.setSelected(true));
        lnkClientesDescuento.setOnAction(e -> tglClientesDescuento.setSelected(true));
        lnkContactoCliente.setOnAction(e -> tglContactoCliente.setSelected(true));
        btnExportarReporte.setOnAction(e -> Ui.accion(this::exportar));

        cargarTodo();
    }

    private void cargarTodo() {
        String f = fecha.toString();
        lblDatosAl.setText("Reportes de atención, ingresos y familias · Datos al "
                + Fmt.fechaDMA(LocalDate.now()) + ", " + Fmt.hora(LocalDateTime.now().toLocalTime()));
        lblFechaReporte.setText(Fmt.fechaLarga(fecha) + " · Cobros liquidados ·");

        int pacientes = CajaRepo.pacientesConCobro(f);
        double total = CajaRepo.cobradoDelDia(f);
        lblPacientesCobro.setText(String.valueOf(pacientes));
        lblTotalDia.setText(Fmt.dinero(total));
        lblTicketPaciente.setText(Fmt.dinero(pacientes == 0 ? 0 : total / pacientes));
        lblTotalCobradoTitulo.setText("Total cobrado · " + Fmt.plural(pacientes, "paciente", "pacientes"));
        lblTotalCobrado.setText(Fmt.dinero(total) + " MXN");

        resumenServicios(f);
        resumenEspecies(f);
        resumenDescuento();
        resumenContacto();
        mostrarReporte();
    }

    // ------------------------------------------------------------------ resumenes de abajo

    private void resumenServicios(String f) {
        List<Row> filas = ReporteRepo.porServicio(f);
        double max = 0;
        for (Row r : filas) {
            max = Math.max(max, r.dbl("importe"));
        }
        List<Node> barras = new ArrayList<>();
        for (Row r : filas) {
            Parent item = Ui.cargarPlantilla("servicio-barra-item.fxml");
            Ui.texto(item, "lblServicioNombre", r.str("servicio") + " · " + r.integer("pacientes") + " pac.");
            Ui.texto(item, "lblServicioImporte", Fmt.dinero(r.dbl("importe")).replace(".00", ""));
            ((javafx.scene.control.ProgressBar) item.lookup("#pbServicio"))
                    .setProgress(max <= 0 ? 0 : r.dbl("importe") / max);
            barras.add(item);
        }
        Ui.llenarLista(contenedorServicios, lblSinServicios, barras);
    }

    private static String pluralEspecie(String especie, int n) {
        String base = especie.toLowerCase(java.util.Locale.ROOT);
        if (n == 1) {
            return base;
        }
        return base.endsWith("o") ? base + "s" : base + "es";
    }

    private void resumenEspecies(String f) {
        List<Row> filas = ReporteRepo.porEspecie(f);
        if (filas.isEmpty()) {
            lblEspecies.setText(iniEspecies);
            pbEspecies.setProgress(0);
            lblPorcentajesEspecie.setText(iniPorcentajes);
            return;
        }
        int total = 0;
        for (Row r : filas) {
            total += r.integer("n");
        }
        StringBuilder cuenta = new StringBuilder();
        StringBuilder pct = new StringBuilder();
        for (Row r : filas) {
            if (cuenta.length() > 0) {
                cuenta.append(" · ");
                pct.append(" · ");
            }
            int n = r.integer("n");
            cuenta.append(n).append(' ').append(pluralEspecie(r.str("especie"), n));
            String etiqueta = r.str("especie");
            pct.append(etiqueta.endsWith("o") ? etiqueta + "s" : etiqueta + "es").append(' ')
                    .append(Math.round(n * 100.0 / total)).append('%');
        }
        lblEspecies.setText(cuenta.toString());
        lblPorcentajesEspecie.setText(pct.toString());
        pbEspecies.setProgress(filas.get(0).integer("n") / (double) total); // la especie mas frecuente
    }

    private void resumenDescuento() {
        Row r = ReporteRepo.mayorDescuentoPendiente();
        if (r == null) {
            lblClienteDescuento.setText(iniClienteDescuento);
            lblAhorroDescuento.setText(iniAhorro);
            return;
        }
        lblClienteDescuento.setText(r.str("propietario") + " · " + r.integer("n_mascotas") + " mascotas · "
                + r.integer("descuento_pct") + "%");
        lblAhorroDescuento.setText("Cuenta pendiente · Ahorro: " + Fmt.dinero(r.dbl("descuento_monto")));
    }

    private void resumenContacto() {
        Row r = ReporteRepo.contactoDeEstancia();
        if (r == null) {
            lblContactoNombre.setText(iniContactoNombre);
            lblContactoTel.setText(iniContactoTel);
            lblContactoCorreo.setText(iniContactoCorreo);
            return;
        }
        lblContactoNombre.setText(r.str("propietario"));
        lblContactoTel.setText(Fmt.nvl(r.str("telefono"), "—"));
        lblContactoCorreo.setText(Fmt.nvl(r.str("correo"), "—"));
    }

    // ------------------------------------------------------------------ tabla grande (5 reportes)

    @SafeVarargs
    private final void columnas(String[] titulos, Function<Row, String>... f) {
        List<TableColumn<Row, String>> cols = java.util.Arrays.asList(colRepPaciente, colRepPropietario,
                colRepServicio, colRepMetodo, colRepCobro);
        for (int i = 0; i < cols.size(); i++) {
            TableColumn<Row, String> c = cols.get(i);
            c.setText(titulos[i]);
            Function<Row, String> fn = i < f.length ? f[i] : r -> "";
            c.setCellValueFactory(d -> new SimpleStringProperty(fn.apply(d.getValue())));
        }
    }

    @SuppressWarnings("unchecked")
    private void mostrarReporte() {
        String f = fecha.toString();
        if (tglPacientesServicio.isSelected()) {
            columnas(new String[]{"Servicio", "Pacientes", "Importe cobrado", "", ""},
                    r -> r.str("servicio"), r -> String.valueOf(r.integer("pacientes")),
                    r -> Fmt.dinero(r.dbl("importe")));
            tblPacientesDia.getItems().setAll(ReporteRepo.porServicio(f));
        } else if (tglContactoCliente.isSelected()) {
            columnas(new String[]{"Propietario", "Teléfono", "Correo", "Mascotas", ""},
                    r -> r.str("propietario"), r -> Fmt.nvl(r.str("telefono"), "—"),
                    r -> Fmt.nvl(r.str("correo"), "—"), r -> r.str("mascotas"));
            tblPacientesDia.getItems().setAll(ReporteRepo.contactosDelDia(f));
        } else if (tglMascotasEspecie.isSelected()) {
            List<Row> filas = ReporteRepo.porEspecie(f);
            int total = 0;
            for (Row r : filas) {
                total += r.integer("n");
            }
            final int totalFinal = total;
            columnas(new String[]{"Especie", "Mascotas cobradas", "Porcentaje", "", ""},
                    r -> r.str("especie"), r -> String.valueOf(r.integer("n")),
                    r -> totalFinal == 0 ? "—" : Math.round(r.integer("n") * 100.0 / totalFinal) + "%");
            tblPacientesDia.getItems().setAll(filas);
        } else if (tglClientesDescuento.isSelected()) {
            columnas(new String[]{"Propietario", "Mascotas", "Descuento", "Ahorro", "Cuenta"},
                    r -> r.str("propietario"), r -> String.valueOf(r.integer("n_mascotas")),
                    r -> r.integer("pct") + "%", r -> Fmt.dinero(r.dbl("ahorro")),
                    r -> "PAGADA".equals(r.str("estado")) ? "Pagada" : "Pendiente");
            tblPacientesDia.getItems().setAll(ReporteRepo.clientesConDescuento(f));
        } else {
            columnas(new String[]{"Paciente", "Propietario", "Servicio", "Método", "Cobro individual"},
                    r -> r.str("paciente"), r -> r.str("propietario"), r -> r.str("servicio"),
                    r -> metodo(r.str("metodo")), r -> Fmt.dinero(r.dbl("cobro")));
            tblPacientesDia.getItems().setAll(ReporteRepo.pacientesDelDia(f));
        }
    }

    private static String metodo(String m) {
        return m.isEmpty() ? "" : m.substring(0, 1) + m.substring(1).toLowerCase(java.util.Locale.ROOT);
    }

    // ------------------------------------------------------------------ exportar CSV

    private static String csv(String s) {
        String limpio = s == null ? "" : s.replace("\"", "\"\"");
        return "\"" + limpio + "\"";
    }

    private void exportar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar reporte");
        fc.setInitialFileName("reporte-" + fecha + ".csv");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        java.io.File destino = fc.showSaveDialog(null);
        if (destino == null) {
            return;
        }
        try (Writer w = Files.newBufferedWriter(destino.toPath(), StandardCharsets.UTF_8)) {
            w.write('\uFEFF'); // para que Excel reconozca los acentos
            StringBuilder cab = new StringBuilder();
            for (TableColumn<Row, ?> c : tblPacientesDia.getColumns()) {
                if (!c.getText().isEmpty()) {
                    cab.append(cab.length() > 0 ? "," : "").append(csv(c.getText()));
                }
            }
            w.write(cab + "\r\n");
            for (Row fila : tblPacientesDia.getItems()) {
                StringBuilder linea = new StringBuilder();
                for (TableColumn<Row, ?> c : tblPacientesDia.getColumns()) {
                    if (!c.getText().isEmpty()) {
                        Object v = c.getCellData(fila);
                        linea.append(linea.length() > 0 ? "," : "").append(csv(v == null ? "" : v.toString()));
                    }
                }
                w.write(linea + "\r\n");
            }
        } catch (IOException ex) {
            Ui.error("No se pudo guardar el archivo: " + ex.getMessage());
            return;
        }
        Ui.info("Reporte exportado a " + destino.getName());
    }
}
