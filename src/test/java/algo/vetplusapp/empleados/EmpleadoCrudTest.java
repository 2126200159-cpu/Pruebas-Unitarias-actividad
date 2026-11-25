package algo.vetplusapp.empleados;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.CitaRepo;
import algo.vetplusapp.repo.UsuarioRepo;
import algo.vetplusapp.soporte.BD;
import algo.vetplusapp.soporte.Datos;
import algo.vetplusapp.soporte.Imagenes;
import algo.vetplusapp.util.Sesion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Personas-empleados: registro, consulta, actualizacion, eliminacion conjunta, duplicados y administrador unico. */
class EmpleadoCrudTest {

    private static final String CORREO = "nuevo.medico@vetplus.com";

    @BeforeAll
    static void inicio() {
        BD.preparar();
    }

    @BeforeEach
    void limpio() {
        BD.limpiar();
    }

    @AfterEach
    void sinSesion() {
        Sesion.limpiar();
    }

    private long nuevoMedico() {
        Datos.empleado(CORREO, "MEDICO", Datos.curp(Datos.siguiente()));
        return BD.idUsuario(CORREO);
    }

    @Test
    @DisplayName("Registrar y consultar un empleado (con contraseña temporal)")
    void registrarYConsultar() {
        String temp = Datos.empleado(CORREO, "MEDICO", Datos.curp(Datos.siguiente()));
        assertNotNull(temp);
        assertEquals(8, temp.length());
        long id = BD.idUsuario(CORREO);
        assertTrue(id > 0);
        Row u = UsuarioRepo.porId(id);
        assertEquals("Pedro Ruiz", u.str("nombre_completo"));
        assertEquals("MEDICO", u.str("rol"));
        assertEquals("Matutino", u.str("turno"));
        assertEquals("Activo", u.str("estado"));
        assertTrue(u.bool("debe_cambiar_contrasena"));
        assertEquals(temp, UsuarioRepo.contrasenaTemporal(id));
        assertTrue(UsuarioRepo.todos().stream().anyMatch(r -> r.id() == id));
        assertTrue(UsuarioRepo.profesionalesActivos("MEDICO").stream().anyMatch(r -> r.id() == id));
    }

    @Test
    @DisplayName("La contraseña temporal permite iniciar sesión; una incorrecta no")
    void inicioDeSesion() {
        String temp = Datos.empleado(CORREO, "MEDICO", Datos.curp(Datos.siguiente()));
        assertEquals(CORREO, UsuarioRepo.autenticar(CORREO, temp).str("correo_usuario"));
        assertEquals(CORREO, UsuarioRepo.autenticar("nuevo.medico", temp).str("correo_usuario"),
                "también acepta solo la parte antes del @");
        assertTrue(assertThrows(ReglaException.class, () -> UsuarioRepo.autenticar(CORREO, "incorrecta"))
                .getMessage().contains("incorrectos"));
    }

    @Test
    @DisplayName("Actualizar los datos de un empleado")
    void actualizar() {
        long id = nuevoMedico();
        String curp = UsuarioRepo.porId(id).str("curp");
        assertNull(UsuarioRepo.guardar(id, "Pedro", "Ruiz", "MEDICO", "Cirugía", "Vespertino", "Activo", CORREO,
                "3399999999", curp, "Dirección nueva 9"));
        Row u = UsuarioRepo.porId(id);
        assertEquals("Vespertino", u.str("turno"));
        assertEquals("Cirugía", u.str("especialidad"));
        assertEquals("Dirección nueva 9", u.str("direccion"));
        assertEquals("3399999999", u.str("telefono"));
    }

    @Test
    @DisplayName("Eliminar un empleado sin historial")
    void eliminarSinHistorial() {
        long id = nuevoMedico();
        UsuarioRepo.eliminar(id);
        assertNull(UsuarioRepo.porId(id));
    }

    @Test
    @DisplayName("Eliminación conjunta: al borrar al empleado también se borra su foto")
    void eliminacionConjuntaConFoto() {
        long id = nuevoMedico();
        UsuarioRepo.guardarFoto(id, Imagenes.png(20, 20));
        assertEquals(1, BD.archivosDeImagen());
        UsuarioRepo.eliminar(id);
        assertNull(UsuarioRepo.porId(id));
        assertEquals(0, BD.archivosDeImagen(), "no queda la foto huérfana");
    }

    @Test
    @DisplayName("Un empleado con citas no se elimina; se puede pasar a Inactivo")
    void conHistorialNoSeElimina() {
        long id = nuevoMedico();
        long mascota = Datos.mascota(Datos.cliente(), "Luna");
        CitaRepo.crear(mascota, Datos.servicio("Consulta"), id, Datos.fechaFutura(), "10:00", "");
        assertTrue(UsuarioRepo.tieneHistorial(id));
        ReglaException ex = assertThrows(ReglaException.class, () -> UsuarioRepo.eliminar(id));
        assertTrue(ex.getMessage().contains("citas u órdenes"));
        assertNotNull(UsuarioRepo.porId(id));
        UsuarioRepo.cambiarEstado(id, "Inactivo");
        assertEquals("Inactivo", UsuarioRepo.porId(id).str("estado"));
    }

