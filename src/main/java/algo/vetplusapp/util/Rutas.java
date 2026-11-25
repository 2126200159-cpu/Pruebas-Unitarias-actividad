package algo.vetplusapp.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Carpetas de la aplicacion dentro de Documentos: Documentos/VetPlus, con imagenes/ y logs/.
 * Se crean solas si no existen. La propiedad vetplus.documentos permite cambiar la carpeta base (pruebas).
 */
public final class Rutas {

    private Rutas() {
    }

    /** Carpeta Documentos del usuario (Documents o Documentos); si no existe se usa Documents. */
    public static Path documentos() {
        String propia = System.getProperty("vetplus.documentos");
        if (propia != null && !propia.trim().isEmpty()) {
            return Paths.get(propia.trim());
        }
        Path home = Paths.get(System.getProperty("user.home"));
        for (String nombre : new String[]{"Documents", "Documentos"}) {
            Path p = home.resolve(nombre);
            if (Files.isDirectory(p)) {
                return p;
            }
        }
        return home.resolve("Documents");
    }

    public static Path carpetaApp() {
        return asegurar(documentos().resolve("VetPlus"));
    }

    public static Path carpetaImagenes() {
        return asegurar(carpetaApp().resolve("imagenes"));
    }

    public static Path carpetaLogs() {
        return asegurar(carpetaApp().resolve("logs"));
    }

    public static Path archivoLog() {
        return carpetaLogs().resolve("vetplus.log");
    }

    private static Path asegurar(Path carpeta) {
        try {
            Files.createDirectories(carpeta);
            return carpeta;
        } catch (IOException | RuntimeException e) {
            throw new AlmacenamientoException("No se pudo crear la carpeta de la aplicación en " + carpeta
                    + ". Verifica que tengas permisos de escritura.", e);
        }
    }
}
