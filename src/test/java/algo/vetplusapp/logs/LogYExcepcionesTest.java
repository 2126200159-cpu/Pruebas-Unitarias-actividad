package algo.vetplusapp.logs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import algo.vetplusapp.db.Db;
import algo.vetplusapp.util.AlmacenamientoException;
import algo.vetplusapp.util.AppLog;
import algo.vetplusapp.util.Manejo;
import algo.vetplusapp.util.Rutas;
import algo.vetplusapp.util.Sensible;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Excepciones y logs: mensajes descriptivos al usuario, errores tecnicos en el archivo log con fecha, hora,
 * modulo, operacion y tipo de excepcion; sin contrasenas ni datos sensibles; la aplicacion sigue funcionando.
 */
class LogYExcepcionesTest {

    private static final Pattern FECHA_HORA = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\|");

    private static String marca() {
        return "M" + System.nanoTime();
    }

    /** Lineas del log que contienen la marca (las demas pruebas tambien escriben en el mismo archivo). */
    private static List<String> lineasCon(String marca) throws IOException {
        List<String> encontradas = new ArrayList<>();
        for (String l : Files.readAllLines(Rutas.archivoLog(), StandardCharsets.UTF_8)) {
            if (l.contains(marca)) {
                encontradas.add(l);
            }
        }
        return encontradas;
    }

    private static void operacionDePrueba(String marca) {
        throw new Db.ReglaException("El teléfono debe tener 10 dígitos. " + marca);
    }

    private static void fallaTecnicaDePrueba(String marca) {
        throw new Db.DbException("Error al guardar: INSERT INTO usuario (contrasena_hash) VALUES (?) " + marca,
                new IllegalStateException("conexión rechazada " + marca));
    }

