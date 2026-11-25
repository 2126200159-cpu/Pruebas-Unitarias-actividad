package algo.vetplusapp.soporte;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.ServicioRepo;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base de datos de pruebas. Usa SIEMPRE la base vetplus_test (nunca la real), en el MariaDB de XAMPP:
 * hay que tener MySQL encendido para correr estas pruebas.
 */
public final class BD {

    public static final String[] CORREOS_SEMILLA = {"admin@vetplus.com", "lucia.torres@vetplus.com",
        "ricardo.mendoza@vetplus.com", "fernanda.castillo@vetplus.com", "diego.salazar@vetplus.com"};
    private static final String[] TABLAS = {"actividad", "pago", "cuenta_detalle", "cuenta", "servicio_estetica",
        "atencion_medica", "vacuna_cuidado", "estancia", "cita", "mascota", "propietario",
        "orden_compra_renglon", "orden_compra"};

    static {
        if (System.getProperty("vetplus.base") == null) {
            System.setProperty("vetplus.base", "vetplus_test");
        }
        if (System.getProperty("vetplus.documentos") == null) {
            System.setProperty("vetplus.documentos",
                    Paths.get("reportes", "documentos-prueba").toAbsolutePath().toString());
        }
    }

    private BD() {
    }

    /** Crea la base de pruebas (si no existe), las tablas y los datos iniciales. */
    public static void preparar() {
        if (!"vetplus_test".equals(System.getProperty("vetplus.base"))) {
            throw new IllegalStateException("Las pruebas solo pueden usar la base vetplus_test.");
        }
        Db.iniciar();
    }

    /** Deja la base como recien sembrada: sin clientes, citas ni cuentas; conserva el catalogo y el personal base. */
    public static void limpiar() {
        Db.ejecutar("SET FOREIGN_KEY_CHECKS = 0");
        for (String t : TABLAS) {
            Db.ejecutar("TRUNCATE TABLE " + t);
        }
        Db.ejecutar("DELETE FROM usuario WHERE correo_usuario NOT IN (?, ?, ?, ?, ?)", (Object[]) CORREOS_SEMILLA);
        Db.ejecutar("DELETE FROM servicio WHERE nombre NOT IN (?, ?, ?, ?, ?, ?)",
                ServicioRepo.BASE.toArray());
        Db.ejecutar("SET FOREIGN_KEY_CHECKS = 1");
        borrarImagenes();
    }

    /** Vacia la carpeta de imagenes de pruebas. */
    public static void borrarImagenes() {
        Path dir = algo.vetplusapp.util.Rutas.carpetaImagenes();
        try (DirectoryStream<Path> archivos = Files.newDirectoryStream(dir)) {
            for (Path p : archivos) {
                if (Files.isRegularFile(p)) {
                    Files.deleteIfExists(p);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo limpiar la carpeta de imagenes de pruebas", e);
        }
    }

    public static long idUsuario(String correo) {
        Row r = Db.uno("SELECT id FROM usuario WHERE correo_usuario = ?", correo);
        return r == null ? 0 : r.id();
    }

    public static int contar(String tabla) {
        return Db.contar("SELECT COUNT(*) AS v FROM " + tabla);
    }

    public static int archivosDeImagen() {
        Path dir = algo.vetplusapp.util.Rutas.carpetaImagenes();
        try (DirectoryStream<Path> archivos = Files.newDirectoryStream(dir)) {
            int n = 0;
            for (Path p : archivos) {
                if (Files.isRegularFile(p)) {
                    n++;
                }
            }
            return n;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
