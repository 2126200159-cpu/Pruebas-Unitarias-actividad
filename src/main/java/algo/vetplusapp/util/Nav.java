package algo.vetplusapp.util;

/** Punto unico para navegar entre vistas desde cualquier controlador. */
public final class Nav {

    private static Navegador actual;

    private Nav() {
    }

    public static void registrar(Navegador n) {
        actual = n;
    }

    public static void ir(String vista, Object dato) {
        if (actual != null) {
            actual.ir(vista, dato);
        }
    }

    public static void ir(String vista) {
        ir(vista, null);
    }
}
