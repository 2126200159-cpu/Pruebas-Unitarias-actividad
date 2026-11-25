package algo.vetplusapp.mascotas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** Mascotas: registro, consulta, actualizacion, eliminacion y asociacion con sus propietarios. */
class MascotaCrudTest {

    @BeforeAll
    static void inicio() {
        BD.preparar();
    }

    @BeforeEach
    void limpio() {
        BD.limpiar();
    }

    private long guardar(Long id, long propietario, String nombre, String raza, Double peso) {
        return ClienteRepo.guardarMascota(id, propietario, nombre, "Perro", raza, "Hembra",
                LocalDate.of(2022, 3, 14), peso, true, "985112345678901", "Croquetas", "Alergia al pollo",
                "Cuidado con las orejas", null, false);
    }

    @Test
    @DisplayName("Registrar y consultar una mascota asociada a su propietario")
    void registrarYConsultar() {
        long c = Datos.cliente();
        long m = guardar(null, c, "Luna", "Golden retriever", 28.4);
        Row r = ClienteRepo.mascota(m);
        assertNotNull(r);
        assertEquals("Luna", r.str("nombre"));
        assertEquals("Golden retriever", r.str("raza"));
        assertEquals(28.4, r.dbl("peso"), 0.001);
        assertEquals("2022-03-14", r.str("fecha_nacimiento"));
        assertTrue(r.bool("esterilizada"));
        assertEquals("Alergia al pollo", r.str("antecedentes"));
        assertEquals("Activa", r.str("estado"));
        assertEquals(c, r.lng("prop_id"), "queda asociada a su propietario");
        assertEquals("Ana Prueba", r.str("propietario"));
    }

    @Test
    @DisplayName("Cada mascota aparece solo con su propietario")
    void asociacionConClientes() {
        long a = Datos.cliente();
        long b = Datos.cliente();
        long luna = Datos.mascota(a, "Luna");
        long milo = Datos.mascota(a, "Milo");
        long toby = Datos.mascota(b, "Toby");
        assertEquals(2, ClienteRepo.mascotasDe(a).size());
        assertEquals(1, ClienteRepo.mascotasDe(b).size());
        assertTrue(ClienteRepo.mascotasDe(a).stream().anyMatch(r -> r.id() == luna));
        assertTrue(ClienteRepo.mascotasDe(a).stream().anyMatch(r -> r.id() == milo));
        assertFalse(ClienteRepo.mascotasDe(a).stream().anyMatch(r -> r.id() == toby));
        assertEquals(b, ClienteRepo.mascota(toby).lng("prop_id"));
    }

    @Test
    @DisplayName("Actualizar los datos de una mascota")
    void actualizar() {
        long c = Datos.cliente();
        long m = guardar(null, c, "Luna", "Golden retriever", 28.4);
        guardar(m, c, "Luna", "Labrador", 30.1);
        Row r = ClienteRepo.mascota(m);
        assertEquals("Labrador", r.str("raza"));
        assertEquals(30.1, r.dbl("peso"), 0.001);
        assertEquals(1, ClienteRepo.mascotasDe(c).size(), "se actualizó, no se duplicó");
    }

    @Test
    @DisplayName("Eliminar una mascota sin historial (y sus vacunas)")
    void eliminar() {
        long c = Datos.cliente();
        long m = Datos.mascota(c, "Luna");
        ClienteRepo.registrarCuidado(m, "Desparasitación", LocalDate.now(), LocalDate.now().plusMonths(3), "Dr. Mendoza");
        ClienteRepo.eliminarMascota(m);
        assertNull(ClienteRepo.mascota(m));
        assertEquals(0, BD.contar("vacuna_cuidado"));
        assertNotNull(ClienteRepo.propietario(c), "el propietario sigue");
    }

    @Test
    @DisplayName("Una mascota con citas no se elimina; se puede pasar a Inactiva")
    void conHistorialNoSeElimina() {
        long c = Datos.cliente();
        long m = Datos.mascota(c, "Luna");
        Datos.cita(m, "10:00");
        assertTrue(ClienteRepo.tieneHistorialMascota(m));
        assertTrue(assertThrows(ReglaException.class, () -> ClienteRepo.eliminarMascota(m)).getMessage()
                .contains("historial"));
        assertNotNull(ClienteRepo.mascota(m));
        ClienteRepo.cambiarEstadoMascota(m, false);
        assertEquals("Inactiva", ClienteRepo.mascota(m).str("estado"));
        assertTrue(ClienteRepo.mascotasActivasDe(c).isEmpty(), "ya no aparece para agendar");
    }

    @Test
    @DisplayName("No se puede registrar una mascota para un propietario que no existe")
    void propietarioInexistente() {
        ReglaException ex = assertThrows(ReglaException.class, () -> guardar(null, 999999L, "Luna", "Mestizo", 10.0));
        assertTrue(ex.getMessage().contains("propietario"));
        assertEquals(0, BD.contar("mascota"));
    }

    @Test
    @DisplayName("Validaciones: nombre solo letras, nacimiento no futuro, peso razonable")
    void validaciones() {
        long c = Datos.cliente();
        assertTrue(assertThrows(ReglaException.class, () -> guardar(null, c, "Luna2", "Mestizo", 10.0))
                .getMessage().contains("no se aceptan números"));
        assertTrue(assertThrows(ReglaException.class, () -> guardar(null, c, "", "Mestizo", 10.0)).getMessage()
                .contains("Falta capturar"));
        assertTrue(assertThrows(ReglaException.class, () -> ClienteRepo.guardarMascota(null, c, "Luna", "Perro", "",
                "Hembra", LocalDate.now().plusDays(1), 10.0, false, "", "", "", "", null, false)).getMessage()
                .contains("futura"));
        assertTrue(assertThrows(ReglaException.class, () -> guardar(null, c, "Luna", "Mestizo", -2.0)).getMessage()
                .contains("peso"));
        assertTrue(assertThrows(ReglaException.class, () -> guardar(null, c, "Luna", "Mestizo", 501.0)).getMessage()
                .contains("peso"));
        assertEquals(0, BD.contar("mascota"));
    }

    @Test
    @DisplayName("Un cliente no puede tener dos mascotas con el mismo nombre y especie")
    void sinDuplicados() {
        long a = Datos.cliente();
        long b = Datos.cliente();
        Datos.mascota(a, "Luna");
        assertTrue(assertThrows(ReglaException.class, () -> Datos.mascota(a, "Luna")).getMessage()
                .contains("ya tiene una mascota"));
        assertDoesNotThrow(() -> Datos.mascota(b, "Luna"), "otro cliente sí puede tener una Luna");
        assertEquals(2, BD.contar("mascota"));
    }
}
