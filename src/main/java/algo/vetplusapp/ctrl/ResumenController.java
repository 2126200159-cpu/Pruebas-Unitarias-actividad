package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.ActividadRepo;
import algo.vetplusapp.repo.CajaRepo;
import algo.vetplusapp.repo.CitaRepo;
import algo.vetplusapp.repo.ReporteRepo;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Nav;
import algo.vetplusapp.util.Sesion;
import algo.vetplusapp.util.Ui;
import java.time.LocalDate;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Resumen (4.3): indicadores del dia, proximas atenciones, actividad y pension. */
public class ResumenController {

    @FXML
    private Label lblSaludo;

    @FXML
    private Label lblSubtitulo;

    @FXML
    private Button btnNuevaCita;

    @FXML
    private Label lblPacientesDia;

    @FXML
    private Label lblDetallePacientes;

    @FXML
    private Label lblCitasProgramadas;

    @FXML
    private Label lblDetalleCitas;

    @FXML
    private Label lblCobradoHoy;

    @FXML
    private Label lblDetalleCobrado;

    @FXML
    private Label lblMascotasPension;

    @FXML
    private Label lblDetallePension;

    @FXML
    private Hyperlink lnkVerAgenda;

    @FXML
    private TableView<Row> tblProximas;

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
    private HBox cajaAvisoDescuento;

    @FXML
    private Label lblAvisoDescuento;

    @FXML
    private VBox contenedorActividad;

    @FXML
    private Label lblSinActividad;

    @FXML
    private VBox contenedorEstancias;

    @FXML
    private Label lblSinEstancias;

    @FXML
    private void initialize() {
        Ui.mostrar(cajaAvisoDescuento, false);

        Ui.col(colHora, r -> Fmt.rango(r.str("hora_inicio"), r.integer("duracion_min")));
        Ui.col(colPaciente, r -> r.str("mascota") + " / " + r.str("propietario"));
        Ui.col(colServicio, "servicio");
        Ui.col(colProfesional, "profesional");
        Ui.col(colEstado, "estado");

        btnNuevaCita.setOnAction(e -> Nav.ir("agenda", "nuevaCita"));
        lnkVerAgenda.setOnAction(e -> Nav.ir("agenda"));

        cargar();
    }

    private void cargar() {
        String hoy = Config.hoy();
        LocalDate fecha = LocalDate.now();
        String nombre = Sesion.nombreCompleto.isEmpty() ? "" : Sesion.nombreCompleto.split("\\s+")[0];
        lblSaludo.setText(nombre.isEmpty() ? "Buen día" : "Buen día, " + nombre);
        lblSubtitulo.setText("Este es el pulso de tu clínica · " + Fmt.fechaLarga(fecha));

        int pacientes = ReporteRepo.pacientesDelDiaConCita(hoy);
        int atendidos = ReporteRepo.pacientesAtendidos(hoy);
        lblPacientesDia.setText(String.valueOf(pacientes));
        lblDetallePacientes.setText(atendidos + " atendidos · " + pacientes + " en agenda");

        int citas = ReporteRepo.citasDelDia(hoy);
        int enCurso = ReporteRepo.citasConEstado(hoy, "En atención", "En proceso");
        int porAtender = ReporteRepo.citasConEstado(hoy, "Confirmada");
        lblCitasProgramadas.setText(String.valueOf(citas));
        lblDetalleCitas.setText(enCurso + " en curso · " + porAtender + " por atender");

        lblCobradoHoy.setText(Fmt.dinero(CajaRepo.cobradoDelDia(hoy)));
        lblDetalleCobrado.setText(CajaRepo.pacientesConCobro(hoy) + " pacientes · "
                + CajaRepo.cuentasPagadas(hoy) + " cuentas pagadas");

        int pension = ReporteRepo.estanciasActivas(hoy);
        int salidas = ReporteRepo.salidasHoy(hoy);
        lblMascotasPension.setText(String.valueOf(pension));
        lblDetallePension.setText(salidas + (salidas == 1 ? " salida programada hoy" : " salidas programadas hoy"));

        tblProximas.getItems().setAll(CitaRepo.proximasHoy());

        Row aviso = ReporteRepo.avisoDescuentoHoy(hoy);
        if (aviso != null) {
            lblAvisoDescuento.setText(aviso.integer("n_mascotas") + " mascotas de " + aviso.str("propietario")
                    + " en la misma cuenta: aplica " + aviso.integer("descuento_pct")
                    + "% de descuento al total. Revisar en caja →");
            cajaAvisoDescuento.setOnMouseClicked(e -> Nav.ir("caja", aviso.id()));
            Ui.mostrar(cajaAvisoDescuento, true);
        } else {
            Ui.mostrar(cajaAvisoDescuento, false);
        }

        List<Node> actividad = new ArrayList<>();
        for (Row a : ActividadRepo.deHoy(6)) {
            Parent item = Ui.cargarPlantilla("actividad-item.fxml");
            Ui.texto(item, "lblActividadTitulo", a.str("titulo"));
            Ui.texto(item, "lblActividadDetalle", a.str("detalle"));
            actividad.add(item);
        }
        Ui.llenarLista(contenedorActividad, lblSinActividad, actividad);

        List<Node> estancias = new ArrayList<>();
        for (Row e : CitaRepo.estanciasActivas()) {
            Parent item = Ui.cargarPlantilla("estancia-item.fxml");
            LocalDate salida = Fmt.fecha(e.str("fecha_salida_prevista"));
            String sale = salida.equals(LocalDate.now()) ? "Sale hoy, " + e.str("hora_salida")
                    : "Sale " + Fmt.fechaDM(salida);
            Ui.texto(item, "lblNombreMascota", e.str("mascota"));
            Ui.texto(item, "lblDetalleEstancia", Fmt.nvl(e.str("raza"), "Mascota") + " · " + sale);
            Ui.texto(item, "lblDuenoEstancia", e.str("propietario"));
            Ui.texto(item, "lblTelefonoEstancia", "Tel. " + Fmt.nvl(e.str("prop_telefono"), "—") + " ·");
            javafx.scene.control.Hyperlink contactar = (javafx.scene.control.Hyperlink) item.lookup("#lnkContactar");
            contactar.setOnAction(ev -> Ui.contacto(e.str("propietario"), e.str("prop_telefono"),
                    e.str("prop_correo")));
            estancias.add(item);
        }
        Ui.llenarLista(contenedorEstancias, lblSinEstancias, estancias);
    }
}
