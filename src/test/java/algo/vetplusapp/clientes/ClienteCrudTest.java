package algo.vetplusapp.clientes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.ClienteRepo;
import algo.vetplusapp.soporte.BD;
import algo.vetplusapp.soporte.Datos;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Personas-clientes: registro, consulta, actualizacion y eliminacion conjunta (cliente + mascotas). */
class ClienteCrudTest {

    @BeforeAll
    static void inicio() {
        BD.preparar();
    }

    @BeforeEach
    void limpio() {
        BD.limpiar();
    }

    @Test
    @DisplayName("Registrar y consultar un cliente")
    void registrarYConsultar() {
        long id = ClienteRepo.guardarPropietario(null, "María", "López Ruiz", "3312345678", "maria@prueba.com",
                "Av. Chapultepec 100", "GOMA900515MJCRRN09", "Pedro López", "3398765432");
        assertTrue(id > 0);
        Row c = ClienteRepo.propietario(id);
        assertNotNull(c);
        assertEquals("María", c.str("nombre"));
        assertEquals("López Ruiz", c.str("apellidos"));
        assertEquals("María López Ruiz", c.str("nombre_completo"));
        assertEquals("3312345678", c.str("telefono"));
        assertEquals("maria@prueba.com", c.str("correo"));
        assertEquals("GOMA900515MJCRRN09", c.str("curp"));
        assertEquals("Activo", c.str("estado"));
        assertEquals("Pedro López", c.str("contacto_alterno_nombre"));
        assertEquals(LocalDate.now().toString(), c.str("fecha_registro"));
        assertTrue(ClienteRepo.propietarios().stream().anyMatch(r -> r.id() == id), "aparece en el listado");
    }

    @Test
    @DisplayName("Actualizar los datos de un cliente")
    void actualizar() {
        long id = Datos.cliente();
        Row antes = ClienteRepo.propietario(id);
        ClienteRepo.guardarPropietario(id, "Ana", "Prueba", "3399999999", antes.str("correo"), "Calle Nueva 55",
                antes.str("curp"), "Luis Prueba", "3311111111");
        Row c = ClienteRepo.propietario(id);
        assertEquals("3399999999", c.str("telefono"));
        assertEquals("Calle Nueva 55", c.str("direccion"));
        assertEquals("Luis Prueba", c.str("contacto_alterno_nombre"));
        assertEquals(1, ClienteRepo.propietarios().stream().filter(r -> r.id() == id).count(), "sigue siendo uno");
    }

    @Test
    @DisplayName("Actualizar un cliente que ya no existe avisa con un mensaje claro")
    void actualizarInexistente() {
        ReglaException ex = assertThrows(ReglaException.class, () -> ClienteRepo.guardarPropietario(999999L, "Ana",
                "Prueba", Datos.telefono(777), "x@prueba.com", "Calle 1", Datos.curp(77), "", ""));
        assertTrue(ex.getMessage().contains("ya no existe"));
    }

    @Test
    @DisplayName("Eliminación conjunta: se borra el cliente junto con sus mascotas y vacunas")
    void eliminacionConjunta() {
        long c = Datos.cliente();
        long m1 = Datos.mascota(c, "Luna");
        long m2 = Datos.mascota(c, "Milo");
        ClienteRepo.registrarCuidado(m1, "Vacunación", LocalDate.now(), LocalDate.now().plusYears(1), "Dra. Torres");
        long otro = Datos.cliente();
        long mOtro = Datos.mascota(otro, "Toby");

        ClienteRepo.eliminarPropietario(c);

        assertNull(ClienteRepo.propietario(c));
        assertNull(ClienteRepo.mascota(m1));
        assertNull(ClienteRepo.mascota(m2));
        assertEquals(0, BD.contar("vacuna_cuidado"));
        assertNotNull(ClienteRepo.propietario(otro), "otro cliente no se toca");
        assertNotNull(ClienteRepo.mascota(mOtro), "sus mascotas tampoco");
    }

