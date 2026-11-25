package algo.vetplusapp.relaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Db.DbException;
import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.repo.CajaRepo;
import algo.vetplusapp.repo.CitaRepo;
import algo.vetplusapp.repo.ClienteRepo;
import algo.vetplusapp.repo.ComprasRepo;
import algo.vetplusapp.soporte.BD;
import algo.vetplusapp.soporte.Datos;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Relaciones entre tablas (llaves foraneas) y reglas de agenda y compras. */
class RelacionesYValidacionesTest {

    private long cliente;
    private long mascota;

    @BeforeAll
    static void inicio() {
        BD.preparar();
    }

    @BeforeEach
    void limpio() {
        BD.limpiar();
        cliente = Datos.cliente();
        mascota = Datos.mascota(cliente, "Luna");
    }

    @Test
    @DisplayName("La base de datos rechaza una mascota sin propietario real")
    void mascotaSinPropietario() {
        assertThrows(DbException.class, () -> Db.insertar("INSERT INTO mascota (propietario_id, nombre) "
                + "VALUES (999999, 'Fantasma')"));
        assertEquals(1, BD.contar("mascota"));
    }

    @Test
    @DisplayName("La base de datos rechaza una cita para una mascota que no existe")
    void citaSinMascota() {
        assertThrows(DbException.class, () -> CitaRepo.crear(999999L, Datos.servicio("Consulta"), Datos.medico(),
                Datos.fechaFutura(), "10:00", ""));
        assertEquals(0, BD.contar("cita"));
    }

    @Test
    @DisplayName("No se pueden eliminar registros que tienen relaciones dependientes")
    void noSeEliminanConDependientes() {
        Datos.cita(mascota, "10:00");
        assertThrows(DbException.class, () -> Db.ejecutar("DELETE FROM propietario WHERE id = ?", cliente),
                "propietario con mascotas");
        assertThrows(DbException.class, () -> Db.ejecutar("DELETE FROM mascota WHERE id = ?", mascota),
                "mascota con citas");
        assertThrows(DbException.class, () -> Db.ejecutar("DELETE FROM usuario WHERE id = ?", Datos.medico()),
                "profesional con citas");
        assertThrows(DbException.class, () -> Db.ejecutar("DELETE FROM servicio WHERE id = ?",
                Datos.servicio("Consulta")), "servicio con citas");
        assertNotNull(ClienteRepo.propietario(cliente));
        assertNotNull(ClienteRepo.mascota(mascota));
        assertEquals(1, BD.contar("cita"));
    }

    @Test
    @DisplayName("Las cuentas quedan ligadas al propietario y a las mascotas correctas")
    void cuentaRelacionada() {
        long cuenta = CajaRepo.agregarRenglon(cliente, mascota, Datos.servicio("Consulta"), null, 1);
        assertEquals(cliente, CajaRepo.cuenta(cuenta).lng("propietario_id"));
        assertEquals("Luna", CajaRepo.detalle(cuenta).get(0).str("mascota"));
    }

    @Test
    @DisplayName("Una cita no puede quedar en el pasado")
    void citaEnElPasado() {
        String ayer = LocalDate.now().minusDays(1).toString();
        assertTrue(assertThrows(ReglaException.class, () -> CitaRepo.crear(mascota, Datos.servicio("Consulta"),
                Datos.medico(), ayer, "10:00", "")).getMessage().contains("pasado"));
        assertEquals(0, BD.contar("cita"));
    }

    @Test
    @DisplayName("Las citas respetan los domingos cerrados y el turno del profesional")
    void horarios() {
        LocalDate domingo = LocalDate.now().plusDays(1);
        while (domingo.getDayOfWeek() != DayOfWeek.SUNDAY) {
            domingo = domingo.plusDays(1);
        }
        String d = domingo.toString();
        assertTrue(assertThrows(ReglaException.class, () -> CitaRepo.crear(mascota, Datos.servicio("Consulta"),
                Datos.medico(), d, "10:00", "")).getMessage().contains("fuera del turno"));
        assertTrue(assertThrows(ReglaException.class, () -> CitaRepo.crear(mascota, Datos.servicio("Consulta"),
                Datos.medico(), Datos.fechaFutura(), "16:00", "")).getMessage().contains("fuera del turno"),
                "la médica matutina no atiende por la tarde");
        assertTrue(assertThrows(ReglaException.class, () -> CitaRepo.crear(mascota, Datos.servicio("Consulta"),
                Datos.medico(), Datos.fechaFutura(), "13:45", "")).getMessage().contains("fuera del turno"),
                "la cita terminaría después de su turno");
        assertTrue(CitaRepo.horariosLibres(Datos.medico(), d, 30).isEmpty(), "domingo sin horarios");
        assertEquals(0, BD.contar("cita"));
    }

