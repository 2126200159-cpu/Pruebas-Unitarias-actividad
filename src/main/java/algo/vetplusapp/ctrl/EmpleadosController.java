package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.UsuarioRepo;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.ImagenStore;
import algo.vetplusapp.util.Ui;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;

/** Empleados (4.10): directorio, filtros por rol y formulario de alta/edicion. */
public class EmpleadosController {

    @FXML
    private ToggleGroup grupoFiltroEmpleados;

    @FXML
    private Button btnAltaEmpleado;

    @FXML
    private TextField txtBuscarEmpleado;

    @FXML
    private ToggleButton tglTodos;

    @FXML
    private ToggleButton tglMedicos;

    @FXML
    private ToggleButton tglEstilistas;

    @FXML
    private ToggleButton tglAdministracion;

    @FXML
    private TableView<Row> tblEmpleados;

    @FXML
    private TableColumn<Row, String> colEmpleado;

    @FXML
    private TableColumn<Row, String> colRolEspecialidad;

    @FXML
    private TableColumn<Row, String> colTurno;

    @FXML
    private TableColumn<Row, String> colEstadoEmpleado;

    @FXML
    private Label lblResumenEmpleados;

    @FXML
    private Label lblEmpleadoEditando;

    @FXML
    private Label lblInicialesFoto;

    @FXML
    private ImageView imgFotoEmpleado;

    @FXML
    private Button btnElegirFoto;

    @FXML
    private Hyperlink lnkQuitarFoto;

    @FXML
    private TextField txtNombreEmpleado;

    @FXML
    private TextField txtApellidosEmpleado;

    @FXML
    private ComboBox<String> cmbRolEmpleado;

    @FXML
    private TextField txtEspecialidad;

    @FXML
    private ComboBox<String> cmbTurno;

    @FXML
    private ComboBox<String> cmbEstadoEmpleado;

    @FXML
    private TextField txtCorreoEmpleado;

    @FXML
    private TextField txtTelefonoEmpleado;

    @FXML
    private TextField txtCurpEmpleado;

    @FXML
    private TextField txtDireccionEmpleado;

    @FXML
    private Button btnGuardarEmpleado;

    @FXML
    private Button btnCancelarEmpleado;

    @FXML
    private Button btnEliminarEmpleado;

    @FXML
    private Hyperlink lnkVerContrasenaTemporal;

    @FXML
    private Hyperlink lnkRestablecerContrasena;

    private Long editandoId = null;
    private boolean llenando = false;
    private String iniEditando;
    private byte[] fotoPendiente = null;   // imagen elegida, se guarda con "Guardar cambios"
    private boolean fotoCambiada = false;
    private boolean tieneFoto = false;

