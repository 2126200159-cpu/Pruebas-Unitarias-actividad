package algo.vetplusapp.util;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Row;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

/** Ayudas de interfaz que se repiten en todas las pantallas (capitulos 2.3 a 2.6). */
public final class Ui {

    private Ui() {
    }

    /** Mostrar u ocultar: hacen falta visible y managed a la vez. */
    public static void mostrar(Node nodo, boolean visible) {
        if (nodo != null) {
            nodo.setVisible(visible);
            nodo.setManaged(visible);
        }
    }

    /** Carga una plantilla de /view y devuelve su raiz. */
    public static Parent cargarPlantilla(String archivo) {
        try {
            return FXMLLoader.load(Ui.class.getResource("/view/" + archivo));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la plantilla " + archivo, e);
        }
    }

    /** Llena un contenedor; si no hay elementos, deja el mensaje de vacio. */
    public static void llenarLista(VBox contenedor, Label vacio, List<Node> items) {
        if (items.isEmpty()) {
            contenedor.getChildren().setAll(vacio);
            mostrar(vacio, true);
        } else {
            contenedor.getChildren().setAll(items);
        }
    }

    public static Label etiqueta(Node raiz, String id) {
        return (Label) raiz.lookup("#" + id);
    }

    public static void texto(Node raiz, String id, String valor) {
        Node n = raiz.lookup("#" + id);
        if (n instanceof Label) {
            ((Label) n).setText(valor);
        }
    }

    public static void ocultarEnPlantilla(Node raiz, String id) {
        mostrar(raiz.lookup("#" + id), false);
    }

    public static void texto(Label l, String valor) {
        if (l != null) {
            l.setText(valor);
        }
    }

