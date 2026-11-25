package algo.vetplusapp.soporte;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Genera imagenes y archivos falsos para las pruebas de imagenes. */
public final class Imagenes {

    private Imagenes() {
    }

    private static byte[] codificar(String formato, int ancho, int alto) {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < ancho; x++) {
            for (int y = 0; y < alto; y++) {
                img.setRGB(x, y, (x * 7 + y * 13) & 0xFFFFFF);
            }
        }
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(img, formato, salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] png(int ancho, int alto) {
        return codificar("png", ancho, alto);
    }

    public static byte[] jpg(int ancho, int alto) {
        return codificar("jpg", ancho, alto);
    }

    public static byte[] gif() {
        return codificar("gif", 8, 8);
    }

    /** Cabecera PNG correcta pero contenido roto. */
    public static byte[] pngDanado() {
        byte[] bueno = png(20, 20);
        byte[] roto = new byte[40];
        System.arraycopy(bueno, 0, roto, 0, 40);
        return roto;
    }

    public static byte[] texto() {
        return "esto no es una imagen, solo texto".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