    @FXML
    private void initialize() {
        iniEditando = lblEmpleadoEditando.getText();

        cmbRolEmpleado.getItems().setAll("Administración", "Médico veterinario", "Estilista canino");
        cmbTurno.getItems().setAll(Config.TURNOS);
        cmbEstadoEmpleado.getItems().setAll("Activo", "Inactivo");

        // filtros de captura: nombres solo letras, telefono solo numeros, CURP en mayusculas
        Ui.soloLetras(txtNombreEmpleado);
        Ui.soloLetras(txtApellidosEmpleado);
        Ui.soloNumeros(txtTelefonoEmpleado, 10);
        Ui.curp(txtCurpEmpleado);

        Ui.col(colEmpleado, "nombre_completo");
        Ui.col(colRolEspecialidad, r -> Config.rolTexto(r.str("rol"))
                + (r.str("especialidad").isEmpty() ? "" : " · " + r.str("especialidad")));
        Ui.col(colTurno, "turno");
        Ui.col(colEstadoEmpleado, "estado");

        Ui.grupoSiempreUno(grupoFiltroEmpleados, t -> refrescar());
        txtBuscarEmpleado.textProperty().addListener((o, a, n) -> refrescar());
        tblEmpleados.getSelectionModel().selectedItemProperty().addListener((o, a, fila) -> {
            if (!llenando && fila != null) {
                llenarFormulario(fila);
            }
        });

        btnAltaEmpleado.setOnAction(e -> nuevo());
        btnGuardarEmpleado.setOnAction(e -> Ui.accion(this::guardar));
        btnCancelarEmpleado.setOnAction(e -> {
            Row sel = tblEmpleados.getSelectionModel().getSelectedItem();
            if (editandoId != null && sel != null) {
                llenarFormulario(UsuarioRepo.porId(editandoId));
            } else {
                vaciarFormulario();
            }
        });
        lnkRestablecerContrasena.setOnAction(e -> Ui.accion(this::restablecer));
        btnEliminarEmpleado.setOnAction(e -> Ui.accion(this::eliminar));
        lnkVerContrasenaTemporal.setOnAction(e -> Ui.accion(this::verContrasenaTemporal));

        // foto de perfil: recorte circular y las iniciales mientras no haya imagen
        imgFotoEmpleado.setClip(new javafx.scene.shape.Circle(36, 36, 36));
        btnElegirFoto.setOnAction(e -> Ui.accion(this::elegirFoto));
        lnkQuitarFoto.setOnAction(e -> {
            fotoPendiente = null;
            fotoCambiada = true;
            pintarFoto(null);
        });
        javafx.beans.value.ChangeListener<String> alEscribir = (o, a, n) -> {
            if (!tieneFoto) {
                lblInicialesFoto.setText(iniciales());
            }
        };
        txtNombreEmpleado.textProperty().addListener(alEscribir);
        txtApellidosEmpleado.textProperty().addListener(alEscribir);

        vaciarFormulario();
        refrescar();
    }

    private void refrescar() {
        List<Row> todos = UsuarioRepo.todos();
        int med = 0, est = 0, adm = 0, activos = 0;
        for (Row r : todos) {
            switch (r.str("rol")) {
                case "MEDICO":
                    med++;
                    break;
                case "ESTILISTA":
                    est++;
                    break;
                default:
                    adm++;
                    break;
            }
            if ("Activo".equals(r.str("estado"))) {
                activos++;
            }
        }
        tglTodos.setText("Todos · " + todos.size());
        tglMedicos.setText("Médicos · " + med);
        tglEstilistas.setText("Estilistas · " + est);
        tglAdministracion.setText("Administración · " + adm);
        lblResumenEmpleados.setText(todos.size() + " empleados registrados · " + activos + " activos");

        String rolFiltro = tglMedicos.isSelected() ? "MEDICO" : tglEstilistas.isSelected() ? "ESTILISTA"
                : tglAdministracion.isSelected() ? "ADMINISTRACION" : "";
        String q = txtBuscarEmpleado.getText() == null ? "" : txtBuscarEmpleado.getText().trim()
                .toLowerCase(Locale.ROOT);
        List<Row> visibles = new ArrayList<>();
        for (Row r : todos) {
            if (!rolFiltro.isEmpty() && !rolFiltro.equals(r.str("rol"))) {
                continue;
            }
            String texto = (r.str("nombre_completo") + " " + r.str("especialidad") + " "
                    + Config.rolTexto(r.str("rol"))).toLowerCase(Locale.ROOT);
            if (q.isEmpty() || texto.contains(q)) {
                visibles.add(r);
            }
        }
        llenando = true;
        tblEmpleados.getItems().setAll(visibles);
        if (editandoId != null) {
            for (Row r : visibles) {
                if (r.id() == editandoId) {
                    tblEmpleados.getSelectionModel().select(r);
                }
            }
        }
        llenando = false;
    }

