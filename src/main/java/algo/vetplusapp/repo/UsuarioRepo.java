package algo.vetplusapp.repo;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Hash;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.util.ImagenStore;
import algo.vetplusapp.util.Sesion;
import algo.vetplusapp.util.Valida;
import java.util.List;

public final class UsuarioRepo {

    private UsuarioRepo() {
    }

    /** Valida usuario y contrasena (hash). Lanza ReglaException con el mensaje para el Alert. */
    public static Row autenticar(String usuario, String contrasena) {
        Row u = Db.uno("SELECT * FROM usuario WHERE correo_usuario = ?", Valida.t(usuario));
        if (u == null && !Valida.t(usuario).isEmpty() && !Valida.t(usuario).contains("@")) {
            // permite entrar escribiendo solo la parte antes del @ (por ejemplo: admin)
            u = Db.uno("SELECT * FROM usuario WHERE SUBSTRING_INDEX(correo_usuario, '@', 1) = ? LIMIT 1",
                    Valida.t(usuario));
        }
        if (u == null || !Hash.verificar(contrasena == null ? "" : contrasena, u.str("contrasena_hash"))) {
            throw new ReglaException("Usuario o contraseña incorrectos.");
        }
        if ("Inactivo".equals(u.str("estado"))) {
            throw new ReglaException("Esta cuenta está inactiva. Pide a administración que la reactive.");
        }
        return u;
    }

    public static void iniciarSesion(Row u) {
        Sesion.usuarioId = u.id();
        Sesion.nombreCompleto = (u.str("nombre") + " " + u.str("apellidos")).trim();
        Sesion.rol = u.str("rol");
        Sesion.turno = u.str("turno");
        Sesion.especialidad = u.str("especialidad");
    }

    public static void cambiarContrasena(long id, String nueva) {
        if (nueva == null || nueva.length() < 6) {
            throw new ReglaException("La contraseña debe tener al menos 6 caracteres.");
        }
        Db.ejecutar("UPDATE usuario SET contrasena_hash = ?, debe_cambiar_contrasena = 0, contrasena_temporal = NULL WHERE id = ?",
                Hash.crear(nueva), id);
    }

    /** Genera una contrasena temporal, la guarda con hash y la devuelve (se muestra una sola vez). */
    public static String restablecer(long id) {
        String temp = Hash.temporal();
        Db.ejecutar("UPDATE usuario SET contrasena_hash = ?, debe_cambiar_contrasena = 1, contrasena_temporal = ? "
                + "WHERE id = ?", Hash.crear(temp), temp, id);
        return temp;
    }

    public static List<Row> todos() {
        return Db.consultar("SELECT id, nombre, apellidos, TRIM(CONCAT(nombre, ' ', apellidos)) AS nombre_completo, "
                + "correo_usuario, rol, especialidad, turno, estado, telefono, curp, direccion "
                + "FROM usuario ORDER BY nombre, apellidos");
    }

    public static Row porId(long id) {
        return Db.uno("SELECT *, TRIM(CONCAT(nombre, ' ', apellidos)) AS nombre_completo FROM usuario WHERE id = ?", id);
    }

    public static List<Row> profesionalesActivos(String rol) {
        return Db.consultar("SELECT id, TRIM(CONCAT(nombre, ' ', apellidos)) AS nombre_completo, especialidad, turno "
                + "FROM usuario WHERE rol = ? AND estado = 'Activo' ORDER BY nombre", rol);
    }

    /**
     * Inserta (id nulo) o actualiza. Devuelve la contrasena temporal si fue alta; si no, null.
     */
    public static String guardar(Long id, String nombre, String apellidos, String rol, String especialidad,
            String turno, String estado, String correo, String telefono, String curp, String direccion) {
        Valida.nombre(nombre, "El nombre", true);
        Valida.nombre(apellidos, "Los apellidos", true);
        Valida.obligatorio(rol, "rol");
        if (!"ADMINISTRACION".equals(rol) && !"MEDICO".equals(rol) && !"ESTILISTA".equals(rol)) {
            throw new ReglaException("El rol elegido no es válido.");
        }
        Valida.obligatorio(turno, "turno");
        Valida.correo(correo, true);
        Valida.telefono(telefono, false);
        Valida.obligatorio(direccion, "dirección");
        Valida.curp(curp, true);
        String correoLimpio = Valida.t(correo);
        Row existente = Db.uno("SELECT id FROM usuario WHERE correo_usuario = ?", correoLimpio);
        if (existente != null && (id == null || existente.id() != id)) {
            throw new ReglaException("Ya existe un empleado con ese correo o usuario.");
        }
        String curpLimpia = Valida.t(curp).toUpperCase(java.util.Locale.ROOT);
        if (Db.contar("SELECT COUNT(*) AS v FROM usuario WHERE curp = ? AND (? IS NULL OR id <> ?)", curpLimpia, id,
                id) > 0) {
            throw new ReglaException("Ya existe un empleado registrado con esa CURP.");
        }
        // un unico Administrador general
        if ("ADMINISTRACION".equals(rol) && Db.contar("SELECT COUNT(*) AS v FROM usuario WHERE "
                + "rol = 'ADMINISTRACION' AND (? IS NULL OR id <> ?)", id, id) > 0) {
            throw new ReglaException("Ya existe un Administrador general. Solo puede haber uno.");
        }
        if (id != null) {
            Row actual = porId(id);
            if (actual == null) {
                throw new ReglaException("El empleado ya no existe.");
            }
            if ("ADMINISTRACION".equals(actual.str("rol")) && (!"ADMINISTRACION".equals(rol)
                    || !"Activo".equals(estado))) {
                throw new ReglaException("El Administrador general no puede cambiar de rol ni inactivarse.");
            }
            Db.ejecutar("UPDATE usuario SET nombre=?, apellidos=?, correo_usuario=?, rol=?, especialidad=?, turno=?, "
                    + "estado=?, telefono=?, curp=?, direccion=? WHERE id=?",
                    Valida.t(nombre), Valida.t(apellidos), correoLimpio, rol, Valida.t(especialidad), turno,
                    estado, Valida.t(telefono), curpLimpia, Valida.t(direccion), id);
            return null;
        }
        String temp = Hash.temporal();
        Db.insertar("INSERT INTO usuario (nombre, apellidos, correo_usuario, contrasena_hash, rol, especialidad, "
                + "turno, estado, telefono, curp, direccion, debe_cambiar_contrasena, contrasena_temporal) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,1,?)",
                Valida.t(nombre), Valida.t(apellidos), correoLimpio, Hash.crear(temp), rol,
                Valida.t(especialidad), turno, estado, Valida.t(telefono), curpLimpia, Valida.t(direccion), temp);
        return temp;
    }

