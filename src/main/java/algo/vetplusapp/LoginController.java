package algo.vetplusapp;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.UsuarioRepo;
import algo.vetplusapp.util.FormDialog;
import algo.vetplusapp.util.Ui;
import java.util.prefs.Preferences;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class LoginController {

    private static final String PREF_USUARIO = "usuarioRecordado";

    @FXML
    private Button btnIniciarSesion;
    @FXML
    private Button btnMostrarContrasena;
    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtContrasena;
    @FXML
    private CheckBox chkRecordarme;
    @FXML
    private Hyperlink lnkOlvideContrasena;

    private HBox contenedor;
    private TextField txtContrasenaVisible;
    private boolean contrasenaVisible = false;

    @FXML
    private void initialize() {
        btnIniciarSesion.setOnAction(e -> Ui.accion(this::entrar));
        lnkOlvideContrasena.setOnAction(e -> Ui.info("Pide a administración que restablezca tu contraseña."));

        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        String recordado = prefs.get(PREF_USUARIO, "");
        if (!recordado.isEmpty()) {
            txtUsuario.setText(recordado);
            chkRecordarme.setSelected(true);
        }

        contenedor = (HBox) txtContrasena.getParent();

        txtContrasenaVisible = new TextField();
        txtContrasenaVisible.getStyleClass().add("campo-contrasena-interno");
        txtContrasenaVisible.setPromptText(txtContrasena.getPromptText());
        txtContrasenaVisible.textProperty().bindBidirectional(txtContrasena.textProperty());
        HBox.setHgrow(txtContrasenaVisible, Priority.ALWAYS);

        btnMostrarContrasena.setOnAction(e -> alternarContrasena());
    }

    private void entrar() {
        Row usuario = UsuarioRepo.autenticar(txtUsuario.getText(), txtContrasena.getText());

        if (usuario.bool("debe_cambiar_contrasena") && !pedirNuevaContrasena(usuario)) {
            return;
        }

        // solo se recuerda el usuario, nunca la contrasena
        Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
        if (chkRecordarme.isSelected()) {
            prefs.put(PREF_USUARIO, txtUsuario.getText().trim());
        } else {
            prefs.remove(PREF_USUARIO);
        }

        UsuarioRepo.iniciarSesion(usuario);
        algo.vetplusapp.util.AppLog.info("LoginController", "iniciarSesion", "Inicio de sesión de "
                + usuario.str("correo_usuario") + " (" + usuario.str("rol") + ")");
        switch (usuario.str("rol")) {
            case "MEDICO":
                App.mostrarMedico();
                break;
            case "ESTILISTA":
                App.mostrarEstilista();
                break;
            default:
                App.mostrarAdmin();
                break;
        }
    }

    /** Cambio obligatorio de contrasena en el primer inicio (o tras un restablecimiento). */
    private boolean pedirNuevaContrasena(Row usuario) {
        FormDialog f = new FormDialog("Cambiar contraseña", "Guardar contraseña");
        f.nota("Tu contraseña es temporal. Elige una nueva para continuar.");
        PasswordField nueva = f.contrasena("Nueva contraseña");
        PasswordField repetir = f.contrasena("Repetir contraseña");
        f.validar(() -> {
            if (nueva.getText().length() < 6) {
                return "La contraseña debe tener al menos 6 caracteres.";
            }
            if (!nueva.getText().equals(repetir.getText())) {
                return "Las contraseñas no coinciden.";
            }
            if (nueva.getText().equals(txtContrasena.getText())) {
                return "La nueva contraseña debe ser distinta a la temporal.";
            }
            return null;
        });
        if (!f.mostrar()) {
            return false;
        }
        try {
            UsuarioRepo.cambiarContrasena(usuario.id(), nueva.getText());
            return true;
        } catch (Db.ReglaException e) {
            Ui.aviso(e.getMessage());
            return false;
        }
    }

    private void alternarContrasena() {
        TextField actual = contrasenaVisible ? txtContrasenaVisible : txtContrasena;
        TextField nuevo = contrasenaVisible ? txtContrasena : txtContrasenaVisible;

        int posicion = contenedor.getChildren().indexOf(actual);
        contenedor.getChildren().set(posicion, nuevo);
        contrasenaVisible = !contrasenaVisible;

        nuevo.requestFocus();
        nuevo.positionCaret(nuevo.getText().length());
    }
}