    /** Columna de texto tomada de una columna de la fila. */
    public static void col(TableColumn<Row, String> c, String clave) {
        c.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().str(clave)));
    }

    public static void col(TableColumn<Row, String> c, Function<Row, String> f) {
        c.setCellValueFactory(d -> new SimpleStringProperty(f.apply(d.getValue())));
    }

    /** Hace que siempre quede un boton marcado en el grupo (capitulo 2.6). */
    public static void grupoSiempreUno(ToggleGroup grupo, Consumer<Toggle> alCambiar) {
        grupo.selectedToggleProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo == null) {
                if (viejo != null) {
                    viejo.setSelected(true);
                }
                return;
            }
            alCambiar.accept(nuevo);
        });
    }

    public static void info(String mensaje) {
        alerta(Alert.AlertType.INFORMATION, "VETPLUS", mensaje);
    }

    public static void aviso(String mensaje) {
        alerta(Alert.AlertType.WARNING, "VETPLUS", mensaje);
    }

    public static void error(String mensaje) {
        alerta(Alert.AlertType.ERROR, "VETPLUS", mensaje);
    }

    private static void alerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert a = new Alert(tipo, mensaje, ButtonType.OK);
        a.setTitle(titulo);
        a.setHeaderText(null);
        copiable(a);
        Estilo.aplicar(a);
        a.showAndWait();
    }

    /** Copia texto al portapapeles del sistema. */
    public static void copiarTexto(String texto) {
        ClipboardContent cc = new ClipboardContent();
        cc.putString(texto);
        Clipboard.getSystemClipboard().setContent(cc);
    }

    /** Permite copiar el mensaje de una ventana de aviso: Ctrl+C o clic derecho > Copiar texto. */
    private static void copiable(Alert a) {
        a.getDialogPane().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            if (e.isShortcutDown() && e.getCode() == javafx.scene.input.KeyCode.C) {
                copiarTexto(a.getContentText());
                e.consume();
            }
        });
        a.getDialogPane().setOnContextMenuRequested(e -> {
            javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
            javafx.scene.control.MenuItem copiar = new javafx.scene.control.MenuItem("Copiar texto");
            copiar.setOnAction(ev -> copiarTexto(a.getContentText()));
            menu.getItems().add(copiar);
            menu.show(a.getDialogPane(), e.getScreenX(), e.getScreenY());
        });
    }

    public static boolean confirmar(String mensaje) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        a.setTitle("VETPLUS");
        a.setHeaderText(null);
        copiable(a);
        Estilo.aplicar(a);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }

    /** Si/No/Cancelar: devuelve YES, NO o CANCEL_CLOSE. */
    public static ButtonType preguntarGuardar(String mensaje) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        a.setTitle("VETPLUS");
        a.setHeaderText(null);
        copiable(a);
        Estilo.aplicar(a);
        return a.showAndWait().orElse(ButtonType.CANCEL);
    }

    /**
     * Ejecuta la accion de un boton. Si algo falla muestra un mensaje claro (regla del negocio o error tecnico),
     * lo registra en el log y la aplicacion sigue funcionando.
     */
    public static void accion(Runnable r) {
        Manejo.ejecutar(r).ifPresent(res -> {
            if (res.esError) {
                error(res.mensaje);
            } else {
                aviso(res.mensaje);
            }
        });
    }

    /** Dialogo de contacto con telefono, correo y boton para copiar (capitulo 8). */
    public static void contacto(String titulo, String telefono, String correo) {
        String texto = "Teléfono: " + Fmt.nvl(telefono, "—") + "\nCorreo: " + Fmt.nvl(correo, "—");
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Contactar");
        a.setHeaderText(titulo);
        TextArea area = new TextArea(texto);
        area.setEditable(false);
        area.setPrefRowCount(3);
        area.setPrefColumnCount(28);
        area.getStyleClass().add("campo-area");
        a.getDialogPane().setContent(area);
        ButtonType copiar = new ButtonType("Copiar", javafx.scene.control.ButtonBar.ButtonData.LEFT);
        a.getButtonTypes().setAll(copiar, ButtonType.CLOSE);
        Estilo.aplicar(a);
        Estilo.primario(a, copiar);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isPresent() && r.get() == copiar) {
            copiarTexto(texto);
        }
    }

    /** Desplaza el ScrollPane que contiene al nodo hasta dejarlo arriba (pestanas que solo hacen scroll). */
    public static void desplazarA(Node nodo) {
        Node padre = nodo.getParent();
        while (padre != null && !(padre instanceof javafx.scene.control.ScrollPane)) {
            padre = padre.getParent();
        }
        if (padre == null) {
            return;
        }
        javafx.scene.control.ScrollPane sp = (javafx.scene.control.ScrollPane) padre;
        Node contenido = sp.getContent();
        double altoContenido = contenido.getBoundsInLocal().getHeight();
        double altoVista = sp.getViewportBounds().getHeight();
        if (altoContenido <= altoVista) {
            return;
        }
        double y = nodo.localToScene(0, 0).getY() - contenido.localToScene(0, 0).getY();
        sp.setVvalue(Math.max(0, Math.min(1, y / (altoContenido - altoVista))));
    }

    /**
     * Muestra una contrasena temporal en una caja seleccionable con boton "Copiar contraseña".
     * El boton no cierra la ventana: cambia a "Copiado" para confirmar.
     */
    public static void credencialTemporal(String titulo, String usuario, String contrasena, String nota) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("VETPLUS");
        a.setHeaderText(titulo);
        javafx.scene.control.TextField campo = new javafx.scene.control.TextField(contrasena);
        campo.setEditable(false);
        campo.getStyleClass().add("campo-texto");
        campo.setStyle("-fx-font-family: monospace; -fx-font-size: 16px;");
        Label lblUsuario = new Label(usuario.isEmpty() ? "" : "Usuario: " + usuario);
        lblUsuario.getStyleClass().add("texto-14-negrita");
        Label lblClave = new Label("Contraseña temporal:");
        lblClave.getStyleClass().add("campo-etiqueta");
        Label lblNota = new Label(nota);
        lblNota.getStyleClass().add("texto-13");
        lblNota.setWrapText(true);
        lblNota.setMaxWidth(340);
        VBox caja = new VBox(8, lblUsuario, lblClave, campo, lblNota);
        a.getDialogPane().setContent(caja);
        ButtonType copiar = new ButtonType("Copiar contraseña", javafx.scene.control.ButtonBar.ButtonData.LEFT);
        a.getButtonTypes().setAll(copiar, ButtonType.CLOSE);
        Estilo.aplicar(a);
        Estilo.primario(a, copiar);
        javafx.scene.control.Button botonCopiar = (javafx.scene.control.Button) a.getDialogPane()
                .lookupButton(copiar);
        botonCopiar.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            copiarTexto(contrasena);
            botonCopiar.setText("Copiado ✓");
            ev.consume(); // la ventana sigue abierta
        });
        campo.requestFocus();
        campo.selectAll();
        a.showAndWait();
    }

    // ------------------------------------------------------------------ filtros de captura

    /** Muestra un aviso rojo flotante junto al campo durante unos segundos (sin ventana emergente). */
    public static void avisoCampo(Node campo, String mensaje) {
        if (campo.getScene() == null || campo.getScene().getWindow() == null) {
            return;
        }
        javafx.scene.control.Tooltip tip = (javafx.scene.control.Tooltip) campo.getProperties().get("vp-aviso");
        if (tip == null) {
            tip = new javafx.scene.control.Tooltip();
            tip.setStyle("-fx-background-color: white; -fx-text-fill: #B3261E; -fx-font-size: 12px; "
                    + "-fx-font-weight: bold; -fx-background-radius: 6; -fx-border-color: #B3261E; "
                    + "-fx-border-radius: 6;");
            campo.getProperties().put("vp-aviso", tip);
        }
        tip.setText(mensaje);
        javafx.geometry.Bounds b = campo.localToScreen(campo.getBoundsInLocal());
        if (b == null) {
            return;
        }
        tip.show(campo, b.getMinX(), b.getMaxY() + 2);
        Object viejo = campo.getProperties().get("vp-aviso-reloj");
        if (viejo instanceof javafx.animation.PauseTransition) {
            ((javafx.animation.PauseTransition) viejo).stop();
        }
        javafx.animation.PauseTransition reloj = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(2.5));
        final javafx.scene.control.Tooltip visible = tip;
        reloj.setOnFinished(e -> visible.hide());
        campo.getProperties().put("vp-aviso-reloj", reloj);
        reloj.play();
    }

    /** Solo digitos (telefonos). Lo demas se rechaza y se avisa que no se aceptan letras. */
    public static void soloNumeros(javafx.scene.control.TextInputControl campo, int maximo) {
        campo.setTextFormatter(new javafx.scene.control.TextFormatter<String>(c -> {
            if (!c.isContentChange()) {
                return c;
            }
            String nuevo = c.getControlNewText();
            if (!nuevo.matches("\\d*")) {
                avisoCampo(campo, "No se aceptan letras: aquí solo van números.");
                return null;
            }
            if (nuevo.length() > maximo) {
                avisoCampo(campo, "Máximo " + maximo + " dígitos.");
                return null;
            }
            return c;
        }));
    }

    /** Solo letras (nombres y apellidos). Rechaza numeros y simbolos y avisa. */
    public static void soloLetras(javafx.scene.control.TextInputControl campo) {
        campo.setTextFormatter(new javafx.scene.control.TextFormatter<String>(c -> {
            if (!c.isContentChange()) {
                return c;
            }
            String nuevo = c.getControlNewText();
            if (!nuevo.matches("[\\p{L} .'’-]*")) {
                avisoCampo(campo, nuevo.matches(".*\\d.*") ? "No se aceptan números: aquí solo van letras."
                        : "Solo se aceptan letras.");
                return null;
            }
            return c;
        }));
    }

    /** CURP: letras y numeros, siempre en mayusculas, maximo 18 caracteres. */
    public static void curp(javafx.scene.control.TextInputControl campo) {
        campo.setTextFormatter(new javafx.scene.control.TextFormatter<String>(c -> {
            if (!c.isContentChange()) {
                return c;
            }
            String texto = c.getText().toUpperCase(java.util.Locale.ROOT);
            if (!texto.matches("[A-Z0-9]*")) {
                avisoCampo(campo, "La CURP solo acepta letras y números.");
                return null;
            }
            c.setText(texto);
            if (c.getControlNewText().length() > 18) {
                avisoCampo(campo, "La CURP tiene 18 caracteres.");
                return null;
            }
            return c;
        }));
    }
}
