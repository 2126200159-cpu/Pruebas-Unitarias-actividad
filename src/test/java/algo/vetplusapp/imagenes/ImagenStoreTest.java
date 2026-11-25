package algo.vetplusapp.imagenes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import algo.vetplusapp.db.Db.ReglaException;
import algo.vetplusapp.soporte.Imagenes;
import algo.vetplusapp.util.AlmacenamientoException;
import algo.vetplusapp.util.ImagenStore;
import algo.vetplusapp.util.Rutas;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Gestion de imagenes: guardar y recuperar, carpeta en Documentos, formatos, inexistentes y fallos de disco. */
class ImagenStoreTest {

    private String documentosOriginal;
    private Path temporal;

    @BeforeEach
    void preparar() throws IOException {
        documentosOriginal = System.getProperty("vetplus.documentos");
        temporal = Files.createTempDirectory("vetplus-img-");
        System.setProperty("vetplus.documentos", temporal.resolve("Documents").toString()); // aun no existe
    }

    @AfterEach
    void restaurar() {
        if (documentosOriginal == null) {
            System.clearProperty("vetplus.documentos");
        } else {
            System.setProperty("vetplus.documentos", documentosOriginal);
        }
    }

    private int archivos(Path dir) throws IOException {
        int n = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                n++;
            }
        }
        return n;
    }

    @Test
    @DisplayName("La carpeta de la aplicación se crea sola dentro de Documentos cuando no existe")
    void carpetaSeCreaSola() {
        assertFalse(Files.exists(temporal.resolve("Documents")), "al inicio no existe");
        Path app = Rutas.carpetaApp();
        assertTrue(Files.isDirectory(app));
        assertEquals("VetPlus", app.getFileName().toString());
        assertEquals(temporal.resolve("Documents"), app.getParent());
        assertTrue(Files.isDirectory(Rutas.carpetaImagenes()));
        assertTrue(Files.isDirectory(Rutas.carpetaLogs()));
        assertEquals(app, Rutas.carpetaApp(), "llamarla otra vez no falla si ya existe");
    }

    @Test
    @DisplayName("Una imagen PNG se guarda y se recupera idéntica")
    void guardarYRecuperarPng() {
        byte[] original = Imagenes.png(40, 30);
        String nombre = ImagenStore.guardar(original, "mascota");
        assertTrue(nombre.startsWith("mascota-") && nombre.endsWith(".png"), nombre);
        assertTrue(ImagenStore.existe(nombre));
        assertArrayEquals(original, ImagenStore.leer(nombre));
    }

    @Test
    @DisplayName("Una imagen JPG se guarda y se recupera idéntica")
    void guardarYRecuperarJpg() {
        byte[] original = Imagenes.jpg(50, 50);
        String nombre = ImagenStore.guardar(original, "empleado-7");
        assertTrue(nombre.endsWith(".jpg"), nombre);
        assertArrayEquals(original, ImagenStore.leer(nombre));
    }

    @Test
    @DisplayName("El formato se reconoce por el contenido, no por la extensión")
    void tipoPorContenido() {
        assertEquals("png", ImagenStore.tipo(Imagenes.png(5, 5)));
        assertEquals("jpg", ImagenStore.tipo(Imagenes.jpg(5, 5)));
        assertNull(ImagenStore.tipo(Imagenes.gif()));
        assertNull(ImagenStore.tipo(Imagenes.texto()));
        assertNull(ImagenStore.tipo(null));
        assertNull(ImagenStore.tipo(new byte[2]));
    }

    @Test
    @DisplayName("Formatos incorrectos, vacías y dañadas se rechazan con mensaje claro")
    void formatosIncorrectos() {
        assertTrue(assertThrows(ReglaException.class, () -> ImagenStore.guardar(Imagenes.texto(), "x"))
                .getMessage().contains("Formato de imagen no válido"));
        assertTrue(assertThrows(ReglaException.class, () -> ImagenStore.guardar(Imagenes.gif(), "x"))
                .getMessage().contains("PNG o JPG"));
        assertTrue(assertThrows(ReglaException.class, () -> ImagenStore.guardar(new byte[0], "x"))
                .getMessage().contains("vacía"));
        assertTrue(assertThrows(ReglaException.class, () -> ImagenStore.guardar(null, "x"))
                .getMessage().contains("vacía"));
        assertTrue(assertThrows(ReglaException.class, () -> ImagenStore.guardar(Imagenes.pngDanado(), "x"))
                .getMessage().contains("dañada"));
        byte[] enorme = new byte[ImagenStore.MAX_BYTES + 1];
        System.arraycopy(Imagenes.png(5, 5), 0, enorme, 0, 20);
        assertTrue(assertThrows(ReglaException.class, () -> ImagenStore.guardar(enorme, "x"))
                .getMessage().contains("5 MB"));
    }

    @Test
    @DisplayName("Una imagen rechazada no deja archivos en la carpeta")
    void rechazadaNoDejaArchivos() throws IOException {
        assertThrows(ReglaException.class, () -> ImagenStore.guardar(Imagenes.texto(), "x"));
        assertEquals(0, archivos(Rutas.carpetaImagenes()));
    }

    @Test
    @DisplayName("Imágenes inexistentes se controlan: devuelven null y no lanzan error")
    void inexistentes() {
        assertNull(ImagenStore.leer("no-existe.png"));
        assertNull(ImagenStore.leer(""));
        assertNull(ImagenStore.leer(null));
        assertFalse(ImagenStore.existe("no-existe.png"));
        assertNull(ImagenStore.leer("../../../../etc/passwd"), "no sale de la carpeta de imágenes");
        ImagenStore.borrar("no-existe.png"); // no debe lanzar
        ImagenStore.borrar(null);
    }

    @Test
    @DisplayName("Una imagen borrada a mano después se detecta sin romper la aplicación")
    void borradaAMano() throws IOException {
        String nombre = ImagenStore.guardar(Imagenes.png(10, 10), "mascota");
        Files.delete(Rutas.carpetaImagenes().resolve(nombre));
        assertNull(ImagenStore.leer(nombre));
    }

    @Test
    @DisplayName("Error de acceso al leer: devuelve null y lo deja en el log")
    void errorDeAccesoAlLeer() throws IOException {
        Path dir = Rutas.carpetaImagenes();
        Files.createDirectory(dir.resolve("carpeta-no-archivo.png")); // leer una carpeta como archivo falla
        assertNull(ImagenStore.leer("carpeta-no-archivo.png"));
    }

    @Test
    @DisplayName("Borrar elimina el archivo")
    void borrar() {
        String nombre = ImagenStore.guardar(Imagenes.png(10, 10), "mascota");
        ImagenStore.borrar(nombre);
        assertFalse(ImagenStore.existe(nombre));
    }

    @Test
    @DisplayName("leerArchivo valida archivos elegidos por el usuario")
    void leerArchivoElegido() throws IOException {
        Path bueno = temporal.resolve("foto.png");
        Files.write(bueno, Imagenes.png(12, 12));
        assertNotNull(ImagenStore.leerArchivo(bueno));
        Path falso = temporal.resolve("foto-falsa.png");
        Files.write(falso, Imagenes.texto());
        assertThrows(ReglaException.class, () -> ImagenStore.leerArchivo(falso));
        assertThrows(ReglaException.class, () -> ImagenStore.leerArchivo(temporal.resolve("no-existe.png")));
        assertThrows(ReglaException.class, () -> ImagenStore.leerArchivo(temporal));
        assertThrows(ReglaException.class, () -> ImagenStore.leerArchivo(null));
    }

    @Test
    @DisplayName("Fallo de almacenamiento: mensaje descriptivo y sin archivos a medias")
    void falloDeAlmacenamiento() throws IOException {
        Path app = Rutas.carpetaApp();
        Files.delete(Rutas.carpetaImagenes());
        Files.createFile(app.resolve("imagenes")); // "imagenes" ahora es un archivo: no se puede escribir dentro
        AlmacenamientoException ex = assertThrows(AlmacenamientoException.class,
                () -> ImagenStore.guardar(Imagenes.png(10, 10), "mascota"));
        assertTrue(ex.getMessage().contains("No se pudo"), ex.getMessage());
        assertTrue(ex.getMessage().contains("permisos") || ex.getMessage().contains("espacio"), ex.getMessage());
        assertTrue(Files.isRegularFile(app.resolve("imagenes")), "la información existente no se tocó");
    }

    @Test
    @DisplayName("Si Documentos no se puede usar, el error es descriptivo")
    void documentosNoUsable() throws IOException {
        Path archivo = temporal.resolve("soy-un-archivo");
        Files.createFile(archivo);
        System.setProperty("vetplus.documentos", archivo.toString()); // Documentos es un archivo, no carpeta
        AlmacenamientoException ex = assertThrows(AlmacenamientoException.class, Rutas::carpetaApp);
        assertTrue(ex.getMessage().contains("No se pudo crear la carpeta de la aplicación"), ex.getMessage());
    }
}
