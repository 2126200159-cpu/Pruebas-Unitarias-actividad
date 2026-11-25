package algo.vetplusapp.util;

/** Fallo tecnico de disco o carpetas (permisos, disco lleno...). El mensaje es apto para mostrarse al usuario. */
public class AlmacenamientoException extends RuntimeException {

    public AlmacenamientoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
