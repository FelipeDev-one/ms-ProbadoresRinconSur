package cl.duoc.rinconsur.turnos.dto;

// Lo que turnos-service necesita saber de un usuario (respuesta de usuarios-service).
public record UsuarioDto(String rut, String nombre, String rol, Boolean activo) {
}
