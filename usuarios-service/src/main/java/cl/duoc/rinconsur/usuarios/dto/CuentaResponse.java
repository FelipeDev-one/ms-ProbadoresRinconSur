package cl.duoc.rinconsur.usuarios.dto;

// Cuenta recién creada o con contraseña nueva + su código de recuperación (se muestra una sola vez).
public record CuentaResponse(UsuarioResponse usuario, String codigoRecuperacion) {
}
