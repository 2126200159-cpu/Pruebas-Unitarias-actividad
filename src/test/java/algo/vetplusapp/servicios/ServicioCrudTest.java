package algo.vetplusapp.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.CajaRepo;
import algo.vetplusapp.repo.CitaRepo;
import algo.vetplusapp.repo.ServicioRepo;
import algo.vetplusapp.soporte.BD;
import algo.vetplusapp.soporte.Datos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Servicios: registro, consulta, actualizacion y eliminacion, incluyendo costo y tiempo estimado. */
class ServicioCrudTest {

    @BeforeAll
    static void inicio() {
        BD.preparar();
    }

    @BeforeEach
    void limpio() {
        BD.limpiar();
    }

    @Test
    @DisplayName("Registrar y consultar un servicio con su costo y tiempo estimado")
    void registrarYConsultar() {
        long id = ServicioRepo.crear("Radiografía", "Estudio de rayos X", "MEDICO", 650.0, 45);
        Row s = ServicioRepo.porId(id);
        assertNotNull(s);
        assertEquals("Radiografía", s.str("nombre"));
        assertEquals("Estudio de rayos X", s.str("descripcion"));
        assertEquals("MEDICO", s.str("tipo"));
        assertEquals(650.0, s.dbl("precio"), 0.001);
        assertEquals(45, s.integer("duracion_min"));
        assertTrue(s.bool("activo"));
        assertEquals(id, ServicioRepo.porNombre("Radiografía").id());
        assertTrue(ServicioRepo.activos().stream().anyMatch(r -> r.id() == id));
        assertTrue(ServicioRepo.todos().size() >= 7);
    }

    @Test
    @DisplayName("El catálogo trae los 6 servicios base con precio y duración")
    void catalogoBase() {
        for (String nombre : ServicioRepo.BASE) {
            Row s = ServicioRepo.porNombre(nombre);
            assertNotNull(s, nombre);
            assertTrue(s.dbl("precio") >= 0, nombre);
        }
        assertNull(ServicioRepo.porNombre("Pensión").obj("duracion_min"), "la pensión se cobra por día");
        assertTrue(ServicioRepo.porNombre("Consulta").integer("duracion_min") > 0);
    }

    @Test
    @DisplayName("Actualizar costo y tiempo estimado")
    void actualizarCostoYTiempo() {
        long id = ServicioRepo.crear("Ultrasonido", "", "MEDICO", 500.0, 30);
        ServicioRepo.actualizarTarifa(id, 720.5, 50);
        Row s = ServicioRepo.porId(id);
        assertEquals(720.5, s.dbl("precio"), 0.001);
        assertEquals(50, s.integer("duracion_min"));
        ServicioRepo.actualizarDatos(id, "Ultrasonido abdominal", "Estudio abdominal");
        assertEquals("Ultrasonido abdominal", ServicioRepo.porId(id).str("nombre"));
        ServicioRepo.cambiarActivo(id, false);
        assertFalse(ServicioRepo.porId(id).bool("activo"));
        assertFalse(ServicioRepo.activos().stream().anyMatch(r -> r.id() == id));
    }

    @Test
    @DisplayName("Los servicios base cambian de precio pero no de nombre")
    void servicioBaseNoSeRenombra() {
        Row consulta = ServicioRepo.porNombre("Consulta");
        double precioOriginal = consulta.dbl("precio");
        int duracionOriginal = consulta.integer("duracion_min");
        try {
            ServicioRepo.actualizarTarifa(consulta.id(), 410.0, 40);
            assertEquals(410.0, ServicioRepo.porId(consulta.id()).dbl("precio"), 0.001);
            assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.actualizarDatos(consulta.id(), "Otro",
                    "")).getMessage().contains("base"));
        } finally {
            ServicioRepo.actualizarTarifa(consulta.id(), precioOriginal, duracionOriginal);
        }
    }

    @Test
    @DisplayName("Eliminar un servicio sin relaciones")
    void eliminar() {
        long id = ServicioRepo.crear("Corte de uñas", "", "ESTETICA", 80.0, 15);
        ServicioRepo.eliminar(id);
        assertNull(ServicioRepo.porId(id));
    }

    @Test
    @DisplayName("Los servicios base no se eliminan")
    void baseNoSeElimina() {
        long id = ServicioRepo.porNombre("Vacunación").id();
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.eliminar(id)).getMessage()
                .contains("servicio base"));
        assertNotNull(ServicioRepo.porId(id));
    }

    @Test
    @DisplayName("Un servicio usado en citas o cuentas no se elimina")
    void conRelacionesNoSeElimina() {
        long id = ServicioRepo.crear("Radiografía", "", "MEDICO", 650.0, 30);
        long m = Datos.mascota(Datos.cliente(), "Luna");
        CitaRepo.crear(m, id, Datos.medico(), Datos.fechaFutura(), "10:00", "");
        assertTrue(ServicioRepo.tieneRelaciones(id));
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.eliminar(id)).getMessage()
                .contains("citas o cuentas"));
        assertNotNull(ServicioRepo.porId(id));
    }

    @Test
    @DisplayName("Validaciones de nombre, tipo, costo y tiempo")
    void validaciones() {
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.crear("", "", "MEDICO", 100.0, 30))
                .getMessage().contains("nombre"));
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.crear("X".repeat(61), "", "MEDICO", 100.0,
                30)).getMessage().contains("60"));
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.crear("Nuevo", "", "OTRO", 100.0, 30))
                .getMessage().contains("tipo"));
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.crear("Nuevo", "", "MEDICO", -1.0, 30))
                .getMessage().contains("negativo"));
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.crear("Nuevo", "", "MEDICO", 2_000_000.0,
                30)).getMessage().contains("mayor"));
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.crear("Nuevo", "", "MEDICO", 100.0, 0))
                .getMessage().contains("minutos"));
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.crear("Nuevo", "", "MEDICO", 100.0, 481))
                .getMessage().contains("minutos"));
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.crear("Nuevo", "", "MEDICO", 100.0, null))
                .getMessage().contains("minutos"));
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.crear("Consulta", "", "MEDICO", 100.0, 30))
                .getMessage().contains("Ya existe"));
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.crear("consulta", "", "MEDICO", 100.0, 30))
                .getMessage().contains("Ya existe"), "sin distinguir mayúsculas");
        assertTrue(assertThrows(ReglaException.class, () -> ServicioRepo.actualizarTarifa(
                ServicioRepo.porNombre("Consulta").id(), 100.0, 0)).getMessage().contains("minutos"));
        assertNull(ServicioRepo.porNombre("Nuevo"), "no se creó nada");
    }

    @Test
    @DisplayName("Al cambiar una tarifa, las cuentas ya creadas conservan el precio anterior")
    void cuentasConservanPrecio() {
        Row consulta = ServicioRepo.porNombre("Consulta");
        double original = consulta.dbl("precio");
        int duracion = consulta.integer("duracion_min");
        try {
            ServicioRepo.actualizarTarifa(consulta.id(), 400.0, duracion);
            long c = Datos.cliente();
            long m = Datos.mascota(c, "Luna");
            long cuenta = CajaRepo.agregarRenglon(c, m, consulta.id(), null, 1);
            ServicioRepo.actualizarTarifa(consulta.id(), 999.0, duracion);
            assertEquals(400.0, CajaRepo.detalle(cuenta).get(0).dbl("precio_unitario"), 0.001);
            assertEquals(400.0, CajaRepo.cuenta(cuenta).dbl("total"), 0.001);
        } finally {
            ServicioRepo.actualizarTarifa(consulta.id(), original, duracion);
        }
    }
}
