package cl.duoc.rinconsur.usuarios.util;

import java.security.SecureRandom;

// Código XXXX-XXXX sin 0/O ni 1/I/L, para que sea fácil de anotar (igual que en la app).
public final class CodigoRecuperacion {

    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private CodigoRecuperacion() {
    }

    public static String generar() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) sb.append('-');
            sb.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    // Acepta el código con o sin guion y en minúsculas.
    public static String normalizar(String codigo) {
        return codigo.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