    @Test
    @DisplayName("Una regla del negocio muestra su mensaje al usuario y queda en el log como AVISO")
    void reglaDelNegocio() throws IOException {
        String m = marca();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> operacionDePrueba(m));
        Manejo.Resultado r = Manejo.procesar(ex);
        assertEquals(ex.getMessage(), r.mensaje, "el usuario ve el mensaje descriptivo tal cual");
        assertFalse(r.esError);
        List<String> lineas = lineasCon(m);
        assertEquals(1, lineas.size(), "un evento = una linea");
        assertTrue(lineas.get(0).contains("| AVISO |"), lineas.get(0));
    }

    @Test
    @DisplayName("El log identifica fecha, hora, módulo, operación y tipo de excepción")
    void camposDelLog() throws IOException {
        String m = marca();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> fallaTecnicaDePrueba(m));
        Manejo.procesar(ex);
        String linea = lineasCon(m).get(0);
        assertTrue(FECHA_HORA.matcher(linea).find(), "fecha y hora con milisegundos: " + linea);
        assertTrue(linea.contains("| ERROR |"), linea);
        assertTrue(linea.contains("| LogYExcepcionesTest |"), "módulo: " + linea);
        assertTrue(linea.contains("| fallaTecnicaDePrueba |"), "operación: " + linea);
        assertTrue(linea.contains("DbException"), "tipo de excepción: " + linea);
        assertTrue(linea.contains("IllegalStateException"), "causa raíz: " + linea);
    }

    @Test
    @DisplayName("El error técnico se oculta al usuario: ve un mensaje claro sin SQL")
    void mensajeAmigableSinDetalleTecnico() {
        String m = marca();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> fallaTecnicaDePrueba(m));
        Manejo.Resultado r = Manejo.procesar(ex);
        assertTrue(r.esError);
        assertTrue(r.mensaje.contains("base de datos") && r.mensaje.contains("MySQL"), r.mensaje);
        assertFalse(r.mensaje.contains("INSERT"), "no se muestra SQL al usuario");
        assertFalse(r.mensaje.contains("contrasena"), r.mensaje);
    }

    @Test
    @DisplayName("Una falla inesperada muestra un mensaje general y no filtra el detalle interno")
    void falloInesperado() throws IOException {
        String m = marca();
        Manejo.Resultado r = Manejo.procesar(new NullPointerException("detalle interno " + m));
        assertTrue(r.esError);
        assertTrue(r.mensaje.contains("problema inesperado") && r.mensaje.contains("log"), r.mensaje);
        assertFalse(r.mensaje.contains(m), "el texto interno no se muestra al usuario");
        assertEquals(1, lineasCon(m).size(), "pero sí queda en el log");
    }

    @Test
    @DisplayName("Un fallo de almacenamiento muestra su mensaje descriptivo y se registra")
    void falloDeAlmacenamiento() throws IOException {
        String m = marca();
        AlmacenamientoException ex = new AlmacenamientoException("No se pudo guardar la imagen. Verifica el espacio "
                + "en disco y los permisos.", new IOException("disco lleno " + m));
        Manejo.Resultado r = Manejo.procesar(ex);
        assertEquals(ex.getMessage(), r.mensaje);
        assertTrue(r.esError);
        assertTrue(lineasCon(m).get(0).contains("IOException"));
    }

    @Test
    @DisplayName("No se registran contraseñas, hashes ni datos personales en el log")
    void sinInformacionSensible() throws IOException {
        String m = marca();
        String secreto = "contrasena=Secreta123 password: Abc12345 clave=\"MiClave99\" pbkdf2$120000$QUJDREVGR0hJSktMTU5PUA==$"
                + "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVowMTIzNDU2Nzg5 correo ana.lopez@dominio.com "
                + "CURP GOMA900515HJCRRN09 telefono 3312345678 " + m;
        Manejo.procesar(new IllegalArgumentException(secreto));
        String linea = lineasCon(m).get(0);
        for (String prohibido : new String[]{"Secreta123", "Abc12345", "MiClave99", "pbkdf2$", "ana.lopez",
            "GOMA900515HJCRRN09", "3312345678"}) {
            assertFalse(linea.contains(prohibido), "el log no debe contener: " + prohibido + " -> " + linea);
        }
        assertTrue(linea.contains("[HASH]") && linea.contains("[CURP]") && linea.contains("[TEL]"), linea);
        assertTrue(linea.contains("a***@dominio.com"), linea);
    }

    @Test
    @DisplayName("El sanitizador oculta cada tipo de dato sensible")
    void sanitizador() {
        assertEquals("password=***", Sensible.limpiar("password=abc123"));
        assertEquals("contraseña=***", Sensible.limpiar("contraseña: otraClave"));
        assertEquals("[HASH]", Sensible.limpiar("pbkdf2$120000$AAAA$BBBB"));
        assertEquals("j***@correo.mx", Sensible.limpiar("juan.perez@correo.mx"));
        assertEquals("tel [TEL]", Sensible.limpiar("tel 3312345678"));
        assertEquals("", Sensible.limpiar(null));
        assertTrue(Sensible.limpiar("x".repeat(2000)).length() <= 601, "recorta mensajes enormes");
        assertFalse(Sensible.limpiar("a\nb\r\nc").contains("\n"), "una sola línea");
    }

    @Test
    @DisplayName("El log no guarda el rastro completo de la excepción")
    void sinRastroCompleto() throws IOException {
        String m = marca();
        Manejo.procesar(new IllegalStateException("fallo " + m));
        String linea = lineasCon(m).get(0);
        assertFalse(linea.contains("\tat "), linea);
        assertFalse(linea.contains("java.base/"), linea);
    }

    @Test
    @DisplayName("La aplicación continúa funcionando después de errores recuperables")
    void continuaDespuesDeErrores() {
        String m = marca();
        List<String> bitacora = new ArrayList<>();
        Optional<Manejo.Resultado> r1 = Manejo.ejecutar(() -> operacionDePrueba(m));
        Optional<Manejo.Resultado> r2 = Manejo.ejecutar(() -> {
            throw new IllegalStateException("otro fallo " + m);
        });
        Optional<Manejo.Resultado> r3 = Manejo.ejecutar(() -> bitacora.add("sigue funcionando"));
        assertTrue(r1.isPresent() && !r1.get().esError, "regla del negocio");
        assertTrue(r2.isPresent() && r2.get().esError, "fallo técnico");
        assertFalse(r3.isPresent(), "la siguiente operación se ejecuta normal");
        assertEquals(1, bitacora.size());
    }

    @Test
    @DisplayName("Si el log no se puede escribir, la aplicación no se cae")
    void logNoEscribible() throws IOException {
        String original = System.getProperty("vetplus.documentos");
        Path archivo = Files.createTempFile("vetplus-sin-carpeta", ".tmp");
        try {
            System.setProperty("vetplus.documentos", archivo.toString()); // Documentos es un archivo
            AppLog.info("Prueba", "logNoEscribible", "esto no debe lanzar excepción");
            Manejo.procesar(new IllegalStateException("tampoco esto"));
        } finally {
            System.setProperty("vetplus.documentos", original);
            Files.deleteIfExists(archivo);
        }
        assertNotNull(Rutas.archivoLog(), "el log vuelve a funcionar con la carpeta normal");
    }

    @Test
    @DisplayName("La carpeta de logs se crea sola y el archivo se llama vetplus.log")
    void archivoDeLog() throws IOException {
        String original = System.getProperty("vetplus.documentos");
        Path tmp = Files.createTempDirectory("vetplus-log-");
        try {
            System.setProperty("vetplus.documentos", tmp.resolve("Documentos").toString());
            AppLog.info("Prueba", "archivoDeLog", "primer evento");
            Path log = tmp.resolve("Documentos").resolve("VetPlus").resolve("logs").resolve("vetplus.log");
            assertTrue(Files.isRegularFile(log), "se creó " + log);
            assertTrue(Files.readString(log).contains("| INFO | Prueba | archivoDeLog |"));
        } finally {
            System.setProperty("vetplus.documentos", original);
        }
    }
}
