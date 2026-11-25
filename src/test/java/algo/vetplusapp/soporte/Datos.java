package algo.vetplusapp.soporte;

import algo.vetplusapp.repo.CitaRepo;
import algo.vetplusapp.repo.ClienteRepo;
import algo.vetplusapp.repo.ServicioRepo;
import algo.vetplusapp.repo.UsuarioRepo;
import java.time.DayOfWeek;
import java.time.LocalDate;

/** Datos de ejemplo validos para armar escenarios en las pruebas. */
public final class Datos {

    private static int contador = 0;

    private Datos() {
    }

    public static synchronized int siguiente() {
        return ++contador;
    }

    /** CURP con estructura valida y distinta para cada numero (hasta 360). */
    public static String curp(int k) {
        String homoclave = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        return "GOMA900515HJCRRN" + homoclave.charAt(k % 36) + ((k / 36) % 10);
    }

    public static String telefono(int k) {
        return String.format("33%08d", k);
    }

    public static String correo(int k) {
        return "persona" + k + "@prueba.com";
    }

    /** Registra un cliente nuevo con datos distintos a los de cualquier otro. */
    public static long cliente() {
        int k = siguiente();
        return ClienteRepo.guardarPropietario(null, "Ana", "Prueba", telefono(k), correo(k), "Calle Hidalgo 123",
                curp(k), "", "");
    }

    public static long mascota(long propietarioId, String nombre) {
        return ClienteRepo.guardarMascota(null, propietarioId, nombre, "Perro", "Mestizo", "Hembra",
                LocalDate.now().minusYears(3), 12.5, false, "", "", "", "", null, false);
    }

    public static long medico() {
        return BD.idUsuario("lucia.torres@vetplus.com");
    }

    public static long servicio(String nombre) {
        return ServicioRepo.porNombre(nombre).id();
    }

    /** Proximo dia habil (no domingo) dentro de unos dias, como texto AAAA-MM-DD. */
    public static String fechaFutura() {
        LocalDate d = LocalDate.now().plusDays(3);
        while (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.plusDays(1);
        }
        return d.toString();
    }

    public static long cita(long mascotaId, String hora) {
        return CitaRepo.crear(mascotaId, servicio("Consulta"), medico(), fechaFutura(), hora, "Revision");
    }

    /** Alta de un empleado de ejemplo; devuelve la contrasena temporal. */
    public static String empleado(String correo, String rol, String curp) {
        return UsuarioRepo.guardar(null, "Pedro", "Ruiz", rol, "General", "Matutino", "Activo", correo,
                "3312345678", curp, "Calle Juarez 45");
    }
}
