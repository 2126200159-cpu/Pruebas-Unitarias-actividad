package algo.vetplusapp;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class App extends Application {

    private static Stage ventana;

    @Override
    public void start(Stage stage) {
        ventana = stage;
        stage.setTitle("VetPlus");
        // cualquier error no controlado se registra en el log y se muestra un mensaje claro; la app sigue abierta
        Thread.setDefaultUncaughtExceptionHandler((hilo, e) -> {
            algo.vetplusapp.util.Manejo.Resultado r = algo.vetplusapp.util.Manejo.procesar(e);
            javafx.application.Platform.runLater(() -> algo.vetplusapp.util.Ui.error(r.mensaje));
        });
        algo.vetplusapp.util.AppLog.info("App", "start", "VetPlus iniciado");
        try {
            algo.vetplusapp.db.Db.iniciar();
        } catch (RuntimeException e) {
            algo.vetplusapp.util.AppLog.error(e);
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "No se pudo conectar a la base de datos.\n\nAbre el Panel de XAMPP y enciende MySQL (Start), "
                    + "luego vuelve a abrir VetPlus.\n\nDetalle: " + algo.vetplusapp.util.Sensible.limpiar(causaRaiz(e)),
                    javafx.scene.control.ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
            javafx.application.Platform.exit();
            return;
        }
        mostrarLogin();
        stage.show();
    }

    private static String causaRaiz(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage();
    }

    public static void mostrarLogin() {
        cambiarEscena("/view/login.fxml", new LoginController(), false);
    }

    public static void mostrarAdmin() {
        cambiarEscena("/view/admin-shell.fxml", new AdminShellController(), true);
    }

    public static void mostrarMedico() {
        cambiarEscena("/view/medico-shell.fxml", new MedicoShellController(), true);
    }

    public static void mostrarEstilista() {
        cambiarEscena("/view/estilista-shell.fxml", new EstilistaShellController(), true);
    }

    private static void cambiarEscena(String ruta, Object controlador, boolean maximizar) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(ruta));
            loader.setController(controlador);
            Parent raiz = loader.load();

            ventana.setMaximized(false);
            ventana.setScene(new Scene(raiz));

            if (maximizar) {
                ventana.setMaximized(true);
            } else {
                ventana.sizeToScene();
                Rectangle2D area = Screen.getPrimary().getVisualBounds();
                if (ventana.getWidth() > area.getWidth()) {
                    ventana.setWidth(area.getWidth());
                }
                if (ventana.getHeight() > area.getHeight()) {
                    ventana.setHeight(area.getHeight());
                }
                ventana.centerOnScreen();
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar " + ruta, e);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}