    @Test
    @DisplayName("Un cliente con historial no se elimina; se puede pasar a Inactivo")
    void conHistorialNoSeElimina() {
        long c = Datos.cliente();
        long m = Datos.mascota(c, "Luna");
        Datos.cita(m, "10:00");
        assertTrue(ClienteRepo.tieneHistorialPropietario(c));

        ReglaException ex = assertThrows(ReglaException.class, () -> ClienteRepo.eliminarPropietario(c));
        assertTrue(ex.getMessage().contains("historial"));
        assertNotNull(ClienteRepo.propietario(c), "el cliente sigue ahí");
        assertNotNull(ClienteRepo.mascota(m), "y su mascota también");

        ClienteRepo.cambiarEstadoPropietario(c, false);
        assertEquals("Inactivo", ClienteRepo.propietario(c).str("estado"));
        ClienteRepo.cambiarEstadoPropietario(c, true);
        assertEquals("Activo", ClienteRepo.propietario(c).str("estado"));
    }

    @Test
    @DisplayName("No se permiten clientes duplicados (CURP, correo, o mismo nombre y teléfono)")
    void sinDuplicados() {
        long c = Datos.cliente();
        Row p = ClienteRepo.propietario(c);

        assertTrue(assertThrows(ReglaException.class, () -> ClienteRepo.guardarPropietario(null, "Luis", "Otro",
                Datos.telefono(801), "otro1@prueba.com", "Calle 2", p.str("curp"), "", "")).getMessage()
                .contains("CURP"));
        assertTrue(assertThrows(ReglaException.class, () -> ClienteRepo.guardarPropietario(null, "Luis", "Otro",
                Datos.telefono(802), p.str("correo"), "Calle 2", Datos.curp(802), "", "")).getMessage()
                .contains("correo"));
        assertTrue(assertThrows(ReglaException.class, () -> ClienteRepo.guardarPropietario(null, p.str("nombre"),
                p.str("apellidos"), p.str("telefono"), "otro3@prueba.com", "Calle 2", Datos.curp(803), "", ""))
                .getMessage().contains("mismo nombre y teléfono"));
        assertEquals(1, BD.contar("propietario"), "no se creó ningún duplicado");
        assertDoesNotThrow(() -> ClienteRepo.guardarPropietario(c, p.str("nombre"), p.str("apellidos"),
                p.str("telefono"), p.str("correo"), "Calle 1", p.str("curp"), "", ""), "editarse a sí mismo sí");
    }

    @Test
    @DisplayName("Correo, dirección y CURP son obligatorios")
    void camposObligatorios() {
        assertTrue(assertThrows(ReglaException.class, () -> ClienteRepo.guardarPropietario(null, "Ana", "Prueba",
                "3312345678", "", "Calle 1", Datos.curp(5), "", "")).getMessage().contains("correo"));
        assertTrue(assertThrows(ReglaException.class, () -> ClienteRepo.guardarPropietario(null, "Ana", "Prueba",
                "3312345678", "a@prueba.com", "  ", Datos.curp(5), "", "")).getMessage().contains("dirección"));
        assertTrue(assertThrows(ReglaException.class, () -> ClienteRepo.guardarPropietario(null, "Ana", "Prueba",
                "3312345678", "a@prueba.com", "Calle 1", "", "", "")).getMessage().contains("CURP"));
        assertTrue(assertThrows(ReglaException.class, () -> ClienteRepo.guardarPropietario(null, "Ana2", "Prueba",
                "3312345678", "a@prueba.com", "Calle 1", Datos.curp(5), "", "")).getMessage()
                .contains("no se aceptan números"));
        assertTrue(assertThrows(ReglaException.class, () -> ClienteRepo.guardarPropietario(null, "Ana", "Prueba",
                "33abc", "a@prueba.com", "Calle 1", Datos.curp(5), "", "")).getMessage().contains("números"));
        assertEquals(0, BD.contar("propietario"));
    }
}