    @Test
    @DisplayName("No se permiten dos citas del mismo profesional al mismo tiempo")
    void sinTraslapes() {
        Datos.cita(mascota, "10:00");
        long otra = Datos.mascota(cliente, "Milo");
        assertTrue(assertThrows(ReglaException.class, () -> Datos.cita(otra, "10:00")).getMessage()
                .contains("ocupado"));
        assertTrue(CitaRepo.hayConflicto(Datos.medico(), Datos.fechaFutura(), "10:15", 30, null));
        Datos.cita(otra, "10:30"); // el siguiente horario sí está libre
        assertEquals(2, BD.contar("cita"));
        assertTrue(CitaRepo.horariosLibres(Datos.medico(), Datos.fechaFutura(), 30).stream()
                .noneMatch(h -> h.equals("10:00") || h.equals("10:30")));
    }

    @Test
    @DisplayName("El profesional debe corresponder al servicio")
    void profesionalDelServicio() {
        long estilista = BD.idUsuario("fernanda.castillo@vetplus.com");
        assertTrue(assertThrows(ReglaException.class, () -> CitaRepo.crear(mascota, Datos.servicio("Consulta"),
                estilista, Datos.fechaFutura(), "10:00", "")).getMessage().contains("no corresponde"));
    }

    @Test
    @DisplayName("Órdenes de compra: fecha de entrega, cantidades y proveedor se validan")
    void ordenesDeCompra() {
        long insumo = ComprasRepo.insumos().get(0).id();
        long proveedor = ComprasRepo.proveedores().get(0).id();
        List<ComprasRepo.RenglonOrden> bien = Collections.singletonList(
                new ComprasRepo.RenglonOrden(insumo, "Vacuna", 10, 95));
        String folio = ComprasRepo.siguienteFolio();

        assertTrue(assertThrows(ReglaException.class, () -> ComprasRepo.guardarOrden(folio, proveedor,
                LocalDate.now().minusDays(1), "", bien, true)).getMessage().contains("pasada"));
        assertTrue(assertThrows(ReglaException.class, () -> ComprasRepo.guardarOrden(folio, proveedor,
                LocalDate.now().plusDays(3), "", Collections.singletonList(new ComprasRepo.RenglonOrden(insumo,
                "Vacuna", 0, 95)), true)).getMessage().contains("mayor a 0"));
        assertTrue(assertThrows(ReglaException.class, () -> ComprasRepo.guardarOrden(folio, null,
                LocalDate.now().plusDays(3), "", bien, true)).getMessage().contains("proveedor"));
        assertTrue(assertThrows(ReglaException.class, () -> ComprasRepo.guardarOrden(folio, proveedor,
                LocalDate.now().plusDays(3), "", new ArrayList<>(), true)).getMessage().contains("al menos un"));
        assertEquals(0, BD.contar("orden_compra"), "ninguna orden inválida se guardó");

        long id = ComprasRepo.guardarOrden(folio, proveedor, LocalDate.now().plusDays(3), "", bien, true);
        assertTrue(id > 0);
        assertEquals(950.0, Db.suma("SELECT total AS v FROM orden_compra WHERE id = ?", id), 0.001);
    }

    @Test
    @DisplayName("Insumos: existencia, mínimo y precio no pueden ser negativos")
    void insumos() {
        assertTrue(assertThrows(ReglaException.class, () -> ComprasRepo.guardarInsumo(null, "Gasas", "Curación", -1,
                5, 10)).getMessage().contains("negativos"));
        assertTrue(assertThrows(ReglaException.class, () -> ComprasRepo.guardarInsumo(null, "", "Curación", 1, 5, 10))
                .getMessage().contains("nombre"));
    }
}
