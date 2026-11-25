package algo.vetplusapp.imagenes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Db.DbException;
import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.repo.ClienteRepo;
import algo.vetplusapp.repo.UsuarioRepo;
import algo.vetplusapp.soporte.BD;
import algo.vetplusapp.soporte.Datos;
import algo.vetplusapp.soporte.Imagenes;
import algo.vetplusapp.util.Rutas;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Imagenes de personas (empleados) y mascotas: almacenamiento, recuperacion e integridad ante fallos. */
class FotosPersistenciaTest {

    private long cliente;
    private long empleado;

    @BeforeAll
    static void inicio() {
        BD.preparar();
    }

    @BeforeEach
    void limpio() {
        BD.limpiar();
        cliente = Datos.cliente();
        Datos.empleado("foto.empleado@vetplus.com", "MEDICO", Datos.curp(Datos.siguiente()));
        empleado = BD.idUsuario("foto.empleado@vetplus.com");
    }

    private long mascotaConFoto(byte[] foto) {
        return ClienteRepo.guardarMascota(null, cliente, "Luna", "Perro", "Mestizo", "Hembra",
                LocalDate.of(2022, 1, 10), 10.0, false, "", "", "", "", foto, false);
    }

    private long actualizarMascota(long id, byte[] foto, boolean quitar) {
        return ClienteRepo.guardarMascota(id, cliente, "Luna", "Perro", "Mestizo", "Hembra",
                LocalDate.of(2022, 1, 10), 10.0, false, "", "", "", "", foto, quitar);
    }

    // ----------------------------------------------------------------- personas (empleados)

    @Test
    @DisplayName("La foto de un empleado se almacena y se recupera idéntica")
    void empleadoGuardaYRecupera() {
        byte[] png = Imagenes.png(30, 30);
        assertNull(UsuarioRepo.foto(empleado), "al inicio no tiene foto");
        UsuarioRepo.guardarFoto(empleado, png);
        assertArrayEquals(png, UsuarioRepo.foto(empleado));
        assertTrue(UsuarioRepo.fotoRuta(empleado).startsWith("empleado-" + empleado + "-"));
        assertEquals(1, BD.archivosDeImagen());
        assertTrue(Files.isRegularFile(Rutas.carpetaImagenes().resolve(UsuarioRepo.fotoRuta(empleado))));
    }

    @Test
    @DisplayName("Reemplazar la foto borra la anterior; quitarla deja la carpeta limpia")
    void empleadoReemplazaYQuita() {
        UsuarioRepo.guardarFoto(empleado, Imagenes.png(30, 30));
        byte[] jpg = Imagenes.jpg(40, 40);
        UsuarioRepo.guardarFoto(empleado, jpg);
        assertArrayEquals(jpg, UsuarioRepo.foto(empleado));
        assertEquals(1, BD.archivosDeImagen(), "no se acumulan fotos viejas");
        UsuarioRepo.guardarFoto(empleado, null);
        assertNull(UsuarioRepo.foto(empleado));
        assertEquals("", UsuarioRepo.fotoRuta(empleado));
        assertEquals(0, BD.archivosDeImagen());
    }

    @Test
    @DisplayName("Una foto inválida se rechaza y la anterior queda intacta")
    void empleadoFotoInvalida() {
        byte[] buena = Imagenes.png(30, 30);
        UsuarioRepo.guardarFoto(empleado, buena);
        assertThrows(ReglaException.class, () -> UsuarioRepo.guardarFoto(empleado, Imagenes.texto()));
        assertThrows(ReglaException.class, () -> UsuarioRepo.guardarFoto(empleado, Imagenes.gif()));
        assertArrayEquals(buena, UsuarioRepo.foto(empleado));
        assertEquals(1, BD.archivosDeImagen());
        assertThrows(ReglaException.class, () -> UsuarioRepo.guardarFoto(999999L, buena), "empleado inexistente");
    }

    @Test
    @DisplayName("Si la base de datos falla al guardar la foto del empleado, se conserva la anterior")
    void empleadoIntegridadAnteFalloDeBD() {
        byte[] original = Imagenes.png(30, 30);
        UsuarioRepo.guardarFoto(empleado, original);
        Db.ejecutar("DROP TRIGGER IF EXISTS t_falla_usuario");
        Db.ejecutar("CREATE TRIGGER t_falla_usuario BEFORE UPDATE ON usuario FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'fallo simulado'");
        try {
            assertThrows(DbException.class, () -> UsuarioRepo.guardarFoto(empleado, Imagenes.jpg(50, 50)));
        } finally {
            Db.ejecutar("DROP TRIGGER IF EXISTS t_falla_usuario");
        }
        assertArrayEquals(original, UsuarioRepo.foto(empleado), "la foto anterior sigue ahí");
        assertEquals(1, BD.archivosDeImagen(), "y no quedó el archivo nuevo huérfano");
    }

    // ----------------------------------------------------------------- mascotas

