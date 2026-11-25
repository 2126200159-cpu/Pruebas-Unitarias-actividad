package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.ServicioRepo;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Ui;
import algo.vetplusapp.util.Valida;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Servicios y tarifas (4.7): catalogo fijo de 6 servicios, solo se editan precio y duracion. */
public class ServiciosController {

    @FXML
    private Button btnGuardarCambios;

    @FXML
    private VBox tarjetaConsulta;

    @FXML
    private Label lblEstadoConsulta;

    @FXML
    private Label lblPrecioConsulta;

    @FXML
    private Label lblDuracionConsulta;

    @FXML
    private Hyperlink lnkEditarConsulta;

    @FXML
    private VBox tarjetaVacunacion;

    @FXML
    private Label lblEstadoVacunacion;

    @FXML
    private Label lblPrecioVacunacion;

    @FXML
    private Label lblDuracionVacunacion;

    @FXML
    private Hyperlink lnkEditarVacunacion;

    @FXML
    private VBox tarjetaDesparasitacion;

    @FXML
    private Label lblEstadoDesparasitacion;

    @FXML
    private Label lblPrecioDesparasitacion;

    @FXML
    private Label lblDuracionDesparasitacion;

    @FXML
    private Hyperlink lnkEditarDesparasitacion;

    @FXML
    private VBox tarjetaCirugia;

    @FXML
    private Label lblEstadoCirugia;

    @FXML
    private Label lblPrecioCirugia;

    @FXML
    private Label lblDuracionCirugia;

    @FXML
    private Hyperlink lnkEditarCirugia;

    @FXML
    private VBox tarjetaEstetica;

    @FXML
    private Label lblEstadoEstetica;

    @FXML
    private Label lblPrecioEstetica;

    @FXML
    private Label lblDuracionEstetica;

    @FXML
    private Hyperlink lnkEditarEstetica;

    @FXML
    private VBox tarjetaPension;

    @FXML
    private Label lblEstadoPension;

    @FXML
    private Label lblPrecioPension;

    @FXML
    private Label lblDuracionPension;

    @FXML
    private Hyperlink lnkEditarPension;

    @FXML
    private ComboBox<String> cmbServicioTarifa;

    @FXML
    private TextField txtPrecioServicio;

    @FXML
    private TextField txtDuracionServicio;

    @FXML
    private Button btnActualizarTarifa;

    @FXML
    private Button btnNuevoServicio;

    @FXML
    private Button btnEliminarServicio;

    @FXML
    private Label lblDescuento1;

    @FXML
    private Label lblDescuento2;

    @FXML
    private Label lblDescuento3;

    private final Map<String, Label> estados = new LinkedHashMap<>();
    private final Map<String, Label> precios = new LinkedHashMap<>();
    private final Map<String, Label> duraciones = new LinkedHashMap<>();
    private final Map<String, Hyperlink> enlaces = new LinkedHashMap<>();
    private boolean llenando = false;

    @FXML
    private void initialize() {
        estados.put("Consulta", lblEstadoConsulta);
        estados.put("Vacunación", lblEstadoVacunacion);
        estados.put("Desparasitación", lblEstadoDesparasitacion);
        estados.put("Cirugía", lblEstadoCirugia);
        estados.put("Estética", lblEstadoEstetica);
        estados.put("Pensión", lblEstadoPension);
        precios.put("Consulta", lblPrecioConsulta);
        precios.put("Vacunación", lblPrecioVacunacion);
        precios.put("Desparasitación", lblPrecioDesparasitacion);
        precios.put("Cirugía", lblPrecioCirugia);
        precios.put("Estética", lblPrecioEstetica);
        precios.put("Pensión", lblPrecioPension);
        duraciones.put("Consulta", lblDuracionConsulta);
        duraciones.put("Vacunación", lblDuracionVacunacion);
        duraciones.put("Desparasitación", lblDuracionDesparasitacion);
        duraciones.put("Cirugía", lblDuracionCirugia);
        duraciones.put("Estética", lblDuracionEstetica);
        enlaces.put("Consulta", lnkEditarConsulta);
        enlaces.put("Vacunación", lnkEditarVacunacion);
        enlaces.put("Desparasitación", lnkEditarDesparasitacion);
        enlaces.put("Cirugía", lnkEditarCirugia);
        enlaces.put("Estética", lnkEditarEstetica);
        enlaces.put("Pensión", lnkEditarPension);

        for (Map.Entry<String, Hyperlink> en : enlaces.entrySet()) {
            String nombre = en.getKey();
            en.getValue().setOnAction(e -> cmbServicioTarifa.getSelectionModel().select(nombre));
        }
        for (Map.Entry<String, Label> en : estados.entrySet()) {
            String nombre = en.getKey();
            en.getValue().setCursor(javafx.scene.Cursor.HAND);
            en.getValue().setOnMouseClicked(e -> Ui.accion(() -> alternarActivo(nombre)));
        }

        recargarCombo();
        cmbServicioTarifa.valueProperty().addListener((o, a, nombre) -> {
            if (!llenando) {
                llenarFormulario(nombre);
            }
        });

        btnActualizarTarifa.setOnAction(e -> Ui.accion(this::actualizar));
        btnNuevoServicio.setOnAction(e -> Ui.accion(this::nuevo));
        btnEliminarServicio.setOnAction(e -> Ui.accion(this::eliminar));
        // confirmacion general: guarda lo que haya en el formulario (igual que Actualizar tarifa)
        btnGuardarCambios.setOnAction(e -> Ui.accion(() -> {
            if (cmbServicioTarifa.getValue() == null) {
                Ui.info("No hay cambios por guardar. Elige un servicio con «Editar tarifa →».");
            } else {
                actualizar();
            }
        }));

        refrescar();
    }

