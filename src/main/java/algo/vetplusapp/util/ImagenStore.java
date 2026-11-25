package algo.vetplusapp.util;

import algo.vetplusapp.db.Db.ReglaException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;

/**
 * Imagenes de personas y mascotas como archivos en Documentos/VetPlus/imagenes. En la base de datos solo se
 * guarda el nombre del archivo. Valida formato real (PNG o JPG), que la imagen se pueda leer y su tamano.
 */
public final class ImagenStore {

    public static final int MAX_BYTES = 5 * 1024 * 1024;

    private ImagenStore() {
    }

    /** "png", "jpg" o null segun los primeros bytes del archivo (no por la extension del nombre). */
    public static String tipo(byte[] d) {
        if (d == null || d.length < 4) {
            return null;
        }
        if ((d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return "png";
        }
        if ((d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        return null;
    }

    /** Valida la imagen; lanza ReglaException con un mensaje claro si no es aceptable. */
    public static String validar(byte[] datos) {
        if (datos == null || datos.length == 0) {
            throw new ReglaException("La imagen está vacía.");
        }
        if (datos.length > MAX_BYTES) {
            throw new ReglaException("La imagen pesa más de 5 MB. Elige una más ligera.");
        }
        String tipo = tipo(datos);
        if (tipo == null) {
            throw new ReglaException("Formato de imagen no válido. Usa un archivo PNG o JPG.");
        }
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(datos));
            if (img == null) {
                throw new ReglaException("La imagen está dañada o no se puede leer. Elige otra.");
            }
        } catch (IOException e) {
            throw new ReglaException("La imagen está dañada o no se puede leer. Elige otra.");
        }
        return tipo;
    }

    /** Lee un archivo elegido por el usuario y valida que sea una imagen aceptable. */
    public static byte[] leerArchivo(Path archivo) {
        if (archivo == null || !Files.isRegularFile(archivo)) {
            throw new ReglaException("El archivo de imagen no existe o ya no está disponible.");
        }
        byte[] datos;
        try {
            datos = Files.readAllBytes(archivo);
        } catch (IOException e) {
            throw new AlmacenamientoException("No se pudo leer el archivo de imagen. Verifica que no esté abierto en "
                    + "otro programa y que tengas permiso para leerlo.", e);
        }
        validar(datos);
        return datos;
    }

    /**
     * Guarda la imagen en la carpeta de la aplicacion y devuelve el nombre del archivo. Escribe primero a un
     * archivo temporal y lo renombra al final: si algo falla no queda un archivo a medias.
     */
    public static String guardar(byte[] datos, String prefijo) {
        String tipo = validar(datos);
        Path dir = Rutas.carpetaImagenes();
        String nombre = prefijo.replaceAll("[^A-Za-z0-9_-]", "") + "-" + System.currentTimeMillis() + "-"
                + (System.nanoTime() % 100000) + "." + tipo;
        Path destino = dir.resolve(nombre);
        Path temporal = dir.resolve(nombre + ".tmp");
        try {
            Files.write(temporal, datos);
            try {
                Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return nombre;
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException ignorada) {
                // sin accion
            }
            throw new AlmacenamientoException("No se pudo guardar la imagen en la carpeta de la aplicación ("
                    + dir + "). Verifica el espacio en disco y los permisos.", e);
        }
    }

    private static Path resolver(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }
        Path soloNombre = Paths.get(nombre.trim()).getFileName(); // evita rutas con ../
        return soloNombre == null ? null : Rutas.carpetaImagenes().resolve(soloNombre.toString());
    }

    public static boolean existe(String nombre) {
        Path p = resolver(nombre);
        return p != null && Files.isRegularFile(p);
    }

    /** Devuelve los bytes de la imagen, o null si no hay nombre, el archivo no existe o no se puede leer. */
    public static byte[] leer(String nombre) {
        Path p;
        try {
            p = resolver(nombre);
        } catch (RuntimeException e) {
            AppLog.error("ImagenStore", "leer", e);
            return null;
        }
        if (p == null) {
            return null;
        }
        if (!Files.exists(p)) {
            AppLog.info("ImagenStore", "leer", "Imagen no encontrada: " + p.getFileName());
            return null;
        }
        try {
            return Files.readAllBytes(p);
        } catch (IOException | RuntimeException e) {
            AppLog.error("ImagenStore", "leer", e);
            return null;
        }
    }

    /** Borra el archivo si existe. Nunca lanza: un archivo que no se pueda borrar solo queda en el log. */
    public static void borrar(String nombre) {
        try {
            Path p = resolver(nombre);
            if (p != null) {
                Files.deleteIfExists(p);
            }
        } catch (IOException | RuntimeException e) {
            AppLog.error("ImagenStore", "borrar", e);
        }
    }
}