    @Test
    @DisplayName("La foto de una mascota se almacena y se recupera idéntica")
    void mascotaGuardaYRecupera() {
        byte[] jpg = Imagenes.jpg(60, 40);
        long m = mascotaConFoto(jpg);
        assertArrayEquals(jpg, ClienteRepo.fotoMascota(m));
        assertFalse(ClienteRepo.fotoRutaMascota(m).isEmpty());
        assertEquals(1, BD.archivosDeImagen());
    }

    @Test
    @DisplayName("Mascota sin foto: se controla (null) sin error")
    void mascotaSinFoto() {
        long m = mascotaConFoto(null);
        assertNull(ClienteRepo.fotoMascota(m));
        assertEquals(0, BD.archivosDeImagen());
    }

    @Test
    @DisplayName("Editar sin tocar la foto la conserva; cambiarla borra la vieja; quitarla la elimina")
    void mascotaCambiosDeFoto() {
        byte[] primera = Imagenes.png(30, 30);
        long m = mascotaConFoto(primera);
        actualizarMascota(m, null, false);
        assertArrayEquals(primera, ClienteRepo.fotoMascota(m), "se conserva");
        byte[] segunda = Imagenes.jpg(30, 30);
        actualizarMascota(m, segunda, false);
        assertArrayEquals(segunda, ClienteRepo.fotoMascota(m));
        assertEquals(1, BD.archivosDeImagen(), "la anterior se borró");
        actualizarMascota(m, null, true);
        assertNull(ClienteRepo.fotoMascota(m));
        assertEquals(0, BD.archivosDeImagen());
    }

    @Test
    @DisplayName("Una foto con formato incorrecto impide el registro y no deja nada guardado")
    void mascotaFotoInvalida() {
        assertTrue(assertThrows(ReglaException.class, () -> mascotaConFoto(Imagenes.texto())).getMessage()
                .contains("Formato"));
        assertThrows(ReglaException.class, () -> mascotaConFoto(Imagenes.pngDanado()));
        assertEquals(0, BD.contar("mascota"), "no se creó la mascota");
        assertEquals(0, BD.archivosDeImagen(), "ni quedaron archivos");
    }

    @Test
    @DisplayName("Si falla la base de datos al registrar, no queda el archivo de la foto")
    void mascotaIntegridadAnteFalloDeBDAlRegistrar() {
        Db.ejecutar("DROP TRIGGER IF EXISTS t_falla_mascota");
        Db.ejecutar("CREATE TRIGGER t_falla_mascota BEFORE INSERT ON mascota FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'fallo simulado'");
        try {
            assertThrows(DbException.class, () -> mascotaConFoto(Imagenes.png(20, 20)));
        } finally {
            Db.ejecutar("DROP TRIGGER IF EXISTS t_falla_mascota");
        }
        assertEquals(0, BD.contar("mascota"));
        assertEquals(0, BD.archivosDeImagen(), "el archivo nuevo se limpió");
    }

    @Test
    @DisplayName("Si falla la base de datos al editar, la foto anterior sigue intacta")
    void mascotaIntegridadAnteFalloDeBDAlEditar() {
        byte[] original = Imagenes.png(30, 30);
        long m = mascotaConFoto(original);
        Db.ejecutar("DROP TRIGGER IF EXISTS t_falla_mascota_upd");
        Db.ejecutar("CREATE TRIGGER t_falla_mascota_upd BEFORE UPDATE ON mascota FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'fallo simulado'");
        try {
            assertThrows(DbException.class, () -> actualizarMascota(m, Imagenes.jpg(50, 50), false));
        } finally {
            Db.ejecutar("DROP TRIGGER IF EXISTS t_falla_mascota_upd");
        }
        assertArrayEquals(original, ClienteRepo.fotoMascota(m));
        assertEquals(1, BD.archivosDeImagen());
    }

    @Test
    @DisplayName("Si el archivo de la foto desaparece, la mascota se sigue mostrando sin romper nada")
    void mascotaArchivoDesaparecido() throws IOException {
        long m = mascotaConFoto(Imagenes.png(30, 30));
        Files.delete(Rutas.carpetaImagenes().resolve(ClienteRepo.fotoRutaMascota(m)));
        assertNull(ClienteRepo.fotoMascota(m));
        assertEquals("Luna", ClienteRepo.mascota(m).str("nombre"));
    }

    @Test
    @DisplayName("Al eliminar la mascota (o al cliente con sus mascotas) se borran sus fotos")
    void eliminarBorraFotos() {
        long m1 = mascotaConFoto(Imagenes.png(30, 30));
        long m2 = ClienteRepo.guardarMascota(null, cliente, "Milo", "Perro", "", "Macho", null, null, false, "", "",
                "", "", Imagenes.jpg(30, 30), false);
        assertEquals(2, BD.archivosDeImagen());
        ClienteRepo.eliminarMascota(m1);
        assertEquals(1, BD.archivosDeImagen());
        ClienteRepo.eliminarPropietario(cliente);
        assertEquals(0, BD.archivosDeImagen());
        assertNull(ClienteRepo.mascota(m2));
    }
}
