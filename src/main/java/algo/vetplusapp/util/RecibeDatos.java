package algo.vetplusapp.util;

/** Las vistas que reciben un dato al abrirse (id de mascota, cita, propietario...) implementan esto. */
public interface RecibeDatos {

    void cargar(Object dato);
}