    @Test
    @DisplayName("No se permiten empleados duplicados (correo o CURP)")
    void sinDuplicados() {
        long id = nuevoMedico();
        String curp = UsuarioRepo.porId(id).str("curp");
        assertTrue(assertThrows(ReglaException.class, () -> Datos.empleado(CORREO, "MEDICO",
                Datos.curp(Datos.siguiente()))).getMessage().contains("correo"));
        assertTrue(assertThrows(ReglaException.class, () -> Datos.empleado(CORREO.toUpperCase(), "MEDICO",
                Datos.curp(Datos.siguiente()))).getMessage().contains("correo"), "sin distinguir mayúsculas");
        assertTrue(assertThrows(ReglaException.class, () -> Datos.empleado("otro@vetplus.com", "ESTILISTA", curp))
                .getMessage().contains("CURP"));
        assertEquals(0, BD.idUsuario("otro@vetplus.com"));
    }

    @Test
    @DisplayName("Solo puede haber un Administrador general")
    void administradorUnico() {
        long admin = BD.idUsuario("admin@vetplus.com");
        assertEquals(1, Db.contar("SELECT COUNT(*) AS v FROM usuario WHERE rol = 'ADMINISTRACION'"));

        assertTrue(assertThrows(ReglaException.class, () -> Datos.empleado("otro.admin@vetplus.com",
                "ADMINISTRACION", Datos.curp(Datos.siguiente()))).getMessage().contains("Administrador general"));

        long medico = nuevoMedico();
        assertTrue(assertThrows(ReglaException.class, () -> UsuarioRepo.guardar(medico, "Pedro", "Ruiz",
                "ADMINISTRACION", "General", "Matutino", "Activo", CORREO, "3312345678",
                UsuarioRepo.porId(medico).str("curp"), "Calle 1")).getMessage().contains("Administrador general"),
                "tampoco se puede ascender a un empleado existente");

        String curp = Datos.curp(Datos.siguiente());
        assertTrue(assertThrows(ReglaException.class, () -> UsuarioRepo.guardar(admin, "Administrador", "VETPLUS",
                "MEDICO", "General", "Matutino", "Activo", "admin@vetplus.com", "3312345678", curp, "Calle 1"))
                .getMessage().contains("no puede cambiar de rol"));
        assertTrue(assertThrows(ReglaException.class, () -> UsuarioRepo.guardar(admin, "Administrador", "VETPLUS",
                "ADMINISTRACION", "General", "Matutino", "Inactivo", "admin@vetplus.com", "3312345678", curp,
                "Calle 1")).getMessage().contains("no puede cambiar de rol ni inactivarse"));
        assertThrows(ReglaException.class, () -> UsuarioRepo.eliminar(admin));
        assertThrows(ReglaException.class, () -> UsuarioRepo.cambiarEstado(admin, "Inactivo"));
        assertEquals(1, UsuarioRepo.administradoresActivos());
        assertEquals(1, Db.contar("SELECT COUNT(*) AS v FROM usuario WHERE rol = 'ADMINISTRACION'"));
    }

    @Test
    @DisplayName("Cuando el empleado cambia su contraseña, la temporal deja de verse")
    void cambioDeContrasena() {
        String temp = Datos.empleado(CORREO, "MEDICO", Datos.curp(Datos.siguiente()));
        long id = BD.idUsuario(CORREO);
        assertEquals(temp, UsuarioRepo.contrasenaTemporal(id));
        UsuarioRepo.cambiarContrasena(id, "Nueva123");
        assertNull(UsuarioRepo.contrasenaTemporal(id));
        assertEquals(CORREO, UsuarioRepo.autenticar(CORREO, "Nueva123").str("correo_usuario"));
        assertThrows(ReglaException.class, () -> UsuarioRepo.autenticar(CORREO, temp));
        assertTrue(assertThrows(ReglaException.class, () -> UsuarioRepo.cambiarContrasena(id, "123")).getMessage()
                .contains("al menos 6"));
    }

    @Test
    @DisplayName("No se puede eliminar ni inactivar la propia cuenta con la sesión abierta")
    void propiaCuenta() {
        long id = nuevoMedico();
        Sesion.usuarioId = id;
        assertTrue(assertThrows(ReglaException.class, () -> UsuarioRepo.eliminar(id)).getMessage()
                .contains("propia cuenta"));
        assertFalse(UsuarioRepo.porId(id) == null);
    }

    @Test
    @DisplayName("Campos obligatorios, rol y correo válidos")
    void validaciones() {
        String curp = Datos.curp(Datos.siguiente());
        assertTrue(assertThrows(ReglaException.class, () -> UsuarioRepo.guardar(null, "Pedro", "Ruiz", "MEDICO", "",
                "Matutino", "Activo", "no-es-correo", "", curp, "Calle 1")).getMessage().contains("correo"));
        assertTrue(assertThrows(ReglaException.class, () -> UsuarioRepo.guardar(null, "Pedro", "Ruiz", "MEDICO", "",
                "Matutino", "Activo", CORREO, "", curp, "")).getMessage().contains("dirección"));
        assertTrue(assertThrows(ReglaException.class, () -> UsuarioRepo.guardar(null, "Pedro", "Ruiz", "MEDICO", "",
                "Matutino", "Activo", CORREO, "", "", "Calle 1")).getMessage().contains("CURP"));
        assertTrue(assertThrows(ReglaException.class, () -> UsuarioRepo.guardar(null, "Pedro", "Ruiz", "GERENTE", "",
                "Matutino", "Activo", CORREO, "", curp, "Calle 1")).getMessage().contains("rol"));
        assertTrue(assertThrows(ReglaException.class, () -> UsuarioRepo.guardar(null, "Pedro9", "Ruiz", "MEDICO", "",
                "Matutino", "Activo", CORREO, "", curp, "Calle 1")).getMessage().contains("números"));
        assertEquals(0, BD.idUsuario(CORREO));
    }
}