    private void llenarFormulario(Row r) {
        editandoId = r.id();
        lblEmpleadoEditando.setText(r.str("nombre_completo"));
        txtNombreEmpleado.setText(r.str("nombre"));
        txtApellidosEmpleado.setText(r.str("apellidos"));
        cmbRolEmpleado.setValue(Config.rolTexto(r.str("rol")));
        txtEspecialidad.setText(r.str("especialidad"));
        cmbTurno.setValue(r.str("turno").isEmpty() ? null : r.str("turno"));
        cmbEstadoEmpleado.setValue(r.str("estado"));
        txtCorreoEmpleado.setText(r.str("correo_usuario"));
        txtTelefonoEmpleado.setText(r.str("telefono"));
        txtCurpEmpleado.setText(r.str("curp"));
        txtDireccionEmpleado.setText(r.str("direccion"));
        btnEliminarEmpleado.setDisable(false);
        fotoPendiente = null;
        fotoCambiada = false;
        pintarFoto(UsuarioRepo.foto(r.id()));
    }

    private void vaciarFormulario() {
        editandoId = null;
        lblEmpleadoEditando.setText(iniEditando);
        txtNombreEmpleado.clear();
        txtApellidosEmpleado.clear();
        cmbRolEmpleado.setValue(null);
        txtEspecialidad.clear();
        cmbTurno.setValue(null);
        cmbEstadoEmpleado.setValue("Activo");
        txtCorreoEmpleado.clear();
        txtTelefonoEmpleado.clear();
        txtCurpEmpleado.clear();
        txtDireccionEmpleado.clear();
        llenando = true;
        tblEmpleados.getSelectionModel().clearSelection();
        llenando = false;
        btnEliminarEmpleado.setDisable(true); // no hay empleado seleccionado
        fotoPendiente = null;
        fotoCambiada = false;
        pintarFoto(null);
    }

    private void nuevo() {
        vaciarFormulario();
        lblEmpleadoEditando.setText("Nuevo empleado");
        txtNombreEmpleado.requestFocus();
    }

    private void guardar() {
        String rol = cmbRolEmpleado.getValue() == null ? "" : Config.rolCodigo(cmbRolEmpleado.getValue());
        String temp = UsuarioRepo.guardar(editandoId, txtNombreEmpleado.getText(), txtApellidosEmpleado.getText(),
                rol, txtEspecialidad.getText(), cmbTurno.getValue() == null ? "" : cmbTurno.getValue(),
                cmbEstadoEmpleado.getValue() == null ? "Activo" : cmbEstadoEmpleado.getValue(),
                txtCorreoEmpleado.getText(), txtTelefonoEmpleado.getText(), txtCurpEmpleado.getText(),
                txtDireccionEmpleado.getText());
        Long idGuardado = editandoId;
        if (idGuardado == null) {
            Row creado = UsuarioRepo.todos().stream()
                    .filter(r -> r.str("correo_usuario").equalsIgnoreCase(txtCorreoEmpleado.getText().trim()))
                    .findFirst().orElse(null);
            idGuardado = creado == null ? null : creado.id();
        }
        editandoId = idGuardado;
        if (fotoCambiada && idGuardado != null) {
            UsuarioRepo.guardarFoto(idGuardado, fotoPendiente);
            fotoCambiada = false;
        }
        refrescar();
        if (temp != null) {
            Ui.credencialTemporal("Empleado registrado", txtCorreoEmpleado.getText().trim(), temp,
                    "Entrégala a la persona: se muestra una sola vez y deberá cambiarla al iniciar sesión.");
        } else {
            Ui.info("Cambios guardados.");
        }
        if (editandoId != null) {
            llenarFormulario(UsuarioRepo.porId(editandoId));
        }
    }

    private void restablecer() {
        if (editandoId == null) {
            Ui.aviso("Selecciona un empleado para restablecer su contraseña.");
            return;
        }
        if (Ui.confirmar("¿Generar una contraseña temporal para este empleado?")) {
            String temp = UsuarioRepo.restablecer(editandoId);
            Ui.credencialTemporal("Contraseña restablecida", txtCorreoEmpleado.getText().trim(), temp,
                    "Se muestra una sola vez. Deberá cambiarla al iniciar sesión.");
        }
    }

