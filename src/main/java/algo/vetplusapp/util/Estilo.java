package algo.vetplusapp.util;

import java.net.URL;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Da a los dialogos el mismo diseno que el resto de la app: hoja vetplus.css, fondo crema, boton naranja, logo. */
public final class Estilo {

    private static final String PRIMARIO = "vp-primario";

    private Estilo() {
    }

    /** Aplica el estilo a un dialogo (Alert o Dialog). Se puede llamar antes o despues de definir sus botones. */
    public static void aplicar(Dialog<?> d) {
        DialogPane p = d.getDialogPane();
        URL css = Estilo.class.getResource("/css/vetplus.css");
        if (css != null && !p.getStylesheets().contains(css.toExternalForm())) {
            p.getStylesheets().add(css.toExternalForm());
        }
        if (!p.getStyleClass().contains("dialogo-vet")) {
            p.getStyleClass().add("dialogo-vet");
        }
        botones(d);
        d.setOnShowing(e -> {
            Window w = p.getScene() == null ? null : p.getScene().getWindow();
            URL logo = Estilo.class.getResource("/images/vetplus-logo.png");
            if (w instanceof Stage && logo != null) {
                ((Stage) w).getIcons().setAll(new Image(logo.toExternalForm()));
            }
            botones(d); // por si los botones se cambiaron despues de aplicar()
        });
    }

    /** Marca cual boton es el principal (naranja). Los demas quedan blancos. */
    public static void primario(Dialog<?> d, ButtonType boton) {
        d.getDialogPane().getProperties().put(PRIMARIO, boton);
        botones(d);
    }

    private static void botones(Dialog<?> d) {
        DialogPane p = d.getDialogPane();
        Object elegido = p.getProperties().get(PRIMARIO);
        boolean unico = p.getButtonTypes().size() == 1;
        for (ButtonType bt : p.getButtonTypes()) {
            Node n = p.lookupButton(bt);
            if (n == null) {
                continue;
            }
            boolean principal;
            if (elegido != null) {
                principal = bt == elegido;
            } else {
                principal = unico || bt == ButtonType.OK || bt == ButtonType.YES
                        || bt.getButtonData() == ButtonBar.ButtonData.OK_DONE;
            }
            n.getStyleClass().removeAll("boton-naranja", "boton-secundario");
            n.getStyleClass().add(principal ? "boton-naranja" : "boton-secundario");
        }
    }
}
