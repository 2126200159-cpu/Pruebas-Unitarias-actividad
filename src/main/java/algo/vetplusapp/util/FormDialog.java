package algo.vetplusapp.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Dialogo de formulario sencillo armado en codigo (los huecos del capitulo 8). */
public class FormDialog {

    private final Dialog<ButtonType> dialogo = new Dialog<>();
    private final GridPane rejilla = new GridPane();
    private final Label lblError = new Label();
    private final VBox contenido = new VBox(8);
    private int fila = 0;
    private Supplier<String> validador = () -> null;

    public FormDialog(String titulo, String textoGuardar) {
        dialogo.setTitle(titulo);
        rejilla.setHgap(10);
        rejilla.setVgap(8);
        rejilla.setPadding(new Insets(8));
        lblError.getStyleClass().add("texto-12");
        lblError.setStyle("-fx-text-fill: #B3261E;");
        lblError.setWrapText(true);
        Ui.mostrar(lblError, false);
        contenido.getChildren().addAll(rejilla, lblError);
        dialogo.getDialogPane().setContent(contenido);
        ButtonType guardar = new ButtonType(textoGuardar, javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
        Estilo.aplicar(dialogo);
        Node boton = dialogo.getDialogPane().lookupButton(guardar);
        boton.addEventFilter(ActionEvent.ACTION, ev -> {
            String problema = validador.get();
            if (problema != null) {
                lblError.setText(problema);
                Ui.mostrar(lblError, true);
                ev.consume();
                dialogo.getDialogPane().getScene().getWindow().sizeToScene();
            }
        });
    }

    public void agregar(String etiqueta, Node campo) {
        Label et = new Label(etiqueta);
        et.getStyleClass().add("campo-etiqueta");
        rejilla.add(et, 0, fila);
        rejilla.add(campo, 1, fila);
        fila++;
    }

    public TextField texto(String etiqueta) {
        TextField t = new TextField();
        t.getStyleClass().add("campo-texto");
        t.setPrefColumnCount(22);
        agregar(etiqueta, t);
        return t;
    }

    public PasswordField contrasena(String etiqueta) {
        PasswordField t = new PasswordField();
        t.getStyleClass().add("campo-texto");
        t.setPrefColumnCount(22);
        agregar(etiqueta, t);
        return t;
    }

    public TextArea area(String etiqueta) {
        TextArea t = new TextArea();
        t.getStyleClass().add("campo-area");
        t.setPrefRowCount(3);
        t.setPrefColumnCount(22);
        t.setWrapText(true);
        agregar(etiqueta, t);
        return t;
    }

    @SafeVarargs
    public final <T> ComboBox<T> combo(String etiqueta, T... opciones) {
        ComboBox<T> c = new ComboBox<>();
        c.getStyleClass().add("campo-combo");
        c.getItems().addAll(opciones);
        c.setPrefWidth(250);
        agregar(etiqueta, c);
        return c;
    }

    public <T> ComboBox<T> combo(String etiqueta, List<T> opciones) {
        ComboBox<T> c = new ComboBox<>();
        c.getStyleClass().add("campo-combo");
        c.getItems().addAll(opciones);
        c.setPrefWidth(250);
        agregar(etiqueta, c);
        return c;
    }

    public DatePicker fecha(String etiqueta, LocalDate inicial) {
        DatePicker d = new DatePicker(inicial);
        d.getStyleClass().add("campo-combo");
        d.setPrefWidth(250);
        agregar(etiqueta, d);
        return d;
    }

    public CheckBox check(String etiqueta) {
        CheckBox c = new CheckBox(etiqueta);
        rejilla.add(c, 1, fila++);
        return c;
    }

    public <T> TableView<T> tabla(double alto) {
        TableView<T> t = new TableView<>();
        t.getStyleClass().add("tabla-vet");
        t.setPrefHeight(alto);
        t.setPrefWidth(520);
        contenido.getChildren().add(0, t);
        return t;
    }

    public void nota(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("texto-13");
        l.setWrapText(true);
        l.setMaxWidth(380);
        contenido.getChildren().add(0, l);
    }

    public void validar(Supplier<String> v) {
        this.validador = v;
    }

    /** Muestra el dialogo; true si la persona pulso el boton de guardar y todo valido. */
    public boolean mostrar() {
        return dialogo.showAndWait().map(b -> b.getButtonData() == javafx.scene.control.ButtonBar.ButtonData.OK_DONE)
                .orElse(false);
    }

    /** Dialogo solo informativo (sin boton de guardar). */
    public void mostrarCerrar() {
        dialogo.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
        dialogo.showAndWait();
    }

    public static List<String> lista(String... v) {
        List<String> l = new ArrayList<>();
        for (String s : v) {
            l.add(s);
        }
        return l;
    }
}
