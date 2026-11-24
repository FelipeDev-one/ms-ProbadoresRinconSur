package cl.duoc.rinconsur.usuarios.util;

// Mismo algoritmo de dígito verificador que usa la app (utils/RutValidator.kt).
public final class RutValidator {

    private RutValidator() {
    }

    // "12.984.561-9" → "12984561-9"
    public static String normalizar(String rut) {
        String limpio = rut.replace(".", "").replace("-", "").replace(" ", "").toUpperCase();
        if (limpio.length() < 2) return limpio;
        return limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
    }

    public static char calcularDv(int cuerpo) {
        int suma = 0;
        int multiplicador = 2;
        int numero = cuerpo;
        while (numero > 0) {
            suma += (numero % 10) * multiplicador;
            numero /= 10;
            multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) return '0';
        if (resto == 10) return 'K';
        return (char) ('0' + resto);
    }

    public static boolean esValido(String rut) {
        if (rut == null) return false;
        String[] partes = normalizar(rut).split("-");
        if (partes.length != 2 || partes[1].length() != 1) return false;
        int cuerpo;
        try {
            cuerpo = Integer.parseInt(partes[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        return cuerpo >= 1_000_000 && calcularDv(cuerpo) == partes[1].charAt(0);
    }
}