    /** Elimina al empleado seleccionado; si tiene historial ofrece pasarlo a Inactivo. */
    private void eliminar() {
        if (editandoId == null) {
            Ui.aviso("Selecciona un empleado de la tabla para eliminarlo.");
            return;
        }
        long id = editandoId;
        String nombre = UsuarioRepo.porId(id).str("nombre_completo");
        UsuarioRepo.validarEliminable(id);
        if (UsuarioRepo.tieneHistorial(id)) {
            if (Ui.confirmar(nombre + " tiene citas u órdenes registradas, así que no se puede eliminar sin perder "
                    + "el historial.\n\n¿Quieres pasarlo a Inactivo? Ya no podrá iniciar sesión ni se le "
                    + "asignarán citas nuevas.")) {
                UsuarioRepo.cambiarEstado(id, "Inactivo");
                refrescar();
                llenarFormulario(UsuarioRepo.porId(id));
                Ui.info(nombre + " quedó como Inactivo.");
            }
            return;
        }
        if (!Ui.confirmar("¿Eliminar definitivamente a " + nombre + "? Esta acción no se puede deshacer.")) {
            return;
        }
        UsuarioRepo.eliminar(id);
        vaciarFormulario();
        refrescar();
        Ui.info("Empleado eliminado.");
    }

    /**
     * Muestra la contrasena temporal solo mientras el empleado no la haya cambiado. Cuando la cambia, la clave
     * real nunca se guarda en claro: para ayudarle se usa "Restablecer contraseña".
     */
    private void verContrasenaTemporal() {
        if (editandoId == null) {
            Ui.aviso("Selecciona un empleado de la tabla.");
            return;
        }
        Row u = UsuarioRepo.porId(editandoId);
        String temp = UsuarioRepo.contrasenaTemporal(editandoId);
        if (temp == null) {
            Ui.info(u.str("nombre_completo") + " ya cambió su contraseña y, por seguridad, no se puede ver.\n\n"
                    + "Si la olvidó, usa «Restablecer contraseña» para generarle una temporal nueva.");
            return;
        }
        Ui.credencialTemporal("Contraseña temporal pendiente", u.str("correo_usuario"), temp,
                "El empleado aún no la ha cambiado. En cuanto la cambie, dejará de poder verse.");
    }

    // ------------------------------------------------------------------ foto de perfil

    private String iniciales() {
        String t = (txtNombreEmpleado.getText() + " " + txtApellidosEmpleado.getText()).trim();
        return t.isEmpty() ? "+" : Fmt.iniciales(t);
    }

    /** Muestra la imagen (recortada al centro en cuadro) o, si no hay, las iniciales. */
    private void pintarFoto(byte[] datos) {
        Image img = datos == null ? null : new Image(new ByteArrayInputStream(datos));
        tieneFoto = img != null && !img.isError();
        if (tieneFoto) {
            double lado = Math.min(img.getWidth(), img.getHeight());
            imgFotoEmpleado.setImage(img);
            imgFotoEmpleado.setViewport(new Rectangle2D((img.getWidth() - lado) / 2, (img.getHeight() - lado) / 2,
                    lado, lado));
        } else {
            imgFotoEmpleado.setImage(null);
            lblInicialesFoto.setText(iniciales());
        }
        Ui.mostrar(imgFotoEmpleado, tieneFoto);
        Ui.mostrar(lblInicialesFoto, !tieneFoto);
        lnkQuitarFoto.setDisable(!tieneFoto);
    }

    private void elegirFoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Elegir imagen del empleado");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes (PNG, JPG)", "*.png", "*.jpg",
                "*.jpeg"));
        File archivo = fc.showOpenDialog(btnElegirFoto.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        byte[] datos = ImagenStore.leerArchivo(archivo.toPath()); // valida formato, tamaño y lectura
        fotoPendiente = datos;
        fotoCambiada = true;
        pintarFoto(datos);
        if (editandoId == null) {
            Ui.info("La foto se guardará junto con el empleado cuando pulses «Guardar cambios».");
        }
    }
}
