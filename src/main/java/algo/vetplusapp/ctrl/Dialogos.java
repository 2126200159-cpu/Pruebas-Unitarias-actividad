package algo.vetplusapp.ctrl;

import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.CajaRepo;
import algo.vetplusapp.repo.ClienteRepo;
import algo.vetplusapp.repo.ComprasRepo;
import algo.vetplusapp.repo.ServicioRepo;
import algo.vetplusapp.util.FormDialog;
import algo.vetplusapp.util.ImagenStore;
import algo.vetplusapp.util.Manejo;
import algo.vetplusapp.util.Fmt;
import algo.vetplusapp.util.Ui;
import algo.vetplusapp.util.Valida;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

/** Dialogos de captura para los botones que en el Figma no tienen pantalla (capitulo 8). */
final class Dialogos {

    private Dialogos() {
    }

    /** Ejecuta el guardado dentro de la validacion del dialogo: si falla, el dialogo sigue abierto. */
    private static Supplier<String> guardando(Runnable guardar) {
        return () -> {
            try {
                guardar.run();
                return null;
            } catch (RuntimeException e) {
                return Manejo.procesar(e).mensaje; // regla del negocio o error tecnico, ya registrado en el log
            }
        };
    }

    /** Alta o edicion de propietario. Devuelve el id guardado o null si se cancelo. */
    static Long propietario(Row existente) {
        boolean nuevo = existente == null;
        FormDialog f = new FormDialog(nuevo ? "Registrar propietario" : "Editar propietario",
                nuevo ? "Guardar propietario" : "Guardar cambios");
        f.nota("Los campos con * son obligatorios.");
        TextField nombre = f.texto("Nombre *");
        TextField apellidos = f.texto("Apellidos *");
        TextField telefono = f.texto("Teléfono * (10 dígitos)");
        TextField correo = f.texto("Correo *");
        TextField direccion = f.texto("Dirección *");
        TextField curp = f.texto("CURP * (18 caracteres)");
        TextField altNombre = f.texto("Contacto alterno");
        TextField altTel = f.texto("Tel. contacto alterno");
        Ui.soloLetras(nombre);
        Ui.soloLetras(apellidos);
        Ui.soloNumeros(telefono, 10);
        Ui.curp(curp);
        Ui.soloLetras(altNombre);
        Ui.soloNumeros(altTel, 10);
        if (!nuevo) {
            nombre.setText(existente.str("nombre"));
            apellidos.setText(existente.str("apellidos"));
            telefono.setText(existente.str("telefono"));
            correo.setText(existente.str("correo"));
            direccion.setText(existente.str("direccion"));
            curp.setText(existente.str("curp"));
            altNombre.setText(existente.str("contacto_alterno_nombre"));
            altTel.setText(existente.str("contacto_alterno_telefono"));
        }
        long[] resultado = {0};
        f.validar(guardando(() -> resultado[0] = ClienteRepo.guardarPropietario(nuevo ? null : existente.id(),
                nombre.getText(), apellidos.getText(), telefono.getText(), correo.getText(), direccion.getText(),
                curp.getText(), altNombre.getText(), altTel.getText())));
        return f.mostrar() ? resultado[0] : null;
    }

