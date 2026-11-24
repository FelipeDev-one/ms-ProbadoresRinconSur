package cl.duoc.rinconsur.usuarios.dto;

// Nunca incluye la contraseña ni sus hashes. tieneContrasena=false: debe crearla en su primer ingreso.
public record UsuarioResponse(
        Long id,
        String rut,
        String nombre,
        String rol,
        String turnoLaboral,
        Boolean activo,
        Boolean tieneContrasena
) {
}