    private void refrescar() {
        for (Row s : ServicioRepo.todos()) {
            String n = s.str("nombre");
            if (!estados.containsKey(n)) {
                continue;
            }
            estados.get(n).setText(s.bool("activo") ? "Activo" : "Inactivo");
            precios.get(n).setText(Fmt.dinero(s.dbl("precio")));
            if (duraciones.containsKey(n)) {
                duraciones.get(n).setText(s.esNulo("duracion_min") ? "—" : s.integer("duracion_min") + " min");
            }
        }
    }

    private void llenarFormulario(String nombre) {
        if (nombre == null) {
            txtPrecioServicio.clear();
            txtDuracionServicio.clear();
            return;
        }
        Row s = ServicioRepo.porNombre(nombre);
        txtPrecioServicio.setText(Fmt.importe(s.dbl("precio")));
        boolean pension = "PENSION".equals(s.str("tipo"));
        txtDuracionServicio.setDisable(pension);
        txtDuracionServicio.setText(pension || s.esNulo("duracion_min") ? "" : String.valueOf(s.integer("duracion_min")));
    }

    private void actualizar() {
        String nombre = cmbServicioTarifa.getValue();
        if (nombre == null) {
            Ui.aviso("Elige el servicio que quieres actualizar.");
            return;
        }
        Row s = ServicioRepo.porNombre(nombre);
        double precio = Valida.numero(txtPrecioServicio.getText(), "El precio", true);
        Integer duracion = "PENSION".equals(s.str("tipo")) ? null
                : Valida.entero(txtDuracionServicio.getText(), "La duración", true);
        ServicioRepo.actualizarTarifa(s.id(), precio, duracion);
        refrescar();
        Ui.info("Tarifa actualizada. Aplica solo a las cuentas nuevas.");
    }

    private void alternarActivo(String nombre) {
        Row s = ServicioRepo.porNombre(nombre);
        boolean activo = s.bool("activo");
        if (Ui.confirmar(activo ? "¿Desactivar el servicio " + nombre + "?" : "¿Activar el servicio " + nombre + "?")) {
            ServicioRepo.cambiarActivo(s.id(), !activo);
            refrescar();
        }
    }

    /** Todos los servicios (base y agregados) en la lista de edicion. */
    private void recargarCombo() {
        llenando = true;
        String actual = cmbServicioTarifa.getValue();
        java.util.List<String> nombres = new java.util.ArrayList<>();
        for (Row s : ServicioRepo.todos()) {
            nombres.add(s.str("nombre"));
        }
        cmbServicioTarifa.getItems().setAll(nombres);
        cmbServicioTarifa.setValue(nombres.contains(actual) ? actual : null);
        llenando = false;
        if (cmbServicioTarifa.getValue() == null) {
            llenarFormulario(null);
        }
    }

    private void nuevo() {
        Long id = Dialogos.servicioNuevo();
        if (id != null) {
            recargarCombo();
            cmbServicioTarifa.setValue(ServicioRepo.porId(id).str("nombre"));
            Ui.info("Servicio registrado. Ya aparece al agendar citas.");
        }
    }

    private void eliminar() {
        String nombre = cmbServicioTarifa.getValue();
        if (nombre == null) {
            Ui.aviso("Elige el servicio que quieres eliminar.");
            return;
        }
        Row s = ServicioRepo.porNombre(nombre);
        if (ServicioRepo.esBase(nombre) || ServicioRepo.tieneRelaciones(s.id())) {
            ServicioRepo.eliminar(s.id()); // lanza el aviso que explica por que no se puede
            return;
        }
        if (Ui.confirmar("¿Eliminar el servicio «" + nombre + "»? Esta acción no se puede deshacer.")) {
            ServicioRepo.eliminar(s.id());
            recargarCombo();
            Ui.info("Servicio eliminado.");
        }
    }
}