    /** Alta o edicion de mascota (incluye alimentacion, antecedentes, nota de estetica y foto). */
    static Long mascota(long propietarioId, Row existente) {
        boolean nueva = existente == null;
        FormDialog f = new FormDialog(nueva ? "Registrar mascota" : "Editar mascota",
                nueva ? "Guardar mascota" : "Guardar cambios");
        TextField nombre = f.texto("Nombre *");
        Ui.soloLetras(nombre);
        ComboBox<String> especie = f.combo("Especie", "Perro", "Gato", "Otro");
        TextField raza = f.texto("Raza");
        ComboBox<String> sexo = f.combo("Sexo", "Hembra", "Macho");
        DatePicker nacimiento = f.fecha("Fecha de nacimiento", null);
        TextField peso = f.texto("Peso (kg)");
        CheckBox esterilizada = f.check("Esterilizada");
        TextField microchip = f.texto("Microchip");
        TextField alimentacion = f.texto("Alimentación");
        TextArea antecedentes = f.area("Antecedentes o alergias");
        TextArea notaEstetica = f.area("Nota para estética");
        TextField foto = new TextField();
        foto.setEditable(false);
        foto.getStyleClass().add("campo-texto");
        byte[][] fotoNueva = {null};
        boolean[] quitarFoto = {false};
        Button elegir = new Button("Elegir foto…");
        elegir.getStyleClass().add("boton-secundario");
        Button quitar = new Button("Quitar");
        quitar.getStyleClass().add("boton-secundario");
        elegir.setOnAction(e -> Ui.accion(() -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes (PNG, JPG)", "*.png", "*.jpg",
                    "*.jpeg"));
            File archivo = fc.showOpenDialog(elegir.getScene().getWindow());
            if (archivo != null) {
                fotoNueva[0] = ImagenStore.leerArchivo(archivo.toPath()); // valida formato, lectura y tamaño
                quitarFoto[0] = false;
                foto.setText(archivo.getName());
            }
        }));
        quitar.setOnAction(e -> {
            fotoNueva[0] = null;
            quitarFoto[0] = true;
            foto.clear();
        });
        javafx.scene.layout.HBox filaFoto = new javafx.scene.layout.HBox(6, foto, elegir, quitar);
        f.agregar("Foto", filaFoto);
        especie.getSelectionModel().select("Perro");
        sexo.getSelectionModel().select("Hembra");
        if (!nueva) {
            nombre.setText(existente.str("nombre"));
            especie.getSelectionModel().select(existente.str("especie"));
            raza.setText(existente.str("raza"));
            sexo.getSelectionModel().select(existente.str("sexo"));
            nacimiento.setValue(Fmt.fecha(existente.str("fecha_nacimiento")));
            peso.setText(existente.esNulo("peso") ? "" : Fmt.importe(existente.dbl("peso")));
            esterilizada.setSelected(existente.bool("esterilizada"));
            microchip.setText(existente.str("microchip"));
            alimentacion.setText(existente.str("alimentacion"));
            antecedentes.setText(existente.str("antecedentes"));
            notaEstetica.setText(existente.str("nota_estetica"));
            foto.setText(existente.str("foto_ruta").isEmpty() ? "" : "(foto guardada)");
        }
        long[] resultado = {0};
        f.validar(guardando(() -> {
            double p = Valida.numero(peso.getText(), "El peso", false);
            resultado[0] = ClienteRepo.guardarMascota(nueva ? null : existente.id(), propietarioId,
                    nombre.getText(), especie.getValue(), raza.getText(), sexo.getValue(), nacimiento.getValue(),
                    Double.isNaN(p) ? null : p, esterilizada.isSelected(), microchip.getText(),
                    alimentacion.getText(), antecedentes.getText(), notaEstetica.getText(), fotoNueva[0],
                    quitarFoto[0]);
        }));
        return f.mostrar() ? resultado[0] : null;
    }

    /** Alta de un servicio nuevo (medico o de estetica) con su precio y tiempo estimado. Devuelve el id o null. */
    static Long servicioNuevo() {
        FormDialog f = new FormDialog("Nuevo servicio", "Guardar servicio");
        f.nota("Los campos con * son obligatorios.");
        TextField nombre = f.texto("Nombre *");
        ComboBox<String> tipo = f.combo("Tipo *", "Médico", "Estética");
        TextField descripcion = f.texto("Descripción");
        TextField precio = f.texto("Precio (MXN) *");
        TextField duracion = f.texto("Tiempo estimado (min) *");
        Ui.soloNumeros(duracion, 3);
        long[] resultado = {0};
        f.validar(guardando(() -> {
            if (tipo.getValue() == null) {
                throw new ReglaException("Elige el tipo de servicio.");
            }
            double p = Valida.numero(precio.getText(), "El precio", true);
            Integer d = Valida.entero(duracion.getText(), "El tiempo estimado", true);
            resultado[0] = ServicioRepo.crear(nombre.getText(), descripcion.getText(),
                    "Médico".equals(tipo.getValue()) ? "MEDICO" : "ESTETICA", p, d);
        }));
        return f.mostrar() ? resultado[0] : null;
    }

    /** Alta o edicion de insumo. */
    static boolean insumo(Row existente) {
        boolean nuevo = existente == null;
        FormDialog f = new FormDialog(nuevo ? "Registrar insumo" : "Editar insumo", "Guardar insumo");
        TextField nombre = f.texto("Nombre");
        TextField categoria = f.texto("Categoría");
        TextField existencia = f.texto(nuevo ? "Existencia inicial" : "Existencia");
        TextField minimo = f.texto("Mínimo");
        TextField precio = f.texto("Precio de compra");
        if (!nuevo) {
            nombre.setText(existente.str("nombre"));
            categoria.setText(existente.str("categoria"));
            existencia.setText(Fmt.importe(existente.dbl("existencia")));
            minimo.setText(Fmt.importe(existente.dbl("minimo")));
            precio.setText(Fmt.importe(existente.dbl("precio_compra")));
        }
        f.validar(guardando(() -> ComprasRepo.guardarInsumo(nuevo ? null : existente.id(), nombre.getText(),
                categoria.getText(), Valida.numero(existencia.getText(), "La existencia", true),
                Valida.numero(minimo.getText(), "El mínimo", true),
                Valida.numero(precio.getText(), "El precio", true))));
        return f.mostrar();
    }

    /** Alta o edicion de proveedor. Devuelve el id o null. */
    static Long proveedor(Row existente) {
        boolean nuevo = existente == null;
        FormDialog f = new FormDialog(nuevo ? "Nuevo proveedor" : "Editar proveedor", "Guardar proveedor");
        f.nota("Los campos con * son obligatorios.");
        TextField empresa = f.texto("Empresa *");
        TextField contacto = f.texto("Contacto");
        TextField telefono = f.texto("Teléfono (10 dígitos)");
        TextField correo = f.texto("Correo *");
        Ui.soloLetras(contacto);
        Ui.soloNumeros(telefono, 10);
        if (!nuevo) {
            empresa.setText(existente.str("empresa"));
            contacto.setText(existente.str("contacto"));
            telefono.setText(existente.str("telefono"));
            correo.setText(existente.str("correo"));
        }
        long[] resultado = {0};
        f.validar(guardando(() -> resultado[0] = ComprasRepo.guardarProveedor(nuevo ? null : existente.id(),
                empresa.getText(), contacto.getText(), telefono.getText(), correo.getText())));
        return f.mostrar() ? resultado[0] : null;
    }

    /** Pide insumo, cantidad y precio para un renglon de orden. */
    static ComprasRepo.RenglonOrden renglonOrden() {
        List<Row> insumos = ComprasRepo.insumos();
        if (insumos.isEmpty()) {
            throw new ReglaException("Primero registra al menos un insumo.");
        }
        FormDialog f = new FormDialog("Agregar producto", "Agregar");
        ComboBox<Row> insumo = f.combo("Insumo", insumos);
        insumo.setConverter(new javafx.util.StringConverter<Row>() {
            @Override
            public String toString(Row r) {
                return r == null ? "" : r.str("nombre");
            }

            @Override
            public Row fromString(String s) {
                return null;
            }
        });
        TextField cantidad = f.texto("Cantidad");
        TextField precio = f.texto("Precio unitario");
        insumo.valueProperty().addListener((o, a, n) -> {
            if (n != null) {
                precio.setText(Fmt.importe(n.dbl("precio_compra")));
            }
        });
        ComprasRepo.RenglonOrden[] resultado = {null};
        f.validar(guardando(() -> {
            if (insumo.getValue() == null) {
                throw new ReglaException("Elige el insumo.");
            }
            double c = Valida.numero(cantidad.getText(), "La cantidad", true);
            if (c <= 0) {
                throw new ReglaException("La cantidad debe ser mayor a 0.");
            }
            double p = Valida.numero(precio.getText(), "El precio", true);
            resultado[0] = new ComprasRepo.RenglonOrden(insumo.getValue().id(), insumo.getValue().str("nombre"), c,
                    p);
        }));
        return f.mostrar() ? resultado[0] : null;
    }

    /** Historial de ordenes de compra, con boton para registrar la recepcion de una orden Emitida. */
    static void historialCompras() {
        FormDialog f = new FormDialog("Historial de compras", "Registrar recepción");
        TableView<Row> tabla = f.tabla(260);
        columna(tabla, "Folio", r -> r.str("folio"), 90);
        columna(tabla, "Proveedor", r -> r.str("proveedor"), 170);
        columna(tabla, "Estado", r -> estadoOrden(r.str("estado")), 90);
        columna(tabla, "Total", r -> Fmt.dinero(r.dbl("total")), 100);
        tabla.getItems().setAll(ComprasRepo.ordenes());
        f.nota("Selecciona una orden Emitida y pulsa «Registrar recepción» para sumar sus cantidades a las "
                + "existencias.");
        f.validar(guardando(() -> {
            Row r = tabla.getSelectionModel().getSelectedItem();
            if (r == null) {
                throw new ReglaException("Selecciona una orden.");
            }
            ComprasRepo.recibir(r.id());
        }));
        f.mostrar();
    }

    static String estadoOrden(String e) {
        switch (e) {
            case "EMITIDA":
                return "Emitida";
            case "RECIBIDA":
                return "Recibida";
            default:
                return "Borrador";
        }
    }

    private static void columna(TableView<Row> t, String titulo, java.util.function.Function<Row, String> f,
            double ancho) {
        TableColumn<Row, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new SimpleStringProperty(f.apply(d.getValue())));
        c.setPrefWidth(ancho);
        t.getColumns().add(c);
    }

    /** Cuenta armada a mano: propietario y renglones (mascota, servicio, cantidad). Devuelve el id o null. */
    static Long cuentaManual() {
        FormDialog f = new FormDialog("Nueva cuenta", "Crear cuenta");
        ComboBox<Row> propietario = f.combo("Propietario", ClienteRepo.propietariosActivos());
        propietario.setConverter(convertidor("nombre_completo"));
        ComboBox<Row> mascota = f.combo("Mascota", new ArrayList<Row>());
        mascota.setConverter(convertidor("nombre"));
        ComboBox<Row> servicio = f.combo("Servicio", ServicioRepo.activos());
        servicio.setConverter(convertidor("nombre"));
        TextField cantidad = f.texto("Cantidad");
        cantidad.setText("1");
        propietario.valueProperty().addListener((o, a, n) -> {
            mascota.getItems().setAll(n == null ? new ArrayList<Row>() : ClienteRepo.mascotasActivasDe(n.id()));
        });
        List<CajaRepo.Renglon> renglones = new ArrayList<>();
        javafx.scene.control.Label resumen = new javafx.scene.control.Label("Sin renglones agregados.");
        Button agregar = new Button("Agregar renglón");
        agregar.setOnAction(e -> {
            try {
                if (mascota.getValue() == null || servicio.getValue() == null) {
                    throw new ReglaException("Elige la mascota y el servicio.");
                }
                Integer c = Valida.entero(cantidad.getText(), "La cantidad", true);
                if (c == null || c <= 0) {
                    throw new ReglaException("La cantidad debe ser mayor a 0.");
                }
                renglones.add(new CajaRepo.Renglon(mascota.getValue().id(), servicio.getValue().id(), c));
                resumen.setText(renglones.size() + " renglón(es) agregado(s).");
            } catch (ReglaException ex) {
                resumen.setText(ex.getMessage());
            }
        });
        f.agregar("", agregar);
        f.agregar("", resumen);
        long[] resultado = {0};
        f.validar(guardando(() -> {
            if (propietario.getValue() == null) {
                throw new ReglaException("Elige el propietario.");
            }
            resultado[0] = CajaRepo.crearCuentaManual(propietario.getValue().id(), renglones);
        }));
        return f.mostrar() ? resultado[0] : null;
    }

    static javafx.util.StringConverter<Row> convertidor(String columna) {
        return new javafx.util.StringConverter<Row>() {
            @Override
            public String toString(Row r) {
                return r == null ? "" : r.str(columna);
            }

            @Override
            public Row fromString(String s) {
                return null;
            }
        };
    }

    /** Selector de fecha en una ventana pequena. Devuelve null si se cancelo. */
    static LocalDate fecha(String titulo, LocalDate inicial) {
        FormDialog f = new FormDialog(titulo, "Aceptar");
        DatePicker d = f.fecha("Fecha", inicial);
        f.validar(() -> d.getValue() == null ? "Elige una fecha." : null);
        return f.mostrar() ? d.getValue() : null;
    }
}
