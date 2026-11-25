package algo.vetplusapp.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Acceso a la base de datos (MySQL/MariaDB de XAMPP). Una sola conexion compartida:
 * es una aplicacion de escritorio de un solo usuario a la vez.
 */
public final class Db {

    private static Connection conexion;
    private static int profundidad = 0;

    private Db() {
    }

    /** Datos de conexion. Por defecto los de XAMPP: localhost:3306, usuario root, sin contrasena. */
    private static Properties config() {
        Properties p = new Properties();
        p.setProperty("host", "localhost");
        p.setProperty("puerto", "3306");
        p.setProperty("base", "vetplus");
        p.setProperty("usuario", "root");
        p.setProperty("clave", "");
        Path archivo = Paths.get(System.getProperty("user.home"), ".vetplus", "conexion.properties");
        if (Files.exists(archivo)) {
            try (InputStream in = Files.newInputStream(archivo)) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new DbException("No se pudo leer " + archivo, e);
            }
        }
        for (String k : new String[]{"host", "puerto", "base", "usuario", "clave"}) {
            String sobre = System.getProperty("vetplus." + k);
            if (sobre != null) {
                p.setProperty(k, sobre);
            }
        }
        if (!p.getProperty("base").matches("[A-Za-z0-9_]+")) {
            throw new DbException("El nombre de la base de datos solo admite letras, numeros y guion bajo.", null);
        }
        return p;
    }

    private static String urlServidor(Properties p) {
        return "jdbc:mariadb://" + p.getProperty("host") + ":" + p.getProperty("puerto") + "/";
    }

    public static synchronized Connection con() {
        try {
            if (conexion == null || conexion.isClosed()) {
                Properties p = config();
                Class.forName("org.mariadb.jdbc.Driver");
                // la base se crea si no existe (utf8mb4 para acentos y enes)
                try (Connection admin = DriverManager.getConnection(urlServidor(p), p.getProperty("usuario"),
                        p.getProperty("clave")); Statement st = admin.createStatement()) {
                    st.execute("CREATE DATABASE IF NOT EXISTS `" + p.getProperty("base")
                            + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                }
                conexion = DriverManager.getConnection(urlServidor(p) + p.getProperty("base"),
                        p.getProperty("usuario"), p.getProperty("clave"));
            }
            return conexion;
        } catch (SQLException | ClassNotFoundException e) {
            throw new DbException("No se pudo conectar a MySQL (XAMPP): " + e.getMessage(), e);
        }
    }

    /** Crea las tablas si no existen y carga los datos iniciales. */
    public static void iniciar() {
        try {
            String script = leerRecurso("/db/schema.sql");
            try (Statement st = con().createStatement()) {
                for (String sentencia : script.split(";\\s*(\\r?\\n|$)")) {
                    String s = sentencia.trim();
                    if (!s.isEmpty()) {
                        st.execute(s);
                    }
                }
            }
            Semilla.cargar();
        } catch (SQLException | IOException e) {
            throw new DbException("No se pudo preparar la base de datos", e);
        }
    }

    private static String leerRecurso(String ruta) throws IOException {
        try (InputStream in = Db.class.getResourceAsStream(ruta)) {
            if (in == null) {
                throw new IOException("Falta el recurso " + ruta);
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String linea;
                while ((linea = r.readLine()) != null) {
                    if (!linea.trim().startsWith("--")) {
                        sb.append(linea).append('\n');
                    }
                }
            }
            return sb.toString();
        }
    }

    private static void enlazar(PreparedStatement ps, Object[] p) throws SQLException {
        for (int i = 0; i < p.length; i++) {
            Object v = p[i];
            if (v instanceof byte[]) {
                ps.setBytes(i + 1, (byte[]) v);
                continue;
            }
            if (v instanceof Boolean) {
                v = ((Boolean) v) ? 1 : 0;
            } else if (v != null && !(v instanceof Number) && !(v instanceof String)) {
                v = v.toString();
            }
            ps.setObject(i + 1, v);
        }
    }

    public static List<Row> consultar(String sql, Object... p) {
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            enlazar(ps, p);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                List<Row> filas = new ArrayList<>();
                while (rs.next()) {
                    Row r = new Row();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        Object valor = rs.getObject(i);
                        if (valor instanceof java.sql.Blob) {
                            valor = rs.getBytes(i);
                        }
                        r.put(md.getColumnLabel(i), valor);
                    }
                    filas.add(r);
                }
                return filas;
            }
        } catch (SQLException e) {
            throw new DbException("Error al consultar: " + sql, e);
        }
    }

    public static Row uno(String sql, Object... p) {
        List<Row> filas = consultar(sql, p);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /** Para consultas con una sola columna llamada v (suma). */
    public static double suma(String sql, Object... p) {
        Row r = uno(sql, p);
        return r == null || r.obj("v") == null ? 0.0 : r.dbl("v");
    }

    /** Para consultas con una sola columna llamada v (conteo). */
    public static int contar(String sql, Object... p) {
        Row r = uno(sql, p);
        return r == null ? 0 : r.integer("v");
    }

    public static int ejecutar(String sql, Object... p) {
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            enlazar(ps, p);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DbException("Error al guardar: " + sql, e);
        }
    }

    /** Inserta y devuelve el id generado (AUTO_INCREMENT). */
    public static long insertar(String sql, Object... p) {
        try (PreparedStatement ps = con().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            enlazar(ps, p);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DbException("Error al insertar: " + sql, e);
        }
    }

    /** Ejecuta varias operaciones como una sola: o se guardan todas o ninguna. */
    public static <T> T transaccion(Supplier<T> trabajo) {
        Connection c = con();
        boolean dueno = profundidad == 0;
        try {
            if (dueno) {
                c.setAutoCommit(false);
            }
            profundidad++;
            T resultado = trabajo.get();
            profundidad--;
            if (dueno) {
                c.commit();
                c.setAutoCommit(true);
            }
            return resultado;
        } catch (RuntimeException | SQLException e) {
            profundidad = Math.max(0, profundidad - 1);
            if (dueno) {
                try {
                    c.rollback();
                    c.setAutoCommit(true);
                } catch (SQLException ignorada) {
                    // sin accion
                }
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new DbException("Error en la transaccion", e);
        }
    }

    public static void transaccion(Runnable trabajo) {
        transaccion(() -> {
            trabajo.run();
            return null;
        });
    }

    public static class DbException extends RuntimeException {

        public DbException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }

    /** Error de regla del negocio: el mensaje se le muestra al usuario tal cual. */
    public static class ReglaException extends RuntimeException {

        public ReglaException(String mensaje) {
            super(mensaje);
        }
    }
}
