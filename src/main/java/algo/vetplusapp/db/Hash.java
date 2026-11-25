package algo.vetplusapp.db;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash con sal (PBKDF2-HMAC-SHA256). Formato guardado: pbkdf2$iteraciones$sal$hash. */
public final class Hash {

    private static final int ITERACIONES = 120_000;
    private static final SecureRandom AZAR = new SecureRandom();

    private Hash() {
    }

    public static String crear(String contrasena) {
        byte[] sal = new byte[16];
        AZAR.nextBytes(sal);
        byte[] h = derivar(contrasena, sal, ITERACIONES);
        return "pbkdf2$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal)
                + "$" + Base64.getEncoder().encodeToString(h);
    }

    public static boolean verificar(String contrasena, String guardado) {
        if (guardado == null) {
            return false;
        }
        String[] partes = guardado.split("\\$");
        if (partes.length != 4 || !"pbkdf2".equals(partes[0])) {
            return false;
        }
        int it = Integer.parseInt(partes[1]);
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        return MessageDigest.isEqual(esperado, derivar(contrasena, sal, it));
    }

    private static byte[] derivar(String contrasena, byte[] sal, int it) {
        try {
            PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(), sal, it, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash", e);
        }
    }

    /** Contrasena temporal legible (sin caracteres que se confundan). */
    public static String temporal() {
        String letras = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(letras.charAt(AZAR.nextInt(letras.length())));
        }
        return sb.toString();
    }
}
