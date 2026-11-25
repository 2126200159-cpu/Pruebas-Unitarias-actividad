package algo.vetplusapp;

import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Nav;
import algo.vetplusapp.util.Navegador;
import algo.vetplusapp.util.PuedeSalir;
import algo.vetplusapp.util.RecibeDatos;
import algo.vetplusapp.util.Sesion;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Logica comun de los tres marcos (administracion, medico, estilista): abre vistas, les pasa el dato
 * (id de mascota, cita...) y llena la barra superior con la sesion.
 */
public abstract class ShellBase implements Navegador {

    @FXML
    protected StackPane contenedorVistas;
    @FXML
    protected ScrollPane scrollVistas;
    @FXML
    protected Label lblMigaPan;
    @FXML
    protected Label lblNombreUsuario;
    @FXML
    protected Label lblIniciales;
    @FXML
    protected Label lblFecha;
    @FXML
    protected Hyperlink lnkCerrarSesion;
    @FXML
    protected Button btnNotificaciones;

    private final Map<String, ToggleButton> menu = new HashMap<>();
    private final Map<String, String> titulos = new HashMap<>();
    private String vistaActual;
    private Object controladorActual;

    /** Avisos simples para el boton de la campana. */
    protected abstract List<String> avisos();

    protected void registrar(String vista, String titulo, ToggleButton boton) {
        menu.put(vista, boton);
        titulos.put(vista, titulo);
        boton.setOnAction(e -> ir(vista, null));
    }

    /** Vista que no tiene boton propio en el menu: marca el de la vista "padre". */
    protected void registrarSecundaria(String vista, String titulo, ToggleButton botonPadre) {
        menu.put(vista, botonPadre);
        titulos.put(vista, titulo);
    }

    protected void iniciarMarco(String vistaInicial) {
        Nav.registrar(this);
        lblNombreUsuario.setText(Sesion.nombreCompleto);
        lblIniciales.setText(Fmt.iniciales(Sesion.nombreCompleto));
        lblFecha.setText(Fmt.fechaCorta(LocalDate.now()));
        lnkCerrarSesion.setOnAction(e -> {
            if (controladorActual instanceof PuedeSalir && !((PuedeSalir) controladorActual).puedeSalir()) {
                return;
            }
            algo.vetplusapp.util.AppLog.info("ShellBase", "cerrarSesion", "Cierre de sesión de " + Sesion.nombreCompleto);
            Sesion.limpiar();
            Nav.registrar(null);
            App.mostrarLogin();
        });
        btnNotificaciones.setOnAction(e -> mostrarAvisos());
        ir(vistaInicial, null);
    }

    private void mostrarAvisos() {
        ContextMenu cm = new ContextMenu();
        List<String> lista = avisos();
        if (lista.isEmpty()) {
            cm.getItems().add(new MenuItem("Sin avisos por ahora."));
        } else {
            for (String a : lista) {
                cm.getItems().add(new MenuItem(a));
            }
        }
        cm.show(btnNotificaciones, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @Override
    public void ir(String vista, Object dato) {
        if (controladorActual instanceof PuedeSalir && !((PuedeSalir) controladorActual).puedeSalir()) {
            if (vistaActual != null && menu.get(vistaActual) != null) {
                menu.get(vistaActual).setSelected(true);
            }
            return;
        }
        Node contenido;
        Object controlador = null;
        URL recurso = getClass().getResource("/view/" + vista + ".fxml");
        if (recurso == null) {
            Label aviso = new Label("Esta pantalla estará disponible pronto.");
            aviso.getStyleClass().add("texto-13");
            VBox caja = new VBox(aviso);
            caja.setPadding(new Insets(32));
            contenido = caja;
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(recurso);
                contenido = loader.load();
                controlador = loader.getController();
            } catch (IOException e) {
                throw new RuntimeException("No se pudo cargar la vista " + vista, e);
            }
        }
        vistaActual = vista;
        controladorActual = controlador;
        contenedorVistas.getChildren().setAll(contenido);
        scrollVistas.setVvalue(0);
        lblMigaPan.setText("Clínica VETPLUS / " + titulos.getOrDefault(vista, vista));
        ToggleButton boton = menu.get(vista);
        if (boton != null) {
            boton.setSelected(true);
        }
        if (controlador instanceof RecibeDatos) {
            ((RecibeDatos) controlador).cargar(dato);
        }
    }
}