    public static int administradoresActivos() {
        return Db.contar("SELECT COUNT(*) AS v FROM usuario WHERE rol='ADMINISTRACION' AND estado='Activo'");
    }

    /** Cuentas con citas u ordenes de compra: no se pueden borrar sin perder el historial. */
    public static boolean tieneHistorial(long id) {
        return Db.contar("SELECT COUNT(*) AS v FROM cita WHERE profesional_id = ? OR creada_por = ?", id, id)
                + Db.contar("SELECT COUNT(*) AS v FROM orden_compra WHERE creada_por = ?", id) > 0;
    }

    private static boolean esAdminActivo(Row u) {
        return "ADMINISTRACION".equals(u.str("rol")) && "Activo".equals(u.str("estado"));
    }

    /** Reglas que impiden eliminar o inactivar: la propia sesion y el unico administrador activo. */
    public static void validarEliminable(long id) {
        if (id == Sesion.usuarioId) {
            throw new ReglaException("No puedes eliminar ni inactivar tu propia cuenta mientras tienes la sesión iniciada.");
        }
        Row u = porId(id);
        if (u != null && "ADMINISTRACION".equals(u.str("rol"))) {
            throw new ReglaException("El Administrador general no se puede eliminar ni inactivar.");
        }
    }

    public static void cambiarEstado(long id, String estado) {
        if ("Inactivo".equals(estado)) {
            validarEliminable(id);
        }
        Db.ejecutar("UPDATE usuario SET estado = ? WHERE id = ?", estado, id);
    }

    /** Borra al empleado. Solo si no tiene historial; si lo tiene, se debe pasar a Inactivo. */
    public static void eliminar(long id) {
        validarEliminable(id);
        if (tieneHistorial(id)) {
            throw new ReglaException("Este empleado tiene citas u órdenes registradas. Para conservar el historial "
                    + "no se puede eliminar; pásalo a Inactivo.");
        }
        String foto = fotoRuta(id);
        Db.ejecutar("DELETE FROM usuario WHERE id = ?", id);
        ImagenStore.borrar(foto);
    }

    /**
     * Contrasena temporal que administracion le asigno y que el empleado todavia no ha cambiado.
     * Devuelve null si ya la cambio (esa contrasena nunca se guarda en claro).
     */
    public static String contrasenaTemporal(long id) {
        Row r = Db.uno("SELECT contrasena_temporal FROM usuario WHERE id = ? AND debe_cambiar_contrasena = 1", id);
        return r == null || r.str("contrasena_temporal").isEmpty() ? null : r.str("contrasena_temporal");
    }

    public static final int FOTO_MAX_BYTES = ImagenStore.MAX_BYTES;

    /** Nombre del archivo de la foto (dentro de Documentos/VetPlus/imagenes), o cadena vacia. */
    public static String fotoRuta(long id) {
        Row r = Db.uno("SELECT foto_ruta FROM usuario WHERE id = ?", id);
        return r == null ? "" : r.str("foto_ruta");
    }

    /** Foto de perfil del empleado, o null si no tiene o el archivo ya no existe. */
    public static byte[] foto(long id) {
        return ImagenStore.leer(fotoRuta(id));
    }

    /**
     * Guarda (o quita, con null) la foto de perfil. Primero se escribe el archivo nuevo; si la base de datos
     * falla se borra ese archivo y la foto anterior queda intacta.
     */
    public static void guardarFoto(long id, byte[] foto) {
        if (porId(id) == null) {
            throw new ReglaException("El empleado ya no existe.");
        }
        String anterior = fotoRuta(id);
        if (foto == null) {
            Db.ejecutar("UPDATE usuario SET foto_ruta = '' WHERE id = ?", id);
            ImagenStore.borrar(anterior);
            return;
        }
        String nuevo = ImagenStore.guardar(foto, "empleado-" + id);
        try {
            Db.ejecutar("UPDATE usuario SET foto_ruta = ? WHERE id = ?", nuevo, id);
        } catch (RuntimeException e) {
            ImagenStore.borrar(nuevo);
            throw e;
        }
        ImagenStore.borrar(anterior);
    }
}
